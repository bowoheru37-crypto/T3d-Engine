# T3D Studio Engine Architecture & Administration Documentation

## 1. Overview & System Architecture

T3D Studio Engine is a high-performance 3D game engine and editor built natively for Android using Kotlin and Jetpack Compose. Designed with a modular Entity-Component-System (ECS) architecture, it targets smooth 60 FPS rendering and interactive 3D authoring on mobile devices, including low-entry class smartphones (1GB RAM minimum, e.g., itel A70).

```
                      +------------------------------------------+
                      |         Jetpack Compose Viewport         |
                      |          (Viewport3DCanvas.kt)          |
                      +--------------------+---------------------+
                                           |
                                           v
                      +--------------------+---------------------+
                      |           Renderer3D Core            |
                      |   (Frustum Culling, PS2 Dither/Shading)  |
                      +--------------------+---------------------+
                                           |
    +-----------------------+--------------+--------------+-----------------------+
    |                       |                             |                       |
    v                       v                             v                       v
+---+------------------+ +--+------------------------+ +--+------------------+ +--+------------------+
|    Entity Component  | |   Coroutine Physics 3D    | |    Math3D & Mesh3D  | | Procedural Generators|
|   System (ECS.kt)    | |  (Physics3D / Coroutine)  | |  (FastTrig, Quads)  | |  (Terrain/FBM/Asset) |
+----------------------+ +---------------------------+ +---------------------+ +---------------------+
```

---

## 2. Core Engine Subsystems

### A. Entity-Component System (ECS)
- **Entity (`Entity.kt`)**: Lightweight game object container holding `Transform`, `Mesh`, `Material`, `RigidBody3D`, `Collider3D`, `Light`, `ScriptComponent`, and `AIBehaviorComponent`.
- **Transform (`Transform.kt`)**: Position, Euler rotation angles, and Scale matrices, calculated via hierarchical global matrix multiplication.
- **Scene3D (`ECS.kt`)**: World container maintaining active entities, Camera3D, lighting, skybox, ambient color, and physics world state.

### B. Geometry, Polygons & Mesh Factory (`Mesh3D.kt`)
- **Primitives**: `createQuad`, `createPlane`, `createCube`, `createSphere`, `createCylinder`, `createTorus`, `createTerrain`, `createCharacterModel`.
- **Quad & Polygon Triangulation**: Quads are constructed with explicit CCW (Counter-Clockwise) winding order `(v0, v1, v2)` and `(v0, v2, v3)`, with normalized per-face UV coordinates `(0..1)`.
- **Normals Calculation**: Automatic face and vertex normal averaging via `recalculateNormals()`.

### C. Math, Trigonometry & Perspective Projection (`Math3D.kt`)
- **Fast Trigonometry (`FastTrig`)**: 3600-entry lookup table for ultra-fast degree-based sine/cosine calculations, bypassing standard JVM Math library overhead.
- **Camera Projection**:
  - `Matrix4x4.perspective(fov, aspect, near, far)`: Perspective projection matrix guarded against zero-aspect and near-plane clipping boundaries.
  - `Matrix4x4.lookAt(eye, target, up)`: View matrix calculation with fallback up-vectors to prevent matrix degeneracy when looking vertically (Top/Bottom camera presets).

### D. PS2-Era Visual Style & Renderer (`Renderer3D.kt`, `Texture3D.kt`)
- **PS2 Aesthetic Emulation**:
  - Gouraud/Phong lighting model combining Diffuse, Specular, Ambient, and Emission intensities.
  - **PS2 Color Quantization & Dithering (`samplePS2`)**: Quantizes RGB color channels into discrete color bands (4 to 64 levels), replicating the crisp, retro PlayStation 2 visual style.
- **Clipping & Culling**: Near-plane clipping prevents negative depth invert artifacts; 2D screen space back-face culling eliminates non-visible polygons; Painter's Algorithm depth sorting sorts faces from back to front.

---

## 3. Streamlined 3D Placement & Editor Layout Tools

Inspired by industry-standard game engines (**Unity**, **Unreal Engine**, **Godot**), T3D Engine provides streamlined 3D placement and layout controls:

| Feature / Tool | T3D Engine Implementation | Unity Equivalent | Unreal Engine Equivalent | Godot Equivalent |
|---|---|---|---|---|
| **Grid Step Snapping** | `snapSelectedToGrid(step = 0.5f)` | Grid Snap (`Ctrl + Shift`) | Grid Snap (`[ / ]`) | Grid Snap (`G`) |
| **Surface Grounding** | `groundSelectedToSurface()` | Align to Ground (`End`) | Snap to Floor (`End`) | Snap to Floor (`Ctrl + Alt + S`) |
| **Origin Alignment** | `alignSelectedToOrigin()` | Reset Transform | Reset Location | Reset Transform |
| **3D Transform Gizmos** | Translate, Rotate, Scale, Sculpt | Translate/Rotate/Scale | W/E/R Gizmos | Move/Rotate/Scale |
| **Camera Presets** | Top, Front, Side, Isometric, Perspective | Scene View Gizmo | Perspective/Top/Front Views | Viewport Presets |

---

## 4. Mobile Device Optimization Guidelines (itel A70 / 1GB RAM)

T3D Engine is optimized for low-end mobile hardware:

1. **Zero-Allocation Render Loops**: Vector/Matrix math in hot rendering loops avoids runtime allocations to eliminate Android Garbage Collector (GC) micro-stutters.
2. **Polygonal Budget Tiering**:
   - `LOW_ITEL_A70`: Terrain grid capped at 12x12 (under 600 vertices), max 30 active dynamic physics bodies.
   - `LOW_ITEL_A70_60FPS`: Capped polycount with baked lighting and simplified 30FPS / 60FPS frame budget targets.
3. **RAM Memory Budget Manager**: Hardware profiler monitors heap usage and provides automated RAM cleanup via `forceGarbageCollection()`.

---

## 5. Administration, Build & Deployment Procedures

### A. Environment Requirements
- JDK 17 or higher
- Android SDK 35 (compileSdk 35, minSdk 24)
- Gradle Wrapper 8.12.1
- Android Gradle Plugin 8.8.2

### B. Common Build & Test Commands
```bash
# 1. Compile Debug APK
./gradlew assembleDebug

# 2. Compile Optimized Release APK
./gradlew assembleRelease

# 3. Run Engine Unit Tests
./gradlew test
```

### C. Export Targets
T3D Studio supports exporting completed 3D scenes and games into:
1. **Sketchware Pro v7 Project**: Native mobile IDE compatible code package.
2. **Android Studio Gradle Project**: Kotlin + Compose application codebase.
3. **HTML5 / WebGL Web Package**: Web-ready 3D canvas project.
4. **Standalone Android APK**: Direct installable application package.

---
*Document Version:* 1.2.0
*Maintained by:* T3D Engine Architecture Team
