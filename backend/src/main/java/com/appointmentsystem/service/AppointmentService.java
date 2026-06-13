package com.appointmentsystem.service;

import com.appointmentsystem.dto.AppointmentRequest;
import com.appointmentsystem.dto.AppointmentResponse;
import com.appointmentsystem.dto.AppointmentStatusUpdateRequest;
import com.appointmentsystem.dto.PatientAppointmentUpdateRequest;
import com.appointmentsystem.model.Appointment;
import com.appointmentsystem.model.AppointmentStatus;
import com.appointmentsystem.model.DoctorProfile;
import com.appointmentsystem.model.Role;
import com.appointmentsystem.model.User;
import com.appointmentsystem.repository.AppointmentRepository;
import com.appointmentsystem.repository.DoctorProfileRepository;
import com.appointmentsystem.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@Service
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              DoctorProfileRepository doctorProfileRepository,
                              UserRepository userRepository,
                              EmailService emailService) {
        this.appointmentRepository = appointmentRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    public AppointmentResponse bookAppointment(AppointmentRequest request) {
        DoctorProfile doctor = doctorProfileRepository.findById(request.getDoctorId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found"));

        User patient = userRepository.findById(request.getPatientId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found"));

        if (patient.getRole() != Role.PATIENT) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only patients can book appointments");
        }

        if (request.getAppointmentDate() == null || request.getAppointmentDate().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Appointment date must be today or later");
        }

        if (appointmentRepository.existsByDoctorIdAndAppointmentDateAndTimeSlot(
                doctor.getId(), request.getAppointmentDate(), request.getTimeSlot())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This time slot is already booked");
        }

        Appointment appointment = new Appointment();
        appointment.setDoctor(doctor);
        appointment.setPatient(patient);
        appointment.setAppointmentDate(request.getAppointmentDate());
        appointment.setTimeSlot(request.getTimeSlot());
        appointment.setSymptoms(request.getSymptoms());
        appointment.setStatus(AppointmentStatus.BOOKED);

        Appointment savedAppointment = appointmentRepository.save(appointment);
        try {
            emailService.sendDoctorAppointmentBookedEmail(
                    doctor.getUser().getEmail(),
                    doctor.getUser().getName(),
                    patient.getName(),
                    savedAppointment.getAppointmentDate().toString(),
                    savedAppointment.getTimeSlot(),
                    savedAppointment.getSymptoms() == null || savedAppointment.getSymptoms().isBlank() ? "Not provided" : savedAppointment.getSymptoms()
            );
        } catch (Exception ignored) {
        }
        try {
            emailService.sendPatientAppointmentBookedEmail(
                    patient.getEmail(),
                    patient.getName(),
                    doctor.getUser().getName(),
                    savedAppointment.getAppointmentDate().toString(),
                    savedAppointment.getTimeSlot(),
                    savedAppointment.getSymptoms() == null || savedAppointment.getSymptoms().isBlank() ? "Not provided" : savedAppointment.getSymptoms()
            );
        } catch (Exception ignored) {
        }
        return mapAppointment(savedAppointment);
    }

    public List<AppointmentResponse> getPatientAppointments(Long patientId) {
        return appointmentRepository.findByPatientIdOrderByAppointmentDateAsc(patientId)
                .stream()
                .map(this::mapAppointment)
                .toList();
    }

    public List<AppointmentResponse> getDoctorAppointments(Long doctorId) {
        return appointmentRepository.findByDoctorIdOrderByAppointmentDateAsc(doctorId)
                .stream()
                .map(this::mapAppointment)
                .toList();
    }

    public AppointmentResponse updateDoctorAppointmentStatus(Long doctorId, Long appointmentId, AppointmentStatusUpdateRequest request) {
        if (request.getStatus() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Appointment status is required");
        }

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));

        if (!appointment.getDoctor().getId().equals(doctorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Doctor cannot manage this appointment");
        }

        appointment.setStatus(request.getStatus());
        Appointment savedAppointment = appointmentRepository.save(appointment);

        try {
            emailService.sendPatientAppointmentStatusEmail(
                    savedAppointment.getPatient().getEmail(),
                    savedAppointment.getPatient().getName(),
                    savedAppointment.getDoctor().getUser().getName(),
                    savedAppointment.getAppointmentDate().toString(),
                    savedAppointment.getTimeSlot(),
                    savedAppointment.getStatus().name()
            );
        } catch (Exception ignored) {
        }

        return mapAppointment(savedAppointment);
    }

    public AppointmentResponse reschedulePatientAppointment(Long patientId, Long appointmentId, PatientAppointmentUpdateRequest request) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));

        if (!appointment.getPatient().getId().equals(patientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Patient cannot update this appointment");
        }

        if (request.getAppointmentDate() == null || request.getAppointmentDate().isBefore(LocalDate.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Appointment date must be today or later");
        }

        if (request.getTimeSlot() == null || request.getTimeSlot().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Time slot is required");
        }

        if (appointmentRepository.existsByDoctorIdAndAppointmentDateAndTimeSlot(
                appointment.getDoctor().getId(), request.getAppointmentDate(), request.getTimeSlot().trim())
                && (!appointment.getAppointmentDate().equals(request.getAppointmentDate())
                || !appointment.getTimeSlot().equals(request.getTimeSlot().trim()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This time slot is already booked");
        }

        appointment.setAppointmentDate(request.getAppointmentDate());
        appointment.setTimeSlot(request.getTimeSlot().trim());
        appointment.setSymptoms(request.getSymptoms() == null ? appointment.getSymptoms() : request.getSymptoms().trim());
        appointment.setStatus(AppointmentStatus.BOOKED);
        return mapAppointment(appointmentRepository.save(appointment));
    }

    public AppointmentResponse cancelPatientAppointment(Long patientId, Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found"));

        if (!appointment.getPatient().getId().equals(patientId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Patient cannot cancel this appointment");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        return mapAppointment(appointmentRepository.save(appointment));
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
}
