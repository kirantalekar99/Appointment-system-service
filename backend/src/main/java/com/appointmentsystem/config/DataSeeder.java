package com.appointmentsystem.config;

import com.appointmentsystem.model.DoctorProfile;
import com.appointmentsystem.model.Role;
import com.appointmentsystem.model.User;
import com.appointmentsystem.repository.DoctorProfileRepository;
import com.appointmentsystem.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DoctorProfileRepository doctorProfileRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      DoctorProfileRepository doctorProfileRepository,
                      BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.doctorProfileRepository = doctorProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        createAdmin("System Admin", "admin@carebridge.com", "Admin@123", "9999999999");
        createDoctor("Dr. Ananya Sharma", "ananya@clinic.com", "Cardiologist", 10, "Heart care and blood pressure support");
        createOrUpdatePatient("Demo Patient", "patient@example.com", "patient123", "9876543210");
    }

    private void createAdmin(String name, String email, String rawPassword, String phone) {
        User admin = userRepository.findByEmail(email).orElseGet(User::new);
        admin.setName(name);
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(rawPassword));
        admin.setPhone(phone);
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
    }

    private void createDoctor(String name, String email, String specialization, int experience, String bio) {
        cleanupDuplicateDoctors(email);

        User user = userRepository.findByEmail(email).orElseGet(User::new);
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("doctor123"));
        user.setPhone("1234567890");
        user.setRole(Role.DOCTOR);
        User savedUser = userRepository.save(user);

        DoctorProfile profile = doctorProfileRepository.findByUserId(savedUser.getId()).orElseGet(DoctorProfile::new);
        profile.setUser(savedUser);
        profile.setSpecialization(specialization);
        profile.setExperience(experience);
        profile.setBio(bio);
        profile.setAvailableDays(String.join(", ", List.of("Mon", "Tue", "Wed", "Thu", "Fri")));
        profile.setAvailableTime("10:00 AM - 4:00 PM");
        doctorProfileRepository.save(profile);
    }

    private void cleanupDuplicateDoctors(String email) {
        List<DoctorProfile> profiles = new ArrayList<>(doctorProfileRepository.findAllByUser_Email(email));
        if (profiles.size() > 1) {
            DoctorProfile keepProfile = profiles.remove(0);
            for (DoctorProfile profile : profiles) {
                User duplicateUser = profile.getUser();
                doctorProfileRepository.delete(profile);
                if (duplicateUser != null && !duplicateUser.getId().equals(keepProfile.getUser().getId())) {
                    userRepository.delete(duplicateUser);
                }
            }
        }

        List<User> users = new ArrayList<>(userRepository.findAllByEmail(email));
        if (users.size() > 1) {
            User keepUser = profiles.isEmpty()
                    ? users.get(0)
                    : doctorProfileRepository.findAllByUser_Email(email).stream()
                    .findFirst()
                    .map(DoctorProfile::getUser)
                    .orElse(users.get(0));

            for (User user : users) {
                if (!user.getId().equals(keepUser.getId())) {
                    userRepository.delete(user);
                }
            }
        }
    }

    private void createOrUpdatePatient(String name, String email, String rawPassword, String phone) {
        User patient = userRepository.findByEmail(email).orElseGet(User::new);
        patient.setName(name);
        patient.setEmail(email);
        patient.setPassword(passwordEncoder.encode(rawPassword));
        patient.setPhone(phone);
        patient.setRole(Role.PATIENT);
        userRepository.save(patient);
    }
}
