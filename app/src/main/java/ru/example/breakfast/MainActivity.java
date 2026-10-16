package ru.gandobog.kasha

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.text.InputType

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("kasha", Context.MODE_PRIVATE) }
    private val defaultNames = listOf("Я", "Порция 2", "Порция 3", "Порция 4", "Порция 5")
    private val names = MutableList(5) { i -> prefs.getString("name_$i", defaultNames[i]) ?: defaultNames[i] }
    private val dry = MutableList(5) { i -> prefs.getString("dry_$i", "") ?: "" }
    private var potFull = prefs.getString("pot_full", "") ?: ""
    private var emptyPot = prefs.getString("empty_pot", "0") ?: "0"
    private var losses = prefs.getString("losses", "0") ?: "0"
    private var selected = 0
    private lateinit var root: LinearLayout
    private lateinit var dryViews: List<TextView>
    private lateinit var cookedViews: List<TextView>
    private lateinit var potView: TextView
    private lateinit var keypad: LinearLayout

    private val ink = Color.rgb(35, 42, 48)
    private val muted = Color.rgb(100, 110, 118)
    private val paper = Color.rgb(248, 249, 247)
    private val green = Color.rgb(39, 105, 75)
    private val border = Color.rgb(220, 225, 220)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = green
        window.navigationBarColor = Color.rgb(25, 65, 48)
        buildUi()
        refresh()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density + .5f).toInt()
    private fun bg(color: Int, radius: Int = 10, stroke: Int = 0) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat(); if (stroke != 0) setStroke(dp(1), stroke)
    }
    private fun text(s: String, size: Float, color: Int = ink, bold: Boolean = false) = TextView(this).apply {
        text = s; textSize = size; setTextColor(color); gravity = Gravity.CENTER_VERTICAL
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }
    private fun makeButton(s: String, action: () -> Unit, primary: Boolean = false) = TextView(this).apply {
        text = s; textSize = if (s == "Ввод") 17f else 24f; gravity = Gravity.CENTER
        setTextColor(if (primary) Color.WHITE else if (s == "⌫") Color.rgb(155, 50, 40) else ink)
        background = bg(if (primary) green else Color.WHITE, 10, if (primary) 0 else border)
        isClickable = true; isFocusable = true; setOnClickListener { action() }
    }

    private fun buildUi() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(5), dp(12), dp(6)); setBackgroundColor(paper)
        }
        setContentView(root)
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        header.addView(text("Моя каша", 25f, green, true), LinearLayout.LayoutParams(0, dp(44), 1f))
        header.addView(makeButton("⚙", { settings() }).apply { textSize = 24f }, LinearLayout.LayoutParams(dp(48), dp(40)))
        root.addView(header)
        root.addView(text("Сухая крупа и готовая каша", 12f, muted), LinearLayout.LayoutParams(-1, dp(22)))

        val headings = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(5), 0, dp(5), 0) }
        headings.addView(text("Порция", 12f, muted, true), LinearLayout.LayoutParams(0, dp(25), 1.1f))
        headings.addView(text("Сухая, г", 12f, muted, true).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(0, dp(25), 1f))
        headings.addView(text("Готовая, г", 12f, muted, true).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(0, dp(25), 1f))
        root.addView(headings)

        val dryList = mutableListOf<TextView>(); val cookedList = mutableListOf<TextView>()
        repeat(5) { i ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; background = bg(Color.WHITE, 9, border); setPadding(dp(6), dp(2), dp(6), dp(2)) }
            row.addView(text(names[i], 13f, ink, true).apply { maxLines = 2; setOnClickListener { selected = i; refresh() } }, LinearLayout.LayoutParams(0, -1, 1.1f))
            val d = text(dry[i].ifBlank { "—" }, 18f, green, true).apply { gravity = Gravity.CENTER; background = bg(Color.rgb(240,247,241), 8, border); setOnClickListener { selected = i; refresh() } }
            row.addView(d, LinearLayout.LayoutParams(0, dp(38), 1f).apply { setMargins(dp(2), 0, dp(2), 0) }); dryList.add(d)
            val c = text("—", 18f, ink, true).apply { gravity = Gravity.CENTER; maxLines = 1; setAutoSizeTextTypeWithDefaults(TextView.AUTO_SIZE_TEXT_TYPE_UNIFORM) }
            row.addView(c, LinearLayout.LayoutParams(0, dp(38), 1f)); cookedList.add(c)
            root.addView(row, LinearLayout.LayoutParams(-1, 0, 1f).apply { setMargins(0, dp(2), 0, dp(2)) })
        }
        dryViews = dryList; cookedViews = cookedList

        val potRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; background = bg(Color.rgb(232,241,234), 10); setPadding(dp(8), dp(3), dp(8), dp(3)) }
        potRow.addView(text("Кастрюля с кашей, г", 13f, ink, true), LinearLayout.LayoutParams(0, dp(42), 1f))
        potView = text(potFull.ifBlank { "Ввести" }, 17f, green, true).apply { gravity = Gravity.CENTER; background = bg(Color.WHITE, 8, border); setOnClickListener { selected = 5; refresh() } }
        potRow.addView(potView, LinearLayout.LayoutParams(dp(112), dp(36)))
        root.addView(potRow, LinearLayout.LayoutParams(-1, dp(48)).apply { setMargins(0, dp(4), 0, dp(3)) })
        root.addView(text("Выбери поле и нажимай цифры", 11f, muted).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(-1, dp(19)))
        keypad = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(keypad, LinearLayout.LayoutParams(-1, 0, 2.5f))
        buildKeypad()
    }

    private fun buildKeypad() {
        keypad.removeAllViews()
        listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"), listOf("⌫","0","Ввод")).forEach { keys ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
            keys.forEach { key -> row.addView(makeButton(key, { press(key) }, key == "Ввод"), LinearLayout.LayoutParams(0, -1, 1f).apply { setMargins(dp(3), dp(3), dp(3), dp(3)) }) }
            keypad.addView(row, LinearLayout.LayoutParams(-1, 0, 1f))
        }
    }
    private fun press(key: String) {
        if (key == "Ввод") { selected = if (selected < 5) 5 else 0; refresh(); return }
        val old = if (selected == 5) potFull else dry[selected]
        val next = if (key == "⌫") old.dropLast(1) else (old + key).trimStart('0').ifEmpty { "0" }
        if (selected == 5) potFull = next else dry[selected] = next
        save(); refresh()
    }
    private fun refresh() {
        dryViews.forEachIndexed { i, v ->
            v.text = dry[i].ifBlank { "—" }
            v.background = bg(if (selected == i) Color.rgb(215,235,219) else Color.rgb(240,247,241), 8, if (selected == i) green else border)
        }
        potView.text = potFull.ifBlank { "Ввести" }; potView.background = bg(Color.WHITE, 8, if (selected == 5) green else border)
        val totalDry = dry.sumOf { it.toLongOrNull() ?: 0L }
        val cooked = (potFull.toLongOrNull() ?: 0L) - (emptyPot.toLongOrNull() ?: 0L) - (losses.toLongOrNull() ?: 0L)
        cookedViews.forEachIndexed { i, v ->
            val portion = dry[i].toLongOrNull() ?: 0L
            v.text = if (totalDry > 0 && cooked > 0 && portion > 0) (cooked * portion / totalDry).toString() else "—"
        }
    }
    private fun save() {
        prefs.edit().apply {
            names.forEachIndexed { i, s -> putString("name_$i", s) }; dry.forEachIndexed { i, s -> putString("dry_$i", s) }
            putString("pot_full", potFull); putString("empty_pot", emptyPot); putString("losses", losses)
        }.apply()
    }
    private fun settings() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(4), dp(20), dp(4)) }
        val nameEdits = mutableListOf<EditText>()
        repeat(5) { i ->
            val e = EditText(this).apply { setSingleLine(true); textSize = 16f; setText(names[i]); hint = "Название порции ${i+1}" }
            box.addView(e, LinearLayout.LayoutParams(-1, dp(48))); nameEdits.add(e)
        }
        val empty = EditText(this).apply { setSingleLine(true); inputType = InputType.TYPE_CLASS_NUMBER; setText(emptyPot); hint = "Вес пустой кастрюли, г" }
        box.addView(text("Вес пустой кастрюли, г", 12f, muted)); box.addView(empty, LinearLayout.LayoutParams(-1, dp(48)))
        val loss = EditText(this).apply { setSingleLine(true); inputType = InputType.TYPE_CLASS_NUMBER; setText(losses); hint = "Потери на стенках, г" }
        box.addView(text("Потери на стенках, г", 12f, muted)); box.addView(loss, LinearLayout.LayoutParams(-1, dp(48)))
        AlertDialog.Builder(this).setTitle("Настройки").setView(ScrollView(this).apply { addView(box) }).setNegativeButton("Отмена", null).setPositiveButton("Сохранить") { _, _ ->
            repeat(5) { i -> names[i] = nameEdits[i].text.toString().trim().ifBlank { "Порция ${i+1}" } }
            emptyPot = empty.text.toString().filter { it.isDigit() }.ifBlank { "0" }; losses = loss.text.toString().filter { it.isDigit() }.ifBlank { "0" }
            save(); buildUi(); refresh()
        }.show()
    }
}
