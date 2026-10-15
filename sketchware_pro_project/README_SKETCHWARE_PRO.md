# T3D Engine Project Template - Sketchware Pro V7.0.0 Stable Release

Proyek ini dibuat khusus dan dioptimalkan secara mendalam untuk **Sketchware Pro V7.0.0 Stable**:
- **Pure Java 7/8 (No Lambda)**: Menggunakan `new View.OnClickListener()`, `new Runnable()`, dan standar loop tanpa ekspresi lambda atau stream Java 8 yang rawan error pada compiler bawaan Sketchware Pro.
- **Pure Standard Android Libraries**: 100% menggunakan library native Android (`android.graphics`, `android.view`, `android.os`, `android.content`) tanpa external dependency yang membingungkan.
- **Zero GC Pressure (No Lags / No Stutter)**: Menggunakan `T3DMathPool` (pre-allocated object pooling untuk Vector3, Matrix, Paint, Point) sehingga **0 alokasi memori** per frame di dalam `onDraw` / `renderLoop`.
- **Anti-Overheating (No Heat)**: Dilengkapi dynamic frame limiter (60 FPS / 30 FPS Eco Mode) dengan precision sleep & delta-time cap untuk mencegah CPU thermal throttling.
- **Zero Memory Leaks & Zero NPE**: Pengelolaan lifecycle ketat (`onPause`, `onResume`, `onDestroy`) dengan pembersihan thread & callback secara tuntas.
- **Zero Crash / Zero FC**: Semua operasi vector, matrix, dan rendering diproteksi boundary checks dan try-catch defensif.

---

### Struktur Folder:
```
sketchware_pro_project/
├── AndroidManifest.xml
├── project_config.json
├── README_SKETCHWARE_PRO.md
├── res/
│   ├── layout/
│   │   └── main.xml
│   └── values/
│       ├── colors.xml
│       ├── strings.xml
│       └── styles.xml
└── src/
    └── com/
        └── t3d/
            └── sketchware/
                ├── MainActivity.java
                └── engine/
                    ├── T3DEngineCore.java
                    ├── T3DEntity.java
                    ├── T3DMathPool.java
                    ├── T3DMeshFactory.java
                    └── T3DSurfaceView.java
```

---

### Cara Menggunakan di Sketchware Pro V7.0.0:
1. **Buka Sketchware Pro V7.0.0**.
2. Buat project baru dengan package name `com.t3d.sketchware`.
3. Masukkan kode Java dari folder `src/com/t3d/sketchware/` ke menu **Source Code / Java** di Sketchware Pro.
4. Ganti isi `main.xml` dengan layout yang tersedia di `res/layout/main.xml`.
5. Run & Compile langsung menjadi APK!
