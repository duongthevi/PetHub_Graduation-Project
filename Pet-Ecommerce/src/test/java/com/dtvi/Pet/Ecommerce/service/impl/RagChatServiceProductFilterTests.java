package com.dtvi.Pet.Ecommerce.service.impl;

import com.dtvi.Pet.Ecommerce.entity.Category;
import com.dtvi.Pet.Ecommerce.entity.Product;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagChatServiceProductFilterTests {

    private final RagChatService service =
            new RagChatService(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );

    @Test
    void unaccentedFoodForDogUnderBudgetFiltersPetFacetAndPrice() {
        Product dogFoodUnderBudget = product(
                "Hạt mềm cho chó nhỏ",
                "Thức ăn",
                "DOG",
                "150000",
                20
        );
        Product dogFoodOverBudget = product(
                "Pate cao cấp cho chó",
                "Thức ăn",
                "DOG",
                "250000",
                100
        );
        Product dogToyUnderBudget = product(
                "Bóng cao su cho chó",
                "Đồ chơi",
                "DOG",
                "90000",
                80
        );
        Product catFoodUnderBudget = product(
                "Pate cá ngừ cho mèo",
                "Thức ăn",
                "CAT",
                "120000",
                70
        );

        List<Product> result = service.filterAndRankProductCandidates(
                "goi y thuc an cho cho duoi 200k",
                List.of(
                        dogFoodOverBudget,
                        dogToyUnderBudget,
                        catFoodUnderBudget,
                        dogFoodUnderBudget
                )
        );

        assertEquals(
                List.of("Hạt mềm cho chó nhỏ"),
                result.stream().map(Product::getName).toList()
        );
    }

    @Test
    void toyForCatAboveBudgetFiltersPetFacetAndPrice() {
        Product catToy = product(
                "Cần câu lông vũ cho mèo",
                "Đồ chơi",
                "CAT",
                "80000",
                30
        );
        Product dogToy = product(
                "Bóng gặm cho chó",
                "Đồ chơi",
                "DOG",
                "90000",
                90
        );
        Product cheapCatToy = product(
                "Chuột giả cho mèo",
                "Đồ chơi",
                "CAT",
                "30000",
                100
        );
        Product catFood = product(
                "Hạt cá hồi cho mèo",
                "Thức ăn",
                "CAT",
                "120000",
                80
        );

        List<Product> result = service.filterAndRankProductCandidates(
                "Gợi ý đồ chơi cho mèo trên 50000",
                List.of(dogToy, cheapCatToy, catFood, catToy)
        );

        assertEquals(
                List.of("Cần câu lông vũ cho mèo"),
                result.stream().map(Product::getName).toList()
        );
    }

    @Test
    void dogProductsUnderBudgetNeverReturnOverBudgetProducts() {
        Product dogUnderBudget = product(
                "Sữa tắm cho chó",
                "Vệ sinh",
                "DOG",
                "180000",
                40
        );
        Product allPetsUnderBudget = product(
                "Khăn lau dùng chung",
                "Phụ kiện",
                "ALL",
                "90000",
                70
        );
        Product dogOverBudget = product(
                "Vòng cổ định vị cho chó",
                "Phụ kiện",
                "DOG",
                "350000",
                100
        );
        Product catUnderBudget = product(
                "Lược chải lông cho mèo",
                "Phụ kiện",
                "CAT",
                "100000",
                90
        );

        List<Product> result = service.filterAndRankProductCandidates(
                "Gợi ý sản phẩm cho chó dưới 200000",
                List.of(
                        dogOverBudget,
                        catUnderBudget,
                        dogUnderBudget,
                        allPetsUnderBudget
                )
        );

        assertEquals(
                List.of("Khăn lau dùng chung", "Sữa tắm cho chó"),
                result.stream().map(Product::getName).toList()
        );
        assertTrue(result.stream()
                .allMatch(product ->
                        product.getPrice()
                                .compareTo(new BigDecimal("200000")) <= 0
                ));
    }

    @Test
    void priceRangeSharesUnitWhenOnlyUpperBoundHasUnit() {
        Product inRange = product(
                "Pate gà cho chó",
                "Thức ăn",
                "DOG",
                "150000",
                40
        );
        Product underRange = product(
                "Snack nhỏ cho chó",
                "Thức ăn",
                "DOG",
                "90000",
                100
        );
        Product overRange = product(
                "Hạt nhập khẩu cho chó",
                "Thức ăn",
                "DOG",
                "250000",
                80
        );

        List<Product> result = service.filterAndRankProductCandidates(
                "Gợi ý thức ăn cho chó 100-200k",
                List.of(underRange, overRange, inRange)
        );

        assertEquals(
                List.of("Pate gà cho chó"),
                result.stream().map(Product::getName).toList()
        );
    }

    private Product product(
            String name,
            String categoryName,
            String petType,
            String price,
            long soldCount
    ) {
        Category category = new Category();
        category.setName(categoryName);

        Product product = new Product();
        product.setName(name);
        product.setDescription(name + " phù hợp cho thú cưng.");
        product.setSku(
                name.toLowerCase()
                        .replaceAll("[^a-z0-9]+", "-")
        );
        product.setBrand("PetHub");
        product.setCategory(category);
        product.setPetType(petType);
        product.setPrice(new BigDecimal(price));
        product.setQuantity(10);
        product.setActive(true);
        product.setSoldCount(soldCount);
        return product;
    }
}
