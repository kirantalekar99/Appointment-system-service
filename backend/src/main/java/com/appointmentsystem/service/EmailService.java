package com.appointmentsystem.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class EmailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailService.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final String brevoBaseUrl;
    private final String brevoApiKey;
    private final String fromEmail;
    private final String senderName;

    public EmailService(@Value("${mail.brevo-base-url:https://api.brevo.com}") String brevoBaseUrl,
                        @Value("${mail.brevo-api-key:}") String brevoApiKey,
                        @Value("${app.mail.from:}") String fromEmail,
                        @Value("${app.mail.sender-name:CareBridge}") String senderName) {
        this.brevoBaseUrl = brevoBaseUrl == null || brevoBaseUrl.isBlank() ? "https://api.brevo.com" : brevoBaseUrl.trim();
        this.brevoApiKey = brevoApiKey == null ? "" : brevoApiKey.trim();
        this.fromEmail = fromEmail == null ? "" : fromEmail.trim();
        this.senderName = senderName == null || senderName.isBlank() ? "CareBridge" : senderName.trim();
    }

    public void sendOtpEmail(String email, String name, String otpCode) {
        sendEmail(email, "CareBridge Registration OTP",
                buildEmailTemplate(
                        "Email verification for CareBridge",
                        "Use the OTP below to complete your registration.",
                        "<div style=\"margin:24px 0;padding:18px;border-radius:16px;background:#eef8fd;border:1px solid #cfe4ef;text-align:center;\">"
                                + "<div style=\"font-size:13px;color:#5d6b7a;margin-bottom:8px;\">Your OTP Code</div>"
                                + "<div style=\"font-size:32px;font-weight:800;color:#154f74;letter-spacing:6px;\">" + escapeHtml(otpCode) + "</div>"
                                + "<div style=\"font-size:13px;color:#5d6b7a;margin-top:10px;\">Valid for 10 minutes</div>"
                                + "</div>",
                        name
                ));
    }

    public void sendWelcomeEmail(String email, String name, String password) {
        sendEmail(email, "Welcome to CareBridge",
                buildEmailTemplate(
                        "Your CareBridge account is ready",
                        "Welcome to CareBridge. Your account has been created successfully.",
                        buildDetailsBlock(
                                detailRow("User ID", email),
                                detailRow("Password", password)
                        ),
                        name
                ));
    }

    public void sendDoctorAppointmentBookedEmail(String email,
                                                 String doctorName,
                                                 String patientName,
                                                 String appointmentDate,
                                                 String timeSlot,
                                                 String symptoms) {
        sendEmail(email, "New appointment booked on CareBridge",
                buildEmailTemplate(
                        "A new appointment has been booked",
                        "Please review the appointment details below and login to CareBridge to manage it.",
                        buildDetailsBlock(
                                detailRow("Patient", patientName),
                                detailRow("Date", appointmentDate),
                                detailRow("Time", timeSlot),
                                detailRow("Symptoms / Reason", symptoms)
                        ),
                        "Dr. " + doctorName
                ));
    }

    public void sendPatientAppointmentBookedEmail(String email,
                                                  String patientName,
                                                  String doctorName,
                                                  String appointmentDate,
                                                  String timeSlot,
                                                  String symptoms) {
        sendEmail(email, "Your CareBridge appointment is confirmed",
                buildEmailTemplate(
                        "Your appointment has been booked successfully",
                        "Your booking has been received. Here are your appointment details.",
                        buildDetailsBlock(
                                detailRow("Doctor", doctorName),
                                detailRow("Date", appointmentDate),
                                detailRow("Time", timeSlot),
                                detailRow("Symptoms / Reason", symptoms)
                        ),
                        patientName
                ));
    }

    public void sendPatientAppointmentStatusEmail(String email,
                                                  String patientName,
                                                  String doctorName,
                                                  String appointmentDate,
                                                  String timeSlot,
                                                  String status) {
        sendEmail(email, "CareBridge appointment status updated",
                buildEmailTemplate(
                        "Your appointment status has been updated",
                        "The doctor has updated your appointment. Please check the latest status below.",
                        buildDetailsBlock(
                                detailRow("Doctor", doctorName),
                                detailRow("Date", appointmentDate),
                                detailRow("Time", timeSlot),
                                detailRow("New Status", formatStatus(status))
                        ),
                        patientName
                ));
    }

    private void sendEmail(String to, String subject, String body) {
        if (fromEmail.isBlank() || brevoApiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Email service is not configured. Please set BREVO_API_KEY and MAIL_FROM.");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            headers.set("api-key", brevoApiKey);

            BrevoEmailRequest payload = new BrevoEmailRequest(
                    new BrevoSender(fromEmail, senderName),
                    List.of(new BrevoRecipient(to)),
                    subject,
                    body
            );

            ResponseEntity<String> response = restTemplate.exchange(
                    brevoBaseUrl + "/v3/smtp/email",
                    HttpMethod.POST,
                    new HttpEntity<>(payload, headers),
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to send email at the moment");
            }
        } catch (HttpStatusCodeException ex) {
            String errorBody = ex.getResponseBodyAsString();
            LOGGER.error("Brevo email send failed with status {}: {}", ex.getStatusCode(), errorBody);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    errorBody == null || errorBody.isBlank()
                            ? "Unable to send email at the moment"
                            : "Brevo error: " + errorBody
            );
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            LOGGER.error("Brevo email send failed: {}", ex.getMessage(), ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to send email at the moment");
        }
    }

    private record BrevoSender(String email, String name) {
    }

    private record BrevoRecipient(String email) {
    }

    private record BrevoEmailRequest(BrevoSender sender,
                                     List<BrevoRecipient> to,
                                     String subject,
                                     String htmlContent) {
    }

    private String buildEmailTemplate(String title, String subtitle, String content, String recipientName) {
        return """
                <div style="margin:0;padding:32px;background:#eef6fa;font-family:Segoe UI,Tahoma,Geneva,Verdana,sans-serif;color:#1c2430;">
                  <div style="max-width:680px;margin:0 auto;background:#ffffff;border-radius:24px;overflow:hidden;border:1px solid #d8e6ee;box-shadow:0 18px 40px rgba(24,74,107,0.12);">
                    <div style="padding:24px 32px;background:linear-gradient(135deg,#1d6fa3 0%,#4eb8bf 100%);color:#ffffff;">
                      <div style="font-size:26px;font-weight:800;">CareBridge</div>
                      <div style="margin-top:6px;font-size:14px;opacity:0.92;">Healthcare communication update</div>
                    </div>
                    <div style="padding:32px;">
                      <div style="font-size:16px;color:#5d6b7a;">Hello {{recipientName}},</div>
                      <h2 style="margin:14px 0 10px;font-size:28px;color:#154f74;">{{title}}</h2>
                      <p style="margin:0 0 20px;font-size:16px;line-height:1.7;color:#445261;">{{subtitle}}</p>
                      {{content}}
                      <p style="margin:24px 0 0;font-size:15px;line-height:1.7;color:#5d6b7a;">Thank you for using CareBridge.</p>
                    </div>
                  </div>
                </div>
                """
                .replace("{{recipientName}}", escapeHtml(recipientName))
                .replace("{{title}}", escapeHtml(title))
                .replace("{{subtitle}}", escapeHtml(subtitle))
                .replace("{{content}}", content);
    }

    private String buildDetailsBlock(String... rows) {
        return "<div style=\"margin-top:18px;padding:20px;border-radius:18px;background:#f7fbfd;border:1px solid #dce7ee;\">"
                + String.join("", rows)
                + "</div>";
    }

    private String detailRow(String label, String value) {
        return "<div style=\"display:flex;gap:12px;justify-content:space-between;padding:10px 0;border-bottom:1px solid #e6eef3;\">"
                + "<div style=\"font-weight:700;color:#154f74;\">" + escapeHtml(label) + "</div>"
                + "<div style=\"color:#1c2430;text-align:right;max-width:62%;\">" + escapeHtml(value == null || value.isBlank() ? "Not provided" : value) + "</div>"
                + "</div>";
    }

    private String formatStatus(String status) {
        if (status == null || status.isBlank()) {
            return "Updated";
        }
        String lower = status.toLowerCase().replace("_", " ");
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
