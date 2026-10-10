# Enregistrement d'une sortie (J1)

## Chaîne

```
LocationSource (Fused / CoreLocation / simulée)
      │ Flow<TrackPoint> bruts, ~1 Hz
RideRecorder (:core:data, scope applicatif, Mutex)
      ├── RideMetricsCalculator (:core:ride, pur) → métriques live
      ├── tampon → Room par lots (10 points ou 5 s, + pause/stop)
      └── StateFlow<RecorderState> → écrans, notification Android
```

- **Les TrackPoints bruts sont stockés tels quels**, avec un numéro de *segment* (incrémenté à chaque reprise).
  `RideMetricsCalculator.compute(segments)` rejoue une sortie et donne exactement les métriques live :
  changer un seuil = recalculer l'historique.
- **Process tué** : au plus ~5 s perdues. Au lancement suivant, `recoverUnfinished()` restaure la sortie
  **en pause**, métriques recalculées depuis les points ; l'accueil affiche « SORTIE INTERROMPUE ».
- **Pause** : le GPS est coupé, la durée active s'arrête, le trajet pendant la pause n'est jamais compté.

## Filtres (RideFilterConfig — tous paramétrables, valeurs à valider sur de vraies sorties)

| Paramètre | Défaut | Rôle |
|---|---|---|
| `maxAccuracyMeters` | 25 m | fix ignoré au-delà |
| `minStepMeters` | 5 m | la distance n'avance que d'au moins max(5 m, précision du fix) |
| `minMovingSpeedMps` | 0,8 m/s (~3 km/h) | en dessous : arrêté, ni distance ni temps en mouvement |
| `maxPlausibleSpeedMps` | 25 m/s (90 km/h) | saut GPS rejeté (pas une règle de vitesse) ; après 3 sauts cohérents, la nouvelle position est adoptée |
| `maxMovingGapMillis` | 30 s | un trou de signal ne compte pas comme temps en mouvement |
| `elevationHysteresisMeters` | 5 m | l'altitude GPS est bruitée : montées < 5 m ignorées |
| `speedSmoothing` | 0,4 | lissage exponentiel de la vitesse affichée / max |

Vitesse : celle de l'OS (Doppler) si disponible, sinon déplacement **net depuis l'ancre** — une position
qui oscille autour d'un cycliste arrêté a une vitesse nette ≈ 0 (cas testé : 5 min à l'arrêt, ±6 m de bruit → < 25 m).

## Limites connues

- Dénivelé basé sur l'altitude GPS : approximatif (pas de baromètre ni de modèle de terrain).
- Pas d'auto-pause ; la distance entre deux fixes après un trou de signal est en ligne droite.
- Une sortie démarrée par erreur reste dans l'historique (pas encore de suppression).

## Plateformes

- **Android** : `FusedLocationProviderClient` (haute précision, 1 s) + `RideTrackingService`
  (foreground service `location`, notification Pause/Reprendre/Terminer). Permissions demandées au
  démarrage d'une sortie : FINE + COARSE (+ POST_NOTIFICATIONS en 13+). **Pas de localisation en arrière-plan.**
- **iOS** : `CLLocationManager` (best, fitness, `allowsBackgroundLocationUpdates`) + mode d'arrière-plan
  `location` ; autorisation « Pendant l'utilisation » seulement.
- **Simulation** (debug) : Profil → « Sortie simulée » rejoue une boucle de ~3,8 km à 22 km/h.
