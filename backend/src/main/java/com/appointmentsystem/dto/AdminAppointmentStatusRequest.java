package com.appointmentsystem.dto;

import com.appointmentsystem.model.AppointmentStatus;

public class AdminAppointmentStatusRequest {

    private AppointmentStatus status;

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }
}
