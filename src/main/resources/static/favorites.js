const favoritesRoot = document.getElementById("favoritesRoot");

function clearNode(node) {
    while (node.firstChild) {
        node.removeChild(node.firstChild);
    }
}

function posterFor(movie) {
    return movie.posterUrl || `https://placehold.co/300x450/f3efe8/171717?text=${encodeURIComponent(movie.title || "Movie")}`;
}

function setMessage(className, message) {
    clearNode(favoritesRoot);
    const element = document.createElement("div");
    element.className = className;
    element.textContent = message;
    favoritesRoot.appendChild(element);
}

function buildFavoriteCard(movie) {
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
    posterLink.appendChild(poster);

    const removeBtn = document.createElement("button");
    removeBtn.type = "button";
    removeBtn.className = "favorite-btn active";
    removeBtn.textContent = "\u2665";
    removeBtn.title = "Remove from favorites";
    removeBtn.setAttribute("aria-label", `Remove ${movie.title} from favorites`);
    removeBtn.addEventListener("click", async event => {
        event.preventDefault();
        removeBtn.disabled = true;
        await toggleFavorite(movie.id);
        article.remove();
        if (!favoritesRoot.querySelector(".movie-card")) {
            setMessage("empty-state", "No favorite movies yet. Browse movies and tap the heart icon to add some.");
        }
    });

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
    [movie.genre || "Genre", movie.rating || "Not Rated"].forEach(text => {
        const pill = document.createElement("span");
        pill.className = "pill";
        pill.textContent = text;
        meta.appendChild(pill);
    });

    copy.append(title, meta);
    article.append(posterLink, removeBtn, copy);
    return article;
}

async function loadFavorites() {
    if (!isLoggedIn()) {
        window.location.href = `/login.html?redirect=${encodeURIComponent("/favorites.html")}`;
        return;
    }

    try {
        const favorites = await apiRequest("/api/profile/favorites");
        setFavoriteIds(favorites.map(movie => movie.id));
        clearNode(favoritesRoot);

        if (!favorites.length) {
            setMessage("empty-state", "No favorite movies yet. Browse movies and tap the heart icon to add some.");
            return;
        }

        const grid = document.createElement("div");
        grid.className = "movie-grid";
        favorites.forEach(movie => grid.appendChild(buildFavoriteCard(movie)));
        favoritesRoot.appendChild(grid);
    } catch (error) {
        setMessage("error-state", "Could not load your favorite movies.");
    }
}

loadFavorites();
