package com.appointmentsystem.config;

import com.appointmentsystem.model.Role;
import com.appointmentsystem.model.User;
import com.appointmentsystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final String adminName;
    private final String adminEmail;
    private final String adminPassword;
    private final String adminPhone;

    public DataSeeder(UserRepository userRepository,
                      BCryptPasswordEncoder passwordEncoder,
                      @Value("${app.admin.name:CareBridge Admin}") String adminName,
                      @Value("${app.admin.email:}") String adminEmail,
                      @Value("${app.admin.password:}") String adminPassword,
                      @Value("${app.admin.phone:9999999999}") String adminPhone) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminName = adminName == null || adminName.isBlank() ? "CareBridge Admin" : adminName.trim();
        this.adminEmail = adminEmail == null ? "" : adminEmail.trim().toLowerCase();
        this.adminPassword = adminPassword == null ? "" : adminPassword.trim();
        this.adminPhone = adminPhone == null || adminPhone.isBlank() ? "9999999999" : adminPhone.trim();
    }

    @Override
    public void run(String... args) {
        createConfiguredAdmin();
    }

    private void createConfiguredAdmin() {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            return;
        }

        createAdmin(adminName, adminEmail, adminPassword, adminPhone);
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
}
