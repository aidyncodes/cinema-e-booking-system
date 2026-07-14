const form = document.getElementById("loginForm");
const submitBtn = document.getElementById("submitBtn");
const formBanner = document.getElementById("formBanner");

const fields = {
    email: { input: document.getElementById("email"), error: document.getElementById("emailError") },
    password: { input: document.getElementById("password"), error: document.getElementById("passwordError") }
};

const validators = {
    email: () => (fields.email.input.value.trim() ? "" : "Email is required."),
    password: () => (fields.password.input.value ? "" : "Password is required.")
};

function setFieldError(key, message) {
    if (!fields[key]) return;
    fields[key].error.textContent = message || "";
    fields[key].input.classList.toggle("invalid", Boolean(message));
}

function validateField(key) {
    const message = validators[key] ? validators[key]() : "";
    setFieldError(key, message);
    return !message;
}

function validateAll() {
    return Object.keys(validators)
        .map(validateField)
        .every(Boolean);
}

function showBanner(message) {
    formBanner.className = "form-banner error";
    formBanner.textContent = message;
}

function hideBanner() {
    formBanner.className = "form-banner hidden";
    formBanner.textContent = "";
}

Object.keys(validators).forEach(key => {
    fields[key].input.addEventListener("blur", () => validateField(key));
});

function redirectAfterLogin() {
    const target = new URLSearchParams(window.location.search).get("redirect");
    window.location.href = target || "/index.html";
}

async function handleSubmit(event) {
    event.preventDefault();
    hideBanner();

    if (!validateAll()) {
        return;
    }

    const payload = {
        email: fields.email.input.value.trim(),
        password: fields.password.input.value
    };

    submitBtn.disabled = true;
    submitBtn.textContent = "Logging in...";

    try {
        const user = await apiRequest("/api/auth/login", {
            method: "POST",
            body: JSON.stringify(payload)
        });
        setCurrentUser(user);
        redirectAfterLogin();
    } catch (error) {
        if (error.status === 400 && error.data && error.data.fields) {
            Object.entries(error.data.fields).forEach(([key, message]) => setFieldError(key, message));
        } else {
            showBanner(error.message || "Could not log in. Please try again.");
        }
        submitBtn.disabled = false;
        submitBtn.textContent = "Log In";
    }
}

form.addEventListener("submit", handleSubmit);
