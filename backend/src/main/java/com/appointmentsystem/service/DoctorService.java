package com.appointmentsystem.service;

import com.appointmentsystem.dto.DoctorResponse;
import com.appointmentsystem.model.DoctorProfile;
import com.appointmentsystem.repository.DoctorProfileRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DoctorService {

    private final DoctorProfileRepository doctorProfileRepository;

    public DoctorService(DoctorProfileRepository doctorProfileRepository) {
        this.doctorProfileRepository = doctorProfileRepository;
    }

    public List<DoctorResponse> getAllDoctors() {
        Map<String, DoctorResponse> uniqueDoctors = new LinkedHashMap<>();
        doctorProfileRepository.findAll().forEach((profile) -> {
            DoctorResponse response = mapDoctor(profile);
            uniqueDoctors.putIfAbsent((response.getEmail() == null ? String.valueOf(response.getId()) : response.getEmail().trim().toLowerCase()), response);
        });
        return uniqueDoctors.values().stream().toList();
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
}
