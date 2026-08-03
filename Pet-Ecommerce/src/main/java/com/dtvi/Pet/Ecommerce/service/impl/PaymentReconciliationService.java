package com.dtvi.Pet.Ecommerce.service.impl;

import com.dtvi.Pet.Ecommerce.repository.PaymentRepo;
import com.dtvi.Pet.Ecommerce.service.impl.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentReconciliationService {

    private final PaymentRepo paymentRepo;
    private final OrderService orderService;


    /**
     * Điều hòa trạng thái thanh toán nội bộ
     * Tìm các thanh toán đã hoàn tất nhưng đơn hàng vẫn ở trạng thái chờ
     * Chạy mỗi 5 phút (mặc định)
     */
    @Scheduled(fixedDelayString = "${app.payment.reconciliation-ms:300000}")
    @Transactional
    public void reconcileInternalState() {
        try {
            log.info("Starting payment reconciliation...");
            
            paymentRepo.findPaidPaymentWithPendingOrder()
                    .forEach(payment -> {
                        try {
                            log.debug("Marking order {} as VNPay paid", payment.getOrder().getId());
                            orderService.markVnpayPaid(payment.getOrder());
                        } catch (Exception e) {
                            log.error("Failed to mark order {} as paid", payment.getOrder().getId(), e);
                        }
                    });
            
            log.info("Payment reconciliation completed successfully");
        } catch (Exception e) {
            log.error("Payment reconciliation failed", e);
        }
    }
}