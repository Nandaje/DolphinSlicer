# 🐬 DolphinSlicer (OrcaSlicer for Android)

**DolphinSlicer** is a native Android 3D printing slicer inspired by [OrcaSlicer](https://github.com/OrcaSlicer/OrcaSlicer), featuring hardware-accelerated **OpenGL ES 3.0** 3D rendering, a **1:1 OrcaSlicer desktop UI layout** with adaptive **Portrait & Landscape** support, live official OrcaSlicer server profile synchronization, and a multi-threaded two-pass slicing engine.

## 📸 Screenshot Preview

<p align="center">
  <img src="docs/screenshots/dolphinslicer-preview.png" alt="DolphinSlicer Android Preview" width="380" />
</p>

---

## ✨ Features

- **1:1 OrcaSlicer UI & Workflow**:
  - **Top Navigation Bar (`#1F2022`)**: Home menu, `Prepare` | `Preview` | `Device` | `Project` tabs, `Sync Server`, `Import`, `Slice plate`, and `Export`.
  - **Left Sidebar (`#262729`)**: Printer & Nozzle selector, numbered multi-filament slots, `Global | Objects` process toggle, and 6 process parameter tabs (`Quality`, `Strength`, `Speed`, `Support`, `Multimaterial`, `Others`).
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

## 🛠️ Building from Source

```bash
./gradlew testDebugUnitTest assembleDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

---

## 👥 Contributors

- **[@Nandaje](https://github.com/Nandaje)** — Creator & Lead Maintainer
- **Google Gemini 4 Antigravity** *(Google DeepMind)* — AI Engineering & Architecture Contributor

