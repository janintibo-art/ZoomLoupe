# 🔍 Zoom Loupe

Application Android native (Java, sans dépendances) qui permet de **zoomer tout l'écran** avec un pincement à 2 doigts, via l'API de magnification du service d'accessibilité.

## Fonctionnement
1. Installer l'APK, ouvrir l'app, activer le service dans les réglages d'accessibilité.
2. Un bouton flottant 🔍 apparaît (déplaçable).
3. Toucher le bouton → il devient **vert** : mode zoom actif.
   - Pincer à 2 doigts = zoomer / dézoomer (jusqu'à x6)
   - Glisser à 1 doigt = déplacer la zone zoomée
4. Retoucher le bouton → gris : l'écran redevient utilisable normalement (le zoom reste appliqué tant que vous ne le remettez pas à x1).

> Note : Android ne permet pas de capter un pincement **et** de laisser passer les touches aux apps en même temps. D'où le mode toggle via le bouton flottant.

## Compilation (GitHub Actions)
Chaque `push` déclenche le workflow `.github/workflows/build.yml` qui produit `app-debug.apk` téléchargeable dans l'onglet **Actions → Artifacts**.

Compatibilité : Android 8.0 (API 26) et +.
