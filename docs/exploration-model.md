# Modèle d'exploration

Code : `core/exploration/src/commonMain/kotlin/com/rideak/exploration/`.
Aucune dépendance Android, Compose, carte ni GPS.

## Concepts

| Type | Rôle |
|---|---|
| `GeoPoint` | Position WGS84 neutre (+ `distanceTo`, haversine) |
| `TrackPoint` | Échantillon GPS brut (position, horodatage, précision, altitude, vitesse). **Source de vérité.** |
| `CellId` | Identité opaque d'une cellule, préfixée par la grille (`tile17/66291/44992`) |
| `CellGrid` / `SlippyTileGrid` | Stratégie position → cellule. V1 : tuiles Web Mercator zoom 17 (~200 m à Cergy) |
| `CellProgress` | `UNKNOWN → TRAVERSED → EXPLORED` (automatique) |
| `UserCell` | Faits bruts (`firstRideId`, `firstSeenAt`, `visitCount`) ; la progression en est *dérivée* |
| `Place` + `PlaceGeometry` | Lieu réel : `Point`, `LineString` ou `Polygon` (avec trous) |
| `PlaceProgress` | `UNDISCOVERED → DISCOVERED → EXPLORED` (automatique) |
| `PlaceDiscovery` | Quand / pendant quelle sortie un lieu a été découvert |
| `FavoritePlace` | Le ❤️ : **choix utilisateur**, indépendant de la progression |
| `ExplorationRegion` | Territoire mesuré : frontière (multi-polygone), `gridId`, `rideableCells` |
| `ExplorationProgress` | `exploredRideableCells / totalRideableCells` |
| `PrivacyZone` | Concept minimal (centre + rayon) — voir `privacy.md` |
| `ExplorationEngine.cellsFor` | `List<TrackPoint>` → `Set<CellId>`, pur et déterministe |
| `TrackFilter` / `AccuracyFilter` | Étapes de validation/nettoyage appliquées avant le calcul |

## Invariants

1. **Le ❤️ n'est jamais un état de progression.** Pas de `MEMORABLE` dans les enums :
   « mémorable » est un mot d'interface (`DISCOVERED + favori`).
2. **Le pourcentage ne compte que les cellules praticables.** Une cellule « lac » visitée
   n'augmente pas la progression (testé dans `DomainModelTest`).
3. **Tout est recalculable.** TrackPoints → moteur → CellIds → UserCells. Changer de grille,
   de seuil ou corriger un bug = rejouer l'historique. `CellId` porte l'id de grille, et
   `ExplorationRegion.gridId` dit avec quelle grille ses cellules ont été précalculées.
4. **Une sortie peut faire progresser plusieurs régions** (Cergy, Cergy-Pontoise, Val-d'Oise) :
   la région ne possède pas les cellules de l'utilisateur, elle les *mesure*.

## Règles du moteur (V1)

`ExplorationEngine.standard()` est la seule configuration utilisée par l'app (accueil, carte, sortie en cours,
résumé). Les sorties sont passées **par segment d'enregistrement** (`cellsForSegments`) : une pause n'est jamais
reliée à la reprise. Ordre d'application :

| Étape | Classe | Seuil V1 | Pourquoi |
|---|---|---|---|
| Précision | `AccuracyFilter` | > 50 m rejeté | fix inutilisable |
| Vitesse motorisée | `MotorizedSpeedFilter` | > 45 km/h soutenu ≥ 60 s | voiture/train ; une descente rapide de 20 s reste |
| Dérive à l'arrêt | `DriftFilter` | point gardé s'il est à ≥ 15 m du dernier gardé | un cycliste arrêté n'explore pas les cellules voisines |
| Interpolation | `Interpolation` | échantillons tous les 10 m, si l'écart ≤ 60 s **et** ≤ 400 m | un échantillonnage clairsemé ne saute pas de cellule |

La vitesse est mesurée entre positions, jamais lue dans l'OS. Le tronçon motorisé retiré laisse un trou que
l'interpolation ne comble pas (trop long ou trop loin). Tous ces seuils sont **à valider avec de vraies traces** ;
les changer recalcule tout l'historique (invariant 3). Tests : `ExplorationEngineTest`.

Reste à faire :

- **TRAVERSED → EXPLORED** : règle à définir (nombre de passages, densité de voisins, temps passé).
- **Découverte de lieux** selon la géométrie : rayon pour `Point`, croisement/proximité pour
  `LineString`, entrée effective pour `Polygon`.
- **Anti-cheat** : nouvelles implémentations de `TrackFilter` (téléportation, traces trop régulières).

## Choix de la grille

Tuiles carrées Web Mercator en V1 : 20 lignes de Kotlin pur, alignées sur les tuiles de carte,
faciles à dessiner en overlay. Alternative : hexagones H3 (plus jolis pour le « brouillard »,
voisinage uniforme) mais pas de bibliothèque KMP officielle. Grâce à `CellGrid`/`CellId`,
le changement ne touche pas le domaine — seulement un recalcul.
