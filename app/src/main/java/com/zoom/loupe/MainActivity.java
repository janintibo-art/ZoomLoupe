package com.zoom.loupe;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(60, 60, 60, 60);
        root.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("🔍 Zoom Loupe");
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);

        TextView info = new TextView(this);
        info.setText("\n1. Appuyez sur le bouton ci-dessous\n"
                + "2. Trouvez « Zoom Loupe » dans la liste\n"
                + "3. Activez le service\n\n"
                + "Un bouton flottant 🔍 apparaîtra.\n"
                + "Touchez-le : il devient vert = mode zoom.\n"
                + "Pincez avec 2 doigts pour zoomer,\n"
                + "glissez avec 1 doigt pour déplacer la vue.\n"
                + "Retouchez le bouton pour reprendre\n"
                + "le contrôle normal de l'écran.\n");
        info.setTextSize(16);

        Button btn = new Button(this);
        btn.setText("Ouvrir les réglages d'accessibilité");
        btn.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));

        root.addView(title);
        root.addView(info);
        root.addView(btn);
        setContentView(root);
    }
}
