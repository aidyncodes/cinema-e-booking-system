function clearNode(node) {
    while (node.firstChild) {
        node.removeChild(node.firstChild);
    }
}

// ── DOM refs ──
const pageBanner = document.getElementById("pageBanner");
const loadingState = document.getElementById("loadingState");
const profileRoot = document.getElementById("profileRoot");

const accountForm = document.getElementById("accountForm");
const accountBanner = document.getElementById("accountBanner");
const accountSaveBtn = document.getElementById("accountSaveBtn");
const profileEmail = document.getElementById("profileEmail");

const accountFields = {
    firstName: { input: document.getElementById("profileFirstName"), error: document.getElementById("profileFirstNameError") },
    lastName: { input: document.getElementById("profileLastName"), error: document.getElementById("profileLastNameError") },
    phone: { input: document.getElementById("profilePhone"), error: document.getElementById("profilePhoneError") }
};
const profilePromotions = document.getElementById("profilePromotions");

const accountValidators = {
    firstName: () => (accountFields.firstName.input.value.trim() ? "" : "First name is required."),
    lastName: () => (accountFields.lastName.input.value.trim() ? "" : "Last name is required."),
    phone: () => (accountFields.phone.input.value.trim() ? "" : "Phone is required.")
};

const addressBanner = document.getElementById("addressBanner");
const addressDisplay = document.getElementById("addressDisplay");
const addressFormWrap = document.getElementById("addressFormWrap");
const addressSaveBtn = document.getElementById("addressSaveBtn");
const addressCancelBtn = document.getElementById("addressCancelBtn");

const addressFields = {
    streetLine1: { input: document.getElementById("addrLine1"), error: document.getElementById("addrLine1Error") },
    city: { input: document.getElementById("addrCity"), error: document.getElementById("addrCityError") },
    state: { input: document.getElementById("addrState"), error: document.getElementById("addrStateError") },
    postalCode: { input: document.getElementById("addrPostalCode"), error: document.getElementById("addrPostalCodeError") },
    country: { input: document.getElementById("addrCountry"), error: document.getElementById("addrCountryError") }
};
const addrLine2 = document.getElementById("addrLine2");

const addressValidators = {
    streetLine1: () => (addressFields.streetLine1.input.value.trim() ? "" : "Street address is required."),
    city: () => (addressFields.city.input.value.trim() ? "" : "City is required."),
    state: () => (addressFields.state.input.value.trim() ? "" : "State is required."),
    postalCode: () => (addressFields.postalCode.input.value.trim() ? "" : "Postal code is required."),
    country: () => (addressFields.country.input.value.trim() ? "" : "Country is required.")
};

const cardsBanner = document.getElementById("cardsBanner");
const cardsList = document.getElementById("cardsList");
const cardsLimitNote = document.getElementById("cardsLimitNote");
const addCardBtn = document.getElementById("addCardBtn");
const cardFormWrap = document.getElementById("cardFormWrap");
const cardSaveBtn = document.getElementById("cardSaveBtn");
const cardCancelBtn = document.getElementById("cardCancelBtn");
const cardEditingId = document.getElementById("cardEditingId");
const cardNumberHint = document.getElementById("cardNumberHint");

const cardFields = {
    cardholderName: { input: document.getElementById("cardHolderName"), error: document.getElementById("cardHolderNameError") },
    cardNumber: { input: document.getElementById("cardNumber"), error: document.getElementById("cardNumberError") },
    expirationMonth: { input: document.getElementById("cardExpMonth"), error: document.getElementById("cardExpMonthError") },
    expirationYear: { input: document.getElementById("cardExpYear"), error: document.getElementById("cardExpYearError") }
};

const cardValidators = {
    cardholderName: () => (cardFields.cardholderName.input.value.trim() ? "" : "Cardholder name is required."),
    cardNumber: () => {
        const digits = cardFields.cardNumber.input.value.replace(/[\s-]/g, "");
        if (!digits) return "Card number is required.";
        if (!/^\d{13,19}$/.test(digits)) return "Enter a valid card number.";
        return "";
    },
    expirationMonth: () => {
        const value = Number(cardFields.expirationMonth.input.value);
        return value >= 1 && value <= 12 ? "" : "Enter a valid month (1-12).";
    },
    expirationYear: () => {
        const value = Number(cardFields.expirationYear.input.value);
        return value >= 2000 ? "" : "Enter a valid year.";
    }
};

const favoritesList = document.getElementById("favoritesList");

// ── State ──
let profile = null;
let movieCatalog = null;

// ── Shared helpers ──
function showBanner(el, type, message) {
    el.className = `form-banner ${type}`;
    el.textContent = message;
}

function hideBanner(el) {
    el.className = "form-banner hidden";
    el.textContent = "";
}

function setFieldError(fieldMap, key, message) {
    fieldMap[key].error.textContent = message || "";
    fieldMap[key].input.classList.toggle("invalid", Boolean(message));
}

function validateAll(fieldMap, validators) {
    return Object.keys(validators)
        .map(key => {
            const message = validators[key]();
            setFieldError(fieldMap, key, message);
            return !message;
        })
        .every(Boolean);
}

function clearErrors(fieldMap) {
    Object.keys(fieldMap).forEach(key => setFieldError(fieldMap, key, ""));
}

function buildActionButton(label, extraClass, onClick) {
    const btn = document.createElement("button");
    btn.type = "button";
    btn.className = extraClass ? `button ${extraClass}` : "button";
    btn.textContent = label;
    btn.addEventListener("click", onClick);
    return btn;
}

function buildItemCard(title, metaLines, actions) {
    const card = document.createElement("div");
    card.className = "item-card";

    const body = document.createElement("div");
    body.className = "item-card-body";

    const titleEl = document.createElement("div");
    titleEl.className = "item-card-title";
    titleEl.textContent = title;
    body.appendChild(titleEl);

    metaLines.forEach(line => {
        const metaEl = document.createElement("div");
        metaEl.className = "item-card-meta";
        metaEl.textContent = line;
        body.appendChild(metaEl);
    });

    const actionsWrap = document.createElement("div");
    actionsWrap.className = "item-card-actions";
    actions.forEach(btn => actionsWrap.appendChild(btn));

    card.append(body, actionsWrap);
    return card;
}

// ── Load profile ──
async function loadProfile() {
    try {
        profile = await apiRequest("/api/profile");
    } catch (error) {
        if (error.status === 401) {
            window.location.href = "/login.html?redirect=/profile.html";
            return;
        }
        loadingState.textContent = "Could not load your profile. Please try again later.";
        return;
    }

    setCurrentUser({
        id: profile.id, email: profile.email,
        firstName: profile.firstName, lastName: profile.lastName, role: getCurrentUser()?.role
    });
    if (typeof renderNavAuth === "function") renderNavAuth();

    populateAccountForm();
    renderAddress();
    renderCards();
    renderFavorites();

    loadingState.classList.add("hidden");
    profileRoot.classList.remove("hidden");
}

// ── Account info ──
function populateAccountForm() {
    profileEmail.value = profile.email;
    accountFields.firstName.input.value = profile.firstName || "";
    accountFields.lastName.input.value = profile.lastName || "";
    accountFields.phone.input.value = profile.phone || "";
    profilePromotions.checked = Boolean(profile.promotionsOptIn);
}

Object.keys(accountValidators).forEach(key => {
    accountFields[key].input.addEventListener("blur", () => {
        setFieldError(accountFields, key, accountValidators[key]());
    });
});

async function handleAccountSubmit(event) {
    event.preventDefault();
    hideBanner(accountBanner);

    if (!validateAll(accountFields, accountValidators)) {
        return;
    }

    const payload = {
        firstName: accountFields.firstName.input.value.trim(),
        lastName: accountFields.lastName.input.value.trim(),
        phone: accountFields.phone.input.value.trim(),
        promotionsOptIn: profilePromotions.checked,
        address: null
    };

    accountSaveBtn.disabled = true;
    accountSaveBtn.textContent = "Saving...";

    try {
        profile = await apiRequest("/api/profile", { method: "PUT", body: JSON.stringify(payload) });
        setCurrentUser({
            id: profile.id, email: profile.email,
            firstName: profile.firstName, lastName: profile.lastName, role: getCurrentUser()?.role
        });
        if (typeof renderNavAuth === "function") renderNavAuth();
        showBanner(accountBanner, "success", "Account info updated.");
    } catch (error) {
        if (error.status === 400 && error.data && error.data.fields) {
            Object.entries(error.data.fields).forEach(([key, message]) => setFieldError(accountFields, key, message));
        } else {
            showBanner(accountBanner, "error", error.message || "Could not save changes.");
        }
    } finally {
        accountSaveBtn.disabled = false;
        accountSaveBtn.textContent = "Save Changes";
    }
}

accountForm.addEventListener("submit", handleAccountSubmit);

// ── Address (max 1) ──
function renderAddress() {
    clearNode(addressDisplay);

    if (profile.address) {
        const a = profile.address;
        const lines = [
            a.streetLine2 ? `${a.streetLine1}, ${a.streetLine2}` : a.streetLine1,
            `${a.city}, ${a.state} ${a.postalCode}`,
            a.country
        ];
        const editBtn = buildActionButton("Edit", "secondary", () => openAddressForm(a));
        const deleteBtn = buildActionButton("Delete", "secondary", handleDeleteAddress);
        addressDisplay.appendChild(buildItemCard("Saved Address", lines, [editBtn, deleteBtn]));
    } else {
        const hint = document.createElement("p");
        hint.className = "empty-hint";
        hint.textContent = "No address on file.";
        const addBtn = buildActionButton("+ Add Address", "secondary", () => openAddressForm(null));
        addBtn.style.width = "auto";
        addressDisplay.append(hint, addBtn);
    }
}

function openAddressForm(address) {
    clearErrors(addressFields);
    hideBanner(addressBanner);
    addressFields.streetLine1.input.value = address ? address.streetLine1 : "";
    addrLine2.value = address && address.streetLine2 ? address.streetLine2 : "";
    addressFields.city.input.value = address ? address.city : "";
    addressFields.state.input.value = address ? address.state : "";
    addressFields.postalCode.input.value = address ? address.postalCode : "";
    addressFields.country.input.value = address ? address.country : "";
    addressFormWrap.classList.remove("hidden");
    addressFormWrap.scrollIntoView({ behavior: "smooth", block: "nearest" });
}

addressCancelBtn.addEventListener("click", () => {
    addressFormWrap.classList.add("hidden");
});

Object.keys(addressValidators).forEach(key => {
    addressFields[key].input.addEventListener("blur", () => {
        setFieldError(addressFields, key, addressValidators[key]());
    });
});

async function handleAddressSave() {
    hideBanner(addressBanner);
    if (!validateAll(addressFields, addressValidators)) {
        return;
    }

    const payload = {
        firstName: profile.firstName,
        lastName: profile.lastName,
        phone: profile.phone,
        promotionsOptIn: profile.promotionsOptIn,
        address: {
            streetLine1: addressFields.streetLine1.input.value.trim(),
            streetLine2: addrLine2.value.trim() || null,
            city: addressFields.city.input.value.trim(),
            state: addressFields.state.input.value.trim(),
            postalCode: addressFields.postalCode.input.value.trim(),
            country: addressFields.country.input.value.trim()
        }
    };

    addressSaveBtn.disabled = true;
    try {
        profile = await apiRequest("/api/profile", { method: "PUT", body: JSON.stringify(payload) });
        addressFormWrap.classList.add("hidden");
        renderAddress();
    } catch (error) {
        showBanner(addressBanner, "error", error.message || "Could not save the address.");
    } finally {
        addressSaveBtn.disabled = false;
    }
}

addressSaveBtn.addEventListener("click", handleAddressSave);

async function handleDeleteAddress() {
    hideBanner(addressBanner);
    try {
        await apiRequest("/api/profile/address", { method: "DELETE" });
        profile.address = null;
        renderAddress();
    } catch (error) {
        showBanner(addressBanner, "error", error.message || "Could not remove the address.");
    }
}

// ── Payment cards (max 3) ──
function renderCards() {
    clearNode(cardsList);
    cardsLimitNote.textContent = `${profile.paymentCards.length} / 3`;

    if (!profile.paymentCards.length) {
        const hint = document.createElement("p");
        hint.className = "empty-hint";
        hint.textContent = "No payment cards saved.";
        cardsList.appendChild(hint);
    } else {
        profile.paymentCards.forEach(card => {
            const title = `${card.cardBrand || "Card"} •••• ${card.lastFour}`;
            const meta = [
                card.cardholderName,
                `Expires ${String(card.expirationMonth).padStart(2, "0")}/${card.expirationYear}`
            ];
            const editBtn = buildActionButton("Edit", "secondary", () => openCardForm(card));
            const deleteBtn = buildActionButton("Delete", "secondary", () => handleDeleteCard(card.id));
            cardsList.appendChild(buildItemCard(title, meta, [editBtn, deleteBtn]));
        });
    }

    addCardBtn.classList.toggle("hidden", profile.paymentCards.length >= 3);
}

function openCardForm(card) {
    clearErrors(cardFields);
    hideBanner(cardsBanner);
    cardEditingId.value = card ? card.id : "";
    cardFields.cardholderName.input.value = card ? card.cardholderName : "";
    cardFields.cardNumber.input.value = "";
    cardFields.expirationMonth.input.value = card ? card.expirationMonth : "";
    cardFields.expirationYear.input.value = card ? card.expirationYear : "";
    cardNumberHint.classList.toggle("hidden", !card);
    cardFormWrap.classList.remove("hidden");
    cardFormWrap.scrollIntoView({ behavior: "smooth", block: "nearest" });
}

addCardBtn.addEventListener("click", () => openCardForm(null));

cardCancelBtn.addEventListener("click", () => {
    cardFormWrap.classList.add("hidden");
});

async function handleCardSave() {
    hideBanner(cardsBanner);
    if (!validateAll(cardFields, cardValidators)) {
        return;
    }

    const payload = {
        cardholderName: cardFields.cardholderName.input.value.trim(),
        cardNumber: cardFields.cardNumber.input.value.replace(/[\s-]/g, ""),
        expirationMonth: Number(cardFields.expirationMonth.input.value),
        expirationYear: Number(cardFields.expirationYear.input.value)
    };

    const editingId = cardEditingId.value;
    cardSaveBtn.disabled = true;

    try {
        if (editingId) {
            const updated = await apiRequest(`/api/profile/cards/${editingId}`, {
                method: "PUT", body: JSON.stringify(payload)
            });
            profile.paymentCards = profile.paymentCards.map(c => (String(c.id) === editingId ? updated : c));
        } else {
            const created = await apiRequest("/api/profile/cards", {
                method: "POST", body: JSON.stringify(payload)
            });
            profile.paymentCards.push(created);
        }
        cardFormWrap.classList.add("hidden");
        renderCards();
    } catch (error) {
        if (error.status === 400 && error.data && error.data.fields) {
            Object.entries(error.data.fields).forEach(([key, message]) => setFieldError(cardFields, key, message));
        } else {
            setFieldError(cardFields, "cardNumber", error.message || "Could not save the card.");
        }
    } finally {
        cardSaveBtn.disabled = false;
    }
}

cardSaveBtn.addEventListener("click", handleCardSave);

async function handleDeleteCard(cardId) {
    hideBanner(cardsBanner);
    try {
        await apiRequest(`/api/profile/cards/${cardId}`, { method: "DELETE" });
        profile.paymentCards = profile.paymentCards.filter(c => c.id !== cardId);
        renderCards();
    } catch (error) {
        showBanner(cardsBanner, "error", error.message || "Could not remove the card.");
    }
}

// ── Favorites ──
function posterFor(movie) {
    if (movie.posterUrl) return movie.posterUrl;
    return `https://placehold.co/300x450/f3efe8/171717?text=${encodeURIComponent(movie.title || "Movie")}`;
}

function buildFavoriteCard(movie) {
    const article = document.createElement("article");
    article.className = "movie-card";

    const detailsHref = `/movie-details.html?id=${encodeURIComponent(movie.id)}`;
    const posterLink = document.createElement("a");
    posterLink.className = "poster-link";
    posterLink.href = detailsHref;

    const poster = document.createElement("img");
    poster.className = "movie-poster";
    poster.src = posterFor(movie);
    poster.alt = `${movie.title} poster`;
    posterLink.appendChild(poster);

    // Sibling of posterLink (not nested inside the <a>) so it's its own click target.
    const favoriteBtn = document.createElement("button");
    favoriteBtn.type = "button";
    favoriteBtn.className = "favorite-btn active";
    favoriteBtn.setAttribute("aria-label", `Remove ${movie.title} from favorites`);
    favoriteBtn.textContent = "♥";
    favoriteBtn.addEventListener("click", event => {
        event.preventDefault();
        toggleFavorite(movie.id);
        renderFavorites();
    });

    const copy = document.createElement("div");
    copy.className = "movie-copy";
    const title = document.createElement("h3");
    title.className = "movie-title";
    const titleLink = document.createElement("a");
    titleLink.href = detailsHref;
    titleLink.textContent = movie.title;
    title.appendChild(titleLink);
    copy.appendChild(title);

    article.append(posterLink, favoriteBtn, copy);
    return article;
}

async function renderFavorites() {
    clearNode(favoritesList);
    const ids = getFavoriteIds();

    if (!ids.length) {
        const hint = document.createElement("p");
        hint.className = "empty-hint";
        hint.textContent = "You haven't favorited any movies yet. Browse movies and tap the heart icon to add some.";
        favoritesList.appendChild(hint);
        return;
    }

    if (!movieCatalog) {
        try {
            const [current, comingSoon] = await Promise.all([
                apiRequest("/api/movies"),
                apiRequest("/api/movies/coming-soon")
            ]);
            movieCatalog = [...current, ...comingSoon];
        } catch (error) {
            movieCatalog = [];
        }
    }

    const favoriteMovies = movieCatalog.filter(movie => ids.includes(movie.id));
    if (!favoriteMovies.length) {
        const hint = document.createElement("p");
        hint.className = "empty-hint";
        hint.textContent = "Your favorited movies aren't in the current catalogue.";
        favoritesList.appendChild(hint);
        return;
    }

    const grid = document.createElement("div");
    grid.className = "movie-grid";
    favoriteMovies.forEach(movie => grid.appendChild(buildFavoriteCard(movie)));
    favoritesList.appendChild(grid);
}

loadProfile();
