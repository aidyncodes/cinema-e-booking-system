
function renderNavAuth() {
    const slot = document.getElementById("navAuthSlot");
    if (!slot) return;

    while (slot.firstChild) {
        slot.removeChild(slot.firstChild);
    }
    const user = getCurrentUser();

    if (user) {
        const profileLink = document.createElement("a");
        profileLink.className = "nav-link";
        profileLink.href = "/profile.html";
        profileLink.textContent = user.firstName ? `Hi, ${user.firstName}` : "Profile";

        const logoutBtn = document.createElement("button");
        logoutBtn.type = "button";
        logoutBtn.className = "nav-link nav-logout-btn";
        logoutBtn.textContent = "Logout";
        logoutBtn.addEventListener("click", logout);

        slot.append(profileLink, logoutBtn);
    } else {
        const loginLink = document.createElement("a");
        loginLink.className = "nav-link";
        loginLink.href = "/login.html";
        loginLink.textContent = "Login";

        const registerLink = document.createElement("a");
        registerLink.className = "nav-link nav-register-link";
        registerLink.href = "/register.html";
        registerLink.textContent = "Sign Up";

        slot.append(loginLink, registerLink);
    }
}

renderNavAuth();
