const BOOKING_KEY = "ces_pending_booking";
const checkoutContent = document.getElementById("checkoutContent");
const checkoutError = document.getElementById("checkoutError");
const backToSeats = document.getElementById("backToSeats");
const emailForm = document.getElementById("emailForm");
const accountEmail = document.getElementById("accountEmail");
const useAccountEmail = document.getElementById("useAccountEmail");
const useDifferentEmail = document.getElementById("useDifferentEmail");
const differentEmail = document.getElementById("differentEmail");
const emailError = document.getElementById("emailError");
const paymentBtn = document.getElementById("paymentBtn");

let booking = null;
let profileEmail = "";

function money(value) {
    return `$${Number(value || 0).toFixed(2)}`;
}

function ticketLabel(type) {
    return type.charAt(0).toUpperCase() + type.slice(1);
}

function showCheckoutError(message) {
    checkoutContent.classList.add("hidden");
    checkoutError.className = "checkout-alert";
    checkoutError.textContent = message;
}

function loadPendingBooking() {
    try {
        return JSON.parse(sessionStorage.getItem(BOOKING_KEY));
    } catch (error) {
        return null;
    }
}

function renderBooking() {
    document.getElementById("summaryMovie").textContent = booking.title;
    document.getElementById("summaryShowtime").textContent = booking.showtime;
    document.getElementById("summaryShowroom").textContent = booking.auditorium;
    document.getElementById("summarySeats").textContent = booking.seats.join(", ");
    document.getElementById("summarySubtotal").textContent = money(booking.subtotal);
    backToSeats.href = booking.bookingUrl || "/index.html";

    const body = document.getElementById("ticketSummaryBody");
    body.replaceChildren();
    ["adult", "senior", "child"].forEach(type => {
        const quantity = Number(booking.tickets[type] || 0);
        if (!quantity) return;
        const price = Number(booking.prices[type] || 0);
        const row = document.createElement("tr");
        [ticketLabel(type), quantity, money(price), money(quantity * price)].forEach(value => {
            const cell = document.createElement("td");
            cell.textContent = value;
            row.appendChild(cell);
        });
        body.appendChild(row);
    });
    checkoutContent.classList.remove("hidden");
}

function setEmailMode() {
    differentEmail.disabled = !useDifferentEmail.checked;
    emailError.textContent = "";
    differentEmail.classList.remove("invalid");
    if (useDifferentEmail.checked) {
        differentEmail.focus();
    }
}

function validEmail(value) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
}

async function loadProfileEmail() {
    try {
        const profile = await apiRequest("/api/profile");
        profileEmail = profile.email || "";
    } catch (error) {
        if (error.status === 401 || error.status === 403) {
            clearCurrentUser();
            window.location.href = `/login.html?redirect=${encodeURIComponent("/checkout.html")}`;
            return;
        }
        const currentUser = getCurrentUser();
        profileEmail = currentUser && currentUser.email ? currentUser.email : "";
    }
    accountEmail.textContent = profileEmail || "No account email available";
    useAccountEmail.disabled = !profileEmail;
    if (!profileEmail) {
        useDifferentEmail.checked = true;
        setEmailMode();
    }
    paymentBtn.disabled = false;
}

emailForm.addEventListener("submit", event => {
    event.preventDefault();
    const chosenEmail = useDifferentEmail.checked
        ? differentEmail.value.trim()
        : profileEmail;

    if (!validEmail(chosenEmail)) {
        if (!useDifferentEmail.checked) {
            useDifferentEmail.checked = true;
            setEmailMode();
        }
        emailError.textContent = "Enter a valid email address.";
        differentEmail.classList.add("invalid");
        return;
    }

    paymentBtn.disabled = true;
    booking.email = chosenEmail;
    sessionStorage.setItem(BOOKING_KEY, JSON.stringify(booking));
    window.location.href = "/payment.html";
});

useAccountEmail.addEventListener("change", setEmailMode);
useDifferentEmail.addEventListener("change", setEmailMode);
differentEmail.addEventListener("input", () => {
    emailError.textContent = "";
    differentEmail.classList.remove("invalid");
});

booking = loadPendingBooking();
if (!booking) {
    showCheckoutError("No pending booking was found. Return to Movies and select a showtime.");
} else if (!isLoggedIn()) {
    window.location.href = `/login.html?redirect=${encodeURIComponent("/checkout.html")}`;
} else {
    renderBooking();
    loadProfileEmail();
}
