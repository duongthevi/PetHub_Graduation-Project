package com.dtvi.Pet.Ecommerce.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(
        name = "products",
        indexes = {
                @Index(name = "idx_products_category_active", columnList = "category_id, active"),
                @Index(name = "idx_products_name", columnList = "name")
        }
)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    private String imageUrl;

    @Column(unique = true, length = 80)
    private String sku;

    private String brand;

    @Column(name = "pet_type", length = 50)
    private String petType;

    private int quantity;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "sold_count", nullable = false)
    private long soldCount;

    @Column(name = "average_rating", precision = 3, scale = 2, nullable = false)
    private BigDecimal averageRating = BigDecimal.ZERO;
    
    @Column(name = "review_count", nullable = false)
    private long reviewCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "created_at")
    private final LocalDateTime createdAt = LocalDateTime.now();

    @Version
    private long version;

    // equals method
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;  // Using instanceof to allow comparison with subclasses
        Product product = (Product) o;
        return id != null && Objects.equals(id, product.id);
    }

    // hashCode method
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    // canEqual method
    public boolean canEqual(Object other) {
        return other instanceof Product;  // Only Product instances or subclasses can be compared
    }


}
