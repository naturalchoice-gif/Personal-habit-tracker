package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestDao {
    // Character Stats queries
    @Query("SELECT * FROM character_stats WHERE id = 1")
    fun getCharacter(): Flow<UserCharacter?>

    @Query("SELECT * FROM character_stats WHERE id = 1")
    suspend fun getCharacterDirect(): UserCharacter?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacter(character: UserCharacter)

    @Update
    suspend fun updateCharacter(character: UserCharacter)

    // Quests queries
    @Query("SELECT * FROM quests ORDER BY id DESC")
    fun getAllQuests(): Flow<List<Quest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuest(quest: Quest)

    @Update
    suspend fun updateQuest(quest: Quest)

    @Query("DELETE FROM quests WHERE id = :id")
    suspend fun deleteQuest(id: Int)

    // Badges queries
    @Query("SELECT * FROM badges ORDER BY isUnlocked DESC, id ASC")
    fun getAllBadges(): Flow<List<Badge>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBadges(badges: List<Badge>)

    @Update
    suspend fun updateBadge(badge: Badge)

    // Logs queries
    @Query("SELECT * FROM game_logs ORDER BY timestamp DESC LIMIT 40")
    fun getGameLogs(): Flow<List<GameLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: GameLog)

    @Query("DELETE FROM game_logs")
    suspend fun clearLogs()
}
