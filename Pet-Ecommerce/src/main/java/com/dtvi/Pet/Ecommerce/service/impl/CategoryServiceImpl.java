package com.dtvi.Pet.Ecommerce.service.impl;

import com.dtvi.Pet.Ecommerce.dto.CategoryDto;
import com.dtvi.Pet.Ecommerce.dto.Response;
import com.dtvi.Pet.Ecommerce.entity.Category;
import com.dtvi.Pet.Ecommerce.exception.CategoryOperationException;
import com.dtvi.Pet.Ecommerce.exception.NotFoundException;
import com.dtvi.Pet.Ecommerce.mapper.EntityDtoMapper;
import com.dtvi.Pet.Ecommerce.repository.CategoryRepo;
import com.dtvi.Pet.Ecommerce.repository.ProductRepo;
import com.dtvi.Pet.Ecommerce.service.interf.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepo categoryRepo;
    private final ProductRepo productRepo;
    private final EntityDtoMapper entityDtoMapper;




    @Override
    public Response createCategory(CategoryDto categoryRequest) {

        if (categoryRequest.getName() == null || categoryRequest.getName().trim().isEmpty()) {
            throw new CategoryOperationException("Category name cannot be empty");
        }
        if (categoryRepo.existsByName(categoryRequest.getName().trim())) {
            throw new CategoryOperationException("Category name already exists: " + categoryRequest.getName());
        }

        Category category = new Category();
        category.setName(categoryRequest.getName().trim());
        categoryRepo.save(category);
        return Response.builder()
                .status(200)
                .message("Category created successfully")
                .build();
    }

    @Override
    public Response updateCategory(Long categoryId, CategoryDto categoryRequest) {

        Category category = categoryRepo.findById(categoryId).orElseThrow(()-> new NotFoundException("Category Not Found"));

        if (categoryRequest.getName() == null || categoryRequest.getName().trim().isEmpty()) {
            throw new CategoryOperationException("Category name cannot be empty");
        }
        if (categoryRepo.existsByNameAndIdNot(categoryRequest.getName().trim(), categoryId)) {
            throw new CategoryOperationException("Category name already exists: " + categoryRequest.getName());
        }

        category.setName(categoryRequest.getName().trim());
        categoryRepo.save(category);
        return Response.builder()
                .status(200)
                .message("category updated successfully")
                .build();
    }

    @Override
    public Response getAllCategories() {
        List<Category> categories = categoryRepo.findAll();
        List<CategoryDto> categoryDtoList =
            categoryRepo.findAll()
                    .stream()
                    .map(category -> {
                        CategoryDto dto =
                                entityDtoMapper
                                        .mapCategoryToDtoBasic(
                                                category
                                        );

                        dto.setProductCount(
                                productRepo
                                        .countByCategoryIdAndActiveTrue(
                                                category.getId()
                                        )
                        );

                        return dto;
                    })
                    .toList();

        return  Response.builder()
                .status(200)
                .categoryList(categoryDtoList)
                .build();
    }

    @Override
    public Response getCategoryById(Long categoryId) {
        Category category = categoryRepo.findById(categoryId).orElseThrow(()-> new NotFoundException("Category Not Found"));
        CategoryDto categoryDto = entityDtoMapper.mapCategoryToDtoBasic(category);
        return Response.builder()
                .status(200)
                .category(categoryDto)
                .build();
    }

    @Override
    public Response deleteCategory(Long categoryId) {
        Category category = categoryRepo.findById(categoryId).orElseThrow(()-> new NotFoundException("Category Not Found"));
        

        // Kiểm tra nếu danh mục đang chứa sản phẩm
        if (productRepo.existsByCategoryId(categoryId)) {
            throw new CategoryOperationException("Cannot delete category with existing products");
        }
        // Xóa danh mục
        categoryRepo.delete(category);
        return Response.builder()
                .status(200)
                .message("Category was deleted successfully")
                .build();
    }
}
