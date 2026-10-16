package ru.example.breakfast

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.content.SharedPreferences
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : Activity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var keypad: LinearLayout

    private lateinit var potField: EditText
    private lateinit var potEmptyField: EditText
    private lateinit var lossField: EditText

    private val nameFields = mutableListOf<EditText>()
    private val gramFields = mutableListOf<EditText>()
    private val resultLabels = mutableListOf<TextView>()

    private var activeNumberField: EditText? = null
    private var rebuilding = false

    private val defaultNames = listOf(
        "Я",
        "Жена",
        "Ребёнок 1",
        "Ребёнок 2",
        "Ребёнок 3"
    )

    private val defaultGrams = listOf(50, 30, 30, 20, 0)

    private val backgroundColor = Color.rgb(247, 245, 239)
    private val cardColor = Color.WHITE
    private val textColor = Color.rgb(45, 48, 43)
    private val mutedColor = Color.rgb(125, 128, 119)
    private val accentColor = Color.rgb(83, 112, 70)
    private val borderColor = Color.rgb(224, 225, 216)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        prefs = getSharedPreferences("porridge_settings", MODE_PRIVATE)

        buildScreen()
        restoreValues()
        recalculate()
    }

    private fun buildScreen() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(12), dp(18), dp(10))
            setBackgroundColor(backgroundColor)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val title = TextView(this).apply {
            text = "Моя каша"
            textSize = 28f
            setTextColor(textColor)
            typeface = Typeface.DEFAULT_BOLD
        }

        val settingsButton = TextView(this).apply {
            text = "⚙"
            textSize = 29f
            gravity = Gravity.CENTER
            setTextColor(accentColor)
            contentDescription = "Настройки"
            setPadding(dp(14), dp(4), dp(4), dp(4))
            setOnClickListener { showSettings() }
        }

        header.addView(
            title,
            LinearLayout.LayoutParams(0, dp(52), 1f)
        )
        header.addView(
            settingsButton,
            LinearLayout.LayoutParams(dp(52), dp(52))
        )
        root.addView(header)

        val scroll = ScrollView(this).apply {
            isFillViewport = false
            clipToPadding = false
            setPadding(0, dp(4), 0, dp(8))
        }

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        scroll.addView(content)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        buildPotCard()
        buildPeopleCards()
        buildKeypad()

        setContentView(root)
    }

    private fun buildPotCard() {
        val card = makeCard()

        addLabel(card, "ВЕС КАСТРЮЛИ С КАШЕЙ", 12f, mutedColor)

        potField = makeNumberField()
        potField.hint = "Например, 850"
        potField.textSize = 25f
        potField.typeface = Typeface.DEFAULT_BOLD

        potField.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                showKeypad(view as EditText)
            }
        }

        card.addView(
            potField,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(54)
            ).apply {
                topMargin = dp(6)
            }
        )

        val explanation = TextView(this).apply {
            text = "Вес в граммах"
            textSize = 13f
            setTextColor(mutedColor)
        }

        card.addView(
            explanation,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(4)
            }
        )

        content.addView(
            card,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(12)
            }
        )
    }

    private fun buildPeopleCards() {
        val heading = TextView(this).apply {
            text = "Кому сколько?"
            textSize = 19f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textColor)
        }

        content.addView(
            heading,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(10)
            }
        )

        for (i in defaultNames.indices) {
            val card = makeCard()

            val topRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val name = EditText(this).apply {
                textSize = 17f
                setTextColor(textColor)
                setSingleLine(true)
                setPadding(0, dp(4), dp(6), dp(4))
                background = null
                hint = "Имя"
                setSelectAllOnFocus(true)
            }

            val grams = makeNumberField().apply {
                textSize = 18f
                gravity = Gravity.CENTER
                setPadding(dp(4), 0, dp(4), 0)
                hint = "0"
            }

            nameFields.add(name)
            gramFields.add(grams)

            topRow.addView(
                name,
                LinearLayout.LayoutParams(0, dp(46), 1f)
            )

            val gramsColumn = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
            }

            addLabel(gramsColumn, "СУХАЯ КРУПА", 10f, mutedColor)

            gramsColumn.addView(
                grams,
                LinearLayout.LayoutParams(dp(86), dp(42))
            )

            topRow.addView(
                gramsColumn,
                LinearLayout.LayoutParams(
                    dp(100),
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )

            card.addView(topRow)

            val divider = View(this).apply {
                setBackgroundColor(borderColor)
            }

            card.addView(
                divider,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(1)
                ).apply {
                    topMargin = dp(8)
                    bottomMargin = dp(8)
                }
            )

            val bottomRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val resultCaption = TextView(this).apply {
                text = "Готовая каша"
                textSize = 14f
                setTextColor(mutedColor)
            }

            val result = TextView(this).apply {
                text = "—"
                textSize = 23f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(accentColor)
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
            }

            resultLabels.add(result)

            bottomRow.addView(
                resultCaption,
                LinearLayout.LayoutParams(0, dp(36), 1f)
            )

            bottomRow.addView(
                result,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    dp(36)
                )
            )

            card.addView(bottomRow)

            content.addView(
                card,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(10)
                }
            )

            grams.setOnFocusChangeListener { view, hasFocus ->
                if (hasFocus) {
                    showKeypad(view as EditText)
                }
            }

            name.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) saveValues()
            }

            grams.addTextChangedListenerSimple {
                if (!rebuilding) recalculate()
            }

            name.addTextChangedListenerSimple {
                if (!rebuilding) saveValues()
            }
        }

        potField.addTextChangedListenerSimple {
            if (!rebuilding) recalculate()
        }
    }

    private fun buildKeypad() {
        keypad = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(4), 0, 0)
            visibility = View.GONE
        }

        val rows = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("⌫", "0", "Ввод")
        )

        for (row in rows) {
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            for (key in row) {
                val button = Button(this).apply {
                    text = key
                    textSize = if (key == "Ввод") 14f else 21f
                    isAllCaps = false
                    setTextColor(
                        if (key == "Ввод") Color.WHITE else textColor
                    )

                    background = roundedBackground(
                        if (key == "Ввод") accentColor else cardColor,
                        12
                    )

                    setPadding(dp(2), 0, dp(2), 0)

                    setOnClickListener {
                        when (key) {
                            "Ввод" -> hideKeypad()
                            "⌫" -> deleteLastDigit()
                            else -> insertDigit(key)
                        }
                    }
                }

                rowLayout.addView(
                    button,
                    LinearLayout.LayoutParams(
                        0,
                        dp(49),
                        1f
                    ).apply {
                        marginStart = dp(3)
                        marginEnd = dp(3)
                        topMargin = dp(3)
                        bottomMargin = dp(3)
                    }
                )
            }

            keypad.addView(
                rowLayout,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }

        root.addView(
            keypad,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
    }

    private fun showKeypad(field: EditText) {
        activeNumberField = field

        val imm = getSystemService(Context.INPUT_METHOD_SERVICE)
            as InputMethodManager

        imm.hideSoftInputFromWindow(field.windowToken, 0)

        field.post {
            field.showSoftInputOnFocus = false
            keypad.visibility = View.VISIBLE
        }
    }

    private fun hideKeypad() {
        keypad.visibility = View.GONE

        activeNumberField?.clearFocus()
        activeNumberField = null

        val imm = getSystemService(Context.INPUT_METHOD_SERVICE)
            as InputMethodManager

        imm.hideSoftInputFromWindow(root.windowToken, 0)

        saveValues()
    }

    private fun insertDigit(digit: String) {
        val field = activeNumberField ?: return

        val start = field.selectionStart.coerceAtLeast(0)
        val end = field.selectionEnd.coerceAtLeast(0)
        val current = field.text.toString()

        val updated = current.replaceRange(
            minOf(start, end),
            maxOf(start, end),
            digit
        )

        field.setText(updated)
        field.setSelection((minOf(start, end) + digit.length).coerceAtMost(updated.length))
    }

    private fun deleteLastDigit() {
        val field = activeNumberField ?: return

        val start = field.selectionStart.coerceAtLeast(0)
        val end = field.selectionEnd.coerceAtLeast(0)
        val current = field.text.toString()

        if (start != end) {
            val updated = current.removeRange(
                minOf(start, end),
                maxOf(start, end)
            )
            field.setText(updated)
            field.setSelection(minOf(start, updated.length))
        } else if (start > 0) {
            val updated = current.removeRange(start - 1, start)
            field.setText(updated)
            field.setSelection(start - 1)
        }
    }

    private fun showSettings() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(8), dp(22), 0)
        }

        val emptyInput = EditText(this).apply {
            hint = "Вес пустой кастрюли, г"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(prefs.getInt("empty_pot", 0).toString())
            setSingleLine(true)
        }

        val lossInput = EditText(this).apply {
            hint = "Потери, г"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(prefs.getInt("loss", 0).toString())
            setSingleLine(true)
        }

        layout.addView(emptyInput)
        layout.addView(lossInput)

        AlertDialog.Builder(this)
            .setTitle("Настройки")
            .setView(layout)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Сохранить") { _, _ ->
                prefs.edit()
                    .putInt("empty_pot", emptyInput.text.toString().toIntOrNull() ?: 0)
                    .putInt("loss", lossInput.text.toString().toIntOrNull() ?: 0)
                    .apply()

                recalculate()
            }
            .show()
    }

    private fun restoreValues() {
        rebuilding = true

        potField.setText(
            prefs.getString("pot_weight", "") ?: ""
        )

        for (i in defaultNames.indices) {
            nameFields[i].setText(
                prefs.getString("name_$i", defaultNames[i])
            )

            gramFields[i].setText(
                prefs.getString("grams_$i", defaultGrams[i].toString())
            )
        }

        rebuilding = false
    }

    private fun saveValues() {
        if (!::prefs.isInitialized || nameFields.size != 5) return

        val editor = prefs.edit()
            .putString("pot_weight", potField.text.toString())

        for (i in defaultNames.indices) {
            editor.putString("name_$i", nameFields[i].text.toString())
            editor.putString("grams_$i", gramFields[i].text.toString())
        }

        editor.apply()
    }

    private fun recalculate() {
        if (rebuilding || nameFields.size != 5) return

        val pot = potField.text.toString().toDoubleOrNull()
        val empty = prefs.getInt("empty_pot", 0)
        val loss = prefs.getInt("loss", 0)

        val totalDry = gramFields.sumOf {
            it.text.toString().toDoubleOrNull() ?: 0.0
        }

        val cooked = if (pot != null) {
            (pot - empty - loss).coerceAtLeast(0.0)
        } else {
            null
        }

        for (i in defaultNames.indices) {
            val dry = gramFields[i].text.toString().toDoubleOrNull() ?: 0.0

            resultLabels[i].text = when {
                dry <= 0 -> "—"
                cooked == null -> "—"
                totalDry <= 0 -> "—"
                else -> "${(cooked * dry / totalDry).roundToInt()} г"
            }
        }

        saveValues()
    }

    private fun makeNumberField(): EditText {
        return EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setSingleLine(true)
            setTextColor(textColor)
            gravity = Gravity.CENTER_VERTICAL
            background = roundedBackground(cardColor, 8)
            setPadding(dp(10), 0, dp(10), 0)
            showSoftInputOnFocus = false
        }
    }

    private fun makeCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = roundedBackground(cardColor, 16)
        }
    }

    private fun addLabel(
        parent: LinearLayout,
        textValue: String,
        size: Float,
        color: Int
    ) {
        val label = TextView(this).apply {
            text = textValue
            textSize = size
            setTextColor(color)
            if (size >= 18f) {
                typeface = Typeface.DEFAULT_BOLD
            }
        }

        parent.addView(
            label,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
    }

    private fun roundedBackground(color: Int, radius: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            setStroke(dp(1), borderColor)
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).roundToInt()
    }

    override fun onPause() {
        saveValues()
        super.onPause()
    }

    private fun EditText.addTextChangedListenerSimple(action: () -> Unit) {
        addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {}

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                action()
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }
}