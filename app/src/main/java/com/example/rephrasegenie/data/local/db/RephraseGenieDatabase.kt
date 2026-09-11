package com.example.rephrasegenie.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ToneEntity::class, GenerationEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class RephraseGenieDatabase : RoomDatabase() {
    abstract fun toneDao(): ToneDao
    abstract fun generationDao(): GenerationDao

    companion object {
        const val NAME = "rephrasegenie.db"
    }
}
