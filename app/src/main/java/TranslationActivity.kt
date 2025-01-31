package ru.example.dictionary

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.util.Log

class TranslationActivity : AppCompatActivity() {
    private lateinit var characterTextView: TextView
    private lateinit var pinyinTextView: TextView
    private lateinit var meaningsTextView: TextView
    private lateinit var backButton: Button
    private lateinit var translationManager: TranslationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_translation)

        translationManager = TranslationManager(this)
        initializeViews()
        setupListeners()

        val character = intent.getStringExtra("character") ?: return
        characterTextView.text = character
        loadTranslation(character)
    }

    private fun initializeViews() {
        characterTextView = findViewById(R.id.characterTextView)
        pinyinTextView = findViewById(R.id.pinyinTextView)
        meaningsTextView = findViewById(R.id.meaningsTextView)
        backButton = findViewById(R.id.backButton)
    }

    private fun setupListeners() {
        backButton.setOnClickListener {
            finish()
        }
    }

    private fun loadTranslation(text: String) {
        Log.d("TranslationActivity", "Looking up text: $text")
        val translations = translationManager.getTranslations(text)

        if (translations.any { it != null }) {
            val characters = text.split(" ")
            val translationTexts = mutableListOf<String>()

            // Формируем текст для каждого иероглифа
            characters.forEachIndexed { index, character ->
                val translation = translations[index]
                if (translation != null) {
                    val translationText = buildString {
                        appendLine("【$character】")
                        appendLine(translation.pinyin)
                        translation.meanings.forEach { meaning ->
                            appendLine("• $meaning")
                        }
                        appendLine() // Пустая строка между переводами
                    }
                    translationTexts.add(translationText)
                } else {
                    translationTexts.add("【$character】\nПеревод не найден\n")
                }
            }

            // Объединяем все переводы
            val fullText = translationTexts.joinToString("\n")

            // Отображаем результат
            characterTextView.text = text
            pinyinTextView.text = translations.mapNotNull { it?.pinyin }.joinToString(" ")
            meaningsTextView.text = fullText

            Log.d("TranslationActivity", "Displayed translations for ${characters.size} characters")
        } else {
            Log.e("TranslationActivity", "No translations found for: $text")
            displayError("Перевод не найден для: $text")
        }
    }


    private fun displayError(message: String) {
        runOnUiThread {
            characterTextView.text = message
            pinyinTextView.text = ""
            meaningsTextView.text = ""
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }
}