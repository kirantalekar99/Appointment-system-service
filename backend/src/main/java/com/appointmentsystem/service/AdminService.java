package com.appointmentsystem.service;

import com.appointmentsystem.dto.AdminAppointmentStatusRequest;
import com.appointmentsystem.dto.AdminAccountRequest;
import com.appointmentsystem.dto.AdminAccountResponse;
import com.appointmentsystem.dto.AdminDoctorRequest;
import com.appointmentsystem.dto.AdminSummaryResponse;
import com.appointmentsystem.dto.AppointmentResponse;
import com.appointmentsystem.dto.DoctorResponse;
import com.appointmentsystem.dto.NotificationRequest;
import com.appointmentsystem.dto.NotificationResponse;
import com.appointmentsystem.model.Appointment;
import com.appointmentsystem.model.AppointmentStatus;
import com.appointmentsystem.model.DoctorProfile;
import com.appointmentsystem.model.NotificationLog;
import com.appointmentsystem.model.Role;
import com.appointmentsystem.model.User;
import com.appointmentsystem.repository.AppointmentRepository;
import com.appointmentsystem.repository.DoctorProfileRepository;
import com.appointmentsystem.repository.NotificationLogRepository;
import com.appointmentsystem.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final AppointmentRepository appointmentRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final InputValidationService inputValidationService;

    public AdminService(UserRepository userRepository,
                        DoctorProfileRepository doctorProfileRepository,
                        AppointmentRepository appointmentRepository,
                        NotificationLogRepository notificationLogRepository,
                        BCryptPasswordEncoder passwordEncoder,
                        InputValidationService inputValidationService) {
        this.userRepository = userRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.appointmentRepository = appointmentRepository;
        this.notificationLogRepository = notificationLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.inputValidationService = inputValidationService;
    }

    public AdminSummaryResponse getSummary() {
        AdminSummaryResponse response = new AdminSummaryResponse();
        response.setTotalDoctors(userRepository.countByRole(Role.DOCTOR));
        response.setTotalPatients(userRepository.countByRole(Role.PATIENT));
        response.setTotalAppointments(appointmentRepository.count());
        response.setBookedAppointments(appointmentRepository.countByStatus(AppointmentStatus.BOOKED));
        response.setCompletedAppointments(appointmentRepository.countByStatus(AppointmentStatus.COMPLETED));
        response.setCancelledAppointments(appointmentRepository.countByStatus(AppointmentStatus.CANCELLED));
        return response;
    }

    public AdminAccountResponse getAdminAccount(Long adminUserId) {
        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin user not found"));
        if (admin.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admin can access this feature");
        }
        return mapAdmin(admin);
    }

    public List<DoctorResponse> getDoctors() {
        Map<String, DoctorResponse> uniqueDoctors = new LinkedHashMap<>();
        doctorProfileRepository.findAll().forEach((profile) -> {
            DoctorResponse response = mapDoctor(profile);
            uniqueDoctors.putIfAbsent((response.getEmail() == null ? String.valueOf(response.getId()) : response.getEmail().trim().toLowerCase()), response);
        });
        return uniqueDoctors.values().stream().toList();
    }

    public AdminAccountResponse updateAdminAccount(Long adminUserId, AdminAccountRequest request) {
        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin user not found"));
        if (admin.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admin can access this feature");
        }

        String email = inputValidationService.requireText(request.getEmail(), "Email");
        inputValidationService.validateEmail(email);

        userRepository.findByEmail(email)
                .filter(existing -> !existing.getId().equals(admin.getId()))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is already registered");
                });

        admin.setEmail(email);
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            inputValidationService.validatePassword(request.getPassword());
            admin.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        return mapAdmin(userRepository.save(admin));
    }

    public DoctorResponse addDoctor(AdminDoctorRequest request) {
        String email = inputValidationService.requireText(request.getEmail(), "Email");
        inputValidationService.validateEmail(email);
        inputValidationService.validatePhone(request.getPhone());
        if (userRepository.findByEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Doctor email is already registered");
        }

        User user = new User();
        user.setName(inputValidationService.requireText(request.getName(), "Name"));
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(defaultDoctorPassword(request.getPassword())));
        user.setPhone(request.getPhone().trim());
        user.setRole(Role.DOCTOR);
        User savedUser = userRepository.save(user);

        DoctorProfile profile = new DoctorProfile();
        profile.setUser(savedUser);
        applyDoctorProfile(profile, request);
        return mapDoctor(doctorProfileRepository.save(profile));
    }

    public DoctorResponse updateDoctor(Long doctorId, AdminDoctorRequest request) {
        DoctorProfile profile = doctorProfileRepository.findById(doctorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found"));

        String email = inputValidationService.requireText(request.getEmail(), "Email");
        inputValidationService.validateEmail(email);
        inputValidationService.validatePhone(request.getPhone());

        userRepository.findByEmail(email)
                .filter(existing -> !existing.getId().equals(profile.getUser().getId()))
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Doctor email is already registered");
                });

        User user = profile.getUser();
        user.setName(inputValidationService.requireText(request.getName(), "Name"));
        user.setEmail(email);
        user.setPhone(request.getPhone().trim());
        user.setRole(Role.DOCTOR);

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            inputValidationService.validatePassword(request.getPassword());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        userRepository.save(user);
        applyDoctorProfile(profile, request);
        return mapDoctor(doctorProfileRepository.save(profile));
    }

    public void deleteDoctor(Long doctorId) {
        DoctorProfile profile = doctorProfileRepository.findById(doctorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found"));
        List<Appointment> linkedAppointments = appointmentRepository.findByDoctorId(doctorId);
        if (!linkedAppointments.isEmpty()) {
            appointmentRepository.deleteAll(linkedAppointments);
        }

        doctorProfileRepository.delete(profile);
        userRepository.delete(profile.getUser());
    }

    public List<AppointmentResponse> getAllAppointments() {
        return appointmentRepository.findAllByOrderByAppointmentDateAsc()
                .stream()
                .map(this::mapAppointment)
                .toList();
    }

    public AppointmentResponse updateAppointmentStatus(Long appointmentId, AdminAppointmentStatusRequest request) {
        if (request.getStatus() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Appointment status is required");
        }

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
        appointment.setStatus(request.getStatus());
        return mapAppointment(appointmentRepository.save(appointment));
    }

    public AppointmentResponse cancelAppointment(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));
        appointment.setStatus(AppointmentStatus.CANCELLED);
        return mapAppointment(appointmentRepository.save(appointment));
    }

    public List<NotificationResponse> getNotifications() {
        return notificationLogRepository.findAll().stream()
                .sorted((left, right) -> right.getCreatedAt().compareTo(left.getCreatedAt()))
                .map(this::mapNotification)
                .toList();
    }

    public NotificationResponse createNotification(NotificationRequest request) {
        String channel = inputValidationService.requireText(request.getChannel(), "Channel").toUpperCase();
        String recipientGroup = inputValidationService.requireText(request.getRecipientGroup(), "Recipient group").toUpperCase();
        String message = inputValidationService.requireText(request.getMessage(), "Message");
        String recipient = request.getRecipient() == null || request.getRecipient().isBlank()
                ? recipientGroup
                : request.getRecipient().trim();

        NotificationLog log = new NotificationLog();
        log.setChannel(channel);
        log.setRecipientGroup(recipientGroup);
        log.setRecipient(recipient);
        log.setMessage(message);
        return mapNotification(notificationLogRepository.save(log));
    }

    public void deleteNotification(Long notificationId) {
        NotificationLog log = notificationLogRepository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        notificationLogRepository.delete(log);
    }

    private void applyDoctorProfile(DoctorProfile profile, AdminDoctorRequest request) {
        profile.setSpecialization(inputValidationService.requireText(request.getSpecialization(), "Specialization"));
        profile.setExperience(inputValidationService.requirePositive(request.getExperience(), "Experience"));
        profile.setBio(defaultDoctorBio(request.getBio(), request.getSpecialization()));
        profile.setAvailableDays(inputValidationService.requireText(request.getAvailableDays(), "Available days"));
        profile.setAvailableTime(inputValidationService.requireText(request.getAvailableTime(), "Available time"));
    }

    private String defaultDoctorPassword(String password) {
        if (password == null || password.isBlank()) {
            return "";
        }
        inputValidationService.validatePassword(password);
        return password;
    }

    private String defaultDoctorBio(String bio, String specialization) {
        if (bio == null || bio.isBlank()) {
            return "Experienced " + specialization.trim() + " available for patient consultation.";
        }
        return bio.trim();
    }

    private DoctorResponse mapDoctor(DoctorProfile profile) {
        DoctorResponse response = new DoctorResponse();
        response.setId(profile.getId());
        response.setUserId(profile.getUser().getId());
        response.setName(profile.getUser().getName());
        response.setEmail(profile.getUser().getEmail());
        response.setPhone(profile.getUser().getPhone());
        response.setSpecialization(profile.getSpecialization());
        response.setExperience(profile.getExperience());
        response.setBio(profile.getBio());
        response.setAvailableDays(profile.getAvailableDays());
        response.setAvailableTime(profile.getAvailableTime());
        return response;
    }

    private AppointmentResponse mapAppointment(Appointment appointment) {
        AppointmentResponse response = new AppointmentResponse();
        response.setId(appointment.getId());
        response.setDoctorId(appointment.getDoctor().getId());
        response.setDoctorName(appointment.getDoctor().getUser().getName());
        response.setSpecialization(appointment.getDoctor().getSpecialization());
        response.setPatientId(appointment.getPatient().getId());
        response.setPatientName(appointment.getPatient().getName());
        response.setPatientEmail(appointment.getPatient().getEmail());
        response.setAppointmentDate(appointment.getAppointmentDate());
        response.setTimeSlot(appointment.getTimeSlot());
        response.setSymptoms(appointment.getSymptoms());
        response.setStatus(appointment.getStatus());
        return response;
    }

    private NotificationResponse mapNotification(NotificationLog log) {
        NotificationResponse response = new NotificationResponse();
        response.setId(log.getId());
        response.setChannel(log.getChannel());
        response.setRecipientGroup(log.getRecipientGroup());
        response.setRecipient(log.getRecipient());
        response.setMessage(log.getMessage());
        response.setCreatedAt(log.getCreatedAt());
        return response;
    }

    private AdminAccountResponse mapAdmin(User user) {
        AdminAccountResponse response = new AdminAccountResponse();
        response.setId(user.getId());
        response.setName(user.getName());
        response.setEmail(user.getEmail());
        return response;
    }
}
