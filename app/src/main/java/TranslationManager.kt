package ru.example.dictionary

import android.util.Log
import org.json.JSONArray
import java.io.File

data class Translation(
    val pinyin: String,
    val meanings: List<String>
)

class TranslationManager(private val dataPath: String) {
    private val translations = mutableMapOf<String, Translation>()

    init {
        loadTranslations()
    }

    private fun loadTranslations() {
        try {
            // Читаем файл dictionary.json из папки LocalAppData
            val file = File(dataPath, "dictionary.json")
            val jsonString = file.readText()
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
                }
            }
        } catch (e: Exception) {
            Log.e("TranslationManager", "Error loading translations", e)
        }
    }

    fun getTranslations(text: String): List<Translation?> {
        return text.split(" ").map { character ->
            translations[character]
        }
    }
}