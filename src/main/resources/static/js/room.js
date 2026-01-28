let roomsState = []; // 서버에서 받은 최신 방 목록

// ===== Room UI (Modals) =====
const createRoomBackdrop = document.getElementById("createRoomBackdrop");
const joinRoomBackdrop = document.getElementById("joinRoomBackdrop");

// Buttons (Global listeners might be in main.js, but specific logic here)
const btnCreateRoom = document.getElementById("btnCreateRoom");
const btnJoinRoom = document.getElementById("btnJoinRoom");

const roomUsePassword = document.getElementById("roomUsePassword");
const roomPassword = document.getElementById("roomPassword");
const createRoomMsg = document.getElementById("createRoomMsg");

const roomListEl = document.getElementById("roomList");
const roomSearchEl = document.getElementById("roomSearch");
const joinRoomMsg = document.getElementById("joinRoomMsg");

function openModal(el) { el?.classList.remove("hidden"); }
function closeModal(el) { el?.classList.add("hidden"); }

function escapeHtml(str) {
    return String(str).replace(/[&<>"']/g, (m) => ({
        "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
    }[m]));
}

async function refreshRooms() {
    try {
        const res = await apiFetch("/rooms", { method: "GET" });
        roomsState = await res.json();
        if (!joinRoomBackdrop.classList.contains("hidden")) {
            renderRoomList(roomsState);
        }
    } catch (e) {
        console.error("Failed to refresh rooms:", e);
    }
}

function renderRoomList(rooms) {
    if (!roomListEl) return;
    roomListEl.innerHTML = "";

    if (!rooms || rooms.length === 0) {
        roomListEl.innerHTML = `<div class="item"><div style="color:var(--muted);">방이 없습니다.</div></div>`;
        return;
    }

    rooms.forEach(r => {
        const row = document.createElement("div");
        row.className = "item";
        row.innerHTML = `
    <div style="display:flex; flex-direction:column; gap:4px;">
        <div style="font-weight:800;">${escapeHtml(r.title)}</div>
        <div style="color: var(--muted); font-size:12px;">
        ${r.now}/${r.max} · ${r.locked ? "비공개(비번)" : "공개"}
        </div>
    </div>
    <button class="badge"
            data-room-id="${r.id}"
            data-locked="${r.locked ? "true" : "false"}">입장</button>
    `;
        roomListEl.appendChild(row);
    });

    roomListEl.querySelectorAll("[data-room-id]").forEach(btn => {
        btn.addEventListener("click", async () => {
            const roomId = btn.dataset.roomId;
            let password = null;

            if (btn.dataset.locked === "true") {
                password = prompt("비밀번호를 입력하세요");
                if (!password) return;
            }

            try {
                await apiFetch(`/rooms/${roomId}/join`, {
                    method: "POST",
                    body: JSON.stringify({ password })
                });

                closeModal(joinRoomBackdrop);
                window.location.href = `/game/room.html?id=${encodeURIComponent(roomId)}`;

            } catch (e) {
                alert(e.message || "방 입장 실패");
            }
        });
    });
}

// ===== Event Listeners =====

// Modal close handlers
document.getElementById("btnCancelCreateRoom")?.addEventListener("click", () => closeModal(createRoomBackdrop));
document.getElementById("btnCloseJoinRoom")?.addEventListener("click", () => closeModal(joinRoomBackdrop));
createRoomBackdrop?.addEventListener("click", (e) => { if (e.target === createRoomBackdrop) closeModal(createRoomBackdrop); });
joinRoomBackdrop?.addEventListener("click", (e) => { if (e.target === joinRoomBackdrop) closeModal(joinRoomBackdrop); });

// Password toggle
roomUsePassword?.addEventListener("change", () => {
    if (!roomPassword) return;
    if (roomUsePassword.checked) roomPassword.classList.remove("hidden");
    else { roomPassword.classList.add("hidden"); roomPassword.value = ""; }
});

// Create Room Confirmation
document.getElementById("btnConfirmCreateRoom")?.addEventListener("click", async () => {
    const title = document.getElementById("roomTitle")?.value.trim();
    const maxPlayers = Number(document.getElementById("roomMaxPlayers")?.value);
    const usePw = !!roomUsePassword?.checked;
    const pw = roomPassword?.value.trim() || "";

    if (!title) { if (createRoomMsg) createRoomMsg.textContent = "방 제목을 입력하세요."; return; }
    if (!maxPlayers || maxPlayers < 2 || maxPlayers > 8) { if (createRoomMsg) createRoomMsg.textContent = "인원 수는 2~8로 설정하세요."; return; }
    if (usePw && !pw) { if (createRoomMsg) createRoomMsg.textContent = "비밀번호를 입력하세요."; return; }

    try {
        await apiFetch("/rooms", {
            method: "POST",
            body: JSON.stringify({
                title,
                maxPlayers,
                usePassword: usePw,
                password: pw
            })
        });

        closeModal(createRoomBackdrop);
        alert("방이 생성되었습니다.");
        // refreshRooms calls via WS or manual refresh could be added here if no WS
    } catch (e) {
        if (createRoomMsg) createRoomMsg.textContent = e.message || "방 생성 실패";
    }
});

// Room Open Buttons (Triggered from Main UI)
btnCreateRoom?.addEventListener("click", () => {
    // isAuthed check relies on main.js global state
    if (!isAuthed) {
        alert("로그인이 필요합니다.");
        showSection("login");
        return;
    }
    openModal(createRoomBackdrop);
    if (createRoomMsg) createRoomMsg.textContent = "";
});

btnJoinRoom?.addEventListener("click", async () => {
    if (!isAuthed) {
        alert("로그인이 필요합니다.");
        showSection("login");
        return;
    }
    openModal(joinRoomBackdrop);
    if (joinRoomMsg) joinRoomMsg.textContent = "";
    try {
        await refreshRooms();
    } catch (e) {
        renderRoomList([]);
        if (joinRoomMsg) joinRoomMsg.textContent = e.message || "방 목록 로드 실패";
    }
});

// Room Search
roomSearchEl?.addEventListener("input", () => {
    const q = roomSearchEl.value.trim().toLowerCase();
    const filtered = roomsState.filter(r => (r.title || "").toLowerCase().includes(q));
    renderRoomList(filtered);
});


// ===== WebSocket (Rooms topic) =====
let stompClient = null;

function connectWs() {
    const token = getAuthToken(); // from main.js
    const socket = new SockJS(`${API_BASE}/ws`); // API_BASE from main.js
    stompClient = Stomp.over(socket);
    stompClient.debug = null;

    const connectHeaders = token ? { Authorization: `Bearer ${token}` } : {};

    stompClient.connect(connectHeaders, async () => {
        try { await refreshRooms(); } catch (e) { }

        stompClient.subscribe("/topic/rooms", (msg) => {
            const rooms = JSON.parse(msg.body);
            roomsState = rooms;
            if (!joinRoomBackdrop.classList.contains("hidden")) {
                renderRoomList(roomsState);
            }
        });
    }, (err) => {
        console.log("WS connect error:", err);
    });
}

// Initialize WS on load
document.addEventListener("DOMContentLoaded", () => {
    connectWs();
});