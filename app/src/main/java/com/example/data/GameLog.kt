package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_logs")
data class GameLog(
    val message: String,
    val logType: String, // "EXP", "GOLD", "DAMAGE", "LEVEL_UP", "BADGE", "DEBUFF_ON", "DEBUFF_OFF", "HEAL", "QUEST_ADD"
    val timestamp: Long = System.currentTimeMillis(),
    @PrimaryKey(autoGenerate = true) val id: Int = 0
)
