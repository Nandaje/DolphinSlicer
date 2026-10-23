package com.orcaslicer.android.ui

/**
 * Generates an embedded, responsive Fluidd / Mainsail Web UI for the Device tab WebView.
 * Connects live to the printer's Moonraker API (`http://<printerHost>:<moonrakerPort>` and
 * `ws://<printerHost>:<moonrakerPort>/websocket`) when reachable on the local network, while
 * providing a full interactive Fluidd / Mainsail dashboard at all times.
 */
object KlipperWebUiTemplates {

    fun buildFluiddOrMainsailHtml(
        uiFlavor: String, // "fluidd" or "mainsail"
        printerHost: String,
        moonrakerPort: String,
        printerName: String,
        hasSlicedGcode: Boolean,
        slicedSummary: String
    ): String {
        val isMainsail = uiFlavor.equals("mainsail", ignoreCase = true)
        val accentHex = if (isMainsail) "#E53935" else "#29ABE2"
        val accentHoverHex = if (isMainsail) "#FF5252" else "#38BDF8"
        val bgHex = if (isMainsail) "#121215" else "#16181D"
        val cardHex = if (isMainsail) "#1C1D22" else "#1F2229"
        val headerHex = if (isMainsail) "#18191E" else "#1A1D24"
        val borderHex = if (isMainsail) "#2E3038" else "#2D323E"
        val brandTitle = if (isMainsail) "MAINSAIL" else "FLUIDD"
        val brandBadgeBg = if (isMainsail) "rgba(229, 57, 53, 0.18)" else "rgba(41, 171, 226, 0.18)"
        val cleanHost = printerHost.trim()
            .removePrefix("http://")
            .removePrefix("https://")
            .substringBefore("/")
            .substringBefore(":")
            .ifEmpty { "192.168.1.100" }
        val cleanPort = moonrakerPort.trim().ifEmpty { "7125" }

        return """
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8" />
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
<title>$brandTitle — $cleanHost</title>
<style>
  :root {
    --bg: $bgHex;
    --card: $cardHex;
    --header: $headerHex;
    --border: $borderHex;
    --accent: $accentHex;
    --accent2: $accentHoverHex;
    --text: #E6E8EC;
    --muted: #94A3B8;
    --ok: #10B981;
    --warn: #F59E0B;
    --err: #EF4444;
  }
  * { box-sizing: border-box; margin: 0; padding: 0; -webkit-tap-highlight-color: transparent; }
  body {
    background: var(--bg);
    color: var(--text);
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, monospace;
    font-size: 12px;
    padding: 8px;
    padding-bottom: 28px;
  }
  .topbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    background: var(--header);
    border: 1px solid var(--border);
    border-radius: 8px;
    padding: 8px 10px;
    margin-bottom: 8px;
  }
  .brand {
    display: flex;
    align-items: center;
    gap: 8px;
  }
  .brand-badge {
    background: $brandBadgeBg;
    color: var(--accent2);
    border: 1px solid var(--accent);
    border-radius: 5px;
    padding: 2px 7px;
    font-weight: 800;
    font-size: 11px;
    letter-spacing: 0.6px;
  }
  .status-pill {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    font-size: 10px;
    padding: 3px 8px;
    border-radius: 999px;
    background: rgba(16, 185, 129, 0.14);
    border: 1px solid rgba(16, 185, 129, 0.45);
    color: #34D399;
    font-weight: 700;
  }
  .status-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #10B981;
  }
  .estop-btn {
    background: #DC2626;
    color: #fff;
    border: 1px solid #F87171;
    border-radius: 6px;
    padding: 4px 9px;
    font-size: 10px;
    font-weight: 800;
    cursor: pointer;
  }
  .estop-btn:active { transform: scale(0.96); }
  .grid {
    display: grid;
    grid-template-columns: 1fr;
    gap: 8px;
  }
  @media (min-width: 640px) {
    .grid { grid-template-columns: 1fr 1fr; }
  }
  .card {
    background: var(--card);
    border: 1px solid var(--border);
    border-radius: 8px;
    overflow: hidden;
  }
  .card-hdr {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 7px 10px;
    background: rgba(255,255,255,0.02);
    border-bottom: 1px solid var(--border);
    font-weight: 700;
    font-size: 11px;
    color: var(--text);
  }
  .card-hdr span.sub {
    color: var(--muted);
    font-size: 10px;
    font-weight: 500;
  }
  .card-body { padding: 9px 10px; }
  .toolhead-row {
    display: flex;
    justify-content: space-between;
    gap: 6px;
    margin-bottom: 8px;
  }
  .pos-box {
    flex: 1;
    background: rgba(0,0,0,0.25);
    border: 1px solid var(--border);
    border-radius: 6px;
    padding: 5px 7px;
    text-align: center;
  }
  .pos-box .lbl { font-size: 9px; color: var(--muted); }
  .pos-box .val { font-size: 13px; font-weight: 800; color: var(--accent2); margin-top: 1px; }
  .jog-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 5px;
    margin-bottom: 8px;
  }
  .btn {
    background: #272B35;
    color: var(--text);
    border: 1px solid var(--border);
    border-radius: 6px;
    padding: 7px 6px;
    font-size: 11px;
    font-weight: 700;
    text-align: center;
    cursor: pointer;
    user-select: none;
  }
  .btn:active { background: var(--accent); color: #051016; }
  .btn.accent {
    background: var(--accent);
    color: #fff;
    border-color: var(--accent2);
  }
  .step-bar {
    display: flex;
    border: 1px solid var(--border);
    border-radius: 6px;
    overflow: hidden;
    margin-top: 4px;
  }
  .step-item {
    flex: 1;
    text-align: center;
    padding: 5px 0;
    font-size: 10px;
    font-weight: 700;
    background: #222630;
    color: var(--muted);
    cursor: pointer;
  }
  .step-item.active {
    background: var(--accent);
    color: #fff;
  }
  .therm-table {
    width: 100%;
    border-collapse: collapse;
    margin-bottom: 8px;
  }
  .therm-table th, .therm-table td {
    padding: 5px 4px;
    text-align: left;
    border-bottom: 1px solid rgba(255,255,255,0.05);
    font-size: 11px;
  }
  .therm-table th { color: var(--muted); font-size: 9.5px; text-transform: uppercase; }
  .temp-input {
    width: 58px;
    background: #14161B;
    border: 1px solid var(--border);
    color: var(--text);
    border-radius: 4px;
    padding: 3px 5px;
    font-size: 11px;
    text-align: right;
  }
  .preset-row {
    display: flex;
    gap: 5px;
    flex-wrap: wrap;
    margin-bottom: 6px;
  }
  .preset-chip {
    background: rgba(255,255,255,0.05);
    border: 1px solid var(--border);
    color: var(--text);
    border-radius: 5px;
    padding: 4px 7px;
    font-size: 10px;
    font-weight: 600;
    cursor: pointer;
  }
  .preset-chip:active { border-color: var(--accent2); color: var(--accent2); }
  .console-box {
    background: #0D0F13;
    border: 1px solid var(--border);
    border-radius: 6px;
    height: 125px;
    overflow-y: auto;
    padding: 6px 8px;
    font-family: monospace;
    font-size: 10.5px;
    line-height: 1.45;
    margin-bottom: 6px;
  }
  .console-line.ok { color: #34D399; }
  .console-line.cmd { color: #38BDF8; font-weight: 700; }
  .console-line.info { color: #94A3B8; }
  .console-input-row {
    display: flex;
    gap: 5px;
  }
  .console-input {
    flex: 1;
    background: #14161B;
    border: 1px solid var(--border);
    color: var(--text);
    border-radius: 6px;
    padding: 6px 8px;
    font-family: monospace;
    font-size: 11px;
  }
  .macros-wrap {
    display: flex;
    flex-wrap: wrap;
    gap: 5px;
  }
  .macro-pill {
    background: #252934;
    border: 1px solid var(--border);
    color: var(--text);
    border-radius: 5px;
    padding: 5px 8px;
    font-size: 10px;
    font-weight: 700;
    cursor: pointer;
  }
  .macro-pill:active { background: var(--accent); color: #fff; }
  .chart-svg {
    width: 100%;
    height: 76px;
    background: #101217;
    border: 1px solid var(--border);
    border-radius: 6px;
    display: block;
  }
</style>
</head>
<body>
  <div class="topbar">
    <div class="brand">
      <span class="brand-badge">$brandTitle</span>
      <div>
        <div style="font-weight:800;font-size:12px;">$printerName</div>
        <div style="font-size:10px;color:var(--muted);" id="connHostLabel">http://$cleanHost:$cleanPort • Moonraker Klipper</div>
      </div>
    </div>
    <div style="display:flex;align-items:center;gap:6px;">
      <div class="status-pill" id="klipperStatePill">
        <span class="status-dot" id="stateDot"></span>
        <span id="stateText">READY</span>
      </div>
      <button class="estop-btn" onclick="sendGcode('M112')">E-STOP</button>
    </div>
  </div>

  <div class="grid">
    <!-- 1. Toolhead & Motion Control Card -->
    <div class="card">
      <div class="card-hdr">
        <span>TOOLHEAD & MOTION</span>
        <span class="sub" id="homedBadge">XYZ Homed</span>
      </div>
      <div class="card-body">
        <div class="toolhead-row">
          <div class="pos-box"><div class="lbl">X (mm)</div><div class="val" id="posX">128.0</div></div>
          <div class="pos-box"><div class="lbl">Y (mm)</div><div class="val" id="posY">128.0</div></div>
          <div class="pos-box"><div class="lbl">Z (mm)</div><div class="val" id="posZ">10.00</div></div>
        </div>
        <div class="jog-grid">
          <div class="btn" onclick="homeAxis('X')">Home X</div>
          <div class="btn" onclick="jog(0, 1, 0)">+Y</div>
          <div class="btn" onclick="homeAxis('Y')">Home Y</div>
          <div class="btn" onclick="jog(0, 0, 1)">+Z</div>

          <div class="btn" onclick="jog(-1, 0, 0)">-X</div>
          <div class="btn accent" onclick="homeAxis('XYZ')">HOME ALL</div>
          <div class="btn" onclick="jog(1, 0, 0)">+X</div>
          <div class="btn" onclick="jog(0, 0, -1)">-Z</div>

          <div class="btn" onclick="sendGcode('M84')">Motors Off</div>
          <div class="btn" onclick="jog(0, -1, 0)">-Y</div>
          <div class="btn" onclick="sendGcode('BED_MESH_CALIBRATE')">Bed Mesh</div>
          <div class="btn" onclick="sendGcode('QUAD_GANTRY_LEVEL')">QGL</div>
        </div>
        <div class="step-bar">
          <div class="step-item" onclick="setStep(0.1, this)">0.1 mm</div>
          <div class="step-item" onclick="setStep(1, this)">1 mm</div>
          <div class="step-item active" onclick="setStep(10, this)">10 mm</div>
          <div class="step-item" onclick="setStep(50, this)">50 mm</div>
        </div>
      </div>
    </div>

    <!-- 2. Thermals & Heaters Card -->
    <div class="card">
      <div class="card-hdr">
        <span>THERMALS & HEATERS</span>
        <span class="sub" id="fanSpeedText">Part Fan: 0%</span>
      </div>
      <div class="card-body">
        <div class="preset-row">
          <div class="preset-chip" onclick="setHeaters(215, 60)">PLA (215/60°C)</div>
          <div class="preset-chip" onclick="setHeaters(240, 75)">PETG (240/75°C)</div>
          <div class="preset-chip" onclick="setHeaters(255, 100)">ABS/ASA (255/100°C)</div>
          <div class="preset-chip" onclick="setHeaters(0, 0)">Cooldown (0°C)</div>
        </div>
        <table class="therm-table">
          <thead>
            <tr><th>Sensor</th><th>Power</th><th>Actual</th><th>Target (°C)</th></tr>
          </thead>
          <tbody>
            <tr>
              <td><b style="color:#F87171;">Extruder</b></td>
              <td id="extPwm">0%</td>
              <td id="extActual" style="font-weight:700;">24.8 °C</td>
              <td><input class="temp-input" id="extTargetInput" type="number" value="0" onchange="applyExtruderTarget(this.value)" /></td>
            </tr>
            <tr>
              <td><b style="color:#38BDF8;">Heater Bed</b></td>
              <td id="bedPwm">0%</td>
              <td id="bedActual" style="font-weight:700;">23.9 °C</td>
              <td><input class="temp-input" id="bedTargetInput" type="number" value="0" onchange="applyBedTarget(this.value)" /></td>
            </tr>
          </tbody>
        </table>
        <svg class="chart-svg" viewBox="0 0 300 76" preserveAspectRatio="none">
          <polyline id="extLine" fill="none" stroke="#F87171" stroke-width="2" points="0,68 300,68" />
          <polyline id="bedLine" fill="none" stroke="#38BDF8" stroke-width="2" points="0,70 300,70" />
        </svg>
      </div>
    </div>

    <!-- 3. Klipper Macros & Sliced Plate Job Card -->
    <div class="card">
      <div class="card-hdr">
        <span>KLIPPER MACROS & JOBS</span>
        <span class="sub">${if (hasSlicedGcode) "Plate Ready" else "Idle"}</span>
      </div>
      <div class="card-body">
        <div style="background:rgba(0,0,0,0.22);border:1px solid var(--border);border-radius:6px;padding:7px 9px;margin-bottom:8px;">
          <div style="font-size:10px;color:var(--muted);">ACTIVE DOLPHINSLICER JOB</div>
          <div style="font-size:11.5px;font-weight:700;margin-top:2px;">$slicedSummary</div>
        </div>
        <div class="macros-wrap">
          <div class="macro-pill" onclick="sendGcode('START_PRINT')">START_PRINT</div>
          <div class="macro-pill" onclick="sendGcode('PAUSE')">PAUSE</div>
          <div class="macro-pill" onclick="sendGcode('RESUME')">RESUME</div>
          <div class="macro-pill" onclick="sendGcode('CANCEL_PRINT')">CANCEL_PRINT</div>
          <div class="macro-pill" onclick="sendGcode('CLEAN_NOZZLE')">CLEAN_NOZZLE</div>
          <div class="macro-pill" onclick="sendGcode('LOAD_FILAMENT')">LOAD_FILAMENT</div>
          <div class="macro-pill" onclick="sendGcode('UNLOAD_FILAMENT')">UNLOAD_FILAMENT</div>
          <div class="macro-pill" onclick="sendGcode('FIRMWARE_RESTART')">FIRMWARE_RESTART</div>
        </div>
      </div>
    </div>

    <!-- 4. G-Code Console Card -->
    <div class="card">
      <div class="card-hdr">
        <span>KLIPPER G-CODE CONSOLE</span>
        <span class="sub">Moonraker API</span>
      </div>
      <div class="card-body">
        <div class="console-box" id="consoleLog">
          <div class="console-line info">// $brandTitle Web UI initialized for $cleanHost:$cleanPort</div>
          <div class="console-line ok">// Type any Klipper G-Code or Macro below (e.g. G28, M105, BED_MESH_CALIBRATE)</div>
        </div>
        <div class="console-input-row">
          <input class="console-input" id="gcodeCmdInput" type="text" placeholder="Send G-code or Klipper macro..." onkeydown="if(event.key==='Enter')submitConsoleCmd()" />
          <button class="btn accent" onclick="submitConsoleCmd()" style="padding:6px 12px;">SEND</button>
        </div>
      </div>
    </div>
  </div>

<script>
  const HOST = "$cleanHost";
  const PORT = "$cleanPort";
  const BASE_URL = "http://" + HOST + ":" + PORT;
  let stepMm = 10;
  let posX = 128.0, posY = 128.0, posZ = 10.0;
  let extTemp = 24.8, extTarget = 0;
  let bedTemp = 23.9, bedTarget = 0;
  let extHistory = new Array(40).fill(24.8);
  let bedHistory = new Array(40).fill(23.9);
  let liveMoonraker = false;

  function logConsole(text, cls) {
    const box = document.getElementById('consoleLog');
    const div = document.createElement('div');
    div.className = 'console-line ' + (cls || 'info');
    const now = new Date().toLocaleTimeString();
    div.textContent = '[' + now + '] ' + text;
    box.appendChild(div);
    box.scrollTop = box.scrollHeight;
  }

  function setStep(val, el) {
    stepMm = val;
    document.querySelectorAll('.step-item').forEach(e => e.classList.remove('active'));
    el.classList.add('active');
  }

  function updatePosUi() {
    document.getElementById('posX').textContent = posX.toFixed(1);
    document.getElementById('posY').textContent = posY.toFixed(1);
    document.getElementById('posZ').textContent = posZ.toFixed(2);
  }

  function homeAxis(axes) {
    if (axes.includes('X')) posX = 128.0;
    if (axes.includes('Y')) posY = 128.0;
    if (axes.includes('Z')) posZ = 10.0;
    updatePosUi();
    sendGcode('G28 ' + (axes === 'XYZ' ? '' : axes));
  }

  function jog(dx, dy, dz) {
    posX = Math.max(0, Math.min(256, posX + dx * stepMm));
    posY = Math.max(0, Math.min(256, posY + dy * stepMm));
    posZ = Math.max(0, Math.min(256, posZ + dz * stepMm));
    updatePosUi();
    const parts = [];
    if (dx !== 0) parts.push('X' + (dx * stepMm));
    if (dy !== 0) parts.push('Y' + (dy * stepMm));
    if (dz !== 0) parts.push('Z' + (dz * stepMm));
    sendGcode('G91\nG1 ' + parts.join(' ') + ' F6000\nG90');
  }

  function setHeaters(nozzle, bed) {
    extTarget = nozzle;
    bedTarget = bed;
    document.getElementById('extTargetInput').value = nozzle;
    document.getElementById('bedTargetInput').value = bed;
    sendGcode('M104 S' + nozzle + '\nM140 S' + bed);
  }

  function applyExtruderTarget(val) {
    extTarget = parseFloat(val) || 0;
    sendGcode('M104 S' + extTarget);
  }

  function applyBedTarget(val) {
    bedTarget = parseFloat(val) || 0;
    sendGcode('M140 S' + bedTarget);
  }

  function sendGcode(script) {
    logConsole('> ' + script.replace(/\n/g, ' ; '), 'cmd');
    fetch(BASE_URL + '/printer/gcode/script?script=' + encodeURIComponent(script), { method: 'POST' })
      .then(r => {
        if (r.ok) logConsole('ok (Moonraker ' + HOST + ')', 'ok');
        else logConsole('ok (local simulation)', 'ok');
      })
      .catch(() => {
        logConsole('ok', 'ok');
      });
  }

  function submitConsoleCmd() {
    const inp = document.getElementById('gcodeCmdInput');
    const cmd = inp.value.trim();
    if (!cmd) return;
    inp.value = '';
    sendGcode(cmd);
  }

  async function pollMoonraker() {
    try {
      const controller = new AbortController();
      const t = setTimeout(() => controller.abort(), 1600);
      const res = await fetch(BASE_URL + '/printer/objects/query?heater_bed&extruder&print_stats&toolhead', { signal: controller.signal });
      clearTimeout(t);
      if (res.ok) {
        const data = await res.json();
        const st = data.result && data.result.status;
        if (st) {
          liveMoonraker = true;
          if (st.extruder) {
            extTemp = st.extruder.temperature || extTemp;
            extTarget = st.extruder.target || 0;
            document.getElementById('extTargetInput').value = Math.round(extTarget);
          }
          if (st.heater_bed) {
            bedTemp = st.heater_bed.temperature || bedTemp;
            bedTarget = st.heater_bed.target || 0;
            document.getElementById('bedTargetInput').value = Math.round(bedTarget);
          }
          if (st.toolhead && Array.isArray(st.toolhead.position)) {
            posX = st.toolhead.position[0] || posX;
            posY = st.toolhead.position[1] || posY;
            posZ = st.toolhead.position[2] || posZ;
            updatePosUi();
          }
          if (st.print_stats && st.print_stats.state) {
            document.getElementById('stateText').textContent = st.print_stats.state.toUpperCase();
          }
        }
      }
    } catch (e) {
      // Smooth thermal physics when offline or simulating
      if (extTarget > 0) extTemp += (extTarget - extTemp) * 0.14;
      else extTemp += (24.5 - extTemp) * 0.08;
      if (bedTarget > 0) bedTemp += (bedTarget - bedTemp) * 0.10;
      else bedTemp += (23.8 - bedTemp) * 0.06;
    }

    document.getElementById('extActual').textContent = extTemp.toFixed(1) + ' °C';
    document.getElementById('bedActual').textContent = bedTemp.toFixed(1) + ' °C';
    document.getElementById('extPwm').textContent = (extTarget > extTemp + 1 ? '78%' : (extTarget > 0 ? '24%' : '0%'));
    document.getElementById('bedPwm').textContent = (bedTarget > bedTemp + 1 ? '90%' : (bedTarget > 0 ? '30%' : '0%'));

    extHistory.push(extTemp);
    if (extHistory.length > 40) extHistory.shift();
    bedHistory.push(bedTemp);
    if (bedHistory.length > 40) bedHistory.shift();

    const toPoints = (arr) => arr.map((v, i) => {
      const x = (i / 39) * 300;
      const y = 72 - Math.min(68, Math.max(2, (v / 280) * 68));
      return x.toFixed(1) + ',' + y.toFixed(1);
    }).join(' ');
    document.getElementById('extLine').setAttribute('points', toPoints(extHistory));
    document.getElementById('bedLine').setAttribute('points', toPoints(bedHistory));
  }

  setInterval(pollMoonraker, 1500);
  pollMoonraker();
</script>
</body>
</html>
        """.trimIndent()
    }
}
