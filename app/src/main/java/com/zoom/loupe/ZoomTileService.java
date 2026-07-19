package com.zoom.loupe;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class ZoomTileService extends TileService {

    @Override
    public void onStartListening() {
        refresh();
    }

    @Override
    public void onClick() {
        ZoomService s = ZoomService.instance;
        if (s == null) {
            // service d'accessibilité pas activé : ouvrir les réglages
            Intent i = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (Build.VERSION.SDK_INT >= 34) {
                startActivityAndCollapse(PendingIntent.getActivity(
                        this, 0, i, PendingIntent.FLAG_IMMUTABLE));
            } else {
                startActivityAndCollapse(i);
            }
            return;
        }
        s.toggleZoomMode();
        refresh();
    }

    private void refresh() {
        Tile t = getQsTile();
        if (t == null) return;
        ZoomService s = ZoomService.instance;
        if (s == null) {
            t.setState(Tile.STATE_UNAVAILABLE);
            t.setSubtitle("Service désactivé");
        } else if (s.zoomMode) {
            t.setState(Tile.STATE_ACTIVE);
            t.setSubtitle("Zoom actif");
        } else {
            t.setState(Tile.STATE_INACTIVE);
            t.setSubtitle("Prêt");
        }
        t.updateTile();
    }
}
