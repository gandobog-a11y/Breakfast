
package ru.example.breakfast;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(32, 32, 32, 32);
        layout.setBackgroundColor(Color.rgb(250, 247, 239));

        TextView title = new TextView(this);
        title.setText("Моя каша");
        title.setTextSize(30);
        title.setTextColor(Color.rgb(75, 58, 40));
        title.setGravity(Gravity.CENTER);

        TextView subtitle = new TextView(this);
        subtitle.setText("Первый запуск удался!\nДальше добавим нужные функции.");
        subtitle.setTextSize(18);
        subtitle.setTextColor(Color.rgb(90, 82, 72));
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 24, 0, 0);

        layout.addView(title);
        layout.addView(subtitle);
        setContentView(layout);
    }
}
