package com.dtvi.Pet.Ecommerce.repository;

import com.dtvi.Pet.Ecommerce.entity.RefundRequest;
import com.dtvi.Pet.Ecommerce.enums.RefundStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefundRequestRepo extends JpaRepository<RefundRequest, Long>,
        JpaSpecificationExecutor<RefundRequest> {

    Optional<RefundRequest> findByOrderId(Long orderId);

    @EntityGraph(attributePaths = {
        "order",
        "requestedBy",
        "reviewedBy"
    })
    Page<RefundRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {
        "order",
        "requestedBy",
        "reviewedBy"
    })
    Page<RefundRequest> findByRequestedByIdOrderByCreatedAtDesc(
        Long userId,
        Pageable pageable
    );

    @EntityGraph(attributePaths = {
        "order",
        "requestedBy",
        "reviewedBy"
    })
    Page<RefundRequest> findByStatusOrderByCreatedAtDesc(
        RefundStatus status,
        Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select refund
            from RefundRequest refund
            join fetch refund.order
            where refund.id = :id
            """)
    Optional<RefundRequest> findForUpdateById(@Param("id") Long id);
}
