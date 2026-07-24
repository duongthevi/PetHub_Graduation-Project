package com.dtvi.Pet.Ecommerce.exception;

import lombok.Getter;

@Getter
public class RefreshTokenException extends RuntimeException {

    private final String code;

    public RefreshTokenException(String code, String message) {
        super(message);
        this.code = code;
    }
}
