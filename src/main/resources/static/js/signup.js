const API_BASE = "http://localhost:8080";
const SIGNUP_URL = `${API_BASE}/auth/signup`;
const SCHOOL_SEARCH_URL = `${API_BASE}/api/schools/search`;

const form = document.getElementById("signupForm");
const btn = document.getElementById("submitBtn");
const msg = document.getElementById("message");

const openSchoolModalBtn = document.getElementById("openSchoolModalBtn");
const closeSchoolModalBtn = document.getElementById("closeSchoolModalBtn");
const overlay = document.getElementById("schoolOverlay");
const schoolQuery = document.getElementById("schoolQuery");
const searchSchoolBtn = document.getElementById("searchSchoolBtn");
const resultsBox = document.getElementById("schoolResults");
const loadingBox = document.getElementById("schoolLoading");
const errorBox = document.getElementById("schoolError");
const schoolNameInput = document.getElementById("schoolName");
const schoolKeyInput = document.getElementById("schoolKey");

function showMessage(text, ok) {
    msg.className = "msg " + (ok ? "ok" : "err");
    msg.textContent = text;
}

function escapeHtml(str) {
    return String(str)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function openModal() {
    overlay.classList.add("open");
    overlay.setAttribute("aria-hidden", "false");
    errorBox.style.display = "none";
    resultsBox.style.display = "none";
    loadingBox.style.display = "none";
    setTimeout(() => schoolQuery.focus(), 50);
}

function closeModal() {
    overlay.classList.remove("open");
    overlay.setAttribute("aria-hidden", "true");
}

overlay.addEventListener("click", (e) => {
    if (e.target === overlay) closeModal();
});

window.addEventListener("keydown", (e) => {
    if (e.key === "Escape" && overlay.classList.contains("open")) closeModal();
});

openSchoolModalBtn.addEventListener("click", openModal);
closeSchoolModalBtn.addEventListener("click", closeModal);

async function searchSchools(q) {
    const url = new URL(SCHOOL_SEARCH_URL);
    url.searchParams.set("q", q);

    const res = await fetch(url.toString(), {
        method: "GET",
        headers: { "Accept": "application/json" },
    });

    let data = null;
    try { data = await res.json(); } catch (_) { }

    if (!res.ok) {
        const detail = data?.message || data?.error || res.statusText;
        throw new Error(detail);
    }

    return Array.isArray(data) ? data : (data?.items ?? []);
}

function renderResults(items) {
    resultsBox.innerHTML = "";

    if (!items.length) {
        resultsBox.style.display = "block";
        resultsBox.innerHTML = `
    <div class="resultItem">
      <p class="resultTitle">검색 결과가 없어요</p>
      <p class="resultMeta">학교명을 더 정확히 입력해보세요. (예: ‘OO고등학교’)</p>
    </div>`;
        return;
    }

    for (const it of items) {
        const title = it.schoolName || it.name || "(이름 없음)";
        const meta1 = [it.schoolType, it.address].filter(Boolean).join(" · ");
        const schoolKey = it.schoolKey || (it.atptCode && it.schoolCode ? `${it.atptCode}:${it.schoolCode}` : null);

        const div = document.createElement("div");
        div.className = "resultItem";
        div.innerHTML = `
    <p class="resultTitle">${escapeHtml(title)}</p>
    <p class="resultMeta">${escapeHtml(meta1 || "")}</p>
    <p class="resultMeta">${escapeHtml(schoolKey ? `코드: ${schoolKey}` : "")}</p>
  `;

        div.addEventListener("click", () => {
            if (!schoolKey) {
                errorBox.textContent = "학교 식별 코드가 없어 선택할 수 없어요. (백엔드 응답 확인 필요)";
                errorBox.style.display = "block";
                return;
            }
            schoolNameInput.value = title;
            schoolKeyInput.value = schoolKey;
            closeModal();
        });

        resultsBox.appendChild(div);
    }

    resultsBox.style.display = "block";
}

searchSchoolBtn.addEventListener("click", async () => {
    const q = schoolQuery.value.trim();
    errorBox.style.display = "none";
    resultsBox.style.display = "none";

    if (q.length < 2) {
        errorBox.textContent = "학교명을 2글자 이상 입력해줘!";
        errorBox.style.display = "block";
        return;
    }

    loadingBox.style.display = "block";
    searchSchoolBtn.disabled = true;

    try {
        const items = await searchSchools(q);
        renderResults(items);
    } catch (e) {
        errorBox.textContent = e.message || "학교 검색 중 오류가 발생했습니다.";
        errorBox.style.display = "block";
    } finally {
        loadingBox.style.display = "none";
        searchSchoolBtn.disabled = false;
    }
});

schoolQuery.addEventListener("keydown", (e) => {
    if (e.key === "Enter") {
        e.preventDefault();
        searchSchoolBtn.click();
    }
});

async function signup(payload) {
    const res = await fetch(SIGNUP_URL, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
    });

    let data = null;
    try { data = await res.json(); } catch (_) { }

    if (!res.ok) {
        const detail = data?.message || data?.error || res.statusText;
        throw new Error(detail);
    }
    return data;
}

form.addEventListener("submit", async (e) => {
    e.preventDefault();

    msg.className = "msg";
    msg.textContent = "";
    btn.disabled = true;

    const email = document.getElementById("email").value.trim();
    const password = document.getElementById("password").value;
    const username = document.getElementById("username").value.trim();
    const schoolName = schoolNameInput.value; // readonly input에 들어있는 값
    const schoolKey = schoolKeyInput.value;

    if (!schoolKey) {
        showMessage("학교 찾기에서 학교를 먼저 선택해줘!", false);
        btn.disabled = false;
        return;
    }

    try {
        await signup({ email, password, username, schoolKey, schoolName });
        showMessage("회원가입 완료! 로그인 페이지로 이동합니다.", true);
        setTimeout(() => { window.location.href = "main.html"; }, 700); // Wait, where is 'login'? main.html?
    } catch (err) {
        showMessage(err.message || "요청 중 오류가 발생했습니다.", false);
    } finally {
        btn.disabled = false;
    }
});
