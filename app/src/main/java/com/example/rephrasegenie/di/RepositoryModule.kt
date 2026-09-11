package com.example.rephrasegenie.di

import com.example.rephrasegenie.data.repository.HistoryRepositoryImpl
import com.example.rephrasegenie.data.repository.IdentityRepositoryImpl
import com.example.rephrasegenie.data.repository.InstalledAppsRepositoryImpl
import com.example.rephrasegenie.data.repository.OpenAiRepositoryImpl
import com.example.rephrasegenie.data.repository.SettingsRepositoryImpl
import com.example.rephrasegenie.data.repository.ToneRepositoryImpl
import com.example.rephrasegenie.domain.repository.HistoryRepository
import com.example.rephrasegenie.domain.repository.IdentityRepository
import com.example.rephrasegenie.domain.repository.InstalledAppsRepository
import com.example.rephrasegenie.domain.repository.OpenAiRepository
import com.example.rephrasegenie.domain.repository.SettingsRepository
import com.example.rephrasegenie.domain.repository.ToneRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindToneRepository(impl: ToneRepositoryImpl): ToneRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindIdentityRepository(impl: IdentityRepositoryImpl): IdentityRepository

    @Binds
    @Singleton
    abstract fun bindOpenAiRepository(impl: OpenAiRepositoryImpl): OpenAiRepository

    @Binds
    @Singleton
    abstract fun bindHistoryRepository(impl: HistoryRepositoryImpl): HistoryRepository

    @Binds
    @Singleton
    abstract fun bindInstalledAppsRepository(
        impl: InstalledAppsRepositoryImpl,
    ): InstalledAppsRepository
}
