package com.example.MFAAuthentication.service;

import com.example.MFAAuthentication.dto.*;
import com.example.MFAAuthentication.entity.User;
import com.example.MFAAuthentication.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private MfaService mfaService;

    public ApiResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            return new ApiResponse("ERROR", "Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            return new ApiResponse("ERROR", "Email already exists");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setMfaEnabled(false);

        userRepository.save(user);

        return new ApiResponse("SUCCESS", "User registered successfully", user.getId());
    }

    public LoginResponse login(LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new BadCredentialsException("User not found"));

            if (user.isMfaEnabled()) {
                return new LoginResponse("MFA_REQUIRED", 
                        "MFA verification required. Please enter the OTP from your authenticator app.", 
                        user.getId());
            } else {
          UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
                String token = jwtService.generateToken(userDetails);

                return new LoginResponse("SUCCESS", token, 
                        "Login successful", user.getId());
            }

        } catch (BadCredentialsException e) {
            return new LoginResponse("ERROR", "Invalid username or password");
        }
    }

    public LoginResponse verifyMfaAndLogin(MfaVerificationRequest request) {
        try {

            User user = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new BadCredentialsException("User not found"));

            if (user.getMfaSecret() == null || !user.isMfaEnabled()) {
                return new LoginResponse("ERROR", "MFA is not enabled for this user");
            }

            // Decode the secret before verifying
            String encoded = user.getMfaSecret();
            String decodedSecret = new String(
                    java.util.Base64.getDecoder().decode(encoded),
                    java.nio.charset.StandardCharsets.UTF_8
            );

            boolean isValid = mfaService.verifyCode(decodedSecret, request.getCode());

            if (isValid) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
                String token = jwtService.generateToken(userDetails);

                return new LoginResponse("SUCCESS", token, 
                        "MFA verification successful", user.getId());
            } else {
                return new LoginResponse("ERROR", "Invalid or expired OTP");
            }

        } catch (Exception e) {
            return new LoginResponse("ERROR", "MFA verification failed: " + e.getMessage());
        }
    }
}

