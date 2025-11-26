package com.example.MFAAuthentication.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class LoginResponse {
    private String status;
    private String token;
    private String message;
    private Long userId;

    public LoginResponse(String status, String message) {
        this.status = status;
        this.message = message;
    }

    public LoginResponse(String status, String message, Long userId) {
        this.status = status;
        this.message = message;
        this.userId = userId;
    }

    public LoginResponse(String status, String token, String message, Long userId) {
        this.status = status;
        this.token = token;
        this.message = message;
        this.userId = userId;
    }
}

