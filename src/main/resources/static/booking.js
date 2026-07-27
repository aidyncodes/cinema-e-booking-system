const movieTitle = document.getElementById("movieTitle");
const movieGenre = document.getElementById("movieGenre");
const movieRating = document.getElementById("movieRating");
const movieStatus = document.getElementById("movieStatus");
const moviePoster = document.getElementById("moviePoster");
const selectedShowtime = document.getElementById("selectedShowtime");
const adultCount = document.getElementById("adultCount");
const seniorCount = document.getElementById("seniorCount");
const childCount = document.getElementById("childCount");
const totalTickets = document.getElementById("totalTickets");
const totalPrice = document.getElementById("totalPrice");
const seatGrid = document.getElementById("seatGrid");
const resetSeatsBtn = document.getElementById("resetSeatsBtn");
const proceedBtn = document.getElementById("proceedBtn");
const bookingMessage = document.getElementById("bookingMessage");

// Display prices, kept in sync with the server-side prices in BookingService.
const PRICES = { adult: 12.00, senior: 8.00, child: 6.00 };
const BOOKING_KEY = "ces_pending_booking";
const params = new URLSearchParams(window.location.search);
const showtimeId = params.get("showtimeId");

// Layout comes from the seat map endpoint; params are only a fallback for display.
let rows = boundedNumber(params.get("rows"), 7, 1, 26);
let cols = boundedNumber(params.get("seatsPerRow"), 10, 1, 20);

let ticketCounts = { adult: 0, senior: 0, child: 0 };
let selectedSeats = new Set();   // seat indices the user has chosen
let occupiedSeats = new Set();   // seat indices taken by someone else

function boundedNumber(value, fallback, min, max) {
    const number = Number(value);
    return Number.isInteger(number) && number >= min && number <= max ? number : fallback;
}

function totalSeatCount() {
    return rows * cols;
}

function bookingIdentity() {
    return showtimeId || [
        params.get("movieId"),
        params.get("showDate"),
        params.get("showTime"),
        params.get("showroomId")
    ].join(":");
}

function showBookingMessage(message, type = "error") {
    bookingMessage.className = `booking-message ${type}`;
    bookingMessage.textContent = message;
}

function hideBookingMessage() {
    bookingMessage.className = "booking-message hidden";
    bookingMessage.textContent = "";
}

function seatLabel(index) {
    const row = String.fromCharCode(65 + Math.floor(index / cols));
    return `${row}${(index % cols) + 1}`;
}

function indexForSeatLabel(label) {
    const match = /^([A-Z])(\d+)$/.exec(label);
    if (!match) return -1;
    const row = match[1].charCodeAt(0) - 65;
    const col = Number(match[2]) - 1;
    if (row < 0 || row >= rows || col < 0 || col >= cols) return -1;
    return row * cols + col;
}

function posterFor(url, title) {
    if (url) return url;
    return `https://placehold.co/300x450/f3efe8/171717?text=${encodeURIComponent(title || "Movie")}`;
}

function totalTicketCount() {
    return ticketCounts.adult + ticketCounts.senior + ticketCounts.child;
}

function calculateSubtotal() {
    return ticketCounts.adult * PRICES.adult
        + ticketCounts.senior * PRICES.senior
        + ticketCounts.child * PRICES.child;
}

function renderSeats() {
    seatGrid.replaceChildren();
    seatGrid.style.gridTemplateColumns = `repeat(${cols}, minmax(28px, 1fr))`;

    for (let index = 0; index < totalSeatCount(); index += 1) {
        const button = document.createElement("button");
        const label = seatLabel(index);
        button.type = "button";
        button.className = "seat";
        button.dataset.index = index;
        button.title = label;
        button.setAttribute("aria-label", `Seat ${label}`);
        button.textContent = label;

        if (occupiedSeats.has(index)) {
            button.classList.add("occupied");
            button.disabled = true;
            button.setAttribute("aria-label", `Seat ${label}, occupied`);
        } else if (selectedSeats.has(index)) {
            button.classList.add("selected");
            button.setAttribute("aria-pressed", "true");
        } else {
            button.classList.add("available");
            button.setAttribute("aria-pressed", "false");
        }

        button.addEventListener("click", () => toggleSeat(index));
        seatGrid.appendChild(button);
    }
}

function toggleSeat(index) {
    if (occupiedSeats.has(index)) return;
    hideBookingMessage();

    if (selectedSeats.has(index)) {
        selectedSeats.delete(index);
    } else {
        const ticketTotal = totalTicketCount();
        if (ticketTotal === 0) {
            showBookingMessage("Choose at least one ticket before selecting seats.");
            return;
        }
        if (selectedSeats.size >= ticketTotal) {
            showBookingMessage(`You selected ${ticketTotal} ticket${ticketTotal === 1 ? "" : "s"}. Remove a seat or add another ticket.`);
            return;
        }
        selectedSeats.add(index);
    }
    renderSeats();
}

function updateTicketDisplay() {
    adultCount.textContent = ticketCounts.adult;
    seniorCount.textContent = ticketCounts.senior;
    childCount.textContent = ticketCounts.child;
    totalTickets.textContent = totalTicketCount();
    totalPrice.textContent = `$${calculateSubtotal().toFixed(2)}`;
}

function adjustTicket(type, delta) {
    hideBookingMessage();
    const nextCount = Math.max(0, ticketCounts[type] + delta);
    const nextTotal = totalTicketCount() - ticketCounts[type] + nextCount;
    if (nextTotal < selectedSeats.size) {
        showBookingMessage("Remove a selected seat before reducing the ticket quantity.");
        return;
    }
    ticketCounts[type] = nextCount;
    updateTicketDisplay();
}

function loadMovieData() {
    const title = params.get("title") || "Movie";
    const showtime = params.get("showtime") || "Showtime unavailable";
    const genre = params.get("genre") || "Genre";
    const rating = params.get("rating") || "Not Rated";
    const status = params.get("status") || "CURRENTLY_RUNNING";
    const poster = params.get("poster") || "";
    const showroom = params.get("showroom") || "Showroom";

    movieTitle.textContent = title;
    movieGenre.textContent = genre;
    movieRating.textContent = rating;
    movieStatus.textContent = status === "COMING_SOON" ? "Coming Soon" : "Now Showing";
    movieStatus.classList.toggle("soon", status === "COMING_SOON");
    movieStatus.classList.toggle("status", status !== "COMING_SOON");
    selectedShowtime.textContent = `${showtime} · ${showroom}`;
    moviePoster.src = posterFor(poster, title);
    moviePoster.alt = `${title} poster`;
    document.title = `Book ${title} - CES Cinema`;
}

// Pulls the real seat map from the backend: showroom layout plus which seats are
// already held/booked. Seats this session is holding come back flagged "mine".
async function loadSeatMap() {
    const map = await apiRequest(`/api/showtimes/${encodeURIComponent(showtimeId)}/seats`);

    rows = map.showroom.rowCount;
    cols = map.showroom.seatsPerRow;

    occupiedSeats = new Set();
    const mine = [];
    (map.unavailableSeats || []).forEach(seat => {
        const index = indexForSeatLabel(seat.seatLabel);
        if (index < 0) return;
        if (seat.mine) {
            mine.push(index);
        } else {
            occupiedSeats.add(index);
        }
    });
    selectedSeats = new Set(mine);
}

// Restores the ticket quantities the user picked earlier (e.g. after a page
// reload) from the pending-booking cache, as long as it is the same showtime.
function restoreTicketCounts() {
    try {
        const stored = JSON.parse(sessionStorage.getItem(BOOKING_KEY));
        if (!stored || stored.identity !== bookingIdentity()) return;
        ticketCounts = {
            adult: Number(stored.tickets && stored.tickets.adult) || 0,
            senior: Number(stored.tickets && stored.tickets.senior) || 0,
            child: Number(stored.tickets && stored.tickets.child) || 0
        };
    } catch (error) {
        sessionStorage.removeItem(BOOKING_KEY);
    }
}

function pendingBooking() {
    const seats = [...selectedSeats].sort((left, right) => left - right).map(seatLabel);
    return {
        identity: bookingIdentity(),
        movieId: params.get("movieId"),
        showtimeId,
        title: params.get("title") || "Movie",
        showtime: params.get("showtime") || "",
        showDate: params.get("showDate") || "",
        showTime: params.get("showTime") || "",
        showroomId: params.get("showroomId") || "",
        auditorium: params.get("showroom") || "Showroom",
        rows,
        seatsPerRow: cols,
        tickets: { ...ticketCounts },
        prices: { ...PRICES },
        seats,
        subtotal: calculateSubtotal(),
        bookingUrl: `${window.location.pathname}${window.location.search}`
    };
}

async function proceedToCheckout() {
    hideBookingMessage();
    const ticketTotal = totalTicketCount();
    if (ticketTotal === 0) {
        showBookingMessage("Please select at least one ticket.");
        return;
    }
    if (selectedSeats.size !== ticketTotal) {
        showBookingMessage(`Select exactly ${ticketTotal} seat${ticketTotal === 1 ? "" : "s"} before checkout.`);
        return;
    }

    proceedBtn.disabled = true;
    const seatLabels = [...selectedSeats].sort((left, right) => left - right).map(seatLabel);

    try {
        // Reserve the seats server-side before moving on. This holds them against
        // the current session so they survive the login step at checkout.
        await apiRequest(`/api/showtimes/${encodeURIComponent(showtimeId)}/hold`, {
            method: "POST",
            body: JSON.stringify({
                seats: seatLabels,
                adultCount: ticketCounts.adult,
                seniorCount: ticketCounts.senior,
                childCount: ticketCounts.child
            })
        });
    } catch (error) {
        proceedBtn.disabled = false;
        // Someone grabbed a seat first - refresh the map so the user can re-pick.
        if (error.status === 409) {
            showBookingMessage(error.message || "Some seats were just taken. Please choose again.");
            await refreshSeatMap();
            return;
        }
        showBookingMessage(error.message || "Could not hold your seats. Please try again.");
        return;
    }

    sessionStorage.setItem(BOOKING_KEY, JSON.stringify(pendingBooking()));

    // Checkout requires login; the held seats stay reserved under this session.
    if (!isLoggedIn()) {
        window.location.href = `/login.html?redirect=${encodeURIComponent("/checkout.html")}`;
        return;
    }
    window.location.href = "/checkout.html";
}

async function refreshSeatMap() {
    try {
        await loadSeatMap();
    } catch (error) {
        showBookingMessage("Could not refresh the seat map. Please reload the page.");
        return;
    }
    renderSeats();
    updateTicketDisplay();
}

document.querySelectorAll(".qty-btn").forEach(button => {
    button.addEventListener("click", () => {
        adjustTicket(button.dataset.type, button.dataset.dir === "plus" ? 1 : -1);
    });
});

resetSeatsBtn.addEventListener("click", () => {
    selectedSeats.clear();
    hideBookingMessage();
    renderSeats();
});
proceedBtn.addEventListener("click", proceedToCheckout);

async function init() {
    loadMovieData();

    if (!showtimeId) {
        showBookingMessage("This showtime is unavailable. Please pick a showtime from the movie page.");
        proceedBtn.disabled = true;
        return;
    }

    try {
        await loadSeatMap();
    } catch (error) {
        if (error.status === 404) {
            showBookingMessage("This showtime could not be found.");
        } else {
            showBookingMessage("Could not load the seat map. Please reload the page.");
        }
        proceedBtn.disabled = true;
        return;
    }

    restoreTicketCounts();
    renderSeats();
    updateTicketDisplay();
}

init();
