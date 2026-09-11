package com.example.rephrasegenie.data.local.tone

import android.content.Context
import android.util.Log
import com.example.rephrasegenie.domain.model.Tone
import com.example.rephrasegenie.domain.prompt.ToneMarkdownParser
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads the built-in tones from assets/tones.
 *
 * A file that fails to parse is skipped and logged rather than failing the whole load, matching
 * the Windows app: one bad file should not leave the user with no tones at all.
 */
@Singleton
class BuiltInToneLoader @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private companion object {
        const val TAG = "BuiltInToneLoader"
        const val DIRECTORY = "tones"
    }

    fun load(): List<Tone> {
        val fileNames: List<String> = try {
            context.assets.list(DIRECTORY)?.toList().orEmpty()
        } catch (e: Exception) {
            Log.w(TAG, "Could not list built-in tones", e)
            emptyList()
        }

        val tones = mutableListOf<Tone>()
        for (fileName in fileNames) {
            if (!fileName.endsWith(".md", ignoreCase = true)) continue
            try {
                val stream = context.assets.open("$DIRECTORY/$fileName")
                val markdown = stream.bufferedReader().use { reader -> reader.readText() }
                tones.add(ToneMarkdownParser.parse(markdown))
            } catch (e: Exception) {
                Log.w(TAG, "Could not read built-in tone $fileName", e)
            }
        }
        return tones.sortedBy { it.name.lowercase() }
    }
}
