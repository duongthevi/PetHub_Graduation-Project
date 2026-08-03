package com.dtvi.Pet.Ecommerce.repository;

import com.dtvi.Pet.Ecommerce.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.List;
import java.time.LocalDateTime;
import com.dtvi.Pet.Ecommerce.enums.OrderStatus;

public interface OrderRepo extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    @EntityGraph(attributePaths = {"orderItemList", "orderItemList.product", "user", "coupon"})
    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"orderItemList", "orderItemList.product", "user", "coupon"})
    Optional<Order> findWithDetailsById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select orders from Order orders where orders.id = :id")
    Optional<Order> findForUpdateById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"orderItemList", "orderItemList.product", "user", "coupon"})
    Optional<Order> findWithDetailsByOrderCode(String orderCode);

    Optional<Order> findByOrderCode(String orderCode);

    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<Order> findByStatusAndPaymentExpiresAtBefore(OrderStatus status, LocalDateTime expiresAt);

    long countByStatus(OrderStatus status);

    @Query("""
            select coalesce(sum(o.totalPrice), 0)
            from Order o
            where o.paymentStatus = com.dtvi.Pet.Ecommerce.enums.PaymentStatus.PAID
            """)
    BigDecimal calculateTotalPaidRevenue();

    @Query("""
            select coalesce(sum(o.totalPrice), 0)
            from Order o
            where month(o.createdAt) = :month
              and year(o.createdAt) = :year
              and o.paymentStatus = com.dtvi.Pet.Ecommerce.enums.PaymentStatus.PAID
            """)
    BigDecimal calculateMonthlyPaidRevenue(@Param("month") int month, @Param("year") int year);

    @Query("""
            select count(o)
            from Order o
            where month(o.createdAt) = :month
              and year(o.createdAt) = :year
              and o.status <> com.dtvi.Pet.Ecommerce.enums.OrderStatus.CANCELLED
            """)
    long countMonthlyValidOrders(@Param("month") int month, @Param("year") int year);

    Optional<Order> findByUserIdAndIdempotencyKey(
                Long userId,
                String idempotencyKey
        );
}
