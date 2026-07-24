package com.dtvi.Pet.Ecommerce.repository;

import com.dtvi.Pet.Ecommerce.entity.Payment;
import com.dtvi.Pet.Ecommerce.enums.PaymentProvider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface PaymentRepo extends JpaRepository<Payment, Long> {
    Optional<Payment> findByProviderAndTransactionRef(PaymentProvider provider, String transactionRef);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select payment
            from Payment payment
            join fetch payment.order
            where payment.provider = :provider and payment.transactionRef = :transactionRef
            """)
    Optional<Payment> findForUpdate(
            @Param("provider") PaymentProvider provider,
            @Param("transactionRef") String transactionRef
    );

    List<Payment> findByOrderIdOrderByCreatedAtDesc(Long orderId);
}
