package com.t3d.sketchware.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * T3DMeshFactory - Pure Native Android Mesh Representation & Procedural Primitives.
 * Compatible with Sketchware Pro V7.0.0 Stable (No Lambdas, Zero Runtime Leaks).
 */
public final class T3DMeshFactory {

    public static final class Vertex {
        public final T3DMathPool.Vector3 position;
        public final T3DMathPool.Vector3 normal;
        public int color;

        public Vertex(float x, float y, float z, int color) {
            this.position = new T3DMathPool.Vector3(x, y, z);
            this.normal = new T3DMathPool.Vector3(0f, 1f, 0f);
            this.color = color;
        }
    }

    public static final class Triangle {
        public final int v0;
        public final int v1;
        public final int v2;
        public float avgDepth;

        public Triangle(int v0, int v1, int v2) {
            this.v0 = v0;
            this.v1 = v1;
            this.v2 = v2;
            this.avgDepth = 0.0f;
        }
    }

    public static final class Mesh {
        public final String name;
        public final List<Vertex> vertices;
        public final List<Triangle> triangles;

        public Mesh(String name) {
            this.name = (name != null) ? name : "Mesh";
            this.vertices = new ArrayList<Vertex>();
            this.triangles = new ArrayList<Triangle>();
        }

        public void addVertex(float x, float y, float z, int color) {
            this.vertices.add(new Vertex(x, y, z, color));
        }

        public void addTriangle(int v0, int v1, int v2) {
            this.triangles.add(new Triangle(v0, v1, v2));
        }
    }

    // =========================================================================
    // 1. PROCEDURAL 3D CUBE
    // =========================================================================
    public static Mesh createCube(float size, int color) {
        Mesh mesh = new Mesh("Cube");
        float h = size * 0.5f;

        // 8 Vertices
        mesh.addVertex(-h, -h, -h, color); // 0
        mesh.addVertex( h, -h, -h, color); // 1
        mesh.addVertex( h,  h, -h, color); // 2
        mesh.addVertex(-h,  h, -h, color); // 3
        mesh.addVertex(-h, -h,  h, color); // 4
        mesh.addVertex( h, -h,  h, color); // 5
        mesh.addVertex( h,  h,  h, color); // 6
        mesh.addVertex(-h,  h,  h, color); // 7

        // 12 Triangles (6 Faces)
        // Front
        mesh.addTriangle(0, 1, 2); mesh.addTriangle(0, 2, 3);
        // Back
        mesh.addTriangle(5, 4, 7); mesh.addTriangle(5, 7, 6);
        // Top
        mesh.addTriangle(3, 2, 6); mesh.addTriangle(3, 6, 7);
        // Bottom
        mesh.addTriangle(4, 5, 1); mesh.addTriangle(4, 1, 0);
        // Right
        mesh.addTriangle(1, 5, 6); mesh.addTriangle(1, 6, 2);
        // Left
        mesh.addTriangle(4, 0, 3); mesh.addTriangle(4, 3, 7);

        return mesh;
    }

    // =========================================================================
    // 2. PROCEDURAL 3D SPHERE (UV-Sphere)
    // =========================================================================
    public static Mesh createSphere(float radius, int rings, int sectors, int color) {
        Mesh mesh = new Mesh("Sphere");
        float R = 1.0f / (float)(rings - 1);
        float S = 1.0f / (float)(sectors - 1);

        for (int r = 0; r < rings; ++r) {
            for (int s = 0; s < sectors; ++s) {
                float y = (float) Math.sin(-Math.PI / 2.0 + Math.PI * r * R);
                float x = (float) (Math.cos(2.0 * Math.PI * s * S) * Math.sin(Math.PI * r * R));
                float z = (float) (Math.sin(2.0 * Math.PI * s * S) * Math.sin(Math.PI * r * R));

                mesh.addVertex(x * radius, y * radius, z * radius, color);
            }
        }

        for (int r = 0; r < rings - 1; ++r) {
            for (int s = 0; s < sectors - 1; ++s) {
                int cur = r * sectors + s;
                int next = cur + sectors;

                mesh.addTriangle(cur, next, next + 1);
                mesh.addTriangle(cur, next + 1, cur + 1);
            }
        }

        return mesh;
    }

    // =========================================================================
    // 3. PROCEDURAL 3D PYRAMID
    // =========================================================================
    public static Mesh createPyramid(float width, float height, int color) {
        Mesh mesh = new Mesh("Pyramid");
        float w = width * 0.5f;

        mesh.addVertex(0, height, 0, color);  // 0: Apex
        mesh.addVertex(-w, 0, -w, color);    // 1: Base FL
        mesh.addVertex( w, 0, -w, color);    // 2: Base FR
        mesh.addVertex( w, 0,  w, color);    // 3: Base BR
        mesh.addVertex(-w, 0,  w, color);    // 4: Base BL

        // 4 Side Faces
        mesh.addTriangle(0, 1, 2);
        mesh.addTriangle(0, 2, 3);
        mesh.addTriangle(0, 3, 4);
        mesh.addTriangle(0, 4, 1);
        // Base
        mesh.addTriangle(1, 4, 3);
        mesh.addTriangle(1, 3, 2);

        return mesh;
    }

    // =========================================================================
    // 4. PROCEDURAL 3D TERRAIN GRID
    // =========================================================================
    public static Mesh createTerrainGrid(int gridSize, float spacing, int color) {
        Mesh mesh = new Mesh("TerrainGrid");
        float halfSize = (gridSize * spacing) * 0.5f;

        for (int z = 0; z <= gridSize; z++) {
            for (int x = 0; x <= gridSize; x++) {
                float posX = (x * spacing) - halfSize;
                float posZ = (z * spacing) - halfSize;
                // Simple sine wave elevation for procedural hills
                float posY = (float) (Math.sin(posX * 0.8) * Math.cos(posZ * 0.8)) * 0.6f;

                mesh.addVertex(posX, posY, posZ, color);
            }
        }

        int rowStride = gridSize + 1;
        for (int z = 0; z < gridSize; z++) {
            for (int x = 0; x < gridSize; x++) {
                int topLeft = (z * rowStride) + x;
                int topRight = topLeft + 1;
                int bottomLeft = ((z + 1) * rowStride) + x;
                int bottomRight = bottomLeft + 1;

                mesh.addTriangle(topLeft, bottomLeft, topRight);
                mesh.addTriangle(topRight, bottomLeft, bottomRight);
            }
        }

        return mesh;
    }

    private T3DMeshFactory() {}
}
