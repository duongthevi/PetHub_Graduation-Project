package com.dtvi.Pet.Ecommerce.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class AddressDto {

    private Long id;

    @NotBlank @Size(max = 255)
    private String street;
    @NotBlank @Size(max = 100)
    private String ward;
    @NotBlank @Size(max = 100)
    private String city;
    @NotBlank @Size(max = 100)
    private String district;
    @NotBlank @Size(max = 100)
    private String country;
    @NotBlank @Size(max = 100)
    private String recipientName;
    @NotBlank @Pattern(regexp = "^[0-9+() .-]{8,20}$")
    private String recipientPhone;

    private UserDto user;

    private LocalDateTime createdAt;
}
