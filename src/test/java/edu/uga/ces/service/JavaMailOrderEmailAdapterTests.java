package edu.uga.ces.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class JavaMailOrderEmailAdapterTests {

    @Test
    void adaptsOrderEmailGatewayCallToJavaMailMessage() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        OrderEmailGateway adapter = new JavaMailOrderEmailAdapter(
                mailSender, "no-reply@ces-cinema.test");

        adapter.send("customer@example.com", "Order confirmed", "Receipt body");

        ArgumentCaptor<SimpleMailMessage> captor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertEquals("no-reply@ces-cinema.test", message.getFrom());
        assertArrayEquals(new String[]{"customer@example.com"}, message.getTo());
        assertEquals("Order confirmed", message.getSubject());
        assertEquals("Receipt body", message.getText());
    }
}
