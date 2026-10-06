# Architecture

## Modules (J0)

```
:core:exploration   KMP pur (jvm, iosArm64, iosSimulatorArm64) — domaine d'exploration
        ▲
:shared             KMP + Compose Multiplatform (android, ios) — design system, navigation, écrans
        ▲                         ▲
:androidApp (Activity)      iosApp/ (SwiftUI → ComposeUIViewController)
```

**Pourquoi un module Gradle pour l'exploration ?** C'est le seul endroit où la frontière est
*imposée par le compilateur* : `:core:exploration` n'a aucune dépendance Android, Compose, carte ou
GPS — il ne peut donc pas en importer par accident. Un simple package ne protégerait rien.
La cible `jvm` sert aux tests rapides et laisse la porte ouverte à un futur backend
(précalcul des régions) qui réutiliserait exactement le même code.

**Pourquoi pas plus de modules ?** Avec un seul écran réel, découper `design-system`, `feature/*`
et `data/*` en modules Gradle ne coûterait que du temps de build et de la configuration.
Ce sont des **packages** dans `:shared` :

```
com.rideak.shared
├── designsystem/            tokens + composants
├── navigation/              routes typées + RideakApp (Scaffold, bottom bar, NavHost)
└── feature/{home,placeholder}
```

Règle d'extraction : un package devient un module quand (a) un deuxième consommateur en a besoin,
ou (b) il a des dépendances lourdes à isoler (SDK carte, Room, Location).
Candidats probables : `:core:design-system`, `:data:location`, `:feature:map`.

## Principes

- UI → Domain → Data, sans cérémonie : pas de UseCase vide, pas d'interface à implémentation unique.
- Pas d'injection de dépendances à J0 (rien à injecter). Koin, déjà maîtrisé sur FRIDIA,
  arrivera avec la première vraie dépendance (base de données, localisation).
- Les SDK carte/GPS ne traversent jamais la frontière du domaine : ils sont convertis en
  `GeoPoint` / `TrackPoint` dans la couche data.
- Pas de convention plugins (`build-logic`) tant qu'il y a 3 modules.

## Tests

| Où | Quoi | Commande |
|---|---|---|
| `core/exploration/src/commonTest` | domaine pur | `./gradlew :core:exploration:allTests` (JVM + simulateur iOS) |
| `shared/src/commonTest` | UI Compose (`runComposeUiTest`) | `./gradlew :shared:iosSimulatorArm64Test` |
| Android lint | | `./gradlew :androidApp:lintDebug` |

Les tests UI tournent sur le simulateur iOS car `:shared` n'a pas de cible JVM/desktop :
en ajouter une juste pour les tests importerait Skiko desktop. À réévaluer si les tests UI se multiplient.
