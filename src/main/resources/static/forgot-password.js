const form = document.getElementById("forgotForm");
const submitBtn = document.getElementById("submitBtn");
const formBanner = document.getElementById("formBanner");
const emailInput = document.getElementById("email");
const emailError = document.getElementById("emailError");

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function validateEmail() {
    const value = emailInput.value.trim();
    let message = "";
    if (!value) {
        message = "Email is required.";
    } else if (!EMAIL_PATTERN.test(value)) {
        message = "Enter a valid email address.";
    }
    emailError.textContent = message;
    emailInput.classList.toggle("invalid", Boolean(message));
    return !message;
}

function showBanner(type, message) {
    formBanner.className = `form-banner ${type}`;
    formBanner.textContent = message;
}

function hideBanner() {
    formBanner.className = "form-banner hidden";
    formBanner.textContent = "";
}

emailInput.addEventListener("blur", validateEmail);

async function handleSubmit(event) {
    event.preventDefault();
    hideBanner();

    if (!validateEmail()) {
        return;
    }

    submitBtn.disabled = true;
    submitBtn.textContent = "Sending...";

    try {
        const response = await apiRequest("/api/auth/forgot-password", {
            method: "POST",
            body: JSON.stringify({ email: emailInput.value.trim() })
        });
        form.reset();
        form.classList.add("hidden");
        showBanner("success", response.message || "If an account exists for that email, a password reset link has been sent.");
    } catch (error) {
        if (error.status === 400 && error.data && error.data.fields && error.data.fields.email) {
            emailError.textContent = error.data.fields.email;
            emailInput.classList.add("invalid");
        } else {
            showBanner("error", error.message || "Something went wrong. Please try again.");
        }
        submitBtn.disabled = false;
        submitBtn.textContent = "Send Reset Link";
    }
}

form.addEventListener("submit", handleSubmit);
