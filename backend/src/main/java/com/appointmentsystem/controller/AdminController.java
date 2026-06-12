package com.appointmentsystem.controller;

import com.appointmentsystem.dto.AdminAppointmentStatusRequest;
import com.appointmentsystem.dto.AdminAccountRequest;
import com.appointmentsystem.dto.AdminAccountResponse;
import com.appointmentsystem.dto.AdminDoctorRequest;
import com.appointmentsystem.dto.AdminSummaryResponse;
import com.appointmentsystem.dto.AppointmentResponse;
import com.appointmentsystem.dto.DoctorResponse;
import com.appointmentsystem.dto.NotificationRequest;
import com.appointmentsystem.dto.NotificationResponse;
import com.appointmentsystem.service.AdminAccessService;
import com.appointmentsystem.service.AdminService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    private final AdminService adminService;
    private final AdminAccessService adminAccessService;

    public AdminController(AdminService adminService, AdminAccessService adminAccessService) {
        this.adminService = adminService;
        this.adminAccessService = adminAccessService;
    }

    @GetMapping("/summary")
    public AdminSummaryResponse getSummary(@RequestHeader("X-User-Id") Long userId,
                                           @RequestHeader("X-User-Role") String role) {
        adminAccessService.requireAdmin(userId, role);
        return adminService.getSummary();
    }

    @GetMapping("/account")
    public AdminAccountResponse getAdminAccount(@RequestHeader("X-User-Id") Long userId,
                                                @RequestHeader("X-User-Role") String role) {
        adminAccessService.requireAdmin(userId, role);
        return adminService.getAdminAccount(userId);
    }

    @GetMapping("/doctors")
    public List<DoctorResponse> getDoctors(@RequestHeader("X-User-Id") Long userId,
                                           @RequestHeader("X-User-Role") String role) {
        adminAccessService.requireAdmin(userId, role);
        return adminService.getDoctors();
    }

    @PostMapping("/doctors")
    public DoctorResponse addDoctor(@RequestHeader("X-User-Id") Long userId,
                                    @RequestHeader("X-User-Role") String role,
                                    @RequestBody AdminDoctorRequest request) {
        adminAccessService.requireAdmin(userId, role);
        return adminService.addDoctor(request);
    }

    @PutMapping("/account")
    public AdminAccountResponse updateAdminAccount(@RequestHeader("X-User-Id") Long userId,
                                                   @RequestHeader("X-User-Role") String role,
                                                   @RequestBody AdminAccountRequest request) {
        adminAccessService.requireAdmin(userId, role);
        return adminService.updateAdminAccount(userId, request);
    }

    @PutMapping("/doctors/{doctorId}")
    public DoctorResponse updateDoctor(@RequestHeader("X-User-Id") Long userId,
                                       @RequestHeader("X-User-Role") String role,
                                       @PathVariable("doctorId") Long doctorId,
                                       @RequestBody AdminDoctorRequest request) {
        adminAccessService.requireAdmin(userId, role);
        return adminService.updateDoctor(doctorId, request);
    }

    @DeleteMapping("/doctors/{doctorId}")
    public void deleteDoctor(@RequestHeader("X-User-Id") Long userId,
                             @RequestHeader("X-User-Role") String role,
                             @PathVariable("doctorId") Long doctorId) {
        adminAccessService.requireAdmin(userId, role);
        adminService.deleteDoctor(doctorId);
    }

    @GetMapping("/appointments")
    public List<AppointmentResponse> getAppointments(@RequestHeader("X-User-Id") Long userId,
                                                     @RequestHeader("X-User-Role") String role) {
        adminAccessService.requireAdmin(userId, role);
        return adminService.getAllAppointments();
    }

    @PatchMapping("/appointments/{appointmentId}/status")
    public AppointmentResponse updateAppointmentStatus(@RequestHeader("X-User-Id") Long userId,
                                                       @RequestHeader("X-User-Role") String role,
                                                       @PathVariable("appointmentId") Long appointmentId,
                                                       @RequestBody AdminAppointmentStatusRequest request) {
        adminAccessService.requireAdmin(userId, role);
        return adminService.updateAppointmentStatus(appointmentId, request);
    }

    @PatchMapping("/appointments/{appointmentId}/cancel")
    public AppointmentResponse cancelAppointment(@RequestHeader("X-User-Id") Long userId,
                                                 @RequestHeader("X-User-Role") String role,
                                                 @PathVariable("appointmentId") Long appointmentId) {
        adminAccessService.requireAdmin(userId, role);
        return adminService.cancelAppointment(appointmentId);
    }

    @GetMapping("/notifications")
    public List<NotificationResponse> getNotifications(@RequestHeader("X-User-Id") Long userId,
                                                       @RequestHeader("X-User-Role") String role) {
        adminAccessService.requireAdmin(userId, role);
        return adminService.getNotifications();
    }

    @PostMapping("/notifications")
    public NotificationResponse createNotification(@RequestHeader("X-User-Id") Long userId,
                                                   @RequestHeader("X-User-Role") String role,
                                                   @RequestBody NotificationRequest request) {
        adminAccessService.requireAdmin(userId, role);
        return adminService.createNotification(request);
    }

    @DeleteMapping("/notifications/{notificationId}")
    public void deleteNotification(@RequestHeader("X-User-Id") Long userId,
                                   @RequestHeader("X-User-Role") String role,
                                   @PathVariable("notificationId") Long notificationId) {
        adminAccessService.requireAdmin(userId, role);
        adminService.deleteNotification(notificationId);
    }
}
