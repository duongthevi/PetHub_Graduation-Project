package com.dtvi.Pet.Ecommerce.service.impl;

import com.dtvi.Pet.Ecommerce.config.VnPayProperties;
import com.dtvi.Pet.Ecommerce.dto.payment.PaymentResultResponse;
import com.dtvi.Pet.Ecommerce.dto.payment.PaymentUrlResponse;
import com.dtvi.Pet.Ecommerce.dto.payment.VnPayIpnResponse;
import com.dtvi.Pet.Ecommerce.entity.Order;
import com.dtvi.Pet.Ecommerce.entity.Payment;
import com.dtvi.Pet.Ecommerce.entity.User;
import com.dtvi.Pet.Ecommerce.enums.PaymentProvider;
import com.dtvi.Pet.Ecommerce.enums.PaymentStatus;
import com.dtvi.Pet.Ecommerce.exception.BusinessException;
import com.dtvi.Pet.Ecommerce.payment.VnPaySigner;
import com.dtvi.Pet.Ecommerce.repository.PaymentRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class VnPayService {

    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNPAY_DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final VnPayProperties properties;
    private final VnPaySigner signer;
    private final PaymentRepo paymentRepo;
    private final OrderService orderService;

    public PaymentUrlResponse createPaymentUrl(User user, Long orderId, String clientIp) {
        requireConfigured();
        Order order = orderService.getOwnedVnpayOrder(user, orderId);
        Payment payment = paymentRepo.findByProviderAndTransactionRef(
                        PaymentProvider.VNPAY,
                        order.getOrderCode()
                )
                .orElseGet(() -> createPayment(order));

        LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);
        LocalDateTime expiresAt = order.getPaymentExpiresAt();
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("vnp_Version", properties.getVersion());
        parameters.put("vnp_Command", "pay");
        parameters.put("vnp_TmnCode", properties.getTmnCode());
        parameters.put("vnp_Amount", toVnPayAmount(order.getTotalPrice()));
        parameters.put("vnp_CurrCode", "VND");
        parameters.put("vnp_TxnRef", payment.getTransactionRef());
        parameters.put("vnp_OrderInfo", "Thanh toan don hang " + order.getOrderCode());
        parameters.put("vnp_OrderType", "other");
        parameters.put("vnp_Locale", "vn");
        parameters.put("vnp_ReturnUrl", properties.getReturnUrl());
        parameters.put("vnp_IpAddr", normalizeIp(clientIp));
        parameters.put("vnp_CreateDate", now.format(VNPAY_DATE));
        parameters.put("vnp_ExpireDate", expiresAt.format(VNPAY_DATE));

        String secureHash = signer.sign(parameters, properties.getHashSecret());
        String paymentUrl = properties.getPayUrl() + "?" + signer.buildQuery(parameters)
                + "&vnp_SecureHash=" + secureHash;
        return new PaymentUrlResponse(
                order.getId(),
                order.getOrderCode(),
                paymentUrl,
                expiresAt.toString()
        );
    }

    public VnPayIpnResponse processIpn(Map<String, String> parameters) {
        if (!properties.isConfigured() || !signer.verify(parameters, properties.getHashSecret())) {
            return new VnPayIpnResponse("97", "Invalid signature");
        }
        if (!properties.getTmnCode().equals(parameters.get("vnp_TmnCode"))) {
            return new VnPayIpnResponse("97", "Invalid terminal code");
        }

        String transactionRef = parameters.get("vnp_TxnRef");
        Payment candidate = paymentRepo.findByProviderAndTransactionRef(
                        PaymentProvider.VNPAY,
                        transactionRef
                )
                .orElse(null);
        if (candidate == null) {
            return new VnPayIpnResponse("01", "Order not found");
        }
        orderService.lockForPayment(candidate.getOrder().getId());
        Payment payment = paymentRepo.findForUpdate(PaymentProvider.VNPAY, transactionRef)
                .orElse(null);
        if (payment == null) {
            return new VnPayIpnResponse("01", "Order not found");
        }
        if (!amountMatches(payment.getAmount(), parameters.get("vnp_Amount"))) {
            return new VnPayIpnResponse("04", "Invalid amount");
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            if (payment.getStatus() == PaymentStatus.PAID) {
                orderService.markVnpayPaid(payment.getOrder());
            }
            return new VnPayIpnResponse("02", "Order already confirmed");
        }

        payment.setProviderTransactionNo(parameters.get("vnp_TransactionNo"));
        payment.setResponseCode(parameters.get("vnp_ResponseCode"));
        payment.setRawResponse(safeRawResponse(parameters));

        boolean successful = "00".equals(parameters.get("vnp_ResponseCode"))
                && "00".equals(parameters.get("vnp_TransactionStatus"));
        if (successful) {
            payment.setStatus(PaymentStatus.PAID);
            payment.setPaidAt(LocalDateTime.now());
            orderService.markVnpayPaid(payment.getOrder());
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            orderService.cancelFailedOnlinePayment(
                    payment.getOrder(),
                    "VNPay failed with response code " + parameters.get("vnp_ResponseCode")
            );
        }
        paymentRepo.save(payment);
        return new VnPayIpnResponse("00", "Confirm Success");
    }

    public PaymentResultResponse verifyReturn(Map<String, String> parameters) {
        if (!properties.isConfigured()) {
            return new PaymentResultResponse(false, false, null, null, "VNPay is not configured");
        }
        String transactionRef = parameters.get("vnp_TxnRef");
        boolean signatureValid = signer.verify(parameters, properties.getHashSecret())
                && properties.getTmnCode().equals(parameters.get("vnp_TmnCode"));
        if (!signatureValid) {
            return new PaymentResultResponse(
                    false,
                    false,
                    transactionRef,
                    parameters.get("vnp_ResponseCode"),
                    "Invalid VNPay signature"
            );
        }

        Payment candidate = paymentRepo.findByProviderAndTransactionRef(
                        PaymentProvider.VNPAY,
                        transactionRef
                )
                .orElse(null);
        if (candidate == null) {
            return new PaymentResultResponse(
                    true,
                    false,
                    transactionRef,
                    parameters.get("vnp_ResponseCode"),
                    "Order was not found"
            );
        }
        orderService.lockForPayment(candidate.getOrder().getId());
        Payment payment = paymentRepo.findForUpdate(PaymentProvider.VNPAY, transactionRef)
                .orElse(null);
        if (payment == null || !amountMatches(payment.getAmount(), parameters.get("vnp_Amount"))) {
            return new PaymentResultResponse(
                    true,
                    false,
                    transactionRef,
                    parameters.get("vnp_ResponseCode"),
                    "Payment amount is invalid"
            );
        }

        boolean gatewaySuccessful = "00".equals(parameters.get("vnp_ResponseCode"))
                && "00".equals(parameters.get("vnp_TransactionStatus"));
        if (payment.getStatus() == PaymentStatus.PENDING) {
            payment.setProviderTransactionNo(parameters.get("vnp_TransactionNo"));
            payment.setResponseCode(parameters.get("vnp_ResponseCode"));
            payment.setRawResponse(safeRawResponse(parameters));
            if (gatewaySuccessful) {
                payment.setStatus(PaymentStatus.PAID);
                payment.setPaidAt(LocalDateTime.now());
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                orderService.cancelFailedOnlinePayment(
                        payment.getOrder(),
                        "VNPay failed with response code " + parameters.get("vnp_ResponseCode")
                );
            }
            paymentRepo.save(payment);
        }

        if (gatewaySuccessful && payment.getStatus() == PaymentStatus.PAID) {
            orderService.markVnpayPaid(payment.getOrder());
        }

        boolean successful = gatewaySuccessful && payment.getStatus() == PaymentStatus.PAID;
        return new PaymentResultResponse(
                true,
                successful,
                transactionRef,
                parameters.get("vnp_ResponseCode"),
                successful
                        ? "Payment successful"
                        : "Payment was not successful or the order is no longer payable"
        );
    }

    private Payment createPayment(Order order) {
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setProvider(PaymentProvider.VNPAY);
        payment.setTransactionRef(order.getOrderCode());
        payment.setAmount(order.getTotalPrice());
        payment.setStatus(PaymentStatus.PENDING);
        return paymentRepo.save(payment);
    }

    private boolean amountMatches(BigDecimal amount, String vnpAmount) {
        try {
            return toVnPayAmount(amount).equals(vnpAmount);
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private String toVnPayAmount(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100)).toBigIntegerExact().toString();
    }

    private String safeRawResponse(Map<String, String> parameters) {
        return parameters.entrySet().stream()
                .filter(entry -> !entry.getKey().equals("vnp_SecureHash"))
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
    }

    private String normalizeIp(String clientIp) {
        return clientIp == null || clientIp.isBlank() ? "127.0.0.1" : clientIp;
    }

    private void requireConfigured() {
        if (!properties.isConfigured()) {
            throw BusinessException.badRequest(
                    "VNPAY_NOT_CONFIGURED",
                    "VNPay credentials and return URL have not been configured"
            );
        }
    }
}