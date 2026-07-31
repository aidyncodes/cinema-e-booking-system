# Team 3 Final Demo Rehearsal Guide

## Demo objective

Present one continuous, customer-focused acceptance test covering discovery,
booking, checkout, payment, confirmation, order history, and admin integration.
Do not open source code or the database during the main demonstration. Keep
those ready for the instructor's design and Q&A portion.

Recommended main-demo duration: **15–17 minutes**, followed by Q&A.

## Contribution table

Use this table if the team agrees that contributions were equal. The team
leader should send it privately in Zoom chat at the beginning of the demo.

| Team member | Primary contribution | Contribution |
| --- | --- | ---: |
| Aidyn Somerville | Integration, checkout finalization, authentication flow | 20% |
| Allen Chang | Payment integration and order-confirmation email | 20% |
| Brian Lin | Booking flow, order history, and admin home | 20% |
| Yi Chen Liu | Customer/admin frontend and checkout page integration | 20% |
| Jiawei Yu | Database schema, seed data, integrity constraints, and system design | 20% |
| **Total** |  | **100%** |

## Presenter assignment and timing

| Order | Presenter | Target time | Use cases and grading areas | Live demonstration | Key explanation |
| ---: | --- | ---: | --- | --- | --- |
| 1 | **Brian Lin** | 3.5 min | UC 14, 15 and prior admin requirements | Admin login/dashboard, Add Movie, Schedule Movie, scheduling conflict, then log out | Explain admin navigation, validation, conflict prevention, and admin/customer integration. |
| 2 | **Yi Chen Liu** | 4 min | UC 1, 2, 5, 6, 7 | Search/filter, movie details/trailer, showtime, ticket types, invalid seat test, and valid seat hold | Explain usability, backend-driven data, booking validation, and held versus sold seats. |
| 3 | **Aidyn Somerville** | 3.5 min | UC 10, 24, 26; Facade | Login gate if applicable, order summary, email selection, saved-card payment, Place Order, confirmation page | Explain atomic checkout and `CheckoutOrderService` as the Facade. |
| 4 | **Allen Chang** | 3 min | UC 9, 25, 27; Adapter | Show payment result, confirmation email, Order History, and briefly show the no-saved-card path | Explain safe card snapshots, the email Adapter, delivery after commit, and user-scoped history. |
| 5 | **Jiawei Yu** | 3 min | Architecture, database, NFRs, Factory Method | Verbal design explanation; show code/database only if requested | Explain layered architecture, order-ticket schema, constraints, security, maintainability, and Factory Method. |

Each team member presents in one continuous block to minimize speaker and
screen-sharing handoffs.

## Main execution path

### 1. Opening and administrator workflow — Brian Lin

Suggested script:

> Good afternoon. We are Team 3. We will demonstrate our complete Cinema
> E-Booking System through one continuous customer journey, followed by the
> administrator workflow and our system design. We will cover movie discovery,
> booking, checkout, payment, confirmation email, order history, architecture,
> design patterns, security, and data integrity.

Actions:

1. Log in with the administrator account.
2. Open Admin Dashboard.
3. Point out Manage Movies, Manage Users, and Schedule Movies.
4. Add a valid movie.
5. Schedule a showtime in one showroom.
6. Attempt the same showroom/date/time again to show conflict prevention.
7. Log out and leave the browser on the customer movie home page.

Talking points:

> The Admin Home page provides the required navigation. Add Movie validates
> required fields and persists the movie. Scheduling validates the selected
> movie, date, time, and showroom. A duplicate showroom/date/time is rejected
> by both the service and a database unique constraint. Admin changes become
> available to the customer portal.

Handoff:

> The administrator setup is complete. Yi will now demonstrate the customer
> discovery and booking flow.

### 2. Customer discovery, booking, and seat validation — Yi Chen Liu

Actions:

1. Search for the admin-created movie or another known movie.
2. Apply one genre or status filter.
3. Clear the filter and open a movie.
4. Show movie details and play the trailer briefly.
5. Select a database-backed showtime.
6. Select Adult, Senior, and/or Child ticket quantities.
7. Attempt to continue with the wrong number of seats.
8. Show the validation message.
9. Point out an already sold or held seat.
10. Select exactly the required number of available seats.
11. Continue to checkout.

Suggested script:

> Guests and registered users can browse, search, filter, and view movie
> details. Movie and showtime data comes from backend APIs rather than static
> page data.

> Ticket category and seat-count rules are validated on both the client and
> server. The seat map comes from the selected showroom's configured layout.
> Temporary selections are stored as session-owned seat reservations, while
> permanently sold seats are read from the ticket table. A sold or held seat
> cannot be selected by another customer.

Handoff:

> These seats are now held for this session. Aidyn will convert the hold into a
> real paid order.

### 3. Checkout, total, and Place Order — Aidyn Somerville

Actions:

1. If starting as a guest, demonstrate that checkout requires login.
2. Confirm that the selected seats survive login.
3. Show movie, showtime, showroom, seats, ticket types, unit prices, subtotal,
   tax, and total.
4. Confirm the account email or enter an alternate confirmation email.
5. Continue to payment.
6. Select a saved payment card.
7. Click **Place Order** once.
8. Show the confirmation page and confirmation number.

Suggested script:

> The checkout summary is reconstructed from server-side held seats rather
> than trusted browser totals. The backend calculates an eight-percent tax and
> the final total.

> `CheckoutOrderService` implements the Facade pattern. The controller makes
> one high-level `placeOrder` call. Behind that Facade, the service validates
> the customer's card, locks and verifies held seats, calculates prices,
> creates one order and its tickets, records the payment transaction, deletes
> temporary reservations, and publishes the confirmation event.

> The operation is transactional. A failure rolls back the order instead of
> leaving partial tickets or payment data.

Handoff:

> The paid order is now committed. Allen will show how payment data and the
> confirmation email are handled.

### 4. Payment, confirmation email, and order history — Allen Chang

Actions:

1. Point out the card brand and last four digits on the completed order.
2. Open the test email inbox prepared before the demo.
3. Open the new confirmation email.
4. Show confirmation number, movie, date/time, seats, ticket types, unit
   prices, subtotal, tax, total, and card last four.
5. Open **Order History** and locate the order just created.
6. Open its details and point out that they match the confirmation email.
7. Briefly log in as the no-card demo user or show a prepared screenshot/tab
   of the new-card payment form.

Suggested script:

> Checkout only accepts a payment-card ID belonging to the authenticated user.
> The backend validates ownership and expiration. Orders and payment
> transactions store only the brand and last four digits; full card data is
> encrypted with AES-GCM in the saved-card table, and CVV is never stored.

> Confirmation email uses the Adapter pattern.
> `OrderConfirmationEmailService` depends on our application-level
> `OrderEmailGateway`, while `JavaMailOrderEmailAdapter` translates that
> operation into Spring's `JavaMailSender` API. The adapter can be replaced if
> we change email providers without changing checkout or receipt formatting.
> Delivery runs after the purchase commits, so email failure cannot create a
> duplicate order or undo a successful payment.

> A customer without a saved card is shown a secure card-entry form directly
> during checkout. The validated card is encrypted, saved, and then used for
> the order.

> Order history is scoped to the authenticated customer. The API derives the
> user ID from the server session, so a customer cannot request another
> customer's order by changing a browser parameter.

Handoff:

> The purchase, email, and order-history flow is complete. Jiawei will conclude
> with the architecture, database design, design patterns, and non-functional
> requirements.

### 5. Architecture, database, Factory Method, and NFRs — Jiawei Yu

Suggested script:

> Our system follows a layered architecture. Controllers handle HTTP and
> session boundaries. Services contain business rules and transactions.
> Repositories isolate persistence. JPA entities map the domain to MySQL, and
> DTOs define the API contract with the frontend.

> Before payment, selected seats are rows in `seat_reservations`. A successful
> purchase creates one `orders` row, multiple `tickets` rows, and one
> `payment_transactions` row. The `order_id` foreign key groups all seats into
> one purchase for confirmation and order history.

> Data integrity is protected with foreign keys, amount checks, status checks,
> a unique confirmation number, a unique showroom/date/time constraint, and
> `UNIQUE(showtime_id, seat_label)` on tickets to prevent a sold seat from
> being sold twice.

> Our creational pattern is Factory Method. `TicketPricingFactory` defines the
> factory method `createTicketPrice`. Adult, Senior, and Child concrete
> factories create the appropriate `TicketPrice` product.
> `TicketPricingService` selects the concrete creator for the requested ticket
> type. Booking and checkout do not construct ticket-price products directly.

> For security, passwords use BCrypt, reset and confirmation tokens are hashed,
> saved card data uses AES-GCM authenticated encryption, sensitive card values
> are not returned to the browser, server sessions establish identity, and
> admin endpoints verify the ADMIN role.

> For maintainability, responsibilities are separated by layer, validation is
> centralized, ticket prices use extensible concrete factories, and email is decoupled
> through events. For performance, repository queries use indexed foreign keys,
> seat availability queries are scoped by showtime, and order history is
> queried by user.

Closing:

> This completes our customer workflow, admin integration, and system-design
> demonstration. We are ready to show the relevant code or database constraints
> and answer questions.

## Design patterns to show during Q&A

| Pattern | Files to open | What to say |
| --- | --- | --- |
| **Facade** | `CheckoutController.java`, `CheckoutOrderService.java` | The controller calls one `placeOrder` operation. The Facade coordinates card validation, seat locking, pricing, order/ticket/payment writes, cleanup, and event publication. |
| **Adapter** | `OrderEmailGateway.java`, `JavaMailOrderEmailAdapter.java`, `OrderConfirmationEmailService.java` | The application uses its own email target interface. The adapter converts that call into a Spring `JavaMailSender` message, isolating provider-specific code. |
| **Factory Method** | `TicketPricingFactory.java`, `AdultTicketPricingFactory.java`, `SeniorTicketPricingFactory.java`, `ChildTicketPricingFactory.java`, `TicketPrice.java` | Concrete creators implement `createTicketPrice` and construct the correct price product for each ticket category. |

## Use-case readiness matrix

| UC | Requirement | Demo owner | Demo status/path |
| ---: | --- | --- | --- |
| 1 | Create Account | Yi | Registration plus verification-email path; mention unless instructor requests full email round trip. |
| 2 | Login | Brian/Aidyn | Use admin login and customer checkout login gate. |
| 3 | Edit Profile | Allen | Prepared for Q&A; payment-card management is also shown at checkout. |
| 5 | Search or Filter | Yi | Main path. |
| 6 | View Movie Details / Trailer | Yi | Main path. |
| 7 | Book Movies | Yi | Main path with invalid and valid seat tests. |
| 9 | View Order History | Allen | Main path after purchase. |
| 10 | Checkout | Aidyn | Main path. |
| 11 | Apply Promotion | — | Dropped; do not demonstrate. |
| 12 | Reset Password | Yi | Prepared backup path; demonstrate only if requested. |
| 13 | Return Tickets | — | Dropped. |
| 14 | Admin Home | Brian | Main path. |
| 15 | Add New Movie | Brian | Main path. |
| 16 | Delete Movie | — | Dropped. |
| 17–20 | Promotion maintenance | — | Dropped/bonus; do not spend main-demo time. |
| 21 | Manage Users | — | Dropped; only show the Admin Home option if present. |
| 22 | Edit Prices | — | Dropped. |
| 24 | Display Order Total | Aidyn | Main checkout summary and confirmation. |
| 25 | Enter Payment Info | Allen | Saved-card path and no-card path. |
| 26 | Calculate Order Payment | Aidyn | Server-calculated subtotal, tax, and total. |
| 27 | Send Order Confirmation Email | Allen | Main path. |
| 28 | Registration Verification Email | Yi | Prepared backup path. |

## Required acceptance tests

| Test ID | Type | Test path | Expected result | Owner |
| --- | --- | --- | --- | --- |
| FT-01 | Valid | Search exact/partial movie title | Matching movies appear | Yi |
| FT-02 | Valid | Filter by genre/status | Only matching movies appear | Yi |
| FT-03 | Invalid | Continue with zero tickets | Clear validation error | Yi |
| FT-04 | Invalid | Select fewer/more seats than tickets | Request is rejected | Yi |
| FT-05 | Invalid | Select a held or sold seat | Seat cannot be selected/held | Yi |
| FT-06 | Valid | Guest selection followed by login | Held seats survive login | Aidyn |
| FT-07 | Valid | Checkout with saved card | Paid order, tickets, payment transaction, confirmation | Aidyn |
| FT-08 | Valid | Checkout with no saved card | Card-entry form appears and purchase can continue | Allen |
| FT-09 | Invalid | Use another user's card ID | Backend rejects the card | Allen |
| FT-10 | Valid | Open order history after purchase | New order appears with tickets and totals | Allen |
| FT-11 | Security | Request another user's confirmation number | Not found/unauthorized for current user | Allen |
| FT-12 | Valid | Admin adds movie/showtime | Data appears in customer portal | Brian |
| FT-13 | Invalid | Same showroom/date/time twice | Conflict error; no duplicate row | Brian |
| FT-14 | Valid | Inspect confirmation email | All required ticket/seat/price information appears | Allen |

## Prepared demo data

| Purpose | Account |
| --- | --- |
| Administrator | `admin@cinema.com` / `Password123!` |
| Customer with saved cards and history | `customer@cinema.com` / `Password123!` |
| Customer without saved cards | `nocard@cinema.com` / `Password123!` |
| Inactive-account validation | `inactive@cinema.com` / `Password123!` |

Before rehearsal, confirm that these exact accounts exist in the database
instance actually used by the application.

## Pre-demo checklist

- Confirm all five cameras and microphones work.
- Send the 100% contribution table privately at the beginning.
- Start Docker/MySQL and the backend before joining the demo.
- Verify the application is using the Final Demo schema.
- Confirm the email sandbox credentials and inbox are open.
- Use fresh available seats for each rehearsal or rebuild/reset demo data.
- Keep customer, no-card customer, admin, and email inbox tabs prepared.
- Do not expose database passwords, encryption keys, full card numbers, or CVV.
- Use one presenter-controlled browser or rehearse screen-sharing handoffs.
- Do two timed full rehearsals.
- Keep source files and database schema ready for Q&A, but do not open them
  during the customer-value portion unless asked.
- Prepare a fallback confirmation email screenshot in case SMTP is slow.
- Prepare a fallback order-history account with seeded orders.

## Likely instructor questions

### Why is checkout a Facade?

Because the controller calls a single high-level operation while the service
coordinates several lower-level subsystems and hides their sequencing.

### Why send email after commit?

It prevents an email from claiming success before the database commits and
prevents an SMTP failure from rolling back or duplicating a paid order.

### How do you prevent duplicate seat sales?

The service locks held reservations during checkout, and the database enforces
a unique `(showtime_id, seat_label)` constraint on permanent tickets.

### How is another customer's data protected?

User identity comes from the authenticated server session. Saved-card and order
queries include that user ID rather than trusting a user ID supplied by the
browser.

### What sensitive payment data is stored?

Saved full card data is AES-GCM encrypted. APIs expose only safe metadata.
Orders and payment transactions store only brand and last four digits. CVV is
never stored.

### How would you add a student ticket?

Add a `StudentTicketPricingFactory` implementing `TicketPricingFactory`, whose
factory method creates a Student `TicketPrice`, then register the new ticket
type in input validation/UI. Checkout orchestration does not construct the new
product directly.
