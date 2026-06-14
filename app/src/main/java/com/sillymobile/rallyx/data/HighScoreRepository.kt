package com.sillymobile.rallyx.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class HighScore(val score: Int, val level: Int, val dateMillis: Long)

/** Stores the local top-10 high score table in [android.content.SharedPreferences] as JSON. */
class HighScoreRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getScores(): List<HighScore> {
        val raw = prefs.getString(KEY_SCORES, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                HighScore(
                    score = obj.getInt("score"),
                    level = obj.getInt("level"),
                    dateMillis = obj.getLong("date"),
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Inserts [score]/[level], keeps only the top [MAX_ENTRIES], and returns the updated table. */
    fun addScore(score: Int, level: Int): List<HighScore> {
        val updated = (getScores() + HighScore(score, level, System.currentTimeMillis()))
            .sortedByDescending { it.score }
            .take(MAX_ENTRIES)

        val array = JSONArray()
        for (entry in updated) {
            array.put(
                JSONObject().apply {
                    put("score", entry.score)
                    put("level", entry.level)
                    put("date", entry.dateMillis)
                },
            )
        }
        prefs.edit().putString(KEY_SCORES, array.toString()).apply()
        return updated
    }

    fun isHighScore(score: Int): Boolean {
        val scores = getScores()
        return scores.size < MAX_ENTRIES || score > (scores.minOfOrNull { it.score } ?: 0)
    }

    companion object {
        private const val PREFS_NAME = "rallyx_highscores"
        private const val KEY_SCORES = "scores"
        const val MAX_ENTRIES = 10
    }
}
