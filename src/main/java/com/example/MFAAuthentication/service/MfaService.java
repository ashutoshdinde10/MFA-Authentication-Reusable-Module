package com.example.MFAAuthentication.service;

import com.example.MFAAuthentication.dto.ApiResponse;
import com.example.MFAAuthentication.dto.MfaSetupResponse;
import com.example.MFAAuthentication.dto.MfaVerificationRequest;
import com.example.MFAAuthentication.entity.User;
import com.example.MFAAuthentication.repository.UserRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorConfig;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class MfaService {


    private final GoogleAuthenticator googleAuthenticator;

    @Autowired
    private UserRepository userRepository;

    @Value("${app.name:MFAAuthentication}")
    private String appName;

    public MfaService() {
        // Configure GoogleAuthenticator
        // Minimum allowed: 128 bits (library enforced for security)
        // 128 bits = ~26 Base32 characters
        GoogleAuthenticatorConfig config = new GoogleAuthenticatorConfig.GoogleAuthenticatorConfigBuilder()
                .setSecretBits(128)  // Minimum allowed by library (26 chars)
                .setWindowSize(3)    // Allow 3 time windows for clock drift tolerance
                .build();
        this.googleAuthenticator = new GoogleAuthenticator(config);
    }

    public String generateSecretKey() {
        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
        return key.getKey();
    }

    public String generateQrCodeUrl(String username, String secret, String issuer) {
        GoogleAuthenticatorKey key = new GoogleAuthenticatorKey.Builder(secret).build();
        return GoogleAuthenticatorQRGenerator.getOtpAuthURL(issuer, username, key);
    }

    public String generateQrCodeImageBase64(String qrCodeUrl) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(qrCodeUrl, BarcodeFormat.QR_CODE, 400, 400);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
        
        byte[] imageBytes = outputStream.toByteArray();
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
    }

    public boolean verifyCode(String secret, int code) {
        return googleAuthenticator.authorize(secret, code);
    }

    public boolean verifyCode(String secret, String code) {
        try {
            int codeInt = Integer.parseInt(code);
            return verifyCode(secret, codeInt);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Transactional
    public MfaSetupResponse setupMfa(Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String secret = generateSecretKey();
            String otpAuthUrl = generateQrCodeUrl(user.getUsername(), secret, appName);
            String qrCodeImage = generateQrCodeImageBase64(otpAuthUrl);
            String encodedSecret =  Base64.getEncoder().encodeToString(secret.getBytes(
                   StandardCharsets.UTF_8
           ));
            user.setMfaSecret(encodedSecret);
            userRepository.save(user);

            return new MfaSetupResponse(
                    secret,
                    qrCodeImage,
                    otpAuthUrl,
                    "Scan the QR code with your authenticator app or manually enter the secret key"
            );

        } catch (WriterException | IOException e) {
            return new MfaSetupResponse(null, null, null, 
                    "Failed to generate QR code: " + e.getMessage());
        } catch (Exception e) {
            return new MfaSetupResponse(null, null, null, 
                    "MFA setup failed: " + e.getMessage());
        }
    }

    @Transactional
    public ApiResponse enableMfa(MfaVerificationRequest request) {
        try {
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (user.getMfaSecret() == null) {
                return new ApiResponse("ERROR", "MFA setup not completed. Please setup MFA first.");
            }

            // Verify the code
            String encoded = user.getMfaSecret();
            String decodedSecret = new String(
                    Base64.getDecoder().decode(encoded),
                    StandardCharsets.UTF_8
            );

            boolean isValid = verifyCode(decodedSecret, request.getCode());

            if (isValid) {
                user.setMfaEnabled(true);
                userRepository.save(user);

                return new ApiResponse("SUCCESS", "MFA enabled successfully");
            } else {
                return new ApiResponse("ERROR", "Invalid OTP code. Please try again.");
            }

        } catch (Exception e) {
            return new ApiResponse("ERROR", "Failed to enable MFA: " + e.getMessage());
        }
    }

    @Transactional
    public ApiResponse disableMfa(MfaVerificationRequest request) {
        try {
            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (!user.isMfaEnabled()) {
                return new ApiResponse("ERROR", "MFA is not enabled for this user");
            }

            // Decode the secret before verifying
            String encoded = user.getMfaSecret();
            String decodedSecret = new String(
                    Base64.getDecoder().decode(encoded),
                    StandardCharsets.UTF_8
            );

            boolean isValid = verifyCode(decodedSecret, request.getCode());

            if (isValid) {
                user.setMfaEnabled(false);
                user.setMfaSecret(null);
                userRepository.save(user);

                return new ApiResponse("SUCCESS", "MFA disabled successfully");
            } else {
                return new ApiResponse("ERROR", "Invalid OTP code. Please try again.");
            }

        } catch (Exception e) {
            return new ApiResponse("ERROR", "Failed to disable MFA: " + e.getMessage());
        }
    }

    public ApiResponse getMfaStatus(Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            return new ApiResponse("SUCCESS", "MFA status retrieved", user.isMfaEnabled());

        } catch (Exception e) {
            return new ApiResponse("ERROR", "Failed to get MFA status: " + e.getMessage());
        }
    }
}

