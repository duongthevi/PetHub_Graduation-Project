package com.dtvi.Pet.Ecommerce.entity;

import com.dtvi.Pet.Ecommerce.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(
        name = "order_items",
        indexes = {
                @Index(name = "idx_order_items_order", columnList = "order_id"),
                @Index(name = "idx_order_items_product", columnList = "product_id"),
                @Index(name = "idx_order_items_user_status", columnList = "user_id, status")
        }
)
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int quantity;
    @Column(name = "unit_price", precision = 15, scale = 2)
    private BigDecimal unitPrice;
    @Column(name = "line_total", precision = 15, scale = 2)
    private BigDecimal lineTotal;
    @Column(name = "product_name")
    private String productName;
    @Column(name = "product_sku", length = 80)
    private String productSku;
    @Column(name = "product_image_url", length = 1000)
    private String productImageUrl;
    private BigDecimal price;
    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "admin_message")
    private String adminMessage;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }

    // Getter và Setter
    public String getAdminMessage() {
        return adminMessage;
    }

    public void setAdminMessage(String adminMessage) {
        this.adminMessage = adminMessage;
    }


    // equals method
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;  // Make sure it's the same class or subclass
        OrderItem that = (OrderItem) o;
        return id != null && Objects.equals(id, that.id);
    }

    // hashCode method
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    // canEqual method
    public boolean canEqual(Object other) {
        return other instanceof OrderItem;  // Only OrderItem instances can be compared
    }
}
