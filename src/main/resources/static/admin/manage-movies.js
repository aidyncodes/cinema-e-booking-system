const movieForm = document.getElementById("movieForm");
const movieSubmitBtn = document.getElementById("movieSubmitBtn");
const movieFormBanner = document.getElementById("movieFormBanner");

const movieFields = {
    title: document.getElementById("title"),
    genre: document.getElementById("genre"),
    status: document.getElementById("status"),
    rating: document.getElementById("rating"),
    description: document.getElementById("description"),
    posterUrl: document.getElementById("posterUrl"),
    trailerUrl: document.getElementById("trailerUrl")
};

function setMovieFieldError(name, message) {
    const input = movieFields[name];
    const error = document.getElementById(`${name}Error`);
    if (!input || !error) return;
    input.classList.toggle("invalid", Boolean(message));
    input.setAttribute("aria-invalid", message ? "true" : "false");
    error.textContent = message || "";
}

function clearMovieErrors() {
    Object.keys(movieFields).forEach(name => setMovieFieldError(name, ""));
}

function showMovieBanner(message, type) {
    movieFormBanner.className = `admin-form-banner ${type}`;
    movieFormBanner.textContent = message;
}

function hideMovieBanner() {
    movieFormBanner.className = "admin-form-banner hidden";
    movieFormBanner.textContent = "";
}

function validOptionalUrl(value) {
    if (!value) return true;
    try {
        const url = new URL(value);
        return url.protocol === "http:" || url.protocol === "https:";
    } catch (error) {
        return false;
    }
}

function validateMovieForm() {
    clearMovieErrors();
    let valid = true;

    if (!movieFields.title.value.trim()) {
        setMovieFieldError("title", "Title is required.");
        valid = false;
    }
    if (!movieFields.genre.value.trim()) {
        setMovieFieldError("genre", "Genre is required.");
        valid = false;
    }
    if (!movieFields.status.value) {
        setMovieFieldError("status", "Release status is required.");
        valid = false;
    }
    if (!validOptionalUrl(movieFields.posterUrl.value.trim())) {
        setMovieFieldError("posterUrl", "Enter a valid HTTP or HTTPS URL.");
        valid = false;
    }
    if (!validOptionalUrl(movieFields.trailerUrl.value.trim())) {
        setMovieFieldError("trailerUrl", "Enter a valid HTTP or HTTPS URL.");
        valid = false;
    }

    return valid;
}

function moviePayload() {
    const payload = {
        title: movieFields.title.value.trim(),
        genre: movieFields.genre.value.trim(),
        status: movieFields.status.value,
        rating: movieFields.rating.value.trim() || null,
        description: movieFields.description.value.trim() || null,
        posterUrl: movieFields.posterUrl.value.trim() || null,
        trailerUrl: movieFields.trailerUrl.value.trim() || null,
        showtimes: []
    };
    return payload;
}

movieForm.addEventListener("reset", () => {
    clearMovieErrors();
    hideMovieBanner();
});

movieForm.addEventListener("submit", async event => {
    event.preventDefault();
    hideMovieBanner();

    if (!validateMovieForm()) return;

    movieSubmitBtn.disabled = true;
    movieSubmitBtn.textContent = "Adding...";

    try {
        const created = await apiRequest("/api/admin/movies", {
            method: "POST",
            body: JSON.stringify(moviePayload())
        });
        movieForm.reset();
        clearMovieErrors();
        showMovieBanner(`“${created.title}” was added successfully.`, "success");
    } catch (error) {
        if (error.status === 400 && error.data && error.data.fields) {
            Object.entries(error.data.fields).forEach(([name, message]) => {
                setMovieFieldError(name, message);
            });
            showMovieBanner("Please correct the highlighted fields.", "error");
        } else {
            showMovieBanner(error.message || "Could not add the movie.", "error");
        }
    } finally {
        movieSubmitBtn.disabled = false;
        movieSubmitBtn.textContent = "Add Movie";
    }
});
