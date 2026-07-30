const confirmationContent = document.getElementById("confirmationContent");
const confirmationError = document.getElementById("confirmationError");
const confirmationNumber = new URLSearchParams(window.location.search).get("confirmation");

function money(value) {
    return `$${Number(value || 0).toFixed(2)}`;
}

function formatDateTime(date, time) {
    const formattedDate = new Intl.DateTimeFormat("en-US", {
        year: "numeric", month: "long", day: "numeric", timeZone: "UTC"
    }).format(new Date(`${date}T00:00:00Z`));
    const [hour, minute] = time.split(":").map(Number);
    const formattedTime = new Intl.DateTimeFormat("en-US", {
        hour: "numeric", minute: "2-digit"
    }).format(new Date(2000, 0, 1, hour, minute));
    return `${formattedDate} at ${formattedTime}`;
}

function ticketText(tickets) {
    return tickets.map(ticket => {
        const type = String(ticket.ticketType || "").toLowerCase();
        const label = type.charAt(0).toUpperCase() + type.slice(1);
        return `${ticket.seatLabel} (${label}, ${money(ticket.unitPrice)})`;
    }).join(", ");
}

async function loadConfirmation() {
    if (!confirmationNumber) {
        confirmationError.className = "orders-alert";
        confirmationError.textContent = "No confirmation number was provided.";
        return;
    }

    try {
        const order = await apiRequest(
            `/api/profile/orders/${encodeURIComponent(confirmationNumber)}`);
        document.getElementById("confirmationNumber").textContent =
            `Confirmation #${order.confirmationNumber}`;
        document.getElementById("movieTitle").textContent = order.movieTitle;
        document.getElementById("showtime").textContent =
            formatDateTime(order.showDate, order.showTime);
        document.getElementById("showroom").textContent = order.showroomName;
        document.getElementById("tickets").textContent = ticketText(order.tickets);
        document.getElementById("paymentCard").textContent =
            `${order.paymentCardBrand} ending in ${order.paymentCardLastFour}`;
        document.getElementById("subtotal").textContent = money(order.subtotal);
        document.getElementById("tax").textContent = money(order.taxAmount);
        document.getElementById("total").textContent = money(order.totalAmount);
        document.title = `${order.confirmationNumber} - Order Confirmation`;
        confirmationContent.classList.remove("hidden");
    } catch (error) {
        if (error.status === 401 || error.status === 403) {
            clearCurrentUser();
            const target = `/order-confirmation.html?confirmation=${encodeURIComponent(confirmationNumber)}`;
            window.location.href = `/login.html?redirect=${encodeURIComponent(target)}`;
            return;
        }
        confirmationError.className = "orders-alert";
        confirmationError.textContent =
            error.message || "Could not load this order confirmation.";
    }
}

loadConfirmation();
