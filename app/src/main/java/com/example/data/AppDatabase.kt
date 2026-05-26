package com.example.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [UserCharacter::class, Quest::class, Badge::class, GameLog::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun questDao(): QuestDao
}
