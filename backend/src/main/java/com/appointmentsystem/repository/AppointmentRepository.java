package com.appointmentsystem.repository;

import com.appointmentsystem.model.Appointment;
import com.appointmentsystem.model.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByPatient_IdOrderByAppointmentDateAsc(Long patientId);
    List<Appointment> findByDoctor_IdOrderByAppointmentDateAsc(Long doctorId);
    List<Appointment> findByDoctor_Id(Long doctorId);
    List<Appointment> findAllByOrderByAppointmentDateAsc();
    boolean existsByDoctor_IdAndAppointmentDateAndTimeSlot(Long doctorId, LocalDate appointmentDate, String timeSlot);
    boolean existsByDoctor_IdAndStatusIn(Long doctorId, List<AppointmentStatus> statuses);
    long countByStatus(AppointmentStatus status);
}
