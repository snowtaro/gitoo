function escapeHtml(str) {
    return String(str).replace(/[&<>"']/g, (m) => ({
        "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
    }[m]));
}

function getRoomId() {
    const params = new URLSearchParams(location.search);
    return params.get("id");
}

function toast(msg){
    const el = document.getElementById("toast");
    const t = document.getElementById("toastText");
    if (!el || !t) return;
    t.textContent = msg;
    el.classList.add("show");
    setTimeout(()=> el.classList.remove("show"), 1600);
}

function parseJwt(token) {
    try{
        const base64Url = token.split(".")[1];
        const base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
        const json = decodeURIComponent(atob(base64).split("")
            .map(c => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2))
            .join(""));
        return JSON.parse(json);
    }catch(e){
        return null;
    }
}

// ================
// State
// ================
let stompClient = null;
let roomId = null;
let roomState = null;
const maxSlots = 8;
let myName = null;

// ================
// Render
// ================
function renderRoom(room){
    roomState = room;

    document.getElementById("roomTitle").textContent = room?.title ?? "대기방";
    const now = room?.now ?? (room?.members?.length ?? 0);
    const max = room?.max ?? room?.maxPlayers ?? 0;
    const slotCount = Math.max(max || 0, 0);
    const locked = !!room?.locked;

    document.getElementById("roomMeta").innerHTML =
        `<b>${now}</b>/${max} · ${locked ? "비공개(비번)" : "공개"}`;

    // slots
    const slots = document.getElementById("slots");
    slots.innerHTML = "";

    const members = room?.members ?? [];
    for (let i = 0; i < slotCount; i++) {
        const m = members[i];
        slots.appendChild(makeSlot(i, m));
    }
}

function makeSlot(idx, m){
    const div = document.createElement("div");
    div.className = "slot";

    // 빈 슬롯
    if (!m){
        div.innerHTML = `
      <div class="slot-col empty">
        <div class="slot-name" style="opacity:.55;">빈 슬롯</div>
        <div class="slot-avatar">?</div>
        <div class="slot-ready"></div>
      </div>
    `;
        return div;
    }

    const nameRaw = (m.nickname ?? m.username ?? ("player" + (idx + 1)));
    const name = escapeHtml(nameRaw);

    const isReady = !!m.ready;
    const readyBadge = isReady
        ? `<span class="badge me" style="border-color:#bfffe0;">Ready</span>`
        : ``;

    div.innerHTML = `
    <div class="slot-col">
      <div class="slot-name">${name}</div>
      <div class="slot-avatar">${name[0] ?? "P"}</div>
      <div class="slot-ready">${readyBadge}</div>
    </div>
  `;

    return div;
}


// ================
// API
// ================
async function loadRoom(){
    const res = await apiFetch(`/rooms/${roomId}`, { method:"GET" });
    const room = await res.json();
    renderRoom(room);
}

async function ensureJoined() {
    await apiFetch(`/rooms/${roomId}/join`, {
        method: "POST",
        body: JSON.stringify({ password: null }) // 일단 공개방만
    });
}

// ================
// WS
// ================
function setWsDot(on){
    const dot = document.getElementById("wsDot");
    if (!dot) return;
    dot.classList.toggle("off", !on);
}

function connectRoomWs(){
    const token = getAuthToken?.();
    const socket = new SockJS(`${API_BASE}/ws`);
    stompClient = Stomp.over(socket);
    stompClient.debug = console.log;

    const headers = token ? { Authorization: `Bearer ${token}` } : {};

    stompClient.connect(headers, () => {
        console.log("STOMP connected!");
        setWsDot(true);

        stompClient.subscribe(`/topic/rooms/${roomId}`, (msg) => {
            try{
                console.log("ROOM TOPIC RECEIVED:", msg.body);
                const room = JSON.parse(msg.body);
                renderRoom(room);
            }catch(e){
                console.log("WS parse error", e);
            }
        });

    }, (err) => {
        setWsDot(false);
        console.log("WS connect error:", err);
    });
}

// ================
// Chat (로컬용)
// ================
function addChatLine(user, text){
    const log = document.getElementById("chatLog");
    const line = document.createElement("div");
    line.className = "chatLine";
    line.innerHTML = `<b>${escapeHtml(user)}</b>: ${escapeHtml(text)}`;
    log.appendChild(line);
    log.scrollTop = log.scrollHeight;
}

function sendChat(){
    const input = document.getElementById("chatInput");
    const text = (input.value || "").trim();
    if (!text) return;
    input.value = "";
    addChatLine("나", text);

    // 서버 채팅 붙이면:
    // stompClient.send(`/app/rooms/${roomId}/chat`, {}, JSON.stringify({ text }))
}

// ================
// Events
// ================
document.addEventListener("DOMContentLoaded", async () => {
    roomId = getRoomId();
    if (!roomId){
        alert("room id가 없습니다.");
        location.href = "/";
        return;
    }

    // ✅ 토큰에서 nickname 추출
    const token = getAuthToken?.();
    const payload = token ? parseJwt(token) : null;
    myName = payload?.nickname ?? null;

    if (!myName){
        toast("닉네임을 불러오지 못했습니다.");
        location.href = "/";
        return;
    }

    // ✅ 상단 "나:" 뱃지에 닉네임 표시
    const youBadge = document.getElementById("youBadge");
    if (youBadge) youBadge.textContent = `나: ${myName}`;

    // 초기 렌더: 빈 슬롯
    renderRoom({ title: "대기방", now: 0, max: 8, locked:false, members: [] });

    try{
        await ensureJoined();
        await loadRoom();
    }catch(e){
        toast(e.message || "방 정보 로드 실패");
        location.href = "/";
        return;
    }

    connectRoomWs();

    document.getElementById("btnRefresh")?.addEventListener("click", async ()=> {
        try{ await loadRoom(); toast("새로고침!"); } catch(e){ toast("실패"); }
    });

    document.getElementById("btnCopyLink")?.addEventListener("click", async ()=> {
        try{
            await navigator.clipboard.writeText(location.href);
            toast("링크 복사 완료!");
        }catch(e){
            toast("복사 실패");
        }
    });

    document.getElementById("btnLeave")?.addEventListener("click", async ()=> {
        try{
            await apiFetch(`/rooms/${roomId}/leave`, { method:"POST" });
        }catch(e){
            // 없어도 이동
        }
        location.href = "/";
    });

    // ✅ Ready 토글 (닉네임 기준)
    document.getElementById("btnReady")?.addEventListener("click", async () => {
        try {
            await apiFetch(`/rooms/${roomId}/ready`, { method: "POST" });
        } catch (e) {
            toast(e.message || "Ready 실패");
        }
    });

    document.getElementById("btnStart")?.addEventListener("click", ()=> {
        toast("게임 시작(미구현)");
        // 서버 시작 붙이면:
        // apiFetch(`/rooms/${roomId}/start`, { method:"POST" })
        // or stompClient.send(`/app/rooms/${roomId}/start`, {}, "{}")
    });

    document.getElementById("btnSend")?.addEventListener("click", sendChat);
    document.getElementById("chatInput")?.addEventListener("keydown", (e)=> {
        if (e.key === "Enter") sendChat();
    });
});
