package edu.uga.ces.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.backend-base-url}")
    private String backendBaseUrl;

    @Value("${spring.mail.username}")
    private String fromAddress;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendConfirmationEmail(String toEmail, String firstName, String token) {
        String confirmUrl = backendBaseUrl + "/api/auth/confirm?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("Confirm your CES Cinema account");
        message.setText(
                "Hi " + firstName + ",\n\n" +
                "Thanks for signing up for CES Cinema. Confirm your account by clicking the link below:\n\n" +
                confirmUrl + "\n\n" +
                "This link expires in 24 hours. If you didn't create this account, you can ignore this email.\n"
        );

        mailSender.send(message);
    }
}