package com.example.MFAAuthentication.controller;

import com.example.MFAAuthentication.dto.ApiResponse;
import com.example.MFAAuthentication.dto.MfaSetupResponse;
import com.example.MFAAuthentication.dto.MfaVerificationRequest;
import com.example.MFAAuthentication.service.MfaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mfa")
@CrossOrigin(origins = "*")
public class MfaController {

    @Autowired
    private MfaService mfaService;

    @PostMapping("/setup")
    public ResponseEntity<MfaSetupResponse> setupMfa(@RequestParam Long userId) {
        MfaSetupResponse response = mfaService.setupMfa(userId);
        
        if (response.getSecret() == null) {
            return ResponseEntity.badRequest().body(response);
        }
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/enable")
    public ResponseEntity<ApiResponse> enableMfa(@RequestBody MfaVerificationRequest request) {
        ApiResponse response = mfaService.enableMfa(request);
        
        if ("ERROR".equals(response.getStatus())) {
            return ResponseEntity.badRequest().body(response);
        }
        
        return ResponseEntity.ok(response);
    }

    @PostMapping("/disable")
    public ResponseEntity<ApiResponse> disableMfa(@RequestBody MfaVerificationRequest request) {
        ApiResponse response = mfaService.disableMfa(request);
        
        if ("ERROR".equals(response.getStatus())) {
            return ResponseEntity.badRequest().body(response);
        }
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse> getMfaStatus(@RequestParam Long userId) {
        ApiResponse response = mfaService.getMfaStatus(userId);
        
        if ("ERROR".equals(response.getStatus())) {
            return ResponseEntity.badRequest().body(response);
        }
        
        return ResponseEntity.ok(response);
    }
}
