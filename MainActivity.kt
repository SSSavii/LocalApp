//MainActivity
package ru.example.dictionary

import android.Manifest
import android.widget.Toast
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.view.ViewGroup.MarginLayoutParams
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import java.io.File
import java.io.IOException
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.appcompat.app.AlertDialog

class MainActivity : AppCompatActivity() {
    private lateinit var selectedGraphemesContainer: LinearLayout
    private lateinit var buttonGrid: GridLayout
    private lateinit var characterInput: EditText
    private lateinit var confirmGraphemesButton: ImageButton
    private lateinit var translateButton: Button
    private val selectedGraphemes = mutableListOf<String>()
    private var isDeleting = false
    private lateinit var localDataManager: LocalDataManager
    private lateinit var clearGraphemesButton: ImageButton
    private val storagePermisionCode = 1
    private lateinit var exitButton : Button


    /*
1. Флаг `isDeleting` помогает различать между добавлением новой графемы и удалением существующей, что предотвращает нежелательные состояния клавиатуры.

2. При удалении графемы происходит полный сброс состояния и последовательное восстановление оставшихся графем, что гарантирует правильный порядок и набор доступных графем.

3. Последовательное добавление графем заново (`currentGraphemes.forEach`) обеспечивает корректное обновление состояния клавиатуры в соответствии с логикой сервера*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                localDataManager = LocalDataManager(this)
                localDataManager.validateAllGraphemes()
                initializeApp()
            } else {
                requestStoragePermission()
            }
        } else {
            if (checkStoragePermission()) {
                localDataManager = LocalDataManager(this)
                localDataManager.validateAllGraphemes()
                initializeApp()
            } else {
                requestStoragePermission()
            }
        }
    }

    private fun checkStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.addCategory("android.intent.category.DEFAULT")
                intent.data = Uri.parse("package:$packageName")
                startActivityForResult(intent, storagePermisionCode)
            } catch (e: Exception) {
                val intent = Intent()
                intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                startActivityForResult(intent, storagePermisionCode)
            }
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                storagePermisionCode
            )
        }
    }

    private fun initializeApp() {
        try {
            localDataManager = LocalDataManager(this)
            setupListeners()
            createImageButtons()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error initializing app", e)
            Toast.makeText(this, "Ошибка инициализации: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == storagePermisionCode) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    initializeApp()
                } else {
                    Toast.makeText(this, "Разрешение необходимо для работы приложения", Toast.LENGTH_LONG).show()
                    requestStoragePermission()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == storagePermisionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeApp()
            } else {
                Toast.makeText(this, "Разрешение необходимо для работы приложения", Toast.LENGTH_LONG).show()
                requestStoragePermission()
            }
        }
    }

    private fun initializeViews() {
        selectedGraphemesContainer = findViewById(R.id.selectedGraphemesContainer)
        buttonGrid = findViewById(R.id.buttonGrid)
        characterInput = findViewById(R.id.characterInput)
        confirmGraphemesButton = findViewById(R.id.confirmGraphemesButton)
        translateButton = findViewById(R.id.translateButton)
        clearGraphemesButton = findViewById(R.id.clearGraphemesButton)
        exitButton = findViewById(R.id.exitButton)
    }

    private fun setupListeners() {
        translateButton.setOnClickListener {
            val character = characterInput.text.toString()
            if (character.isNotEmpty()) {
                startTranslationActivity(character)
            } else {
                Toast.makeText(this, "Введите иероглиф", Toast.LENGTH_SHORT).show()
            }
        }

        // Обработчик для кнопки очистки
        clearGraphemesButton.setOnClickListener {
            selectedGraphemes.clear()
            updateSelectedGraphemesView()
            createImageButtons()
        }

        // Измененный обработчик для кнопки подтверждения
        confirmGraphemesButton.setOnClickListener {
            if (selectedGraphemes.isNotEmpty()) {
                checkHieroglyphExists()
            } else {
                Toast.makeText(this, "Выберите графемы", Toast.LENGTH_SHORT).show()
            }
        }
        exitButton.setOnClickListener {
            // Показываем диалог подтверждения
            AlertDialog.Builder(this)
                .setTitle("Подтверждение")
                .setMessage("Вы действительно хотите выйти?")
                .setPositiveButton("Да") { _, _ ->
                    finish() // Закрываем активность
                }
                .setNegativeButton("Нет", null)
                .show()
        }
    }
    private fun startTranslationActivity(character: String) {
        val intent = Intent(this, TranslationActivity::class.java)
        intent.putExtra("character", character)
        startActivity(intent)
    }

    private fun createImageButtons() {
        buttonGrid.removeAllViews()
        val screenWidth = resources.displayMetrics.widthPixels
        val imageWidth = (screenWidth * 0.09).toInt()
        val imageMargin = (screenWidth * 0.005).toInt()

        try {
            val graphemsFolder = File("${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath}/DictionaryData/graphems")

            if (!graphemsFolder.exists() || !graphemsFolder.isDirectory) {
                Log.e("MainActivity", "Graphems folder does not exist or is not a directory")
                return
            }

            val files = graphemsFolder.listFiles()?.filter {
                it.isFile && it.name.endsWith(".png")
            }?.sortedBy {
                it.name.removeSuffix(".png").toIntOrNull() ?: Int.MAX_VALUE
            }

            Log.d("MainActivity", "Found ${files?.size} PNG files")

            files?.forEach { file ->
                Log.d("MainActivity", "Processing file: ${file.name}")
                val imageView = createImageView(file.name, imageWidth, imageMargin)
                if (imageView != null) {
                    buttonGrid.addView(imageView)
                } else {
                    Log.e("MainActivity", "Failed to create ImageView for ${file.name}")
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in createImageButtons", e)
            e.printStackTrace()
        }
    }

    private fun createImageView(fileName: String, width: Int, margin: Int): ImageView? {
        try {
            val bitmap = localDataManager.getBitmapForGrapheme(fileName)
            if (bitmap == null) {
                Log.e("MainActivity", "Failed to load bitmap for $fileName")
                return null
            }

            return ImageView(this).apply {
                setImageBitmap(bitmap)
                setOnClickListener {
                    Log.d("MainActivity", "Clicked on grapheme: $fileName")
                    onImageClick(fileName)
                }

                layoutParams = GridLayout.LayoutParams().apply {
                    this.width = width
                    this.height = width
                    (this as MarginLayoutParams).setMargins(margin, margin, margin, margin)
                }

                isClickable = true
                isFocusable = true
                background = ResourcesCompat.getDrawable(resources, R.drawable.button_background, null)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error creating ImageView for $fileName", e)
            return null
        }
    }

    private fun onImageClick(fileName: String) {
        if (isDeleting) {
            isDeleting = false
            createImageButtons()
            return
        }

        val grapheme = fileName.removeSuffix(".png")
        Log.d("MainActivity", "Clicked grapheme: $grapheme")

        selectedGraphemes.add(grapheme)
        updateSelectedGraphemesView()
        fetchAvailableGraphemes()
    }


    private fun updateSelectedGraphemesView() {
        selectedGraphemesContainer.removeAllViews()

        val screenWidth = resources.displayMetrics.widthPixels
        val imageWidth = (screenWidth * 0.09).toInt()
        val margin = (resources.displayMetrics.density * 4).toInt()

        val graphemeIndices = selectedGraphemes.withIndex().toList()

        for ((index, grapheme) in graphemeIndices) {
            val filePath = localDataManager.getGraphemePath("$grapheme.png")
            if (filePath == null) {
                // Если файл графемы не существует, удаляем её из выбранных
                selectedGraphemes.removeAt(index)
                continue
            }

            val imageView = ImageView(this).apply {
                try {
                    val bitmap = BitmapFactory.decodeFile(filePath)
                    if (bitmap != null) {
                        setImageBitmap(bitmap)
                    } else {
                        selectedGraphemes.removeAt(index)
                        return@apply
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error loading image for $grapheme", e)
                    selectedGraphemes.removeAt(index)
                    return@apply
                }

                layoutParams = LinearLayout.LayoutParams(imageWidth, imageWidth).apply {
                    setMargins(margin, margin, margin, margin)
                }

                setOnClickListener {
                    isDeleting = true
                    selectedGraphemes.removeAt(index)
                    updateSelectedGraphemesView()

                    if (selectedGraphemes.isEmpty()) {
                        createImageButtons()
                    } else {
                        val currentGraphemes = selectedGraphemes.toList()
                        selectedGraphemes.clear()
                        createImageButtons()

                        currentGraphemes.forEach { g ->
                            if (localDataManager.getGraphemePath("$g.png") != null) {
                                selectedGraphemes.add(g)
                                fetchAvailableGraphemes()
                            }
                        }
                    }
                }
            }
            selectedGraphemesContainer.addView(imageView)
        }
    }

    private fun fetchAvailableGraphemes() {
        if (selectedGraphemes.isEmpty()) {
            createImageButtons()
            return
        }

        val availableGraphemes = localDataManager.getAvailableGraphemes(selectedGraphemes)

        runOnUiThread {
            buttonGrid.removeAllViews()
            if (availableGraphemes.isEmpty()) {
                createImageButtons()
            } else {
                val screenWidth = resources.displayMetrics.widthPixels
                val imageWidth = (screenWidth * 0.09).toInt()
                val imageMargin = (screenWidth * 0.005).toInt()

                availableGraphemes.forEach { grapheme ->
                    try {
                        val imageView = createImageView("$grapheme.png", imageWidth, imageMargin)
                        if (imageView != null) {
                            buttonGrid.addView(imageView)
                        }
                    } catch (e: IOException) {
                        Log.e("MainActivity", "Error creating image view for grapheme: $grapheme", e)
                    }
                }
            }
        }
    }

    private fun checkHieroglyphExists() {
        val hieroglyph = localDataManager.getHieroglyph(selectedGraphemes)

        if (hieroglyph != null) {
            // Добавляем новый иероглиф через пробел
            val currentText = characterInput.text.toString()
            val newText = if (currentText.isEmpty()) {
                hieroglyph
            } else {
                "$currentText $hieroglyph"
            }
            characterInput.setText(newText)

            Toast.makeText(
                this@MainActivity,
                "Иероглиф найден: $hieroglyph",
                Toast.LENGTH_SHORT
            ).show()

            selectedGraphemes.clear()
            updateSelectedGraphemesView()
            createImageButtons()
        } else {
            Toast.makeText(
                this@MainActivity,
                "Иероглиф не найден",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        // Очищаем кэш при выходе
        localDataManager.clearCache()
    }
}