package com.t3d.sketchware.engine;

import android.graphics.Canvas;
import android.graphics.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * T3DEngineCore - High-Performance 3D Software Rasterizer for Sketchware Pro V7.0.0.
 * Pure Java 7/8, zero lambdas, zero allocations during render, strictly null-safe.
 */
public class T3DEngineCore {

    public final List<T3DEntity> entities;

    // Camera Parameters
    public float cameraDistance;
    public float cameraYaw;
    public float cameraPitch;
    public final T3DMathPool.Vector3 cameraTarget;

    // View & Projection Caches
    private final T3DMathPool.Matrix4x4 viewMatrix;
    private final T3DMathPool.Matrix4x4 vpMatrix;
    private final T3DMathPool.Vector3 tempWorldPos;
    private final T3DMathPool.Vector3 tempViewPos;

    // Projected Vertices Cache (Dynamic buffer)
    private T3DMathPool.ScreenPoint[] screenPoints;
    private int screenPointsCapacity;

    // Depth Sorting Buffer
    private final List<T3DMeshFactory.Triangle> visibleTriangles;

    // Triangle Depth Comparator (Anonymous class instance, NO lambda)
    private final Comparator<T3DMeshFactory.Triangle> depthComparator = new Comparator<T3DMeshFactory.Triangle>() {
        @Override
        public int compare(T3DMeshFactory.Triangle t1, T3DMeshFactory.Triangle t2) {
            // Far to Near for correct Painter's Algorithm
            if (t1.avgDepth < t2.avgDepth) return 1;
            if (t1.avgDepth > t2.avgDepth) return -1;
            return 0;
        }
    };

    public T3DEngineCore() {
        this.entities = new ArrayList<T3DEntity>();
        this.cameraDistance = 8.0f;
        this.cameraYaw = 35.0f;
        this.cameraPitch = 25.0f;
        this.cameraTarget = new T3DMathPool.Vector3(0f, 0f, 0f);

        this.viewMatrix = new T3DMathPool.Matrix4x4();
        this.vpMatrix = new T3DMathPool.Matrix4x4();
        this.tempWorldPos = new T3DMathPool.Vector3();
        this.tempViewPos = new T3DMathPool.Vector3();

        this.screenPointsCapacity = 256;
        this.screenPoints = new T3DMathPool.ScreenPoint[screenPointsCapacity];
        for (int i = 0; i < screenPointsCapacity; i++) {
            screenPoints[i] = new T3DMathPool.ScreenPoint();
        }

        this.visibleTriangles = new ArrayList<T3DMeshFactory.Triangle>(512);

        initDefaultScene();
    }

    public void initDefaultScene() {
        entities.clear();

        // 1. Center Cube
        T3DEntity cube = new T3DEntity("Main Cube");
        cube.mesh = T3DMeshFactory.createCube(2.0f, Color.argb(220, 0, 240, 255));
        cube.position.set(0f, 0f, 0f);
        entities.add(cube);

        // 2. Floating Sphere
        T3DEntity sphere = new T3DEntity("Planet");
        sphere.mesh = T3DMeshFactory.createSphere(1.2f, 10, 10, Color.argb(220, 168, 85, 247));
        sphere.position.set(-3.5f, 1.5f, -1.0f);
        entities.add(sphere);

        // 3. Grid Platform
        T3DEntity terrain = new T3DEntity("Ground Terrain");
        terrain.mesh = T3DMeshFactory.createTerrainGrid(8, 1.2f, Color.argb(160, 16, 185, 129));
        terrain.position.set(0f, -2.0f, 0f);
        entities.add(terrain);
    }

    public void addEntity(T3DEntity entity) {
        if (entity != null) {
            entities.add(entity);
        }
    }

    public void update(float deltaTime) {
        // Auto-spin entities slightly
        for (int i = 0; i < entities.size(); i++) {
            T3DEntity e = entities.get(i);
            if (e != null && e.isEnabled) {
                if (i == 0) {
                    e.rotation.y += 25.0f * deltaTime;
                    e.rotation.x += 15.0f * deltaTime;
                } else if (i == 1) {
                    e.rotation.y -= 30.0f * deltaTime;
                }
                e.updateMatrix();
            }
        }
    }

    private void ensureScreenPointsCapacity(int required) {
        if (required > screenPointsCapacity) {
            int newCap = Math.max(required, screenPointsCapacity * 2);
            T3DMathPool.ScreenPoint[] newArr = new T3DMathPool.ScreenPoint[newCap];
            System.arraycopy(screenPoints, 0, newArr, 0, screenPointsCapacity);
            for (int i = screenPointsCapacity; i < newCap; i++) {
                newArr[i] = new T3DMathPool.ScreenPoint();
            }
            screenPoints = newArr;
            screenPointsCapacity = newCap;
        }
    }

    public void render(Canvas canvas, int width, int height) {
        if (canvas == null || width <= 0 || height <= 0) return;

        // Clear Background
        canvas.drawColor(Color.parseColor("#0A0E17"));

        float cx = width * 0.5f;
        float cy = height * 0.5f;
        float fov = Math.min(width, height) * 0.9f;

        // Update Camera View Matrix
        float radYaw = (float) Math.toRadians(cameraYaw);
        float radPitch = (float) Math.toRadians(cameraPitch);

        float camX = (float) (cameraTarget.x + cameraDistance * Math.cos(radPitch) * Math.sin(radYaw));
        float camY = (float) (cameraTarget.y + cameraDistance * Math.sin(radPitch));
        float camZ = (float) (cameraTarget.z + cameraDistance * Math.cos(radPitch) * Math.cos(radYaw));

        // Draw 3D Grid Axis
        drawGridAxis(canvas, cx, cy, fov, camX, camY, camZ);

        // Render each entity
        for (int eIdx = 0; eIdx < entities.size(); eIdx++) {
            T3DEntity entity = entities.get(eIdx);
            if (entity == null || !entity.isEnabled || entity.mesh == null) continue;

            T3DMeshFactory.Mesh mesh = entity.mesh;
            int vertCount = mesh.vertices.size();
            ensureScreenPointsCapacity(vertCount);

            // Project all vertices
            for (int vIdx = 0; vIdx < vertCount; vIdx++) {
                T3DMeshFactory.Vertex v = mesh.vertices.get(vIdx);
                entity.modelMatrix.transform(v.position, tempWorldPos);

                // View Space Transform relative to camera
                float vx = tempWorldPos.x - camX;
                float vy = tempWorldPos.y - camY;
                float vz = tempWorldPos.z - camZ;

                // Orbit rotation inverse
                float rYx = (float) (vx * Math.cos(-radYaw) - vz * Math.sin(-radYaw));
                float rYz = (float) (vx * Math.sin(-radYaw) + vz * Math.cos(-radYaw));
                float rPx = rYx;
                float rPy = (float) (vy * Math.cos(radPitch) - rYz * Math.sin(radPitch));
                float depth = (float) (vy * Math.sin(radPitch) + rYz * Math.cos(radPitch));

                if (depth > 0.1f) {
                    float invDepth = 1.0f / depth;
                    float sx = cx + (rPx * fov * invDepth);
                    float sy = cy - (rPy * fov * invDepth);
                    screenPoints[vIdx].set(sx, sy, depth, true);
                } else {
                    screenPoints[vIdx].set(0f, 0f, depth, false);
                }
            }

            // Depth sorting triangles
            visibleTriangles.clear();
            int triCount = mesh.triangles.size();
            for (int tIdx = 0; tIdx < triCount; tIdx++) {
                T3DMeshFactory.Triangle tri = mesh.triangles.get(tIdx);
                T3DMathPool.ScreenPoint p0 = screenPoints[tri.v0];
                T3DMathPool.ScreenPoint p1 = screenPoints[tri.v1];
                T3DMathPool.ScreenPoint p2 = screenPoints[tri.v2];

                if (p0.isVisible && p1.isVisible && p2.isVisible) {
                    // Backface culling: 2D cross product
                    float cross = (p1.x - p0.x) * (p2.y - p0.y) - (p1.y - p0.y) * (p2.x - p0.x);
                    if (cross > 0f) {
                        tri.avgDepth = (p0.depth + p1.depth + p2.depth) * 0.3333f;
                        visibleTriangles.add(tri);
                    }
                }
            }

            Collections.sort(visibleTriangles, depthComparator);

            // Rasterize Triangles
            for (int i = 0; i < visibleTriangles.size(); i++) {
                T3DMeshFactory.Triangle tri = visibleTriangles.get(i);
                T3DMathPool.ScreenPoint p0 = screenPoints[tri.v0];
                T3DMathPool.ScreenPoint p1 = screenPoints[tri.v1];
                T3DMathPool.ScreenPoint p2 = screenPoints[tri.v2];

                T3DMathPool.REUSABLE_PATH.reset();
                T3DMathPool.REUSABLE_PATH.moveTo(p0.x, p0.y);
                T3DMathPool.REUSABLE_PATH.lineTo(p1.x, p1.y);
                T3DMathPool.REUSABLE_PATH.lineTo(p2.x, p2.y);
                T3DMathPool.REUSABLE_PATH.close();

                // Simple Lambertian shading from depth
                int baseColor = mesh.vertices.get(tri.v0).color;
                float shade = Math.max(0.2f, Math.min(1.0f, 1.2f - (tri.avgDepth * 0.08f)));
                int r = (int) (Color.red(baseColor) * shade);
                int g = (int) (Color.green(baseColor) * shade);
                int b = (int) (Color.blue(baseColor) * shade);

                T3DMathPool.FILL_PAINT.setColor(Color.argb(Color.alpha(baseColor), r, g, b));
                canvas.drawPath(T3DMathPool.REUSABLE_PATH, T3DMathPool.FILL_PAINT);
                canvas.drawPath(T3DMathPool.REUSABLE_PATH, T3DMathPool.STROKE_PAINT);
            }
        }
    }

    private void drawGridAxis(Canvas canvas, float cx, float cy, float fov, float camX, float camY, float camZ) {
        // Minimal axis indicator
        T3DMathPool.STROKE_PAINT.setColor(Color.argb(100, 100, 116, 139));
        canvas.drawLine(cx - 50, cy, cx + 50, cy, T3DMathPool.STROKE_PAINT);
        canvas.drawLine(cx, cy - 50, cx, cy + 50, T3DMathPool.STROKE_PAINT);
    }
}
