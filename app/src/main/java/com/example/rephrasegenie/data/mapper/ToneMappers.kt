package com.example.rephrasegenie.data.mapper

import com.example.rephrasegenie.data.local.db.GenerationEntity
import com.example.rephrasegenie.data.local.db.ToneEntity
import com.example.rephrasegenie.domain.model.GenerationRecord
import com.example.rephrasegenie.domain.model.Tone
import java.time.Instant

fun ToneEntity.toDomain(): Tone = Tone(
    id = id,
    name = name,
    description = description,
    prompt = prompt,
    rawInstruction = rawInstruction,
    exampleInput = exampleInput,
    exampleOutput = exampleOutput,
    iconKey = iconKey,
    category = category,
    color = color,
    version = version,
    isBuiltIn = isBuiltIn,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
    usageCount = usageCount,
    lastUsedAt = lastUsedAt?.let(Instant::ofEpochMilli),
)

fun Tone.toEntity(): ToneEntity = ToneEntity(
    id = id,
    name = name,
    description = description,
    prompt = prompt,
    rawInstruction = rawInstruction,
    exampleInput = exampleInput,
    exampleOutput = exampleOutput,
    iconKey = iconKey,
    category = category,
    color = color,
    version = version,
    isBuiltIn = isBuiltIn,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
    usageCount = usageCount,
    lastUsedAt = lastUsedAt?.toEpochMilli(),
)

fun GenerationEntity.toDomain(): GenerationRecord = GenerationRecord(
    id = id,
    toneId = toneId,
    toneVersion = toneVersion,
    timestamp = Instant.ofEpochMilli(timestamp),
    inputLength = inputLength,
    outputLength = outputLength,
    model = model,
    status = status,
    durationMs = durationMs,
)

fun GenerationRecord.toEntity(): GenerationEntity = GenerationEntity(
    id = id,
    toneId = toneId,
    toneVersion = toneVersion,
    timestamp = timestamp.toEpochMilli(),
    inputLength = inputLength,
    outputLength = outputLength,
    model = model,
    status = status,
    durationMs = durationMs,
)
