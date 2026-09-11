package com.example.rephrasegenie.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tones")
data class ToneEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val prompt: String,
    val rawInstruction: String?,
    val exampleInput: String?,
    val exampleOutput: String?,
    val iconKey: String?,
    val category: String?,
    val color: String?,
    val version: String,
    val isBuiltIn: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val usageCount: Int,
    val lastUsedAt: Long?,
)

/** Lengths and timing only — never the text. */
@Entity(tableName = "generations")
data class GenerationEntity(
    @PrimaryKey val id: String,
    val toneId: String,
    val toneVersion: String,
    val timestamp: Long,
    val inputLength: Int,
    val outputLength: Int,
    val model: String,
    val status: String,
    val durationMs: Int,
)
