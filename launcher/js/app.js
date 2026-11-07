const state = {
  settings: loadSettings(),
  gameIndex: 0,
};

const TARGET_FPS_STEPS = [30, 60, 120];

const native = {
  call(name, ...args) {
    const bridge = window.Android;
    if (bridge && typeof bridge[name] === "function") {
      bridge[name](...args);
      return true;
    }
    return false;
  },
};

let romFiles = null;

window.launcher = {
  setFps(value) {
    document.getElementById("fps-counter").textContent = `FPS ${value}`;
  },
  setRomFolder(path, gameCount) {
    ROM_FOLDER.path = path;
    ROM_FOLDER.gameCount = gameCount;
    renderRom();
  },
  setRomFiles(files) {
    romFiles = new Set(files);
    renderAll();
  },
  setCover(gameId, url) {
    const game = GAMES.find((g) => g.id === gameId);
    if (!game) return;
    game.cover = url;
    renderAll();
  },
};

function readJson(text) {
  try {
    return JSON.parse(text || "{}");
  } catch {
    return {};
  }
}

function loadSettings() {
  const local = readJson(localStorage.getItem(SETTINGS_KEY));
  const native = readJson(window.Android ? window.Android.loadSettings() : null);
  return { ...DEFAULT_SETTINGS, ...local, ...native };
}

function saveSettings() {
  localStorage.setItem(SETTINGS_KEY, JSON.stringify(state.settings));
  native.call("saveSettings", JSON.stringify(state.settings));
}

function renderAll() {
  const game = GAMES[state.gameIndex];
  renderHero(game, state.settings);
  renderPerf(state.settings);
  renderRom();
  renderCredits();

  const settingsRoot = document.getElementById("settings-root");
  const scroll = settingsRoot.scrollTop;
  renderSettings(state.settings);
  settingsRoot.scrollTop = scroll;

  document.getElementById("fps-counter").hidden = !state.settings.fpsCounter;
}

function applySetting(key, value) {
  state.settings[key] = value;
  saveSettings();
  renderAll();
}

function switchTab(tab) {
  document.querySelectorAll(".tab").forEach((t) => t.classList.toggle("is-active", t.dataset.tab === tab));
  document.querySelectorAll(".view").forEach((v) => v.classList.toggle("is-active", v.id === `view-${tab}`));
}

function scrollToSettingsCard(index) {
  switchTab("settings");
  const card = document.querySelectorAll("#settings-root > .card")[index];
  card?.scrollIntoView({ behavior: "smooth", block: "start" });
}

function handleAction(action) {
  const game = GAMES[state.gameIndex];
  switch (action) {
    case "cover":
      if (!native.call("pickCoverArt", game.id)) showToast("Pilih cover art (fitur native)");
      break;
    case "perf":
      scrollToSettingsCard(2);
      break;
    case "display":
      scrollToSettingsCard(1);
      break;
    case "hardware":
      scrollToSettingsCard(0);
      break;
    case "rom":
      if (!native.call("pickRomFolder")) showToast("Pilih folder ROM (fitur native)");
      break;
  }
}

function play(gameId) {
  const game = GAMES.find((g) => g.id === gameId);
  const launched = window.Android ? window.Android.launchGame(game.rom) : true;
  showToast(launched ? `Memuat ${game.title}...` : `ROM "${game.rom}" tidak ditemukan atau WatermelonDS belum terpasang`);
}

function cycleGame() {
  state.gameIndex = (state.gameIndex + 1) % GAMES.length;
  renderHero(GAMES[state.gameIndex], state.settings);
}

function stepTargetFps(delta) {
  const i = TARGET_FPS_STEPS.indexOf(state.settings.targetFps);
  const next = Math.min(TARGET_FPS_STEPS.length - 1, Math.max(0, i + Math.sign(delta)));
  applySetting("targetFps", TARGET_FPS_STEPS[next]);
}

document.addEventListener("click", (e) => {
  const el = e.target.closest("[data-tab],[data-action],[data-play],[data-cycle],[data-toggle],[data-set],[data-step]");
  if (!el) return;

  if (el.dataset.tab) return switchTab(el.dataset.tab);
  if (el.dataset.action) return handleAction(el.dataset.action);
  if (el.dataset.play) return play(el.dataset.play);
  if (el.dataset.cycle) return cycleGame();
  if (el.dataset.toggle) {
    const key = el.dataset.toggle;
    return applySetting(key, !state.settings[key]);
  }
  if (el.dataset.set) {
    const key = el.dataset.set;
    const value = typeof DEFAULT_SETTINGS[key] === "number" ? Number(el.dataset.value) : el.dataset.value;
    return applySetting(key, value);
  }
  if (el.dataset.step === "targetFps") return stepTargetFps(Number(el.dataset.delta));
});

document.addEventListener("change", (e) => {
  const el = e.target;
  if (el.dataset.range) applySetting(el.dataset.range, Number(el.value));
});

document.addEventListener("pointerdown", (e) => {
  const pad = e.target.closest("[data-pad]");
  if (!pad) return;
  pad.classList.add("is-pressed");
  native.call("padInput", pad.dataset.pad, true);
});

document.addEventListener("pointerup", releasePad);
document.addEventListener("pointercancel", releasePad);

function releasePad() {
  document.querySelectorAll(".pad-btn.is-pressed").forEach((pad) => {
    pad.classList.remove("is-pressed");
    native.call("padInput", pad.dataset.pad, false);
  });
}

let toastTimer;
function showToast(message) {
  const el = document.getElementById("toast");
  el.textContent = message;
  el.classList.add("is-show");
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => el.classList.remove("is-show"), 2000);
}

function tickClock() {
  const now = new Date();
  document.getElementById("clock-time").textContent = now.toLocaleTimeString("id-ID", {
    hour: "2-digit",
    minute: "2-digit",
  });
  document.getElementById("clock-date").textContent = now.toLocaleDateString("id-ID", {
    weekday: "short",
    day: "numeric",
    month: "short",
  });
}

tickClock();
setInterval(tickClock, 1000);
renderAll();
