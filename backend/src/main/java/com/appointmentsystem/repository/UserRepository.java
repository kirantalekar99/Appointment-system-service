package com.appointmentsystem.repository;

import com.appointmentsystem.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserRepository extends MongoRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findFirstByPhone(String phone);
    long countByRole(com.appointmentsystem.model.Role role);
}
