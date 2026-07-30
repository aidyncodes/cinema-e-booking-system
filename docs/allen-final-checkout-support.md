# Allen final checkout support

This change supplies the saved-card and order-confirmation-email pieces used by
the final order endpoint.

## Checkout request

The payment page should load the user's existing masked cards from
`GET /api/profile` and send only the selected card id and confirmation email:

```json
{
  "paymentCardId": 7,
  "confirmationEmail": "customer@example.com"
}
```

`CheckoutPaymentRequest` defines this request contract.

## Order finalization integration

The order-finalization service should validate the selected card using the
authenticated user id:

```java
PaymentCardSnapshot card = checkoutPaymentService.requireSavedCard(
        userId,
        request.paymentCardId());

order.setConfirmationEmail(request.confirmationEmail());
order.setPaymentCardBrand(card.cardBrand());
order.setPaymentCardLastFour(card.lastFour());
```

The complete card number must not be accepted from the frontend or copied onto
the order. The order stores only the saved card's brand and last four digits.

After the order, tickets, and payment transaction are saved, publish an
`OrderConfirmationEvent` with Spring's `ApplicationEventPublisher`:

```java
eventPublisher.publishEvent(new OrderConfirmationEvent(
        confirmationNumber,
        confirmationEmail,
        customerFirstName,
        movieTitle,
        showDate,
        showTime,
        showroom,
        seats,
        ticketLines,
        subtotal,
        taxAmount,
        totalAmount,
        card.cardBrand(),
        card.lastFour()));
```

`OrderConfirmationEmailService` handles the event after the database
transaction commits. An SMTP failure is logged but does not undo or duplicate
the completed purchase.
