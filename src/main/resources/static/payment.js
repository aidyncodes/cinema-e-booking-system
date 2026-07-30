const paymentContent = document.getElementById("paymentContent");
const paymentError = document.getElementById("paymentError");
const paymentForm = document.getElementById("paymentForm");
const savedCards = document.getElementById("savedCards");
const noCards = document.getElementById("noCards");
const placeOrderBtn = document.getElementById("placeOrderBtn");
const paymentSubmitError = document.getElementById("paymentSubmitError");

let summary = null;
let confirmationEmail = "";

function money(value) {
    return `$${Number(value || 0).toFixed(2)}`;
}

function formatDate(value) {
    if (!value) return "Date unavailable";
    return new Intl.DateTimeFormat("en-US", {
        year: "numeric", month: "long", day: "numeric", timeZone: "UTC"
    }).format(new Date(`${value}T00:00:00Z`));
}

function formatTime(value) {
    if (!value) return "Time unavailable";
    const [hour, minute] = value.split(":").map(Number);
    return new Intl.DateTimeFormat("en-US", {
        hour: "numeric", minute: "2-digit"
    }).format(new Date(2000, 0, 1, hour, minute));
}

function showPaymentError(message) {
    paymentContent.classList.add("hidden");
    paymentError.className = "checkout-alert";
    paymentError.textContent = message;
}

function renderSummary() {
    document.getElementById("paymentMovie").textContent = summary.movieTitle;
    document.getElementById("paymentShowtime").textContent =
        `${formatDate(summary.showDate)} at ${formatTime(summary.showTime)} · ${summary.showroomName}`;
    document.getElementById("paymentSeats").textContent = summary.seats.join(", ");
    document.getElementById("paymentEmail").textContent = confirmationEmail;
    document.getElementById("paymentSubtotal").textContent = money(summary.totalBeforeTax);
    document.getElementById("paymentTax").textContent = money(summary.taxAmount);
    document.getElementById("paymentTotal").textContent = money(summary.totalAmount);
}

function renderCards(cards) {
    savedCards.replaceChildren();
    if (!cards.length) {
        noCards.classList.remove("hidden");
        placeOrderBtn.disabled = true;
        return;
    }

    cards.forEach((card, index) => {
        const label = document.createElement("label");
        label.className = "saved-card-option";

        const radio = document.createElement("input");
        radio.type = "radio";
        radio.name = "paymentCardId";
        radio.value = card.id;
        radio.checked = index === 0;

        const details = document.createElement("span");
        const title = document.createElement("strong");
        title.textContent = `${card.cardBrand || "Card"} ending in ${card.lastFour}`;
        const meta = document.createElement("span");
        meta.textContent = `${card.cardholderName} · Expires ${String(card.expirationMonth).padStart(2, "0")}/${card.expirationYear}`;
        details.append(title, meta);
        label.append(radio, details);
        savedCards.appendChild(label);
    });
    placeOrderBtn.disabled = false;
}

async function handlePayment(event) {
    event.preventDefault();
    const selectedCard = paymentForm.querySelector('input[name="paymentCardId"]:checked');
    if (!selectedCard) {
        paymentSubmitError.textContent = "Select a saved payment card.";
        return;
    }

    placeOrderBtn.disabled = true;
    placeOrderBtn.textContent = "Processing...";
    paymentSubmitError.textContent = "";
    try {
        const order = await apiRequest("/api/checkout/payment", {
            method: "POST",
            body: JSON.stringify({
                paymentCardId: Number(selectedCard.value),
                confirmationEmail
            })
        });
        window.location.href =
            `/order-confirmation.html?confirmation=${encodeURIComponent(order.confirmationNumber)}`;
    } catch (error) {
        if (error.status === 401 || error.status === 403) {
            clearCurrentUser();
            window.location.href = `/login.html?redirect=${encodeURIComponent("/payment.html")}`;
            return;
        }
        if (error.status === 404) {
            showPaymentError("This seat hold is no longer available. Please select your seats again.");
            return;
        }
        paymentSubmitError.textContent = error.message || "Payment could not be completed.";
        placeOrderBtn.disabled = false;
        placeOrderBtn.textContent = "Place Order";
    }
}

async function initPayment() {
    try {
        const [checkoutSummary, profile, emailState] = await Promise.all([
            apiRequest("/api/checkout/summary"),
            apiRequest("/api/profile"),
            apiRequest("/api/checkout/confirmation-email")
        ]);
        summary = checkoutSummary;
        confirmationEmail = emailState.confirmationEmail || "";
        if (!confirmationEmail) {
            window.location.href = "/checkout.html";
            return;
        }
        renderSummary();
        renderCards(profile.paymentCards || []);
        paymentContent.classList.remove("hidden");
    } catch (error) {
        if (error.status === 401 || error.status === 403) {
            clearCurrentUser();
            window.location.href = `/login.html?redirect=${encodeURIComponent("/payment.html")}`;
            return;
        }
        if (error.status === 404) {
            showPaymentError("No pending booking was found. Return to Movies and select a showtime.");
            return;
        }
        showPaymentError(error.message || "Could not load payment. Please try again.");
    }
}

paymentForm.addEventListener("submit", handlePayment);
initPayment();
