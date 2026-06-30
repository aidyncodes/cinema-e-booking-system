const movieSections = document.getElementById("movieSections");
const resultSummary = document.getElementById("resultSummary");
const searchForm = document.getElementById("searchForm");
const searchInput = document.getElementById("searchInput");
const filterMenu = document.getElementById("filterMenu");
const filterSummary = document.getElementById("filterSummary");
const genreFilter = document.getElementById("genreFilter");
const dateFilter = document.getElementById("dateFilter");
const clearFilters = document.getElementById("clearFilters");
const tabNowPlaying = document.getElementById("tabNowPlaying");
const tabComingSoon = document.getElementById("tabComingSoon");

const state = {
    currentlyRunning: [],
    comingSoon: [],
    genres: [],
    activeTab: "now-playing"
};

function clearNode(node) {
    while (node.firstChild) {
        node.removeChild(node.firstChild);
    }
}

function fetchJson(url) {
    return fetch(url).then(response => {
        if (!response.ok) {
            throw new Error(`Request failed: ${response.status}`);
        }
        return response.json();
    });
}

function posterFor(movie) {
    if (movie.posterUrl) {
        return movie.posterUrl;
    }
    return `https://placehold.co/300x450/f3efe8/171717?text=${encodeURIComponent(movie.title || "Movie")}`;
}

function statusLabel(status) {
    return status === "COMING_SOON" ? "Coming Soon" : "Now Showing";
}

function setMessage(className, message) {
    clearNode(movieSections);
    const element = document.createElement("div");
    element.className = className;
    element.textContent = message;
    movieSections.appendChild(element);
}

function updateFilterSummary() {
    filterSummary.textContent = genreFilter.value ? `Genre: ${genreFilter.value}` : "Filters";
}

function renderGenreOptions(genres) {
    const currentValue = genreFilter.value;
    clearNode(genreFilter);

    const allOption = document.createElement("option");
    allOption.value = "";
    allOption.textContent = "All genres";
    genreFilter.appendChild(allOption);

    genres.forEach(genre => {
        const option = document.createElement("option");
        option.value = genre;
        option.textContent = genre;
        genreFilter.appendChild(option);
    });

    genreFilter.value = currentValue;
}

function buildMovieCard(movie) {
    const article = document.createElement("article");
    article.className = "movie-card";

    const detailsHref = `/movie-details.html?id=${encodeURIComponent(movie.id)}`;

    const posterLink = document.createElement("a");
    posterLink.className = "poster-link";
    posterLink.href = detailsHref;
    posterLink.setAttribute("aria-label", `Open details for ${movie.title}`);

    const poster = document.createElement("img");
    poster.className = "movie-poster";
    poster.src = posterFor(movie);
    poster.alt = `${movie.title} poster`;
    poster.loading = "lazy";
    poster.addEventListener("error", () => {
        poster.src = `https://placehold.co/300x450/f3efe8/171717?text=${encodeURIComponent(movie.title || "Movie")}`;
    }, { once: true });
    posterLink.appendChild(poster);

    const copy = document.createElement("div");
    copy.className = "movie-copy";

    const title = document.createElement("h3");
    title.className = "movie-title";
    const titleLink = document.createElement("a");
    titleLink.href = detailsHref;
    titleLink.textContent = movie.title;
    title.appendChild(titleLink);

    const meta = document.createElement("div");
    meta.className = "meta-row";

    const genre = document.createElement("span");
    genre.className = "pill";
    genre.textContent = movie.genre || "Genre";
    meta.appendChild(genre);

    const rating = document.createElement("span");
    rating.className = "pill";
    rating.textContent = movie.rating || "Not Rated";
    meta.appendChild(rating);

    const status = document.createElement("span");
    status.className = movie.status === "COMING_SOON" ? "pill soon" : "pill status";
    status.textContent = statusLabel(movie.status);
    meta.appendChild(status);

    const link = document.createElement("a");
    link.className = "details-link";
    link.href = detailsHref;
    link.textContent = "Details";

    copy.append(title, meta, link);
    article.append(posterLink, copy);
    return article;
}

function buildSection(title, movies) {
    const section = document.createElement("section");
    section.className = "movie-section";

    const header = document.createElement("div");
    header.className = "section-header";

    const heading = document.createElement("h2");
    heading.textContent = title;

    const count = document.createElement("span");
    count.className = "section-count";
    count.textContent = `${movies.length} ${movies.length === 1 ? "movie" : "movies"}`;

    header.append(heading, count);

    const grid = document.createElement("div");
    grid.className = "movie-grid";
    movies.forEach(movie => grid.appendChild(buildMovieCard(movie)));

    section.append(header, grid);
    return section;
}

function groupByGenre(movies) {
    return movies.reduce((groups, movie) => {
        const genre = movie.genre || "Other";
        if (!groups.has(genre)) {
            groups.set(genre, []);
        }
        groups.get(genre).push(movie);
        return groups;
    }, new Map());
}

function setActiveTab(tab) {
    state.activeTab = tab;
    tabNowPlaying.classList.toggle("active", tab === "now-playing");
    tabComingSoon.classList.toggle("active", tab === "coming-soon");
}

function renderHome() {
    clearNode(movieSections);
    resultSummary.textContent = "";

    if (state.activeTab === "coming-soon") {
        if (state.comingSoon.length) {
            movieSections.appendChild(buildSection("Coming Soon", state.comingSoon));
        } else {
            setMessage("empty-state", "No upcoming movies.");
        }
        return;
    }

    const grouped = groupByGenre(state.currentlyRunning);
    const orderedGenres = [
        ...state.genres.filter(genre => grouped.has(genre)),
        ...[...grouped.keys()].filter(genre => !state.genres.includes(genre)).sort((a, b) => a.localeCompare(b))
    ];

    orderedGenres.forEach(genre => {
        movieSections.appendChild(buildSection(genre, grouped.get(genre)));
    });

    if (!movieSections.children.length) {
        setMessage("empty-state", "No movies found.");
    }
}

function renderSingleSection(title, movies, summary) {
    clearNode(movieSections);
    resultSummary.textContent = summary;

    if (!movies.length) {
        setMessage("empty-state", "No movies found.");
        return;
    }

    movieSections.appendChild(buildSection(title, movies));
}

async function loadHome() {
    setMessage("loading-state", "Loading movies...");
    resultSummary.textContent = "";

    try {
        const [currentlyRunning, comingSoon, genres] = await Promise.all([
            fetchJson("/api/movies"),
            fetchJson("/api/movies/coming-soon"),
            fetchJson("/api/movies/genres")
        ]);

        state.currentlyRunning = currentlyRunning;
        state.comingSoon = comingSoon;
        state.genres = genres;

        renderGenreOptions(genres);
        updateFilterSummary();
        renderHome();
    } catch (error) {
        setMessage("error-state", "Could not load movies from the database.");
    }
}

async function applyGenreFilter() {
    const genre = genreFilter.value;
    searchInput.value = "";
    updateFilterSummary();

    if (!genre) {
        filterMenu.removeAttribute("open");
        renderHome();
        return;
    }

    setMessage("loading-state", "Loading movies...");
    try {
        const movies = await fetchJson(`/api/movies?genre=${encodeURIComponent(genre)}`);
        renderSingleSection(genre, movies, `Showing ${movies.length} ${movies.length === 1 ? "movie" : "movies"} in ${genre}.`);
        filterMenu.removeAttribute("open");
    } catch (error) {
        setMessage("error-state", "Could not apply the genre filter.");
    }
}

async function runSearch(event) {
    event.preventDefault();
    const title = searchInput.value.trim();
    genreFilter.value = "";
    updateFilterSummary();

    if (!title) {
        renderHome();
        return;
    }

    setMessage("loading-state", "Searching movies...");
    try {
        const movies = await fetchJson(`/api/movies/search?title=${encodeURIComponent(title)}`);
        renderSingleSection("Search Results", movies, `Showing ${movies.length} ${movies.length === 1 ? "result" : "results"} for "${title}".`);
    } catch (error) {
        setMessage("error-state", "Could not search movies.");
    }
}

function resetFilters() {
    genreFilter.value = "";
    dateFilter.value = "";
    searchInput.value = "";
    updateFilterSummary();
    filterMenu.removeAttribute("open");
    renderHome();
}

searchForm.addEventListener("submit", runSearch);
genreFilter.addEventListener("change", applyGenreFilter);
dateFilter.addEventListener("change", updateFilterSummary);
clearFilters.addEventListener("click", resetFilters);

tabNowPlaying.addEventListener("click", () => {
    setActiveTab("now-playing");
    searchInput.value = "";
    genreFilter.value = "";
    updateFilterSummary();
    renderHome();
});

tabComingSoon.addEventListener("click", () => {
    setActiveTab("coming-soon");
    searchInput.value = "";
    genreFilter.value = "";
    updateFilterSummary();
    renderHome();
});

loadHome();
