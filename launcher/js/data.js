const GAMES = [
  {
    id: "mkds",
    title: "Mario Kart DS",
    subtitle: "Nintendo · NDS · 2005",
    cover: "",
    resolution: "2x (512×768)",
    fps: 60,
    lastPlayed: "Kemarin",
    rom: "Mario Kart DS (USA).nds",
  },
  {
    id: "pkmnhg",
    title: "Pokémon HeartGold",
    subtitle: "Game Freak · NDS · 2010",
    cover: "",
    resolution: "1x (256×384)",
    fps: 60,
    lastPlayed: "3 hari lalu",
    rom: "Pokemon HeartGold (USA).nds",
  },
  {
    id: "sm64ds",
    title: "Super Mario 64 DS",
    subtitle: "Nintendo · NDS · 2004",
    cover: "",
    resolution: "2x (512×768)",
    fps: 30,
    lastPlayed: "Minggu lalu",
    rom: "Super Mario 64 DS (USA).nds",
  },
];

const ROM_FOLDER = {
  path: "/storage/emulated/0/ROMs/NDS",
  gameCount: GAMES.length,
  totalSizeMB: 1487,
  lastScan: "Baru saja",
};

const TEAM = [
  { name: "Rex Dev", role: "Lead Developer", initials: "RD" },
  { name: "Nara", role: "Renderer & Vulkan", initials: "NR" },
  { name: "Bagas", role: "UI / UX Design", initials: "BG" },
  { name: "Mira", role: "Input & Overlay", initials: "MR" },
  { name: "Dimas", role: "Modder & Testing", initials: "DM" },
];

const DEFAULT_SETTINGS = {
  renderer: "vulkan",
  driver: "default",
  resolution: 2,
  targetFps: 60,
  frameSkip: 0,
  threadedCpu: true,
  shaderCache: true,
  screenLayout: "vertical",
  smoothing: true,
  overlayVisible: true,
  opacity: 70,
  speedLimiter: true,
  fpsCounter: false,
};

const SETTINGS_KEY = "rex-launcher-settings";
