package com.dtvi.Pet.Ecommerce.controller;

import com.dtvi.Pet.Ecommerce.dto.ApiResponse;
import com.dtvi.Pet.Ecommerce.dto.inventory.InventoryTransactionResponse;
import com.dtvi.Pet.Ecommerce.service.impl.InventoryTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/inventory-transactions")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminInventoryController {

    private final InventoryTransactionService inventoryService;

    @GetMapping
    public ApiResponse<Page<InventoryTransactionResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long productId
    ) {
        return ApiResponse.success(
                "Inventory transactions retrieved",
                inventoryService.getAll(page, size, productId)
        );
    }
}
