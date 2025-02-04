//LocalDataManager
package ru.example.dictionary

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.util.Log
import android.util.LruCache
import java.io.File

class LocalDataManager(private val context: Context) {
    private val hieroglyphsMap = mutableMapOf<String, List<String>>()
    private val downloadsPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
    private val appFolderName = "DictionaryData"
    private val hieroglyphsFileName = "hieroglyphs.txt"
    private val dictionaryFileName = "dictionary.json"
    private val graphemsFolderName = "graphems"
    private val bitmapCache = LruCache<String, Bitmap>(1000) // Кэширует до 100 изображений

    init {
        Log.d("LocalDataManager", "Initializing...")
        try {
            createAppFolders()
            copyFiles()
            loadData()
        } catch (e: Exception) {
            Log.e("LocalDataManager", "Error in initialization", e)
        }
    }

    private fun createAppFolders() {
        val appFolder = File(downloadsPath, appFolderName)
        val graphemsFolder = File(appFolder, graphemsFolderName)

        if (!appFolder.exists()) {
            val created = appFolder.mkdirs()
            Log.d("LocalDataManager", "App folder created: $created at ${appFolder.absolutePath}")
        }

        if (!graphemsFolder.exists()) {
            val created = graphemsFolder.mkdirs()
            Log.d("LocalDataManager", "Graphems folder created: $created at ${graphemsFolder.absolutePath}")
        }
    }
    fun getBitmapForGrapheme(fileName: String): Bitmap? {
        try {
            // Сначала проверяем кэш
            val cachedBitmap = bitmapCache.get(fileName)
            if (cachedBitmap != null) {
                return cachedBitmap
            }

            // Если в кэше нет, загружаем из файла
            val filePath = getGraphemePath(fileName)
            if (filePath == null) {
                Log.e("LocalDataManager", "File path is null for $fileName")
                return null
            }

            val file = File(filePath)
            if (!file.exists()) {
                Log.e("LocalDataManager", "File does not exist: $filePath")
                return null
            }

            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            val bitmap = BitmapFactory.decodeFile(filePath, options)
            if (bitmap == null) {
                Log.e("LocalDataManager", "Failed to decode bitmap from $filePath")
                return null
            }

            // Сохраняем в кэш
            bitmapCache.put(fileName, bitmap)
            return bitmap
        } catch (e: Exception) {
            Log.e("LocalDataManager", "Error loading bitmap for $fileName", e)
            return null
        }
    }
    fun validateAllGraphemes() {
        val graphemsFolder = File("${downloadsPath}/${appFolderName}/${graphemsFolderName}")
        if (!graphemsFolder.exists() || !graphemsFolder.isDirectory) {
            Log.e("LocalDataManager", "Graphems folder does not exist or is not a directory")
            return
        }

        val files = graphemsFolder.listFiles()?.filter { it.isFile && it.name.endsWith(".png") }
        Log.d("LocalDataManager", "Total grapheme files found: ${files?.size}")

        files?.forEach { file ->
            val number = file.name.removeSuffix(".png").toIntOrNull()
            Log.d("LocalDataManager", "Grapheme ${file.name}: number=$number, size=${file.length()}")
        }
    }

    private fun copyFiles() {
        val appFolder = File(downloadsPath, appFolderName)
        val graphemsFolder = File(appFolder, graphemsFolderName)

        // Проверяем наличие графем
        if (graphemsFolder.exists() && graphemsFolder.listFiles()?.isNotEmpty() == true) {
            Log.d("LocalDataManager", "Graphems folder already contains files, skipping copy")
            return
        }

        // Если графем нет, копируем все файлы
        Log.d("LocalDataManager", "No graphems found, copying all files")

        // Копируем hieroglyphs.txt
        val hieroglyphsFile = File(appFolder, hieroglyphsFileName)
        if (!hieroglyphsFile.exists() || hieroglyphsFile.length() == 0L) {
            copyAssetFile(hieroglyphsFileName, hieroglyphsFile)
        }

        // Копируем dictionary.json
        val dictionaryFile = File(appFolder, dictionaryFileName)
        if (!dictionaryFile.exists() || dictionaryFile.length() == 0L) {
            copyAssetFile(dictionaryFileName, dictionaryFile)
        }

        // Копируем графемы
        context.assets.list("graphems")?.forEach { graphemFile ->
            val targetFile = File(graphemsFolder, graphemFile)
            if (!targetFile.exists() || targetFile.length() == 0L) {
                copyAssetFile("graphems/$graphemFile", targetFile)
            }
        }
    }

    private fun copyAssetFile(assetPath: String, targetFile: File) {
        try {
            context.assets.open(assetPath).use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Log.d("LocalDataManager", "Successfully copied $assetPath to ${targetFile.absolutePath}")
        } catch (e: Exception) {
            Log.e("LocalDataManager", "Error copying $assetPath", e)
            throw e
        }
    }

    private fun loadData() {
        try {
            val hieroglyphsFile = File(File(downloadsPath, appFolderName), hieroglyphsFileName)
            if (hieroglyphsFile.exists()) {
                hieroglyphsFile.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        val (unicode, parts) = line.split(":")
                        hieroglyphsMap[unicode] = parts.split(",")
                    }
                }
                Log.d("LocalDataManager", "Successfully loaded data from ${hieroglyphsFile.absolutePath}")
            } else {
                Log.e("LocalDataManager", "Hieroglyphs file not found at ${hieroglyphsFile.absolutePath}")
            }
        } catch (e: Exception) {
            Log.e("LocalDataManager", "Error loading data", e)
            throw e
        }
    }

    fun getGraphemePath(graphemName: String): String? {
        val file = File("${downloadsPath}/${appFolderName}/${graphemsFolderName}/${graphemName}")
        return if (file.exists() && file.length() > 0L) {
            file.absolutePath
        } else {
            null
        }
    }

    fun getAvailableGraphemes(selectedGraphemes: List<String>): List<String> {
        Log.d("LocalDataManager", "Getting available graphemes for selected: $selectedGraphemes")

        if (selectedGraphemes.isEmpty()) {
            val initialGraphemes = getAllInitialGraphemes()
            Log.d("LocalDataManager", "Returning all initial graphemes: ${initialGraphemes.size}")
            return initialGraphemes
        }

        val availableGraphemes = mutableSetOf<String>() // Используем Set для уникальности

        hieroglyphsMap.forEach { (_, parts) ->
            if (parts.containsAll(selectedGraphemes)) {
                // Добавляем все оставшиеся части
                parts.forEach { part ->
                    if (!selectedGraphemes.contains(part) && graphemeFileExists("$part.png")) {
                        availableGraphemes.add(part)
                    }
                }
            }
        }

        Log.d("LocalDataManager", "Found available graphemes: ${availableGraphemes.size}")
        return availableGraphemes.toList()
    }

    private fun getAllInitialGraphemes(): List<String> {
        val graphemsFolder = File("${downloadsPath}/${appFolderName}/${graphemsFolderName}")
        val allGraphemes = mutableListOf<String>()

        if (graphemsFolder.exists() && graphemsFolder.isDirectory) {
            graphemsFolder.listFiles()?.forEach { file ->
                if (file.isFile && file.name.endsWith(".png")) {
                    val graphemeName = file.name.removeSuffix(".png")
                    allGraphemes.add(graphemeName)
                }
            }
        }

        Log.d("LocalDataManager", "Total initial graphemes found: ${allGraphemes.size}")
        return allGraphemes
    }

    private fun graphemeFileExists(fileName: String): Boolean {
        val file = File("${downloadsPath}/${appFolderName}/${graphemsFolderName}/${fileName}")
        val exists = file.exists() && file.length() > 0L
        if (!exists) {
            Log.d("LocalDataManager", "Grapheme file not found: $fileName")
        }
        return exists
    }


    fun getHieroglyph(graphemes: List<String>): String? {
        return hieroglyphsMap.entries.find { (_, parts) ->
            parts.size == graphemes.size &&
                    parts.groupBy { it } == graphemes.groupBy { it }
        }?.key?.let { unicode ->
            String(Character.toChars(unicode.toInt(16)))
        }
    }
    fun clearCache() {
        bitmapCache.evictAll()
    }
}