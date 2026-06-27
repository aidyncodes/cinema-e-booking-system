const detailsRoot = document.getElementById("detailsRoot");

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

function toEmbedUrl(value) {
    if (!value) {
        return "";
    }

    try {
        const url = new URL(value, window.location.origin);
        const host = url.hostname.replace(/^www\./, "");

        if (host === "youtube.com" || host === "m.youtube.com") {
            if (url.pathname.startsWith("/embed/")) {
                return url.href;
            }

            const videoId = url.searchParams.get("v");
            if (videoId) {
                return `https://www.youtube.com/embed/${encodeURIComponent(videoId)}`;
            }
        }

        if (host === "youtu.be") {
            const videoId = url.pathname.split("/").filter(Boolean)[0];
            if (videoId) {
                return `https://www.youtube.com/embed/${encodeURIComponent(videoId)}`;
            }
        }

        return url.href;
    } catch (error) {
        return value;
    }
}

function setMessage(className, message) {
    clearNode(detailsRoot);
    detailsRoot.className = className;
    detailsRoot.textContent = message;
}

function createPill(text, className = "pill") {
    const pill = document.createElement("span");
    pill.className = className;
    pill.textContent = text;
    return pill;
}

function renderShowtimes(showtimes) {
    const section = document.createElement("section");
    section.className = "showtimes";

    const heading = document.createElement("h2");
    heading.textContent = "Showtimes";

    const list = document.createElement("div");
    list.className = "showtime-list";

    if (Array.isArray(showtimes) && showtimes.length) {
        showtimes.forEach(value => {
            const item = document.createElement("span");
            item.className = "showtime";
            item.textContent = value;
            list.appendChild(item);
        });
    } else {
        const empty = document.createElement("p");
        empty.className = "empty-state";
        empty.textContent = "No showtimes listed.";
        list.appendChild(empty);
    }

    section.append(heading, list);
    return section;
}

function renderTrailer(movie) {
    const trailerUrl = toEmbedUrl(movie.trailerUrl);
    const section = document.createElement("section");
    section.className = "trailer-section";

    const heading = document.createElement("h2");
    heading.textContent = "Trailer";
    section.appendChild(heading);

    if (!trailerUrl) {
        const empty = document.createElement("div");
        empty.className = "empty-state";
        empty.textContent = "Trailer unavailable.";
        section.appendChild(empty);
        return section;
    }

    const frameWrap = document.createElement("div");
    frameWrap.className = "trailer-frame";

    const iframe = document.createElement("iframe");
    iframe.src = trailerUrl;
    iframe.title = `${movie.title} trailer`;
    iframe.loading = "lazy";
    iframe.allow = "accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share";
    iframe.allowFullscreen = true;

    frameWrap.appendChild(iframe);
    section.appendChild(frameWrap);
    return section;
}

function renderMovie(movie) {
    document.title = `${movie.title} - CES Cinema`;
    clearNode(detailsRoot);
    detailsRoot.className = "detail-layout";

    const poster = document.createElement("img");
    poster.className = "detail-poster";
    poster.src = posterFor(movie);
    poster.alt = `${movie.title} poster`;
    poster.addEventListener("error", () => {
        poster.src = `https://placehold.co/300x450/f3efe8/171717?text=${encodeURIComponent(movie.title || "Movie")}`;
    }, { once: true });

    const copy = document.createElement("div");
    copy.className = "detail-copy";

    const eyebrow = document.createElement("p");
    eyebrow.className = "eyebrow";
    eyebrow.textContent = statusLabel(movie.status);

    const title = document.createElement("h1");
    title.textContent = movie.title;

    const meta = document.createElement("div");
    meta.className = "meta-row";
    meta.append(
        createPill(movie.genre || "Genre"),
        createPill(movie.rating || "Not Rated"),
        createPill(statusLabel(movie.status), movie.status === "COMING_SOON" ? "pill soon" : "pill status")
    );

    const description = document.createElement("p");
    description.className = "description";
    description.textContent = movie.description || "Description unavailable.";

    copy.append(
        eyebrow,
        title,
        meta,
        description,
        renderShowtimes(movie.showtimes),
        renderTrailer(movie)
    );

    detailsRoot.append(poster, copy);
}

async function loadMovie() {
    const id = new URLSearchParams(window.location.search).get("id");

    if (!id) {
        setMessage("error-state", "Movie id is missing.");
        return;
    }

    try {
        const movie = await fetchJson(`/api/movies/${encodeURIComponent(id)}`);
        renderMovie(movie);
    } catch (error) {
        setMessage("error-state", "Could not load this movie.");
    }
}

loadMovie();
