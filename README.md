# RIDEAK

> Plus tu roules, plus ton monde se dévoile.

## Vision

RIDEAK est un jeu d'exploration du monde réel à vélo, avec une identité manga/JRPG moderne.
Le GPS et les statistiques sont le socle ; le produit, c'est la carte réelle qui devient
peu à peu une world map personnelle : cellules explorées, lieux découverts, lieux ❤️, régions.
Ce n'est pas un clone de Strava : la vitesse n'est jamais récompensée.

## Stack

Kotlin Multiplatform · Compose Multiplatform (Android + iOS) · Material 3 · Coroutines/Flow ·
Navigation Compose (routes typées) · Gradle Kotlin DSL + Version Catalog.
Room KMP (SQLite embarqué) · Koin · kotlinx-datetime · Play Services Location / CoreLocation.
Plus tard : MapLibre (voir `docs/maps-decision.md`).

## Architecture

```
:core:exploration   domaine d'exploration pur (aucune dépendance Android/Compose/carte/GPS)
:core:ride          métriques de sortie pures (distance, vitesses, dénivelé, filtres GPS)
:core:data          Room, sources de localisation, enregistreur de sortie
:shared             UI Compose Multiplatform : design system, navigation, écrans
:androidApp         hôte Android (+ foreground service de sortie)
iosApp/             hôte iOS (Xcode)
```

Détails et justification : [`docs/architecture.md`](docs/architecture.md).

## Design System

Palette v0.1 avec contrastes mesurés, deux voix typographiques (UI / impact manga),
composants de base. Voir [`docs/design-system.md`](docs/design-system.md).

## Exploration Model

`TrackPoint` (source de vérité) → `ExplorationEngine.cellsFor` → `CellId` → `UserCell`.
Régions mesurées sur les cellules **praticables** ; le ❤️ n'est jamais un état de progression.
Voir [`docs/exploration-model.md`](docs/exploration-model.md).

## Build

Prérequis : JDK 21 (le JBR d'Android Studio), Android SDK 36, Xcode 26 pour iOS.

```bash
export JAVA_HOME="$HOME/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :androidApp:assembleDebug        # APK debug
./gradlew :androidApp:installDebug         # installe sur l'appareil branché
```

iOS : ouvrir `iosApp/iosApp.xcodeproj` dans Xcode (la phase de build appelle Gradle),
ou `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -sdk iphonesimulator ARCHS=arm64 build`.

## Tests

```bash
./gradlew :core:exploration:allTests :core:ride:allTests   # domaines purs, JVM + simulateur iOS
./gradlew :core:data:jvmTest               # enregistreur + Room
./gradlew :shared:iosSimulatorArm64Test    # tests UI Compose
./gradlew :androidApp:lintDebug            # lint
```

CI (GitHub Actions, `.github/workflows/ci.yml`) : build Android + lint + tests du domaine à chaque push et PR ;
tests iOS (domaine + UI Compose) sur macOS, sur `main` et à la demande seulement (minutes macOS coûteuses).

## Roadmap

- **J0** — fondations ✅
- **J1** — enregistrement d'une sortie : GPS Android/iOS, recorder testable, persistance, Ride mode ✅
- Spike carte stylisée MapLibre
- Moteur d'exploration : interpolation, dérive, filtres ; premières régions précalculées
- Découverte de lieux, favoris, progression, puis gamification

## Technical Decisions

- KMP + CMP : une seule UI pour Android et iOS.
- Module Gradle dédié au domaine d'exploration, packages pour le reste.
- Carte recommandée : MapLibre + OSM ([`docs/maps-decision.md`](docs/maps-decision.md)).
- GPS : [`docs/location-architecture.md`](docs/location-architecture.md).
- Confidentialité : [`docs/privacy.md`](docs/privacy.md). Données : [`docs/osm-data-strategy.md`](docs/osm-data-strategy.md).

## Current Milestone

J1 — enregistrer une vraie sortie : GPS Android (foreground service) et iOS (mode arrière-plan),
métriques filtrées et rejouables, persistance Room par lots, reprise après process tué, écran En sortie,
résumé, historique, ❤️ spots, sortie simulée (debug). Détails : [`docs/ride-recording.md`](docs/ride-recording.md).
Pas encore de carte.
