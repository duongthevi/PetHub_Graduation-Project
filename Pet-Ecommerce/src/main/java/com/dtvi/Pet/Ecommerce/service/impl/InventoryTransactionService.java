package com.dtvi.Pet.Ecommerce.service.impl;

import com.dtvi.Pet.Ecommerce.dto.inventory.InventoryTransactionResponse;
import com.dtvi.Pet.Ecommerce.entity.InventoryTransaction;
import com.dtvi.Pet.Ecommerce.entity.Order;
import com.dtvi.Pet.Ecommerce.entity.Product;
import com.dtvi.Pet.Ecommerce.entity.User;
import com.dtvi.Pet.Ecommerce.enums.InventoryTransactionType;
import com.dtvi.Pet.Ecommerce.repository.InventoryTransactionRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InventoryTransactionService {

    private final InventoryTransactionRepo transactionRepo;
    private final CurrentActorService currentActorService;

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(
            Product product,
            Order order,
            InventoryTransactionType type,
            int quantityChanged,
            String reference,
            String notes
    ) {
        if (quantityChanged == 0) {
            return;
        }

        User actor = currentActorService.getOrNull();

        InventoryTransaction transaction =
                new InventoryTransaction();

        transaction.setProduct(product);
        transaction.setOrder(order);
        transaction.setTransactionType(type);
        transaction.setQuantityChanged(quantityChanged);
        transaction.setBalanceAfter(product.getQuantity());
        transaction.setReferenceNumber(reference);
        transaction.setIdempotencyKey(
                order == null
                        ? "INVENTORY:" + type + ":PRODUCT:"
                            + product.getId() + ":" + UUID.randomUUID()
                        : "ORDER:" + order.getId() + ":" + type
                            + ":PRODUCT:" + product.getId()
        );
        transaction.setNotes(notes);
        transaction.setCreatedBy(actor);

        transactionRepo.save(transaction);
    }

    @Transactional(readOnly = true)
    public Page<InventoryTransactionResponse> getAll(
            int page,
            int size,
            Long productId
    ) {
        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                Math.max(1, Math.min(size, 100))
        );

        Page<InventoryTransaction> result =
                productId == null
                        ? transactionRepo.findAllByOrderByCreatedAtDesc(pageable)
                        : transactionRepo
                                .findByProductIdOrderByCreatedAtDesc(
                                        productId,
                                        pageable
                                );

        return result.map(InventoryTransactionResponse::from);
    }
}
