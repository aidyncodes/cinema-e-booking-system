package edu.uga.ces.service;

/**
 * Target interface used by the application. Infrastructure adapters translate
 * this application-level operation to a concrete email provider API.
 */
public interface OrderEmailGateway {
    void send(String recipient, String subject, String body);
}
