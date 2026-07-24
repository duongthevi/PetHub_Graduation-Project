package com.dtvi.Pet.Ecommerce.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "address")
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String street;
    private String ward;
    private String city;
    private String district;
    private String country;
    @Column(name = "recipient_name")
    private String recipientName;
    @Column(name = "recipient_phone", length = 20)
    private String recipientPhone;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(name = "created_at")
    private final LocalDateTime createdAt = LocalDateTime.now();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof Address)) return false;
        Address address = (Address) o;
        return id != null && Objects.equals(id, address.id);
    }


    public boolean canEqual(Object other) {
        return other instanceof Address;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
