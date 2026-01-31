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

// ================
// State
// ================
let stompClient = null;
let roomId = null;
let roomState = null;
let ready = false; // 프론트 상태(서버 준비상태 붙이면 대체)
const maxSlots = 8;

// ================
// Render
// ================
function renderRoom(room){
    roomState = room;

    document.getElementById("roomTitle").textContent = room?.title ?? "대기방";
    const now = room?.now ?? (room?.members?.length ?? 0);
    const max = room?.max ?? room?.maxPlayers ?? 0;
    const slotCount = Math.max(max || 0,0);
    const locked = !!room?.locked;

    document.getElementById("roomMeta").innerHTML =
        `<b>${now}</b>/${max} · ${locked ? "비공개(비번)" : "공개"}`;

    // 내 표시(일단 토큰 기반 username이 없으면 "-"로)
    const youBadge = document.getElementById("youBadge");
    if (youBadge) youBadge.textContent = "나: (로그인됨)";

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

    if (!m){
        div.innerHTML = `
        <div class="slot-top">
          <div class="avatar">?</div>
          <div>
            <div class="name" style="opacity:.55;">빈 슬롯</div>
          </div>
        </div>
      `;
        return div;
    }

    const name = escapeHtml(m.username ?? m.nickname ?? ("player" + (idx+1)));
    const role = (m.role ?? "").toUpperCase();
    const isHost = role === "HOST";
    const badge = isHost ? `<span class="badge host">방장</span>` : `<span class="badge">멤버</span>`;

    div.innerHTML = `
      <div class="slot-top">
        <div class="avatar">${name[0] ?? "P"}</div>
        <div>
          <div class="name">${name}</div>
          <div class="sub">${badge}</div>
        </div>
      </div>
      <div class="sub" style="margin-top:10px;">
        ${escapeHtml(m.schoolName ?? "")}
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

        // 방 전용 토픽 (백엔드에서 맞춰줘야 함)
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
// Chat (로컬용. 서버 채팅 붙이면 여기서 publish/subscribe 하면 됨)
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
        // 백엔드에 leave 있으면 호출 추천
        try{
            await apiFetch(`/rooms/${roomId}/leave`, { method:"POST" });
        }catch(e){
            // 없어도 그냥 이동은 시킴
        }
        location.href = "/";
    });

    document.getElementById("btnReady")?.addEventListener("click", ()=> {
        ready = !ready;
        toast(ready ? "준비 완료!" : "준비 해제!");
        // 서버 준비상태 붙이면:
        // apiFetch(`/rooms/${roomId}/ready`, { method:"POST", body: JSON.stringify({ ready }) })
    });

    document.getElementById("btnStart")?.addEventListener("click", ()=> {
        // 방장만 가능하게 하려면 roomState.members 중 내 role 확인 필요
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