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
import com.example.model.*
import kotlin.math.*

class LynxGraphicsRenderer {

    // Palette constants
    val SKY_TOP = Color(0xFF1E3C72)
    val SKY_MID = Color(0xFFE65C00)
    val SKY_BOTTOM = Color(0xFFF9D423)
    val SUN_COLOR = Color(0xFFFFEB3B)
    val MOUNTAIN_FAR = Color(0xFF4A148C)
    val MOUNTAIN_NEAR = Color(0xFF7B1FA2)
    val OCEAN_BLUE = Color(0xFF00ACC1)
    val PALM_TRUNK = Color(0xFF5D4037)
    val PALM_LEAVES = Color(0xFF2E7D32)
    val DIRT_TOP = Color(0xFFD7CCC8)
    val DIRT_MID = Color(0xFF8D6E63)
    val DIRT_DEEP = Color(0xFF4E342E)
    val DIRT_GRASS = Color(0xFF66BB6A)
    val MUD_COLOR = Color(0xFF3E2723)
    val HUD_BG = Color(0xF01C1B1F)
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

        // Camera tracking
        val cameraX = physics.posX
        val cameraY = physics.posY
        // Viewport scale: maps game meter coordinates to screen pixels
        val scale = width / 140f // ~140 meters visible across screen
        val screenCenterX = width * 0.35f
        val screenCenterY = height * 0.60f

        // Convert world coords to screen
        fun worldToScreenX(wx: Float): Float = screenCenterX + (wx - cameraX) * scale
        fun worldToScreenY(wy: Float): Float = screenCenterY + (wy - cameraY) * scale

        // 1. Draw Parallax Background
        drawSkyAndSun(drawScope, width, height, cameraX)
        drawDistantMountains(drawScope, width, height, cameraX)
        drawOceanAndBeach(drawScope, width, height, cameraX)
        drawPalmTrees(drawScope, width, height, cameraX)

        // 2. Draw Terrain & Ground
        drawTerrain(drawScope, width, height, physics, ::worldToScreenX, ::worldToScreenY, scale)

        // 3. Draw Obstacles & Decor
        drawObstacles(drawScope, physics, ::worldToScreenX, ::worldToScreenY, scale)

        // 4. Draw Particles (Dust, mud, debris)
        drawParticles(drawScope, physics, ::worldToScreenX, ::worldToScreenY)

        // 5. Draw Rider & Bike
        drawRiderAndBike(drawScope, physics, theme, ::worldToScreenX, ::worldToScreenY, scale)

        // 6. Draw Scanlines & Retro CRT/LCD Effect
        if (showScanlines) {
            drawScanlines(drawScope, width, height)
        }

        // 7. Draw HUD (Score, Time, Speed, Multiplier, Banner)
        drawHud(drawScope, width, height, physics, textMeasurer)
    }

    private fun drawSkyAndSun(drawScope: DrawScope, width: Float, height: Float, cameraX: Float) {
        // Sunset gradient
        val skyBrush = Brush.verticalGradient(
            0.0f to SKY_TOP,
            0.4f to SKY_MID,
            0.75f to SKY_BOTTOM,
            startY = 0f,
            endY = height * 0.75f
        )
        drawScope.drawRect(skyBrush, size = Size(width, height))

        // Retro striped California Sun
        val sunX = ((width * 0.75f) - (cameraX * 0.2f)) % (width + 200f) - 100f
        val sunY = height * 0.22f
        val sunRadius = height * 0.12f

        drawScope.drawCircle(
            color = SUN_COLOR,
            radius = sunRadius,
            center = Offset(sunX, sunY)
        )

        // Horizontal sun cut lines for authentic 80s California aesthetic
        for (i in 0..5) {
            val lineY = sunY + (i * sunRadius * 0.16f)
            val lineThickness = 3f + i * 2f
            drawScope.drawLine(
                color = SKY_MID,
                start = Offset(sunX - sunRadius * 1.1f, lineY),
                end = Offset(sunX + sunRadius * 1.1f, lineY),
                strokeWidth = lineThickness
            )
        }
    }

    private fun drawDistantMountains(drawScope: DrawScope, width: Float, height: Float, cameraX: Float) {
        val pathFar = Path()
        val pathNear = Path()
        val horizonY = height * 0.52f

        pathFar.moveTo(0f, horizonY)
        val step = 40f
        var x = 0f
        while (x <= width) {
            val wx = x + cameraX * 0.1f
            val my = horizonY - 45f - sin(wx * 0.015f) * 30f - cos(wx * 0.03f) * 15f
            pathFar.lineTo(x, my)
            x += step
        }
        pathFar.lineTo(width, height)
        pathFar.lineTo(0f, height)
        pathFar.close()
        drawScope.drawPath(pathFar, MOUNTAIN_FAR)

        pathNear.moveTo(0f, horizonY)
        x = 0f
        while (x <= width) {
            val wx = x + cameraX * 0.25f
            val my = horizonY - 25f - sin(wx * 0.025f) * 20f - cos(wx * 0.05f) * 10f
            pathNear.lineTo(x, my)
            x += step
        }
        pathNear.lineTo(width, height)
        pathNear.lineTo(0f, height)
        pathNear.close()
        drawScope.drawPath(pathNear, MOUNTAIN_NEAR)
    }

    private fun drawOceanAndBeach(drawScope: DrawScope, width: Float, height: Float, cameraX: Float) {
        val oceanY = height * 0.50f
        val oceanHeight = height * 0.08f
        drawScope.drawRect(
            color = OCEAN_BLUE,
            topLeft = Offset(0f, oceanY),
            size = Size(width, oceanHeight)
        )

        // Pixel waves
        val waveStep = 60f
        var wx = -(cameraX * 0.5f) % waveStep
        while (wx < width) {
            drawScope.drawLine(
                color = Color.White.copy(alpha = 0.6f),
                start = Offset(wx, oceanY + 12f),
                end = Offset(wx + 25f, oceanY + 12f),
                strokeWidth = 3f
            )
            drawScope.drawLine(
                color = Color.White.copy(alpha = 0.4f),
                start = Offset(wx + 30f, oceanY + 22f),
                end = Offset(wx + 48f, oceanY + 22f),
                strokeWidth = 2f
            )
            wx += waveStep
        }
    }

    private fun drawPalmTrees(drawScope: DrawScope, width: Float, height: Float, cameraX: Float) {
        val treeSpacing = 280f
        val startTreeX = -((cameraX * 0.6f) % treeSpacing) - 50f

        var tx = startTreeX
        while (tx < width + 100f) {
            val baseTreeY = height * 0.56f
            val trunkTopX = tx + 18f
            val trunkTopY = baseTreeY - 75f

            // Trunk
            val trunkPath = Path().apply {
                moveTo(tx, baseTreeY)
                quadraticTo(tx + 6f, baseTreeY - 38f, trunkTopX, trunkTopY)
                lineTo(trunkTopX + 8f, trunkTopY)
                quadraticTo(tx + 14f, baseTreeY - 38f, tx + 10f, baseTreeY)
                close()
            }
            drawScope.drawPath(trunkPath, PALM_TRUNK)

            // Palm fronds
            val frondAngles = listOf(-140f, -100f, -60f, -20f, 20f, 60f)
            for (angle in frondAngles) {
                drawScope.rotate(angle, pivot = Offset(trunkTopX + 4f, trunkTopY)) {
                    drawOval(
                        color = PALM_LEAVES,
                        topLeft = Offset(trunkTopX - 25f, trunkTopY - 40f),
                        size = Size(50f, 16f)
                    )
                }
            }
            tx += treeSpacing
        }
    }

    private fun drawTerrain(
        drawScope: DrawScope,
        width: Float,
        height: Float,
        physics: BmxPhysicsEngine,
        w2sX: (Float) -> Float,
        w2sY: (Float) -> Float,
        scale: Float
    ) {
        val visibleWorldStartX = physics.posX - (width / scale) * 0.5f - 10f
        val visibleWorldEndX = physics.posX + (width / scale) * 0.7f + 10f

        val path = Path()
        val grassPath = Path()

        var first = true
        var wx = visibleWorldStartX
        val step = 1.5f // World meters per terrain polygon slice

        while (wx <= visibleWorldEndX) {
            val wy = physics.getGroundHeight(wx)
            val sx = w2sX(wx)
            val sy = w2sY(wy)

            if (first) {
                path.moveTo(sx, sy)
                grassPath.moveTo(sx, sy)
                first = false
            } else {
                path.lineTo(sx, sy)
                grassPath.lineTo(sx, sy)
            }
            wx += step
        }

        // Fill bottom of dirt
        val lastSx = w2sX(visibleWorldEndX)
        val firstSx = w2sX(visibleWorldStartX)
        path.lineTo(lastSx, height + 50f)
        path.lineTo(firstSx, height + 50f)
        path.close()

        // Draw dirt gradient fill
        val dirtBrush = Brush.verticalGradient(
            0.0f to DIRT_TOP,
            0.15f to DIRT_MID,
            0.7f to DIRT_DEEP,
            startY = height * 0.4f,
            endY = height
        )
        drawScope.drawPath(path, dirtBrush)

        // Draw lush California grass / track ridge line on top of dirt
        drawScope.drawPath(
            grassPath,
            color = DIRT_GRASS,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 6f)
        )

        // Draw gravel/dirt speckles
        wx = visibleWorldStartX
        while (wx <= visibleWorldEndX) {
            val wy = physics.getGroundHeight(wx)
            val sx = w2sX(wx)
            val sy = w2sY(wy)
            if (((wx * 10).toInt() % 7) == 0) {
                drawScope.drawCircle(
                    color = Color.Black.copy(alpha = 0.25f),
                    radius = 3f,
                    center = Offset(sx, sy + 14f)
                )
            }
            if (((wx * 10).toInt() % 13) == 0) {
                drawScope.drawCircle(
                    color = Color.White.copy(alpha = 0.35f),
                    radius = 2.5f,
                    center = Offset(sx, sy + 24f)
                )
            }
            wx += 4f
        }
    }

    private fun drawObstacles(
        drawScope: DrawScope,
        physics: BmxPhysicsEngine,
        w2sX: (Float) -> Float,
        w2sY: (Float) -> Float,
        scale: Float
    ) {
        for (obs in physics.obstacles) {
            val sx = w2sX(obs.x)
            val groundY = physics.getGroundHeight(obs.x)
            val sy = w2sY(groundY)
            val sWidth = obs.width * scale

            when (obs.type) {
                ObstacleType.MUD_PUDDLE -> {
                    // Mud puddle on ground
                    drawScope.drawOval(
                        color = MUD_COLOR,
                        topLeft = Offset(sx, sy - 4f),
                        size = Size(sWidth, 12f)
                    )
                    drawScope.drawOval(
                        color = Color(0xFF2D1810),
                        topLeft = Offset(sx + 8f, sy - 2f),
                        size = Size(sWidth - 16f, 8f)
                    )
                }
                ObstacleType.ROCK -> {
                    // 3D Pixel Rock
                    val rockPath = Path().apply {
                        moveTo(sx, sy)
                        lineTo(sx + sWidth * 0.3f, sy - 18f)
                        lineTo(sx + sWidth * 0.7f, sy - 22f)
                        lineTo(sx + sWidth, sy - 6f)
                        lineTo(sx + sWidth * 0.8f, sy + 4f)
                        lineTo(sx, sy)
                        close()
                    }
                    drawScope.drawPath(rockPath, Color(0xFF616161))
                    drawScope.drawPath(
                        rockPath,
                        Color(0xFF9E9E9E),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                    )
                }
                ObstacleType.LOG -> {
                    // Brown Drift Log
                    drawScope.drawRoundRect(
                        color = Color(0xFF4E342E),
                        topLeft = Offset(sx, sy - 14f),
                        size = Size(sWidth, 16f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                    )
                    drawScope.drawCircle(
                        color = Color(0xFF8D6E63),
                        radius = 7f,
                        center = Offset(sx + sWidth, sy - 6f)
                    )
                }
                ObstacleType.CONE -> {
                    // Orange Traffic/Safety Cone
                    val conePath = Path().apply {
                        moveTo(sx, sy)
                        lineTo(sx + sWidth * 0.5f, sy - 26f)
                        lineTo(sx + sWidth, sy)
                        close()
                    }
                    drawScope.drawPath(conePath, Color(0xFFFF6D00))
                    // White stripe on cone
                    drawScope.drawLine(
                        color = Color.White,
                        start = Offset(sx + sWidth * 0.3f, sy - 12f),
                        end = Offset(sx + sWidth * 0.7f, sy - 12f),
                        strokeWidth = 4f
                    )
                }
                ObstacleType.BIG_RAMP -> {
                    // Big ramp warning stripes / banner
                    drawScope.drawRect(
                        color = Color(0xFFFFD600),
                        topLeft = Offset(sx, sy - 8f),
                        size = Size(sWidth, 6f)
                    )
                }
                ObstacleType.SPEED_BUMP -> {}
            }
        }

        // Draw Finish Line Banner & Grandstand at finishX
        val finishSx = w2sX(physics.finishX)
        val finishSy = w2sY(physics.getGroundHeight(physics.finishX))

        // Poles
        drawScope.drawRect(
            color = Color.White,
            topLeft = Offset(finishSx - 6f, finishSy - 140f),
            size = Size(12f, 140f)
        )
        drawScope.drawRect(
            color = Color.White,
            topLeft = Offset(finishSx + 60f, finishSy - 140f),
            size = Size(12f, 140f)
        )

        // Banner
        drawScope.drawRect(
            color = Color(0xFFFF1744),
            topLeft = Offset(finishSx - 10f, finishSy - 135f),
            size = Size(80f, 32f)
        )
        // Checkered stripes on banner
        for (row in 0..1) {
            for (col in 0..9) {
                val isBlack = (row + col) % 2 == 0
                drawScope.drawRect(
                    color = if (isBlack) Color.Black else Color.White,
                    topLeft = Offset(finishSx - 10f + col * 8f, finishSy - 135f + row * 16f),
                    size = Size(8f, 16f)
                )
            }
        }
    }

    private fun drawParticles(
        drawScope: DrawScope,
        physics: BmxPhysicsEngine,
        w2sX: (Float) -> Float,
        w2sY: (Float) -> Float
    ) {
        for (p in physics.particles) {
            val sx = w2sX(p.x)
            val sy = w2sY(p.y)
            val alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
            drawScope.drawCircle(
                color = Color(p.color).copy(alpha = alpha),
                radius = p.size,
                center = Offset(sx, sy)
            )
        }
    }

    private fun drawRiderAndBike(
        drawScope: DrawScope,
        physics: BmxPhysicsEngine,
        theme: ColorTheme,
        w2sX: (Float) -> Float,
        w2sY: (Float) -> Float,
        scale: Float
    ) {
        val riderSx = w2sX(physics.posX)
        val riderSy = w2sY(physics.posY)

        if (physics.riderPose == RiderPose.CRASHED) {
            // Draw Crashed Flying Rider and Tumbled Bike
            val bSx = w2sX(physics.crashBikeX)
            val bSy = w2sY(physics.crashBikeY)
            val rSx = w2sX(physics.crashRiderX)
            val rSy = w2sY(physics.crashRiderY)

            // Rolling Bike
            drawScope.rotate(degrees = physics.crashBikeX * 40f, pivot = Offset(bSx, bSy)) {
                drawBikeFrame(this, bSx, bSy, theme.bikeFrame, isCrashed = true)
            }
            // Tumbling Rider
            drawScope.rotate(degrees = physics.crashRiderX * 60f, pivot = Offset(rSx, rSy)) {
                drawCrashedRider(this, rSx, rSy, theme)
            }
            return
        }

        // Draw Active Bike & Rider at proper angle
        drawScope.rotate(degrees = physics.bikeAngle, pivot = Offset(riderSx, riderSy)) {
            // Bike Wheels, Frame, Pedals
            drawBikeFrame(
                this,
                riderSx,
                riderSy,
                theme.bikeFrame,
                isPedaling = physics.riderPose == RiderPose.PEDALING,
                pedalPhase = physics.posX * 12f
            )

            // Rider Body Posed
            drawRiderPose(
                this,
                riderSx,
                riderSy,
                physics.riderPose,
                theme,
                physics.posX * 12f
            )
        }
    }

    private fun drawBikeFrame(
        drawScope: DrawScope,
        cx: Float,
        cy: Float,
        frameColorLong: Long,
        isCrashed: Boolean = false,
        isPedaling: Boolean = false,
        pedalPhase: Float = 0f
    ) {
        val frameColor = Color(frameColorLong)
        val wheelRadius = 14f
        val rearWheelX = cx - 22f
        val frontWheelX = cx + 22f
        val wheelY = cy - 2f

        // Rear Wheel (Tire + Rim + Spokes)
        drawScope.drawCircle(color = Color(0xFF111111), radius = wheelRadius, center = Offset(rearWheelX, wheelY))
        drawScope.drawCircle(color = Color(0xFF00E5FF), radius = wheelRadius - 3f, center = Offset(rearWheelX, wheelY), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
        drawScope.drawCircle(color = Color(0xFFFFD600), radius = 3.5f, center = Offset(rearWheelX, wheelY))

        // Front Wheel
        drawScope.drawCircle(color = Color(0xFF111111), radius = wheelRadius, center = Offset(frontWheelX, wheelY))
        drawScope.drawCircle(color = Color(0xFF00E5FF), radius = wheelRadius - 3f, center = Offset(frontWheelX, wheelY), style = androidx.compose.ui.graphics.drawscope.Stroke(2f))
        drawScope.drawCircle(color = Color(0xFFFFD600), radius = 3.5f, center = Offset(frontWheelX, wheelY))

        // Frame Tubes
        val bottomBracket = Offset(cx - 3f, cy - 4f)
        val seatJunction = Offset(cx - 10f, cy - 24f)
        val headTube = Offset(cx + 14f, cy - 28f)
        val handlebars = Offset(cx + 12f, cy - 40f)

        // Chainstay / Seatstay
        drawScope.drawLine(frameColor, start = Offset(rearWheelX, wheelY), end = bottomBracket, strokeWidth = 3.5f)
        drawScope.drawLine(frameColor, start = Offset(rearWheelX, wheelY), end = seatJunction, strokeWidth = 3.5f)
        // Seat tube
        drawScope.drawLine(frameColor, start = bottomBracket, end = seatJunction, strokeWidth = 4f)
        // Top tube
        drawScope.drawLine(frameColor, start = seatJunction, end = headTube, strokeWidth = 4f)
        // Down tube
        drawScope.drawLine(frameColor, start = bottomBracket, end = headTube, strokeWidth = 4f)
        // Fork to front wheel
        drawScope.drawLine(Color.LightGray, start = headTube, end = Offset(frontWheelX, wheelY), strokeWidth = 3.5f)
        // Handlebars Stem & Crossbar
        drawScope.drawLine(Color.LightGray, start = headTube, end = handlebars, strokeWidth = 3f)
        drawScope.drawLine(Color(0xFFFF9100), start = Offset(handlebars.x - 8f, handlebars.y), end = Offset(handlebars.x + 8f, handlebars.y), strokeWidth = 3.5f)

        // Saddle / Seat
        drawScope.drawRoundRect(
            color = Color.Black,
            topLeft = Offset(seatJunction.x - 10f, seatJunction.y - 6f),
            size = Size(16f, 6f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
        )

        // Crankset & Pedals
        val crankRad = 7f
        val crankAngle = if (isPedaling) pedalPhase else 45f
        val pedalOffset = Offset(
            bottomBracket.x + cos(crankAngle * Math.PI.toFloat() / 180f) * crankRad,
            bottomBracket.y + sin(crankAngle * Math.PI.toFloat() / 180f) * crankRad
        )
        drawScope.drawLine(Color.White, start = bottomBracket, end = pedalOffset, strokeWidth = 2.5f)
        drawScope.drawCircle(color = Color(0xFFFFD600), radius = 2.5f, center = pedalOffset)
    }

    private fun drawRiderPose(
        drawScope: DrawScope,
        cx: Float,
        cy: Float,
        pose: RiderPose,
        theme: ColorTheme,
        pedalPhase: Float
    ) {
        val jersey = Color(theme.riderJersey)
        val pants = Color(theme.riderPants)
        val helmet = Color(theme.helmet)

        when (pose) {
            RiderPose.PEDALING -> {
                val pSin = sin(pedalPhase * Math.PI.toFloat() / 180f)
                val hip = Offset(cx - 8f, cy - 28f)
                val knee = Offset(cx - 2f + pSin * 6f, cy - 14f + pSin * 4f)
                val foot = Offset(cx - 3f + pSin * 8f, cy - 4f + pSin * 6f)

                // Leg
                drawScope.drawLine(pants, start = hip, end = knee, strokeWidth = 5f, cap = StrokeCap.Round)
                drawScope.drawLine(pants, start = knee, end = foot, strokeWidth = 4.5f, cap = StrokeCap.Round)

                // Torso
                val shoulder = Offset(cx + 2f, cy - 40f)
                drawScope.drawLine(jersey, start = hip, end = shoulder, strokeWidth = 8f, cap = StrokeCap.Round)

                // Arm to Handlebars
                val hand = Offset(cx + 12f, cy - 39f)
                val elbow = Offset(cx + 6f, cy - 32f)
                drawScope.drawLine(jersey, start = shoulder, end = elbow, strokeWidth = 4.5f, cap = StrokeCap.Round)
                drawScope.drawLine(jersey, start = elbow, end = hand, strokeWidth = 4f, cap = StrokeCap.Round)

                // Head / Helmet
                val head = Offset(cx + 6f, cy - 48f)
                drawScope.drawCircle(color = helmet, radius = 7f, center = head)
                drawScope.drawRect(color = Color(0xFF111111), topLeft = Offset(head.x + 1f, head.y - 3f), size = Size(6f, 4f))
            }
            RiderPose.TABLETOP -> {
                // Tabletop trick: bike flat, body twisted
                val hip = Offset(cx - 14f, cy - 22f)
                val knee = Offset(cx - 4f, cy - 10f)
                val foot = Offset(cx - 6f, cy - 4f)
                drawScope.drawLine(pants, start = hip, end = knee, strokeWidth = 5f, cap = StrokeCap.Round)
                drawScope.drawLine(pants, start = knee, end = foot, strokeWidth = 4.5f, cap = StrokeCap.Round)

                val shoulder = Offset(cx + 4f, cy - 32f)
                drawScope.drawLine(jersey, start = hip, end = shoulder, strokeWidth = 8f, cap = StrokeCap.Round)
                drawScope.drawLine(jersey, start = shoulder, end = Offset(cx + 12f, cy - 38f), strokeWidth = 4.5f, cap = StrokeCap.Round)

                val head = Offset(cx + 10f, cy - 38f)
                drawScope.drawCircle(color = helmet, radius = 7f, center = head)
            }
            RiderPose.SUPERMAN -> {
                // Superman: body fully extended backward horizontally!
                val hip = Offset(cx - 24f, cy - 36f)
                val feet = Offset(cx - 46f, cy - 34f)
                drawScope.drawLine(pants, start = hip, end = feet, strokeWidth = 6f, cap = StrokeCap.Round)

                val shoulder = Offset(cx - 4f, cy - 38f)
                drawScope.drawLine(jersey, start = hip, end = shoulder, strokeWidth = 8f, cap = StrokeCap.Round)
                drawScope.drawLine(jersey, start = shoulder, end = Offset(cx + 12f, cy - 39f), strokeWidth = 4.5f, cap = StrokeCap.Round)

                val head = Offset(cx + 2f, cy - 44f)
                drawScope.drawCircle(color = helmet, radius = 7f, center = head)
            }
            RiderPose.WHEELIE, RiderPose.TAILWHIP, RiderPose.BACKFLIP, RiderPose.FRONTFLIP, RiderPose.SPIN_360, RiderPose.IN_AIR, RiderPose.BUNNY_HOP, RiderPose.COASTING, RiderPose.FINISHED -> {
                // Classic aggressive riding pose
                val hip = Offset(cx - 9f, cy - 28f)
                val knee = Offset(cx - 4f, cy - 16f)
                val foot = Offset(cx - 3f, cy - 4f)
                drawScope.drawLine(pants, start = hip, end = knee, strokeWidth = 5f, cap = StrokeCap.Round)
                drawScope.drawLine(pants, start = knee, end = foot, strokeWidth = 4.5f, cap = StrokeCap.Round)

                val shoulder = Offset(cx + 1f, cy - 42f)
                drawScope.drawLine(jersey, start = hip, end = shoulder, strokeWidth = 8f, cap = StrokeCap.Round)

                val elbow = Offset(cx + 6f, cy - 33f)
                val hand = Offset(cx + 12f, cy - 39f)
                drawScope.drawLine(jersey, start = shoulder, end = elbow, strokeWidth = 4.5f, cap = StrokeCap.Round)
                drawScope.drawLine(jersey, start = elbow, end = hand, strokeWidth = 4f, cap = StrokeCap.Round)

                val head = Offset(cx + 5f, cy - 50f)
                drawScope.drawCircle(color = helmet, radius = 7f, center = head)
                drawScope.drawRect(color = Color(0xFF111111), topLeft = Offset(head.x + 1f, head.y - 3f), size = Size(6f, 4f))
            }
            RiderPose.CRASHED -> {}
        }
    }

    private fun drawCrashedRider(drawScope: DrawScope, rx: Float, ry: Float, theme: ColorTheme) {
        val jersey = Color(theme.riderJersey)
        val pants = Color(theme.riderPants)
        val helmet = Color(theme.helmet)

        // Limbs flailing
        drawScope.drawLine(pants, start = Offset(rx, ry), end = Offset(rx - 16f, ry - 12f), strokeWidth = 5f, cap = StrokeCap.Round)
        drawScope.drawLine(pants, start = Offset(rx, ry), end = Offset(rx + 14f, ry - 18f), strokeWidth = 5f, cap = StrokeCap.Round)
        drawScope.drawLine(jersey, start = Offset(rx, ry), end = Offset(rx + 8f, ry + 12f), strokeWidth = 7f, cap = StrokeCap.Round)
        drawScope.drawLine(jersey, start = Offset(rx + 8f, ry + 12f), end = Offset(rx + 22f, ry + 16f), strokeWidth = 4f, cap = StrokeCap.Round)
        drawScope.drawCircle(color = helmet, radius = 7f, center = Offset(rx + 14f, ry + 22f))
    }

    private fun drawScanlines(drawScope: DrawScope, width: Float, height: Float) {
        // Atari Lynx 160x102 LCD grid lines
        val scanlineSpacing = 4f
        var y = 0f
        while (y < height) {
            drawScope.drawLine(
                color = Color.Black.copy(alpha = 0.16f),
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
        // Retro Top Scoreboard Bar
        val barHeight = 44f
        drawScope.drawRect(
            color = HUD_BG,
            topLeft = Offset(0f, 0f),
            size = Size(width, barHeight)
        )
        drawScope.drawLine(
            color = HUD_BORDER,
            start = Offset(0f, barHeight),
            end = Offset(width, barHeight),
            strokeWidth = 2.5f
        )

        // Text style for retro 8-bit look
        val hudLabelStyle = TextStyle(
            color = Color(0xFFD0BCFF),
            fontSize = 11.sp,
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
        drawScope.drawText(textMeasurer, "SCORE", Offset(12f, 2f), hudLabelStyle)
        drawScope.drawText(textMeasurer, scoreText, Offset(12f, 18f), hudValueStyle)

        // 2. TIME
        val timeSec = physics.timeRemaining.toInt()
        val timeText = String.format("%02d:%02d", timeSec / 60, timeSec % 60)
        val timeColor = if (timeSec <= 10) Color(0xFFFF8585) else Color(0xFFD0BCFF)
        val timeStyle = hudValueStyle.copy(color = timeColor)
        val timeX = width * 0.32f
        drawScope.drawText(textMeasurer, "TIME", Offset(timeX, 2f), hudLabelStyle)
        drawScope.drawText(textMeasurer, timeText, Offset(timeX, 18f), timeStyle)

        // 3. SPEED (MPH)
        val speedMph = (physics.velX * 2.237f).toInt().coerceAtLeast(0)
        val speedX = width * 0.54f
        drawScope.drawText(textMeasurer, "SPEED", Offset(speedX, 2f), hudLabelStyle)
        drawScope.drawText(textMeasurer, "$speedMph MPH", Offset(speedX, 18f), hudValueStyle)

        // 4. MULTIPLIER / DISTANCE
        val distPct = ((physics.posX / physics.trackLength) * 100f).toInt().coerceIn(0, 100)
        val multX = width * 0.78f
        drawScope.drawText(textMeasurer, "COURSE", Offset(multX, 2f), hudLabelStyle)
        drawScope.drawText(textMeasurer, "$distPct%", Offset(multX, 18f), hudValueStyle.copy(color = Color(0xFFE8DEF8)))

        // Mini Progress bar at top edge
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

        // 5. TRICK / STATUS POPUP BANNER (Center screen)
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
            val by = height * 0.18f

            // Banner shadow & background
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
