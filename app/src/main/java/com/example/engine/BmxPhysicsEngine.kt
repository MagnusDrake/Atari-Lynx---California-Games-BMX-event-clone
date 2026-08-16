package com.example.engine

import com.example.audio.LynxAudioEngine
import com.example.model.*
import kotlin.math.*
import kotlin.random.Random

/**
 * 2D BMX Physics Engine inspired by California Games (Atari Lynx / NES / C64).
 * Handles gravity, velocity integration, dual-wheel terrain collision, downhill/uphill
 * momentum transfer, aerial stunt rotational dynamics, obstacle clearance/impact, and wipeout tumbling.
 */
class BmxPhysicsEngine(
    private val audio: LynxAudioEngine
) {
    // Track Geometry Constants
    val trackLength = 1200f // total course length in meters
    val startX = 20f
    val finishX = 1150f

    // Physical Constants
    val gravity = 26.5f // m/s^2 downwards
    val baseFriction = 0.988f // rolling resistance per 1/60s frame
    val mudFriction = 0.88f // viscous drag in mud
    val airDrag = 0.996f // aerodynamic drag in air
    val pedalAccel = 17.5f // pedaling acceleration
    val maxSpeed = 38.0f // top speed ceiling
    val wheelBase = 4.8f // distance between front and rear wheel centers
    val landingToleranceDeg = 36.0f // max allowed angular difference for safe landing

    // Rider 2D Physical State
    var posX = startX
    var posY = 0f
    var velX = 0f
    var velY = 0f
    var bikeAngle = 0f // in degrees (0 = horizontal facing right, positive = nose down, negative = nose up)
    var angularVel = 0f // deg/s

    var isOnGround = true
    var riderPose = RiderPose.COASTING
    var gamePhase = GamePhase.TITLE

    // Timers & Score Tracking
    var timeRemaining = 90.0f // seconds
    var score = 0
    var comboMultiplier = 1
    var wipeoutCount = 0
    var tricksCount = 0
    var bestTrick = "None"
    var bestTrickScore = 0

    // Air Trick Dynamics
    var inAirRotation = 0f
    var accumulatedFlipAngle = 0f
    var airTime = 0f
    var airTricksExecuted = mutableListOf<TrickScore>()
    var currentTrickBanner = ""
    var bannerTimer = 0f

    // Wheelie Tracking
    var wheelieDistance = 0f
    var isWheelieActive = false

    // Wipeout / Crash Ragdoll State
    var wipeoutTimer = 0f
    val wipeoutDuration = 2.4f
    var crashBikeX = 0f
    var crashBikeY = 0f
    var crashBikeVx = 0f
    var crashBikeVy = 0f
    var crashRiderX = 0f
    var crashRiderY = 0f
    var crashRiderVx = 0f
    var crashRiderVy = 0f

    // Visual Particles (Dirt, Mud, Dust, Spark Debris)
    val particles = mutableListOf<DustParticle>()
    private val random = Random(1337)

    // Obstacles
    val obstacles = mutableListOf<Obstacle>()

    init {
        generateObstacles()
        resetToStart()
    }

    fun resetToStart() {
        posX = startX
        posY = getGroundHeight(posX)
        velX = 0f
        velY = 0f
        bikeAngle = getGroundSlope(posX)
        angularVel = 0f
        isOnGround = true
        riderPose = RiderPose.COASTING
        timeRemaining = 90.0f
        score = 0
        comboMultiplier = 1
        wipeoutCount = 0
        tricksCount = 0
        bestTrick = "None"
        bestTrickScore = 0
        inAirRotation = 0f
        accumulatedFlipAngle = 0f
        airTime = 0f
        airTricksExecuted.clear()
        currentTrickBanner = ""
        bannerTimer = 0f
        wheelieDistance = 0f
        isWheelieActive = false
        wipeoutTimer = 0f
        particles.clear()
        obstacles.forEach { it.isCleared = false }
    }

    private fun generateObstacles() {
        obstacles.clear()
        var id = 0
        var x = 80f
        while (x < finishX - 60f) {
            val type = when (random.nextInt(6)) {
                0, 1 -> ObstacleType.MUD_PUDDLE
                2 -> ObstacleType.ROCK
                3 -> ObstacleType.LOG
                4 -> ObstacleType.CONE
                else -> ObstacleType.BIG_RAMP
            }
            val width = when (type) {
                ObstacleType.MUD_PUDDLE -> 18f
                ObstacleType.BIG_RAMP -> 14f
                ObstacleType.ROCK -> 6f
                ObstacleType.LOG -> 8f
                ObstacleType.CONE -> 4f
                ObstacleType.SPEED_BUMP -> 12f
            }
            obstacles.add(
                Obstacle(
                    id = ++id,
                    x = x,
                    width = width,
                    height = if (type == ObstacleType.BIG_RAMP) 9f else 5f,
                    type = type
                )
            )
            x += random.nextFloat() * 65f + 50f
        }
    }

    /**
     * Mathematical Ground Height Profile Y(x).
     * Combines multiple harmonic elevation waves and ramp kickers for authentic California BMX terrain.
     */
    fun getGroundHeight(x: Float): Float {
        val baseH = 200f
        if (x < 0) return baseH
        if (x >= finishX) return baseH // Smooth flat finish line

        // Multi-frequency smooth rolling hills
        val wave1 = sin(x * 0.035f) * 22f
        val wave2 = cos(x * 0.08f) * 14f
        val wave3 = sin(x * 0.18f) * 8f

        // Specific dirt ramp elevations based on obstacle placement
        var rampOffset = 0f
        for (obs in obstacles) {
            if (obs.type == ObstacleType.BIG_RAMP && x in (obs.x - 8f)..(obs.x + obs.width)) {
                val p = (x - (obs.x - 8f)) / (obs.width + 8f)
                if (p < 0.75f) {
                    rampOffset = -sin(p * Math.PI.toFloat() * 0.65f) * 28f
                } else {
                    rampOffset = -(1f - p) * 20f
                }
            }
        }

        return baseH + wave1 + wave2 + wave3 + rampOffset
    }

    /**
     * Mathematical slope in degrees at coordinate x.
     * Evaluated using centered finite differences with delta smoothing.
     */
    fun getGroundSlope(x: Float): Float {
        val delta = 0.8f
        val y1 = getGroundHeight(x - delta)
        val y2 = getGroundHeight(x + delta)
        val rad = atan2(y2 - y1, delta * 2f)
        return rad * 180f / Math.PI.toFloat()
    }

    /**
     * Primary 2D Physics Tick.
     * Updates velocity, gravity, collisions, stunt angular rotation, and wipeouts.
     */
    fun update(
        dt: Float,
        isPedalPressed: Boolean,
        isJumpPressed: Boolean,
        isLeanBack: Boolean,
        isLeanForward: Boolean,
        isTrickUp: Boolean,
        isTrickDown: Boolean
    ) {
        // Banner countdown
        if (bannerTimer > 0f) {
            bannerTimer -= dt
            if (bannerTimer <= 0f) {
                currentTrickBanner = ""
            }
        }

        // Particle physics update
        updateParticles(dt)

        // Phase specific physics dispatch
        when (gamePhase) {
            GamePhase.TITLE, GamePhase.LEADERBOARD, GamePhase.TRICK_GUIDE, GamePhase.COUNTDOWN, GamePhase.SETTINGS -> {
                updateTitleDemo(dt)
            }
            GamePhase.PLAYING -> {
                updatePlaying(dt, isPedalPressed, isJumpPressed, isLeanBack, isLeanForward, isTrickUp, isTrickDown)
            }
            GamePhase.WIPEOUT_RECOVERY -> {
                updateWipeout(dt)
            }
            GamePhase.FINISHED -> {
                // Smooth deceleration across the finish line
                velX *= 0.94f
                posX += velX * dt
                posY = getGroundHeight(posX)
                bikeAngle = getGroundSlope(posX)
                riderPose = RiderPose.FINISHED
            }
            else -> {
                updateTitleDemo(dt)
            }
        }
    }

    private fun updateTitleDemo(dt: Float) {
        // Continuous scenic cruise across the California hills for attract mode
        velX = 14.5f
        posX += velX * dt
        if (posX > finishX) {
            posX = startX
        }
        posY = getGroundHeight(posX)
        bikeAngle = getGroundSlope(posX)
        riderPose = if ((System.currentTimeMillis() / 350) % 2 == 0L) RiderPose.PEDALING else RiderPose.COASTING
    }

    private fun updatePlaying(
        dt: Float,
        isPedalPressed: Boolean,
        isJumpPressed: Boolean,
        isLeanBack: Boolean,
        isLeanForward: Boolean,
        isTrickUp: Boolean,
        isTrickDown: Boolean
    ) {
        // Race clock countdown
        timeRemaining -= dt
        if (timeRemaining <= 0f) {
            timeRemaining = 0f
            finishRun()
            return
        }

        // Finish line crossing check
        if (posX >= finishX) {
            finishRun()
            return
        }

        val groundY = getGroundHeight(posX)
        val groundSlope = getGroundSlope(posX)
        val slopeRad = groundSlope * Math.PI.toFloat() / 180f

        // 1. Obstacle Collision Detection
        var currentFriction = baseFriction
        for (obs in obstacles) {
            val riderInObstacleX = posX >= obs.x && posX <= (obs.x + obs.width)
            if (riderInObstacleX) {
                when (obs.type) {
                    ObstacleType.MUD_PUDDLE -> {
                        if (isOnGround) {
                            currentFriction = mudFriction
                            velX = max(velX - 14f * dt, 2f)
                            if (random.nextFloat() < 0.35f) {
                                audio.playMudSplash()
                                spawnMudParticles(posX, groundY)
                            }
                        }
                    }
                    ObstacleType.ROCK, ObstacleType.LOG, ObstacleType.CONE -> {
                        val obstacleTopY = groundY - obs.height * 2.5f
                        if (isOnGround && !obs.isCleared) {
                            // Direct impact crash!
                            triggerCrash("Direct ${obs.type.displayName} Impact!")
                            return
                        } else if (!isOnGround && posY >= obstacleTopY && !obs.isCleared) {
                            // Clipped obstacle in low jump
                            triggerCrash("Clipped ${obs.type.displayName}!")
                            return
                        } else if (!isOnGround && posY < obstacleTopY && !obs.isCleared) {
                            // Clean hurdle cleared above obstacle
                            obs.isCleared = true
                            addScore(obs.type.pointsOnClear, "Clean Jump! ${obs.type.displayName} +${obs.type.pointsOnClear}")
                            audio.playTrickSuccess("Hurdle")
                        }
                    }
                    ObstacleType.BIG_RAMP, ObstacleType.SPEED_BUMP -> {
                        // Natural ramp launch
                    }
                }
            }
        }

        // 2. Ground vs Air Physics Pipeline
        if (isOnGround) {
            // Ground Dynamics
            posY = groundY
            velY = 0f
            airTime = 0f
            inAirRotation = 0f
            accumulatedFlipAngle = 0f

            // A. Pedaling & Forward Acceleration
            if (isPedalPressed) {
                // Diminishing acceleration near top speed
                val speedRatio = (velX / maxSpeed).coerceIn(0f, 1f)
                val effectiveAccel = pedalAccel * (1f - speedRatio * 0.4f)
                velX = min(velX + effectiveAccel * dt, maxSpeed)
                riderPose = RiderPose.PEDALING
                if (random.nextFloat() < 0.22f) {
                    audio.playPedalTick()
                    spawnDirtParticles(posX, posY)
                }
            } else {
                velX *= currentFriction.pow(dt * 60f)
                riderPose = RiderPose.COASTING
            }

            // B. Downhill / Uphill Slope Gravitational Acceleration
            // Downslope: accelerates forward; Upslope: decelerates
            val slopeGravityAccel = sin(slopeRad) * gravity * 0.45f
            velX += slopeGravityAccel * dt
            velX = velX.coerceIn(0f, maxSpeed * 1.15f)

            // C. Ground Stunt: Wheelie
            if (isLeanBack && velX > 3.5f) {
                riderPose = RiderPose.WHEELIE
                bikeAngle = groundSlope - 34f
                wheelieDistance += velX * dt
                isWheelieActive = true
                if (random.nextFloat() < 0.16f) {
                    addScore(2, "Wheelie!", false)
                }
            } else {
                isWheelieActive = false
                // Match bike angle to terrain slope
                bikeAngle = groundSlope
            }

            // D. Jump / Bunnyhop Impulse
            if (isJumpPressed) {
                isOnGround = false
                // Launch impulse combines vertical pop and forward speed scaling
                val jumpPop = -16.8f - (velX * 0.16f)
                velY = jumpPop
                bikeAngle = groundSlope - 8f
                inAirRotation = 0f
                accumulatedFlipAngle = 0f
                airTricksExecuted.clear()
                riderPose = RiderPose.BUNNY_HOP
                audio.playJump()
                spawnDirtParticles(posX, posY)
            }

            // E. Crest Launch Detection (Launching off hill peaks)
            val lookAheadX = posX + velX * dt * 1.5f
            val lookAheadGroundY = getGroundHeight(lookAheadX)
            val projectedY = posY + (velX * dt * sin(slopeRad))
            if (lookAheadGroundY > projectedY + 4.0f && velX > 7.0f && slopeRad < 0f) {
                // Launched into the air by slope drop-off
                isOnGround = false
                velY = -velX * sin(abs(slopeRad)) * 0.85f - 2.5f
                inAirRotation = 0f
                accumulatedFlipAngle = 0f
                airTricksExecuted.clear()
                riderPose = RiderPose.IN_AIR
                audio.playJump()
            }

            posX += velX * dt

        } else {
            // 3. Air Flight Physics
            airTime += dt

            // Integrate Gravity and Air Drag
            velY += gravity * dt
            velX *= airDrag.pow(dt * 60f)
            posX += velX * dt
            posY += velY * dt

            // Angular Stunt Torque & Pose Evaluation
            var targetAngularSpeed = 0f

            if (isLeanBack) {
                // Backflip rotation (counter-clockwise)
                targetAngularSpeed = -375f
                riderPose = RiderPose.BACKFLIP
            } else if (isLeanForward) {
                // Frontflip rotation (clockwise)
                targetAngularSpeed = 375f
                riderPose = RiderPose.FRONTFLIP
            } else if (isTrickUp) {
                // Tabletop / Lookback (Bike tilted flat)
                riderPose = RiderPose.TABLETOP
                targetAngularSpeed = 0f
                checkAirTrick("Tabletop", 300)
            } else if (isTrickDown) {
                // Superman (Rider extends legs back)
                riderPose = RiderPose.SUPERMAN
                targetAngularSpeed = 0f
                checkAirTrick("Superman", 450)
            } else if (isJumpPressed) {
                // Tailwhip / 360 Spin in air
                riderPose = RiderPose.TAILWHIP
                targetAngularSpeed = -240f
                checkAirTrick("Tailwhip", 350)
            } else {
                riderPose = RiderPose.IN_AIR
                targetAngularSpeed = 0f
            }

            // Angular integration
            bikeAngle += targetAngularSpeed * dt
            val deltaAngle = abs(targetAngularSpeed * dt)
            inAirRotation += deltaAngle
            accumulatedFlipAngle += targetAngularSpeed * dt

            // Check completed 360 and 720 degree flips
            if (abs(accumulatedFlipAngle) >= 330f && abs(accumulatedFlipAngle) < 660f) {
                if (accumulatedFlipAngle < 0) {
                    checkAirTrick("Backflip!", 600)
                } else {
                    checkAirTrick("Frontflip!", 750)
                }
            } else if (abs(accumulatedFlipAngle) >= 660f) {
                if (accumulatedFlipAngle < 0) {
                    checkAirTrick("Double Backflip!", 1400)
                } else {
                    checkAirTrick("Double Frontflip!", 1800)
                }
            }

            // 4. Ground Contact & Landing Collision Check
            // Dual wheel collision check
            val halfWheelbase = wheelBase / 2f
            val radAngle = bikeAngle * Math.PI.toFloat() / 180f
            val rearX = posX - cos(radAngle) * halfWheelbase
            val frontX = posX + cos(radAngle) * halfWheelbase
            val rearGroundY = getGroundHeight(rearX)
            val frontGroundY = getGroundHeight(frontX)
            val avgGroundY = (rearGroundY + frontGroundY) / 2f

            if ((posY >= groundY || posY >= avgGroundY) && velY > 0f) {
                // Bike has contacted terrain
                posY = groundY
                val landingSlope = getGroundSlope(posX)
                val diffAngle = normalizeAngle(bikeAngle - landingSlope)

                // Safe Landing Criteria:
                // 1. Angle aligned with terrain slope within tolerance
                // 2. Not completely upside down
                val isUpright = abs(diffAngle) <= landingToleranceDeg

                if (isUpright) {
                    // Clean Safe Landing!
                    isOnGround = true
                    bikeAngle = landingSlope
                    velY = 0f
                    riderPose = RiderPose.COASTING

                    // Downslope Landing Pump:
                    // Converting landing impact into forward acceleration if landing on downslope
                    if (landingSlope > 5f) {
                        val pumpBoost = sin(landingSlope * Math.PI.toFloat() / 180f) * 4.5f
                        velX = min(velX + pumpBoost, maxSpeed * 1.1f)
                    }

                    // Combo Multiplier & Score calculation
                    if (airTricksExecuted.isNotEmpty()) {
                        comboMultiplier++
                        val baseTrickSum = airTricksExecuted.sumOf { it.points }
                        val comboTotal = baseTrickSum * comboMultiplier
                        addScore(comboTotal, "Stunt Combo x$comboMultiplier! +$comboTotal")
                        audio.playTrickSuccess("Combo")
                    }

                    spawnLandingDust(posX, groundY)
                    airTricksExecuted.clear()
                    inAirRotation = 0f
                    accumulatedFlipAngle = 0f

                } else {
                    // BAD ANGLE / HEAD IMPACT - CRASH & WIPEOUT!
                    val crashReason = if (abs(diffAngle) > 90f) "Inverted Crash!" else "Rough Angle Wipeout!"
                    triggerCrash(crashReason)
                }
            }
        }
    }

    private fun checkAirTrick(name: String, points: Int) {
        if (airTricksExecuted.none { it.trickName == name }) {
            val trick = TrickScore(name, points)
            airTricksExecuted.add(trick)
            tricksCount++
            if (points > bestTrickScore) {
                bestTrickScore = points
                bestTrick = name
            }
            audio.playTrickSuccess(name)
            showTrickBanner("$name! +$points")
        }
    }

    private fun triggerCrash(reason: String) {
        isOnGround = true
        gamePhase = GamePhase.WIPEOUT_RECOVERY
        wipeoutCount++
        wipeoutTimer = wipeoutDuration
        riderPose = RiderPose.CRASHED
        audio.playCrash()

        // Decouple rider and bike into two tumbling bodies with initial impulse
        crashBikeX = posX
        crashBikeY = posY
        crashBikeVx = velX * 0.6f + 4f
        crashBikeVy = -8f

        crashRiderX = posX + 3f
        crashRiderY = posY - 4f
        crashRiderVx = velX * 0.75f + 6f
        crashRiderVy = -12f

        velX = 0f
        velY = 0f
        airTricksExecuted.clear()
        comboMultiplier = 1
        showTrickBanner("WIPEOUT! $reason")
        spawnCrashDebris(posX, posY)
    }

    private fun updateWipeout(dt: Float) {
        wipeoutTimer -= dt

        // 2D Ragdoll Physics for Flying Bike
        crashBikeVy += gravity * 1.2f * dt
        crashBikeX += crashBikeVx * dt
        crashBikeY += crashBikeVy * dt
        val bikeGroundY = getGroundHeight(crashBikeX)
        if (crashBikeY >= bikeGroundY) {
            crashBikeY = bikeGroundY
            crashBikeVy = -crashBikeVy * 0.35f // bounce damping
            crashBikeVx *= 0.88f // ground friction
        }

        // 2D Ragdoll Physics for Tumbling Rider
        crashRiderVy += gravity * 1.2f * dt
        crashRiderX += crashRiderVx * dt
        crashRiderY += crashRiderVy * dt
        val riderGroundY = getGroundHeight(crashRiderX)
        if (crashRiderY >= riderGroundY) {
            crashRiderY = riderGroundY
            crashRiderVy = -crashRiderVy * 0.25f // bounce damping
            crashRiderVx *= 0.82f // ground friction
        }

        if (wipeoutTimer <= 0f) {
            // Recovery back onto the bike
            posX = max(crashRiderX, crashBikeX) + 3f
            posY = getGroundHeight(posX)
            bikeAngle = getGroundSlope(posX)
            velX = 4.5f // gentle restart push
            velY = 0f
            isOnGround = true
            riderPose = RiderPose.COASTING
            gamePhase = GamePhase.PLAYING
        }
    }

    private fun finishRun() {
        gamePhase = GamePhase.FINISHED
        // Time remaining bonus
        val timeBonus = (timeRemaining * 45f).toInt()
        val cleanRunBonus = max(0, 1600 - (wipeoutCount * 400))
        val totalBonus = timeBonus + cleanRunBonus
        addScore(totalBonus, "COURSE COMPLETE! +$totalBonus")
        audio.playFinishCheer()
    }

    fun addScore(points: Int, banner: String, showBanner: Boolean = true) {
        score += points
        if (showBanner) {
            showTrickBanner(banner)
        }
    }

    fun showTrickBanner(text: String) {
        currentTrickBanner = text
        bannerTimer = 1.8f
    }

    private fun normalizeAngle(angle: Float): Float {
        var a = angle % 360f
        if (a > 180f) a -= 360f
        if (a < -180f) a += 360f
        return a
    }

    private fun updateParticles(dt: Float) {
        val pIter = particles.iterator()
        while (pIter.hasNext()) {
            val p = pIter.next()
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.vy += gravity * 0.4f * dt // particle gravity
            p.life -= dt
            if (p.life <= 0f) {
                pIter.remove()
            }
        }
    }

    private fun spawnDirtParticles(x: Float, y: Float) {
        for (i in 0 until 3) {
            particles.add(
                DustParticle(
                    x = x - random.nextFloat() * 5f,
                    y = y + random.nextFloat() * 3f,
                    vx = -velX * 0.35f - random.nextFloat() * 6f,
                    vy = -random.nextFloat() * 5f - 2f,
                    life = 0.35f,
                    maxLife = 0.35f,
                    color = 0xFF8D6E63,
                    size = 4f + random.nextFloat() * 3f
                )
            )
        }
    }

    private fun spawnMudParticles(x: Float, y: Float) {
        for (i in 0 until 6) {
            particles.add(
                DustParticle(
                    x = x + random.nextFloat() * 6f - 3f,
                    y = y,
                    vx = -velX * 0.45f - random.nextFloat() * 8f,
                    vy = -random.nextFloat() * 14f - 4f,
                    life = 0.5f,
                    maxLife = 0.5f,
                    color = 0xFF4E342E,
                    size = 5f + random.nextFloat() * 4f
                )
            )
        }
    }

    private fun spawnLandingDust(x: Float, y: Float) {
        for (i in 0 until 12) {
            val dir = if (i % 2 == 0) 1f else -1f
            particles.add(
                DustParticle(
                    x = x,
                    y = y,
                    vx = dir * (random.nextFloat() * 14f + 3f),
                    vy = -random.nextFloat() * 7f - 2f,
                    life = 0.55f,
                    maxLife = 0.55f,
                    color = 0xFFD7CCC8,
                    size = 5f + random.nextFloat() * 4f
                )
            )
        }
    }

    private fun spawnCrashDebris(x: Float, y: Float) {
        for (i in 0 until 20) {
            particles.add(
                DustParticle(
                    x = x,
                    y = y - 4f,
                    vx = (random.nextFloat() - 0.5f) * 26f,
                    vy = -random.nextFloat() * 18f - 4f,
                    life = 0.85f,
                    maxLife = 0.85f,
                    color = if (i % 3 == 0) 0xFFFF5722 else if (i % 3 == 1) 0xFFFFEB3B else 0xFF8D6E63,
                    size = 4f + random.nextFloat() * 5f
                )
            )
        }
    }
}
