package com.dtvi.Pet.Ecommerce.service;

import com.dtvi.Pet.Ecommerce.config.VnPayProperties;
import com.dtvi.Pet.Ecommerce.dto.payment.PaymentResultResponse;
import com.dtvi.Pet.Ecommerce.entity.Order;
import com.dtvi.Pet.Ecommerce.entity.Payment;
import com.dtvi.Pet.Ecommerce.enums.PaymentProvider;
import com.dtvi.Pet.Ecommerce.enums.PaymentStatus;
import com.dtvi.Pet.Ecommerce.payment.VnPaySigner;
import com.dtvi.Pet.Ecommerce.repository.PaymentRepo;
import com.dtvi.Pet.Ecommerce.service.impl.OrderService;
import com.dtvi.Pet.Ecommerce.service.impl.VnPayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VnPayServiceTests {

    @Mock
    private PaymentRepo paymentRepo;

    @Mock
    private OrderService orderService;

    private VnPaySigner signer;
    private VnPayService service;

    @BeforeEach
    void setUp() {
        VnPayProperties properties = new VnPayProperties();
        properties.setTmnCode("TEST0001");
        properties.setHashSecret("demo-secret");
        properties.setReturnUrl("http://localhost:5173/payment/vnpay/return");
        signer = new VnPaySigner();
        service = new VnPayService(properties, signer, paymentRepo, orderService);
    }

    @Test
    void signedReturnFinalizesPendingPaymentForLocalDemo() {
        Order order = new Order();
        order.setId(10L);
        order.setOrderCode("PET-20260725-ABC12345");

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setProvider(PaymentProvider.VNPAY);
        payment.setTransactionRef(order.getOrderCode());
        payment.setAmount(new BigDecimal("125000"));
        payment.setStatus(PaymentStatus.PENDING);

        when(paymentRepo.findByProviderAndTransactionRef(
                PaymentProvider.VNPAY,
                order.getOrderCode()
        )).thenReturn(Optional.of(payment));
        when(paymentRepo.findForUpdate(
                PaymentProvider.VNPAY,
                order.getOrderCode()
        )).thenReturn(Optional.of(payment));
        when(orderService.lockForPayment(order.getId())).thenReturn(order);

        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("vnp_TmnCode", "TEST0001");
        parameters.put("vnp_TxnRef", order.getOrderCode());
        parameters.put("vnp_Amount", "12500000");
        parameters.put("vnp_ResponseCode", "00");
        parameters.put("vnp_TransactionStatus", "00");
        parameters.put("vnp_TransactionNo", "14422551");
        parameters.put("vnp_SecureHash", signer.sign(parameters, "demo-secret"));

        PaymentResultResponse result = service.verifyReturn(parameters);

        assertTrue(result.signatureValid());
        assertTrue(result.successful());
        assertEquals(PaymentStatus.PAID, payment.getStatus());
        verify(orderService).markVnpayPaid(order);
        verify(paymentRepo).save(payment);
    }
}
