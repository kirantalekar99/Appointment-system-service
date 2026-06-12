package com.appointmentsystem.service;

import com.appointmentsystem.model.Role;
import com.appointmentsystem.model.User;
import com.appointmentsystem.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminAccessService {

    private final UserRepository userRepository;

    public AdminAccessService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void requireAdmin(Long userId, String roleHeader) {
        if (userId == null || roleHeader == null || roleHeader.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin login is required");
        }

        Role role;
        try {
            role = Role.valueOf(roleHeader.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admin can access this feature");
        }

        if (role != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admin can access this feature");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin user not found"));

        if (user.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admin can access this feature");
        }
    }
}
