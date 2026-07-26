const BOOKING_KEY = "ces_pending_booking";
const paymentContent = document.getElementById("paymentContent");
const paymentError = document.getElementById("paymentError");

function loadPaymentBooking() {
    try {
        return JSON.parse(sessionStorage.getItem(BOOKING_KEY));
    } catch (error) {
        return null;
    }
}

function showPaymentError(message) {
    paymentContent.classList.add("hidden");
    paymentError.className = "checkout-alert";
    paymentError.textContent = message;
}

const paymentBooking = loadPaymentBooking();
if (!paymentBooking) {
    showPaymentError("No pending booking was found. Return to Movies and select a showtime.");
} else if (!isLoggedIn()) {
    window.location.href = `/login.html?redirect=${encodeURIComponent("/payment.html")}`;
} else if (!paymentBooking.email) {
    window.location.href = "/checkout.html";
} else {
    document.getElementById("paymentMovie").textContent = paymentBooking.title;
    document.getElementById("paymentShowtime").textContent = `${paymentBooking.showtime} · ${paymentBooking.auditorium}`;
    document.getElementById("paymentSeats").textContent = paymentBooking.seats.join(", ");
    document.getElementById("paymentEmail").textContent = paymentBooking.email;
    document.getElementById("paymentSubtotal").textContent = `$${Number(paymentBooking.subtotal || 0).toFixed(2)}`;
    paymentContent.classList.remove("hidden");
}
