package com.appointmentsystem.repository;

import com.appointmentsystem.model.Appointment;
import com.appointmentsystem.model.AppointmentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentRepository extends MongoRepository<Appointment, Long> {
    List<Appointment> findByPatientIdOrderByAppointmentDateAsc(Long patientId);
    List<Appointment> findByDoctorIdOrderByAppointmentDateAsc(Long doctorId);
    List<Appointment> findByDoctorId(Long doctorId);
    List<Appointment> findAllByOrderByAppointmentDateAsc();
    boolean existsByDoctorIdAndAppointmentDateAndTimeSlot(Long doctorId, LocalDate appointmentDate, String timeSlot);
    boolean existsByDoctorIdAndStatusIn(Long doctorId, List<AppointmentStatus> statuses);
    long countByStatus(AppointmentStatus status);
}
