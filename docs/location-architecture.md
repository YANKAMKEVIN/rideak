# Architecture GPS

> **État J1 : implémentée** (voir `ride-recording.md`). Reste à faire : auto-pause, batterie adaptative,
> test terrain sur vraie sortie.

## Forme

```
commonMain : interface LocationSource { fun positions(): Flow<TrackPoint> }
androidMain: FusedLocationProviderClient  → TrackPoint
iosMain    : CLLocationManager            → TrackPoint
                     │
           RideRecorder (commun, pur, testable avec une Flow simulée)
           filtrage · distance · vitesse · temps en mouvement · pauses
                     │
           persistance incrémentale des TrackPoints (base locale)
                     │
           ExplorationEngine (:core:exploration)
```

Le moteur d'enregistrement est commun et testé sans UI ni appareil, en rejouant des traces.

## Android

- `FusedLocationProviderClient`, priorité haute précision, intervalle ~1 s pendant une sortie.
- **Foreground service** de type `location` pendant la sortie, avec notification persistante (pause / stop).
- Permissions : `ACCESS_FINE_LOCATION` (+ `COARSE`, obligatoire en même temps). Démarré au premier plan,
  le service n'a **pas besoin** de `ACCESS_BACKGROUND_LOCATION` : à ne demander que si une
  fonctionnalité l'exige vraiment (revue Play Store stricte).
- Localisation **approximative** (~2 km) : inutilisable pour les cellules → on explique et on
  propose de passer en précis, sans bloquer le reste de l'app.
- `POST_NOTIFICATIONS` (Android 13+) pour la notification du service.

## iOS

- `CLLocationManager`, `kCLLocationAccuracyBest`, `activityType = .fitness`.
- Mode d'arrière-plan `location` + `allowsBackgroundLocationUpdates` ; autorisation « Pendant l'utilisation »
  suffit (indicateur bleu). « Toujours » seulement si justifié.
- Précision réduite (iOS 14+) : même traitement que l'approximatif Android.

## Robustesse

- **Process death** : les TrackPoints sont écrits par lots (ex. toutes les 5–10 s) ; au redémarrage,
  une sortie « en cours » est détectée et proposée à la reprise.
- **Perte de signal** : le trou est marqué (pas d'interpolation par-dessus) ; la distance n'est pas
  extrapolée.
- **Batterie** : intervalle adaptatif à l'arrêt, pas de GPS hors sortie, pas de wake lock superflu.
- **Filtrage** : précision, sauts aberrants, dérive à l'arrêt — dans le recorder pour les stats
  affichées, mais **les points bruts restent stockés** pour le recalcul.
- **Confidentialité** : voir `privacy.md` (zones privées appliquées avant affichage/export/sync).
