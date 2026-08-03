package com.dtvi.Pet.Ecommerce.service.impl;

import com.dtvi.Pet.Ecommerce.dto.ProductDto;
import com.dtvi.Pet.Ecommerce.entity.Product;
import com.dtvi.Pet.Ecommerce.exception.NotFoundException;
import com.dtvi.Pet.Ecommerce.mapper.EntityDtoMapper;
import com.dtvi.Pet.Ecommerce.repository.ProductRepo;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductCatalogService implements com.dtvi.Pet.Ecommerce.service.interf.ProductCatalogService {

    private static final Set<String> SORT_FIELDS =
            Set.of(
                    "id",
                    "createdAt",
                    "name",
                    "price",
                    "soldCount",
                    "averageRating"
            );

    private final ProductRepo productRepo;
    private final EntityDtoMapper mapper;

    @Override
    public Page<ProductDto> search(
            String query,
            Long categoryId,
            String petType,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size,
            String sortBy,
            String direction
    ) {
        return searchInternal(
                query,
                categoryId,
                petType,
                minPrice,
                maxPrice,
                page,
                size,
                sortBy,
                direction,
                false
        );
    }

    @Override
    public Page<ProductDto> searchAdmin(
            String query,
            int page,
            int size,
            String sortBy,
            String direction
    ) {
        return searchInternal(
                query,
                null,
                null,
                null,
                null,
                page,
                size,
                sortBy,
                direction,
                true
        );
    }

    @Override
    public ProductDto getAdminProduct(Long id) {
        Product product = productRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found"));

        return mapper.mapProductToDtoBasic(product);
    }

    private Page<ProductDto> searchInternal(
            String query,
            Long categoryId,
            String petType,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            int page,
            int size,
            String sortBy,
            String direction,
            boolean includeInactive
    ) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw com.dtvi.Pet.Ecommerce.exception.BusinessException.badRequest(
                    "INVALID_PRICE_RANGE",
                    "minPrice must not exceed maxPrice"
            );
        }
        String safeSort = SORT_FIELDS.contains(sortBy) ? sortBy : "createdAt";
        Sort.Direction safeDirection = "asc".equalsIgnoreCase(direction)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        PageRequest pageable = PageRequest.of(
                Math.max(0, page),
                Math.max(1, Math.min(size, 100)),
                Sort.by(safeDirection, safeSort)
        );

        Specification<Product> specification = (root, criteriaQuery, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (!includeInactive) {
                predicates.add(builder.isTrue(root.get("active")));
            }

            if (query != null && !query.isBlank()) {
                String pattern = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(builder.or(
                        builder.like(builder.lower(root.get("name")), pattern),
                        builder.like(
                                builder.lower(builder.coalesce(root.<String>get("description"), "")),
                                pattern
                        ),
                        builder.like(
                                builder.lower(builder.coalesce(root.<String>get("brand"), "")),
                                pattern
                        ),
                        builder.like(
                                builder.lower(builder.coalesce(root.<String>get("sku"), "")),
                                pattern
                        )
                ));
            }
            if (categoryId != null) {
                predicates.add(builder.equal(root.get("category").get("id"), categoryId));
            }
            if (petType != null && !petType.isBlank()) {
                predicates.add(builder.equal(
                        builder.lower(root.get("petType")),
                        petType.trim().toLowerCase(Locale.ROOT)
                ));
            }
            if (minPrice != null) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(builder.lessThanOrEqualTo(root.get("price"), maxPrice));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        return productRepo.findAll(specification, pageable).map(mapper::mapProductToDtoBasic);
    }
}
