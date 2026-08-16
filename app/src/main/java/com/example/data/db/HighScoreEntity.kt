package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "high_scores")
data class HighScoreEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val playerName: String,
    val score: Int,
    val bestTrick: String,
    val timeRemainingSeconds: Int,
    val tricksCount: Int,
    val wipeouts: Int,
    val timestamp: Long = System.currentTimeMillis()
)
