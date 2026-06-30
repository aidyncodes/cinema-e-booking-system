// ── DOM refs ──
const movieTitle = document.getElementById('movieTitle');
const movieGenre = document.getElementById('movieGenre');
const movieRating = document.getElementById('movieRating');
const movieStatus = document.getElementById('movieStatus');
const moviePoster = document.getElementById('moviePoster');
const selectedShowtime = document.getElementById('selectedShowtime');

const adultCount = document.getElementById('adultCount');
const seniorCount = document.getElementById('seniorCount');
const childCount = document.getElementById('childCount');
const totalTickets = document.getElementById('totalTickets');
const totalPrice = document.getElementById('totalPrice');

const seatGrid = document.getElementById('seatGrid');
const resetSeatsBtn = document.getElementById('resetSeatsBtn');
const proceedBtn = document.getElementById('proceedBtn');

// ── Constants ──
const PRICES = { adult: 12.00, senior: 8.00, child: 6.00 };
const ROWS = 7;
const COLS = 10;
const TOTAL_SEATS = ROWS * COLS;

// ── State ──
let ticketCounts = { adult: 0, senior: 0, child: 0 };
let selectedSeats = new Set();
let occupiedSeats = new Set();

// ── Helper functions ──
function generateOccupiedSeats() {
    const count = Math.floor(Math.random() * 12) + 4;
    const seats = new Set();
    while (seats.size < count) {
        seats.add(Math.floor(Math.random() * TOTAL_SEATS));
    }
    return seats;
}

function seatLabel(index) {
    const row = String.fromCharCode(65 + Math.floor(index / COLS));
    const col = (index % COLS) + 1;
    return row + col;
}

function posterFor(url, title) {
    if (url) return url;
    return `https://placehold.co/300x450/f3efe8/171717?text=${encodeURIComponent(title || 'Movie')}`;
}

// ── Render seat grid ──
function renderSeats() {
    seatGrid.innerHTML = '';
    for (let i = 0; i < TOTAL_SEATS; i++) {
        const btn = document.createElement('button');
        const label = seatLabel(i);
        btn.className = 'seat';
        btn.dataset.index = i;
        btn.title = label;

        if (occupiedSeats.has(i)) {
            btn.classList.add('occupied');
            btn.disabled = true;
        } else if (selectedSeats.has(i)) {
            btn.classList.add('selected');
        } else {
            btn.classList.add('available');
        }

        btn.addEventListener('click', () => toggleSeat(i));
        seatGrid.appendChild(btn);
    }
}

function toggleSeat(index) {
    if (occupiedSeats.has(index)) return;
    const btn = seatGrid.querySelector(`[data-index="${index}"]`);
    if (!btn) return;

    if (selectedSeats.has(index)) {
        selectedSeats.delete(index);
        btn.classList.remove('selected');
        btn.classList.add('available');
    } else {
        selectedSeats.add(index);
        btn.classList.remove('available');
        btn.classList.add('selected');
    }
}

// ── Update ticket display ──
function updateTicketDisplay() {
    adultCount.textContent = ticketCounts.adult;
    seniorCount.textContent = ticketCounts.senior;
    childCount.textContent = ticketCounts.child;

    const total = ticketCounts.adult + ticketCounts.senior + ticketCounts.child;
    totalTickets.textContent = total;

    const price = ticketCounts.adult * PRICES.adult +
                  ticketCounts.senior * PRICES.senior +
                  ticketCounts.child * PRICES.child;
    totalPrice.textContent = `$${price.toFixed(2)}`;
}

function adjustTicket(type, delta) {
    ticketCounts[type] = Math.max(0, ticketCounts[type] + delta);
    updateTicketDisplay();
}

// ── Load movie data from URL ──
function loadMovieData() {
    const params = new URLSearchParams(window.location.search);
    const title = params.get('title') || 'Dune: Part Two';
    const showtime = params.get('showtime') || '2:00 PM';
    const genre = params.get('genre') || 'Sci-Fi';
    const rating = params.get('rating') || 'PG-13';
    const status = params.get('status') || 'CURRENTLY_RUNNING';
    const poster = params.get('poster') || '';

    movieTitle.textContent = title;
    movieGenre.textContent = genre || 'Genre';
    movieRating.textContent = rating || 'Not Rated';
    movieStatus.textContent = status === 'COMING_SOON' ? 'Coming Soon' : 'Now Showing';

    if (status === 'COMING_SOON') {
        movieStatus.classList.add('soon');
        movieStatus.classList.remove('status');
    } else {
        movieStatus.classList.remove('soon');
        movieStatus.classList.add('status');
    }

    selectedShowtime.textContent = showtime;
    moviePoster.src = posterFor(poster, title);
    moviePoster.alt = `${title} poster`;

    document.title = `Book ${title} - CES Cinema`;
}

// ── Proceed to checkout ──
function proceedToCheckout() {
    const total = ticketCounts.adult + ticketCounts.senior + ticketCounts.child;
    if (total === 0) {
        alert('Please select at least one ticket.');
        return;
    }
    if (selectedSeats.size === 0) {
        alert('Please select at least one seat.');
        return;
    }

    const params = new URLSearchParams(window.location.search);
    const title = params.get('title') || 'Dune: Part Two';
    const showtime = params.get('showtime') || '2:00 PM';
    const seatLabels = [...selectedSeats].map(i => seatLabel(i)).join(', ');
    const price = ticketCounts.adult * PRICES.adult +
                  ticketCounts.senior * PRICES.senior +
                  ticketCounts.child * PRICES.child;

    const confirmParams = new URLSearchParams();
    confirmParams.set('title', title);
    confirmParams.set('showtime', showtime);
    confirmParams.set('seats', seatLabels);
    confirmParams.set('total', price.toFixed(2));
    confirmParams.set('auditorium', 'Auditorium 1');
    confirmParams.set('confirmation', `CES-${Date.now().toString().slice(-8)}`);

    window.location.href = `/order-confirmation.html?${confirmParams.toString()}`;
}

// ── Event listeners ──
document.querySelectorAll('.qty-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        const type = btn.dataset.type;
        const dir = btn.dataset.dir;
        adjustTicket(type, dir === 'plus' ? 1 : -1);
    });
});

resetSeatsBtn.addEventListener('click', () => {
    selectedSeats.clear();
    renderSeats();
});

proceedBtn.addEventListener('click', proceedToCheckout);

// ── Init ──
occupiedSeats = generateOccupiedSeats();
loadMovieData();
renderSeats();
updateTicketDisplay();