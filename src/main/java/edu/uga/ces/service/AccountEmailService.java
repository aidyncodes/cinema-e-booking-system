package edu.uga.ces.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/** Email messages owned by Allen's password/profile deliverable. */
@Service
public class AccountEmailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String frontendBaseUrl;

    public AccountEmailService(JavaMailSender mailSender,
                               @Value("${spring.mail.username}") String fromAddress,
                               @Value("${FRONTEND_BASE_URL:http://localhost:8080}") String frontendBaseUrl) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    public void sendPasswordResetEmail(String toEmail, String firstName, String token) {
        String resetUrl = frontendBaseUrl + "/reset-password.html?token=" + token;
        send(
                toEmail,
                "Reset your CES Cinema password",
                "Hi " + firstName + ",\n\n" +
                        "Use the link below to reset your CES Cinema password:\n\n" +
                        resetUrl + "\n\n" +
                        "This link expires in 1 hour and can only be used once. " +
                        "If you did not request it, you can ignore this email.\n");
    }

    public void sendPasswordChangedEmail(String toEmail, String firstName) {
        send(
                toEmail,
                "Your CES Cinema password was changed",
                "Hi " + firstName + ",\n\nYour CES Cinema password was changed. " +
                        "If you did not make this change, contact the cinema administrator immediately.\n");
    }

    public void sendProfileChangedEmail(String toEmail, String firstName) {
        send(
                toEmail,
                "Your CES Cinema profile was updated",
                "Hi " + firstName + ",\n\nYour CES Cinema profile information was updated. " +
                        "If you did not make this change, contact the cinema administrator immediately.\n");
    }

    private void send(String toEmail, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
}
