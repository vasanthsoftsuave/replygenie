package com.example.rephrasegenie.di

import android.content.Context
import androidx.room.Room
import com.example.rephrasegenie.data.local.db.GenerationDao
import com.example.rephrasegenie.data.local.db.RephraseGenieDatabase
import com.example.rephrasegenie.data.local.db.ToneDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): RephraseGenieDatabase =
        Room.databaseBuilder(context, RephraseGenieDatabase::class.java, RephraseGenieDatabase.NAME)
            .build()

    @Provides
    fun provideToneDao(db: RephraseGenieDatabase): ToneDao = db.toneDao()

    @Provides
    fun provideGenerationDao(db: RephraseGenieDatabase): GenerationDao = db.generationDao()
}
