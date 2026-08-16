package com.example.model

enum class RiderPose {
    PEDALING,
    COASTING,
    WHEELIE,
    BUNNY_HOP,
    IN_AIR,
    BACKFLIP,
    FRONTFLIP,
    SPIN_360,
    TABLETOP,
    SUPERMAN,
    TAILWHIP,
    CRASHED,
    FINISHED
}

enum class ObstacleType(val displayName: String, val pointsOnClear: Int) {
    MUD_PUDDLE("Mud Trap", 100),
    ROCK("Boulder Rock", 150),
    LOG("Drift Log", 150),
    CONE("Safety Cone", 100),
    BIG_RAMP("Mega Dirt Ramp", 200),
    SPEED_BUMP("Moguls", 100)
}

data class Obstacle(
    val id: Int,
    val x: Float,          // Track progression (meters)
    val laneY: Float = 0f, // Lateral track lane (-8f to +8f)
    val width: Float,      // X length
    val depth: Float = 6f, // Y depth across track
    val height: Float,     // Z height
    val type: ObstacleType,
    var isCleared: Boolean = false
)

data class DustParticle(
    var x: Float,
    var y: Float,
    var z: Float,
    var vx: Float,
    var vy: Float,
    var vz: Float,
    var life: Float,
    val maxLife: Float,
    val color: Long,
    val size: Float
)

data class TrickScore(
    val trickName: String,
    val points: Int,
    val isCombo: Boolean = false
)

enum class GamePhase {
    TITLE,
    COUNTDOWN,
    PLAYING,
    WIPEOUT_RECOVERY,
    FINISHED,
    LEADERBOARD,
    TRICK_GUIDE,
    SETTINGS
}

data class ColorTheme(
    val name: String,
    val riderJersey: Long, // 0xAARRGGBB
    val riderPants: Long,
    val bikeFrame: Long,
    val helmet: Long,
    val groundTop: Long = 0xFFD7CCC8,
    val groundSide: Long = 0xFF8D6E63,
    val grassAccent: Long = 0xFF66BB6A
)

val RETRO_PALETTES = listOf(
    ColorTheme(
        name = "Elegant Dark",
        riderJersey = 0xFFD0BCFF,
        riderPants = 0xFF4A4458,
        bikeFrame = 0xFFE8DEF8,
        helmet = 0xFFE6E1E9,
        groundTop = 0xFF36343B,
        groundSide = 0xFF211F26,
        grassAccent = 0xFFD0BCFF
    ),
    ColorTheme(
        name = "California Sunset",
        riderJersey = 0xFFFFD600,
        riderPants = 0xFF00E5FF,
        bikeFrame = 0xFFFF3D00,
        helmet = 0xFFFFFFFF,
        groundTop = 0xFFD7CCC8,
        groundSide = 0xFF8D6E63,
        grassAccent = 0xFF66BB6A
    ),
    ColorTheme(
        name = "Neon Cyber 80s",
        riderJersey = 0xFFFF007F,
        riderPants = 0xFF76FF03,
        bikeFrame = 0xFFFF9100,
        helmet = 0xFF00E5FF,
        groundTop = 0xFF1A1A2E,
        groundSide = 0xFF16213E,
        grassAccent = 0xFF00FFCC
    ),
    ColorTheme(
        name = "Lynx Matrix",
        riderJersey = 0xFF00E5FF,
        riderPants = 0xFFD500F9,
        bikeFrame = 0xFFFFFFFF,
        helmet = 0xFFFFEB3B,
        groundTop = 0xFF1E293B,
        groundSide = 0xFF0F172A,
        grassAccent = 0xFF38BDF8
    ),
    ColorTheme(
        name = "Radical Stealth",
        riderJersey = 0xFF212121,
        riderPants = 0xFFFF1744,
        bikeFrame = 0xFFB0BEC5,
        helmet = 0xFFFFC400,
        groundTop = 0xFF263238,
        groundSide = 0xFF1A237E,
        grassAccent = 0xFFFF5252
    )
)
