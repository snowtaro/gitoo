const btnLogin = document.getElementById("btnLogin");

async function loginRequest(email, password) {
    const res = await fetch(`${API_BASE}/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password }),
    });

    if (!res.ok) {
        let msg = `로그인 실패 (HTTP ${res.status})`;
        try {
            const data = await res.json();
            msg = data.message || data.error || msg;
        } catch (e) { }
        throw new Error(msg);
    }

    const data = await res.json();
    if (!data.token) throw new Error("로그인 응답에 token이 없습니다.");
    return data;
}

if (btnLogin) {
    btnLogin.addEventListener("click", async () => {
        const email = document.getElementById("email").value.trim();
        const pw = document.getElementById("password").value.trim();
        const msgEl = document.getElementById("loginMsg");

        if (!email || !pw) {
            msgEl.textContent = "이메일과 비밀번호를 입력하세요.";
            return;
        }

        msgEl.textContent = "로그인 요청 중...";

        try {
            // Login Request
            const data = await loginRequest(email, pw);

            setAuthToken(data.token, data.expiration);

            const uName = email.split("@")[0]; // Fallback if data doesn't have name
            const role = data.role || "USER";
            localStorage.setItem("username", uName);
            localStorage.setItem("role", role);

            // Update global state associated with main.js
            isAuthed = true;
            user = { name: uName, point: 0, role: role };
            renderAuth();

            msgEl.textContent = "로그인 성공. 이동합니다...";
            showSection("mypage");

        } catch (err) {
            clearAuthToken();
            isAuthed = false;
            user = { name: "Guest", point: 0, role: "USER" };
            renderAuth();
            msgEl.textContent = err.message || "오류 발생";
        }
    });
}
