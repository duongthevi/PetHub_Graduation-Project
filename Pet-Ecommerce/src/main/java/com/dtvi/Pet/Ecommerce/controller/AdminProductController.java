package com.dtvi.Pet.Ecommerce.controller;

import com.dtvi.Pet.Ecommerce.dto.ApiResponse;
import com.dtvi.Pet.Ecommerce.dto.ProductDto;
import com.dtvi.Pet.Ecommerce.service.interf.ProductCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminProductController {

    private final ProductCatalogService productCatalogService;

    @GetMapping
    public ApiResponse<Page<ProductDto>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Boolean lowStock,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        return ApiResponse.success(
                "Products retrieved",
                productCatalogService.searchAdmin(
                        q,
                        lowStock,
                        page,
                        size,
                        sortBy,
                        direction
                )
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductDto> get(@PathVariable Long id) {
        return ApiResponse.success(
                "Product retrieved",
                productCatalogService.getAdminProduct(id)
        );
    }
}
