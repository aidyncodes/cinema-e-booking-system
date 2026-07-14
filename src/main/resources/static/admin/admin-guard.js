(function guardAdminPage() {
    const user = typeof getCurrentUser === "function" ? getCurrentUser() : null;
    if (!user) {
        window.location.href = `/login.html?redirect=${encodeURIComponent(window.location.pathname)}`;
        return;
    }
    if (user.role !== "ADMIN") {
        window.location.href = "/index.html";
    }
})();
