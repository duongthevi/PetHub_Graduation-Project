package com.dtvi.Pet.Ecommerce.repository;

import com.dtvi.Pet.Ecommerce.entity.InventoryTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryTransactionRepo
        extends JpaRepository<InventoryTransaction, Long> {

    @EntityGraph(attributePaths = {
            "product",
            "order",
            "createdBy"
    })
    Page<InventoryTransaction> findAllByOrderByCreatedAtDesc(
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "product",
            "order",
            "createdBy"
    })
    Page<InventoryTransaction>
    findByProductIdOrderByCreatedAtDesc(
            Long productId,
            Pageable pageable
    );
}