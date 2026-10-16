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
import android.view.inputmethod.InputMethodManager
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

        prefs = getSharedPreferences(
            "porridge_settings",
            MODE_PRIVATE
        )

        buildScreen()
        restoreValues()
        recalculate()
    }

    private fun buildScreen() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(4), dp(12), dp(4))
            setBackgroundColor(backgroundColor)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val title = TextView(this).apply {
            text = "Моя каша"
            textSize = 26f
            setTextColor(textColor)
            typeface = Typeface.DEFAULT_BOLD
        }

        val settingsButton = TextView(this).apply {
            text = "⚙"
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(accentColor)
            contentDescription = "Настройки"
            setOnClickListener {
                showSettings()
            }
        }

        header.addView(
            title,
            LinearLayout.LayoutParams(
                0,
                dp(46),
                1f
            )
        )

        header.addView(
            settingsButton,
            LinearLayout.LayoutParams(
                dp(48),
                dp(46)
            )
        )

        root.addView(header)

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

        // Учитываем нижнюю системную панель Android.
        // Это оставляет цифровую клавиатуру выше системных кнопок.
        root.setOnApplyWindowInsetsListener { view, insets ->

            val bottomInset = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                insets.getInsets(
                    WindowInsets.Type.systemBars()
                ).bottom
            } else {
                @Suppress("DEPRECATION")
                insets.systemWindowInsetBottom
            }

            view.setPadding(
                dp(12),
                dp(4),
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
            dp(6),
            dp(12),
            dp(6)
        )

        addLabel(
            card,
            "ВЕС КАСТРЮЛИ С КАШЕЙ, г",
            12f,
            mutedColor
        )

        potField = makeNumberField().apply {
            hint = "Например, 850"
            textSize = 23f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER_VERTICAL
        }

        potField.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                showKeypad(view as EditText)
            }
        }

        card.addView(
            potField,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(44)
            )
        )

        content.addView(
            card,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(6)
            }
        )

        potField.addTextChangedListenerSimple {
            if (!rebuilding) {
                recalculate()
            }
        }
    }

    private fun buildPeopleCards() {
        val heading = TextView(this).apply {
            text = "Кому сколько?"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textColor)
        }

        content.addView(
            heading,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(28)
            )
        )

        // Заголовки трёх колонок.
        val columns = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), 0, dp(10), 0)
        }

        columns.addView(
            TextView(this).apply {
                text = "Имя"
                textSize = 12f
                setTextColor(mutedColor)
            },
            LinearLayout.LayoutParams(
                0,
                dp(24),
                1f
            )
        )

        columns.addView(
            TextView(this).apply {
                text = "Крупа"
                textSize = 12f
                gravity = Gravity.CENTER
                setTextColor(mutedColor)
            },
            LinearLayout.LayoutParams(
                dp(64),
                dp(24)
            )
        )

        columns.addView(
            TextView(this).apply {
                text = "Каша"
                textSize = 12f
                gravity = Gravity.END or Gravity.CENTER_VERTICAL
                setTextColor(mutedColor)
            },
            LinearLayout.LayoutParams(
                dp(80),
                dp(24)
            )
        )

        content.addView(columns)

        for (i in defaultNames.indices) {

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL

                background = roundedBackground(
                    cardColor,
                    10
                )

                setPadding(
                    dp(10),
                    0,
                    dp(10),
                    0
                )
            }

            // Имя едока.
            val name = EditText(this).apply {
                textSize = 16f
                setTextColor(textColor)
                setSingleLine(true)
                setPadding(0, 0, dp(3), 0)
                background = null
                hint = "Имя"
                setSelectAllOnFocus(true)
                imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
            }

            // Количество сухой крупы.
            val grams = makeNumberField().apply {
                textSize = 17f
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 0)
                hint = "0"
                background = null
            }

            // Расчётная порция готовой каши.
            val result = TextView(this).apply {
                text = "—"
                textSize = 19f
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
                    dp(44),
                    1f
                )
            )

            row.addView(
                grams,
                LinearLayout.LayoutParams(
                    dp(64),
                    dp(42)
                )
            )

            row.addView(
                result,
                LinearLayout.LayoutParams(
                    dp(80),
                    dp(42)
                )
            )

            content.addView(
                row,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(4)
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
    }

    private fun buildKeypad() {
        keypad = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(2), 0, 0)
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
                        if (key == "Ввод") {
                            Color.WHITE
                        } else {
                            textColor
                        }
                    )

                    background = roundedBackground(
                        if (key == "Ввод") {
                            accentColor
                        } else {
                            cardColor
                        },
                        10
                    )

                    setPadding(
                        dp(2),
                        0,
                        dp(2),
                        0
                    )

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
                        dp(45),
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

        val imm = getSystemService(Context.INPUT_METHOD_SERVICE)
            as InputMethodManager

        imm.hideSoftInputFromWindow(
            field.windowToken,
            0
        )

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

        imm.hideSoftInputFromWindow(
            root.windowToken,
            0
        )

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
            field.setSelection(
                minOf(start, updated.length)
            )

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
            setPadding(
                dp(22),
                dp(8),
                dp(22),
                0
            )
        }

        val emptyInput = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(
                prefs.getInt("empty_pot", 0).toString()
            )
            setSingleLine(true)
            textSize = 18f
            showSoftInputOnFocus = false
        }

        val lossInput = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(
                prefs.getInt("loss", 0).toString()
            )
            setSingleLine(true)
            textSize = 18f
            showSoftInputOnFocus = false
        }

        // Постоянные подписи не исчезают при заполнении полей.
        addLabel(
            layout,
            "Вес пустой кастрюли, г",
            14f,
            textColor
        )

        layout.addView(
            emptyInput,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(46)
            )
        )

        addLabel(
            layout,
            "Потери при варке, г",
            14f,
            textColor
        )

        layout.addView(
            lossInput,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(46)
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
                )
            )

            gramFields[i].setText(
                prefs.getString(
                    "grams_$i",
                    defaultGrams[i].toString()
                )
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

            background = roundedBackground(
                cardColor,
                8
            )

            setPadding(
                dp(8),
                0,
                dp(8),
                0
            )

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

            background = roundedBackground(
                cardColor,
                16
            )
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
            setStroke(
                dp(1),
                borderColor
            )
        }
    }

    private fun dp(value: Int): Int {
        return (
            value * resources.displayMetrics.density
        ).roundToInt()
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