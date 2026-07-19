package com.zoom.loupe;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final int VIOLET = 0xFF6C3BF7;
    private static final int BLEU   = 0xFF2196F3;
    private static final int FOND   = 0xFF12121A;
    private static final int CARTE  = 0xFF1E1E2A;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Fond dégradé sombre
        GradientDrawable fond = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF1A1030, FOND});

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackground(fond);
        root.setPadding(dp(24), dp(48), dp(24), dp(24));

        // En-tête
        TextView logo = new TextView(this);
        logo.setText("🔍");
        logo.setTextSize(64);
        logo.setGravity(Gravity.CENTER);

        TextView titre = new TextView(this);
        titre.setText("Zoom Loupe");
        titre.setTextSize(32);
        titre.setTextColor(Color.WHITE);
        titre.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        titre.setGravity(Gravity.CENTER);

        TextView sousTitre = new TextView(this);
        sousTitre.setText("Zoomez tout votre écran à 2 doigts");
        sousTitre.setTextSize(15);
        sousTitre.setTextColor(0xFF9E9EB8);
        sousTitre.setGravity(Gravity.CENTER);
        sousTitre.setPadding(0, dp(4), 0, dp(24));

        root.addView(logo);
        root.addView(titre);
        root.addView(sousTitre);

        // Cartes d'étapes
        root.addView(carte("1️⃣  Activer le service",
                "Appuyez sur le bouton ci-dessous, trouvez « Zoom Loupe » dans la liste et activez-le.\n\n⚠️ Si « Paramètre restreint » s'affiche : Paramètres → Applications → Zoom Loupe → menu ⋮ → Autoriser les paramètres restreints."));

        root.addView(carte("2️⃣  Le bouton flottant",
                "Un bouton 🔍 apparaît sur l'écran. Vous pouvez le déplacer où vous voulez en le faisant glisser."));

        root.addView(carte("3️⃣  Zoomer",
                "Touchez le bouton → il devient vert 🟢 :\n• Pincez à 2 doigts pour zoomer (jusqu'à ×6)\n• Glissez à 1 doigt pour déplacer la vue\n\nRetouchez le bouton pour reprendre le contrôle normal de l'écran."));

        // Bouton principal
        Button btn = new Button(this);
        btn.setText("⚙️  Ouvrir l'accessibilité");
        btn.setTextSize(17);
        btn.setTextColor(Color.WHITE);
        btn.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        btn.setAllCaps(false);
        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{VIOLET, BLEU});
        bg.setCornerRadius(dp(28));
        btn.setBackground(bg);
        btn.setPadding(dp(24), dp(14), dp(24), dp(14));
        btn.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));

        LinearLayout.LayoutParams pBtn = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(56));
        pBtn.topMargin = dp(20);
        root.addView(btn, pBtn);

        TextView version = new TextView(this);
        version.setText("v1.1 • sans pub, sans internet");
        version.setTextSize(12);
        version.setTextColor(0xFF55556A);
        version.setGravity(Gravity.CENTER);
        version.setPadding(0, dp(16), 0, 0);
        root.addView(version);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackground(fond);
        scroll.addView(root);
        setContentView(scroll);
    }

    private View carte(String titre, String texte) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARTE);
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), 0xFF2E2E42);
        c.setBackground(bg);
        c.setPadding(dp(18), dp(16), dp(18), dp(16));

        TextView t = new TextView(this);
        t.setText(titre);
        t.setTextSize(16);
        t.setTextColor(Color.WHITE);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));

        TextView x = new TextView(this);
        x.setText(texte);
        x.setTextSize(14);
        x.setTextColor(0xFFB8B8CC);
        x.setLineSpacing(dp(3), 1f);
        x.setPadding(0, dp(6), 0, 0);

        c.addView(t);
        c.addView(x);

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        p.bottomMargin = dp(12);
        c.setLayoutParams(p);
        return c;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
