package com.appointmentsystem.controller;

import com.appointmentsystem.dto.AppointmentRequest;
import com.appointmentsystem.dto.AppointmentResponse;
import com.appointmentsystem.dto.AppointmentStatusUpdateRequest;
import com.appointmentsystem.dto.PatientAppointmentUpdateRequest;
import com.appointmentsystem.service.AppointmentService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@CrossOrigin(origins = "*")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping("/book")
    public AppointmentResponse bookAppointment(@RequestBody AppointmentRequest request) {
        return appointmentService.bookAppointment(request);
    }

    @GetMapping("/patient/{patientId}")
    public List<AppointmentResponse> getPatientAppointments(@PathVariable Long patientId) {
        return appointmentService.getPatientAppointments(patientId);
    }

    @GetMapping("/doctor/{doctorId}")
    public List<AppointmentResponse> getDoctorAppointments(@PathVariable Long doctorId) {
        return appointmentService.getDoctorAppointments(doctorId);
    }

    @PutMapping("/doctor/{doctorId}/{appointmentId}/status")
    public AppointmentResponse updateDoctorAppointmentStatus(@PathVariable("doctorId") Long doctorId,
                                                             @PathVariable("appointmentId") Long appointmentId,
                                                             @RequestBody AppointmentStatusUpdateRequest request) {
        return appointmentService.updateDoctorAppointmentStatus(doctorId, appointmentId, request);
    }

    @PutMapping("/patient/{patientId}/{appointmentId}/reschedule")
    public AppointmentResponse reschedulePatientAppointment(@PathVariable("patientId") Long patientId,
                                                            @PathVariable("appointmentId") Long appointmentId,
                                                            @RequestBody PatientAppointmentUpdateRequest request) {
        return appointmentService.reschedulePatientAppointment(patientId, appointmentId, request);
    }

    @PutMapping("/patient/{patientId}/{appointmentId}/cancel")
    public AppointmentResponse cancelPatientAppointment(@PathVariable("patientId") Long patientId,
                                                        @PathVariable("appointmentId") Long appointmentId) {
        return appointmentService.cancelPatientAppointment(patientId, appointmentId);
    }
}
