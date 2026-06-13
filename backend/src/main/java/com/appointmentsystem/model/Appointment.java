package com.appointmentsystem.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Document(collection = "appointments")
public class Appointment implements SequencedDocument {

    @Id
    private Long id;

    @Indexed
    private Long doctorId;

    @DBRef
    private DoctorProfile doctor;

    @Indexed
    private Long patientId;

    @DBRef
    private User patient;

    private LocalDate appointmentDate;

    private String timeSlot;

    private String legacyTimeSlot;

    private String symptoms;

    private String legacySymptoms;

    @Indexed
    private AppointmentStatus status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public DoctorProfile getDoctor() {
        return doctor;
    }

    public void setDoctor(DoctorProfile doctor) {
        this.doctor = doctor;
        this.doctorId = doctor == null ? null : doctor.getId();
    }

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public User getPatient() {
        return patient;
    }

    public void setPatient(User patient) {
        this.patient = patient;
        this.patientId = patient == null ? null : patient.getId();
    }

    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(LocalDate appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public String getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(String timeSlot) {
        this.timeSlot = timeSlot;
        this.legacyTimeSlot = timeSlot;
    }

    public String getSymptoms() {
        return symptoms;
    }

    public void setSymptoms(String symptoms) {
        this.symptoms = symptoms;
        this.legacySymptoms = symptoms;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    public void syncLegacyColumns() {
        this.legacyTimeSlot = this.timeSlot;
        this.legacySymptoms = this.symptoms;
    }
}
