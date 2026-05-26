package com.example.data

import kotlinx.coroutines.flow.Flow

class QuestRepository(private val questDao: QuestDao) {
    val character: Flow<UserCharacter?> = questDao.getCharacter()
    val allQuests: Flow<List<Quest>> = questDao.getAllQuests()
    val allBadges: Flow<List<Badge>> = questDao.getAllBadges()
    val gameLogs: Flow<List<GameLog>> = questDao.getGameLogs()

    suspend fun getCharacterDirect(): UserCharacter? = questDao.getCharacterDirect()

    suspend fun insertCharacter(character: UserCharacter) {
        questDao.insertCharacter(character)
    }

    suspend fun updateCharacter(character: UserCharacter) {
        questDao.updateCharacter(character)
    }

    suspend fun insertQuest(quest: Quest) = questDao.insertQuest(quest)
    suspend fun updateQuest(quest: Quest) = questDao.updateQuest(quest)
    suspend fun deleteQuest(id: Int) = questDao.deleteQuest(id)

    suspend fun insertBadges(badges: List<Badge>) = questDao.insertBadges(badges)
    suspend fun updateBadge(badge: Badge) = questDao.updateBadge(badge)

    suspend fun insertLog(log: GameLog) = questDao.insertLog(log)
    suspend fun clearLogs() = questDao.clearLogs()
}
