package com.appointmentsystem.dto;

public class AdminSummaryResponse {

    private long totalDoctors;
    private long totalPatients;
    private long totalAppointments;
    private long bookedAppointments;
    private long completedAppointments;
    private long cancelledAppointments;

    public long getTotalDoctors() {
        return totalDoctors;
    }

    public void setTotalDoctors(long totalDoctors) {
        this.totalDoctors = totalDoctors;
    }

    public long getTotalPatients() {
        return totalPatients;
    }

    public void setTotalPatients(long totalPatients) {
        this.totalPatients = totalPatients;
    }

    public long getTotalAppointments() {
        return totalAppointments;
    }

    public void setTotalAppointments(long totalAppointments) {
        this.totalAppointments = totalAppointments;
    }

    public long getBookedAppointments() {
        return bookedAppointments;
    }

    public void setBookedAppointments(long bookedAppointments) {
        this.bookedAppointments = bookedAppointments;
    }

    public long getCompletedAppointments() {
        return completedAppointments;
    }

    public void setCompletedAppointments(long completedAppointments) {
        this.completedAppointments = completedAppointments;
    }

    public long getCancelledAppointments() {
        return cancelledAppointments;
    }

    public void setCancelledAppointments(long cancelledAppointments) {
        this.cancelledAppointments = cancelledAppointments;
    }
}
