package com.appointmentsystem.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AppointmentSchemaCompatibilityFix implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public AppointmentSchemaCompatibilityFix(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(org.springframework.boot.ApplicationArguments args) {
        List<String> doctorConstraints = jdbcTemplate.query(
                """
                SELECT CONSTRAINT_NAME
                FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'appointments'
                  AND COLUMN_NAME = 'doctor_id'
                  AND REFERENCED_TABLE_NAME = 'doctors'
                """,
                (rs, rowNum) -> rs.getString("CONSTRAINT_NAME"));

        for (String constraintName : doctorConstraints) {
            jdbcTemplate.execute("ALTER TABLE appointments DROP FOREIGN KEY " + constraintName);
        }

        Integer compatibleConstraintCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'appointments'
                  AND COLUMN_NAME = 'doctor_id'
                  AND REFERENCED_TABLE_NAME = 'doctor_profiles'
                """,
                Integer.class);

        if (compatibleConstraintCount != null && compatibleConstraintCount == 0) {
            jdbcTemplate.execute("""
                    ALTER TABLE appointments
                    ADD CONSTRAINT fk_appointments_doctor_profiles
                    FOREIGN KEY (doctor_id) REFERENCES doctor_profiles(id)
                    """);
        }
    }
}
