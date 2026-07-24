package com.dtvi.Pet.Ecommerce.service.impl;

import com.dtvi.Pet.Ecommerce.dto.ProductDto;
import com.dtvi.Pet.Ecommerce.dto.Response;
import com.dtvi.Pet.Ecommerce.entity.Category;
import com.dtvi.Pet.Ecommerce.entity.Product;
import com.dtvi.Pet.Ecommerce.exception.CategoryOperationException;
import com.dtvi.Pet.Ecommerce.exception.NotFoundException;
import com.dtvi.Pet.Ecommerce.mapper.EntityDtoMapper;
import com.dtvi.Pet.Ecommerce.repository.CategoryRepo;
import com.dtvi.Pet.Ecommerce.repository.ProductRepo;
import com.dtvi.Pet.Ecommerce.repository.OrderItemRepo;
import com.dtvi.Pet.Ecommerce.service.interf.AwsS3Service;
import com.dtvi.Pet.Ecommerce.service.interf.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {

    private static final BigDecimal MAX_PRODUCT_PRICE = new BigDecimal("1000000000");

    private final ProductRepo productRepo;
    private final CategoryRepo categoryRepo;
    private final OrderItemRepo orderItemRepo;
    private final EntityDtoMapper entityDtoMapper;
    private final AwsS3Service awsS3Service;



    @Override
    public Response createProduct(Long categoryId, MultipartFile image, String name, String description,
                                  BigDecimal price, int quantity, String sku, String brand, String petType) {

        // Kiểm tra nếu tên sản phẩm đã tồn tại
        if (productRepo.existsByName(name)) {
            throw new CategoryOperationException("Product name already exists: " + name);
        }
        if (sku != null && !sku.isBlank() && productRepo.existsBySkuIgnoreCase(sku.trim())) {
            throw new CategoryOperationException("Product SKU already exists: " + sku);
        }

        if (categoryId == null || image == null || image.isEmpty() || name == null || name.isBlank()
                || description == null || description.isBlank() || price == null
                || price.signum() <= 0 || price.stripTrailingZeros().scale() > 0 || quantity <= 0) {
            throw new NotFoundException("All Fields are Required");
        }
        validatePrice(price);
        Category category = categoryRepo.findById(categoryId).orElseThrow(()-> new NotFoundException("Category not found"));
        String productImageUrl = awsS3Service.saveImageToS3(image);

        Product product = new Product();
        product.setCategory(category);
        product.setPrice(price);
        product.setName(name);
        product.setDescription(description);
        product.setImageUrl(productImageUrl);
        product.setQuantity(quantity);
        product.setSku(sku == null || sku.isBlank()
                ? "PET-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT)
                : sku.trim().toUpperCase(Locale.ROOT));
        product.setBrand(brand == null ? null : brand.trim());
        product.setPetType(petType == null ? null : petType.trim());
        product.setActive(true);

        productRepo.save(product);
        return Response.builder()
                .status(200)
                .message("Product successfully created")
                .build();
    }

    @Override
    public Response updateProduct(Long productId, Long categoryId, MultipartFile image, String name,
                                  String description, BigDecimal price, Integer quantity, String sku,
                                  String brand, String petType, Boolean active) {

        // Kiểm tra nếu tên sản phẩm đã tồn tại (trừ chính nó)
        if (productRepo.existsByNameAndIdNot(name, productId)) {
            throw new CategoryOperationException("Product name already exists: " + name);
        }
        if (sku != null && !sku.isBlank()
                && productRepo.existsBySkuIgnoreCaseAndIdNot(sku.trim(), productId)) {
            throw new CategoryOperationException("Product SKU already exists: " + sku);
        }

        Product product = productRepo.findById(productId).orElseThrow(()-> new NotFoundException("Product Not Found"));

        Category category = null;
        String productImageUrl = null;

        if(categoryId != null ){
            category = categoryRepo.findById(categoryId).orElseThrow(()-> new NotFoundException("Category not found"));
        }
        if (image != null && !image.isEmpty()){
            productImageUrl = awsS3Service.saveImageToS3(image);
        }

        if (category != null) product.setCategory(category);
        if (name != null) product.setName(name);
        if (price != null) {
            validatePrice(price);
            product.setPrice(price);
        }
        if (description != null) product.setDescription(description);
        if (productImageUrl != null) product.setImageUrl(productImageUrl);
        if (sku != null && !sku.isBlank()) product.setSku(sku.trim().toUpperCase(Locale.ROOT));
        if (brand != null) product.setBrand(brand.trim());
        if (petType != null) product.setPetType(petType.trim());
        if (active != null) product.setActive(active);
        if (quantity != null) {
            if (quantity < 0) {
                throw new CategoryOperationException("Product quantity cannot be negative");
            }
            product.setQuantity(quantity);
        }

        productRepo.save(product);
        return Response.builder()
                .status(200)
                .message("Product updated successfully")
                .build();

    }

    @Override
    public Response deleteProduct(Long productId) {
        Product product = productRepo.findById(productId).orElseThrow(()-> new NotFoundException("Product Not Found"));
        
        product.setActive(false);
        productRepo.save(product);

        return Response.builder()
                .status(200)
                .message("Product deactivated successfully")
                .build();
    }

    @Override
    public Response getProductById(Long productId) {
        Product product = productRepo.findById(productId).orElseThrow(()-> new NotFoundException("Product Not Found"));
        if (!product.isActive()) {
            throw new NotFoundException("Product Not Found");
        }
        ProductDto productDto = entityDtoMapper.mapProductToDtoBasic(product);

        return Response.builder()
                .status(200)
                .product(productDto)
                .build();
    }

    @Override
    public Response getAllProducts() {
        List<ProductDto> productList = productRepo.findAll(Sort.by(Sort.Direction.DESC, "id"))
                .stream()
                .filter(Product::isActive)
                .map(entityDtoMapper::mapProductToDtoBasic)
                .collect(Collectors.toList());

        return Response.builder()
                .status(200)
                .productList(productList)
                .build();

    }

    @Override
    public Response getProductsByCategory(Long categoryId) {
        List<Product> products = productRepo.findByCategoryId(categoryId);
        if(products.isEmpty()){
            throw new NotFoundException("No Products found for this category");
        }
        List<ProductDto> productDtoList = products.stream()
                .filter(Product::isActive)
                .map(entityDtoMapper::mapProductToDtoBasic)
                .collect(Collectors.toList());

        return Response.builder()
                .status(200)
                .productList(productDtoList)
                .build();

    }

    @Override
    public Response searchProduct(String searchValue) {
        List<Product> products = productRepo.findByNameContainingOrDescriptionContaining(searchValue, searchValue);

        if (products.isEmpty()){
            throw new NotFoundException("No Products Found");
        }
        List<ProductDto> productDtoList = products.stream()
                .filter(Product::isActive)
                .map(entityDtoMapper::mapProductToDtoBasic)
                .collect(Collectors.toList());


        return Response.builder()
                .status(200)
                .productList(productDtoList)
                .build();
    }

    private void validatePrice(BigDecimal price) {
        if (price.signum() <= 0) {
            throw new CategoryOperationException("Product price must be greater than zero");
        }
        if (price.stripTrailingZeros().scale() > 0) {
            throw new CategoryOperationException("Product price must be a whole VND amount");
        }
        if (price.compareTo(MAX_PRODUCT_PRICE) > 0) {
            throw new CategoryOperationException(
                    "Product price must not exceed 1,000,000,000 VND"
            );
        }
    }

}
