package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quests")
data class Quest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    
    // Types:
    // "POSITIVE" (Standard habit, rewards Gold, EXP, and Attribute points)
    // "NEGATIVE" (Bad habit, triggers immediately on tap - reduces HP and can inflict debuffs)
    // "DAILY" (Positive mandatory daily quest - rewards high Gold/EXP. Restoring or failing them daily dictates health)
    val type: String,
    
    val difficulty: String, // "EASY" (rewards ~10), "MEDIUM" (rewards ~20), "HARD" (rewards ~40)
    val statType: String,   // "STR" (Strength), "INT" (Intellect), "AGI" (Agility), "CON" (Constitution)
    
    val completedCount: Int = 0,
    val failCount: Int = 0,
    val lastTickedTimestamp: Long = 0L // Last time this quest was activated or checked off
) {
    // Check if the quest has been marked completed / triggered today
    fun isCompletedToday(): Boolean {
        if (lastTickedTimestamp == 0L) return false
        val lastTickedCal = java.util.Calendar.getInstance().apply { timeInMillis = lastTickedTimestamp }
        val nowCal = java.util.Calendar.getInstance()
        return lastTickedCal.get(java.util.Calendar.YEAR) == nowCal.get(java.util.Calendar.YEAR) &&
               lastTickedCal.get(java.util.Calendar.DAY_OF_YEAR) == nowCal.get(java.util.Calendar.DAY_OF_YEAR)
    }
}
