const showtimeForm = document.getElementById("showtimeForm");
const showtimeSubmitBtn = document.getElementById("showtimeSubmitBtn");
const showtimeFormBanner = document.getElementById("showtimeFormBanner");
const showtimesBody = document.getElementById("showtimesBody");
const refreshShowtimesBtn = document.getElementById("refreshShowtimesBtn");
const movieSelect = document.getElementById("movieId");
const dateInput = document.getElementById("date");
const timeInput = document.getElementById("time");
const showroomSelect = document.getElementById("showroomId");
const showroomHint = document.getElementById("showroomHint");

const showtimeFields = {
    movieId: movieSelect,
    date: dateInput,
    time: timeInput,
    showroomId: showroomSelect
};

let showroomOptions = [];

function localDateValue(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
}

function setShowtimeFieldError(name, message) {
    const input = showtimeFields[name];
    const error = document.getElementById(`${name}Error`);
    if (!input || !error) return;
    input.classList.toggle("invalid", Boolean(message));
    input.setAttribute("aria-invalid", message ? "true" : "false");
    error.textContent = message || "";
}

function clearShowtimeErrors() {
    Object.keys(showtimeFields).forEach(name => setShowtimeFieldError(name, ""));
}

function showShowtimeBanner(message, type) {
    showtimeFormBanner.className = `admin-form-banner ${type}`;
    showtimeFormBanner.textContent = message;
}

function hideShowtimeBanner() {
    showtimeFormBanner.className = "admin-form-banner hidden";
    showtimeFormBanner.textContent = "";
}

function validateShowtimeForm() {
    clearShowtimeErrors();
    let valid = true;

    if (!movieSelect.value) {
        setShowtimeFieldError("movieId", "Select a movie.");
        valid = false;
    }
    if (!dateInput.value) {
        setShowtimeFieldError("date", "Select a date.");
        valid = false;
    } else if (dateInput.value < dateInput.min) {
        setShowtimeFieldError("date", "Showtime date cannot be in the past.");
        valid = false;
    }
    if (!timeInput.value) {
        setShowtimeFieldError("time", "Select a time.");
        valid = false;
    }
    if (!showroomSelect.value) {
        setShowtimeFieldError("showroomId", "Select a showroom.");
        valid = false;
    }

    return valid;
}

function populateSelect(select, items, placeholder, labelFor) {
    select.replaceChildren();
    const empty = document.createElement("option");
    empty.value = "";
    empty.textContent = placeholder;
    select.appendChild(empty);

    items.forEach(item => {
        const option = document.createElement("option");
        option.value = item.id;
        option.textContent = labelFor(item);
        select.appendChild(option);
    });
}

function formatDate(value) {
    if (!value) return "";
    const [year, month, day] = value.split("-").map(Number);
    return new Intl.DateTimeFormat("en-US", {
        year: "numeric",
        month: "short",
        day: "numeric"
    }).format(new Date(year, month - 1, day));
}

function formatTime(value) {
    if (!value) return "";
    const [hour, minute] = value.split(":").map(Number);
    return new Intl.DateTimeFormat("en-US", {
        hour: "numeric",
        minute: "2-digit"
    }).format(new Date(2000, 0, 1, hour, minute));
}

function tableState(message) {
    showtimesBody.replaceChildren();
    const row = document.createElement("tr");
    const cell = document.createElement("td");
    cell.colSpan = 4;
    cell.className = "admin-table-state";
    cell.textContent = message;
    row.appendChild(cell);
    showtimesBody.appendChild(row);
}

function renderShowtimes(showtimes) {
    showtimesBody.replaceChildren();
    if (!showtimes.length) {
        tableState("No showtimes have been scheduled.");
        return;
    }

    showtimes.forEach(showtime => {
        const row = document.createElement("tr");
        [
            showtime.movieTitle,
            formatDate(showtime.date),
            formatTime(showtime.time),
            showtime.showroom ? showtime.showroom.name : ""
        ].forEach(value => {
            const cell = document.createElement("td");
            cell.textContent = value || "—";
            row.appendChild(cell);
        });
        showtimesBody.appendChild(row);
    });
}

async function loadFormOptions() {
    try {
        const [movies, showrooms] = await Promise.all([
            apiRequest("/api/admin/showtimes/movies"),
            apiRequest("/api/admin/showtimes/showrooms")
        ]);
        showroomOptions = showrooms;
        populateSelect(movieSelect, movies, "Select a movie", movie => {
            const status = movie.status === "COMING_SOON" ? "Coming Soon" : "Currently Running";
            return `${movie.title} — ${status}`;
        });
        populateSelect(showroomSelect, showrooms, "Select a showroom", showroom => {
            return `${showroom.name} (${showroom.rowCount} × ${showroom.seatsPerRow} seats)`;
        });
    } catch (error) {
        movieSelect.innerHTML = '<option value="">Could not load movies</option>';
        showroomSelect.innerHTML = '<option value="">Could not load showrooms</option>';
        showShowtimeBanner(error.message || "Could not load scheduling options.", "error");
    }
}

async function loadShowtimes() {
    tableState("Loading showtimes...");
    refreshShowtimesBtn.disabled = true;
    try {
        const showtimes = await apiRequest("/api/admin/showtimes");
        renderShowtimes(showtimes);
    } catch (error) {
        tableState(error.message || "Could not load showtimes.");
    } finally {
        refreshShowtimesBtn.disabled = false;
    }
}

showroomSelect.addEventListener("change", () => {
    const selected = showroomOptions.find(item => String(item.id) === showroomSelect.value);
    showroomHint.textContent = selected
        ? `${selected.name} has ${selected.rowCount * selected.seatsPerRow} seats.`
        : "";
});

showtimeForm.addEventListener("reset", () => {
    clearShowtimeErrors();
    hideShowtimeBanner();
    showroomHint.textContent = "";
    dateInput.min = localDateValue(new Date());
});

showtimeForm.addEventListener("submit", async event => {
    event.preventDefault();
    hideShowtimeBanner();

    if (!validateShowtimeForm()) return;

    showtimeSubmitBtn.disabled = true;
    showtimeSubmitBtn.textContent = "Scheduling...";

    try {
        const created = await apiRequest("/api/admin/showtimes", {
            method: "POST",
            body: JSON.stringify({
                movieId: Number(movieSelect.value),
                date: dateInput.value,
                time: timeInput.value,
                showroomId: Number(showroomSelect.value)
            })
        });
        showtimeForm.reset();
        clearShowtimeErrors();
        showShowtimeBanner(
            `${created.movieTitle} was scheduled in ${created.showroom.name} on ${formatDate(created.date)} at ${formatTime(created.time)}.`,
            "success"
        );
        await loadShowtimes();
    } catch (error) {
        if (error.status === 409) {
            setShowtimeFieldError("showroomId", "This showroom is already booked at the selected date and time.");
            showShowtimeBanner(error.message || "Scheduling conflict. Choose another showroom or time.", "error");
        } else if (error.status === 400 && error.data && error.data.fields) {
            Object.entries(error.data.fields).forEach(([name, message]) => {
                setShowtimeFieldError(name, message);
            });
            showShowtimeBanner("Please correct the highlighted fields.", "error");
        } else {
            showShowtimeBanner(error.message || "Could not schedule the showtime.", "error");
        }
    } finally {
        showtimeSubmitBtn.disabled = false;
        showtimeSubmitBtn.textContent = "Add Showtime";
    }
});

refreshShowtimesBtn.addEventListener("click", loadShowtimes);

dateInput.min = localDateValue(new Date());
loadFormOptions();
loadShowtimes();
