package com.t3d.sketchware.engine;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;

/**
 * T3DMathPool - Pre-allocated Object Pool for Zero-GC 3D Rendering.
 * Strictly avoids runtime 'new' allocations inside onDraw and render loops.
 * Compatible with Sketchware Pro V7.0.0 Stable (Java 7/8, No Lambdas).
 */
public final class T3DMathPool {

    // =========================================================================
    // 1. POOLED VECTOR3
    // =========================================================================
    public static final class Vector3 {
        public float x;
        public float y;
        public float z;

        public Vector3() {
            this.x = 0.0f;
            this.y = 0.0f;
            this.z = 0.0f;
        }

        public Vector3(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public void set(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public void set(Vector3 other) {
            if (other != null) {
                this.x = other.x;
                this.y = other.y;
                this.z = other.z;
            }
        }

        public void add(Vector3 other) {
            if (other != null) {
                this.x += other.x;
                this.y += other.y;
                this.z += other.z;
            }
        }

        public void subtract(Vector3 other) {
            if (other != null) {
                this.x -= other.x;
                this.y -= other.y;
                this.z -= other.z;
            }
        }

        public void scale(float s) {
            this.x *= s;
            this.y *= s;
            this.z *= s;
        }

        public float dot(Vector3 other) {
            if (other == null) return 0.0f;
            return (this.x * other.x) + (this.y * other.y) + (this.z * other.z);
        }

        public float length() {
            return (float) Math.sqrt((x * x) + (y * y) + (z * z));
        }

        public void normalize() {
            float len = length();
            if (len > 0.00001f) {
                float inv = 1.0f / len;
                this.x *= inv;
                this.y *= inv;
                this.z *= inv;
            } else {
                this.x = 0.0f;
                this.y = 1.0f;
                this.z = 0.0f;
            }
        }
    }

    // =========================================================================
    // 2. POOLED MATRIX 4x4
    // =========================================================================
    public static final class Matrix4x4 {
        public final float[] m = new float[16];

        public Matrix4x4() {
            identity();
        }

        public void identity() {
            for (int i = 0; i < 16; i++) {
                m[i] = 0.0f;
            }
            m[0] = 1.0f;
            m[5] = 1.0f;
            m[10] = 1.0f;
            m[15] = 1.0f;
        }

        public void set(Matrix4x4 other) {
            if (other != null) {
                System.arraycopy(other.m, 0, this.m, 0, 16);
            }
        }

        public void multiply(Matrix4x4 a, Matrix4x4 b) {
            if (a == null || b == null) return;
            for (int row = 0; row < 4; row++) {
                int rIdx = row * 4;
                for (int col = 0; col < 4; col++) {
                    float sum = 0.0f;
                    for (int k = 0; k < 4; k++) {
                        sum += a.m[rIdx + k] * b.m[k * 4 + col];
                    }
                    TEMP_MULT[rIdx + col] = sum;
                }
            }
            System.arraycopy(TEMP_MULT, 0, this.m, 0, 16);
        }

        private static final float[] TEMP_MULT = new float[16];

        public void transform(Vector3 in, Vector3 out) {
            if (in == null || out == null) return;
            float x = m[0] * in.x + m[1] * in.y + m[2] * in.z + m[3];
            float y = m[4] * in.x + m[5] * in.y + m[6] * in.z + m[7];
            float z = m[8] * in.x + m[9] * in.y + m[10] * in.z + m[11];
            float w = m[12] * in.x + m[13] * in.y + m[14] * in.z + m[15];

            if (w != 0.0f && w != 1.0f) {
                float invW = 1.0f / w;
                out.x = x * invW;
                out.y = y * invW;
                out.z = z * invW;
            } else {
                out.x = x;
                out.y = y;
                out.z = z;
            }
        }
    }

    // =========================================================================
    // 3. POOLED 2D SCREEN POINT
    // =========================================================================
    public static final class ScreenPoint {
        public float x;
        public float y;
        public float depth;
        public boolean isVisible;

        public void set(float x, float y, float depth, boolean isVisible) {
            this.x = x;
            this.y = y;
            this.depth = depth;
            this.isVisible = isVisible;
        }
    }

    // Static Pool Instances (Thread-Safe within Main/Render Thread)
    private static final int POOL_SIZE = 128;
    private static final Vector3[] VEC_POOL = new Vector3[POOL_SIZE];
    private static int vecPoolIndex = 0;

    static {
        for (int i = 0; i < POOL_SIZE; i++) {
            VEC_POOL[i] = new Vector3();
        }
    }

    public static synchronized Vector3 obtainVector() {
        if (vecPoolIndex >= POOL_SIZE) {
            vecPoolIndex = 0; // Wrap around safely
        }
        return VEC_POOL[vecPoolIndex++];
    }

    // Reusable Graphic Drawing Objects (Zero GC)
    public static final Paint FILL_PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);
    public static final Paint STROKE_PAINT = new Paint(Paint.ANTI_ALIAS_FLAG);
    public static final Path REUSABLE_PATH = new Path();

    static {
        FILL_PAINT.setStyle(Paint.Style.FILL);
        STROKE_PAINT.setStyle(Paint.Style.STROKE);
        STROKE_PAINT.setStrokeWidth(2.0f);
        STROKE_PAINT.setColor(Color.argb(160, 0, 240, 255));
    }

    private T3DMathPool() {
        // Prevent Instantiation
    }
}
