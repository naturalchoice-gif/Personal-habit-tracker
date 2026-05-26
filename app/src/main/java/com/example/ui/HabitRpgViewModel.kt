package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class HabitRpgViewModel(application: Application) : AndroidViewModel(application) {
    private val questDao = DatabaseProvider.getDatabase(application).questDao()
    private val repository = QuestRepository(questDao)

    // State flows representing actual DB bindings
    val character: StateFlow<UserCharacter?> = repository.character.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val quests: StateFlow<List<Quest>> = repository.allQuests.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val badges: StateFlow<List<Badge>> = repository.allBadges.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val gameLogs: StateFlow<List<GameLog>> = repository.gameLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        // Initialize character and predefined badges if empty
        viewModelScope.launch {
            repository.allBadges.first().let { currentBadges ->
                if (currentBadges.isEmpty()) {
                    val defaultBadges = listOf(
                        Badge("LVL_5", "Seasoned Adventurer", "Reach level 5", "star", "Level 5"),
                        Badge("LVL_10", "Ascended Legend", "Reach level 10", "diamond", "Level 10"),
                        Badge("QUEST_10", "Dedicated Squire", "Complete 10 habits/quests", "assignment", "10 Completions"),
                        Badge("QUEST_50", "Grandmaster of Order", "Complete 50 habits/quests", "emoji_events", "50 Completions"),
                        Badge("GOLD_100", "Flamboyant Merchant", "Accumulate 100 Gold", "payments", "100 Gold"),
                        Badge("STAT_25", "Specialist Pioneer", "Get any attribute (STR, INT, etc.) to 25+", "fitness_center", "Attribute 25+"),
                        Badge("STREAK_7", "Consistency Sentinel", "Achieve a 7-day login streak", "bolt", "7-Day Streak"),
                        Badge("DEBUFF_SURVIVOR", "Phoenix Reborn", "Successfully heal critical health", "history_edu", "Heal from < 20 HP")
                    )
                    repository.insertBadges(defaultBadges)
                }
            }

            val currentChar = repository.getCharacterDirect()
            if (currentChar == null) {
                // Insert a default starting character
                val newChar = UserCharacter(
                    name = "Scholar Dev",
                    characterClass = "Scholar",
                    lastActiveDayTimestamp = System.currentTimeMillis()
                )
                repository.insertCharacter(newChar)
                repository.insertLog(GameLog("Your RPG Self-Journey Begins! Choose your quests and grow daily.", "LEVEL_UP"))
            } else {
                // Check daily reset transition
                checkDailyResetAndStreaks(currentChar)
            }
        }
    }

    // Process daily transitions, reset daily checklist, and apply penalties for missed chores
    private suspend fun checkDailyResetAndStreaks(char: UserCharacter) {
        val now = System.currentTimeMillis()
        val lastActiveCal = Calendar.getInstance().apply { timeInMillis = char.lastActiveDayTimestamp }
        val nowCal = Calendar.getInstance().apply { timeInMillis = now }

        val isSameDay = lastActiveCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                lastActiveCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)

        if (!isSameDay) {
            // Check day difference for streak calculation
            val yesterdayCal = Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.DAY_OF_YEAR, -1)
            }
            val isYesterday = lastActiveCal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
                    lastActiveCal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR)

            val newStreak = if (isYesterday) char.streakDays + 1 else 1

            // Count incomplete "DAILY" quests from yesterday & apply damage
            val activeQuests = repository.allQuests.first()
            val incompleteDailies = activeQuests.filter { it.type == "DAILY" && !it.isCompletedToday() }
            
            var totalDamage = 0
            if (incompleteDailies.isNotEmpty()) {
                totalDamage = incompleteDailies.size * 12 // 12 damage per missed daily Quest
                repository.insertLog(
                    GameLog("Missed ${incompleteDailies.size} Daily Quests yesterday! Suffered $totalDamage damage overnight.", "DAMAGE")
                )
            }

            // Apply health deduction & update character attributes
            val revisedHp = kotlin.math.max(0, char.hp - totalDamage)
            var updatedChar = char.copy(
                streakDays = newStreak,
                lastActiveDayTimestamp = now,
                hp = revisedHp
            )

            if (revisedHp <= 0) {
                updatedChar = resurrectCharacter(updatedChar)
            }

            repository.updateCharacter(updatedChar)

            // Dynamic badges checks
            checkBadges(updatedChar)

            // Trigger debuff infections optionally if they had multiple consecutive failed dailies
            if (incompleteDailies.isNotEmpty()) {
                // Infect based on missed categories
                infectCorrespondingDebuff(incompleteDailies.first().statType)
            }
        }
    }

    // Inflict visual debuffs depending on type
    private suspend fun infectCorrespondingDebuff(statRef: String) {
        val char = repository.getCharacterDirect() ?: return
        val updated = when (statRef) {
            "STR" -> {
                if (!char.isGlassSwordActive) {
                    repository.insertLog(GameLog("Physical inactivity Weakens you! Infected with 'Glass Sword' (-2 STR) debuff.", "DEBUFF_ON"))
                    char.copy(isGlassSwordActive = true)
                } else char
            }
            "INT" -> {
                if (!char.isBrainFogActive) {
                    repository.insertLog(GameLog("Uncompleted intellectual quests! Infected with 'Brain Fog' (-2 INT) debuff.", "DEBUFF_ON"))
                    char.copy(isBrainFogActive = true)
                } else char
            }
            "AGI" -> {
                if (!char.isSlothActive) {
                    repository.insertLog(GameLog("Procrastination makes you tardy! Infected with 'Sluggish' (-2 AGI) debuff.", "DEBUFF_ON"))
                    char.copy(isSlothActive = true)
                } else char
            }
            "CON" -> {
                if (!char.isSnackSackerActive) {
                    repository.insertLog(GameLog("Neglecting healthy routines! Infected with 'Snack Sacker' (-2 CON) debuff.", "DEBUFF_ON"))
                    char.copy(isSnackSackerActive = true)
                } else char
            }
            else -> char
        }
        repository.updateCharacter(updated)
    }

    // Setup / Reset name or class
    fun setupCharacter(name: String, selectedClass: String) {
        viewModelScope.launch {
            val char = repository.getCharacterDirect() ?: return@launch
            // Define starting stats depending on selected class
            val updated = when (selectedClass) {
                "Warrior" -> char.copy(name = name, characterClass = selectedClass, strength = 14, constitution = 12, intellect = 8, agility = 10)
                "Mage" -> char.copy(name = name, characterClass = selectedClass, strength = 8, constitution = 9, intellect = 14, agility = 11)
                "Rogue" -> char.copy(name = name, characterClass = selectedClass, strength = 10, constitution = 10, intellect = 10, agility = 14)
                "Scholar" -> char.copy(name = name, characterClass = selectedClass, strength = 9, constitution = 11, intellect = 13, agility = 9)
                else -> char.copy(name = name, characterClass = selectedClass)
            }
            repository.updateCharacter(updated)
            repository.insertLog(GameLog("Class Chosen: Welcome, $name the $selectedClass!", "LEVEL_UP"))
        }
    }

    // Add a brand new Habit/Quest
    fun addQuest(title: String, description: String, type: String, difficulty: String, statType: String) {
        viewModelScope.launch {
            val quest = Quest(
                title = title,
                description = description,
                type = type,
                difficulty = difficulty,
                statType = statType
            )
            repository.insertQuest(quest)
            repository.insertLog(GameLog("New Quest Added: $title (${difficulty})", "QUEST_ADD"))
        }
    }

    // Delete a habit/quest
    fun deleteQuest(id: Int, title: String) {
        viewModelScope.launch {
            repository.deleteQuest(id)
            repository.insertLog(GameLog("Discarded Quest: $title", "DAMAGE"))
        }
    }

    // Finish a positive Daily Quest / Habit
    fun completeQuest(questId: Int) {
        viewModelScope.launch {
            val originalQuests = repository.allQuests.first()
            val quest = originalQuests.find { it.id == questId } ?: return@launch
            val char = repository.getCharacterDirect() ?: return@launch

            // Check if already completed today to avoid duplicate clicking exploits for dailies
            if (quest.type == "DAILY" && quest.isCompletedToday()) {
                return@launch
            }

            // EXP/Gold reward ratios
            val difficultyMod = when (quest.difficulty) {
                "EASY" -> Pair(15, 5)
                "MEDIUM" -> Pair(30, 12)
                "HARD" -> Pair(50, 25)
                else -> Pair(15, 5)
            }
            val earnedExp = difficultyMod.first
            val earnedGold = difficultyMod.second

            // Exp level-up check
            var newLevel = char.level
            var newExp = char.exp + earnedExp
            var maxExp = char.maxExp
            var currentHp = char.hp
            var maxHp = char.maxHp
            
            // Stats increments
            var mStr = char.strength
            var mInt = char.intellect
            var mAgi = char.agility
            var mCon = char.constitution

            var levelUpTriggered = false
            while (newExp >= maxExp) {
                levelUpTriggered = true
                newLevel++
                newExp -= maxExp
                maxExp += 55 // Leveling up gets tougher

                // Boost stats depending on character class specialization
                when (char.characterClass) {
                    "Warrior" -> { mStr += 3; mCon += 2; mAgi += 1 }
                    "Mage" -> { mInt += 3; mAgi += 2; mStr += 1 }
                    "Rogue" -> { mAgi += 3; mStr += 1; mInt += 1; mCon += 1 }
                    "Scholar" -> { mInt += 3; mCon += 2; mStr += 1 }
                    else -> { mStr++; mInt++; mAgi++; mCon++ }
                }

                // Fully heal on level up
                maxHp += 8
                currentHp = maxHp
            }

            if (levelUpTriggered) {
                repository.insertLog(GameLog("LEVEL UP! Reached Level $newLevel! Health restored & specialization buffed.", "LEVEL_UP"))
            }

            // Attribute chance increase (50% chance to gain +1 in assigned Stat category of the Quest)
            val statGainRoll = (1..100).random()
            var optionalStatMsg = ""
            if (statGainRoll > 50) {
                when (quest.statType) {
                    "STR" -> { mStr++; optionalStatMsg = " Gained +1 Strength!" }
                    "INT" -> { mInt++; optionalStatMsg = " Gained +1 Intellect!" }
                    "AGI" -> { mAgi++; optionalStatMsg = " Gained +1 Agility!" }
                    "CON" -> { mCon++; optionalStatMsg = " Gained +1 Constitution!" }
                }
            }

            // Update quest statistics
            val updatedQuest = quest.copy(
                completedCount = quest.completedCount + 1,
                lastTickedTimestamp = System.currentTimeMillis()
            )
            repository.updateQuest(updatedQuest)

            // Save character stats
            val updatedChar = char.copy(
                level = newLevel,
                exp = newExp,
                maxExp = maxExp,
                hp = currentHp,
                maxHp = maxHp,
                gold = char.gold + earnedGold,
                strength = mStr,
                intellect = mInt,
                agility = mAgi,
                constitution = mCon,
                totalQuestsCompleted = char.totalQuestsCompleted + 1
            )
            repository.updateCharacter(updatedChar)

            repository.insertLog(
                GameLog("Completed '${quest.title}'! +$earnedExp EXP, +$earnedGold Gold!$optionalStatMsg", "EXP")
            )

            // Check achievement progress
            checkBadges(updatedChar)
        }
    }

    // Trigger a negative habit -> Takes health damage & inflates debuffs
    fun triggerNegativeHabit(questId: Int) {
        viewModelScope.launch {
            val originalQuests = repository.allQuests.first()
            val quest = originalQuests.find { it.id == questId } ?: return@launch
            val char = repository.getCharacterDirect() ?: return@launch

            val damage = when (quest.difficulty) {
                "EASY" -> 10
                "MEDIUM" -> 18
                "HARD" -> 30
                else -> 10
            }

            val nextHp = kotlin.math.max(0, char.hp - damage)
            var updatedChar = char.copy(hp = nextHp)

            repository.insertLog(
                GameLog("Damage Suffered! Triggered '${quest.title}' and took -$damage HP!", "DAMAGE")
            )

            // Update quest fails
            val updatedQuest = quest.copy(
                failCount = quest.failCount + 1,
                lastTickedTimestamp = System.currentTimeMillis()
            )
            repository.updateQuest(updatedQuest)

            // Apply bad habits debuffs on a 40% probability roll
            val debuffRoll = (1..100).random()
            if (debuffRoll > 60) {
                updatedChar = when (quest.statType) {
                    "STR" -> {
                        if (!updatedChar.isGlassSwordActive) {
                            repository.insertLog(GameLog("Shattered! Infected with 'Glass Sword' debuff (-2 STR).", "DEBUFF_ON"))
                            updatedChar.copy(isGlassSwordActive = true)
                        } else updatedChar
                    }
                    "INT" -> {
                        if (!updatedChar.isBrainFogActive) {
                            repository.insertLog(GameLog("Mindless scrolling! Infected with 'Brain Fog' debuff (-2 INT).", "DEBUFF_ON"))
                            updatedChar.copy(isBrainFogActive = true)
                        } else updatedChar
                    }
                    "AGI" -> {
                        if (!updatedChar.isSlothActive) {
                            repository.insertLog(GameLog("Inertia! Infected with 'Sluggish' debuff (-2 AGI).", "DEBUFF_ON"))
                            updatedChar.copy(isSlothActive = true)
                        } else updatedChar
                    }
                    "CON" -> {
                        if (!updatedChar.isSnackSackerActive) {
                            repository.insertLog(GameLog("Unhealthy habits! Infected with 'Snack Sacker' debuff (-2 CON).", "DEBUFF_ON"))
                            updatedChar.copy(isSnackSackerActive = true)
                        } else updatedChar
                    }
                    else -> updatedChar
                }
            }

            // Resurrect if HP is depleted
            if (nextHp <= 0) {
                updatedChar = resurrectCharacter(updatedChar)
            }

            repository.updateCharacter(updatedChar)
            checkBadges(updatedChar)
        }
    }

    // Resurrection mechanism upon dying/fainting
    private suspend fun resurrectCharacter(char: UserCharacter): UserCharacter {
        val penalisedLvl = kotlin.math.max(1, char.level - 1)
        // Lose some gold
        val goldPenalty = (char.gold * 0.20).toInt()
        val nextGold = kotlin.math.max(0, char.gold - goldPenalty)

        repository.insertLog(
            GameLog("DEATH WARNING: You fainted! Lost 1 Level, lost $goldPenalty Gold, but safely resurrected with half health.", "DAMAGE")
        )

        return char.copy(
            level = penalisedLvl,
            exp = 0,
            gold = nextGold,
            hp = char.maxHp / 2, // Resurrect with half HP
            streakDays = 0 // Lose streak
        )
    }

    // Purchase shop items
    fun buyShopItem(itemName: String, cost: Int) {
        viewModelScope.launch {
            val char = repository.getCharacterDirect() ?: return@launch
            if (char.gold < cost) {
                repository.insertLog(GameLog("Insufficient Gold to purchase $itemName!", "DAMAGE"))
                return@launch
            }

            var updatedChar = char.copy(gold = char.gold - cost)
            var logMessage = ""

            when (itemName) {
                "Health Potion" -> {
                    val newHp = kotlin.math.min(char.maxHp, char.hp + 30)
                    updatedChar = updatedChar.copy(hp = newHp)
                    logMessage = "Purchased Health Potion: Recovered +30 HP!"
                }
                "Golden Elixir" -> {
                    // Cures all active debuffs
                    val hadDebuffs = char.isSlothActive || char.isBrainFogActive || char.isSnackSackerActive || char.isGlassSwordActive
                    updatedChar = updatedChar.copy(
                        isSlothActive = false,
                        isBrainFogActive = false,
                        isSnackSackerActive = false,
                        isGlassSwordActive = false
                    )
                    logMessage = if (hadDebuffs) {
                        "Drank Golden Elixir: Cured all sluggish, brain fog, weak, and decay debuffs!"
                    } else {
                        "Drank Golden Elixir: Trivial taste! No debuffs were active, but feels refreshed."
                    }
                }
                "Ring of Power" -> {
                    updatedChar = updatedChar.copy(strength = char.strength + 2)
                    logMessage = "Equipped Ring of Power: Saved dynamically! +2 Strength gained!"
                }
                "Amulet of Scholar" -> {
                    updatedChar = updatedChar.copy(intellect = char.intellect + 2)
                    logMessage = "Equipped Amulet of Scholar: +2 Intellect gained!"
                }
                "Boots of Swiftness" -> {
                    updatedChar = updatedChar.copy(agility = char.agility + 2)
                    logMessage = "Equipped Boots of Swiftness: +2 Agility gained!"
                }
                "Belt of Vitality" -> {
                    val addedMaxHp = 10
                    updatedChar = updatedChar.copy(
                        constitution = char.constitution + 2,
                        maxHp = char.maxHp + addedMaxHp,
                        hp = char.hp + addedMaxHp
                    )
                    logMessage = "Equipped Belt of Vitality: +2 Constitution & +10 Max HP gained!"
                }
            }

            repository.updateCharacter(updatedChar)
            repository.insertLog(GameLog(logMessage, "HEAL"))
            checkBadges(updatedChar)
        }
    }

    // Scan badges and trigger unlock notifications
    private suspend fun checkBadges(char: UserCharacter) {
        val currentBadges = repository.allBadges.first()
        for (badge in currentBadges) {
            if (badge.isUnlocked) continue

            val shouldUnlock = when (badge.id) {
                "LVL_5" -> char.level >= 5
                "LVL_10" -> char.level >= 10
                "QUEST_10" -> char.totalQuestsCompleted >= 10
                "QUEST_50" -> char.totalQuestsCompleted >= 50
                "GOLD_100" -> char.gold >= 100
                "STAT_25" -> (char.strength >= 25 || char.intellect >= 25 || char.agility >= 25 || char.constitution >= 25)
                "STREAK_7" -> char.streakDays >= 7
                "DEBUFF_SURVIVOR" -> char.hp in 1..20
                else -> false
            }

            if (shouldUnlock) {
                val updatedBadge = badge.copy(isUnlocked = true, unlockedAt = System.currentTimeMillis())
                repository.updateBadge(updatedBadge)
                repository.insertLog(GameLog("ACHIEVEMENT UNLOCKED: \"${badge.title}\"! (${badge.description})", "BADGE"))
            }
        }
    }

    // Diagnostic actions
    fun clearLogHistory() {
        viewModelScope.launch {
            repository.clearLogs()
        }
    }

    // Force Sleep at Inn (Resets all Dailies and gives a refresh, costs 5 gold but heals full health)
    fun restAtInn() {
        viewModelScope.launch {
            val char = repository.getCharacterDirect() ?: return@launch
            if (char.gold < 5) {
                repository.insertLog(GameLog("Not enough gold to rent a room at the Tavern!", "DAMAGE"))
                return@launch
            }

            // Deduct 5 gold, fully heal, reset all ticked DAILY quests
            val originalQuests = repository.allQuests.first()
            for (quest in originalQuests) {
                if (quest.type == "DAILY" && quest.isCompletedToday()) {
                    val resetQuest = quest.copy(lastTickedTimestamp = 0L)
                    repository.updateQuest(resetQuest)
                }
            }

            val updatedChar = char.copy(
                gold = char.gold - 5,
                hp = char.maxHp
            )
            repository.updateCharacter(updatedChar)

            repository.insertLog(GameLog("Rented a bed at the Tavern. HP fully restored! All Daily Quests reset.", "HEAL"))
        }
    }
}
