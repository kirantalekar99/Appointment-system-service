package com.appointmentsystem.service;

import com.appointmentsystem.dto.AuthRequest;
import com.appointmentsystem.dto.AuthResponse;
import com.appointmentsystem.dto.ForgotPasswordRequest;
import com.appointmentsystem.dto.ProfileResponse;
import com.appointmentsystem.dto.ProfileUpdateRequest;
import com.appointmentsystem.dto.RegisterOtpVerificationRequest;
import com.appointmentsystem.dto.RegisterRequest;
import com.appointmentsystem.model.DoctorProfile;
import com.appointmentsystem.model.Role;
import com.appointmentsystem.model.User;
import com.appointmentsystem.repository.DoctorProfileRepository;
import com.appointmentsystem.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final InputValidationService inputValidationService;
    private final RegistrationOtpService registrationOtpService;
    private final EmailService emailService;

    public AuthService(UserRepository userRepository,
                       DoctorProfileRepository doctorProfileRepository,
                       BCryptPasswordEncoder passwordEncoder,
                       InputValidationService inputValidationService,
                       RegistrationOtpService registrationOtpService,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.inputValidationService = inputValidationService;
        this.registrationOtpService = registrationOtpService;
        this.emailService = emailService;
    }

    public String startRegistration(RegisterRequest request) {
        validateRegistrationRequest(request);
        String otp = registrationOtpService.createOtp(request);
        emailService.sendOtpEmail(request.getEmail().trim(), request.getName().trim(), otp);
        return "OTP sent successfully to your email. Please enter the OTP to complete registration.";
    }

    public AuthResponse verifyRegistrationOtp(RegisterOtpVerificationRequest request) {
        String email = inputValidationService.requireText(request.getEmail(), "Email");
        inputValidationService.validateEmail(email);
        String otp = inputValidationService.requireText(request.getOtp(), "OTP");

        RegisterRequest savedRequest = registrationOtpService.verifyAndConsume(email, otp);
        AuthResponse response = completeRegistration(savedRequest);
        try {
            emailService.sendWelcomeEmail(response.getEmail(), response.getName(), savedRequest.getPassword());
        } catch (Exception ignored) {
        }
        return response;
    }

    private AuthResponse completeRegistration(RegisterRequest request) {
        validateRegistrationRequest(request);

        String email = request.getEmail().trim();
        String phone = request.getPhone().trim();

        User user = new User();
        user.setName(inputValidationService.requireText(request.getName(), "Name"));
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(phone);
        user.setRole(request.getRole());
        User savedUser = userRepository.save(user);

        Long doctorProfileId = null;
        if (request.getRole() == Role.DOCTOR) {
            DoctorProfile doctorProfile = new DoctorProfile();
            doctorProfile.setUser(savedUser);
            doctorProfile.setSpecialization(defaultValue(request.getSpecialization(), "General Physician"));
            doctorProfile.setExperience(request.getExperience() == null ? 1 : request.getExperience());
            doctorProfile.setBio(defaultValue(request.getBio(), "Experienced doctor available for regular consultation."));
            doctorProfile.setAvailableDays(defaultValue(request.getAvailableDays(), "Mon, Tue, Wed, Thu, Fri"));
            doctorProfile.setAvailableTime("10:00 AM - 4:00 PM");
            doctorProfileId = doctorProfileRepository.save(doctorProfile).getId();
        }

        return toAuthResponse(savedUser, doctorProfileId);
    }

    private void validateRegistrationRequest(RegisterRequest request) {
        if (request.getRole() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Role is required");
        }

        if (request.getRole() == Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Admin accounts can only be created by the system");
        }

        String email = inputValidationService.requireText(request.getEmail(), "Email");
        inputValidationService.validateEmail(email);
        String phone = inputValidationService.requireText(request.getPhone(), "Phone");
        inputValidationService.validatePhone(phone);
        inputValidationService.validatePassword(request.getPassword());

        if (userRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is already registered");
        }

        if (userRepository.findFirstByPhone(phone.trim()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone number is already registered");
        }
    }

    public AuthResponse login(AuthRequest request) {
        String identifier = request.getIdentifier();
        if (identifier == null || identifier.isBlank()) {
            identifier = request.getEmail();
        }

        String loginValue = inputValidationService.requireText(identifier, "Email or mobile number");
        User user = findUserByIdentifier(loginValue)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email/mobile number or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email/mobile number or password");
        }

        Long doctorProfileId = doctorProfileRepository.findByUserId(user.getId())
                .map(DoctorProfile::getId)
                .orElse(null);

        return toAuthResponse(user, doctorProfileId);
    }

    public void resetPassword(ForgotPasswordRequest request) {
        String identifier = inputValidationService.requireText(request.getIdentifier(), "Registered email or mobile number");
        inputValidationService.validatePassword(request.getNewPassword());

        User user = findUserByIdentifier(identifier)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No account found with this email or mobile number"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    public ProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        return mapProfile(user, doctorProfileRepository.findByUserId(userId).orElse(null));
    }

    @Transactional
    public AuthResponse updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        String email = inputValidationService.requireText(request.getEmail(), "Email");
        inputValidationService.validateEmail(email);
        String phone = inputValidationService.requireText(request.getPhone(), "Phone");
        inputValidationService.validatePhone(phone);

        userRepository.findByEmail(email)
                .filter(existing -> !existing.getId().equals(userId))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is already registered");
                });

        userRepository.findFirstByPhone(phone)
                .filter(existing -> !existing.getId().equals(userId))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Phone number is already registered");
                });

        user.setName(inputValidationService.requireText(request.getName(), "Name"));
        user.setEmail(email);
        user.setPhone(phone);

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            inputValidationService.validatePassword(request.getPassword());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        User savedUser = userRepository.save(user);
        Long doctorProfileId = null;
        if (savedUser.getRole() == Role.DOCTOR) {
            DoctorProfile doctorProfile = doctorProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor profile not found"));
            doctorProfile.setSpecialization(inputValidationService.requireText(request.getSpecialization(), "Specialization"));
            doctorProfile.setExperience(inputValidationService.requirePositive(request.getExperience(), "Experience"));
            doctorProfile.setBio(inputValidationService.requireText(request.getBio(), "Bio"));
            doctorProfile.setAvailableDays(inputValidationService.requireText(request.getAvailableDays(), "Available days"));
            doctorProfile.setAvailableTime(inputValidationService.requireText(request.getAvailableTime(), "Available time"));
            doctorProfileId = doctorProfileRepository.save(doctorProfile).getId();
        }

        return toAuthResponse(savedUser, doctorProfileId != null ? doctorProfileId : doctorProfileRepository.findByUserId(userId).map(DoctorProfile::getId).orElse(null));
    }

    private AuthResponse toAuthResponse(User user, Long doctorProfileId) {
        AuthResponse response = new AuthResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setRole(user.getRole());
        response.setDoctorProfileId(doctorProfileId);
        response.setToken(UUID.randomUUID().toString());
        return response;
    }

    private String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private java.util.Optional<User> findUserByIdentifier(String identifier) {
        String trimmedIdentifier = identifier.trim();
        if (trimmedIdentifier.matches("^\\d{10}$")) {
            return userRepository.findFirstByPhone(trimmedIdentifier);
        }

        inputValidationService.validateEmail(trimmedIdentifier);
        return userRepository.findByEmail(trimmedIdentifier);
    }

    private ProfileResponse mapProfile(User user, DoctorProfile doctorProfile) {
        ProfileResponse response = new ProfileResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setRole(user.getRole());
        if (doctorProfile != null) {
            response.setDoctorProfileId(doctorProfile.getId());
            response.setSpecialization(doctorProfile.getSpecialization());
            response.setExperience(doctorProfile.getExperience());
            response.setBio(doctorProfile.getBio());
            response.setAvailableDays(doctorProfile.getAvailableDays());
            response.setAvailableTime(doctorProfile.getAvailableTime());
        }
        return response;
    }
}