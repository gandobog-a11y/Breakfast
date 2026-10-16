package ru.example.breakfast;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Locale;

public class MainActivity extends Activity {

    private final int cream = Color.rgb(250, 247, 239);
    private final int brown = Color.rgb(75, 58, 40);
    private final int muted = Color.rgb(125, 112, 96);
    private final int green = Color.rgb(87, 119, 79);
    private final int white = Color.WHITE;

    private SharedPreferences prefs;
    private LinearLayout root;
    private LinearLayout peopleContainer;

    private final String[] defaultNames = {
        "Я", "Жена", "Ребёнок 1", "Ребёнок 2", "Ребёнок 3"
    };

    private final int[] defaultDry = {50, 30, 30, 20, 0};
    private final String[] names = new String[5];
    private final int[] dry = new int[5];
    private final int[] cooked = new int[5];

    private int potWeight = 1200;
    private int emptyPot = 0;
    private int losses = 0;

    private TextView potValue;
    private TextView totalValue;
    private TextView settingsSummary;

    private int editingPerson = -1;
    private String numberBuffer = "";
    private TextView keypadDisplay;
    private AlertDialog keypadDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("my_porridge", MODE_PRIVATE);
        loadData();
        buildScreen();
        recalculate();
    }

    private void loadData() {
        for (int i = 0; i < 5; i++) {
            names[i] = prefs.getString("name" + i, defaultNames[i]);
            dry[i] = prefs.getInt("dry" + i, defaultDry[i]);
        }

        potWeight = prefs.getInt("pot", 1200);
        emptyPot = prefs.getInt("empty", 0);
        losses = prefs.getInt("losses", 0);
    }

    private void saveData() {
        SharedPreferences.Editor e = prefs.edit();

        for (int i = 0; i < 5; i++) {
            e.putString("name" + i, names[i]);
            e.putInt("dry" + i, dry[i]);
        }

        e.putInt("pot", potWeight);
        e.putInt("empty", emptyPot);
        e.putInt("losses", losses);
        e.apply();
    }

    private GradientDrawable background(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private TextView text(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }

    private TextView centeredText(String value, int size, int color) {
        TextView t = text(value, size, color);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private LinearLayout vertical() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout horizontal() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    private LinearLayout.LayoutParams params(
            int width, int height) {
        return new LinearLayout.LayoutParams(width, height);
    }

    private LinearLayout.LayoutParams weighted() {
        return new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1
        );
    }

    private TextView label(String value) {
        TextView t = centeredText(value, 12, muted);
        t.setPadding(2, 4, 2, 4);
        return t;
    }

    private void buildScreen() {
        root = vertical();
        root.setBackgroundColor(cream);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout page = vertical();
        page.setPadding(18, 10, 18, 22);

        // Заголовок
        LinearLayout header = horizontal();

        TextView title = text("Моя каша", 29, brown);
        title.setTypeface(null, Typeface.BOLD);

        header.addView(title, weighted());

        TextView gear = centeredText("⚙", 29, brown);
        gear.setPadding(12, 4, 4, 4);
        gear.setOnClickListener(v -> showSettings());

        header.addView(gear, params(52, 52));
        page.addView(header);

        TextView subtitle = text(
            "Расчёт порций на всю семью", 14, muted
        );
        subtitle.setPadding(0, 0, 0, 16);
        page.addView(subtitle);

        // Таблица
        LinearLayout table = vertical();
        table.setPadding(12, 8, 12, 8);
        table.setBackground(background(white, 18));

        LinearLayout heading = horizontal();
        heading.addView(label("КТО"), weighted());
        heading.addView(label("СУХАЯ\nКРУПА"), params(86, 46));
        heading.addView(label("ГОТОВАЯ\nКАША"), params(92, 46));
        table.addView(heading);

        View divider = new View(this);
        divider.setBackgroundColor(Color.rgb(235, 229, 217));
        table.addView(divider, params(
            ViewGroup.LayoutParams.MATCH_PARENT, 1
        ));

        peopleContainer = vertical();
        table.addView(peopleContainer);

        page.addView(table);

        TextView hint = text(
            "Нажми на количество граммов, чтобы изменить порцию.",
            12, muted
        );
        hint.setPadding(4, 8, 4, 18);
        page.addView(hint);

        // Вес кастрюли
        LinearLayout potCard = vertical();
        potCard.setPadding(16, 14, 16, 14);
        potCard.setBackground(background(Color.rgb(239, 233, 219), 18));

        TextView potTitle = text("⚖  Вес кастрюли с кашей", 17, brown);
        potTitle.setTypeface(null, Typeface.BOLD);
        potCard.addView(potTitle);

        LinearLayout potRow = horizontal();

        TextView potHint = text(
            "Вместе с кастрюлей, в граммах", 12, muted
        );
        potRow.addView(potHint, weighted());

        potValue = centeredText("", 25, green);
        potValue.setTypeface(null, Typeface.BOLD);
        potValue.setPadding(8, 4, 8, 4);
        potValue.setBackground(background(white, 12));
        potValue.setOnClickListener(v -> showKeypad(-1));
        potRow.addView(potValue, params(110, 54));

        potCard.addView(potRow);
        page.addView(potCard);

        // Итог
        LinearLayout result = vertical();
        result.setPadding(16, 14, 16, 14);
        result.setBackground(background(green, 18));

        TextView resultTitle = text(
            "Готовой каши для распределения", 16, white
        );
        resultTitle.setTypeface(null, Typeface.BOLD);
        result.addView(resultTitle);

        totalValue = text("", 32, white);
        totalValue.setTypeface(null, Typeface.BOLD);
        totalValue.setPadding(0, 4, 0, 0);
        result.addView(totalValue);

        TextView grams = text("граммов", 13, white);
        result.addView(grams);

        page.addView(result, params(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        settingsSummary = centeredText("", 12, muted);
        settingsSummary.setPadding(4, 12, 4, 0);
        page.addView(settingsSummary);

        scroll.addView(page);
        root.addView(scroll, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1
        ));

        setContentView(root);
        renderPeople();
    }

    private void renderPeople() {
        peopleContainer.removeAllViews();

        for (int i = 0; i < 5; i++) {
            final int index = i;

            LinearLayout row = horizontal();
            row.setPadding(0, 8, 0, 8);

            TextView name = text(names[i], 16, brown);
            name.setTypeface(null, Typeface.BOLD);
            name.setSingleLine(false);
            name.setOnClickListener(v -> showSettings());

            row.addView(name, weighted());

            TextView dryValue = centeredText(
                dry[i] + " г", 18, green
            );
            dryValue.setTypeface(null, Typeface.BOLD);
            dryValue.setBackground(background(
                Color.rgb(240, 245, 235), 10
            ));
            dryValue.setPadding(3, 10, 3, 10);
            dryValue.setOnClickListener(v -> showKeypad(index));

            row.addView(dryValue, params(86, 46));

            TextView cookedValue = centeredText(
                dry[i] == 0 ? "—" : cooked[i] + " г",
                16, brown
            );
            row.addView(cookedValue, params(92, 46));

            peopleContainer.addView(row);

            if (i < 4) {
                View line = new View(this);
                line.setBackgroundColor(Color.rgb(240, 235, 226));
                peopleContainer.addView(line, params(
                    ViewGroup.LayoutParams.MATCH_PARENT, 1
                ));
            }
        }
    }

    private void recalculate() {
        int totalDry = 0;

        for (int value : dry) {
            totalDry += Math.max(0, value);
        }

        int ready = Math.max(0, potWeight - emptyPot - losses);

        // Сначала вычисляем все порции, округляя до целого грамма.
        int assigned = 0;

        for (int i = 0; i < 5; i++) {
            if (totalDry == 0 || dry[i] <= 0) {
                cooked[i] = 0;
            } else {
                cooked[i] = (int) Math.round(
                    (double) ready * dry[i] / totalDry
                );
                assigned += cooked[i];
            }
        }

        // Корректируем округление, чтобы сумма порций совпала
        // с весом готовой каши, если есть хотя бы одна порция.
        if (totalDry > 0) {
            int difference = ready - assigned;

            for (int i = 4; i >= 0; i--) {
                if (dry[i] > 0) {
                    cooked[i] += difference;
                    break;
                }
            }
        }

        if (potValue != null) {
            potValue.setText(potWeight + " г");
        }

        if (totalValue != null) {
            totalValue.setText(ready + " г");
        }

        if (settingsSummary != null) {
            settingsSummary.setText(
                "Пустая кастрюля: " + emptyPot
                + " г  •  Потери: " + losses + " г"
            );
        }

        if (peopleContainer != null) {
            renderPeople();
        }

        saveData();
    }

    private void showKeypad(int person) {
        editingPerson = person;

        int current = person == -1 ? potWeight : dry[person];
        numberBuffer = String.valueOf(current);

        LinearLayout panel = vertical();
        panel.setPadding(16, 16, 16, 16);
        panel.setBackgroundColor(cream);

        TextView caption = centeredText(
            person == -1
                ? "Вес кастрюли с кашей"
                : "Сухая крупа: " + names[person],
            18, brown
        );
        caption.setPadding(0, 0, 0, 12);
        panel.addView(caption);

        keypadDisplay = centeredText(numberBuffer + " г", 32, green);
        keypadDisplay.setTypeface(null, Typeface.BOLD);
        keypadDisplay.setBackground(background(white, 12));
        keypadDisplay.setPadding(8, 12, 8, 12);

        panel.addView(keypadDisplay, params(
            ViewGroup.LayoutParams.MATCH_PARENT, 64
        ));

        String[][] keys = {
            {"1", "2", "3"},
            {"4", "5", "6"},
            {"7", "8", "9"},
            {"⌫", "0", "Ввод"}
        };

        for (String[] keyRow : keys) {
            LinearLayout row = horizontal();
            row.setPadding(0, 4, 0, 4);

            for (String key : keyRow) {
                Button button = new Button(this);
                button.setText(key);
                button.setTextSize(20);
                button.setAllCaps(false);
                button.setTextColor(brown);
                button.setBackground(background(white, 12));

                LinearLayout.LayoutParams bp =
                    new LinearLayout.LayoutParams(
                        0, 58, 1
                    );
                bp.setMargins(3, 0, 3, 0);
                row.addView(button, bp);

                button.setOnClickListener(v -> {
                    if (key.equals("Ввод")) {
                        keypadDialog.dismiss();
                        return;
                    }

                    if (key.equals("⌫")) {
                        if (!numberBuffer.isEmpty()) {
                            numberBuffer = numberBuffer.substring(
                                0, numberBuffer.length() - 1
                            );
                        }
                    } else {
                        if (numberBuffer.length() < 7) {
                            if (numberBuffer.equals("0")) {
                                numberBuffer = key;
                            } else {
                                numberBuffer += key;
                            }
                        }
                    }

                    if (numberBuffer.isEmpty()) {
                        keypadDisplay.setText("0 г");
                    } else {
                        keypadDisplay.setText(numberBuffer + " г");
                    }

                    applyNumber();
                });
            }

            panel.addView(row);
        }

        keypadDialog = new AlertDialog.Builder(this)
            .setView(panel)
            .create();

        keypadDialog.show();

        if (keypadDialog.getWindow() != null) {
            keypadDialog.getWindow().setLayout(
                (int) (getResources().getDisplayMetrics().widthPixels
                    * 0.90),
                ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }

    private void applyNumber() {
        int value = 0;

        try {
            if (!numberBuffer.isEmpty()) {
                value = Integer.parseInt(numberBuffer);
            }
        } catch (NumberFormatException ignored) {
            value = 0;
        }

        value = Math.max(0, Math.min(value, 999999));

        if (editingPerson == -1) {
            potWeight = value;
        } else {
            dry[editingPerson] = value;
        }

        recalculate();
    }

    private void showSettings() {
        LinearLayout panel = vertical();
        panel.setPadding(20, 8, 20, 4);

        TextView title = text("Настройки", 24, brown);
        title.setTypeface(null, Typeface.BOLD);
        title.setPadding(0, 0, 0, 12);
        panel.addView(title);

        EditText[] nameInputs = new EditText[5];

        for (int i = 0; i < 5; i++) {
            TextView caption = text(
                "Имя едока " + (i + 1), 13, muted
            );
            panel.addView(caption);

            EditText input = new EditText(this);
            input.setSingleLine(true);
            input.setText(names[i]);
            input.setTextSize(16);
            input.setHint("Имя");
            nameInputs[i] = input;
            panel.addView(input, params(
                ViewGroup.LayoutParams.MATCH_PARENT, 48
            ));
        }

        TextView emptyLabel = text(
            "Вес пустой кастрюли, г", 13, muted
        );
        emptyLabel.setPadding(0, 10, 0, 0);
        panel.addView(emptyLabel);

        EditText emptyInput = numberInput(emptyPot);
        panel.addView(emptyInput);

        TextView lossLabel = text(
            "Потери при приготовлении, г", 13, muted
        );
        lossLabel.setPadding(0, 10, 0, 0);
        panel.addView(lossLabel);

        EditText lossInput = numberInput(losses);
        panel.addView(lossInput);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(panel);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setView(scroll)
            .setPositiveButton("Сохранить", null)
            .setNegativeButton("Отмена", null)
            .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    for (int i = 0; i < 5; i++) {
                        String n = nameInputs[i].getText()
                            .toString().trim();

                        names[i] = n.isEmpty()
                            ? defaultNames[i] : n;
                    }

                    emptyPot = readNumber(emptyInput, 0);
                    losses = readNumber(lossInput, 0);

                    saveData();
                    renderPeople();
                    recalculate();
                    dialog.dismiss();
                });
        });

        dialog.show();
    }

    private EditText numberInput(int value) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(String.valueOf(value));
        input.setTextSize(16);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setSelectAllOnFocus(true);
        return input;
    }

    private int readNumber(EditText input, int fallback) {
        try {
            return Math.max(0, Integer.parseInt(
                input.getText().toString().trim()
            ));
        } catch (Exception e) {
            return fallback;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveData();
    }
}