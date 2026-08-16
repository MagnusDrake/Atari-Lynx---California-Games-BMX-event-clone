package com.example.data.db

import kotlinx.coroutines.flow.Flow

class HighScoreRepository(private val dao: HighScoreDao) {
    val topScores: Flow<List<HighScoreEntity>> = dao.getTopScores()

    suspend fun saveScore(score: HighScoreEntity): Long {
        return dao.insertScore(score)
    }

    suspend fun clearAll() {
        dao.clearScores()
    }
}
