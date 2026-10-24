# <img src="docs/screenshots/dolphin-icon.png" alt="DolphinSlicer Icon" width="36" align="center" /> DolphinSlicer (OrcaSlicer for Android)

**DolphinSlicer** is a native Android 3D printing slicer inspired by [OrcaSlicer](https://github.com/OrcaSlicer/OrcaSlicer), featuring hardware-accelerated **OpenGL ES 3.0** 3D rendering, a **1:1 OrcaSlicer desktop UI layout** with adaptive **Portrait & Landscape** support, live official OrcaSlicer server profile synchronization, and a multi-threaded two-pass slicing engine.

> 🐬 **Why "DolphinSlicer"?**
> Because an **Orca** is big (actually the largest member of the dolphin family, built for desktop PCs), and a **Dolphin** is its smaller, agile cousin that fits right in your pocket on Android!
>
> 🤖 **Vibe-Coding Disclosure:**
> This project was vibe-coded using **Google Gemini 4 Argon (Antigravity)** using the official [OrcaSlicer](https://github.com/OrcaSlicer/OrcaSlicer) repository as a reference and remaking it from scratch into native Android **Kotlin**. As the human creator ([@Nandaje](https://github.com/Nandaje)), I gave the direction on what to build, requested features and UI refinements, and tested every build directly on my phone.

## 📸 Screenshot Preview

<p align="center">
  <img src="docs/screenshots/dolphinslicer-prepare.png" alt="DolphinSlicer Prepare Tab" width="215" />
  <img src="docs/screenshots/dolphinslicer-slicing.png" alt="DolphinSlicer Slicing Engine" width="215" />
  <img src="docs/screenshots/dolphinslicer-preview.png" alt="DolphinSlicer 3D Toolpath Preview" width="215" />
  <img src="docs/screenshots/dolphinslicer-device.png" alt="DolphinSlicer Device Klipper Web UI" width="215" />
</p>

---

## ✨ Features

- **1:1 OrcaSlicer UI & Workflow**:
  - **Compact Top Navigation Bar (`#1F2022`)**: Unified Dolphin menu, `Prepare` | `Preview` | `Device` | `Project` tabs, and quick `Import`, `Slice`, and `Export` actions.
  - **Complete OrcaSlicer Process Settings**: Full 6-tab parameter suite (`Quality`, `Strength`, `Speed`, `Support`, `Multimaterial`, `Others`) including Precision, Seam, Scarf Joint, Ironing, Arachne/Classic Wall Generator, Overhang Speeds, Acceleration/Jerk, Prime Tower, Skirt/Brim, Special Modes, and G-code Post-Processing.
  - **Custom Currency System & Cost Calculator**: Preset global currencies (`$`, `€`, `£`, `¥`, `Rp`, `₹`, `₩`, `R$`, `₱`, `฿`, `₫`) plus a free-form custom currency symbol/code input box that updates across all filament profiles and production cost breakdowns.
  - **3D Viewport Overlays**: Interactive 3D Orientation **ViewCube** (`Top` / `Front` / `Left` with RGB `X`/`Y`/`Z` axes), sidebar collapse button (`‹›`), and 3D manipulation gizmos (`Move`, `Scale`, `Rotate`, `Lay Flat`, `Cut at Z`, `Mirror`, `Measure`, `Auto-Arrange`, `Clone`).
- **OpenGL ES 3.0 GPU Acceleration**:
  - Real-time 3D build plate rendering with overhang severity highlighting.
  - Volumetric 3D extruded bead ribbon preview (`GL_TRIANGLES`) with layer range and single-layer nozzle progress scrubbing.
- **Two-Pass Multi-Threaded Slicing Engine**:
  - Generates **Outer Wall**, **Inner Wall**, **Bottom Surface**, **Top Surface** (including intermediate horizontal roofs/steps and optional Ironing), **Internal Solid Infill**, **Sparse Infill** (`Gyroid`, `Grid`, `Honeycomb`, `Adaptive Cubic`, `Triangles`, `Tri-Hexagon`, `Rectilinear`, `Concentric`, `Cross Hatch`, `Lightning`), **Supports** (`Normal Grid` & `Tree Organic`), and **Skirt / Brim**.
- **Live Official OrcaSlicer Server Profile Sync**:
  - Synchronizes printer and filament profiles directly from the official `OrcaSlicer/OrcaSlicer` GitHub repository across 23 vendor catalogs (`Bambu Lab`, `Creality`, `Prusa`, `Voron`, `Qidi`, `Sovol`, `Elegoo`, `Anycubic`, `Flashforge`, `Flsun`, `RatRig`, etc.), in addition to 56+ built-in printer profiles and 51+ built-in filament profiles.
- **Filament & Production Cost Calculator**:
  - Computes model vs. support filament weight (`g`), length (`m`), volume (`cm³`), spool usage (`%` and prints/spool), electricity (`kWh`), machine wear, total production cost, and suggested quote price.
- **File Import & Export**:
  - Import: `.stl` (Binary & ASCII), `.obj`, `.3mf`, `.gcode`, and `.json` / `.ini` profiles.
  - Export: `.gcode` (Bambu / Klipper / Marlin / Prusa / RepRap), `.3mf` project archives, and Binary `.stl`.
- **Direct Wi-Fi Printer Control (`Device` Tab)**:
  - Upload and start prints directly over LAN to **Moonraker / Klipper** (Mainsail / Fluidd) and **OctoPrint** servers.

---

## 🗂️ Repository Structure & Maintainer Guide

The codebase is organized into clean, modular Kotlin packages with comprehensive KDoc comments so any developer can maintain or extend it manually without needing AI tools. See [**docs/ARCHITECTURE.md**](docs/ARCHITECTURE.md) for the full step-by-step developer guide.

```text
app/src/main/java/com/orcaslicer/android/
├── MainActivity.kt               # Android entry point, SharedPreferences init, & file URI intents
├── gpu/                          # Hardware-accelerated OpenGL ES 3.0 3D engine
│   ├── OrcaGlRenderer.kt         # Shaders, build plate grid, 3D mesh, gizmos, & G-code ribbons
│   └── OrcaGlViewport.kt         # Multi-touch gesture handler (orbit, pan, zoom, gizmo drag)
├── io/                           # 3D model & profile importers/exporters
│   ├── ModelImporter.kt          # Binary/ASCII STL, Wavefront OBJ, 3MF ZIP, & G-code reader
│   ├── ModelExporter.kt          # G-code writer, Binary STL exporter, & 3MF project archiver
│   └── ProfileImporter.kt        # OrcaSlicer / PrusaSlicer .json & .ini profile importer
├── model/                        # Core data structures, 3D math, & built-in presets
│   ├── Geometry.kt               # Vec2, Vec3, Triangle, MeshData, SceneObject, SliceResult
│   ├── ProceduralMeshes.kt       # Primitives, Calibration models, & Z-plane mesh cutter
│   └── SlicerProfiles.kt         # PrinterProfile, FilamentProfile, ProcessProfile, BuiltInPresets
├── network/                      # LAN printer control & GitHub profile sync
│   ├── PrinterNetworkClient.kt   # Moonraker (Klipper) & OctoPrint REST upload/telemetry
│   └── OrcaServerProfileSync.kt  # Fetches official vendor profiles from OrcaSlicer GitHub
├── slicer/                       # Core 3D-to-GCode slicing engine
│   └── SlicingEngine.kt          # Two-pass parallel contour, wall, skin, infill, & support engine
└── ui/                           # Modular Jetpack Compose UI screens & ViewModel
    ├── OrcaTheme.kt              # Centralized dark-charcoal & Dolphin Azure color palette
    ├── OrcaViewModel.kt          # Single source of truth (OrcaUiState) & user actions
    ├── OrcaSlicerScreen.kt       # Main responsive screen, 3D viewport overlays, & bottom drawer
    ├── OrcaSettingsPanels.kt     # Reusable parameter rows & 6-tab Process Settings suite
    ├── OrcaProfileModals.kt      # Printer/Filament browsers, editors, & Calibration panel
    ├── DeviceNetworkPanel.kt     # Embedded Klipper WebView browser & Cost Calculator cards
    └── KlipperWebUiTemplates.kt  # Built-in fallback Fluidd/Mainsail HTML templates
```

---

## 🛠️ Building from Source

```bash
./gradlew testDebugUnitTest assembleDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

---

## 👥 Contributors & Credits

### Contributors
- **[@Nandaje](https://github.com/Nandaje)** — Creator & Lead Maintainer
- **Google Gemini 4 Antigravity** *(Google DeepMind)* — AI Engineering & Architecture Contributor

### Special Credits & Upstream Inspiration
- **[OrcaSlicer](https://github.com/OrcaSlicer/OrcaSlicer)** — UI/UX workflow, calibration tools, scarf seam concepts, and official printer & filament profile catalogs.
- **[PrusaSlicer](https://github.com/prusa3d/PrusaSlicer)** — Foundational slicing architecture, perimeter/infill path generation principles, and 3MF ecosystem standards.
- **[SuperSlicer](https://github.com/supermerill/SuperSlicer)** — Advanced print calibration, fine-grained process parameter tuning, and community slicer innovations.
- **[UltiMaker Cura](https://github.com/Ultimaker/Cura)** *(CuraSlicer / CuraEngine)* — Arachne variable-width perimeter concepts, tree support inspirations, and open-source 3D printing foundations.

---

## ☕ Support & Donation

If you find **DolphinSlicer** useful for your 3D printing workflow, consider supporting ongoing development:

[![Buy Me A Coffee](https://img.shields.io/badge/Buy%20Me%20a%20Coffee-ffdd00?style=for-the-badge&logo=buy-me-a-coffee&logoColor=black)](https://buymeacoffee.com/nandaje)

- **Buy Me a Coffee (@Nandaje)**: [https://buymeacoffee.com/nandaje](https://buymeacoffee.com/nandaje)

### 🐋 Don't Forget to Support the Official OrcaSlicer Team!
Don't forget to also give a donation to the official **[OrcaSlicer Team on Ko-fi](https://ko-fi.com/G2G5IP3CP)** so **DolphinSlicer** can grow as big and powerful as **OrcaSlicer** as they continue to develop!

[![Support OrcaSlicer on Ko-fi](https://img.shields.io/badge/Support_OrcaSlicer_on_Ko--fi-FF5E5B?style=for-the-badge&logo=ko-fi&logoColor=white)](https://ko-fi.com/G2G5IP3CP)

- **OrcaSlicer Official Ko-fi**: [https://ko-fi.com/G2G5IP3CP](https://ko-fi.com/G2G5IP3CP)


