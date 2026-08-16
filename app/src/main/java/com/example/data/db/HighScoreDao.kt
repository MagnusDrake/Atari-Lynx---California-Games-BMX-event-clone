package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HighScoreDao {
    @Query("SELECT * FROM high_scores ORDER BY score DESC LIMIT 20")
    fun getTopScores(): Flow<List<HighScoreEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScore(score: HighScoreEntity): Long

    @Query("DELETE FROM high_scores")
    suspend fun clearScores()
}
