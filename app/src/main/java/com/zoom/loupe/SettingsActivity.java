package com.zoom.loupe;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

public class SettingsActivity extends Activity {

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("zoomloupe", MODE_PRIVATE);

        GradientDrawable fond = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF1A1030, 0xFF12121A});

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(fond);
        root.setPadding(dp(24), dp(48), dp(24), dp(24));

        TextView titre = new TextView(this);
        titre.setText("🎛  Réglages");
        titre.setTextSize(28);
        titre.setTextColor(Color.WHITE);
        titre.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        titre.setPadding(0, 0, 0, dp(24));
        root.addView(titre);

        // Zoom maximum : 2.0 -> 10.0 (pas de 0.5)
        float mz = prefs.getFloat("maxZoom", 6f);
        root.addView(reglage("Zoom maximum", "×" + fmt(mz), 16,
                (int) ((mz - 2f) * 2), (progress, valueView) -> {
                    float v = 2f + progress * 0.5f;
                    valueView.setText("×" + fmt(v));
                    prefs.edit().putFloat("maxZoom", v).apply();
                }));

        // Taille du bouton : 40 -> 80 dp
        int bs = prefs.getInt("btnSize", 56);
        root.addView(reglage("Taille du bouton flottant", bs + " dp", 40,
                bs - 40, (progress, valueView) -> {
                    int v = 40 + progress;
                    valueView.setText(v + " dp");
                    prefs.edit().putInt("btnSize", v).apply();
                }));

        // Opacité du bouton : 30 -> 100 %
        int op = prefs.getInt("btnOpacity", 90);
        root.addView(reglage("Opacité du bouton", op + " %", 70,
                op - 30, (progress, valueView) -> {
                    int v = 30 + progress;
                    valueView.setText(v + " %");
                    prefs.edit().putInt("btnOpacity", v).apply();
                }));

        TextView note = new TextView(this);
        note.setText("Les changements s'appliquent immédiatement.\nLa position du bouton et le dernier niveau de zoom sont mémorisés automatiquement.");
        note.setTextSize(13);
        note.setTextColor(0xFF9E9EB8);
        note.setPadding(dp(4), dp(12), dp(4), 0);
        root.addView(note);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackground(fond);
        scroll.addView(root);
        setContentView(scroll);
    }

    interface OnChange { void run(int progress, TextView valueView); }

    private LinearLayout reglage(String label, String valeurInitiale,
                                 int max, int progress, OnChange onChange) {
        LinearLayout carte = new LinearLayout(this);
        carte.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF1E1E2A);
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), 0xFF2E2E42);
        carte.setBackground(bg);
        carte.setPadding(dp(18), dp(16), dp(18), dp(16));

        LinearLayout ligne = new LinearLayout(this);
        ligne.setOrientation(LinearLayout.HORIZONTAL);

        TextView l = new TextView(this);
        l.setText(label);
        l.setTextSize(16);
        l.setTextColor(Color.WHITE);
        l.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView val = new TextView(this);
        val.setText(valeurInitiale);
        val.setTextSize(16);
        val.setTextColor(0xFF64B5F6);
        val.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        val.setGravity(Gravity.END);

        ligne.addView(l);
        ligne.addView(val);

        SeekBar sb = new SeekBar(this);
        sb.setMax(max);
        sb.setProgress(progress);
        sb.setPadding(dp(4), dp(12), dp(4), dp(4));
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                onChange.run(p, val);
                if (ZoomService.instance != null)
                    ZoomService.instance.refreshFromPrefs();
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        carte.addView(ligne);
        carte.addView(sb);

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        p.bottomMargin = dp(12);
        carte.setLayoutParams(p);
        return carte;
    }

    private String fmt(float f) {
        return (f == Math.floor(f)) ? String.valueOf((int) f) : String.valueOf(f);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
