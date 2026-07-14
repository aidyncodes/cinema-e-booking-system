const form = document.getElementById("resetForm");
const submitBtn = document.getElementById("submitBtn");
const formBanner = document.getElementById("formBanner");

const fields = {
    newPassword: { input: document.getElementById("newPassword"), error: document.getElementById("newPasswordError") },
    confirmPassword: { input: document.getElementById("confirmPassword"), error: document.getElementById("confirmPasswordError") }
};

const validators = {
    newPassword: () => {
        const value = fields.newPassword.input.value;
        if (!value) return "New password is required.";
        if (value.length < 8) return "Password must be at least 8 characters.";
        return "";
    },
    confirmPassword: () => {
        const value = fields.confirmPassword.input.value;
        if (!value) return "Please confirm your new password.";
        if (value !== fields.newPassword.input.value) return "Passwords do not match.";
        return "";
    }
};

const token = new URLSearchParams(window.location.search).get("token");

function setFieldError(key, message) {
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

function showBanner(type, message) {
    formBanner.className = `form-banner ${type}`;
    formBanner.textContent = message;
}

Object.keys(validators).forEach(key => {
    fields[key].input.addEventListener("blur", () => validateField(key));
});

fields.newPassword.input.addEventListener("input", () => {
    if (fields.confirmPassword.error.textContent) {
        validateField("confirmPassword");
    }
});

async function handleSubmit(event) {
    event.preventDefault();
    formBanner.className = "form-banner hidden";

    if (!validateAll()) {
        return;
    }

    submitBtn.disabled = true;
    submitBtn.textContent = "Resetting...";

    try {
        const response = await apiRequest("/api/auth/reset-password", {
            method: "POST",
            body: JSON.stringify({ token, newPassword: fields.newPassword.input.value })
        });
        form.reset();
        form.classList.add("hidden");
        showBanner("success", `${response.message || "Password reset successfully."} You can now log in.`);
    } catch (error) {
        showBanner("error", error.message || "Could not reset your password. The link may have expired.");
        submitBtn.disabled = false;
        submitBtn.textContent = "Reset Password";
    }
}

if (!token) {
    form.classList.add("hidden");
    showBanner("error", "This password reset link is invalid or missing. Please request a new one from the forgot password page.");
} else {
    form.addEventListener("submit", handleSubmit);
}
