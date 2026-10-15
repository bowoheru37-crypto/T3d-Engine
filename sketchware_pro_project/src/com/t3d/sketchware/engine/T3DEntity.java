package com.t3d.sketchware.engine;

/**
 * T3DEntity - Robust, Null-Safe 3D Scene Entity for Sketchware Pro.
 * Compatible with Sketchware Pro V7.0.0 Stable (No Lambdas, Zero Memory Leaks).
 */
public class T3DEntity {
    public final String name;
    public boolean isEnabled;

    // Transform Coordinates
    public final T3DMathPool.Vector3 position;
    public final T3DMathPool.Vector3 rotation; // Euler angles in degrees
    public final T3DMathPool.Vector3 scale;

    // Mesh
    public T3DMeshFactory.Mesh mesh;

    // Local-to-World Matrix Cache
    public final T3DMathPool.Matrix4x4 modelMatrix;

    public T3DEntity(String name) {
        this.name = (name != null) ? name : "Entity";
        this.isEnabled = true;
        this.position = new T3DMathPool.Vector3(0f, 0f, 0f);
        this.rotation = new T3DMathPool.Vector3(0f, 0f, 0f);
        this.scale = new T3DMathPool.Vector3(1f, 1f, 1f);
        this.modelMatrix = new T3DMathPool.Matrix4x4();
    }

    public void updateMatrix() {
        // Translation & Rotation & Scale to Model Matrix
        modelMatrix.identity();

        float radX = (float) Math.toRadians(rotation.x);
        float radY = (float) Math.toRadians(rotation.y);
        float radZ = (float) Math.toRadians(rotation.z);

        float cosX = (float) Math.cos(radX), sinX = (float) Math.sin(radX);
        float cosY = (float) Math.cos(radY), sinY = (float) Math.sin(radY);
        float cosZ = (float) Math.cos(radZ), sinZ = (float) Math.sin(radZ);

        // Standard yaw/pitch/roll rotation with scaling
        modelMatrix.m[0] = (cosY * cosZ) * scale.x;
        modelMatrix.m[1] = (cosY * sinZ) * scale.x;
        modelMatrix.m[2] = (-sinY) * scale.x;
        modelMatrix.m[3] = position.x;

        modelMatrix.m[4] = (sinX * sinY * cosZ - cosX * sinZ) * scale.y;
        modelMatrix.m[5] = (sinX * sinY * sinZ + cosX * cosZ) * scale.y;
        modelMatrix.m[6] = (sinX * cosY) * scale.y;
        modelMatrix.m[7] = position.y;

        modelMatrix.m[8] = (cosX * sinY * cosZ + sinX * sinZ) * scale.z;
        modelMatrix.m[9] = (cosX * sinY * sinZ - sinX * cosZ) * scale.z;
        modelMatrix.m[10] = (cosX * cosY) * scale.z;
        modelMatrix.m[11] = position.z;

        modelMatrix.m[15] = 1.0f;
    }
}
