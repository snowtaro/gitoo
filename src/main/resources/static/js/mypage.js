
// DOM Elements
const btnOpenChangePw = document.getElementById("btnOpenChangePw");
const changePwBackdrop = document.getElementById("changePwBackdrop");
const btnCloseChangePw = document.getElementById("btnCloseChangePw");
const btnSubmitChangePw = document.getElementById("btnSubmitChangePw");
const changePwMsg = document.getElementById("changePwMsg");

const btnOpenDeleteAccount = document.getElementById("btnOpenDeleteAccount");
const deleteAccountBackdrop = document.getElementById("deleteAccountBackdrop");
const btnCloseDeleteAccount = document.getElementById("btnCloseDeleteAccount");
const btnSubmitDeleteAccount = document.getElementById("btnSubmitDeleteAccount");
const deleteAccountMsg = document.getElementById("deleteAccountMsg");

// --- Change Password Logic ---

if (btnOpenChangePw) {
    btnOpenChangePw.addEventListener("click", () => {
        if (!isAuthed) {
            alert("로그인이 필요합니다.");
            return;
        }
        changePwBackdrop.classList.remove("hidden");
        // Reset fields
        document.getElementById("currentPw").value = "";
        document.getElementById("newPw").value = "";
        changePwMsg.textContent = "";
    });
}

if (btnCloseChangePw) {
    btnCloseChangePw.addEventListener("click", () => {
        changePwBackdrop.classList.add("hidden");
    });
}

if (btnSubmitChangePw) {
    btnSubmitChangePw.addEventListener("click", async () => {
        const currentPw = document.getElementById("currentPw").value;
        const newPw = document.getElementById("newPw").value;

        if (!currentPw || !newPw) {
            changePwMsg.textContent = "모든 필드를 입력해주세요.";
            return;
        }

        changePwMsg.textContent = "요청 중...";

        try {
            const res = await apiFetch("/user/change-password", {
                method: "POST",
                body: JSON.stringify({ currentPassword: currentPw, newPassword: newPw }),
            });

            if (!res.ok) {
                // Try to parse error message
                let errMsg = "비밀번호 변경 실패";
                try {
                    const errData = await res.json();
                    errMsg = errData.message || errMsg; // Use message field if available
                } catch (e) {
                    errMsg = await res.text(); // or text if not json
                }
                throw new Error(errMsg);
            }

            alert("비밀번호가 변경되었습니다. 다시 로그인해주세요.");
            changePwBackdrop.classList.add("hidden");
            handleLogout(); // Force logout to re-authenticate with new credentials

        } catch (err) {
            changePwMsg.textContent = err.message || "오류가 발생했습니다.";
            changePwMsg.style.color = "#ef4444";
        }
    });
}

// --- Delete Account Logic ---

if (btnOpenDeleteAccount) {
    btnOpenDeleteAccount.addEventListener("click", () => {
        if (!isAuthed) {
            alert("로그인이 필요합니다.");
            return;
        }
        deleteAccountBackdrop.classList.remove("hidden");
        document.getElementById("deleteAccountPw").value = "";
        deleteAccountMsg.textContent = "";
    });
}

if (btnCloseDeleteAccount) {
    btnCloseDeleteAccount.addEventListener("click", () => {
        deleteAccountBackdrop.classList.add("hidden");
    });
}

if (btnSubmitDeleteAccount) {
    btnSubmitDeleteAccount.addEventListener("click", async () => {
        const password = document.getElementById("deleteAccountPw").value;

        if (!password) {
            deleteAccountMsg.textContent = "비밀번호를 입력해주세요.";
            return;
        }

        if (!confirm("정말 탈퇴하시겠습니까?")) return;

        deleteAccountMsg.textContent = "탈퇴 처리 중...";

        try {
            const res = await apiFetch("/user/delete-account", {
                method: "POST",
                body: JSON.stringify({ password: password }),
            });

            if (!res.ok) {
                let errMsg = "탈퇴 실패";
                try {
                    const errData = await res.json();
                    errMsg = errData.message || errMsg;
                } catch (e) {
                    errMsg = await res.text();
                }
                throw new Error(errMsg);
            }

            alert("회원 탈퇴가 완료되었습니다. 이용해 주셔서 감사합니다.");
            deleteAccountBackdrop.classList.add("hidden");
            handleLogout();

        } catch (err) {
            deleteAccountMsg.textContent = err.message || "오류가 발생했습니다.";
            deleteAccountMsg.style.color = "#ef4444";
        }
    });
}
