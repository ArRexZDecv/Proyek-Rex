const GAUGE_C = 251.3;
const ICON = (id) => `<svg><use href="#${id}"/></svg>`;

function esc(value) {
  return String(value).replace(/[&<>"']/g, (c) =>
    ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c])
  );
}

function toggle(key, on) {
  return `<button class="toggle ${on ? "is-on" : ""}" data-toggle="${key}" role="switch" aria-checked="${on}" aria-label="${key}"></button>`;
}

function segmented(key, options, current) {
  const buttons = options
    .map(
      ([value, label]) =>
        `<button class="${value === current ? "is-active" : ""}" data-set="${key}" data-value="${esc(value)}">${esc(label)}</button>`
    )
    .join("");
  return `<div class="segmented">${buttons}</div>`;
}

function gauge(label, percent, centerValue, centerUnit) {
  const p = Math.max(0, Math.min(1, percent));
  return `
    <div class="gauge">
      <div class="gauge-ring" style="--c:${GAUGE_C};--p:${p.toFixed(3)}">
        <svg viewBox="0 0 100 100" aria-hidden="true">
          <circle class="gauge-track" cx="50" cy="50" r="40"/>
          <circle class="gauge-value" cx="50" cy="50" r="40"/>
        </svg>
        <div class="gauge-center">${esc(centerValue)}<small>${esc(centerUnit)}</small></div>
      </div>
      <span class="gauge-label">${esc(label)}</span>
    </div>`;
}

function renderHero(game, settings) {
  const el = document.getElementById("card-game");
  const banner = game.cover
    ? `background-image: url('${esc(game.cover)}')`
    : "";
  el.innerHTML = `
    <div class="hero-banner" style="${banner}" data-cycle="1" title="Ganti game">
      <div class="hero-badge-row">
        <span class="badge"><span class="dot"></span>${esc(game.resolution)}</span>
        <span class="badge">${esc(game.fps)} FPS</span>
        <span class="badge">Terakhir: ${esc(game.lastPlayed)}</span>
      </div>
    </div>
    <div class="hero-body">
      <div>
        <h1 class="hero-title">${esc(game.title)}</h1>
        <p class="hero-sub">${esc(game.subtitle)}</p>
        <div class="hero-meta">
          <span class="badge">${ICON("i-gamepad")} ${esc(game.rom)}</span>
        </div>
      </div>
      <button class="btn-play" data-play="${esc(game.id)}">${ICON("i-play")} MAINKAN</button>
    </div>`;
}

function renderPerf(settings) {
  const resPct = settings.resolution / 4;
  const fpsPct = settings.targetFps / 120;
  const skipPct = settings.frameSkip / 4;
  document.getElementById("card-perf").innerHTML = `
    <div class="card-title"><span>Performa</span><span class="muted">${esc(settings.renderer.toUpperCase())}</span></div>
    <div class="gauges">
      ${gauge("Resolusi", resPct, `${settings.resolution}x`, "SCALE")}
      ${gauge("Target FPS", fpsPct, settings.targetFps, "FPS")}
      ${gauge("Frame Skip", skipPct, settings.frameSkip, "SKIP")}
    </div>
    <div class="perf-footer">
      <span>Atur target FPS</span>
      <div class="stepper">
        <button data-step="targetFps" data-delta="-1" aria-label="Kurangi FPS">−</button>
        <span>${settings.targetFps} FPS</span>
        <button data-step="targetFps" data-delta="1" aria-label="Tambah FPS">+</button>
      </div>
    </div>`;
}

function renderRom() {
  document.getElementById("card-rom").innerHTML = `
    <div class="card-title"><span>Status Folder ROM</span><span class="badge"><span class="dot"></span>Terhubung</span></div>
    <div class="rom-path">
      ${ICON("i-folder")}
      <div class="rom-path-text">
        <strong>Lokasi ROM</strong>
        <code title="${esc(ROM_FOLDER.path)}">${esc(ROM_FOLDER.path)}</code>
      </div>
    </div>
    <div class="rom-stats">
      <div class="stat"><strong>${ROM_FOLDER.gameCount}</strong><span>Game terdeteksi</span></div>
      <div class="stat"><strong>${(ROM_FOLDER.totalSizeMB / 1024).toFixed(1)} GB</strong><span>Total ukuran</span></div>
      <div class="stat"><strong>${esc(ROM_FOLDER.lastScan)}</strong><span>Pemindaian</span></div>
    </div>
    <div class="rom-actions">
      <button class="btn btn-block btn-primary" data-action="rom">${ICON("i-folder")} Ubah / Pilih Folder</button>
    </div>`;
}

function renderCredits() {
  const items = TEAM.map(
    (m) => `
      <div class="credit">
        <div class="avatar">${esc(m.initials)}</div>
        <div><strong>${esc(m.name)}</strong><span>${esc(m.role)}</span></div>
      </div>`
  ).join("");
  document.getElementById("card-credits").innerHTML = `
    <div class="card-title"><span>Tim Pembuat</span><span>v1.0</span></div>
    <div class="credits-list">${items}</div>`;
}

function renderSettings(settings) {
  const pads = ["X", "Y", "A", "B"]
    .map((k) => `<button class="pad-btn" data-pad="${k}">${k}</button>`)
    .join("");

  document.getElementById("settings-root").innerHTML = `
    <article class="card">
      <div class="card-title"><span>CPU &amp; GPU</span>${ICON("i-chip")}</div>
      <div class="setting-row">
        <div class="setting-info"><strong>Renderer</strong><span>Backend grafis yang dipakai</span></div>
        ${segmented("renderer", [["vulkan", "Vulkan"], ["opengl", "OpenGL"], ["software", "Software"]], settings.renderer)}
      </div>
      <div class="setting-row">
        <div class="setting-info"><strong>Driver GPU</strong><span>Pilih driver khusus untuk chipset</span></div>
        ${segmented("driver", [["default", "Default"], ["turnip", "Turnip"], ["mesa", "Mesa"], ["custom", "Custom"]], settings.driver)}
      </div>
      <div class="setting-row">
        <div class="setting-info"><strong>Threaded CPU</strong><span>Jalankan emulasi ARM9/ARM7 multi-thread</span></div>
        ${toggle("threadedCpu", settings.threadedCpu)}
      </div>
      <div class="setting-row">
        <div class="setting-info"><strong>Shader Cache</strong><span>Simpan shader agar stutter berkurang</span></div>
        ${toggle("shaderCache", settings.shaderCache)}
      </div>
    </article>

    <article class="card">
      <div class="card-title"><span>Layar &amp; Touch Overlay</span>${ICON("i-display")}</div>
      <div class="setting-row">
        <div class="setting-info"><strong>Tata letak layar</strong><span>Posisi dua layar</span></div>
        ${segmented("screenLayout", [["vertical", "Vertikal"], ["horizontal", "Horizontal"], ["single", "Satu"]], settings.screenLayout)}
      </div>
      <div class="setting-row">
        <div class="setting-info"><strong>Smoothing</strong><span>Haluskan tepi piksel</span></div>
        ${toggle("smoothing", settings.smoothing)}
      </div>
      <div class="setting-row">
        <div class="setting-info"><strong>Tampilkan overlay</strong><span>Kontrol sentuh di layar</span></div>
        ${toggle("overlayVisible", settings.overlayVisible)}
      </div>
      <div class="setting-row">
        <div class="setting-info"><strong>Opasitas overlay</strong><span>${settings.opacity}%</span></div>
        <input class="slider" type="range" min="10" max="100" step="5" value="${settings.opacity}" data-range="opacity" aria-label="Opasitas overlay" />
      </div>
      <div class="overlay-preview">
        <div class="pad-dpad">
          <span></span><button class="pad-btn" data-pad="UP">▲</button><span></span>
          <button class="pad-btn" data-pad="LEFT">◀</button><span></span><button class="pad-btn" data-pad="RIGHT">▶</button>
          <span></span><button class="pad-btn" data-pad="DOWN">▼</button><span></span>
        </div>
        <div class="pad-abxy">${pads}</div>
      </div>
    </article>

    <article class="card card-wide">
      <div class="card-title"><span>Performa &amp; Debug</span>${ICON("i-perf")}</div>
      <div class="setting-row">
        <div class="setting-info"><strong>Speed limiter</strong><span>Batasi emulasi ke target FPS</span></div>
        ${toggle("speedLimiter", settings.speedLimiter)}
      </div>
      <div class="setting-row">
        <div class="setting-info"><strong>FPS counter</strong><span>Tampilkan FPS real-time di pojok layar</span></div>
        ${toggle("fpsCounter", settings.fpsCounter)}
      </div>
      <div class="setting-row">
        <div class="setting-info"><strong>Frame skip</strong><span>0 = paling halus, makin tinggi makin ringan</span></div>
        ${segmented("frameSkip", [[0, "0"], [1, "1"], [2, "2"], [3, "3"], [4, "4"]], settings.frameSkip)}
      </div>
      <div class="setting-row">
        <div class="setting-info"><strong>Target FPS</strong><span>Batas kecepatan emulasi</span></div>
        ${segmented("targetFps", [[30, "30"], [60, "60"], [120, "120"]], settings.targetFps)}
      </div>
    </article>`;
}
