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
    ROCK("Bolder Rock", 150),
    LOG("Drift Log", 150),
    CONE("Safety Cone", 100),
    BIG_RAMP("Mega Dirt Ramp", 200),
    SPEED_BUMP("Moguls", 100)
}

data class Obstacle(
    val id: Int,
    val x: Float,
    val width: Float,
    val height: Float,
    val type: ObstacleType,
    var isCleared: Boolean = false
)

data class DustParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
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
    val helmet: Long
)

val RETRO_PALETTES = listOf(
    ColorTheme("Elegant Dark", 0xFFD0BCFF, 0xFF4A4458, 0xFFE8DEF8, 0xFFE6E1E9),
    ColorTheme("California Classic", 0xFFFFD600, 0xFF00E5FF, 0xFFFF3D00, 0xFFFFFFFF),
    ColorTheme("Neon Sunset", 0xFFFF007F, 0xFF76FF03, 0xFFFF9100, 0xFF00E5FF),
    ColorTheme("Lynx Cyber", 0xFF00E5FF, 0xFFD500F9, 0xFFFFFFFF, 0xFFFFEB3B),
    ColorTheme("Radical Stealth", 0xFF212121, 0xFFFF1744, 0xFFB0BEC5, 0xFFFFC400)
)
