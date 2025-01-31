package ru.example.dictionary

import android.content.Context
import android.util.Log
import org.json.JSONArray

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
            val jsonString = context.assets.open("dictionary.json").bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)

            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val keys = jsonObject.keys()

                if (keys.hasNext()) {
                    val character = keys.next()
                    val characterData = jsonObject.getJSONArray(character)

                    val pinyin = characterData.getString(0)
                    val meanings = mutableListOf<String>()
                    val meaningsArray = characterData.getJSONArray(1)

                    for (j in 0 until meaningsArray.length()) {
                        meanings.add(meaningsArray.getString(j))
                    }

                    translations[character] = Translation(pinyin, meanings)

                    Log.d("TranslationManager", "Loaded translation for: $character")
                }
            }

            Log.d("TranslationManager", "Total translations loaded: ${translations.size}")
        } catch (e: Exception) {
            Log.e("TranslationManager", "Error loading translations", e)
            e.printStackTrace()
        }
    }

    fun getTranslations(text: String): List<Translation?> {
        Log.d("TranslationManager", "Searching translations for: '$text'")

        // Разбиваем входной текст на отдельные иероглифы
        return text.split(" ").map { character ->
            translations[character].also { result ->
                Log.d("TranslationManager", "Translation for '$character' found: ${result != null}")
            }
        }
    }
}