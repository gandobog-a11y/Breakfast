package ru.example.breakfast

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlin.math.roundToInt

class MainActivity : Activity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var keypad: LinearLayout
    private lateinit var potField: EditText

    private lateinit var totalDryLabel: TextView
    private lateinit var waterLabel: TextView

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

        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        )

        prefs = getSharedPreferences(
            "porridge_settings",
            MODE_PRIVATE
        )

        buildScreen()
        restoreValues()
        recalculate()

        root.requestFocus()
    }

    private fun buildScreen() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            isFocusableInTouchMode = true
            descendantFocusability =
                LinearLayout.FOCUS_AFTER_DESCENDANTS

            setPadding(dp(12), dp(4), dp(12), dp(4))
            setBackgroundColor(backgroundColor)
        }

        val header = FrameLayout(this)

        val title = TextView(this).apply {
            text = "Наша каша"
            textSize = 30f
            setTextColor(textColor)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }

        header.addView(
            title,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dp(52)
            )
        )

        val settingsButton = TextView(this).apply {
            text = "⚙"
            textSize = 30f
            gravity = Gravity.CENTER
            setTextColor(accentColor)
            contentDescription = "Настройки"

            setOnClickListener {
                showSettings()
            }
        }

        header.addView(
            settingsButton,
            FrameLayout.LayoutParams(
                dp(48),
                dp(52),
                Gravity.END or Gravity.CENTER_VERTICAL
            )
        )

        root.addView(
            header,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52)
            )
        )

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            setPadding(0, 0, 0, dp(2))
            overScrollMode = View.OVER_SCROLL_NEVER
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
        root.requestFocus()

        root.setOnApplyWindowInsetsListener { view, insets ->
            val topInset: Int
            val bottomInset: Int

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bars = insets.getInsets(
                    WindowInsets.Type.systemBars()
                )

                topInset = bars.top
                bottomInset = bars.bottom
            } else {
                @Suppress("DEPRECATION")
                val legacyTop = insets.systemWindowInsetTop

                @Suppress("DEPRECATION")
                val legacyBottom = insets.systemWindowInsetBottom

                topInset = legacyTop
                bottomInset = legacyBottom
            }

            view.setPadding(
                dp(12),
                dp(4) + topInset,
                dp(12),
                dp(4) + bottomInset
            )

            insets
        }

        root.requestApplyInsets()
    }

    private fun buildPotCard() {
        val card = makeCard()

        card.setPadding(
            dp(12),
            dp(8),
            dp(12),
            dp(8)
        )

        addLabel(
            card,
            "ВЕС КАСТРЮЛИ С КАШЕЙ, г",
            15f,
            mutedColor
        )

        potField = makeNumberField().apply {
            hint = "Например, 850"
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER_VERTICAL
        }

        potField.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                showKeypad(view as EditText)
            }
        }

        potField.addTextChangedListenerSimple {
            if (!rebuilding) {
                recalculate()
            }
        }

        card.addView(
            potField,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(50)
            )
        )

        content.addView(
            card,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(8)
            }
        )
    }

    private fun buildPeopleCards() {
        val heading = TextView(this).apply {
            text = "Кому сколько?"
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textColor)
        }

        content.addView(
            heading,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(34)
            )
        )

        val columns = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), 0, dp(10), 0)
        }

        columns.addView(
            TextView(this).apply {
                text = "Имя"
                textSize = 15f
                setTextColor(mutedColor)
            },
            LinearLayout.LayoutParams(
                0,
                dp(28),
                1f
            )
        )

        columns.addView(
            TextView(this).apply {
                text = "Крупа"
                textSize = 15f
                gravity = Gravity.CENTER
                setTextColor(mutedColor)
            },
            LinearLayout.LayoutParams(
                dp(70),
                dp(28)
            )
        )

        columns.addView(
            TextView(this).apply {
                text = "Каша"
                textSize = 15f
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
                setTextColor(mutedColor)
            },
            LinearLayout.LayoutParams(
                dp(88),
                dp(28)
            )
        )

        content.addView(columns)

        for (i in defaultNames.indices) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL

                background = roundedBackground(cardColor, 10)

                setPadding(
                    dp(10),
                    0,
                    dp(10),
                    0
                )
            }

            val name = EditText(this).apply {
                textSize = 21f
                setTextColor(textColor)
                setSingleLine(true)
                setPadding(0, 0, dp(3), 0)
                background = null
                hint = "Имя"
                setSelectAllOnFocus(true)
                imeOptions = EditorInfo.IME_ACTION_DONE
            }

            val grams = makeNumberField().apply {
                textSize = 24f
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 0)
                hint = "0"
                background = null
            }

            val result = TextView(this).apply {
                text = "—"
                textSize = 26f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(accentColor)
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
                maxLines = 1
            }

            nameFields.add(name)
            gramFields.add(grams)
            resultLabels.add(result)

            row.addView(
                name,
                LinearLayout.LayoutParams(
                    0,
                    dp(52),
                    1f
                )
            )

            row.addView(
                grams,
                LinearLayout.LayoutParams(
                    dp(70),
                    dp(48)
                )
            )

            row.addView(
                result,
                LinearLayout.LayoutParams(
                    dp(88),
                    dp(48)
                )
            )

            content.addView(
                row,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(5)
                }
            )

            grams.setOnFocusChangeListener { view, hasFocus ->
                if (hasFocus) {
                    showKeypad(view as EditText)
                }
            }

            name.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    saveValues()
                }
            }

            grams.addTextChangedListenerSimple {
                if (!rebuilding) {
                    recalculate()
                }
            }

            name.addTextChangedListenerSimple {
                if (!rebuilding) {
                    saveValues()
                }
            }
        }

        // Итоговая строка под таблицей.
        // Слева оставляем свободное место под колонкой имён,
        // затем показываем общую крупу и воду.
        val totalsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(4), dp(10), dp(2))
        }

        totalsRow.addView(
            View(this),
            LinearLayout.LayoutParams(
                0,
                dp(1),
                1f
            )
        )

        val dryTotalColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        dryTotalColumn.addView(
            TextView(this).apply {
                text = "ВСЕГО КРУПЫ"
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(mutedColor)
                gravity = Gravity.CENTER
            }
        )

        totalDryLabel = TextView(this).apply {
            text = "0 г"
            textSize = 21f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textColor)
            gravity = Gravity.CENTER
        }

        dryTotalColumn.addView(totalDryLabel)

        val waterColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        waterColumn.addView(
            TextView(this).apply {
                text = "ВОДА"
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(mutedColor)
                gravity = Gravity.CENTER
            }
        )

        waterLabel = TextView(this).apply {
            text = "0 мл"
            textSize = 21f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(accentColor)
            gravity = Gravity.CENTER
        }

        waterColumn.addView(waterLabel)

        totalsRow.addView(
            dryTotalColumn,
            LinearLayout.LayoutParams(
                dp(70),
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        totalsRow.addView(
            waterColumn,
            LinearLayout.LayoutParams(
                dp(88),
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        content.addView(
            totalsRow,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(6)
            }
        )
    }

    private fun buildKeypad() {
        keypad = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(3), 0, 0)
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
                    textSize = if (key == "Ввод") 17f else 25f
                    isAllCaps = false

                    setTextColor(
                        if (key == "Ввод") Color.WHITE else textColor
                    )

                    background = roundedBackground(
                        if (key == "Ввод") accentColor else cardColor,
                        10
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
                        topMargin = dp(2)
                        bottomMargin = dp(2)
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

        field.showSoftInputOnFocus = false

        val imm = getSystemService(Context.INPUT_METHOD_SERVICE)
            as InputMethodManager

        imm.hideSoftInputFromWindow(
            field.windowToken,
            0
        )

        keypad.visibility = View.VISIBLE
    }

    private fun hideKeypad() {
        keypad.visibility = View.GONE

        activeNumberField?.clearFocus()
        activeNumberField = null

        val imm = getSystemService(Context.INPUT_METHOD_SERVICE)
            as InputMethodManager

        imm.hideSoftInputFromWindow(
            root.windowToken,
            0
        )

        root.requestFocus()
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

        field.setSelection(
            (minOf(start, end) + digit.length)
                .coerceAtMost(updated.length)
        )
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
            val updated = current.removeRange(
                start - 1,
                start
            )

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
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(prefs.getInt("empty_pot", 0).toString())
            setSingleLine(true)
            textSize = 21f
            hint = "Например, 254"
        }

        val lossInput = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(prefs.getInt("loss", 0).toString())
            setSingleLine(true)
            textSize = 21f
            hint = "Например, 30"
        }

        addLabel(
            layout,
            "Вес пустой кастрюли, г",
            17f,
            textColor
        )

        layout.addView(
            emptyInput,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(50)
            )
        )

        addLabel(
            layout,
            "Потери при варке, г",
            17f,
            textColor
        )

        layout.addView(
            lossInput,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(50)
            )
        )

        val dialog = AlertDialog.Builder(this)
            .setTitle("Настройки")
            .setView(layout)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Сохранить", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {
                    prefs.edit()
                        .putInt(
                            "empty_pot",
                            emptyInput.text.toString()
                                .toIntOrNull() ?: 0
                        )
                        .putInt(
                            "loss",
                            lossInput.text.toString()
                                .toIntOrNull() ?: 0
                        )
                        .apply()

                    recalculate()
                    dialog.dismiss()
                }
        }

        dialog.show()
    }

    private fun restoreValues() {
        rebuilding = true

        potField.setText(
            prefs.getString("pot_weight", "") ?: ""
        )

        for (i in defaultNames.indices) {
            nameFields[i].setText(
                prefs.getString(
                    "name_$i",
                    defaultNames[i]
                ) ?: defaultNames[i]
            )

            gramFields[i].setText(
                prefs.getString(
                    "grams_$i",
                    defaultGrams[i].toString()
                ) ?: defaultGrams[i].toString()
            )
        }

        rebuilding = false
    }

    private fun saveValues() {
        if (!::prefs.isInitialized || nameFields.size != 5) {
            return
        }

        val editor = prefs.edit()
            .putString(
                "pot_weight",
                potField.text.toString()
            )

        for (i in defaultNames.indices) {
            editor.putString(
                "name_$i",
                nameFields[i].text.toString()
            )

            editor.putString(
                "grams_$i",
                gramFields[i].text.toString()
            )
        }

        editor.apply()
    }

    private fun recalculate() {
        if (rebuilding || nameFields.size != 5) {
            return
        }

        val pot = potField.text.toString().toDoubleOrNull()
        val empty = prefs.getInt("empty_pot", 0)
        val loss = prefs.getInt("loss", 0)

        val totalDry = gramFields.sumOf {
            it.text.toString().toDoubleOrNull() ?: 0.0
        }

        // Вода: 630 мл на каждые 130 г сухой крупы.
        // Округляем результат до целого миллилитра.
        val waterMl = (totalDry * 630.0 / 130.0).roundToInt()

        totalDryLabel.text = "${totalDry.roundToInt()} г"
        waterLabel.text = "$waterMl мл"

        val cooked = if (pot != null) {
            (pot - empty - loss).coerceAtLeast(0.0)
        } else {
            null
        }

        for (i in defaultNames.indices) {
            val dry = gramFields[i]
                .text.toString()
                .toDoubleOrNull() ?: 0.0

            resultLabels[i].text = when {
                dry <= 0 -> "—"
                cooked == null -> "—"
                totalDry <= 0 -> "—"
                else -> {
                    "${(cooked * dry / totalDry).roundToInt()} г"
                }
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

            setPadding(dp(8), 0, dp(8), 0)

            showSoftInputOnFocus = false
        }
    }

    private fun makeCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            setPadding(
                dp(14),
                dp(12),
                dp(14),
                dp(12)
            )

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

    private fun roundedBackground(
        color: Int,
        radius: Int
    ): GradientDrawable {
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

    private fun EditText.addTextChangedListenerSimple(
        action: () -> Unit
    ) {
        addTextChangedListener(
            object : android.text.TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    action()
                }

                override fun afterTextChanged(
                    s: android.text.Editable?
                ) {
                }
            }
        )
    }
}