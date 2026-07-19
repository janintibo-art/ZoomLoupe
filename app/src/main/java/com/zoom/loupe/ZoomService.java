package com.zoom.loupe;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.service.quicksettings.TileService;
import android.content.ComponentName;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ZoomService extends AccessibilityService {

    static ZoomService instance;

    private WindowManager wm;
    private SharedPreferences prefs;
    private TextView floatingBtn;
    private WindowManager.LayoutParams btnParams;
    private View touchPad;
    private LinearLayout toolbar;
    boolean zoomMode = false;
    private float scale = 1f;
    private float lastScale = 2f;
    private float centerX, centerY;
    private ScaleGestureDetector scaleDetector;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingTap;
    private long lastBtnTap = 0;

    // détection double-tap 2 doigts
    private long twoDownTime = 0, lastTwoTap = 0;
    private boolean twoMoved = false;
    private float twoX, twoY;

    /* ---------- Préférences ---------- */

    private float maxZoom()  { return prefs.getFloat("maxZoom", 6f); }
    private int btnSizePx()  { return dp(prefs.getInt("btnSize", 56)); }
    private float btnAlpha() { return prefs.getInt("btnOpacity", 90) / 100f; }
    private int dp(int v)    { return (int) (v * getResources().getDisplayMetrics().density); }

    @Override
    protected void onServiceConnected() {
        instance = this;
        prefs = getSharedPreferences("zoomloupe", MODE_PRIVATE);
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        lastScale = prefs.getFloat("lastScale", 2f);
        createFloatingButton();
        updateTile();
    }

    private void vibrate(int ms) {
        try {
            Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (v != null) v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
        } catch (Exception ignored) {}
    }

    private void updateTile() {
        try {
            TileService.requestListeningState(this,
                    new ComponentName(this, ZoomTileService.class));
        } catch (Exception ignored) {}
    }

    /* ---------- Bouton flottant ---------- */

    @SuppressLint("ClickableViewAccessibility")
    private void createFloatingButton() {
        floatingBtn = new TextView(this);
        floatingBtn.setText("🔍");
        floatingBtn.setGravity(Gravity.CENTER);
        int size = btnSizePx();
        floatingBtn.setTextSize(size / 6f);
        setBtnStyle();

        btnParams = new WindowManager.LayoutParams(
                size, size,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        btnParams.gravity = Gravity.TOP | Gravity.START;
        btnParams.x = prefs.getInt("btnX", 30);
        btnParams.y = prefs.getInt("btnY", 300);

        floatingBtn.setOnTouchListener(new View.OnTouchListener() {
            private int startX, startY;
            private float touchX, touchY;
            private boolean moved;

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = btnParams.x; startY = btnParams.y;
                        touchX = e.getRawX(); touchY = e.getRawY();
                        moved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) (e.getRawX() - touchX);
                        int dy = (int) (e.getRawY() - touchY);
                        if (Math.abs(dx) > 20 || Math.abs(dy) > 20) moved = true;
                        btnParams.x = startX + dx;
                        btnParams.y = startY + dy;
                        wm.updateViewLayout(floatingBtn, btnParams);
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (moved) {
                            // mémorise la position
                            prefs.edit().putInt("btnX", btnParams.x)
                                        .putInt("btnY", btnParams.y).apply();
                        } else {
                            handleBtnTap();
                        }
                        return true;
                }
                return false;
            }
        });

        wm.addView(floatingBtn, btnParams);
    }

    /** simple tap = toggle, double tap = retour ×1 */
    private void handleBtnTap() {
        long now = System.currentTimeMillis();
        if (now - lastBtnTap < 300) {
            lastBtnTap = 0;
            if (pendingTap != null) handler.removeCallbacks(pendingTap);
            setScaleTo(1f, true);
            vibrate(60);
        } else {
            lastBtnTap = now;
            pendingTap = this::toggleZoomMode;
            handler.postDelayed(pendingTap, 280);
        }
    }

    private void setBtnStyle() {
        GradientDrawable bg;
        if (zoomMode) {
            bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    new int[]{0xFF43A047, 0xFF1B5E20});
        } else {
            bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    new int[]{0xFF6C3BF7, 0xFF2196F3});
        }
        bg.setShape(GradientDrawable.OVAL);
        bg.setStroke(3, 0x66FFFFFF);
        floatingBtn.setBackground(bg);
        floatingBtn.setTextColor(Color.WHITE);
        floatingBtn.setAlpha(btnAlpha());
    }

    /** appelé par SettingsActivity quand les réglages changent */
    void refreshFromPrefs() {
        if (floatingBtn != null) {
            int size = btnSizePx();
            btnParams.width = size;
            btnParams.height = size;
            floatingBtn.setTextSize(size / 6f);
            setBtnStyle();
            try { wm.updateViewLayout(floatingBtn, btnParams); } catch (Exception ignored) {}
        }
        if (scale > maxZoom()) setScaleTo(maxZoom(), true);
    }

    /* ---------- Mode zoom ---------- */

    void toggleZoomMode() {
        zoomMode = !zoomMode;
        vibrate(30);
        if (zoomMode) {
            addTouchPad();
            addToolbar();
            raiseButton();
        } else {
            removeTouchPad();
            removeToolbar();
        }
        setBtnStyle();
        updateTile();
    }

    private void raiseButton() {
        try { wm.removeView(floatingBtn); } catch (Exception ignored) {}
        try { wm.addView(floatingBtn, btnParams); } catch (Exception ignored) {}
    }

    @SuppressLint("ClickableViewAccessibility")
    private void addTouchPad() {
        touchPad = new View(this);
        touchPad.setBackgroundColor(0x11000000);

        scaleDetector = new ScaleGestureDetector(this,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector d) {
                        scale *= d.getScaleFactor();
                        scale = Math.max(1f, Math.min(maxZoom(), scale));
                        if (scale > 1.05f) saveLastScale(scale);
                        centerX = d.getFocusX();
                        centerY = d.getFocusY();
                        applyMagnification(false);
                        return true;
                    }
                });

        touchPad.setOnTouchListener(new View.OnTouchListener() {
            private float lastX, lastY;

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                scaleDetector.onTouchEvent(e);
                long now = System.currentTimeMillis();

                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_POINTER_DOWN:
                        if (e.getPointerCount() == 2) {
                            twoDownTime = now;
                            twoMoved = false;
                            twoX = e.getX(0); twoY = e.getY(0);
                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (e.getPointerCount() >= 2) {
                            if (Math.abs(e.getX(0) - twoX) > 40
                                    || Math.abs(e.getY(0) - twoY) > 40) twoMoved = true;
                        } else if (e.getPointerCount() == 1 && scale > 1f
                                && !scaleDetector.isInProgress()) {
                            centerX -= (e.getRawX() - lastX) / scale;
                            centerY -= (e.getRawY() - lastY) / scale;
                            lastX = e.getRawX(); lastY = e.getRawY();
                            applyMagnification(false);
                        }
                        break;
                    case MotionEvent.ACTION_DOWN:
                        lastX = e.getRawX(); lastY = e.getRawY();
                        break;
                    case MotionEvent.ACTION_POINTER_UP:
                        // fin d'un tap à 2 doigts ?
                        if (e.getPointerCount() == 2 && !twoMoved
                                && now - twoDownTime < 250) {
                            if (now - lastTwoTap < 450) {
                                // double-tap 2 doigts : bascule ×1 <-> dernier zoom
                                lastTwoTap = 0;
                                if (scale > 1f) {
                                    saveLastScale(scale);
                                    setScaleTo(1f, true);
                                } else {
                                    centerX = e.getX(0);
                                    centerY = e.getY(0);
                                    setScaleTo(lastScale, true);
                                }
                                vibrate(50);
                            } else {
                                lastTwoTap = now;
                            }
                        }
                        break;
                }
                return true;
            }
        });

        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        wm.addView(touchPad, p);
    }

    /* ---------- Barre d'outils − / ×1 / + ---------- */

    private void addToolbar() {
        toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xE61E1E2A);
        bg.setCornerRadius(dp(28));
        bg.setStroke(2, 0x662196F3);
        toolbar.setBackground(bg);
        toolbar.setPadding(dp(8), dp(4), dp(8), dp(4));

        toolbar.addView(toolBtn("−", v -> { setScaleTo(scale - 0.5f, true); vibrate(15); }));
        toolbar.addView(toolBtn("×1", v -> { setScaleTo(1f, true); vibrate(40); }));
        toolbar.addView(toolBtn("+", v -> { setScaleTo(scale + 0.5f, true); vibrate(15); }));

        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        p.y = dp(40);
        wm.addView(toolbar, p);
    }

    private TextView toolBtn(String label, View.OnClickListener action) {
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextSize(22);
        t.setTextColor(Color.WHITE);
        t.setGravity(Gravity.CENTER);
        t.setWidth(dp(56));
        t.setHeight(dp(48));
        t.setOnClickListener(action);
        return t;
    }

    private void removeToolbar() {
        if (toolbar != null) {
            try { wm.removeView(toolbar); } catch (Exception ignored) {}
            toolbar = null;
        }
    }

    private void removeTouchPad() {
        if (touchPad != null) {
            try { wm.removeView(touchPad); } catch (Exception ignored) {}
            touchPad = null;
        }
    }

    /* ---------- Magnification ---------- */

    private void saveLastScale(float s) {
        lastScale = s;
        prefs.edit().putFloat("lastScale", s).apply();
    }

    private void setScaleTo(float s, boolean animate) {
        scale = Math.max(1f, Math.min(maxZoom(), s));
        if (scale > 1.05f) saveLastScale(scale);
        applyMagnification(animate);
    }

    private void applyMagnification(boolean animate) {
        try {
            MagnificationController mc = getMagnificationController();
            mc.setScale(scale, animate);
            if (scale > 1f) mc.setCenter(centerX, centerY, animate);
        } catch (Exception ignored) {}
    }

    /* ---------- Cycle de vie ---------- */

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) { }

    @Override
    public void onInterrupt() { }

    @Override
    public boolean onUnbind(android.content.Intent intent) {
        try { getMagnificationController().reset(false); } catch (Exception ignored) {}
        removeTouchPad();
        removeToolbar();
        if (floatingBtn != null) {
            try { wm.removeView(floatingBtn); } catch (Exception ignored) {}
        }
        instance = null;
        updateTile();
        return super.onUnbind(intent);
    }
}
