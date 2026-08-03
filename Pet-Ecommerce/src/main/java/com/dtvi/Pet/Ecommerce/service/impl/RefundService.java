package com.dtvi.Pet.Ecommerce.service.impl;

import com.dtvi.Pet.Ecommerce.dto.refund.CreateRefundRequest;
import com.dtvi.Pet.Ecommerce.dto.refund.RefundCommand;
import com.dtvi.Pet.Ecommerce.dto.refund.RefundResponse;
import com.dtvi.Pet.Ecommerce.entity.Order;
import com.dtvi.Pet.Ecommerce.entity.OrderItem;
import com.dtvi.Pet.Ecommerce.entity.Payment;
import com.dtvi.Pet.Ecommerce.entity.Product;
import com.dtvi.Pet.Ecommerce.entity.RefundRequest;
import com.dtvi.Pet.Ecommerce.entity.User;
import com.dtvi.Pet.Ecommerce.enums.InventoryTransactionType;
import com.dtvi.Pet.Ecommerce.enums.OrderStatus;
import com.dtvi.Pet.Ecommerce.enums.PaymentProvider;
import com.dtvi.Pet.Ecommerce.enums.PaymentStatus;
import com.dtvi.Pet.Ecommerce.enums.RefundStatus;
import com.dtvi.Pet.Ecommerce.exception.BusinessException;
import com.dtvi.Pet.Ecommerce.exception.NotFoundException;
import com.dtvi.Pet.Ecommerce.repository.OrderRepo;
import com.dtvi.Pet.Ecommerce.repository.PaymentRepo;
import com.dtvi.Pet.Ecommerce.repository.ProductRepo;
import com.dtvi.Pet.Ecommerce.repository.RefundRequestRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class RefundService {

    private final RefundRequestRepo refundRepo;
    private final OrderRepo orderRepo;
    private final PaymentRepo paymentRepo;
    private final ProductRepo productRepo;
    private final InventoryTransactionService inventoryService;

    @Value("${app.refund.return-days:7}")
    private long returnDays;

    public RefundResponse request(
            User user,
            Long orderId,
            CreateRefundRequest request
    ) {
        Order order = orderRepo.findForUpdateById(orderId)
                .orElseThrow(() ->
                        new NotFoundException("Order not found")
                );

        if (order.getUser() == null
                || !order.getUser().getId().equals(user.getId())) {
            throw BusinessException.forbidden(
                    "ORDER_FORBIDDEN",
                    "Order does not belong to this user"
            );
        }

        if (order.getStatus() != OrderStatus.DELIVERED
                || order.getPaymentStatus() != PaymentStatus.PAID) {
            throw BusinessException.conflict(
                    "REFUND_NOT_ALLOWED",
                    "Only delivered and paid orders can be refunded"
            );
        }

        if (order.getDeliveredAt() != null
                && order.getDeliveredAt().isBefore(
                        LocalDateTime.now().minusDays(returnDays)
                )) {
            throw BusinessException.conflict(
                    "RETURN_WINDOW_EXPIRED",
                    "The return period has expired"
            );
        }

        if (refundRepo.findByOrderId(orderId).isPresent()) {
            throw BusinessException.conflict(
                    "REFUND_ALREADY_EXISTS",
                    "This order already has a refund request"
            );
        }

        RefundRequest refund = new RefundRequest();
        refund.setOrder(order);
        refund.setRequestedBy(user);
        refund.setAmount(order.getTotalPrice());
        refund.setReason(request.reason().trim());
        refund.setStatus(RefundStatus.REQUESTED);
        refund.setRestockRequired(true);

        return RefundResponse.from(refundRepo.save(refund));
    }

    public RefundResponse createLatePaymentRefund(Order order) {
        return refundRepo.findByOrderId(order.getId())
                .map(RefundResponse::from)
                .orElseGet(() -> {
                    RefundRequest refund = new RefundRequest();
                    refund.setOrder(order);
                    refund.setRequestedBy(order.getUser());
                    refund.setAmount(order.getTotalPrice());
                    refund.setReason(
                            "VNPay payment was confirmed after the order was cancelled"
                    );
                    refund.setStatus(RefundStatus.REQUESTED);
                    refund.setRestockRequired(false);
                    refund.setAdminNote(
                            "Automatically created; verify and refund the late payment"
                    );
                    return RefundResponse.from(refundRepo.save(refund));
                });
    }

    @Transactional(readOnly = true)
    public Page<RefundResponse> getMyRefunds(
            User user,
            int page,
            int size
    ) {
        return refundRepo.findByRequestedByIdOrderByCreatedAtDesc(
                user.getId(),
                PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)))
        ).map(RefundResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<RefundResponse> getAllRefunds(
            int page,
            int size,
            RefundStatus status,
            String query
    ) {
        PageRequest pageable = PageRequest.of(
                Math.max(0, page),
                Math.max(1, Math.min(size, 100)),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        String keyword = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        Specification<RefundRequest> specification = (root, criteria, builder) -> {
            var predicate = builder.conjunction();
            if (status != null) {
                predicate = builder.and(predicate, builder.equal(root.get("status"), status));
            }
            if (!keyword.isBlank()) {
                String pattern = "%" + keyword + "%";
                predicate = builder.and(predicate, builder.or(
                        builder.like(builder.lower(root.get("order").get("orderCode")), pattern),
                        builder.like(builder.lower(root.get("requestedBy").get("name")), pattern),
                        builder.like(builder.lower(root.get("requestedBy").get("email")), pattern),
                        builder.like(builder.lower(root.get("reason")), pattern)
                ));
            }
            return predicate;
        };

        return refundRepo.findAll(specification, pageable).map(RefundResponse::from);
    }

    public RefundResponse approve(
            Long refundId,
            User admin,
            String note
    ) {
        RefundRequest refund = lock(refundId);

        requireStatus(refund, RefundStatus.REQUESTED);

        refund.setStatus(RefundStatus.APPROVED);
        refund.setReviewedBy(admin);
        refund.setReviewedAt(LocalDateTime.now());
        refund.setAdminNote(trimToNull(note));
        refund.setLastError(null);

        return RefundResponse.from(refundRepo.save(refund));
    }

    public RefundResponse reject(
            Long refundId,
            User admin,
            String note
    ) {
        RefundRequest refund = lock(refundId);

        if (refund.getStatus() != RefundStatus.REQUESTED
                && refund.getStatus() != RefundStatus.APPROVED) {
            throw BusinessException.conflict(
                    "INVALID_REFUND_TRANSITION",
                    "Refund cannot be rejected in current status"
            );
        }

        refund.setStatus(RefundStatus.REJECTED);
        refund.setReviewedBy(admin);
        refund.setReviewedAt(LocalDateTime.now());
        refund.setAdminNote(trimToNull(note));

        return RefundResponse.from(refundRepo.save(refund));
    }

    public RefundCommand beginProcessing(
            Long refundId,
            User admin
    ) {
        RefundRequest refund = lock(refundId);
        requireStatus(refund, RefundStatus.APPROVED);

        Order order = orderRepo.findForUpdateById(
                refund.getOrder().getId()
        ).orElseThrow(() ->
                new NotFoundException("Order not found")
        );

        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            throw BusinessException.conflict(
                    "PAYMENT_NOT_PAID",
                    "Only paid orders can be refunded"
            );
        }

        Payment payment = paymentRepo
                .findPaidByOrderIdForUpdate(order.getId())
                .stream()
                .findFirst()
                .orElseThrow(() ->
                        new NotFoundException(
                                "Paid payment was not found"
                        )
                );

        if (payment.getProvider() == PaymentProvider.VNPAY
                && (payment.getProviderTransactionNo() == null
                || payment.getProviderTransactionNo().isBlank())) {
            throw BusinessException.conflict(
                    "VNPAY_TRANSACTION_MISSING",
                    "VNPay transaction number is missing"
            );
        }

        if (refund.getProviderRequestId() == null) {
            refund.setProviderRequestId(
                    "REF-" + refund.getId() + "-"
                            + UUID.randomUUID()
                            .toString()
                            .substring(0, 8)
                            .toUpperCase(Locale.ROOT)
            );
        }

        refund.setStatus(RefundStatus.PROCESSING);
        refund.setProcessedBy(admin);
        refund.setProcessingStartedAt(LocalDateTime.now());
        refund.setLastError(null);
        refundRepo.save(refund);

        return new RefundCommand(
                refund.getId(),
                refund.getProviderRequestId(),
                order.getId(),
                order.getOrderCode(),
                payment.getTransactionRef(),
                payment.getProviderTransactionNo(),
                refund.getAmount(),
                payment.getProviderPaidAt()
        );
    }

    RefundResponse complete(
            Long refundId,
            String providerRefundReference,
            String providerResponseCode,
            String providerRawResponse
    ) {
        RefundRequest refund = lock(refundId);

        if (refund.getStatus() == RefundStatus.COMPLETED) {
            return RefundResponse.from(refund);
        }

        requireStatus(refund, RefundStatus.PROCESSING);

        Order order = orderRepo.findForUpdateById(
                refund.getOrder().getId()
        ).orElseThrow(() ->
                new NotFoundException("Order not found")
        );

        Payment payment = paymentRepo
                .findPaidByOrderIdForUpdate(order.getId())
                .stream()
                .findFirst()
                .orElseThrow(() ->
                        new NotFoundException("Paid payment not found")
                );

        List<OrderItem> items = order.getOrderItemList()
                .stream()
                .sorted(Comparator.comparing(
                        item -> item.getProduct().getId()
                ))
                .toList();

        if (refund.isRestockRequired()) {
            for (OrderItem item : items) {
                Product product = productRepo.findByIdForUpdate(
                        item.getProduct().getId()
                ).orElseThrow(() ->
                        new NotFoundException("Product not found")
                );

                product.setQuantity(
                        product.getQuantity() + item.getQuantity()
                );

                product.setSoldCount(
                        Math.max(
                                0,
                                product.getSoldCount()
                                        - item.getQuantity()
                        )
                );

                inventoryService.record(
                        product,
                        order,
                        InventoryTransactionType.RETURN_RESTOCK,
                        item.getQuantity(),
                        order.getOrderCode(),
                        "Inventory restored after confirmed refund"
                );
            }
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        order.setPaymentStatus(PaymentStatus.REFUNDED);
        order.setStatus(OrderStatus.RETURNED);
        order.setAdminMessage("Refund completed");
        order.getOrderItemList().forEach(
                item -> item.setStatus(OrderStatus.RETURNED)
        );

        refund.setStatus(RefundStatus.COMPLETED);
        refund.setProviderRefundRef(providerRefundReference);
        refund.setProviderResponseCode(providerResponseCode);
        refund.setProviderRawResponse(providerRawResponse);
        refund.setCompletedAt(LocalDateTime.now());
        refund.setLastError(null);

        return RefundResponse.from(refundRepo.save(refund));
    }

    void processingFailed(
            Long refundId,
            String error,
            String providerResponseCode,
            String providerRawResponse
    ) {
        RefundRequest refund = lock(refundId);

        if (refund.getStatus() == RefundStatus.PROCESSING) {
            refund.setStatus(RefundStatus.APPROVED);
            refund.setLastError(error);
            refund.setProviderResponseCode(providerResponseCode);
            refund.setProviderRawResponse(providerRawResponse);
            refundRepo.save(refund);
        }
    }

    void processingUncertain(
            Long refundId,
            String error
    ) {
        RefundRequest refund = lock(refundId);

        if (refund.getStatus() == RefundStatus.PROCESSING) {
            refund.setLastError(
                    trimToNull(error) == null
                            ? "Provider result is unknown"
                            : error
            );
            refundRepo.save(refund);
        }
    }

    private RefundRequest lock(Long refundId) {
        return refundRepo.findForUpdateById(refundId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Refund request not found"
                        )
                );
    }

    private void requireStatus(
            RefundRequest refund,
            RefundStatus expected
    ) {
        if (refund.getStatus() != expected) {
            throw BusinessException.conflict(
                    "INVALID_REFUND_TRANSITION",
                    "Expected " + expected
                            + " but current status is "
                            + refund.getStatus()
            );
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }
}
