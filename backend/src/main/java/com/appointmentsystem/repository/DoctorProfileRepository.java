package com.appointmentsystem.repository;

import com.appointmentsystem.model.DoctorProfile;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DoctorProfileRepository extends MongoRepository<DoctorProfile, Long> {
    Optional<DoctorProfile> findByUserId(Long userId);
    List<DoctorProfile> findAllByUserIdIn(Collection<Long> userIds);
}
