package com.example.engine

import com.example.audio.LynxAudioEngine
import com.example.model.*
import kotlin.math.*
import kotlin.random.Random

/**
 * 3D Isometric BMX Physics Engine inspired by California Games (Atari Lynx / Arcade).
 * Simulates 3-axis motion:
 * - X: Forward track progression (0..1200m)
 * - Y: Lateral lane position (-10m left to +10m right)
 * - Z: Elevation, hills, ramps, and aerial jump altitude
 *
 * Handles 3D Euler stunt rotations (Pitch/Flips, Roll/Tabletop, Yaw/Tailwhip),
 * dynamic ground normal collision, 3D obstacle avoidance/clearance, and 3D tumbling ragdoll wipeouts.
 */
class BmxPhysicsEngine(
    private val audio: LynxAudioEngine
) {
    // Track 3D Geometry Constants
    val trackLength = 1200f
    val trackHalfWidth = 9.5f // Track boundaries: Y in [-9.5, +9.5]
    val startX = 20f
    val finishX = 1150f

    // Physical Constants
    val gravity = 27.5f // m/s^2 downwards (Z axis)
    val baseFriction = 0.989f // rolling resistance
    val mudFriction = 0.86f // mud drag
    val airDrag = 0.995f // aerodynamic drag in air
    val pedalAccel = 18.5f // forward pedaling acceleration
    val steerAccel = 36.0f // lateral steering responsiveness
    val maxSpeed = 38.0f // top forward speed
    val maxSteerSpeed = 14.0f // max lateral velocity
    val wheelBase = 4.8f // distance between wheels
    val landingToleranceDeg = 38.0f // safe landing angle tolerance

    // 3D Rider Physical State
    var posX = startX
    var posY = 0f
    var posZ = 0f

    var velX = 0f
    var velY = 0f
    var velZ = 0f

    // 3D Euler Angles (in degrees)
    var pitchAngle = 0f // Nose up / down & Flips
    var rollAngle = 0f  // Lean into turns & Tabletop
    var yawAngle = 0f   // Heading & Tailwhip spins

    var isOnGround = true
    var riderPose = RiderPose.COASTING
    var gamePhase = GamePhase.TITLE

    // Timers & Scoring
    var timeRemaining = 90.0f
    var score = 0
    var comboMultiplier = 1
    var wipeoutCount = 0
    var tricksCount = 0
    var bestTrick = "None"
    var bestTrickScore = 0

    // 3D Aerial Stunt Dynamics
    var airTime = 0f
    var accumulatedPitchFlip = 0f
    var accumulatedYawSpin = 0f
    var airTricksExecuted = mutableListOf<TrickScore>()
    var currentTrickBanner = ""
    var bannerTimer = 0f

    // Wheelie
    var wheelieDistance = 0f
    var isWheelieActive = false

    // 3D Wipeout / Crash Ragdoll State
    var wipeoutTimer = 0f
    val wipeoutDuration = 2.5f

    // Bike Ragdoll
    var crashBikePos = Vector3D()
    var crashBikeVel = Vector3D()
    var crashBikeRot = Vector3D() // Euler angles

    // Rider Ragdoll
    var crashRiderPos = Vector3D()
    var crashRiderVel = Vector3D()
    var crashRiderRot = Vector3D()

    // 3D Particles
    val particles = mutableListOf<DustParticle>()
    private val random = Random(1337)

    // 3D Obstacles
    val obstacles = mutableListOf<Obstacle>()

    init {
        generateObstacles()
        resetToStart()
    }

    fun resetToStart() {
        posX = startX
        posY = 0f
        posZ = getGroundHeight(posX, posY)
        velX = 0f
        velY = 0f
        velZ = 0f
        pitchAngle = getGroundPitch(posX, posY)
        rollAngle = getGroundRoll(posX, posY)
        yawAngle = 0f
        isOnGround = true
        riderPose = RiderPose.COASTING
        timeRemaining = 90.0f
        score = 0
        comboMultiplier = 1
        wipeoutCount = 0
        tricksCount = 0
        bestTrick = "None"
        bestTrickScore = 0
        airTime = 0f
        accumulatedPitchFlip = 0f
        accumulatedYawSpin = 0f
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
        var x = 70f
        while (x < finishX - 50f) {
            val type = when (random.nextInt(6)) {
                0, 1 -> ObstacleType.MUD_PUDDLE
                2 -> ObstacleType.ROCK
                3 -> ObstacleType.LOG
                4 -> ObstacleType.CONE
                else -> ObstacleType.BIG_RAMP
            }

            // Distribute across lanes: Left (-5.5m), Center (0m), Right (+5.5m)
            val laneY = when (type) {
                ObstacleType.MUD_PUDDLE -> (random.nextFloat() * 10f - 5f)
                ObstacleType.BIG_RAMP -> (random.nextInt(3) - 1) * 4.5f // Ramp spans lane
                ObstacleType.ROCK -> (random.nextFloat() * 12f - 6f)
                ObstacleType.LOG -> (random.nextFloat() * 10f - 5f)
                ObstacleType.CONE -> (random.nextFloat() * 14f - 7f)
                ObstacleType.SPEED_BUMP -> 0f
            }

            val width = when (type) {
                ObstacleType.MUD_PUDDLE -> 18f
                ObstacleType.BIG_RAMP -> 16f
                ObstacleType.ROCK -> 6f
                ObstacleType.LOG -> 7f
                ObstacleType.CONE -> 4f
                ObstacleType.SPEED_BUMP -> 14f
            }

            val depth = when (type) {
                ObstacleType.MUD_PUDDLE -> 8f
                ObstacleType.BIG_RAMP -> 7.5f
                ObstacleType.ROCK -> 5f
                ObstacleType.LOG -> 9f
                ObstacleType.CONE -> 4f
                ObstacleType.SPEED_BUMP -> 16f
            }

            val height = when (type) {
                ObstacleType.BIG_RAMP -> 10.5f
                ObstacleType.ROCK -> 4.5f
                ObstacleType.LOG -> 4.0f
                ObstacleType.CONE -> 3.5f
                ObstacleType.MUD_PUDDLE -> 0.4f
                ObstacleType.SPEED_BUMP -> 3.0f
            }

            obstacles.add(
                Obstacle(
                    id = ++id,
                    x = x,
                    laneY = laneY,
                    width = width,
                    depth = depth,
                    height = height,
                    type = type
                )
            )
            x += random.nextFloat() * 55f + 40f
        }
    }

    /**
     * 3D Ground Elevation Z(x, y).
     * Combines rolling harmonic hills along X, banked curves along Y, and 3D ramp kickers.
     */
    fun getGroundHeight(x: Float, y: Float): Float {
        val baseH = 0f
        if (x < 0) return baseH
        if (x >= finishX) return baseH

        // Multi-frequency rolling terrain
        val wave1 = sin(x * 0.032f) * 14f
        val wave2 = cos(x * 0.075f) * 8f
        val wave3 = sin(x * 0.16f) * 4f

        // Lateral banking (curves)
        val banking = sin(x * 0.04f) * (y * 0.25f)

        // Mega ramp and kicker elevation additions
        var rampOffset = 0f
        for (obs in obstacles) {
            if (obs.type == ObstacleType.BIG_RAMP || obs.type == ObstacleType.SPEED_BUMP) {
                val dx = x - obs.x
                val dy = y - obs.laneY
                val halfW = obs.width / 2f
                val halfD = obs.depth / 2f

                if (abs(dx) <= halfW && abs(dy) <= halfD) {
                    val progressX = (dx + halfW) / obs.width // 0..1 along ramp
                    val widthFade = 1f - (abs(dy) / halfD).pow(2)

                    if (obs.type == ObstacleType.BIG_RAMP) {
                        // Curved kicker launch ramp
                        val rampProfile = sin(progressX * (Math.PI.toFloat() * 0.5f)).pow(1.8f) * obs.height
                        rampOffset = max(rampOffset, rampProfile * widthFade)
                    } else {
                        // Mogul bump
                        val bumpProfile = sin(progressX * Math.PI.toFloat()) * obs.height
                        rampOffset = max(rampOffset, bumpProfile * widthFade)
                    }
                }
            }
        }

        return baseH + wave1 + wave2 + wave3 + banking + rampOffset
    }

    /**
     * Mathematical Pitch slope (nose up/down) in degrees along X.
     */
    fun getGroundPitch(x: Float, y: Float): Float {
        val delta = 0.8f
        val z1 = getGroundHeight(x - delta, y)
        val z2 = getGroundHeight(x + delta, y)
        val rad = atan2(z2 - z1, delta * 2f)
        return -rad * 180f / Math.PI.toFloat() // negative = nose down, positive = nose up
    }

    /**
     * Mathematical Roll slope (lateral tilt) in degrees along Y.
     */
    fun getGroundRoll(x: Float, y: Float): Float {
        val delta = 0.8f
        val z1 = getGroundHeight(x, y - delta)
        val z2 = getGroundHeight(x, y + delta)
        val rad = atan2(z2 - z1, delta * 2f)
        return rad * 180f / Math.PI.toFloat()
    }

    /**
     * Calculate 3D surface normal vector at (x, y) for directional sunlight calculations.
     */
    fun getGroundNormal(x: Float, y: Float): Vector3D {
        val delta = 0.6f
        val zL = getGroundHeight(x - delta, y)
        val zR = getGroundHeight(x + delta, y)
        val zD = getGroundHeight(x, y - delta)
        val zU = getGroundHeight(x, y + delta)

        val vX = Vector3D(delta * 2f, 0f, zR - zL)
        val vY = Vector3D(0f, delta * 2f, zU - zD)
        return vX.cross(vY).normalized()
    }

    /**
     * Primary 3D Physics Loop Tick (60 Hz).
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
        if (bannerTimer > 0f) {
            bannerTimer -= dt
            if (bannerTimer <= 0f) {
                currentTrickBanner = ""
            }
        }

        updateParticles(dt)

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
                velX *= 0.94f
                velY *= 0.90f
                posX += velX * dt
                posY += velY * dt
                posZ = getGroundHeight(posX, posY)
                pitchAngle = getGroundPitch(posX, posY)
                rollAngle = getGroundRoll(posX, posY)
                riderPose = RiderPose.FINISHED
            }
        }
    }

    private fun updateTitleDemo(dt: Float) {
        velX = 16.0f
        posX += velX * dt
        if (posX > finishX) {
            posX = startX
        }
        posY = sin(posX * 0.05f) * 4.5f
        posZ = getGroundHeight(posX, posY)
        pitchAngle = getGroundPitch(posX, posY)
        rollAngle = getGroundRoll(posX, posY) + sin(posX * 0.05f) * 8f
        yawAngle = cos(posX * 0.05f) * 6f
        riderPose = if ((System.currentTimeMillis() / 320) % 2 == 0L) RiderPose.PEDALING else RiderPose.COASTING
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
        timeRemaining -= dt
        if (timeRemaining <= 0f) {
            timeRemaining = 0f
            finishRun()
            return
        }

        if (posX >= finishX) {
            finishRun()
            return
        }

        val groundZ = getGroundHeight(posX, posY)
        val groundPitch = getGroundPitch(posX, posY)
        val groundRoll = getGroundRoll(posX, posY)
        val pitchRad = -groundPitch * Math.PI.toFloat() / 180f

        // 1. Obstacle Interaction (3D Bounding Box Check)
        var currentFriction = baseFriction
        for (obs in obstacles) {
            val halfW = obs.width / 2f
            val halfD = obs.depth / 2f
            val inObsX = posX in (obs.x - halfW)..(obs.x + halfW)
            val inObsY = posY in (obs.laneY - halfD)..(obs.laneY + halfD)

            if (inObsX && inObsY) {
                when (obs.type) {
                    ObstacleType.MUD_PUDDLE -> {
                        if (isOnGround) {
                            currentFriction = mudFriction
                            velX = max(velX - 16f * dt, 3f)
                            if (random.nextFloat() < 0.35f) {
                                audio.playMudSplash()
                                spawnMudParticles(posX, posY, groundZ)
                            }
                        }
                    }
                    ObstacleType.ROCK, ObstacleType.LOG, ObstacleType.CONE -> {
                        val obsTopZ = groundZ + obs.height
                        if (isOnGround && !obs.isCleared) {
                            triggerCrash("Direct ${obs.type.displayName} Crash!")
                            return
                        } else if (!isOnGround && posZ < obsTopZ && !obs.isCleared) {
                            triggerCrash("Clipped ${obs.type.displayName}!")
                            return
                        } else if (!isOnGround && posZ >= obsTopZ && !obs.isCleared) {
                            obs.isCleared = true
                            addScore(obs.type.pointsOnClear, "Clear! ${obs.type.displayName} +${obs.type.pointsOnClear}")
                            audio.playTrickSuccess("Hurdle")
                        }
                    }
                    ObstacleType.BIG_RAMP, ObstacleType.SPEED_BUMP -> {
                        // Ramp launch physics handled seamlessly by terrain curvature
                    }
                }
            }
        }

        // 2. Ground vs Air Physics
        if (isOnGround) {
            posZ = groundZ
            velZ = 0f
            airTime = 0f
            accumulatedPitchFlip = 0f
            accumulatedYawSpin = 0f

            // A. Forward Pedaling & Acceleration
            if (isPedalPressed || isTrickUp) {
                val speedRatio = (velX / maxSpeed).coerceIn(0f, 1f)
                val effectiveAccel = pedalAccel * (1f - speedRatio * 0.4f)
                velX = min(velX + effectiveAccel * dt, maxSpeed)
                riderPose = RiderPose.PEDALING
                if (random.nextFloat() < 0.25f) {
                    audio.playPedalTick()
                    spawnDirtParticles(posX, posY, posZ)
                }
            } else if (isTrickDown) {
                // Braking
                velX = max(0f, velX - 22f * dt)
                riderPose = RiderPose.COASTING
            } else {
                velX *= currentFriction.pow(dt * 60f)
                riderPose = RiderPose.COASTING
            }

            // B. Downhill Slope Gravitational Acceleration
            val slopeGravityAccel = sin(pitchRad) * gravity * 0.5f
            velX += slopeGravityAccel * dt
            velX = velX.coerceIn(0f, maxSpeed * 1.2f)

            // C. Lateral Steering (Lane Changing Y)
            var targetRoll = groundRoll
            if (isLeanBack) {
                // Steer Left (towards negative Y)
                velY = max(velY - steerAccel * dt, -maxSteerSpeed)
                targetRoll -= 18f // Lean bike into turn
                yawAngle = -12f
            } else if (isLeanForward) {
                // Steer Right (towards positive Y)
                velY = min(velY + steerAccel * dt, maxSteerSpeed)
                targetRoll += 18f
                yawAngle = 12f
            } else {
                velY *= 0.85f // Self-centering lateral drag
                yawAngle *= 0.85f
            }

            // Smooth roll transition
            rollAngle += (targetRoll - rollAngle) * 12f * dt

            // Track boundaries containment
            posY += velY * dt
            if (posY < -trackHalfWidth) {
                posY = -trackHalfWidth
                velY = 0f
            } else if (posY > trackHalfWidth) {
                posY = trackHalfWidth
                velY = 0f
            }

            // D. Ground Stunt: Wheelie
            if (isLeanBack && velX > 4f && !isLeanForward && isPedalPressed) {
                riderPose = RiderPose.WHEELIE
                pitchAngle = groundPitch + 32f
                wheelieDistance += velX * dt
                isWheelieActive = true
                if (random.nextFloat() < 0.15f) {
                    addScore(3, "Wheelie!", false)
                }
            } else {
                isWheelieActive = false
                pitchAngle = groundPitch
            }

            // E. Jump / Bunnyhop Pop
            if (isJumpPressed) {
                isOnGround = false
                val jumpPop = 16.5f + (velX * 0.18f)
                velZ = jumpPop
                pitchAngle = groundPitch + 12f
                accumulatedPitchFlip = 0f
                accumulatedYawSpin = 0f
                airTricksExecuted.clear()
                riderPose = RiderPose.BUNNY_HOP
                audio.playJump()
                spawnDirtParticles(posX, posY, posZ)
            }

            // F. Crest Launch Detection (Launching off hill peaks)
            val lookAheadX = posX + velX * dt * 1.5f
            val lookAheadGroundZ = getGroundHeight(lookAheadX, posY)
            val projectedZ = posZ + (velX * dt * sin(pitchRad))
            if (lookAheadGroundZ < projectedZ - 1.2f && velX > 8.0f && pitchRad > 0f) {
                isOnGround = false
                velZ = velX * sin(pitchRad) * 0.9f + 3.0f
                accumulatedPitchFlip = 0f
                accumulatedYawSpin = 0f
                airTricksExecuted.clear()
                riderPose = RiderPose.IN_AIR
                audio.playJump()
            }

            posX += velX * dt

        } else {
            // 3. 3D Aerial Flight Physics
            airTime += dt

            // Integrate 3D Velocity & Gravity
            velZ -= gravity * dt
            velX *= airDrag.pow(dt * 60f)
            velY *= airDrag.pow(dt * 60f)

            posX += velX * dt
            posY += velY * dt
            posZ += velZ * dt

            // Lateral boundary containment in air
            posY = posY.coerceIn(-trackHalfWidth, trackHalfWidth)

            // 3D Angular Stunts & Rotational Torques
            var pitchTorque = 0f
            var rollTorque = 0f
            var yawTorque = 0f

            if (isLeanBack) {
                // Backflip (Pitch counter-clockwise)
                pitchTorque = 360f
                riderPose = RiderPose.BACKFLIP
            } else if (isLeanForward) {
                // Frontflip (Pitch clockwise)
                pitchTorque = -360f
                riderPose = RiderPose.FRONTFLIP
            } else if (isTrickUp) {
                // Tabletop (Roll tweak)
                riderPose = RiderPose.TABLETOP
                rollAngle = 72f
                checkAirTrick("Tabletop", 300)
            } else if (isTrickDown) {
                // Superman (Legs extend horizontally backward)
                riderPose = RiderPose.SUPERMAN
                checkAirTrick("Superman", 450)
            } else if (isJumpPressed) {
                // 360 Tailwhip (Yaw spin)
                riderPose = RiderPose.TAILWHIP
                yawTorque = 420f
                checkAirTrick("Tailwhip", 350)
            } else {
                riderPose = RiderPose.IN_AIR
            }

            // Angular Integrations
            pitchAngle += pitchTorque * dt
            yawAngle += yawTorque * dt
            accumulatedPitchFlip += pitchTorque * dt
            accumulatedYawSpin += yawTorque * dt

            // Completed 360 & 720 rotation checks
            if (abs(accumulatedPitchFlip) >= 330f && abs(accumulatedPitchFlip) < 660f) {
                if (accumulatedPitchFlip > 0) checkAirTrick("Backflip!", 600)
                else checkAirTrick("Frontflip!", 750)
            } else if (abs(accumulatedPitchFlip) >= 660f) {
                if (accumulatedPitchFlip > 0) checkAirTrick("Double Backflip!", 1400)
                else checkAirTrick("Double Frontflip!", 1800)
            }

            if (abs(accumulatedYawSpin) >= 330f) {
                checkAirTrick("360 Helicopter Spin!", 850)
            }

            // 4. 3D Ground Landing & Impact Collision
            if (posZ <= groundZ && velZ < 0f) {
                posZ = groundZ
                val landingPitch = getGroundPitch(posX, posY)
                val landingRoll = getGroundRoll(posX, posY)

                val diffPitch = normalizeAngle(pitchAngle - landingPitch)
                val diffRoll = normalizeAngle(rollAngle - landingRoll)
                val diffYaw = normalizeAngle(yawAngle)

                // Safe Landing Criteria:
                // Pitch and Roll within safe tolerances, and bike not facing sideways on touchdown
                val isUpright = abs(diffPitch) <= landingToleranceDeg &&
                                abs(diffRoll) <= landingToleranceDeg + 15f &&
                                abs(diffYaw) <= 45f

                if (isUpright) {
                    // Clean Safe Landing!
                    isOnGround = true
                    pitchAngle = landingPitch
                    rollAngle = landingRoll
                    yawAngle = 0f
                    velZ = 0f
                    riderPose = RiderPose.COASTING

                    // Downslope pump acceleration
                    if (landingPitch < -4f) {
                        val pumpBoost = sin(-landingPitch * Math.PI.toFloat() / 180f) * 5.5f
                        velX = min(velX + pumpBoost, maxSpeed * 1.15f)
                    }

                    // Combo Multiplier
                    if (airTricksExecuted.isNotEmpty()) {
                        comboMultiplier++
                        val baseSum = airTricksExecuted.sumOf { it.points }
                        val comboTotal = baseSum * comboMultiplier
                        addScore(comboTotal, "Stunt Combo x$comboMultiplier! +$comboTotal")
                        audio.playTrickSuccess("Combo")
                    }

                    spawnLandingDust(posX, posY, groundZ)
                    airTricksExecuted.clear()
                    accumulatedPitchFlip = 0f
                    accumulatedYawSpin = 0f

                } else {
                    // Bad Angle / Upside Down Wipeout
                    val crashReason = when {
                        abs(diffPitch) > 90f -> "Inverted Wipeout!"
                        abs(diffYaw) > 45f -> "Crossed Landing Crash!"
                        else -> "Rough Angle Wipeout!"
                    }
                    triggerCrash(crashReason)
                }
            }
        }
    }

    private fun checkAirTrick(name: String, points: Int) {
        if (airTricksExecuted.none { it.trickName == name }) {
            airTricksExecuted.add(TrickScore(name, points))
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

        // Decouple Bike and Rider into 3D tumbling rigid bodies
        crashBikePos.set(posX, posY, posZ)
        crashBikeVel.set(velX * 0.65f + 4f, (random.nextFloat() - 0.5f) * 6f, 8f)
        crashBikeRot.set(pitchAngle, rollAngle, yawAngle)

        crashRiderPos.set(posX + 2f, posY + (random.nextFloat() - 0.5f) * 2f, posZ + 1f)
        crashRiderVel.set(velX * 0.8f + 6f, (random.nextFloat() - 0.5f) * 8f, 11f)
        crashRiderRot.set(0f, 0f, 0f)

        velX = 0f
        velY = 0f
        velZ = 0f
        airTricksExecuted.clear()
        comboMultiplier = 1
        showTrickBanner("WIPEOUT! $reason")
        spawnCrashDebris(posX, posY, posZ)
    }

    private fun updateWipeout(dt: Float) {
        wipeoutTimer -= dt

        // Bike 3D Tumbling
        crashBikeVel.z -= gravity * 1.2f * dt
        crashBikePos.x += crashBikeVel.x * dt
        crashBikePos.y += crashBikeVel.y * dt
        crashBikePos.z += crashBikeVel.z * dt
        crashBikeRot.x += 380f * dt
        crashBikeRot.y += 260f * dt

        val bikeGz = getGroundHeight(crashBikePos.x, crashBikePos.y)
        if (crashBikePos.z <= bikeGz) {
            crashBikePos.z = bikeGz
            crashBikeVel.z = -crashBikeVel.z * 0.35f
            crashBikeVel.x *= 0.84f
            crashBikeVel.y *= 0.84f
        }

        // Rider 3D Tumbling
        crashRiderVel.z -= gravity * 1.2f * dt
        crashRiderPos.x += crashRiderVel.x * dt
        crashRiderPos.y += crashRiderVel.y * dt
        crashRiderPos.z += crashRiderVel.z * dt
        crashRiderRot.x += 460f * dt
        crashRiderRot.z += 320f * dt

        val riderGz = getGroundHeight(crashRiderPos.x, crashRiderPos.y)
        if (crashRiderPos.z <= riderGz) {
            crashRiderPos.z = riderGz
            crashRiderVel.z = -crashRiderVel.z * 0.25f
            crashRiderVel.x *= 0.80f
            crashRiderVel.y *= 0.80f
        }

        if (wipeoutTimer <= 0f) {
            // Recovery
            posX = max(crashRiderPos.x, crashBikePos.x) + 3f
            posY = crashRiderPos.y.coerceIn(-trackHalfWidth + 1f, trackHalfWidth - 1f)
            posZ = getGroundHeight(posX, posY)
            pitchAngle = getGroundPitch(posX, posY)
            rollAngle = getGroundRoll(posX, posY)
            yawAngle = 0f
            velX = 5.0f
            velY = 0f
            velZ = 0f
            isOnGround = true
            riderPose = RiderPose.COASTING
            gamePhase = GamePhase.PLAYING
        }
    }

    private fun finishRun() {
        gamePhase = GamePhase.FINISHED
        val timeBonus = (timeRemaining * 50f).toInt()
        val cleanRunBonus = max(0, 2000 - (wipeoutCount * 450))
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
        val iter = particles.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.z += p.vz * dt
            p.vz -= gravity * 0.45f * dt
            p.life -= dt
            if (p.life <= 0f) {
                iter.remove()
            }
        }
    }

    private fun spawnDirtParticles(x: Float, y: Float, z: Float) {
        for (i in 0 until 3) {
            particles.add(
                DustParticle(
                    x = x - random.nextFloat() * 4f,
                    y = y + (random.nextFloat() - 0.5f) * 1.5f,
                    z = z + random.nextFloat() * 1.5f,
                    vx = -velX * 0.35f - random.nextFloat() * 5f,
                    vy = (random.nextFloat() - 0.5f) * 4f,
                    vz = random.nextFloat() * 6f + 2f,
                    life = 0.4f,
                    maxLife = 0.4f,
                    color = 0xFF8D6E63,
                    size = 4f + random.nextFloat() * 3f
                )
            )
        }
    }

    private fun spawnMudParticles(x: Float, y: Float, z: Float) {
        for (i in 0 until 6) {
            particles.add(
                DustParticle(
                    x = x + (random.nextFloat() - 0.5f) * 4f,
                    y = y + (random.nextFloat() - 0.5f) * 4f,
                    z = z + 0.5f,
                    vx = -velX * 0.45f - random.nextFloat() * 6f,
                    vy = (random.nextFloat() - 0.5f) * 10f,
                    vz = random.nextFloat() * 14f + 4f,
                    life = 0.55f,
                    maxLife = 0.55f,
                    color = 0xFF3E2723,
                    size = 5f + random.nextFloat() * 4f
                )
            )
        }
    }

    private fun spawnLandingDust(x: Float, y: Float, z: Float) {
        for (i in 0 until 14) {
            val ang = (i / 14f) * (Math.PI.toFloat() * 2f)
            val spd = random.nextFloat() * 9f + 4f
            particles.add(
                DustParticle(
                    x = x + cos(ang) * 1.5f,
                    y = y + sin(ang) * 1.5f,
                    z = z + 0.5f,
                    vx = cos(ang) * spd,
                    vy = sin(ang) * spd,
                    vz = random.nextFloat() * 7f + 2f,
                    life = 0.6f,
                    maxLife = 0.6f,
                    color = 0xFFD7CCC8,
                    size = 5f + random.nextFloat() * 4f
                )
            )
        }
    }

    private fun spawnCrashDebris(x: Float, y: Float, z: Float) {
        for (i in 0 until 24) {
            val ang = random.nextFloat() * Math.PI.toFloat() * 2f
            val spd = random.nextFloat() * 16f + 5f
            particles.add(
                DustParticle(
                    x = x,
                    y = y,
                    z = z + 1.5f,
                    vx = cos(ang) * spd,
                    vy = sin(ang) * spd,
                    vz = random.nextFloat() * 18f + 4f,
                    life = 0.9f,
                    maxLife = 0.9f,
                    color = if (i % 3 == 0) 0xFFFF5722 else if (i % 3 == 1) 0xFFFFEB3B else 0xFF8D6E63,
                    size = 4f + random.nextFloat() * 5f
                )
            )
        }
    }
}
