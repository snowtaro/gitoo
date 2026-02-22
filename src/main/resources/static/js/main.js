const API_BASE = "http://localhost:8080";

// Simple in-page navigation
const navButtons = Array.from(document.querySelectorAll(".nav-btn"));
const sections = {
    mypage: document.getElementById("section-mypage"),
    game: document.getElementById("section-game"),
    shop: document.getElementById("section-shop"),
    monitor: document.getElementById("section-monitor"),
    login: document.getElementById("section-login"),
};

function showSection(key) {
    navButtons.forEach(b => b.classList.toggle("active", b.dataset.target === key));
    Object.entries(sections).forEach(([k, el]) => {
        if (el) el.classList.toggle("active", k === key);
    });
}

navButtons.forEach(btn => {
    btn.addEventListener("click", () => {
        if (btn.dataset.target === "logout_action") {
            handleLogout();
        } else {
            showSection(btn.dataset.target);
        }
    });
});

// Mock auth state (front-end only)
let isAuthed = false;
let user = { name: "Guest", point: 0 };

// Game actions
const gameMsg = document.getElementById("gameMsg");
const btnStartGame = document.getElementById("btnStartGame");
if (btnStartGame) {
    btnStartGame.addEventListener("click", () => {
        if (!isAuthed) {
            if (gameMsg) gameMsg.textContent = "로그인이 필요합니다. 로그인 탭으로 이동합니다.";
            showSection("login");
            return;
        }
        window.location.href = "/game/index.html";
    });
}

const btnHowTo = document.getElementById("btnHowTo");
if (btnHowTo) {
    btnHowTo.addEventListener("click", () => {
        if (gameMsg) gameMsg.textContent = "규칙(데모): 제한 시간 내 점수 획득, 학교 랭킹 반영.";
    });
}

// Shop actions
const shopMsg = document.getElementById("shopMsg");
document.querySelectorAll("[data-buy]").forEach(btn => {
    btn.addEventListener("click", () => {
        if (!isAuthed) {
            if (shopMsg) shopMsg.textContent = "구매하려면 로그인 필요. 로그인 탭으로 이동합니다.";
            showSection("login");
            return;
        }
        const item = btn.dataset.buy;
        const cost = item === "booster" ? 300 : 500;
        if (user.point < cost) {
            if (shopMsg) shopMsg.textContent = `포인트 부족: 필요 ${cost}, 보유 ${user.point}`;
            return;
        }
        user.point -= cost;
        renderAuth();
        if (shopMsg) shopMsg.textContent = `구매 완료: ${item} (-${cost}p)`;
    });
});

// Monitoring mock
const healthBadge = document.getElementById("healthBadge");
const rpmBadge = document.getElementById("rpmBadge");
const errBadge = document.getElementById("errBadge");
const latencyBadge = document.getElementById("latencyBadge");
const monitorMsg = document.getElementById("monitorMsg");

let healthy = false;

function randInt(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}

const btnMockHealth = document.getElementById("btnMockHealth");
if (btnMockHealth) {
    btnMockHealth.addEventListener("click", () => {
        healthy = !healthy;
        if (healthBadge) healthBadge.textContent = healthy ? "UP" : "DOWN";
        if (monitorMsg) monitorMsg.textContent = `Health 상태 변경(데모): ${healthy ? "UP" : "DOWN"}`;
    });
}

const btnMockMetrics = document.getElementById("btnMockMetrics");
if (btnMockMetrics) {
    btnMockMetrics.addEventListener("click", () => {
        if (rpmBadge) rpmBadge.textContent = `${randInt(120, 980)} rpm`;
        if (errBadge) errBadge.textContent = `${randInt(0, 7)}`;
        if (latencyBadge) latencyBadge.textContent = `${randInt(40, 220)} ms`;
        if (monitorMsg) monitorMsg.textContent = "지표 갱신(데모). 실제로는 /actuator/metrics 또는 별도 API 연동.";
    });
}

// Token Helpers
function setAuthToken(token, expiration) {
    localStorage.setItem("accessToken", token);
    localStorage.setItem("accessTokenExp", String(expiration));
}

function getAuthToken() {
    return localStorage.getItem("accessToken");
}

function clearAuthToken() {
    localStorage.removeItem("accessToken");
    localStorage.removeItem("accessTokenExp");
}

async function apiFetch(path, options = {}) {
    const token = getAuthToken();

    const headers = {
        ...(options.headers || {}),
        ...(options.body ? { "Content-Type": "application/json" } : {}),
    };

    if (token) {
        headers["Authorization"] = `Bearer ${token}`;
    }

    const res = await fetch(`${API_BASE}${path}`, {
        ...options,
        headers,
    });

    if (res.status === 401 || res.status === 403) {
        // 토큰 만료/무효 등
        clearAuthToken();
        isAuthed = false;
        renderAuth();
        showSection("login");
        throw new Error("인증이 필요합니다. 다시 로그인하세요.");
    }

    return res;
}

// Updated Init
async function initAuthFromStorage() {
    const token = getAuthToken();
    if (token) {
        isAuthed = true;
        try {
            // Fetch fresh user info
            const res = await apiFetch("/user/me"); // Assuming this endpoint exists or I need to create it
            if (res.ok) {
                const userData = await res.json();
                user = {
                    name: userData.nickname || userData.username,
                    point: userData.score || 0,
                    role: userData.role
                };
                // Update storage if needed
                localStorage.setItem("username", user.name);
                localStorage.setItem("role", user.role);
            } else {
                // Fallback
                const storedName = localStorage.getItem("username") || "User";
                const storedRole = localStorage.getItem("role") || "USER";
                user = { name: storedName, point: 0, role: storedRole };
            }
        } catch (e) {
            console.error("Failed to fetch user info", e);
            const storedName = localStorage.getItem("username") || "User";
            const storedRole = localStorage.getItem("role") || "USER";
            user = { name: storedName, point: 0, role: storedRole };
        }
    } else {
        isAuthed = false;
        user = { name: "Guest", point: 0, role: "USER" };
    }
    renderAuth();
}

// Auth UI Update
const navLoginBtn = document.getElementById("navLoginBtn");
const btnLogout = document.getElementById("btnLogout");

function handleLogout() {
    clearAuthToken();
    localStorage.removeItem("username");
    localStorage.removeItem("role");
    isAuthed = false;
    user = { name: "Guest", point: 0, role: "USER" };
    renderAuth();
    showSection("login");
    alert("로그아웃 되었습니다.");
}

if (btnLogout) {
    btnLogout.addEventListener("click", handleLogout);
}

// Toggle Logic in renderAuth
function renderAuth() {
    const authStateEl = document.getElementById("authState");
    const userNameEl = document.getElementById("userName");
    const userPointEl = document.getElementById("userPoint");

    authStateEl.textContent = isAuthed ? "로그인" : "로그아웃";
    userNameEl.textContent = isAuthed ? user.name : "Guest";
    userPointEl.textContent = String(isAuthed ? user.point : 0);

    // Profile Detail
    const profileDetail = document.getElementById("profileDetail");
    if (profileDetail) {
        if (isAuthed) {
            profileDetail.innerHTML = `<strong>${user.name}</strong>님<br>Point: ${user.point}<br>Role: ${user.role}`;
        } else {
            profileDetail.textContent = "로그인이 필요합니다.";
        }
    }

    // Buttons
    if (isAuthed) {
        if (navLoginBtn) {
            navLoginBtn.textContent = "로그아웃";
            navLoginBtn.dataset.target = "logout_action";
        }
        if (btnLogout) btnLogout.style.display = "inline-block";
    } else {
        if (navLoginBtn) {
            navLoginBtn.textContent = "로그인";
            navLoginBtn.dataset.target = "login";
        }
        if (btnLogout) btnLogout.style.display = "none";
    }
}

// Initialize on load
document.addEventListener("DOMContentLoaded", () => {
    initAuthFromStorage();
});
