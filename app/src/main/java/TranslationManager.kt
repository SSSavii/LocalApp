package ru.example.dictionary

import android.content.Context
import android.util.Log
import java.io.IOException

data class Translation(
    val pinyin: String,
    val meanings: List<String>
)

class TranslationManager(private val context: Context) {
    private val translations = mutableMapOf<String, Translation>()

    init {
        loadTranslations()
    }

    private fun loadTranslations() {
        try {
            context.assets.open("translations.txt").bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    val parts = line.split("|")
                    if (parts.size >= 3) {
                        val character = parts[0]
                        val pinyin = parts[1]
                        val meanings = parts[2].split(";")
                        translations[character] = Translation(pinyin, meanings)
                    }
                }
            }
        } catch (e: IOException) {
            Log.e("TranslationManager", "Error loading translations", e)
        }
    }

    fun getTranslation(character: String): Translation? {
        return translations[character]
    }
}