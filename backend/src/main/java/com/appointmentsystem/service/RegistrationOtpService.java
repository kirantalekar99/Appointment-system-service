package com.appointmentsystem.service;

import com.appointmentsystem.dto.RegisterRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class RegistrationOtpService {

    private final Map<String, PendingRegistration> pendingRegistrations = new ConcurrentHashMap<>();
    private final Duration otpExpiry;

    public RegistrationOtpService(@Value("${app.otp.expiry-minutes:10}") long otpExpiryMinutes) {
        this.otpExpiry = Duration.ofMinutes(Math.max(1, otpExpiryMinutes));
    }

    public String createOtp(RegisterRequest request) {
        String otp = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        pendingRegistrations.put(normalizeEmail(request.getEmail()), new PendingRegistration(copyRequest(request), otp, Instant.now()));
        return otp;
    }

    public RegisterRequest verifyAndConsume(String email, String otp) {
        PendingRegistration pending = pendingRegistrations.get(normalizeEmail(email));
        if (pending == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please request a new OTP and try again");
        }

        if (Instant.now().isAfter(pending.createdAt().plus(otpExpiry))) {
            pendingRegistrations.remove(normalizeEmail(email));
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "OTP has expired. Please request a new OTP");
        }

        if (!pending.otp().equals(otp == null ? "" : otp.trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OTP. Please enter the correct OTP");
        }

        pendingRegistrations.remove(normalizeEmail(email));
        return pending.request();
    }

    private RegisterRequest copyRequest(RegisterRequest source) {
        RegisterRequest copy = new RegisterRequest();
        copy.setName(source.getName());
        copy.setEmail(source.getEmail());
        copy.setPassword(source.getPassword());
        copy.setPhone(source.getPhone());
        copy.setRole(source.getRole());
        copy.setSpecialization(source.getSpecialization());
        copy.setExperience(source.getExperience());
        copy.setBio(source.getBio());
        copy.setAvailableDays(source.getAvailableDays());
        return copy;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private record PendingRegistration(RegisterRequest request, String otp, Instant createdAt) {
    }
}
