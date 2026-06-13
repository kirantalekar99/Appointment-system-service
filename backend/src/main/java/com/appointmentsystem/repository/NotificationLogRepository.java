package com.appointmentsystem.repository;

import com.appointmentsystem.model.NotificationLog;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface NotificationLogRepository extends MongoRepository<NotificationLog, Long> {
}
