package com.appointmentsystem.controller;

import com.appointmentsystem.dto.ContactRequest;
import com.appointmentsystem.service.ContactService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contact")
@CrossOrigin(origins = "*")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @PostMapping
    public String sendSupportRequest(@RequestBody ContactRequest request) {
        return contactService.sendSupportRequest(request);
    }
}
