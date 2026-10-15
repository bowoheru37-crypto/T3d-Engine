package com.example.engine.core

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.engine.ecs.*
import kotlin.math.*

enum class GizmoMode {
    TRANSLATE,
    ROTATE,
    SCALE,
    SCULPT,
    VIEW_ONLY
}

data class ProjectedVertex(
    val screenX: Float,
    val screenY: Float,
    val depth: Float,
    val worldNormal: Vector3,
    val worldPos: Vector3,
    val color: Color4,
    val isVisible: Boolean
)

data class RenderPolygon(
    val p0: ProjectedVertex,
    val p1: ProjectedVertex,
    val p2: ProjectedVertex,
    val averageDepth: Float,
    val normal: Vector3,
    val material: Material,
    val entityColor: Color4,
    val entityId: String
)

class Renderer3D {
    var drawCallCount: Int = 0
    var polygonCount: Int = 0
    var vertexCount: Int = 0

    fun renderScene(
        drawScope: DrawScope,
        scene: Scene3D,
        selectedEntity: Entity?,
        gizmoMode: GizmoMode,
        sculptRadius: Float = 1.5f,
        sculptCenter: Vector3 = Vector3.ZERO,
        isSculpting: Boolean = false,
        wireframeOverlay: Boolean = false,
        showSkeleton: Boolean = true,
        particleSystem: com.example.engine.particles.ParticleSystem3D? = null,
        movieDirector: com.example.engine.cgi.CgiMovieDirector? = null,
        showPhysicsColliders: Boolean = false,
        characterRigStudio: com.example.engine.character.CharacterDesignRigStudio? = null
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height
        if (width <= 10f || height <= 10f) return

        drawCallCount = 0
        polygonCount = 0
        vertexCount = 0

        val aspectRatio = width / height
        val viewMatrix = scene.camera.getViewMatrix()
        val projMatrix = scene.camera.getProjectionMatrix(aspectRatio)
        val viewProj = projMatrix * viewMatrix

        // 1. Draw Background Sky Gradient
        drawSky(drawScope, scene, width, height)

        // 2. Draw 3D Grid on Ground Plane
        if (scene.showGrid) {
            drawGrid(drawScope, scene, viewProj, width, height)
        }

        // 2.5 Draw Ground Contact Drop Shadows under Entities
        drawContactDropShadows(drawScope, scene, viewProj, width, height)

        // 3. Project and Collect all Polygons
        val polygons = mutableListOf<RenderPolygon>()

        for (entity in scene.entities) {
            if (!entity.enabled || entity.mesh == null) continue
            val mesh = entity.mesh!!
            val modelMatrix = entity.getGlobalMatrix()
            val mvp = viewProj * modelMatrix

            drawCallCount++
            vertexCount += mesh.vertices.size

            val projectedVertices = ArrayList<ProjectedVertex>(mesh.vertices.size)
            for (v in mesh.vertices) {
                val worldPos = modelMatrix.transformPoint(v.position)
                val worldNormal = modelMatrix.transformVector(v.normal).normalized()
                val clipPos = mvp.transformPoint(v.position)

                // Convert NDC (-1..1) to screen coordinates
                val screenX = (clipPos.x * 0.5f + 0.5f) * width
                val screenY = (-clipPos.y * 0.5f + 0.5f) * height
                val depth = clipPos.z
                val isVisible = clipPos.z > 0f && clipPos.z < 100f &&
                        screenX >= -100f && screenX <= width + 100f &&
                        screenY >= -100f && screenY <= height + 100f

                projectedVertices.add(
                    ProjectedVertex(
                        screenX = screenX,
                        screenY = screenY,
                        depth = depth,
                        worldNormal = worldNormal,
                        worldPos = worldPos,
                        color = v.color,
                        isVisible = isVisible
                    )
                )
            }

            for (tri in mesh.triangles) {
                if (tri.v0 >= projectedVertices.size || tri.v1 >= projectedVertices.size || tri.v2 >= projectedVertices.size) continue
                val p0 = projectedVertices[tri.v0]
                val p1 = projectedVertices[tri.v1]
                val p2 = projectedVertices[tri.v2]

                // Frustum culling
                if (!p0.isVisible && !p1.isVisible && !p2.isVisible) continue

                // 2D Screen Back-face culling (cross product of 2D screen edges)
                val edge1X = p1.screenX - p0.screenX
                val edge1Y = p1.screenY - p0.screenY
                val edge2X = p2.screenX - p0.screenX
                val edge2Y = p2.screenY - p0.screenY
                val cross2D = edge1X * edge2Y - edge1Y * edge2X

                if (!entity.material.doubleSided && cross2D <= 0f) continue // Back-facing

                val avgDepth = (p0.depth + p1.depth + p2.depth) / 3f
                val worldTriNormal = (p1.worldPos - p0.worldPos).cross(p2.worldPos - p0.worldPos).normalized()

                polygons.add(
                    RenderPolygon(
                        p0 = p0,
                        p1 = p1,
                        p2 = p2,
                        averageDepth = avgDepth,
                        normal = worldTriNormal,
                        material = entity.material,
                        entityColor = entity.material.albedoColor,
                        entityId = entity.id
                    )
                )
            }
        }

        // 4. Sort Polygons from Back to Front (Painter's Algorithm for Depth)
        polygons.sortByDescending { it.averageDepth }
        polygonCount = polygons.size

        // 5. Draw Polygons with Lighting & Shading
        val lightDir = Vector3(0.5f, 1.0f, -0.7f).normalized()
        val camPos = scene.camera.position

        for (poly in polygons) {
            val p0 = poly.p0
            val p1 = poly.p1
            val p2 = poly.p2

            val isSelected = selectedEntity?.id == poly.entityId

            if (poly.material.wireframe || poly.material.shadingMode == ShadingMode.WIREFRAME) {
                // Draw Wireframe
                val wireColor = if (isSelected) Color(0xFF00F0FF) else poly.material.wireframeColor.toComposeColor()
                drawScope.drawLine(wireColor, Offset(p0.screenX, p0.screenY), Offset(p1.screenX, p1.screenY), strokeWidth = 1.5f)
                drawScope.drawLine(wireColor, Offset(p1.screenX, p1.screenY), Offset(p2.screenX, p2.screenY), strokeWidth = 1.5f)
                drawScope.drawLine(wireColor, Offset(p2.screenX, p2.screenY), Offset(p0.screenX, p0.screenY), strokeWidth = 1.5f)
            } else {
                // Calculate Lighting (Diffuse + Specular + Ambient)
                val n = poly.normal
                val nDotL = max(0f, n.dot(lightDir))
                val ambient = scene.ambientColor

                // View direction for Blinn-Phong specular
                val avgWorldPos = (p0.worldPos + p1.worldPos + p2.worldPos) / 3f
                val viewDir = (camPos - avgWorldPos).normalized()
                val halfDir = (lightDir + viewDir).normalized()
                val nDotH = max(0f, n.dot(halfDir))
                val specular = (poly.material.metallic * (nDotH.pow(16f)) * scene.directionalLight.intensity).coerceIn(0f, 1f)

                var diffR = (ambient.r + poly.entityColor.r * (nDotL * scene.directionalLight.intensity) + specular + poly.material.emissionColor.r * poly.material.emissionIntensity).coerceIn(0f, 1f)
                var diffG = (ambient.g + poly.entityColor.g * (nDotL * scene.directionalLight.intensity) + specular + poly.material.emissionColor.g * poly.material.emissionIntensity).coerceIn(0f, 1f)
                var diffB = (ambient.b + poly.entityColor.b * (nDotL * scene.directionalLight.intensity) + specular + poly.material.emissionColor.b * poly.material.emissionIntensity).coerceIn(0f, 1f)

                // Optional Movie CGI Color Grading LUT
                if (movieDirector != null) {
                    val graded = movieDirector.applyColorGrading(Color4(diffR, diffG, diffB, 1f))
                    diffR = graded.r
                    diffG = graded.g
                    diffB = graded.b
                }

                // Selection highlight tint
                val finalColor = if (isSelected) {
                    Color(
                        red = (diffR * 0.7f + 0.1f).coerceIn(0f, 1f),
                        green = (diffG * 0.8f + 0.3f).coerceIn(0f, 1f),
                        blue = (diffB * 0.8f + 0.4f).coerceIn(0f, 1f),
                        alpha = 1f
                    )
                } else {
                    Color(diffR, diffG, diffB, 1f)
                }

                // Render filled triangle Path
                val path = Path().apply {
                    moveTo(p0.screenX, p0.screenY)
                    lineTo(p1.screenX, p1.screenY)
                    lineTo(p2.screenX, p2.screenY)
                    close()
                }

                drawScope.drawPath(path, color = finalColor, style = Fill)

                // Optional subtle edge stroke or selection outline
                if (wireframeOverlay || isSelected) {
                    val strokeColor = if (isSelected) Color(0xFF00F0FF) else Color(0x33FFFFFF)
                    drawScope.drawPath(path, color = strokeColor, style = Stroke(width = if (isSelected) 1.5f else 0.5f))
                }
            }
        }

        // 5.5 Draw 3D Particle Simulation System
        if (particleSystem != null) {
            drawParticleSystem(drawScope, particleSystem, viewProj, width, height)
        }

        // 5.8 Draw Real-time Physics Collider Debug Wireframes if enabled
        if (showPhysicsColliders) {
            drawPhysicsColliders(drawScope, scene, viewProj, width, height)
        }

        // 6. Draw Bone Skeleton Rigging if enabled
        if (showSkeleton && selectedEntity?.mesh?.skeleton != null) {
            drawSkeleton(drawScope, selectedEntity, viewProj, width, height)
        }

        // 6.5 Draw 3D Selection Bounding Box Corners
        if (selectedEntity != null) {
            drawSelectionBoundingBox(drawScope, selectedEntity, viewProj, width, height)
        }

        // 7. Draw Sculpt Brush Ring if in Sculpt mode
        if (gizmoMode == GizmoMode.SCULPT) {
            drawSculptBrush(drawScope, sculptCenter, sculptRadius, viewProj, width, height, isSculpting)
        }

        // 8. Draw Gizmos for Selected Entity
        if (selectedEntity != null && gizmoMode != GizmoMode.VIEW_ONLY && gizmoMode != GizmoMode.SCULPT) {
            drawGizmo(drawScope, selectedEntity, gizmoMode, viewProj, width, height)
        }

        // 9. Draw Movie CGI CinemaScope Matte Bars & Film Metadata
        if (movieDirector != null) {
            drawCinematicMatteAndHUD(drawScope, movieDirector, width, height)
        }
    }

    private fun drawContactDropShadows(drawScope: DrawScope, scene: Scene3D, viewProj: Matrix4x4, width: Float, height: Float) {
        for (entity in scene.entities) {
            if (!entity.enabled || entity.mesh == null) continue
            val pos = entity.transform.position
            if (pos.y < -0.5f || pos.y > 15f) continue

            val heightFactor = (1f - (pos.y / 12f).coerceIn(0f, 1f))
            if (heightFactor <= 0.05f) continue

            val shadowRadius = (max(entity.transform.scale.x, entity.transform.scale.z) * 0.75f * (1f + pos.y * 0.15f)).coerceIn(0.2f, 4f)
            val groundCenter = Vector3(pos.x, 0.02f, pos.z)
            val pCenter = projectPoint(groundCenter, viewProj, width, height) ?: continue

            val pEdgeX = projectPoint(groundCenter + Vector3(shadowRadius, 0f, 0f), viewProj, width, height) ?: continue
            val pEdgeZ = projectPoint(groundCenter + Vector3(0f, 0f, shadowRadius), viewProj, width, height) ?: continue

            val radiusX = (pEdgeX - pCenter).getDistance()
            val radiusY = (pEdgeZ - pCenter).getDistance() * 0.6f

            if (radiusX > 2f && radiusY > 1f) {
                val alpha = (0.45f * heightFactor).coerceIn(0f, 0.6f)
                drawScope.drawOval(
                    color = Color.Black.copy(alpha = alpha),
                    topLeft = Offset(pCenter.x - radiusX, pCenter.y - radiusY),
                    size = androidx.compose.ui.geometry.Size(radiusX * 2f, radiusY * 2f)
                )
            }
        }
    }

    private fun drawSelectionBoundingBox(drawScope: DrawScope, entity: Entity, viewProj: Matrix4x4, width: Float, height: Float) {
        val mesh = entity.mesh ?: return
        val aabb = mesh.aabb
        val modelMatrix = entity.getGlobalMatrix()

        val corners = arrayOf(
            modelMatrix.transformPoint(Vector3(aabb.min.x, aabb.min.y, aabb.min.z)),
            modelMatrix.transformPoint(Vector3(aabb.max.x, aabb.min.y, aabb.min.z)),
            modelMatrix.transformPoint(Vector3(aabb.max.x, aabb.max.y, aabb.min.z)),
            modelMatrix.transformPoint(Vector3(aabb.min.x, aabb.max.y, aabb.min.z)),
            modelMatrix.transformPoint(Vector3(aabb.min.x, aabb.min.y, aabb.max.z)),
            modelMatrix.transformPoint(Vector3(aabb.max.x, aabb.min.y, aabb.max.z)),
            modelMatrix.transformPoint(Vector3(aabb.max.x, aabb.max.y, aabb.max.z)),
            modelMatrix.transformPoint(Vector3(aabb.min.x, aabb.max.y, aabb.max.z))
        )

        val screenCorners = corners.map { projectPoint(it, viewProj, width, height) }
        val accentColor = Color(0xFF00F0FF)

        // Draw Corner Dots
        for (sc in screenCorners) {
            if (sc != null) {
                drawScope.drawCircle(accentColor, radius = 3.5f, center = sc)
            }
        }
    }

    private fun drawSky(drawScope: DrawScope, scene: Scene3D, width: Float, height: Float) {
        val skyBrush = Brush.verticalGradient(
            colors = listOf(
                scene.skyTopColor.toComposeColor(),
                scene.skyBottomColor.toComposeColor()
            ),
            startY = 0f,
            endY = height
        )
        drawScope.drawRect(skyBrush)
    }

    private fun drawGrid(drawScope: DrawScope, scene: Scene3D, viewProj: Matrix4x4, width: Float, height: Float) {
        val size = scene.gridSize
        val spacing = scene.gridSpacing
        val gridColor = Color(0x2800F0FF)
        val axisXColor = Color(0x88FF3B30)
        val axisZColor = Color(0x88007AFF)

        for (i in -size..size) {
            val x = i * spacing
            val z = i * spacing

            // Line along Z
            val p1 = projectPoint(Vector3(x, 0f, -size * spacing), viewProj, width, height)
            val p2 = projectPoint(Vector3(x, 0f, size * spacing), viewProj, width, height)
            if (p1 != null && p2 != null) {
                val col = if (i == 0) axisZColor else gridColor
                drawScope.drawLine(col, p1, p2, strokeWidth = if (i == 0) 2f else 1f)
            }

            // Line along X
            val p3 = projectPoint(Vector3(-size * spacing, 0f, z), viewProj, width, height)
            val p4 = projectPoint(Vector3(size * spacing, 0f, z), viewProj, width, height)
            if (p3 != null && p4 != null) {
                val col = if (i == 0) axisXColor else gridColor
                drawScope.drawLine(col, p3, p4, strokeWidth = if (i == 0) 2f else 1f)
            }
        }
    }

    private fun drawSkeleton(drawScope: DrawScope, entity: Entity, viewProj: Matrix4x4, width: Float, height: Float) {
        val skeleton = entity.mesh?.skeleton ?: return
        val modelMatrix = entity.getGlobalMatrix()
        val boneColor = Color(0xFFFFD60A)
        val jointColor = Color(0xFFFF9F0A)

        val boneScreenPoints = mutableMapOf<Int, Offset>()

        for (bone in skeleton.bones) {
            val worldPos = modelMatrix.transformPoint(bone.localPosition)
            val screenPos = projectPoint(worldPos, viewProj, width, height)
            if (screenPos != null) {
                boneScreenPoints[bone.id] = screenPos
                // Draw joint node circle
                drawScope.drawCircle(jointColor, radius = 5f, center = screenPos)
                drawScope.drawCircle(Color.White, radius = 2f, center = screenPos)
            }
        }

        for (bone in skeleton.bones) {
            if (bone.parentId >= 0) {
                val pStart = boneScreenPoints[bone.parentId]
                val pEnd = boneScreenPoints[bone.id]
                if (pStart != null && pEnd != null) {
                    drawScope.drawLine(boneColor, pStart, pEnd, strokeWidth = 3f)
                }
            }
        }
    }

    private fun drawSculptBrush(
        drawScope: DrawScope,
        center: Vector3,
        radius: Float,
        viewProj: Matrix4x4,
        width: Float,
        height: Float,
        isSculpting: Boolean
    ) {
        val screenCenter = projectPoint(center, viewProj, width, height) ?: return
        val edgePos = projectPoint(center + Vector3(radius, 0f, 0f), viewProj, width, height) ?: return
        val screenRadius = max(10f, (edgePos - screenCenter).getDistance())

        val brushColor = if (isSculpting) Color(0xFFFF375F) else Color(0xFF00F0FF)
        drawScope.drawCircle(brushColor, radius = screenRadius, center = screenCenter, style = Stroke(width = 2.5f))
        drawScope.drawCircle(brushColor.copy(alpha = 0.2f), radius = screenRadius, center = screenCenter)
        drawScope.drawCircle(brushColor, radius = 4f, center = screenCenter)
    }

    private fun drawGizmo(
        drawScope: DrawScope,
        entity: Entity,
        mode: GizmoMode,
        viewProj: Matrix4x4,
        width: Float,
        height: Float
    ) {
        val origin = entity.transform.position
        val pOrigin = projectPoint(origin, viewProj, width, height) ?: return

        val gizmoLength = 1.8f
        val pX = projectPoint(origin + Vector3(gizmoLength, 0f, 0f), viewProj, width, height)
        val pY = projectPoint(origin + Vector3(0f, gizmoLength, 0f), viewProj, width, height)
        val pZ = projectPoint(origin + Vector3(0f, 0f, gizmoLength), viewProj, width, height)

        val red = Color(0xFFFF3B30)
        val green = Color(0xFF34C759)
        val blue = Color(0xFF007AFF)

        when (mode) {
            GizmoMode.TRANSLATE -> {
                // Center hub
                drawScope.drawCircle(Color.White, radius = 6f, center = pOrigin)

                // X Arrow (Red)
                if (pX != null) {
                    drawScope.drawLine(red, pOrigin, pX, strokeWidth = 3.5f)
                    drawScope.drawCircle(red, radius = 7f, center = pX)
                }
                // Y Arrow (Green)
                if (pY != null) {
                    drawScope.drawLine(green, pOrigin, pY, strokeWidth = 3.5f)
                    drawScope.drawCircle(green, radius = 7f, center = pY)
                }
                // Z Arrow (Blue)
                if (pZ != null) {
                    drawScope.drawLine(blue, pOrigin, pZ, strokeWidth = 3.5f)
                    drawScope.drawCircle(blue, radius = 7f, center = pZ)
                }
            }
            GizmoMode.ROTATE -> {
                // Rotation Circles
                val rScreen = 55f
                drawScope.drawCircle(red, radius = rScreen, center = pOrigin, style = Stroke(width = 3f))
                drawScope.drawCircle(green, radius = rScreen * 0.8f, center = pOrigin, style = Stroke(width = 3f))
                drawScope.drawCircle(blue, radius = rScreen * 0.6f, center = pOrigin, style = Stroke(width = 3f))
            }
            GizmoMode.SCALE -> {
                // Scale Boxes
                drawScope.drawCircle(Color.White, radius = 5f, center = pOrigin)
                if (pX != null) {
                    drawScope.drawLine(red, pOrigin, pX, strokeWidth = 3f)
                    drawScope.drawRect(red, topLeft = Offset(pX.x - 6f, pX.y - 6f), size = androidx.compose.ui.geometry.Size(12f, 12f))
                }
                if (pY != null) {
                    drawScope.drawLine(green, pOrigin, pY, strokeWidth = 3f)
                    drawScope.drawRect(green, topLeft = Offset(pY.x - 6f, pY.y - 6f), size = androidx.compose.ui.geometry.Size(12f, 12f))
                }
                if (pZ != null) {
                    drawScope.drawLine(blue, pOrigin, pZ, strokeWidth = 3f)
                    drawScope.drawRect(blue, topLeft = Offset(pZ.x - 6f, pZ.y - 6f), size = androidx.compose.ui.geometry.Size(12f, 12f))
                }
            }
            else -> {}
        }
    }

    fun drawParticleSystem(
        drawScope: DrawScope,
        particleSystem: com.example.engine.particles.ParticleSystem3D,
        viewProj: Matrix4x4,
        width: Float,
        height: Float
    ) {
        for (p in particleSystem.particles) {
            if (!p.isAlive) continue
            val screenPos = projectPoint(p.position, viewProj, width, height) ?: continue
            val clipZ = (viewProj.transformPoint(p.position).z).coerceIn(0.1f, 50f)
            val pixelRadius = (p.size * 50f / clipZ).coerceIn(1.5f, 28f)
            val composeColor = Color(
                p.currentColor.r.coerceIn(0f, 1f),
                p.currentColor.g.coerceIn(0f, 1f),
                p.currentColor.b.coerceIn(0f, 1f),
                p.currentColor.a.coerceIn(0f, 1f)
            )

            // Outer soft glow
            drawScope.drawCircle(
                color = composeColor.copy(alpha = (composeColor.alpha * 0.35f).coerceIn(0f, 1f)),
                radius = pixelRadius * 1.8f,
                center = screenPos
            )
            // Core spark
            drawScope.drawCircle(
                color = composeColor,
                radius = pixelRadius,
                center = screenPos
            )
        }
    }

    private fun projectPoint(pos: Vector3, viewProj: Matrix4x4, width: Float, height: Float): Offset? {
        val clip = viewProj.transformPoint(pos)
        if (clip.z <= 0f) return null
        val screenX = (clip.x * 0.5f + 0.5f) * width
        val screenY = (-clip.y * 0.5f + 0.5f) * height
        return Offset(screenX, screenY)
    }

    private fun drawPhysicsColliders(
        drawScope: DrawScope,
        scene: Scene3D,
        viewProj: Matrix4x4,
        width: Float,
        height: Float
    ) {
        val boxColor = Color(0xFF39FF14) // Lime Green
        val sphereColor = Color(0xFF00F0FF) // Electric Cyan
        val capsuleColor = Color(0xFFFFD60A) // Gold Amber

        for (entity in scene.entities) {
            val col = entity.collider ?: continue
            if (!entity.enabled) continue

            val centerWorld = entity.transform.position + col.center
            val aabb = col.getAABB(entity.transform.position, entity.transform.scale)

            when (col.type) {
                ColliderType.BOX -> {
                    // Draw 12 edges of the AABB wireframe box
                    val c = arrayOf(
                        projectPoint(Vector3(aabb.min.x, aabb.min.y, aabb.min.z), viewProj, width, height),
                        projectPoint(Vector3(aabb.max.x, aabb.min.y, aabb.min.z), viewProj, width, height),
                        projectPoint(Vector3(aabb.max.x, aabb.max.y, aabb.min.z), viewProj, width, height),
                        projectPoint(Vector3(aabb.min.x, aabb.max.y, aabb.min.z), viewProj, width, height),
                        projectPoint(Vector3(aabb.min.x, aabb.min.y, aabb.max.z), viewProj, width, height),
                        projectPoint(Vector3(aabb.max.x, aabb.min.y, aabb.max.z), viewProj, width, height),
                        projectPoint(Vector3(aabb.max.x, aabb.max.y, aabb.max.z), viewProj, width, height),
                        projectPoint(Vector3(aabb.min.x, aabb.max.y, aabb.max.z), viewProj, width, height)
                    )

                    fun drawEdge(i1: Int, i2: Int) {
                        val p1 = c[i1]
                        val p2 = c[i2]
                        if (p1 != null && p2 != null) {
                            drawScope.drawLine(boxColor.copy(alpha = 0.8f), p1, p2, strokeWidth = 1.5f)
                        }
                    }

                    // Front & Back face
                    drawEdge(0, 1); drawEdge(1, 2); drawEdge(2, 3); drawEdge(3, 0)
                    drawEdge(4, 5); drawEdge(5, 6); drawEdge(6, 7); drawEdge(7, 4)
                    // Connectors
                    drawEdge(0, 4); drawEdge(1, 5); drawEdge(2, 6); drawEdge(3, 7)
                }
                ColliderType.SPHERE -> {
                    val pCenter = projectPoint(centerWorld, viewProj, width, height)
                    val r = col.radius * max(entity.transform.scale.x, max(entity.transform.scale.y, entity.transform.scale.z))
                    val pEdge = projectPoint(centerWorld + Vector3(r, 0f, 0f), viewProj, width, height)
                    if (pCenter != null && pEdge != null) {
                        val screenR = (pEdge - pCenter).getDistance().coerceAtLeast(3f)
                        drawScope.drawCircle(sphereColor.copy(alpha = 0.75f), radius = screenR, center = pCenter, style = Stroke(width = 1.5f))
                        drawScope.drawCircle(sphereColor.copy(alpha = 0.15f), radius = screenR, center = pCenter)
                    }
                }
                ColliderType.CAPSULE, ColliderType.MESH_CONVEX -> {
                    val pCenter = projectPoint(centerWorld, viewProj, width, height)
                    if (pCenter != null) {
                        val r = col.radius * entity.transform.scale.x
                        val h = col.size.y * entity.transform.scale.y
                        val pTop = projectPoint(centerWorld + Vector3(0f, h * 0.5f, 0f), viewProj, width, height)
                        val pBottom = projectPoint(centerWorld - Vector3(0f, h * 0.5f, 0f), viewProj, width, height)
                        if (pTop != null && pBottom != null) {
                            drawScope.drawLine(capsuleColor.copy(alpha = 0.85f), pTop, pBottom, strokeWidth = 2.5f)
                            drawScope.drawCircle(capsuleColor.copy(alpha = 0.85f), radius = 6f, center = pTop, style = Stroke(width = 1.5f))
                            drawScope.drawCircle(capsuleColor.copy(alpha = 0.85f), radius = 6f, center = pBottom, style = Stroke(width = 1.5f))
                        }
                    }
                }
            }
        }
    }

    private fun drawCinematicMatteAndHUD(
        drawScope: DrawScope,
        movieDirector: com.example.engine.cgi.CgiMovieDirector,
        width: Float,
        height: Float
    ) {
        val mattePercent = movieDirector.activeAspect.letterboxPercent
        if (mattePercent > 0.01f) {
            val barHeight = height * mattePercent
            // Top CinemaScope Black Bar
            drawScope.drawRect(
                color = Color.Black,
                topLeft = Offset(0f, 0f),
                size = androidx.compose.ui.geometry.Size(width, barHeight)
            )
            // Bottom CinemaScope Black Bar
            drawScope.drawRect(
                color = Color.Black,
                topLeft = Offset(0f, height - barHeight),
                size = androidx.compose.ui.geometry.Size(width, barHeight)
            )
        }

        // Vignette Overlay at edges
        val vignetteAlpha = movieDirector.activeLut.vignetteIntensity.coerceIn(0.1f, 0.7f)
        val radialVignette = Brush.radialGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = vignetteAlpha * 0.7f)),
            center = Offset(width * 0.5f, height * 0.5f),
            radius = max(width, height) * 0.7f
        )
        drawScope.drawRect(radialVignette)
    }

    private fun Color4.toComposeColor(): Color {
        return Color(r.coerceIn(0f, 1f), g.coerceIn(0f, 1f), b.coerceIn(0f, 1f), a.coerceIn(0f, 1f))
    }
}
