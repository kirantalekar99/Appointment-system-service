package com.appointmentsystem.controller;

import com.appointmentsystem.dto.AuthRequest;
import com.appointmentsystem.dto.AuthResponse;
import com.appointmentsystem.dto.ForgotPasswordRequest;
import com.appointmentsystem.dto.ProfileResponse;
import com.appointmentsystem.dto.ProfileUpdateRequest;
import com.appointmentsystem.dto.RegisterOtpVerificationRequest;
import com.appointmentsystem.dto.RegisterRequest;
import com.appointmentsystem.service.AuthService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public String register(@RequestBody RegisterRequest request) {
        return authService.startRegistration(request);
    }

    @PostMapping("/register/verify-otp")
    public AuthResponse verifyRegisterOtp(@RequestBody RegisterOtpVerificationRequest request) {
        return authService.verifyRegistrationOtp(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody AuthRequest request) {
        return authService.login(request);
    }

    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestBody ForgotPasswordRequest request) {
        authService.resetPassword(request);
        return "Password updated successfully. Please login with your new password.";
    }

    @GetMapping("/profile/{userId}")
    public ProfileResponse getProfile(@PathVariable("userId") Long userId) {
        return authService.getProfile(userId);
    }

    @PutMapping("/profile/{userId}")
    public AuthResponse updateProfile(@PathVariable("userId") Long userId,
                                      @RequestBody ProfileUpdateRequest request) {
        return authService.updateProfile(userId, request);
    }

}


