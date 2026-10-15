package com.example.engine.spatial

import com.example.engine.core.*
import com.example.engine.ecs.Entity
import kotlin.math.*

data class AABB3D(
    var min: Vector3,
    var max: Vector3
) {
    val center: Vector3 get() = (min + max) * 0.5f
    val size: Vector3 get() = max - min
    val halfSize: Vector3 get() = size * 0.5f

    fun contains(point: Vector3): Boolean {
        return point.x >= min.x && point.x <= max.x &&
               point.y >= min.y && point.y <= max.y &&
               point.z >= min.z && point.z <= max.z
    }

    fun intersects(other: AABB3D): Boolean {
        return (min.x <= other.max.x && max.x >= other.min.x) &&
               (min.y <= other.max.y && max.y >= other.min.y) &&
               (min.z <= other.max.z && max.z >= other.min.z)
    }

    fun intersectsRay(rayOrigin: Vector3, rayDir: Vector3): Float? {
        var tmin = (min.x - rayOrigin.x) / (if (rayDir.x != 0f) rayDir.x else 0.00001f)
        var tmax = (max.x - rayOrigin.x) / (if (rayDir.x != 0f) rayDir.x else 0.00001f)
        if (tmin > tmax) { val tmp = tmin; tmin = tmax; tmax = tmp }

        var tymin = (min.y - rayOrigin.y) / (if (rayDir.y != 0f) rayDir.y else 0.00001f)
        var tymax = (max.y - rayOrigin.y) / (if (rayDir.y != 0f) rayDir.y else 0.00001f)
        if (tymin > tymax) { val tmp = tymin; tymin = tymax; tymax = tmp }

        if ((tmin > tymax) || (tymin > tmax)) return null
        if (tymin > tmin) tmin = tymin
        if (tymax < tmax) tmax = tymax

        var tzmin = (min.z - rayOrigin.z) / (if (rayDir.z != 0f) rayDir.z else 0.00001f)
        var tzmax = (max.z - rayOrigin.z) / (if (rayDir.z != 0f) rayDir.z else 0.00001f)
        if (tzmin > tzmax) { val tmp = tzmin; tzmin = tzmax; tzmax = tmp }

        if ((tmin > tzmax) || (tzmin > tmax)) return null
        if (tzmin > tmin) tmin = tzmin
        if (tzmax < tmax) tmax = tzmax

        return if (tmin >= 0) tmin else if (tmax >= 0) tmax else null
    }
}

class SpatialOctreeNode(
    val bounds: AABB3D,
    val depth: Int = 0,
    val maxDepth: Int = 4,
    val maxItems: Int = 4
) {
    val items: MutableList<Pair<Entity, AABB3D>> = mutableListOf()
    var children: Array<SpatialOctreeNode>? = null
    val isLeaf: Boolean get() = children == null

    fun insert(entity: Entity, entityBounds: AABB3D): Boolean {
        if (!bounds.intersects(entityBounds)) return false

        if (isLeaf && (items.size < maxItems || depth >= maxDepth)) {
            items.add(Pair(entity, entityBounds))
            return true
        }

        if (isLeaf && depth < maxDepth) {
            subdivide()
        }

        var inserted = false
        children?.let { childNodes ->
            for (child in childNodes) {
                if (child.insert(entity, entityBounds)) {
                    inserted = true
                }
            }
        }
        return inserted
    }

    private fun subdivide() {
        val center = bounds.center
        val min = bounds.min
        val max = bounds.max
        children = arrayOf(
            SpatialOctreeNode(AABB3D(min, center), depth + 1, maxDepth, maxItems),
            SpatialOctreeNode(AABB3D(Vector3(center.x, min.y, min.z), Vector3(max.x, center.y, center.z)), depth + 1, maxDepth, maxItems),
            SpatialOctreeNode(AABB3D(Vector3(min.x, center.y, min.z), Vector3(center.x, max.y, center.z)), depth + 1, maxDepth, maxItems),
            SpatialOctreeNode(AABB3D(Vector3(center.x, center.y, min.z), Vector3(max.x, max.y, center.z)), depth + 1, maxDepth, maxItems),
            SpatialOctreeNode(AABB3D(Vector3(min.x, min.y, center.z), Vector3(center.x, center.y, max.z)), depth + 1, maxDepth, maxItems),
            SpatialOctreeNode(AABB3D(Vector3(center.x, min.y, center.z), Vector3(max.x, center.y, max.z)), depth + 1, maxDepth, maxItems),
            SpatialOctreeNode(AABB3D(Vector3(min.x, center.y, center.z), Vector3(center.x, max.y, max.z)), depth + 1, maxDepth, maxItems),
            SpatialOctreeNode(AABB3D(center, max), depth + 1, maxDepth, maxItems)
        )

        // Re-distribute existing leaf items into children
        for ((ent, b) in items) {
            for (child in children!!) {
                child.insert(ent, b)
            }
        }
        items.clear()
    }

    fun queryRay(rayOrigin: Vector3, rayDir: Vector3, results: MutableList<Pair<Entity, Float>>) {
        if (bounds.intersectsRay(rayOrigin, rayDir) == null) return

        for ((entity, b) in items) {
            val dist = b.intersectsRay(rayOrigin, rayDir)
            if (dist != null) {
                results.add(Pair(entity, dist))
            }
        }

        children?.forEach { it.queryRay(rayOrigin, rayDir, results) }
    }
}

class SpatialOctree3D(
    val worldBounds: AABB3D = AABB3D(Vector3(-50f, -10f, -50f), Vector3(50f, 50f, 50f))
) {
    var root: SpatialOctreeNode = SpatialOctreeNode(worldBounds)

    fun rebuild(entities: List<Entity>) {
        root = SpatialOctreeNode(worldBounds)
        for (entity in entities) {
            if (!entity.enabled) continue
            val pos = entity.transform.position
            val scale = entity.transform.scale
            val meshBounds = entity.mesh?.aabb
            val extent = if (meshBounds != null) {
                val s = max(max(scale.x, scale.y), scale.z)
                (meshBounds.size.length() * 0.5f) * s
            } else 1.0f

            val b = AABB3D(
                Vector3(pos.x - extent, pos.y - extent, pos.z - extent),
                Vector3(pos.x + extent, pos.y + extent, pos.z + extent)
            )
            root.insert(entity, b)
        }
    }

    fun raycast(rayOrigin: Vector3, rayDir: Vector3): Entity? {
        val hits = mutableListOf<Pair<Entity, Float>>()
        root.queryRay(rayOrigin, rayDir, hits)
        return hits.minByOrNull { it.second }?.first
    }
}
