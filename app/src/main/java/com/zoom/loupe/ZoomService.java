package com.zoom.loupe;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;

public class ZoomService extends AccessibilityService {

    private WindowManager wm;
    private TextView floatingBtn;
    private View touchPad;                 // couche plein écran qui capte le pincement
    private boolean zoomMode = false;
    private float scale = 1f;
    private float centerX, centerY;
    private ScaleGestureDetector scaleDetector;

    private static final float MIN_SCALE = 1f;
    private static final float MAX_SCALE = 6f;

    @Override
    protected void onServiceConnected() {
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        createFloatingButton();
    }

    /* ---------- Bouton flottant 🔍 (déplaçable, toggle) ---------- */

    @SuppressLint("ClickableViewAccessibility")
    private void createFloatingButton() {
        floatingBtn = new TextView(this);
        floatingBtn.setText("🔍");
        floatingBtn.setTextSize(26);
        floatingBtn.setGravity(Gravity.CENTER);
        setBtnColor(0xCC333333);

        final WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                150, 150,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.START;
        p.x = 30;
        p.y = 300;

        floatingBtn.setOnTouchListener(new View.OnTouchListener() {
            private int startX, startY;
            private float touchX, touchY;
            private boolean moved;

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        startX = p.x; startY = p.y;
                        touchX = e.getRawX(); touchY = e.getRawY();
                        moved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) (e.getRawX() - touchX);
                        int dy = (int) (e.getRawY() - touchY);
                        if (Math.abs(dx) > 20 || Math.abs(dy) > 20) moved = true;
                        p.x = startX + dx;
                        p.y = startY + dy;
                        wm.updateViewLayout(floatingBtn, p);
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!moved) toggleZoomMode();
                        return true;
                }
                return false;
            }
        });

        wm.addView(floatingBtn, p);
    }

    private void setBtnColor(int color) {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(color);
        floatingBtn.setBackground(bg);
        floatingBtn.setTextColor(Color.WHITE);
    }

    /* ---------- Mode zoom : couche tactile plein écran ---------- */

    private void toggleZoomMode() {
        zoomMode = !zoomMode;
        if (zoomMode) {
            setBtnColor(0xCC2E7D32);   // vert = actif
            addTouchPad();
        } else {
            setBtnColor(0xCC333333);   // gris = inactif
            removeTouchPad();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void addTouchPad() {
        touchPad = new View(this);
        touchPad.setBackgroundColor(0x11000000); // très léger voile pour indiquer le mode

        scaleDetector = new ScaleGestureDetector(this,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector d) {
                        scale *= d.getScaleFactor();
                        scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, scale));
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
                // Glissement à 1 doigt = déplacement de la vue zoomée
                if (e.getPointerCount() == 1 && scale > 1f) {
                    switch (e.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            lastX = e.getRawX(); lastY = e.getRawY();
                            break;
                        case MotionEvent.ACTION_MOVE:
                            centerX -= (e.getRawX() - lastX) / scale;
                            centerY -= (e.getRawY() - lastY) / scale;
                            lastX = e.getRawX(); lastY = e.getRawY();
                            applyMagnification(false);
                            break;
                    }
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

        // Le bouton doit rester au-dessus de la couche tactile
        wm.removeView(floatingBtn);
        createFloatingButtonAgain();
    }

    private void createFloatingButtonAgain() {
        // recrée le bouton par-dessus le touchPad
        TextView old = floatingBtn;
        createFloatingButton();
        if (zoomMode) setBtnColor(0xCC2E7D32);
    }

    private void removeTouchPad() {
        if (touchPad != null) {
            try { wm.removeView(touchPad); } catch (Exception ignored) {}
            touchPad = null;
        }
    }

    private void applyMagnification(boolean animate) {
        MagnificationController mc = getMagnificationController();
        mc.setScale(scale, animate);
        if (scale > 1f) {
            mc.setCenter(centerX, centerY, animate);
        }
    }

    /* ---------- Cycle de vie ---------- */

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) { }

    @Override
    public void onInterrupt() { }

    @Override
    public boolean onUnbind(android.content.Intent intent) {
        // remise à zéro du zoom et nettoyage
        try { getMagnificationController().reset(false); } catch (Exception ignored) {}
        removeTouchPad();
        if (floatingBtn != null) {
            try { wm.removeView(floatingBtn); } catch (Exception ignored) {}
        }
        return super.onUnbind(intent);
    }
}
