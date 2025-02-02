package ru.example.dictionary

import android.util.Log
import java.io.File
import java.io.IOException

class LocalDataManager(private val dataPath: String) {
    private val hieroglyphsMap = mutableMapOf<String, List<String>>()

    init {
        loadData()
    }

    private fun loadData() {
        try {
            // Читаем файл hieroglyphs.txt из папки LocalAppData
            val file = File(dataPath, "hieroglyphs.txt")
            file.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    val (unicode, parts) = line.split(":")
                    hieroglyphsMap[unicode] = parts.split(",")
                }
            }
        } catch (e: IOException) {
            Log.e("LocalDataManager", "Error loading hieroglyphs data", e)
        }
    }

    fun getAvailableGraphemes(selectedGraphemes: List<String>): List<String> {
        val availableGraphemes = mutableListOf<String>()
        hieroglyphsMap.forEach { (_, parts) ->
            if (selectedGraphemes.all { parts.contains(it) }) {
                availableGraphemes.addAll(parts)
            }
        }
        return availableGraphemes
    }

    fun getHieroglyph(graphemes: List<String>): String? {
        return hieroglyphsMap.entries.find { (_, parts) ->
            parts.size == graphemes.size && parts.groupBy { it } == graphemes.groupBy { it }
        }?.key?.let { unicode ->
            try {
                String(Character.toChars(unicode.toInt(16))) // Преобразуем Unicode в символ
            } catch (e: Exception) {
                Log.e("LocalDataManager", "Invalid Unicode: $unicode", e)
                null
            }
        }
    }
}