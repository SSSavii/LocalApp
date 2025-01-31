package ru.example.dictionary
import android.content.Context
import java.io.IOException
import android.util.Log
class LocalDataManager(private val context: Context) {
    private val hieroglyphsMap = mutableMapOf<String, List<String>>()

    init {
        loadData()
    }

    private fun loadData() {
        try {
            context.assets.open("hieroglyphs.txt").bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    val (unicode, parts) = line.split(":")
                    hieroglyphsMap[unicode] = parts.split(",")
                }
            }
        } catch (e: IOException) {
            Log.e("LocalDataManager", "Error loading data", e)
        }
    }

    fun getAvailableGraphemes(selectedGraphemes: List<String>): List<String> {
        if (selectedGraphemes.isEmpty()) {
            return getAllInitialGraphemes()
        }

        val availableGraphemes = mutableListOf<String>()

        hieroglyphsMap.forEach { (_, parts) ->
            // Создаем копии списков для подсчета
            val remainingParts = parts.toMutableList()
            val selectedGraphemesCopy = selectedGraphemes.toMutableList()

            // Проверяем, содержатся ли все выбранные графемы в текущем иероглифе
            var allFound = true
            for (selected in selectedGraphemes) {
                val index = remainingParts.indexOf(selected)
                if (index != -1) {
                    remainingParts.removeAt(index)
                    selectedGraphemesCopy.remove(selected)
                } else {
                    allFound = false
                    break
                }
            }

            // Если все выбранные графемы найдены
            if (allFound) {
                // Добавляем оставшиеся части, включая повторяющиеся
                remainingParts.forEach { part ->
                    availableGraphemes.add(part)
                }
            }
        }

        return availableGraphemes
    }

    fun getHieroglyph(graphemes: List<String>): String? {
        return hieroglyphsMap.entries.find { (_, parts) ->
            parts.size == graphemes.size &&
                    parts.groupBy { it } == graphemes.groupBy { it }
        }?.key?.let { unicode ->
            String(Character.toChars(unicode.toInt(16)))
        }
    }

    private fun getAllInitialGraphemes(): List<String> {
        return hieroglyphsMap.values.flatten().distinct()
    }
}