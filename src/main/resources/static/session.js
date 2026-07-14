
const CURRENT_USER_KEY = "ces_current_user";

function getCurrentUser() {
    try {
        const raw = localStorage.getItem(CURRENT_USER_KEY);
        return raw ? JSON.parse(raw) : null;
    } catch (error) {
        return null;
    }
}

function setCurrentUser(user) {
    localStorage.setItem(CURRENT_USER_KEY, JSON.stringify(user));
}

function clearCurrentUser() {
    localStorage.removeItem(CURRENT_USER_KEY);
}

function isLoggedIn() {
    return getCurrentUser() !== null;
}

async function apiRequest(url, options = {}) {
    const response = await fetch(url, {
        headers: { "Content-Type": "application/json", ...(options.headers || {}) },
        ...options
    });

    let data = null;
    try {
        data = await response.json();
    } catch (error) {
        data = null;
    }

    if (!response.ok) {
        const message = (data && data.message) || `Request failed: ${response.status}`;
        const error = new Error(message);
        error.status = response.status;
        error.data = data;
        throw error;
    }

    return data;
}

async function logout() {
    try {
        await fetch("/api/auth/logout", { method: "POST" });
    } catch (error) {
        // Still clear local state even if the network call failed.
    }
    clearCurrentUser();
    window.location.href = "/index.html";
}

// Favorites are stored server-side, with a small local cache for instant UI state.
let favoriteIdsCache = null;

function favoritesStorageKey() {
    const user = getCurrentUser();
    return user ? `ces_favorites_${user.id}` : null;
}

function getFavoriteIds() {
    const key = favoritesStorageKey();
    if (!key) return [];
    try {
        const raw = localStorage.getItem(key);
        return raw ? JSON.parse(raw) : [];
    } catch (error) {
        return [];
    }
}

function isFavorite(movieId) {
    return getFavoriteIds().includes(Number(movieId));
}

function setFavoriteIds(ids) {
    favoriteIdsCache = ids.map(Number);
    const key = favoritesStorageKey();
    if (key) {
        localStorage.setItem(key, JSON.stringify(favoriteIdsCache));
    }
}

async function loadFavoriteIds() {
    if (!isLoggedIn()) {
        favoriteIdsCache = [];
        return favoriteIdsCache;
    }

    const favorites = await apiRequest("/api/profile/favorites");
    setFavoriteIds(favorites.map(movie => movie.id));
    return favoriteIdsCache;
}

// Returns the new favorite state (true/false), or null if nobody is logged in.
async function toggleFavorite(movieId) {
    if (!isLoggedIn()) return null;

    const id = Number(movieId);
    const ids = favoriteIdsCache || getFavoriteIds();
    const nowFavorite = !ids.includes(id);

    if (nowFavorite) {
        await apiRequest(`/api/profile/favorites/${encodeURIComponent(id)}`, { method: "POST" });
        setFavoriteIds([...ids, id]);
    } else {
        await apiRequest(`/api/profile/favorites/${encodeURIComponent(id)}`, { method: "DELETE" });
        setFavoriteIds(ids.filter(value => value !== id));
    }

    return nowFavorite;
}
