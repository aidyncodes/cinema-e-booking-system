package edu.uga.ces.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Adapter pattern: adapts Spring's JavaMailSender interface to the
 * application-specific OrderEmailGateway target interface.
 */
@Component
public class JavaMailOrderEmailAdapter implements OrderEmailGateway {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public JavaMailOrderEmailAdapter(JavaMailSender mailSender,
                                     @Value("${app.mail.from}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void send(String recipient, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
    }
}
