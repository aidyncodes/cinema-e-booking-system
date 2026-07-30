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

let summary = null;
let profile = null;
let profileEmail = "";

function money(value) {
    return `$${Number(value || 0).toFixed(2)}`;
}

function ticketLabel(type) {
    const normalized = String(type || "").toLowerCase();
    return normalized.charAt(0).toUpperCase() + normalized.slice(1);
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

function showCheckoutError(message) {
    checkoutContent.classList.add("hidden");
    checkoutError.className = "checkout-alert";
    checkoutError.textContent = message;
}

function renderBooking() {
    const showtimeText = `${formatDate(summary.showDate)} at ${formatTime(summary.showTime)}`;
    document.getElementById("summaryMovie").textContent = summary.movieTitle;
    document.getElementById("summaryShowtime").textContent = showtimeText;
    document.getElementById("summaryShowroom").textContent = summary.showroomName;
    document.getElementById("summarySeats").textContent = summary.seats.join(", ");
    document.getElementById("summarySubtotal").textContent = money(summary.totalBeforeTax);

    const bookingParams = new URLSearchParams({
        showtimeId: summary.showtimeId,
        title: summary.movieTitle,
        showtime: showtimeText,
        showDate: summary.showDate,
        showTime: summary.showTime,
        showroom: summary.showroomName
    });
    backToSeats.href = `/booking.html?${bookingParams}`;

    const body = document.getElementById("ticketSummaryBody");
    body.replaceChildren();
    summary.tickets.forEach(ticket => {
        const row = document.createElement("tr");
        [
            ticketLabel(ticket.type),
            ticket.count,
            money(ticket.pricePerTicket),
            money(ticket.lineTotal)
        ].forEach(value => {
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
    if (useDifferentEmail.checked) differentEmail.focus();
}

function validEmail(value) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
}

async function handleEmailSubmit(event) {
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
    paymentBtn.textContent = "Saving...";
    try {
        await apiRequest("/api/checkout/confirmation-email", {
            method: "POST",
            body: JSON.stringify({ confirmationEmail: chosenEmail })
        });
        window.location.href = "/payment.html";
    } catch (error) {
        if (error.status === 401 || error.status === 403) {
            clearCurrentUser();
            window.location.href = `/login.html?redirect=${encodeURIComponent("/checkout.html")}`;
            return;
        }
        emailError.textContent = error.message || "Could not save the confirmation email.";
    } finally {
        paymentBtn.disabled = false;
        paymentBtn.textContent = "Continue to Payment";
    }
}

async function initCheckout() {
    try {
        [summary, profile] = await Promise.all([
            apiRequest("/api/checkout/summary"),
            apiRequest("/api/profile")
        ]);
    } catch (error) {
        if (error.status === 401 || error.status === 403) {
            clearCurrentUser();
            window.location.href = `/login.html?redirect=${encodeURIComponent("/checkout.html")}`;
            return;
        }
        if (error.status === 404) {
            showCheckoutError("No pending booking was found. Return to Movies and select a showtime.");
            return;
        }
        showCheckoutError(error.message || "Could not load checkout. Please try again.");
        return;
    }

    profileEmail = profile.email || "";
    accountEmail.textContent = profileEmail || "No account email available";
    useAccountEmail.disabled = !profileEmail;
    if (!profileEmail) {
        useDifferentEmail.checked = true;
        setEmailMode();
    }
    renderBooking();
    paymentBtn.disabled = false;
}

emailForm.addEventListener("submit", handleEmailSubmit);
useAccountEmail.addEventListener("change", setEmailMode);
useDifferentEmail.addEventListener("change", setEmailMode);
differentEmail.addEventListener("input", () => {
    emailError.textContent = "";
    differentEmail.classList.remove("invalid");
});

initCheckout();
