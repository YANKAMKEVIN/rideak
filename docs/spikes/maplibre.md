# Spike MapLibre — conclusions (2026-10-11)

Branche : `spike/maplibre`. Écran : onglet **Carte** (`feature/map/WorldMapScreen`).

## Question

Peut-on obtenir, en Compose Multiplatform, une vraie carte au style RIDEAK avec nos overlays
d'exploration (tracés, zones explorées, « brouillard », spots), fluide sur téléphone ?

## Réponse : oui — recommandation : adopter

| Critère | Résultat |
|---|---|
| Intégration CMP (Android + iOS) | ✅ `org.maplibre.compose:maplibre-compose:0.20.0`, une seule implémentation dans `commonMain` |
| Style RIDEAK | ✅ style JSON maison (`composeResources/files/rideak_style.json`), palette nuit/nature/eau, pistes cyclables en jaune |
| Tuiles | ✅ OpenFreeMap (schéma OpenMapTiles), gratuit, sans clé ; interchangeable (MapTiler, Protomaps auto-hébergé) |
| Overlays | ✅ GeoJSON en mémoire → `FillLayer` (brouillard troué + zones), `LineLayer` (tracé + halo), `CircleLayer` (spots) |
| Effet « ton monde se dévoile » | ✅ un seul polygone monde avec un trou par cellule explorée : rendu propre, cellules adjacentes sans couture |
| Performance (Galaxy S21+, OpenGL, Adreno 660) | ✅ 1 327 zones + tracé de 45 000 points : 3 % d'images UI en retard, p99 27 ms ; 1ʳᵉ image ~200 ms ; ~276 Mo PSS |
| Attribution | ✅ « OpenFreeMap © OpenMapTiles Data from OpenStreetMap » affichée par défaut |
| Crash | aucun (Android) |

Les mesures UI (`dumpsys gfxinfo`) couvrent l'interface Compose ; la carte est rendue sur son propre fil
(`maplibre-compose-render`), elle ne bloque donc pas l'UI. Ressenti fluide à confirmer à vélo sur le S24.

## Coût de l'adoption

- **Montée de stack obligatoire** : Kotlin 2.3.21 → **2.4.21**, Compose MP 1.11 → **1.12.1**, AGP 9.0.1 → **9.1.1**,
  compileSdk 36 → **37**, lifecycle 2.11.0, material3 1.12.0-alpha03. Tout le projet compile et les tests passent.
- **iOS** : cible minimale 15.3 → 15.5 et options de liaison MapLibre (`-lc++ -lz` + frameworks Metal, CoreText…)
  dans le projet Xcode. Pas de CocoaPods ni SPM.

## Risques identifiés

1. **Bibliothèque jeune** (0.x, API changée entre versions récentes) : figer la version, encapsuler l'usage
   dans `feature/map` (déjà le cas) pour limiter l'impact des montées.
2. **Deux SQLite sur iOS** : MapLibre embarque SQLite, comme notre Room (`sqlite-bundled`) → avertissements
   « duplicate symbol » au link. Fonctionne, mais à surveiller ; option : utiliser le SQLite de MapLibre/système pour Room sur iOS.
3. **Avertissement iOS** : un objet ICU de MapLibre est compilé pour iOS 18.5 alors que la cible est 15.5 →
   à vérifier sur un vieil iPhone, ou monter la cible minimale (iOS 17/18 ≈ l'essentiel du parc en 2026).
   **Octobre 2026** : cible minimale de l'app montée à **iOS 17.0** (parc 2026, et `CLBackgroundActivitySession`
   disponible pour fiabiliser la localisation en arrière-plan, pas encore utilisé). Le code natif de maplibre-native-ffi
   0.202610.0 vise iOS 15.5 ; l'avertissement reste pour `libicu.icudtl_dat.o` (18.5), qui ne contient que la table de
   données ICU, pas de code : risque faible, **à confirmer sur un appareil iOS 17** avant publication.
4. **Confidentialité** : le fournisseur de tuiles voit les zones consultées (pas les traces). À mentionner dans
   la politique de confidentialité ; auto-hébergement Protomaps possible plus tard.
5. **Dépendance réseau** : sans réseau, pas de fond de carte (overlays seuls). Offline (PMTiles) à prévoir plus tard.
   **Résolu** : carte hors ligne par région (Profil → Carte hors ligne), packs MapLibre zooms 10–14 (`OfflineMaps.kt`),
   ~45 Mo pour Cergy (tuiles, polices et sprites compris).
6. **Esthétique** : carte vectorielle stylisée, pas l'illustration de la planche (attendu, cf. `maps-decision.md`).

## Suite proposée

1. Fusionner la branche (montée de stack + carte).
2. Remplacer `TrackPreview` par MapLibre dans l'écran En sortie (suivi caméra) et le résumé.
3. Affiner le style (labels, POI nature, relief léger) et des marqueurs illustrés pour les spots.
4. Retirer l'outil « 1 000 zones de test » une fois les perfs validées sur le S24.
