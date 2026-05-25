const state = {
    users: [],
    flights: [],
    flightCache: new Map(),
    selectedFlight: null,
    selectedSeat: null,
    currentUser: null,
    selectedSession: null,
    adminMode: false,
    lastSessionValue: "",
    pendingReservation: false,
    reservationSubmitting: false
};

const storageKeys = {
    currentUser: "flyport.currentUser",
    selectedFlightId: "flyport.selectedFlightId",
    selectedSeatId: "flyport.selectedSeatId",
    pendingReservation: "flyport.pendingReservation"
};

const el = (id) => document.getElementById(id);
const money = new Intl.NumberFormat("tr-TR", { style: "currency", currency: "TRY" });
const dateTime = new Intl.DateTimeFormat("tr-TR", { dateStyle: "medium", timeStyle: "short" });
const timeOnly = new Intl.DateTimeFormat("tr-TR", { hour: "2-digit", minute: "2-digit" });
const dateOnly = new Intl.DateTimeFormat("tr-TR", { dateStyle: "long" });

async function api(path, options = {}) {
    const response = await fetch(path, {
        headers: { "Content-Type": "application/json" },
        ...options
    });
    if (!response.ok) {
        const error = await response.json().catch(() => ({ message: "Beklenmeyen hata" }));
        throw new Error(error.message);
    }
    return response.json();
}

function currentUserId() {
    return state.currentUser?.id;
}

function currentUser() {
    return state.currentUser;
}

function reservationSession() {
    return state.selectedSession || state.currentUser;
}

function toast(message) {
    const toastEl = el("toast");
    toastEl.textContent = message;
    toastEl.className = "toast";
    toastEl.classList.add("show");
    setTimeout(() => toastEl.classList.remove("show"), 3200);
}

function successToast(message) {
    const toastEl = el("toast");
    toastEl.textContent = message;
    toastEl.className = "toast success show";
    setTimeout(() => toastEl.classList.remove("show"), 3600);
}

function errorToast(message) {
    const toastEl = el("toast");
    toastEl.textContent = message;
    toastEl.className = "toast error show";
    setTimeout(() => toastEl.classList.remove("show"), 3600);
}

async function init() {
    state.users = await api("/api/users");
    restoreCurrentUser();
    renderUsers();
    await loadFlights();
    await restoreSelectedTrip();
    await loadReservations();
    bindEvents();
    renderAuthState();
    prefillPassengerInfo();
}

function bindEvents() {
    updateActiveNav();
    window.addEventListener("hashchange", updateActiveNav);
    document.querySelectorAll(".nav-links a").forEach((link) => {
        link.addEventListener("click", () => requestAnimationFrame(updateActiveNav));
    });
    el("showLoginBtn").addEventListener("click", showLogin);
    el("showRegisterBtn").addEventListener("click", showRegister);
    el("loginForm").addEventListener("submit", loginUser);
    el("registerForm").addEventListener("submit", registerUser);
    el("logoutBtn").addEventListener("click", logoutUser);

    document.querySelectorAll(".flight-search-form").forEach((form) => {
        form.addEventListener("submit", async (event) => {
            event.preventDefault();
            const params = new URLSearchParams(new FormData(event.currentTarget));
            syncFlightSearchForms(params);
            if (event.currentTarget.id === "searchForm") {
                navigateToFlightSearch();
            }
            await loadFlights(params);
        });
    });

    el("resetSearch").addEventListener("click", async () => {
        document.querySelectorAll(".flight-search-form").forEach((form) => form.reset());
        await loadFlights();
    });

    el("userSelect").addEventListener("change", handleReservationSessionChange);
    el("userSelect").addEventListener("input", handleReservationSessionChange);
    el("userSelect").addEventListener("pointerup", () => {
        setTimeout(() => handleReservationSessionChange(), 0);
    });
    window.setInterval(() => {
        const sessionValue = el("userSelect")?.value || "";
        if (sessionValue !== state.lastSessionValue) {
            state.lastSessionValue = sessionValue;
            handleReservationSessionChange();
        }
    }, 300);

    el("reservationForm").addEventListener("submit", createReservation);
    el("reservationForm").querySelector("button[type='submit']").addEventListener("click", createReservation);
    bindReservationValidation();
    el("pnrForm").addEventListener("submit", searchPnr);
}

function syncFlightSearchForms(params) {
    document.querySelectorAll(".flight-search-form").forEach((form) => {
        ["fromAirportCode", "toAirportCode", "flightDate"].forEach((name) => {
            const input = form.elements[name];
            if (input && params.has(name)) {
                input.value = params.get(name);
            }
        });
    });
}

function navigateToFlightSearch() {
    window.location.href = "#ucus-ara";
    const target = document.querySelector("#ucus-ara");
    if (target) {
        setTimeout(() => target.scrollIntoView({ behavior: "smooth", block: "start" }), 80);
    }
    updateActiveNav();
}

function showLogin() {
    setAuthMode("login");
}

function showRegister() {
    setAuthMode("register");
}

function setAuthMode(mode) {
    const isRegister = mode === "register";
    el("authPanel").classList.toggle("auth-mode-login", !isRegister);
    el("authPanel").classList.toggle("auth-mode-register", isRegister);
    el("showLoginBtn").classList.toggle("active", !isRegister);
    el("showRegisterBtn").classList.toggle("active", isRegister);
    el("authFormTitle").textContent = isRegister ? "Kayıt Ol" : "Giriş Yap";
}

async function loginUser(event) {
    event.preventDefault();
    try {
        const user = await api("/api/auth/login", {
            method: "POST",
            body: JSON.stringify({
                email: el("loginEmail").value.trim(),
                password: el("loginPassword").value
            })
        });
        await refreshUsers(user.id);
        setCurrentUser(user);
        toast(`Hoş geldiniz, ${user.fullName}.`);
        await continuePendingReservation();
    } catch (error) {
        toast(error.message);
    }
}

async function registerUser(event) {
    event.preventDefault();
    const password = el("registerPassword").value;
    const passwordConfirm = el("registerPasswordConfirm").value;
    if (password !== passwordConfirm) {
        toast("Şifreler eşleşmiyor.");
        return;
    }
    try {
        const user = await api("/api/auth/register", {
            method: "POST",
            body: JSON.stringify({
                fullName: el("registerName").value.trim(),
                email: el("registerEmail").value.trim(),
                password,
                passwordConfirm
            })
        });
        el("registerForm").reset();
        await refreshUsers(user.id);
        setCurrentUser(user);
        showLogin();
        el("loginEmail").value = user.email;
        toast(`Kaydoldunuz, ${user.fullName}.`);
        await continuePendingReservation();
    } catch (error) {
        toast(error.message);
    }
}

async function refreshUsers(selectedUserId) {
    state.users = await api("/api/users");
    renderUsers();
    if (selectedUserId && el("userSelect").querySelector(`option[value="${selectedUserId}"]`)) {
        el("userSelect").value = selectedUserId;
    }
    await loadReservations();
    renderSeatMap();
}

function restoreCurrentUser() {
    const stored = localStorage.getItem(storageKeys.currentUser);
    if (!stored) {
        return;
    }
    try {
        const parsed = JSON.parse(stored);
        const user = state.users.find((item) => item.id === parsed.id);
        if (user) {
            state.currentUser = user;
            setReservationSession(user);
        } else {
            localStorage.removeItem(storageKeys.currentUser);
        }
    } catch (error) {
        localStorage.removeItem(storageKeys.currentUser);
    }
}

function setCurrentUser(user) {
    state.currentUser = user;
    setReservationSession(user);
    localStorage.setItem(storageKeys.currentUser, JSON.stringify(user));
    renderAuthState();
    prefillPassengerInfo();
    if (el("userSelect").querySelector(`option[value="${user.id}"]`)) {
        el("userSelect").value = user.id;
    }
    loadReservations();
}

function setReservationSession(user) {
    state.selectedSession = user;
    state.adminMode = isAdminUser(user);
    if (el("userSelect")) {
        el("userSelect").value = user ? String(user.id) : "";
        state.lastSessionValue = el("userSelect").value;
    }
}

async function handleReservationSessionChange() {
    syncReservationSessionFromDropdown();
    await loadReservations();
    renderSeatMap();
}

function syncReservationSessionFromDropdown() {
    const select = el("userSelect");
    if (!select) {
        return;
    }
    const selected = state.users.find((user) => user.id === Number(select.value)) || null;
    if ((state.selectedSession?.id || null) !== (selected?.id || null)) {
        setReservationSession(selected);
    } else if (selected) {
        state.adminMode = isAdminUser(selected);
    }
}

function logoutUser() {
    state.currentUser = null;
    state.selectedSession = null;
    state.adminMode = false;
    state.pendingReservation = false;
    state.selectedSeat = null;
    clearAuthStorage();
    renderAuthState();
    resetUserDropdown();
    loadReservations();
    renderSeatMap();
    toast("Çıkış yapıldı.");
}

function clearAuthStorage() {
    [
        storageKeys.currentUser,
        storageKeys.pendingReservation,
        "currentUser",
        "selectedUser",
        "selectedSession",
        "authToken",
        "token",
        "user",
        "session",
        "flyport.authToken",
        "flyport.selectedUser",
        "flyport.selectedSession"
    ].forEach((key) => {
        localStorage.removeItem(key);
        sessionStorage.removeItem(key);
    });
}

function resetUserDropdown() {
    if (el("userSelect")) {
        renderUsers();
        el("userSelect").value = "";
    }
}

function renderAuthState() {
    const user = currentUser();
    el("authGuestLink").style.display = user ? "none" : "inline-flex";
    el("authUserMenu").style.display = user ? "inline-flex" : "none";
    if (user) {
        el("navWelcome").textContent = `Hoş geldin, ${user.fullName}`;
    }
    renderReservationPanelState();
}

function renderReservationPanelState() {
    document.body.classList.toggle("is-authenticated", !!currentUser());
}

function requireAuthForReservation() {
    state.pendingReservation = true;
    localStorage.setItem(storageKeys.pendingReservation, "true");
    persistSelection();
    showLogin();
    document.querySelector("#giris").scrollIntoView({ behavior: "smooth", block: "start" });
    toast("Rezervasyonu tamamlamak için giriş yapın veya kayıt olun.");
}

async function continuePendingReservation() {
    const hasPendingReservation = state.pendingReservation || localStorage.getItem(storageKeys.pendingReservation) === "true";
    if (!hasPendingReservation) {
        return;
    }
    state.pendingReservation = false;
    localStorage.removeItem(storageKeys.pendingReservation);
    await restoreSelectedTrip();
    prefillPassengerInfo();
    document.querySelector(".reservation-panel").scrollIntoView({ behavior: "smooth", block: "start" });
    toast("Kaldığınız yerden devam edebilirsiniz.");
}

function prefillPassengerInfo() {
    const user = currentUser();
    if (!user) {
        return;
    }
    if (!el("passengerName").value) {
        el("passengerName").value = user.fullName;
    }
    if (!el("passengerEmail").value) {
        el("passengerEmail").value = user.email;
    }
    validateReservationForm();
}

function bindReservationValidation() {
    const inputs = ["passengerName", "passengerEmail", "passengerPhone", "cardNumber", "cardExpiry", "cardCvv"];
    inputs.forEach((id) => {
        el(id).addEventListener("input", () => {
            formatReservationInput(id);
            validateReservationForm();
        });
        el(id).addEventListener("blur", () => validateReservationForm(true));
    });
    validateReservationForm();
}

function formatReservationInput(id) {
    if (id === "passengerName") {
        el(id).value = el(id).value.replace(/[^A-Za-zÇĞİÖŞÜçğıöşü\s]/g, "").replace(/\s{2,}/g, " ");
    }
    if (id === "passengerPhone") {
        el(id).value = formatTurkishPhone(el(id).value);
    }
    if (id === "cardNumber") {
        const digits = onlyDigits(el(id).value).slice(0, 16);
        el(id).value = digits.replace(/(\d{4})(?=\d)/g, "$1 ");
        el("cardType").textContent = detectCardType(digits);
    }
    if (id === "cardExpiry") {
        const digits = onlyDigits(el(id).value).slice(0, 4);
        el(id).value = digits.length > 2 ? `${digits.slice(0, 2)}/${digits.slice(2)}` : digits;
    }
    if (id === "cardCvv") {
        el(id).value = onlyDigits(el(id).value).slice(0, 4);
    }
}

function validateReservationForm(showErrors = false) {
    const validations = {
        passengerName: validatePassengerName(),
        passengerEmail: validateEmail(),
        passengerPhone: validatePhone(),
        cardNumber: validateCardNumber(),
        cardExpiry: validateCardExpiry(),
        cardCvv: validateCvv()
    };
    Object.entries(validations).forEach(([id, result]) => setFieldState(id, result, showErrors));
    const isValid = Object.values(validations).every((result) => result.valid);
    el("reservationSubmitBtn").disabled = !isValid || state.reservationSubmitting;
    return isValid;
}

function setFieldState(id, result, showErrors) {
    const input = el(id);
    const field = input.closest(".field");
    const error = el(`${id}Error`);
    const hasValue = input.value.trim().length > 0;
    field.classList.toggle("is-valid", hasValue && result.valid);
    field.classList.toggle("is-invalid", hasValue && !result.valid);
    if (error) {
        error.textContent = (showErrors || hasValue) && !result.valid ? result.message : "";
    }
}

function validatePassengerName() {
    const value = el("passengerName").value.trim();
    return {
        valid: /^[A-Za-zÇĞİÖŞÜçğıöşü]+(?:\s+[A-Za-zÇĞİÖŞÜçğıöşü]+)+$/.test(value),
        message: "Ad ve soyadınızı sadece harflerle girin."
    };
}

function validateEmail() {
    const value = el("passengerEmail").value.trim();
    return {
        valid: /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(value),
        message: "Geçerli bir e-posta girin."
    };
}

function validatePhone() {
    return {
        valid: normalizeTurkishPhone(el("passengerPhone").value).length === 10,
        message: "Telefonu +90 5XX XXX XX XX formatında girin."
    };
}

function validateCardNumber() {
    const digits = onlyDigits(el("cardNumber").value);
    return {
        valid: digits.length === 16 && luhnCheck(digits),
        message: "Geçerli bir kart numarası girin."
    };
}

function validateCardExpiry() {
    const value = el("cardExpiry").value;
    const [monthText, yearText] = value.split("/");
    const month = Number(monthText);
    const year = Number(yearText);
    const now = new Date();
    const currentYear = now.getFullYear() % 100;
    const currentMonth = now.getMonth() + 1;
    const validFormat = /^\d{2}\/\d{2}$/.test(value);
    const notExpired = year > currentYear || (year === currentYear && month >= currentMonth);
    return {
        valid: validFormat && month >= 1 && month <= 12 && notExpired,
        message: "Geçerli ve güncel bir tarih girin."
    };
}

function validateCvv() {
    return {
        valid: /^\d{3,4}$/.test(el("cardCvv").value),
        message: "CVV 3 veya 4 haneli olmalı."
    };
}

function onlyDigits(value) {
    return value.replace(/\D/g, "");
}

function normalizeTurkishPhone(value) {
    let digits = onlyDigits(value);
    if (digits.startsWith("90")) {
        digits = digits.slice(2);
    }
    if (digits.startsWith("0")) {
        digits = digits.slice(1);
    }
    return digits.slice(0, 10);
}

function formatTurkishPhone(value) {
    const digits = normalizeTurkishPhone(value);
    const parts = [];
    if (digits.length > 0) parts.push(digits.slice(0, 3));
    if (digits.length > 3) parts.push(digits.slice(3, 6));
    if (digits.length > 6) parts.push(digits.slice(6, 8));
    if (digits.length > 8) parts.push(digits.slice(8, 10));
    return digits ? `+90 ${parts.join(" ")}` : "";
}

function detectCardType(digits) {
    if (/^4/.test(digits)) return "Visa";
    if (/^(5[1-5]|2[2-7])/.test(digits)) return "Mastercard";
    if (/^3[47]/.test(digits)) return "Amex";
    if (/^6/.test(digits)) return "Discover";
    return "Kart";
}

function luhnCheck(digits) {
    let sum = 0;
    let doubleDigit = false;
    for (let i = digits.length - 1; i >= 0; i -= 1) {
        let digit = Number(digits[i]);
        if (doubleDigit) {
            digit *= 2;
            if (digit > 9) digit -= 9;
        }
        sum += digit;
        doubleDigit = !doubleDigit;
    }
    return sum % 10 === 0;
}

function persistSelection() {
    if (state.selectedFlight?.id) {
        localStorage.setItem(storageKeys.selectedFlightId, String(state.selectedFlight.id));
    } else {
        localStorage.removeItem(storageKeys.selectedFlightId);
    }
    if (state.selectedSeat?.id) {
        localStorage.setItem(storageKeys.selectedSeatId, String(state.selectedSeat.id));
    } else {
        localStorage.removeItem(storageKeys.selectedSeatId);
    }
}

function clearStoredSelection() {
    localStorage.removeItem(storageKeys.selectedFlightId);
    localStorage.removeItem(storageKeys.selectedSeatId);
}

async function restoreSelectedTrip() {
    const flightId = Number(localStorage.getItem(storageKeys.selectedFlightId));
    const seatId = Number(localStorage.getItem(storageKeys.selectedSeatId));
    if (!flightId) {
        return;
    }
    try {
        state.selectedFlight = await api(`/api/flights/${flightId}`);
        state.selectedSeat = seatId ? state.selectedFlight.seats.find((seat) => seat.id === seatId) || null : null;
        if (state.selectedFlight) {
            const fromCode = state.selectedFlight.departureAirportCode || state.selectedFlight.fromCity;
            const toCode = state.selectedFlight.arrivalAirportCode || state.selectedFlight.toCity;
            el("selectedFlightTitle").textContent = `${fromCode} - ${toCode}`;
            renderFlights();
            renderSeatMap();
        }
    } catch (error) {
        clearStoredSelection();
    }
}

function updateActiveNav() {
    const currentHash = window.location.hash || "#ana-sayfa";
    document.querySelectorAll(".nav-links a").forEach((link) => {
        link.classList.toggle("active", link.getAttribute("href") === currentHash);
    });
}

function renderUsers() {
    el("userSelect").innerHTML = `<option value="">Oturum yok</option>` + state.users
        .map((user) => `<option value="${user.id}">${user.fullName} (${user.role})</option>`)
        .join("");
    const selected = reservationSession();
    if (selected && el("userSelect").querySelector(`option[value="${selected.id}"]`)) {
        el("userSelect").value = selected.id;
    } else {
        el("userSelect").value = "";
    }
    state.lastSessionValue = el("userSelect").value;
}

async function loadFlights(params = new URLSearchParams()) {
    const query = params.toString();
    state.flights = await api(`/api/flights${query ? `?${query}` : ""}`);
    state.flights.forEach((flight) => state.flightCache.set(flight.id, flight));
    updateSelectedDateLabel(params.get("flightDate") || params.get("date"));
    renderFlights();
    updateStats();
}

async function getFlightForReservation(flightId) {
    if (!state.flightCache.has(flightId)) {
        state.flightCache.set(flightId, await api(`/api/flights/${flightId}`));
    }
    return state.flightCache.get(flightId);
}

function renderFlights() {
    const list = el("flightList");
    if (state.flights.length === 0) {
        list.className = "flight-list empty-state";
        list.textContent = "Arama kriterlerine uygun uçuş yok.";
        return;
    }
    list.className = "flight-list";
    list.innerHTML = state.flights.map((flight) => {
        const seatsLeft = flight.seats.filter((seat) => seat.status === "AVAILABLE").length;
        const active = state.selectedFlight?.id === flight.id ? "active" : "";
        const departure = new Date(flight.departureTime);
        const arrival = new Date(flight.arrivalTime);
        const durationMinutes = flight.durationMinutes || Math.max(0, Math.round((arrival - departure) / 60000));
        const duration = `${Math.floor(durationMinutes / 60)}sa ${durationMinutes % 60}dk`;
        const fromCode = flight.departureAirportCode || flight.fromCity.slice(0, 3).toUpperCase();
        const toCode = flight.arrivalAirportCode || flight.toCity.slice(0, 3).toUpperCase();
        const flightCode = flight.flightCode || flight.flightNumber;
        const price = flight.price || flight.basePrice;
        return `
            <article class="flight-card ${active}" data-flight-id="${flight.id}">
                <div class="flight-card-top">
                    <div>
                        <strong>${flight.airline}</strong>
                        <small>${flightCode} · Direkt · ${flight.flightType || "Yurt İçi"}</small>
                    </div>
                    <span class="price">${money.format(price)}</span>
                </div>
                <div class="route flight-route">
                    <div>
                        <span>${timeOnly.format(departure)}</span>
                        <small>${fromCode} · ${flight.departureAirportName || flight.fromCity}</small>
                    </div>
                    <span class="route-line"></span>
                    <div>
                        <span>${timeOnly.format(arrival)}</span>
                        <small>${toCode} · ${flight.arrivalAirportName || flight.toCity}</small>
                    </div>
                </div>
                <div class="meta flight-details">
                    <span>${dateOnly.format(departure)}</span>
                    <span>${duration}</span>
                    <span>${seatsLeft} kalan koltuk</span>
                    <button class="select-flight" type="button" data-flight-id="${flight.id}" aria-label="${flightCode} uçuşunu seç">Seç →</button>
                </div>
            </article>
        `;
    }).join("");

    list.querySelectorAll(".flight-card").forEach((card) => {
        card.addEventListener("click", () => selectFlight(Number(card.dataset.flightId)));
    });
    list.querySelectorAll(".select-flight").forEach((button) => {
        button.addEventListener("click", (event) => {
            event.stopPropagation();
            selectFlight(Number(button.dataset.flightId));
        });
    });
}

async function selectFlight(flightId) {
    state.selectedFlight = await api(`/api/flights/${flightId}`);
    state.selectedSeat = null;
    persistSelection();
    const fromCode = state.selectedFlight.departureAirportCode || state.selectedFlight.fromCity;
    const toCode = state.selectedFlight.arrivalAirportCode || state.selectedFlight.toCity;
    el("selectedFlightTitle").textContent = `${fromCode} - ${toCode}`;
    el("lockBadge").textContent = "Kilit yok";
    renderFlights();
    renderSeatMap();
    updateStats();
}

function renderSeatMap() {
    const map = el("seatMap");
    if (!state.selectedFlight) {
        map.className = "seat-map empty-state";
        map.textContent = "Bir uçuş seçildiğinde koltuklar burada görünür.";
        return;
    }

    map.className = "seat-map";
    const seats = [...state.selectedFlight.seats].sort((a, b) => seatSort(a.seatNumber, b.seatNumber));
    map.innerHTML = seats.map((seat, index) => {
        const aisle = (index % 6 === 3) ? "<div class=\"aisle\">koridor</div>" : "";
        const selected = state.selectedSeat?.id === seat.id ? "selected" : "";
        const lockedByOther = seat.status === "LOCKED" && seat.lockedByUserId !== currentUserId();
        const disabled = seat.status === "RESERVED" || lockedByOther ? "disabled" : "";
        return `${aisle}<button class="seat ${seat.status.toLowerCase()} ${seat.cabinClass.toLowerCase()} ${selected}" ${disabled} data-seat-id="${seat.id}" title="${seat.cabinClass} ${money.format(seat.price)}">${seat.seatNumber}</button>`;
    }).join("");

    map.querySelectorAll(".seat").forEach((button) => {
        button.addEventListener("click", () => lockSeat(Number(button.dataset.seatId)));
    });
}

function seatSort(left, right) {
    const leftRow = Number.parseInt(left, 10);
    const rightRow = Number.parseInt(right, 10);
    return leftRow === rightRow ? left.localeCompare(right) : leftRow - rightRow;
}

async function lockSeat(seatId) {
    if (!state.selectedFlight) {
        return;
    }
    const seat = state.selectedFlight.seats.find((item) => item.id === seatId);
    if (!seat || seat.status === "RESERVED") {
        return;
    }
    if (!currentUser()) {
        state.selectedSeat = seat;
        persistSelection();
        renderSeatMap();
        requireAuthForReservation();
        return;
    }
    const response = await api(`/api/seats/${seatId}/lock`, {
        method: "POST",
        body: JSON.stringify({ userId: currentUserId() })
    });
    state.selectedFlight = await api(`/api/flights/${state.selectedFlight.id}`);
    state.selectedSeat = state.selectedFlight.seats.find((seat) => seat.id === seatId);
    persistSelection();
    el("lockBadge").textContent = `Kilit: ${new Date(response.lockedUntil).toLocaleTimeString("tr-TR", { hour: "2-digit", minute: "2-digit" })}`;
    renderSeatMap();
    toast(`${state.selectedSeat.seatNumber} koltuğu geçici olarak kilitlendi.`);
}

async function createReservation(event) {
    event.preventDefault();
    if (state.reservationSubmitting) {
        return;
    }
    if (!state.selectedFlight || !state.selectedSeat) {
        toast("Önce uçuş ve koltuk seçin.");
        return;
    }
    if (!currentUser()) {
        requireAuthForReservation();
        return;
    }
    formatReservationInput("passengerName");
    formatReservationInput("passengerEmail");
    formatReservationInput("passengerPhone");
    formatReservationInput("cardNumber");
    formatReservationInput("cardExpiry");
    formatReservationInput("cardCvv");
    if (!validateReservationForm(true)) {
        errorToast("Rezervasyon bilgilerini kontrol edin.");
        return;
    }
    const payload = {
        userId: currentUserId(),
        flightId: state.selectedFlight.id,
        seatId: state.selectedSeat.id,
        passengerName: el("passengerName").value,
        passengerEmail: el("passengerEmail").value,
        phone: el("passengerPhone").value,
        cardNumber: el("cardNumber").value,
        cardExpiry: el("cardExpiry").value,
        cardCvv: el("cardCvv").value
    };
    state.reservationSubmitting = true;
    const submitButton = el("reservationForm").querySelector("button[type='submit']");
    submitButton.disabled = true;
    submitButton.classList.add("loading");
    submitButton.textContent = "İşleniyor...";
    try {
        const reservation = await api("/api/reservations", {
            method: "POST",
            body: JSON.stringify(payload)
        });
        el("reservationForm").reset();
        state.selectedSeat = null;
        clearStoredSelection();
        state.selectedFlight = await api(`/api/flights/${state.selectedFlight.id}`);
        await loadReservations();
        await loadFlights();
        renderSeatMap();
        renderPnrResult(reservation);
        prefillPassengerInfo();
        successToast(`Rezervasyon oluştu: ${reservation.pnrCode || `FR-${reservation.id}`}`);
    } catch (error) {
        errorToast(error.message);
    } finally {
        state.reservationSubmitting = false;
        submitButton.disabled = false;
        submitButton.classList.remove("loading");
        submitButton.textContent = "Rezervasyon yap";
        validateReservationForm();
    }
}

async function loadReservations() {
    syncReservationSessionFromDropdown();
    const user = reservationSession();
    if (!user) {
        const myEl = el("myReservations");
        myEl.className = "reservation-list empty-state";
        myEl.textContent = "Rezervasyonlarınızı görmek için giriş yapın.";
        const adminEl = el("adminReservations");
        adminEl.className = "reservation-list empty-state";
        adminEl.textContent = "Admin kullanıcıyı seçin.";
        updateStats(0);
        return;
    }
    state.adminMode = isAdminUser(user);
    if (state.adminMode) {
        const myEl = el("myReservations");
        myEl.className = "reservation-list empty-state";
        myEl.textContent = "Admin modunda kullanıcı rezervasyonları görüntülenmez.";
        const adminReservations = await api(`/api/admin/reservations?userId=${user.id}`);
        await renderReservations(el("adminReservations"), adminReservations, { mode: "admin" });
        updateStats(adminReservations.length);
        return;
    }

    const myReservations = await api(`/api/reservations?userId=${user.id}`);
    await renderReservations(el("myReservations"), myReservations, { mode: "user" });
    const adminEl = el("adminReservations");
    adminEl.className = "reservation-list empty-state";
    adminEl.textContent = "Admin kullanıcıyı seçin.";
    updateStats(myReservations.length);
}

function isAdminUser(user) {
    return user?.role === "ADMIN" || user?.staff || user?.isStaff || user?.is_staff;
}

async function renderReservations(container, reservations, options = {}) {
    if (reservations.length === 0) {
        container.className = "reservation-list empty-state";
        container.textContent = options.mode === "admin" ? "Henüz rezervasyon bulunmuyor." : "Henüz rezervasyon yok.";
        return;
    }
    const enrichedReservations = await Promise.all(reservations.map(async (reservation) => ({
        reservation,
        flight: await getFlightForReservation(reservation.flightId)
    })));
    container.className = "reservation-list";
    container.innerHTML = enrichedReservations.map(({ reservation, flight }) => {
        const departure = flight ? new Date(flight.departureTime) : null;
        const route = flight
            ? `${flight.flightCode || `#${flight.id}`} · ${flight.departureAirportCode || flight.fromCity} → ${flight.arrivalAirportCode || flight.toCity}`
            : `Uçuş: ${reservation.flightId}`;
        return `
        <article class="reservation-card">
            <strong>${reservation.passengerName}</strong>
            <div class="meta">
                <span>PNR: ${reservation.pnrCode || `FR-${reservation.id}`}</span>
                <span>Uçuş: ${route}</span>
                <span>Tarih: ${departure ? dateOnly.format(departure) : "-"}</span>
                <span>Koltuk: ${reservation.seatId}</span>
                <span>Telefon: ${reservation.phone || "-"}</span>
                <span>Kart: **** ${reservation.cardLast4 || "----"}</span>
                <span>Durum: ${reservation.status}</span>
                <span>Ödeme: ${money.format(reservation.paidAmount)}</span>
                <span>İade: ${money.format(reservation.refundAmount)}</span>
            </div>
            ${reservation.status === "ACTIVE" ? reservationActions(reservation.id, options.mode) : ""}
        </article>
    `}).join("");

    container.querySelectorAll(".cancel").forEach((button) => {
        button.addEventListener("click", () => cancelReservation(Number(button.dataset.reservationId)));
    });
    container.querySelectorAll(".refund").forEach((button) => {
        button.addEventListener("click", () => refundReservation(Number(button.dataset.reservationId)));
    });
}

function reservationActions(reservationId, mode) {
    if (mode === "admin") {
        return `
            <div class="reservation-actions">
                <button class="cancel" data-reservation-id="${reservationId}">İptal Et</button>
                <button class="refund" data-reservation-id="${reservationId}">İade Et</button>
            </div>
        `;
    }
    return `<button class="cancel" data-reservation-id="${reservationId}">İptal / iade</button>`;
}

async function cancelReservation(reservationId) {
    const reservation = await api(`/api/reservations/${reservationId}/cancel`, { method: "POST" });
    if (state.selectedFlight) {
        state.selectedFlight = await api(`/api/flights/${state.selectedFlight.id}`);
        renderSeatMap();
    }
    await loadReservations();
    await loadFlights();
    renderPnrResult(reservation);
    toast(`İptal tamamlandı. İade: ${money.format(reservation.refundAmount)}`);
}

async function refundReservation(reservationId) {
    const reservation = await api(`/api/reservations/${reservationId}/refund`, { method: "POST" });
    if (state.selectedFlight) {
        state.selectedFlight = await api(`/api/flights/${state.selectedFlight.id}`);
        renderSeatMap();
    }
    await loadReservations();
    await loadFlights();
    renderPnrResult(reservation);
    successToast(`İade tamamlandı: ${money.format(reservation.refundAmount)}`);
}

async function searchPnr(event) {
    event.preventDefault();
    if (!currentUser()) {
        renderPnrMessage("PNR sorgulamak için giriş yapın.");
        document.querySelector("#giris").scrollIntoView({ behavior: "smooth", block: "start" });
        return;
    }
    const rawCode = el("pnrInput").value.trim().toUpperCase();
    const reservationId = Number(rawCode.replace("FR-", "").replace("#", ""));
    if (!reservationId) {
        renderPnrMessage("Geçerli bir PNR ya da rezervasyon numarası girin.");
        return;
    }

    const reservations = await api(`/api/reservations?userId=${currentUserId()}`);
    const reservation = reservations.find((item) => item.id === reservationId);
    if (!reservation) {
        renderPnrMessage("Bu kullanıcı için eşleşen rezervasyon bulunamadı.");
        scrollToPnrResult();
        return;
    }

    renderPnrResult(reservation);
    scrollToPnrResult();
}

function renderPnrResult(reservation) {
    const result = el("pnrResult");
    result.className = "pnr-result";
    result.innerHTML = `
        <strong>${reservation.pnrCode || `FR-${reservation.id}`} - ${reservation.passengerName}</strong>
        <div class="meta">
            <span>Uçuş: ${reservation.flightId}</span>
            <span>Koltuk: ${reservation.seatId}</span>
            <span>Telefon: ${reservation.phone || "-"}</span>
            <span>Kart: **** ${reservation.cardLast4 || "----"}</span>
            <span>Durum: ${reservation.status}</span>
            <span>Ödeme: ${money.format(reservation.paidAmount)}</span>
            <span>İade: ${money.format(reservation.refundAmount)}</span>
        </div>
    `;
}

function renderPnrMessage(message) {
    const result = el("pnrResult");
    result.className = "pnr-result empty-state";
    result.textContent = message;
}

function scrollToPnrResult() {
    setTimeout(() => el("pnrResult").scrollIntoView({ behavior: "smooth", block: "center" }), 50);
}

function updateStats(myReservationCount) {
    const flights = state.flights.length;
    const seats = state.flights.reduce((total, flight) => total + flight.seats.length, 0);
    if (el("flightCount")) {
        el("flightCount").textContent = flights;
    }
    if (el("seatCount")) {
        el("seatCount").textContent = seats;
    }
    if (el("reservationCount") && typeof myReservationCount === "number") {
        el("reservationCount").textContent = myReservationCount;
    }
}

function updateSelectedDateLabel(dateValue) {
    if (!el("selectedDateLabel")) {
        return;
    }
    if (!dateValue) {
        el("selectedDateLabel").textContent = "Tüm tarihler";
        return;
    }
    el("selectedDateLabel").textContent = dateOnly.format(new Date(`${dateValue}T12:00:00`));
}

init().catch((error) => toast(error.message));
