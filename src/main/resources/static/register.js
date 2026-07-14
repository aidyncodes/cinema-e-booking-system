const form = document.getElementById("registerForm");
const submitBtn = document.getElementById("submitBtn");
const formBanner = document.getElementById("formBanner");
const promotionsOptIn = document.getElementById("promotionsOptIn");

const fields = {
    firstName: { input: document.getElementById("firstName"), error: document.getElementById("firstNameError") },
    lastName: { input: document.getElementById("lastName"), error: document.getElementById("lastNameError") },
    email: { input: document.getElementById("email"), error: document.getElementById("emailError") },
    phone: { input: document.getElementById("phone"), error: document.getElementById("phoneError") },
    password: { input: document.getElementById("password"), error: document.getElementById("passwordError") },
    confirmPassword: { input: document.getElementById("confirmPassword"), error: document.getElementById("confirmPasswordError") }
};

const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const validators = {
    firstName: () => (fields.firstName.input.value.trim() ? "" : "First name is required."),
    lastName: () => (fields.lastName.input.value.trim() ? "" : "Last name is required."),
    email: () => {
        const value = fields.email.input.value.trim();
        if (!value) return "Email is required.";
        if (!EMAIL_PATTERN.test(value)) return "Enter a valid email address.";
        return "";
    },
    password: () => {
        const value = fields.password.input.value;
        if (!value) return "Password is required.";
        if (value.length < 8) return "Password must be at least 8 characters.";
        return "";
    },
    confirmPassword: () => {
        const value = fields.confirmPassword.input.value;
        if (!value) return "Please confirm your password.";
        if (value !== fields.password.input.value) return "Passwords do not match.";
        return "";
    }
};

function setFieldError(key, message) {
    if (!fields[key]) return;
    fields[key].error.textContent = message || "";
    fields[key].input.classList.toggle("invalid", Boolean(message));
}

function validateField(key) {
    const validator = validators[key];
    const message = validator ? validator() : "";
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

function hideBanner() {
    formBanner.className = "form-banner hidden";
    formBanner.textContent = "";
}

function applyServerFieldErrors(fieldErrors) {
    let shownInBanner = false;
    Object.entries(fieldErrors).forEach(([key, message]) => {
        if (fields[key]) {
            setFieldError(key, message);
        } else if (!shownInBanner) {
            showBanner("error", message);
            shownInBanner = true;
        }
    });
}

Object.keys(validators).forEach(key => {
    fields[key].input.addEventListener("blur", () => validateField(key));
});

// Re-check the confirm-password field once it's been touched and the password changes.
fields.password.input.addEventListener("input", () => {
    if (fields.confirmPassword.error.textContent) {
        validateField("confirmPassword");
    }
});

async function handleSubmit(event) {
    event.preventDefault();
    hideBanner();

    if (!validateAll()) {
        return;
    }

    const payload = {
        firstName: fields.firstName.input.value.trim(),
        lastName: fields.lastName.input.value.trim(),
        email: fields.email.input.value.trim(),
        phone: fields.phone.input.value.trim() || null,
        password: fields.password.input.value,
        promotionsOptIn: promotionsOptIn.checked
    };

    submitBtn.disabled = true;
    submitBtn.textContent = "Creating account...";

    try {
        await apiRequest("/api/auth/register", {
            method: "POST",
            body: JSON.stringify(payload)
        });
        form.reset();
        showBanner("success", "Registration successful! Check your email for a confirmation link, then log in.");
    } catch (error) {
        if (error.status === 409) {
            setFieldError("email", error.message);
        } else if (error.status === 400 && error.data && error.data.fields) {
            applyServerFieldErrors(error.data.fields);
        } else {
            showBanner("error", error.message || "Registration failed. Please try again.");
        }
    } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = "Create Account";
    }
}

form.addEventListener("submit", handleSubmit);
