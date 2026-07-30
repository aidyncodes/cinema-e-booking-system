const ordersList = document.getElementById("ordersList");
const ordersLoading = document.getElementById("ordersLoading");
const ordersEmpty = document.getElementById("ordersEmpty");
const ordersError = document.getElementById("ordersError");

function money(value) {
    return `$${Number(value || 0).toFixed(2)}`;
}

function formatShowtime(date, time) {
    if (!date || !time) return "Showtime unavailable";
    const formattedDate = new Intl.DateTimeFormat("en-US", {
        year: "numeric", month: "short", day: "numeric", timeZone: "UTC"
    }).format(new Date(`${date}T00:00:00Z`));
    const [hour, minute] = time.split(":").map(Number);
    const formattedTime = new Intl.DateTimeFormat("en-US", {
        hour: "numeric", minute: "2-digit"
    }).format(new Date(2000, 0, 1, hour, minute));
    return `${formattedDate} at ${formattedTime}`;
}

function formatPlacedAt(value) {
    if (!value) return "Date unavailable";
    return new Intl.DateTimeFormat("en-US", {
        year: "numeric", month: "short", day: "numeric",
        hour: "numeric", minute: "2-digit"
    }).format(new Date(value));
}

function ticketLabel(ticket) {
    const type = String(ticket.ticketType || "").toLowerCase();
    const label = type.charAt(0).toUpperCase() + type.slice(1);
    return `${ticket.seatLabel} · ${label} ${money(ticket.unitPrice)}`;
}

function detailRow(label, value) {
    const row = document.createElement("div");
    const term = document.createElement("dt");
    const detail = document.createElement("dd");
    term.textContent = label;
    detail.textContent = value;
    row.append(term, detail);
    return row;
}

function createOrderCard(order) {
    const article = document.createElement("article");
    article.className = "order-card";

    const header = document.createElement("header");
    header.className = "order-card-header";
    const headingWrap = document.createElement("div");
    const heading = document.createElement("h2");
    heading.textContent = order.movieTitle;
    const confirmation = document.createElement("p");
    confirmation.textContent = `Confirmation #${order.confirmationNumber}`;
    headingWrap.append(heading, confirmation);
    const status = document.createElement("span");
    status.className = `order-status ${String(order.status || "").toLowerCase()}`;
    status.textContent = order.status;
    header.append(headingWrap, status);

    const details = document.createElement("dl");
    details.className = "order-card-details";
    details.append(
        detailRow("Showtime", formatShowtime(order.showDate, order.showTime)),
        detailRow("Showroom", order.showroomName),
        detailRow("Seats / Tickets", order.tickets.map(ticketLabel).join(", ")),
        detailRow("Placed", formatPlacedAt(order.placedAt)),
        detailRow("Payment", `${order.paymentCardBrand} ending in ${order.paymentCardLastFour}`)
    );

    const footer = document.createElement("footer");
    footer.className = "order-card-footer";
    const amounts = document.createElement("div");
    amounts.className = "order-amounts";
    amounts.innerHTML =
        `<span>Subtotal ${money(order.subtotal)}</span>` +
        `<span>Tax ${money(order.taxAmount)}</span>` +
        `<strong>Total ${money(order.totalAmount)}</strong>`;
    const link = document.createElement("a");
    link.className = "button secondary";
    link.href =
        `/order-confirmation.html?confirmation=${encodeURIComponent(order.confirmationNumber)}`;
    link.textContent = "View Details";
    footer.append(amounts, link);

    article.append(header, details, footer);
    return article;
}

async function loadOrders() {
    try {
        const orders = await apiRequest("/api/profile/orders");
        ordersLoading.classList.add("hidden");
        if (!orders.length) {
            ordersEmpty.classList.remove("hidden");
            return;
        }
        ordersList.replaceChildren(...orders.map(createOrderCard));
    } catch (error) {
        if (error.status === 401 || error.status === 403) {
            clearCurrentUser();
            window.location.href = `/login.html?redirect=${encodeURIComponent("/orders.html")}`;
            return;
        }
        ordersLoading.classList.add("hidden");
        ordersError.className = "orders-alert";
        ordersError.textContent = error.message || "Could not load your orders.";
    }
}

loadOrders();
