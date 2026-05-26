package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "badges")
data class Badge(
    @PrimaryKey val id: String, // Unique ID (e.g., "LVL_5", "QUEST_15", "STAT_INT", "RICH_POCKETS")
    val title: String,
    val description: String,
    val iconName: String, // Icon ID reference (e.g., "star", "shield", "fitness", "gold")
    val unlockCriteria: String,
    val isUnlocked: Boolean = false,
    val unlockedAt: Long = 0L
)
