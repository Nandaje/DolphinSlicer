# Architecture & Human Maintainer Guide

This document explains how **DolphinSlicer** is structured so any Android / Kotlin developer can easily navigate, debug, and extend the codebase manually without needing AI assistance.

---

## 📁 Directory & Package Structure

```text
orca-android/
├── app/
│   ├── build.gradle.kts                  # Android app versionCode, versionName, & dependencies
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml       # Permissions, file intent filters (.stl, .3mf, .obj, .gcode)
│       │   ├── java/com/orcaslicer/android/
│       │   │   ├── MainActivity.kt       # App entry point, SharedPreferences init, & file URI intents
│       │   │   ├── gpu/                  # Hardware-accelerated OpenGL ES 3.0 3D engine
│       │   │   │   ├── OrcaGlRenderer.kt # Shaders, build plate grid, 3D mesh, gizmos, & G-code ribbons
│       │   │   │   └── OrcaGlViewport.kt # Multi-touch gesture handler (orbit, pan, zoom, gizmo drag)
│       │   │   ├── io/                   # File parsers & exporters
│       │   │   │   ├── ModelImporter.kt  # Binary/ASCII STL, Wavefront OBJ, 3MF ZIP, & G-code reader
│       │   │   │   ├── ModelExporter.kt  # G-code writer, Binary STL exporter, & 3MF project archiver
│       │   │   │   └── ProfileImporter.kt# OrcaSlicer / PrusaSlicer .json & .ini profile importer
│       │   │   ├── model/                # Core data structures, 3D math, & built-in presets
│       │   │   │   ├── Geometry.kt       # Vec2, Vec3, Triangle, MeshData, SceneObject, SliceResult
│       │   │   │   ├── ProceduralMeshes.kt # Primitives, Calibration models, & Z-plane mesh cutter
│       │   │   │   └── SlicerProfiles.kt # PrinterProfile, FilamentProfile, ProcessProfile, BuiltInPresets
│       │   │   ├── network/              # LAN printer control & GitHub profile sync
│       │   │   │   ├── PrinterNetworkClient.kt # Moonraker (Klipper) & OctoPrint REST upload/telemetry
│       │   │   │   └── OrcaServerProfileSync.kt# Fetches official vendor profiles from OrcaSlicer GitHub
│       │   │   ├── slicer/               # Core 3D-to-GCode slicing engine
│       │   │   │   └── SlicingEngine.kt  # Two-pass parallel contour, wall, skin, infill, & support engine
│       │   │   └── ui/                   # Jetpack Compose UI screens & ViewModel
│       │   │       ├── OrcaTheme.kt          # Centralized dark-charcoal & Dolphin Azure color palette
│       │   │       ├── OrcaViewModel.kt      # Single source of truth (OrcaUiState) & user actions
│       │   │       ├── OrcaSlicerScreen.kt   # Main responsive screen, 3D viewport overlays, & bottom drawer
│       │   │       ├── OrcaSettingsPanels.kt # Reusable parameter rows & 6-tab Process Settings suite
│       │   │       ├── OrcaProfileModals.kt  # Printer/Filament browsers, editors, & Calibration panel
│       │   │       ├── DeviceNetworkPanel.kt # Embedded Klipper WebView browser & Cost Calculator cards
│       │   │       └── KlipperWebUiTemplates.kt # Built-in fallback Fluidd/Mainsail HTML templates
│       │   └── res/                      # App icons & vector drawables
│       └── test/                         # JUnit4 unit tests for geometry, slicing, & importers
└── docs/
    ├── ARCHITECTURE.md                   # This developer & maintainer guide
    └── screenshots/                      # README screenshots & attribution sidecars
```

---

## 🛠️ Common Maintenance Recipes (How to Add or Change Things)

### 1. How to Add a New Slicing Parameter
1. **Model (`model/SlicerProfiles.kt`)**:
   Add the new property with a sensible default value to `data class ProcessProfile(...)`.
2. **UI Control (`ui/OrcaSettingsPanels.kt`)**:
   Inside `FullProcessSettingsCard`, locate the appropriate tab (`Quality`, `Strength`, `Speed`, `Support`, `Multimaterial`, or `Others`) and add an `OrcaParamNumberRow`, `OrcaParamIntStepperRow`, `OrcaParamCheckboxRow`, or `OrcaParamDropdownRow`.
3. **Slicing Logic (`slicer/SlicingEngine.kt`)**:
   Read `process.<yourNewField>` inside `SlicingEngine.slicePlate(...)` or `generateGCodeAndStats(...)`.

### 2. How to Add a New Built-in Printer or Filament Preset
- Open [`model/SlicerProfiles.kt`](../app/src/main/java/com/orcaslicer/android/model/SlicerProfiles.kt) and scroll to `object BuiltInPresets`.
- Add a new `PrinterProfile(...)` entry to `defaultPrinters()` or a new `FilamentProfile(...)` entry to `defaultFilaments()`.

### 3. How to Modify the 3D Viewport or Transform Gizmos
- **Rendering & Ray-Picking**: Open [`gpu/OrcaGlRenderer.kt`](../app/src/main/java/com/orcaslicer/android/gpu/OrcaGlRenderer.kt).
  - Coordinate system is **right-handed Z-up** (`X` = right, `Y` = back, `Z` = up from `0 mm` bed floor).
  - `drawTransformGizmo(...)` renders the 3D Move/Scale/Rotate handles, Cut plane, and Lay-on-Face patches.
  - `pickGizmoAxis(...)` and `pickLayOnFaceNormal(...)` perform 3D ray-intersection against touch coordinates.
- **Touch Gestures**: Open [`gpu/OrcaGlViewport.kt`](../app/src/main/java/com/orcaslicer/android/gpu/OrcaGlViewport.kt) (`onTouchEvent`).
- **Floating Tool Overlays**: Open [`ui/OrcaSlicerScreen.kt`](../app/src/main/java/com/orcaslicer/android/ui/OrcaSlicerScreen.kt) (`ViewportWithTouchOverlays` and `FloatingGizmoControlCard`).

### 4. How the Slicing Engine Works (`slicer/SlicingEngine.kt`)
The slicing engine runs asynchronously on `Dispatchers.Default` in two parallel passes:
1. **Pass 1 (`layerGeometries`)**:
   - Slices all world-space triangles at each layer height `sliceZ` (`intersectTrianglesAtZ`).
   - Chains intersection segments into closed counter-clockwise polygons (`chainSegmentsIntoPolygons`).
   - Offsets outer contours inward (`offsetPolygonInward`) to generate `Outer Wall`, `Inner Wall`, and the `innermostBoundary` for infill.
2. **Pass 2 (`layers`)**:
   - Detects top/bottom horizontal skins and intermediate horizontal roofs/steps by comparing polygon coverage against layers above and below.
   - Generates clipped infill lines (`generateClippedInfillSegments`) for `Gyroid`, `Grid`, `Honeycomb`, `Adaptive Cubic`, `Concentric`, `Lightning`, etc.
   - Generates `Normal Grid` or `Tree Organic` support structures (`computeTreeSupportPlans`).
   - Applies inside-boundary combed travel moves (`computeCuraCombedTravel`) and serializes the final G-code (`generateGCodeAndStats`).

### 5. How to Build, Test, and Release
```bash
# Run all unit tests and build the debug APK
./gradlew testDebugUnitTest assembleDebug
```
The compiled APK will be output to:
`app/build/outputs/apk/debug/app-debug.apk`
