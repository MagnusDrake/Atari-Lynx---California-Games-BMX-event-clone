package com.example.graphics

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.engine.BmxPhysicsEngine
import com.example.engine.Vector3D
import com.example.model.*
import kotlin.math.*

/**
 * High-Performance Isometric 3D Graphics Renderer for Lynx BMX.
 *
 * Implements:
 * - 2:1 True Isometric 3D Projection Matrix
 * - 3D Multi-Slice Terrain Mesh with Directional Sunlight Shading
 * - 3D Dynamic Soft Drop Shadows on Terrain
 * - 3D Polygonal Ramps, Boulders, Logs, Mud Puddles, and Cones
 * - 3D Articulated Low-Poly BMX & Rider with Full 3D Stunt Rotations (Pitch/Roll/Yaw)
 * - Volumetric 3D Particle Roosts & Shockwaves
 * - Parallax California Sunset Backdrop & Cyber-Retro HUD with 3D Course Radar
 */
class LynxGraphicsRenderer {

    // Sun Lighting Vector (Directional Light from top-left)
    private val sunDir = Vector3D(0.45f, -0.55f, 0.70f).normalized()

    // Environment & Backdrop Colors
    val SKY_TOP = Color(0xFF15193B)
    val SKY_MID = Color(0xFFE65100)
    val SKY_BOTTOM = Color(0xFFFBC02D)
    val SUN_COLOR = Color(0xFFFFEE58)
    val MOUNTAIN_FAR = Color(0xFF311B92)
    val MOUNTAIN_NEAR = Color(0xFF512DA8)
    val OCEAN_BLUE = Color(0xFF0097A7)
    val PALM_TRUNK = Color(0xFF4E342E)
    val PALM_LEAVES = Color(0xFF1B5E20)

    // Terrain Colors
    val DIRT_MAIN = Color(0xFF8D6E63)
    val DIRT_LIGHT = Color(0xFFA1887F)
    val DIRT_DARK = Color(0xFF5D4037)
    val DIRT_CLIFF = Color(0xFF3E2723)
    val GRASS_BORDER = Color(0xFF4CAF50)
    val GRASS_HIGHLIGHT = Color(0xFF81C784)
    val TRACK_GROOVE = Color(0xFF4E342E)

    // HUD Colors
    val HUD_BG = Color(0xF21C1B1F)
    val HUD_BORDER = Color(0xFF4A4458)
    val HUD_TEXT = Color(0xFFD0BCFF)

    fun render(
        drawScope: DrawScope,
        physics: BmxPhysicsEngine,
        theme: ColorTheme,
        showScanlines: Boolean,
        textMeasurer: TextMeasurer
    ) {
        val width = drawScope.size.width
        val height = drawScope.size.height

        // Isometric Camera Tracking
        val cameraX = physics.posX
        val cameraY = physics.posY
        val cameraZ = physics.posZ

        // Isometric Scale: meters to pixels
        val isoScale = width / 78f
        val screenCenterX = width * 0.42f
        val screenCenterY = height * 0.54f

        // 3D to Isometric Screen Projection Function
        // Iso formula: X increases right-down (30°), Y increases left-down (30°), Z increases straight UP
        val cos30 = 0.8660254f
        val sin30 = 0.5000000f

        fun worldToScreen(wx: Float, wy: Float, wz: Float): Offset {
            val dx = wx - cameraX
            val dy = wy - cameraY
            val dz = wz - cameraZ

            val screenX = screenCenterX + ((dx - dy) * cos30) * isoScale
            val screenY = screenCenterY + ((dx + dy) * sin30) * isoScale - (dz * 1.35f) * isoScale
            return Offset(screenX, screenY)
        }

        // 1. Parallax Backdrop (Sky, Sun, Mountains, Ocean, Palms)
        drawParallaxBackdrop(drawScope, width, height, cameraX)

        // 2. 3D Isometric Terrain Grid (Ground, Track Lanes, Side Cliffs)
        drawIsometricTerrain(drawScope, width, height, physics, ::worldToScreen, theme, isoScale)

        // 3. 3D Obstacles (Ramps, Rocks, Logs, Cones, Mud)
        drawIsometricObstacles(drawScope, physics, ::worldToScreen, isoScale)

        // 4. 3D Dynamic Soft Drop Shadow (Cast on ground beneath bike)
        drawDynamicDropShadow(drawScope, physics, ::worldToScreen, isoScale)

        // 5. 3D Volumetric Particles (Roost dust, mud sprays, crash debris)
        drawIsometricParticles(drawScope, physics, ::worldToScreen, isoScale)

        // 6. 3D Articulated BMX Bike & Rider Stunt Model
        draw3DBikeAndRider(drawScope, physics, theme, ::worldToScreen, isoScale)

        // 7. Retro CRT Scanline Overlay
        if (showScanlines) {
            drawScanlines(drawScope, width, height)
        }

        // 8. Cyber-Retro HUD with 3D Course Mini-Radar
        drawHud(drawScope, width, height, physics, textMeasurer)
    }

    private fun drawParallaxBackdrop(drawScope: DrawScope, width: Float, height: Float, cameraX: Float) {
        // Sunset Sky Gradient
        val skyBrush = Brush.verticalGradient(
            0.0f to SKY_TOP,
            0.35f to SKY_MID,
            0.70f to SKY_BOTTOM,
            startY = 0f,
            endY = height * 0.65f
        )
        drawScope.drawRect(skyBrush, size = Size(width, height * 0.7f))

        // Retro California Sun with 80s horizontal slices
        val sunX = ((width * 0.72f) - (cameraX * 0.15f)) % (width + 300f) - 150f
        val sunY = height * 0.18f
        val sunRadius = height * 0.11f

        drawScope.drawCircle(
            color = SUN_COLOR,
            radius = sunRadius,
            center = Offset(sunX, sunY)
        )

        for (i in 0..5) {
            val lineY = sunY + (i * sunRadius * 0.17f)
            drawScope.drawLine(
                color = SKY_MID,
                start = Offset(sunX - sunRadius * 1.15f, lineY),
                end = Offset(sunX + sunRadius * 1.15f, lineY),
                strokeWidth = 3f + i * 1.8f
            )
        }

        // Distant 3D Mountains
        val horizonY = height * 0.44f
        val mountainPath = Path().apply {
            moveTo(0f, horizonY)
            var x = 0f
            val step = 45f
            while (x <= width + 50f) {
                val wx = x + cameraX * 0.08f
                val my = horizonY - 40f - sin(wx * 0.012f) * 28f - cos(wx * 0.026f) * 14f
                lineTo(x, my)
                x += step
            }
            lineTo(width, horizonY + 60f)
            lineTo(0f, horizonY + 60f)
            close()
        }
        drawScope.drawPath(mountainPath, MOUNTAIN_FAR)

        // Nearer Foothills
        val nearMountainPath = Path().apply {
            moveTo(0f, horizonY)
            var x = 0f
            val step = 35f
            while (x <= width + 50f) {
                val wx = x + cameraX * 0.18f
                val my = horizonY - 20f - sin(wx * 0.022f) * 18f - cos(wx * 0.045f) * 9f
                lineTo(x, my)
                x += step
            }
            lineTo(width, horizonY + 60f)
            lineTo(0f, horizonY + 60f)
            close()
        }
        drawScope.drawPath(nearMountainPath, MOUNTAIN_NEAR)

        // Ocean & Beach line
        drawScope.drawRect(
            color = OCEAN_BLUE,
            topLeft = Offset(0f, horizonY - 2f),
            size = Size(width, 32f)
        )

        // Animated ocean surf lines
        val waveStep = 70f
        var wx = -(cameraX * 0.35f) % waveStep
        while (wx < width) {
            drawScope.drawLine(
                color = Color.White.copy(alpha = 0.55f),
                start = Offset(wx, horizonY + 10f),
                end = Offset(wx + 28f, horizonY + 10f),
                strokeWidth = 2.5f
            )
            drawScope.drawLine(
                color = Color.White.copy(alpha = 0.35f),
                start = Offset(wx + 34f, horizonY + 20f),
                end = Offset(wx + 52f, horizonY + 20f),
                strokeWidth = 2f
            )
            wx += waveStep
        }

        // Coastal Palm Trees in Parallax
        val treeSpacing = 240f
        var tx = -((cameraX * 0.45f) % treeSpacing) - 60f
        while (tx < width + 120f) {
            val baseY = horizonY + 28f
            val trunkTopX = tx + 14f
            val trunkTopY = baseY - 65f

            val trunkPath = Path().apply {
                moveTo(tx, baseY)
                quadraticTo(tx + 4f, baseY - 32f, trunkTopX, trunkTopY)
                lineTo(trunkTopX + 6f, trunkTopY)
                quadraticTo(tx + 10f, baseY - 32f, tx + 8f, baseY)
                close()
            }
            drawScope.drawPath(trunkPath, PALM_TRUNK)

            // Fronds
            val frondAngles = listOf(-135f, -95f, -55f, -15f, 25f, 65f)
            for (angle in frondAngles) {
                drawScope.rotate(angle, pivot = Offset(trunkTopX + 3f, trunkTopY)) {
                    drawOval(
                        color = PALM_LEAVES,
                        topLeft = Offset(trunkTopX - 22f, trunkTopY - 32f),
                        size = Size(44f, 13f)
                    )
                }
            }
            tx += treeSpacing
        }
    }

    private fun drawIsometricTerrain(
        drawScope: DrawScope,
        width: Float,
        height: Float,
        physics: BmxPhysicsEngine,
        w2s: (Float, Float, Float) -> Offset,
        theme: ColorTheme,
        scale: Float
    ) {
        val visibleRangeBack = 28f
        val visibleRangeFront = 62f
        val startX = (physics.posX - visibleRangeBack).coerceAtLeast(0f)
        val endX = (physics.posX + visibleRangeFront).coerceAtMost(physics.trackLength + 40f)

        val halfW = physics.trackHalfWidth
        val xStep = 3.5f // Segment length along track
        val yLanes = listOf(-halfW, -halfW * 0.5f, 0f, halfW * 0.5f, halfW)

        // Palette overrides from active theme
        val baseDirt = Color(theme.groundTop)
        val cliffSide = Color(theme.groundSide)
        val grassAccent = Color(theme.grassAccent)

        // 1. Draw Under-Cliffs (Left and Right canyon embankments)
        var x = startX
        while (x < endX) {
            val nextX = min(x + xStep, endX)

            // Left track edge cliff (drop down)
            val z1L = physics.getGroundHeight(x, -halfW)
            val z2L = physics.getGroundHeight(nextX, -halfW)
            val p1L = w2s(x, -halfW, z1L)
            val p2L = w2s(nextX, -halfW, z2L)
            val p1L_bot = w2s(x, -halfW, z1L - 35f)
            val p2L_bot = w2s(nextX, -halfW, z2L - 35f)

            val leftCliffPath = Path().apply {
                moveTo(p1L.x, p1L.y)
                lineTo(p2L.x, p2L.y)
                lineTo(p2L_bot.x, height + 40f)
                lineTo(p1L_bot.x, height + 40f)
                close()
            }
            drawScope.drawPath(leftCliffPath, cliffSide)

            // Right track edge cliff (drop down)
            val z1R = physics.getGroundHeight(x, halfW)
            val z2R = physics.getGroundHeight(nextX, halfW)
            val p1R = w2s(x, halfW, z1R)
            val p2R = w2s(nextX, halfW, z2R)
            val p1R_bot = w2s(x, halfW, z1R - 35f)
            val p2R_bot = w2s(nextX, halfW, z2R - 35f)

            val rightCliffPath = Path().apply {
                moveTo(p1R.x, p1R.y)
                lineTo(p2R.x, p2R.y)
                lineTo(p2R_bot.x, height + 40f)
                lineTo(p1R_bot.x, height + 40f)
                close()
            }
            drawScope.drawPath(rightCliffPath, cliffSide.copy(alpha = 0.88f))

            x += xStep
        }

        // 2. Draw 3D Isometric Terrain Ribbons & Lane Quads with Directional Shading
        x = startX
        while (x < endX) {
            val nextX = min(x + xStep, endX)

            for (laneIdx in 0 until yLanes.size - 1) {
                val y1 = yLanes[laneIdx]
                val y2 = yLanes[laneIdx + 1]

                val zA = physics.getGroundHeight(x, y1)
                val zB = physics.getGroundHeight(nextX, y1)
                val zC = physics.getGroundHeight(nextX, y2)
                val zD = physics.getGroundHeight(x, y2)

                val pA = w2s(x, y1, zA)
                val pB = w2s(nextX, y1, zB)
                val pC = w2s(nextX, y2, zC)
                val pD = w2s(x, y2, zD)

                // Compute Surface Normal for Sunlight Shading
                val normal = physics.getGroundNormal((x + nextX) * 0.5f, (y1 + y2) * 0.5f)
                val lightIntensity = (normal.dot(sunDir) * 0.45f + 0.55f).coerceIn(0.25f, 1.0f)

                // Shaded quad color
                val quadColor = Color(
                    red = (baseDirt.red * lightIntensity).coerceIn(0f, 1f),
                    green = (baseDirt.green * lightIntensity).coerceIn(0f, 1f),
                    blue = (baseDirt.blue * lightIntensity).coerceIn(0f, 1f),
                    alpha = 1.0f
                )

                val quadPath = Path().apply {
                    moveTo(pA.x, pA.y)
                    lineTo(pB.x, pB.y)
                    lineTo(pC.x, pC.y)
                    lineTo(pD.x, pD.y)
                    close()
                }
                drawScope.drawPath(quadPath, quadColor)

                // Subtle lane divider lines / tire track grooves
                if (laneIdx == 1 || laneIdx == 2) {
                    drawScope.drawLine(
                        color = TRACK_GROOVE.copy(alpha = 0.25f),
                        start = pA,
                        end = pB,
                        strokeWidth = 1.5f
                    )
                }
            }

            // Top Grass / Foliage Edge Bevels along Left and Right Boundaries
            val z1L = physics.getGroundHeight(x, -halfW)
            val z2L = physics.getGroundHeight(nextX, -halfW)
            val z1R = physics.getGroundHeight(x, halfW)
            val z2R = physics.getGroundHeight(nextX, halfW)

            val p1L = w2s(x, -halfW, z1L)
            val p2L = w2s(nextX, -halfW, z2L)
            val p1R = w2s(x, halfW, z1R)
            val p2R = w2s(nextX, halfW, z2R)

            drawScope.drawLine(grassAccent, start = p1L, end = p2L, strokeWidth = 3.5f)
            drawScope.drawLine(grassAccent, start = p1R, end = p2R, strokeWidth = 3.5f)

            x += xStep
        }
    }

    private fun drawIsometricObstacles(
        drawScope: DrawScope,
        physics: BmxPhysicsEngine,
        w2s: (Float, Float, Float) -> Offset,
        scale: Float
    ) {
        val camX = physics.posX
        for (obs in physics.obstacles) {
            // Render only obstacles near the camera view
            if (obs.x < camX - 35f || obs.x > camX + 75f) continue

            val gx = obs.x
            val gy = obs.laneY
            val gz = physics.getGroundHeight(gx, gy)
            val halfW = obs.width / 2f
            val halfD = obs.depth / 2f

            when (obs.type) {
                ObstacleType.MUD_PUDDLE -> {
                    // 3D Isometric Mud Puddle (Fluid oval with specular reflection)
                    val p1 = w2s(gx - halfW, gy - halfD, gz)
                    val p2 = w2s(gx + halfW, gy - halfD, gz)
                    val p3 = w2s(gx + halfW, gy + halfD, gz)
                    val p4 = w2s(gx - halfW, gy + halfD, gz)

                    val mudPath = Path().apply {
                        moveTo(p1.x, p1.y)
                        lineTo(p2.x, p2.y)
                        lineTo(p3.x, p3.y)
                        lineTo(p4.x, p4.y)
                        close()
                    }
                    drawScope.drawPath(mudPath, Color(0xFF3E2723))

                    // Mud surface glint
                    val centerP = w2s(gx, gy, gz)
                    drawScope.drawCircle(
                        color = Color(0xFF5D4037).copy(alpha = 0.7f),
                        radius = 8f * scale * 0.1f,
                        center = centerP
                    )
                }

                ObstacleType.ROCK -> {
                    // 3D Low-Poly Multifaceted Boulder
                    val baseP = w2s(gx, gy, gz)
                    val topP = w2s(gx, gy, gz + obs.height)
                    val leftP = w2s(gx - halfW, gy + halfD * 0.3f, gz + obs.height * 0.3f)
                    val rightP = w2s(gx + halfW, gy - halfD * 0.3f, gz + obs.height * 0.4f)
                    val frontP = w2s(gx + halfW * 0.3f, gy + halfD, gz + obs.height * 0.2f)

                    // Front-Lit Facet
                    val f1 = Path().apply {
                        moveTo(topP.x, topP.y)
                        lineTo(rightP.x, rightP.y)
                        lineTo(frontP.x, frontP.y)
                        close()
                    }
                    drawScope.drawPath(f1, Color(0xFF9E9E9E))

                    // Left-Lit Facet
                    val f2 = Path().apply {
                        moveTo(topP.x, topP.y)
                        lineTo(leftP.x, leftP.y)
                        lineTo(frontP.x, frontP.y)
                        close()
                    }
                    drawScope.drawPath(f2, Color(0xFF757575))

                    // Shadow Facet
                    val f3 = Path().apply {
                        moveTo(topP.x, topP.y)
                        lineTo(leftP.x, leftP.y)
                        lineTo(baseP.x, baseP.y)
                        close()
                    }
                    drawScope.drawPath(f3, Color(0xFF424242))

                    // Outline edges
                    drawScope.drawLine(Color(0xFFBDBDBD), topP, rightP, strokeWidth = 2f)
                    drawScope.drawLine(Color(0xFFBDBDBD), topP, frontP, strokeWidth = 2f)
                }

                ObstacleType.LOG -> {
                    // 3D Cylindrical Fallen Redwood Log
                    val pL1 = w2s(gx - halfW, gy - halfD, gz)
                    val pL2 = w2s(gx + halfW, gy - halfD, gz)
                    val pR1 = w2s(gx - halfW, gy + halfD, gz)
                    val pR2 = w2s(gx + halfW, gy + halfD, gz)

                    val pTopL1 = w2s(gx - halfW, gy - halfD, gz + obs.height)
                    val pTopL2 = w2s(gx + halfW, gy - halfD, gz + obs.height)
                    val pTopR1 = w2s(gx - halfW, gy + halfD, gz + obs.height)
                    val pTopR2 = w2s(gx + halfW, gy + halfD, gz + obs.height)

                    // Bark surface
                    val logBody = Path().apply {
                        moveTo(pTopL1.x, pTopL1.y)
                        lineTo(pTopL2.x, pTopL2.y)
                        lineTo(pTopR2.x, pTopR2.y)
                        lineTo(pTopR1.x, pTopR1.y)
                        close()
                    }
                    drawScope.drawPath(logBody, Color(0xFF4E342E))

                    // End grain face
                    val endCap = Path().apply {
                        moveTo(pTopR2.x, pTopR2.y)
                        lineTo(pTopR1.x, pTopR1.y)
                        lineTo(pR1.x, pR1.y)
                        lineTo(pR2.x, pR2.y)
                        close()
                    }
                    drawScope.drawPath(endCap, Color(0xFF8D6E63))
                    drawScope.drawPath(endCap, Color(0xFF3E2723), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
                }

                ObstacleType.CONE -> {
                    // 3D Safety Traffic Cone
                    val topP = w2s(gx, gy, gz + obs.height)
                    val b1 = w2s(gx - halfW, gy - halfD, gz)
                    val b2 = w2s(gx + halfW, gy - halfD, gz)
                    val b3 = w2s(gx + halfW, gy + halfD, gz)
                    val b4 = w2s(gx - halfW, gy + halfD, gz)

                    // Base square
                    val basePath = Path().apply {
                        moveTo(b1.x, b1.y); lineTo(b2.x, b2.y); lineTo(b3.x, b3.y); lineTo(b4.x, b4.y); close()
                    }
                    drawScope.drawPath(basePath, Color(0xFFE65100))

                    // Cone cone body
                    val conePath = Path().apply {
                        moveTo(topP.x, topP.y)
                        lineTo(b1.x, b1.y)
                        lineTo(b3.x, b3.y)
                        close()
                    }
                    drawScope.drawPath(conePath, Color(0xFFFF6D00))

                    // White reflective retro stripe
                    val stripeTop = w2s(gx, gy, gz + obs.height * 0.6f)
                    val stripeB1 = w2s(gx - halfW * 0.4f, gy - halfD * 0.4f, gz + obs.height * 0.4f)
                    val stripeB2 = w2s(gx + halfW * 0.4f, gy + halfD * 0.4f, gz + obs.height * 0.4f)
                    drawScope.drawLine(Color.White, stripeB1, stripeB2, strokeWidth = 3f)
                }

                ObstacleType.BIG_RAMP -> {
                    // 3D Mega Dirt / Wooden Launch Ramp with yellow/black danger stripes
                    val r1 = w2s(gx - halfW, gy - halfD, gz)
                    val r2 = w2s(gx + halfW, gy - halfD, gz + obs.height)
                    val r3 = w2s(gx + halfW, gy + halfD, gz + obs.height)
                    val r4 = w2s(gx - halfW, gy + halfD, gz)

                    val rampFace = Path().apply {
                        moveTo(r1.x, r1.y)
                        lineTo(r2.x, r2.y)
                        lineTo(r3.x, r3.y)
                        lineTo(r4.x, r4.y)
                        close()
                    }
                    drawScope.drawPath(rampFace, Color(0xFFFFB300))

                    // Side wall
                    val r2_bot = w2s(gx + halfW, gy - halfD, gz)
                    val sideWall = Path().apply {
                        moveTo(r1.x, r1.y)
                        lineTo(r2.x, r2.y)
                        lineTo(r2_bot.x, r2_bot.y)
                        close()
                    }
                    drawScope.drawPath(sideWall, Color(0xFFE65100))

                    // Caution stripes along the ramp lip
                    drawScope.drawLine(Color.Black, r2, r3, strokeWidth = 4f)
                    drawScope.drawLine(Color(0xFFFFD600), r2, r3, strokeWidth = 2f)
                }

                ObstacleType.SPEED_BUMP -> {}
            }
        }

        // 3D Finish Line Truss Structure at finishX
        val finX = physics.finishX
        val finZ = physics.getGroundHeight(finX, 0f)
        val pLeft = w2s(finX, -physics.trackHalfWidth, finZ)
        val pRight = w2s(finX, physics.trackHalfWidth, finZ)
        val pLeftTop = w2s(finX, -physics.trackHalfWidth, finZ + 15f)
        val pRightTop = w2s(finX, physics.trackHalfWidth, finZ + 15f)

        // Truss Pillars
        drawScope.drawLine(Color.White, pLeft, pLeftTop, strokeWidth = 5f)
        drawScope.drawLine(Color.White, pRight, pRightTop, strokeWidth = 5f)
        // Overhead Arch Truss
        drawScope.drawLine(Color.White, pLeftTop, pRightTop, strokeWidth = 6f)

        // Checkered Banner
        val bannerMidL = w2s(finX, -physics.trackHalfWidth, finZ + 11f)
        val bannerMidR = w2s(finX, physics.trackHalfWidth, finZ + 11f)
        val bannerQuad = Path().apply {
            moveTo(pLeftTop.x, pLeftTop.y)
            lineTo(pRightTop.x, pRightTop.y)
            lineTo(bannerMidR.x, bannerMidR.y)
            lineTo(bannerMidL.x, bannerMidL.y)
            close()
        }
        drawScope.drawPath(bannerQuad, Color(0xFFFF1744))
    }

    private fun drawDynamicDropShadow(
        drawScope: DrawScope,
        physics: BmxPhysicsEngine,
        w2s: (Float, Float, Float) -> Offset,
        scale: Float
    ) {
        val groundZ = physics.getGroundHeight(physics.posX, physics.posY)
        val airAltitude = (physics.posZ - groundZ).coerceAtLeast(0f)

        // Shadow Position on ground plane
        val shadowP = w2s(physics.posX, physics.posY, groundZ)

        // Shadow scales larger and softer as bike gains altitude
        val shadowRadiusX = (18f + airAltitude * 1.8f) * (scale * 0.08f)
        val shadowRadiusY = (10f + airAltitude * 0.9f) * (scale * 0.08f)
        val shadowAlpha = (0.55f - (airAltitude / 25f) * 0.35f).coerceIn(0.12f, 0.55f)

        drawScope.drawOval(
            color = Color.Black.copy(alpha = shadowAlpha),
            topLeft = Offset(shadowP.x - shadowRadiusX, shadowP.y - shadowRadiusY),
            size = Size(shadowRadiusX * 2f, shadowRadiusY * 2f)
        )
    }

    private fun drawIsometricParticles(
        drawScope: DrawScope,
        physics: BmxPhysicsEngine,
        w2s: (Float, Float, Float) -> Offset,
        scale: Float
    ) {
        for (p in physics.particles) {
            val sp = w2s(p.x, p.y, p.z)
            val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
            drawScope.drawCircle(
                color = Color(p.color).copy(alpha = alpha),
                radius = p.size * (scale * 0.07f).coerceAtLeast(1.5f),
                center = sp
            )
        }
    }

    private fun draw3DBikeAndRider(
        drawScope: DrawScope,
        physics: BmxPhysicsEngine,
        theme: ColorTheme,
        w2s: (Float, Float, Float) -> Offset,
        scale: Float
    ) {
        if (physics.riderPose == RiderPose.CRASHED) {
            // 3D Tumbling Ragdoll Bike & Rider
            drawCrashedRagdoll3D(drawScope, physics, theme, w2s, scale)
            return
        }

        val frameColor = Color(theme.bikeFrame)
        val jerseyColor = Color(theme.riderJersey)
        val pantsColor = Color(theme.riderPants)
        val helmetColor = Color(theme.helmet)

        // Local 3D Bike Model Coordinates centered at (0, 0, 0)
        val wheelRadius = 1.35f
        val rearWheelCenter = Vector3D(-1.65f, 0f, wheelRadius)
        val frontWheelCenter = Vector3D(1.65f, 0f, wheelRadius)
        val bottomBracket = Vector3D(-0.15f, 0f, 1.25f)
        val seatJunction = Vector3D(-0.85f, 0f, 2.25f)
        val headTube = Vector3D(1.15f, 0f, 2.5f)
        val handlebars = Vector3D(1.05f, 0f, 3.2f)
        val handlebarsL = Vector3D(1.05f, -0.85f, 3.2f)
        val handlebarsR = Vector3D(1.05f, 0.85f, 3.2f)
        val saddle = Vector3D(-0.85f, 0f, 2.5f)

        // Stunt & Orientation Euler Rotations in 3D
        val pitch = physics.pitchAngle
        val roll = physics.rollAngle
        val yaw = physics.yawAngle

        fun transformPoint(localVec: Vector3D): Offset {
            val rotated = localVec.rotateEuler(pitch, roll, yaw)
            val wx = physics.posX + rotated.x
            val wy = physics.posY + rotated.y
            val wz = physics.posZ + rotated.z
            return w2s(wx, wy, wz)
        }

        // Screen projections of 3D Bike nodes
        val pRearWheel = transformPoint(rearWheelCenter)
        val pFrontWheel = transformPoint(frontWheelCenter)
        val pBB = transformPoint(bottomBracket)
        val pSeat = transformPoint(seatJunction)
        val pHead = transformPoint(headTube)
        val pBarL = transformPoint(handlebarsL)
        val pBarR = transformPoint(handlebarsR)
        val pSaddle = transformPoint(saddle)

        // 1. Draw 3D Rear Wheel (Rim + Spokes + Tire)
        val wheelScreenR = 17f * (scale * 0.08f)
        drawScope.drawCircle(Color(0xFF111111), radius = wheelScreenR, center = pRearWheel)
        drawScope.drawCircle(Color(0xFF00E5FF), radius = wheelScreenR * 0.75f, center = pRearWheel, style = androidx.compose.ui.graphics.drawscope.Stroke(2.5f))
        drawScope.drawCircle(Color(0xFFFFD600), radius = 3.5f, center = pRearWheel)

        // 2. Draw 3D Front Wheel
        drawScope.drawCircle(Color(0xFF111111), radius = wheelScreenR, center = pFrontWheel)
        drawScope.drawCircle(Color(0xFF00E5FF), radius = wheelScreenR * 0.75f, center = pFrontWheel, style = androidx.compose.ui.graphics.drawscope.Stroke(2.5f))
        drawScope.drawCircle(Color(0xFFFFD600), radius = 3.5f, center = pFrontWheel)

        // 3. Draw 3D Tubular Diamond Frame (Chainstay, Seatstay, Top tube, Down tube, Seat tube, Fork)
        drawScope.drawLine(frameColor, pRearWheel, pBB, strokeWidth = 4.5f)
        drawScope.drawLine(frameColor, pRearWheel, pSeat, strokeWidth = 4.5f)
        drawScope.drawLine(frameColor, pBB, pSeat, strokeWidth = 5f)
        drawScope.drawLine(frameColor, pSeat, pHead, strokeWidth = 5f)
        drawScope.drawLine(frameColor, pBB, pHead, strokeWidth = 5.2f)
        drawScope.drawLine(Color.LightGray, pHead, pFrontWheel, strokeWidth = 4.5f) // Fork

        // Handlebars Stem & Crossbar
        val pBarCenter = transformPoint(handlebars)
        drawScope.drawLine(Color.LightGray, pHead, pBarCenter, strokeWidth = 3.8f)
        drawScope.drawLine(Color(0xFFFF9100), pBarL, pBarR, strokeWidth = 4.8f) // Handlebar pad

        // 3D Saddle
        drawScope.drawCircle(Color.Black, radius = 6.5f, center = pSaddle)

        // 4. Rotating 3D Cranks & Pedals
        val crankRad = 0.55f
        val crankAngle = if (physics.riderPose == RiderPose.PEDALING) physics.posX * 14f else 40f
        val cSin = sin(crankAngle * Math.PI.toFloat() / 180f) * crankRad
        val cCos = cos(crankAngle * Math.PI.toFloat() / 180f) * crankRad

        val pedalR = Vector3D(bottomBracket.x + cCos, bottomBracket.y + 0.45f, bottomBracket.z + cSin)
        val pedalL = Vector3D(bottomBracket.x - cCos, bottomBracket.y - 0.45f, bottomBracket.z - cSin)
        val pPedalR = transformPoint(pedalR)
        val pPedalL = transformPoint(pedalL)

        drawScope.drawLine(Color.White, pBB, pPedalR, strokeWidth = 3f)
        drawScope.drawLine(Color.White, pBB, pPedalL, strokeWidth = 3f)
        drawScope.drawCircle(Color(0xFFFFD600), radius = 3f, center = pPedalR)
        drawScope.drawCircle(Color(0xFFFFD600), radius = 3f, center = pPedalL)

        // 5. Draw 3D Articulated Rider Body Poses
        draw3DRiderPose(
            drawScope = drawScope,
            pose = physics.riderPose,
            pPedalR = pPedalR,
            pPedalL = pPedalL,
            pBarL = pBarL,
            pBarR = pBarR,
            pSaddle = pSaddle,
            transformPoint = ::transformPoint,
            jersey = jerseyColor,
            pants = pantsColor,
            helmet = helmetColor,
            scale = scale
        )
    }

    private fun draw3DRiderPose(
        drawScope: DrawScope,
        pose: RiderPose,
        pPedalR: Offset,
        pPedalL: Offset,
        pBarL: Offset,
        pBarR: Offset,
        pSaddle: Offset,
        transformPoint: (Vector3D) -> Offset,
        jersey: Color,
        pants: Color,
        helmet: Color,
        scale: Float
    ) {
        when (pose) {
            RiderPose.SUPERMAN -> {
                // Superman: Legs extended straight back horizontally in 3D air!
                val hip = transformPoint(Vector3D(-1.2f, 0f, 2.7f))
                val feet = transformPoint(Vector3D(-3.6f, 0f, 3.1f))
                val shoulder = transformPoint(Vector3D(0.35f, 0f, 3.1f))
                val head = transformPoint(Vector3D(0.75f, 0f, 3.6f))

                // Extended legs
                drawScope.drawLine(pants, hip, feet, strokeWidth = 8f, cap = StrokeCap.Round)
                // Extended torso
                drawScope.drawLine(jersey, hip, shoulder, strokeWidth = 10f, cap = StrokeCap.Round)
                // Arms to handlebars
                drawScope.drawLine(jersey, shoulder, pBarR, strokeWidth = 5.5f, cap = StrokeCap.Round)
                // 3D Helmet
                drawScope.drawCircle(helmet, radius = 9.5f, center = head)
                drawScope.drawCircle(Color(0xFF111111), radius = 4f, center = Offset(head.x + 4f, head.y))
            }

            RiderPose.TABLETOP -> {
                // Tabletop: Torso shifted and knees tucked sideways
                val hip = transformPoint(Vector3D(-0.7f, -0.8f, 2.5f))
                val knee = transformPoint(Vector3D(-0.15f, -0.9f, 2.2f))
                val shoulder = transformPoint(Vector3D(0.45f, -0.3f, 3.2f))
                val head = transformPoint(Vector3D(0.7f, 0f, 3.7f))

                drawScope.drawLine(pants, hip, knee, strokeWidth = 8f, cap = StrokeCap.Round)
                drawScope.drawLine(pants, knee, pPedalR, strokeWidth = 6.5f, cap = StrokeCap.Round)
                drawScope.drawLine(jersey, hip, shoulder, strokeWidth = 10f, cap = StrokeCap.Round)
                drawScope.drawLine(jersey, shoulder, pBarR, strokeWidth = 5.5f, cap = StrokeCap.Round)
                drawScope.drawCircle(helmet, radius = 9.5f, center = head)
            }

            else -> {
                // Standard Aggressive 3D Riding & Stunt Pose
                val hip = transformPoint(Vector3D(-0.65f, 0f, 2.7f))
                val kneeR = transformPoint(Vector3D(-0.2f, 0.45f, 1.9f))
                val kneeL = transformPoint(Vector3D(-0.2f, -0.45f, 1.9f))
                val shoulder = transformPoint(Vector3D(0.4f, 0f, 3.5f))
                val head = transformPoint(Vector3D(0.7f, 0f, 4.2f))

                // Right Leg
                drawScope.drawLine(pants, hip, kneeR, strokeWidth = 7.5f, cap = StrokeCap.Round)
                drawScope.drawLine(pants, kneeR, pPedalR, strokeWidth = 6.5f, cap = StrokeCap.Round)

                // Left Leg
                drawScope.drawLine(pants, hip, kneeL, strokeWidth = 7.5f, cap = StrokeCap.Round)
                drawScope.drawLine(pants, kneeL, pPedalL, strokeWidth = 6.5f, cap = StrokeCap.Round)

                // 3D Torso
                drawScope.drawLine(jersey, hip, shoulder, strokeWidth = 11f, cap = StrokeCap.Round)

                // 3D Arms gripping handlebars
                val elbowR = transformPoint(Vector3D(0.55f, 0.6f, 3.3f))
                val elbowL = transformPoint(Vector3D(0.55f, -0.6f, 3.3f))
                drawScope.drawLine(jersey, shoulder, elbowR, strokeWidth = 6f, cap = StrokeCap.Round)
                drawScope.drawLine(jersey, elbowR, pBarR, strokeWidth = 5.5f, cap = StrokeCap.Round)
                drawScope.drawLine(jersey, shoulder, elbowL, strokeWidth = 6f, cap = StrokeCap.Round)
                drawScope.drawLine(jersey, elbowL, pBarL, strokeWidth = 5.5f, cap = StrokeCap.Round)

                // 3D Helmet with visor
                drawScope.drawCircle(helmet, radius = 9.5f, center = head)
                drawScope.drawRect(
                    color = Color(0xFF111111),
                    topLeft = Offset(head.x + 2f, head.y - 3f),
                    size = Size(8f, 5f)
                )
            }
        }
    }

    private fun drawCrashedRagdoll3D(
        drawScope: DrawScope,
        physics: BmxPhysicsEngine,
        theme: ColorTheme,
        w2s: (Float, Float, Float) -> Offset,
        scale: Float
    ) {
        val bP = w2s(physics.crashBikePos.x, physics.crashBikePos.y, physics.crashBikePos.z)
        val rP = w2s(physics.crashRiderPos.x, physics.crashRiderPos.y, physics.crashRiderPos.z)

        val frameColor = Color(theme.bikeFrame)
        val jersey = Color(theme.riderJersey)
        val pants = Color(theme.riderPants)
        val helmet = Color(theme.helmet)

        // Tumbling Bike
        drawScope.rotate(degrees = physics.crashBikeRot.x, pivot = bP) {
            drawCircle(Color(0xFF111111), radius = 13f, center = Offset(bP.x - 18f, bP.y))
            drawCircle(Color(0xFF111111), radius = 13f, center = Offset(bP.x + 18f, bP.y))
            drawLine(frameColor, Offset(bP.x - 18f, bP.y), Offset(bP.x + 18f, bP.y), strokeWidth = 5f)
            drawLine(Color(0xFFFF9100), Offset(bP.x + 12f, bP.y - 14f), Offset(bP.x + 20f, bP.y - 14f), strokeWidth = 5f)
        }

        // Tumbling Rider Ragdoll
        drawScope.rotate(degrees = physics.crashRiderRot.x, pivot = rP) {
            drawLine(pants, rP, Offset(rP.x - 20f, rP.y - 16f), strokeWidth = 7f, cap = StrokeCap.Round)
            drawLine(pants, rP, Offset(rP.x + 18f, rP.y - 22f), strokeWidth = 7f, cap = StrokeCap.Round)
            drawLine(jersey, rP, Offset(rP.x + 10f, rP.y + 14f), strokeWidth = 9f, cap = StrokeCap.Round)
            drawLine(jersey, Offset(rP.x + 10f, rP.y + 14f), Offset(rP.x + 26f, rP.y + 20f), strokeWidth = 5.5f, cap = StrokeCap.Round)
            drawCircle(helmet, radius = 9f, center = Offset(rP.x + 18f, rP.y + 26f))
        }
    }

    private fun drawScanlines(drawScope: DrawScope, width: Float, height: Float) {
        val scanlineSpacing = 4f
        var y = 0f
        while (y < height) {
            drawScope.drawLine(
                color = Color.Black.copy(alpha = 0.14f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.2f
            )
            y += scanlineSpacing
        }
    }

    private fun drawHud(
        drawScope: DrawScope,
        width: Float,
        height: Float,
        physics: BmxPhysicsEngine,
        textMeasurer: TextMeasurer
    ) {
        val barHeight = 44f
        drawScope.drawRect(color = HUD_BG, topLeft = Offset(0f, 0f), size = Size(width, barHeight))
        drawScope.drawLine(color = HUD_BORDER, start = Offset(0f, barHeight), end = Offset(width, barHeight), strokeWidth = 2f)

        val hudLabelStyle = TextStyle(
            color = Color(0xFFD0BCFF),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        val hudValueStyle = TextStyle(
            color = Color(0xFFE6E1E9),
            fontSize = 13.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )

        // 1. SCORE
        val scoreText = String.format("%06d", physics.score)
        drawScope.drawText(textMeasurer, "SCORE", Offset(14f, 4f), hudLabelStyle)
        drawScope.drawText(textMeasurer, scoreText, Offset(14f, 18f), hudValueStyle)

        // 2. TIME
        val timeSec = physics.timeRemaining.toInt()
        val timeText = String.format("%02d:%02d", timeSec / 60, timeSec % 60)
        val timeColor = if (timeSec <= 10) Color(0xFFFF8585) else Color(0xFFD0BCFF)
        val timeX = width * 0.30f
        drawScope.drawText(textMeasurer, "TIME", Offset(timeX, 4f), hudLabelStyle)
        drawScope.drawText(textMeasurer, timeText, Offset(timeX, 18f), hudValueStyle.copy(color = timeColor))

        // 3. SPEED (MPH)
        val speedMph = (physics.velX * 2.237f).toInt().coerceAtLeast(0)
        val speedX = width * 0.52f
        drawScope.drawText(textMeasurer, "SPEED", Offset(speedX, 4f), hudLabelStyle)
        drawScope.drawText(textMeasurer, "$speedMph MPH", Offset(speedX, 18f), hudValueStyle)

        // 4. 3D COURSE RADAR & PROGRESS
        val distPct = ((physics.posX / physics.trackLength) * 100f).toInt().coerceIn(0, 100)
        val multX = width * 0.76f
        drawScope.drawText(textMeasurer, "3D RADAR", Offset(multX, 4f), hudLabelStyle)
        drawScope.drawText(textMeasurer, "$distPct%", Offset(multX, 18f), hudValueStyle.copy(color = Color(0xFFE8DEF8)))

        // Mini 3D Radar Track Progress Bar at bottom of HUD bar
        drawScope.drawRect(
            color = Color(0xFF2B2930),
            topLeft = Offset(0f, barHeight - 4f),
            size = Size(width, 4f)
        )
        drawScope.drawRect(
            color = Color(0xFFD0BCFF),
            topLeft = Offset(0f, barHeight - 4f),
            size = Size(width * (physics.posX / physics.trackLength).coerceIn(0f, 1f), 4f)
        )

        // 5. STUNT / WIPEOUT BANNER POPUP
        if (physics.currentTrickBanner.isNotEmpty()) {
            val banner = physics.currentTrickBanner
            val isWipeout = banner.contains("WIPEOUT")
            val bannerLayout = textMeasurer.measure(
                text = banner,
                style = TextStyle(
                    color = if (isWipeout) Color(0xFFFFB4AB) else Color(0xFFD0BCFF),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            )
            val bw = bannerLayout.size.width + 32f
            val bh = bannerLayout.size.height + 16f
            val bx = (width - bw) / 2f
            val by = height * 0.16f

            drawScope.drawRoundRect(
                color = Color(0xEA1C1B1F),
                topLeft = Offset(bx, by),
                size = Size(bw, bh),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f)
            )
            drawScope.drawRoundRect(
                color = if (isWipeout) Color(0xFFFFB4AB) else Color(0xFFD0BCFF),
                topLeft = Offset(bx, by),
                size = Size(bw, bh),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(2f)
            )
            drawScope.drawText(
                textMeasurer,
                text = banner,
                topLeft = Offset(bx + 16f, by + 8f),
                style = TextStyle(
                    color = if (isWipeout) Color(0xFFFFB4AB) else Color(0xFFD0BCFF),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            )
        }
    }
}
