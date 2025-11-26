package com.example.MFAAuthentication.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MfaSetupResponse {
    private String secret;
    private String qrCodeUrl;
    private String otpAuthUrl;
    private String message;

    public MfaSetupResponse(String secret, String qrCodeUrl, String message) {
        this.secret = secret;
        this.qrCodeUrl = qrCodeUrl;
        this.message = message;
    }
}

