package ru.example.dictionary

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

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

    private fun loadTranslation(character: String) {
        val translation = translationManager.getTranslation(character)

        if (translation != null) {
            pinyinTextView.text = translation.pinyin
            meaningsTextView.text = translation.meanings.joinToString("\n") { meaning -> "• $meaning" }
        } else {
            displayError("Перевод не найден")
        }
    }

    private fun displayTranslation(tokens: List<TokenDetail>) {
        runOnUiThread {
            if (tokens.isNotEmpty()) {
                val token = tokens[0]
                pinyinTextView.text = token.pinyin
                meaningsTextView.text = token.meanings.joinToString("\n") { meaning -> "• $meaning" }
            }
        }
    }

    private fun displaySingleToken(response: TranslationResponse) {
        runOnUiThread {
            pinyinTextView.text = response.pinyin ?: ""
            response.meanings?.let { meanings ->
                meaningsTextView.text = meanings.joinToString("\n") { meaning -> "• $meaning" }
            } ?: run {
                meaningsTextView.text = ""
            }
        }
    }

    private fun displayError(message: String) {
        runOnUiThread {
            pinyinTextView.text = ""
            meaningsTextView.text = message
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }
}