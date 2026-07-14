
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

// ── Favorites (client-side only for now; movie cards call these helpers so
// the storage can be swapped for a real /api/favorites backend later without
// touching app.js / movie-details.js call sites) ──

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

// Returns the new favorite state (true/false), or null if nobody is logged in.
function toggleFavorite(movieId) {
    const key = favoritesStorageKey();
    if (!key) return null;

    const id = Number(movieId);
    const ids = getFavoriteIds();
    const index = ids.indexOf(id);
    let nowFavorite;

    if (index === -1) {
        ids.push(id);
        nowFavorite = true;
    } else {
        ids.splice(index, 1);
        nowFavorite = false;
    }

    localStorage.setItem(key, JSON.stringify(ids));
    return nowFavorite;
}
