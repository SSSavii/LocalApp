package ru.example.dictionary

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class FileManager(private val context: Context) {
    private val appFolder: File

    init {
        // Путь к папке LocalAppData
        val externalDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        appFolder = File(externalDir, "LocalAppData")
    }

    fun initializeData() {
        if (!appFolder.exists()) {
            appFolder.mkdirs() // Создаём папку, если её нет
        }

        // Копируем все данные из assets
        copyAssetsRecursively("")
    }

    private fun copyAssetsRecursively(assetPath: String) {
        try {
            val assetFiles = context.assets.list(assetPath) ?: return

            for (fileName in assetFiles) {
                val fullPath = if (assetPath.isEmpty()) fileName else "$assetPath/$fileName"
                val targetFile = File(appFolder, fullPath)

                if (context.assets.list(fullPath)?.isNotEmpty() == true) {
                    // Если это папка, создаём её и копируем содержимое
                    Log.d("FileManager", "Creating folder: ${targetFile.absolutePath}")
                    targetFile.mkdirs()
                    copyAssetsRecursively(fullPath)
                } else {
                    // Если это файл, копируем его
                    Log.d("FileManager", "Copying file: $fullPath to ${targetFile.absolutePath}")
                    context.assets.open(fullPath).use { inputStream ->
                        FileOutputStream(targetFile).use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                }
            }
        } catch (e: IOException) {
            Log.e("FileManager", "Error copying assets: $assetPath", e)
        }
    }

    fun getFolderPath(): String {
        return appFolder.absolutePath
    }
}