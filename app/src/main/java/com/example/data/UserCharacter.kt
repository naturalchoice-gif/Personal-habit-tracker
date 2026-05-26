package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "character_stats")
data class UserCharacter(
    @PrimaryKey val id: Int = 1, // Global single character
    val name: String = "Hero",
    val characterClass: String = "Scholar", // Warrior, Mage, Rogue, Scholar
    val level: Int = 1,
    val exp: Int = 0,
    val maxExp: Int = 100,
    val hp: Int = 100,
    val maxHp: Int = 100,
    val gold: Int = 30, // Starting gold
    
    // Core Attributes
    val strength: Int = 10,
    val intellect: Int = 10,
    val agility: Int = 10,
    val constitution: Int = 10,
    
    // Streaks & Stats
    val streakDays: Int = 0,
    val lastActiveDayTimestamp: Long = 0L,
    val totalQuestsCompleted: Int = 0,
    
    // Active Debuffs (Toggle states which affect final computed attributes)
    val isSlothActive: Boolean = false,       // Sluggish: -2 Agility (from missing exercise or sleeping in)
    val isBrainFogActive: Boolean = false,    // Confused: -2 Intellect (from screen-time / distraction habits)
    val isSnackSackerActive: Boolean = false, // Decay: -2 Constitution (from poor diet/junk food)
    val isGlassSwordActive: Boolean = false   // Weakened: -2 Strength (from skipping fitness quests)
)
