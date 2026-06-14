package com.appointmentsystem.service;

import com.appointmentsystem.dto.ContactRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ContactService {

    private final InputValidationService validationService;
    private final EmailService emailService;

    public ContactService(InputValidationService validationService, EmailService emailService) {
        this.validationService = validationService;
        this.emailService = emailService;
    }

    public String sendSupportRequest(ContactRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contact request is required");
        }

        String name = validationService.requireText(request.getName(), "Name");
        String email = validationService.requireText(request.getEmail(), "Email");
        String message = validationService.requireText(request.getMessage(), "Message");

        validationService.validateEmail(email);
        validateLength(name, "Name", 80);
        validateLength(message, "Message", 1600);

        emailService.sendSupportRequestEmail(name, email.trim().toLowerCase(), message);
        return "Your message has been sent. Our support team will contact you soon.";
    }

    private void validateLength(String value, String fieldName, int maxLength) {
        if (value.length() > maxLength) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    fieldName + " must be " + maxLength + " characters or less");
        }
    }
}
