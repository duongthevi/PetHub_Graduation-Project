package com.dtvi.Pet.Ecommerce.service.interf;

import com.dtvi.Pet.Ecommerce.dto.ProductDto;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;

public interface ProductCatalogService {
    Page<ProductDto> search(
            String query,
            Long categoryId,
            String petType,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size,
            String sortBy,
            String direction
    );

    Page<ProductDto> searchAdmin(
            String query,
            int page,
            int size,
            String sortBy,
            String direction
    );

    ProductDto getAdminProduct(Long id);
}
