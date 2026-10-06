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

## `cellsFor` à J0 — limites volontaires

Une cellule par échantillon, après les filtres fournis. Pas d'interpolation, pas d'anti-dérive,
pas de règle de vitesse. Testé : trace vide, un point, même cellule, plusieurs cellules,
pas de doublon, filtre de précision.

## Ce que le moteur devra traiter (jalons suivants)

- **Interpolation** : deux points espacés traversent plusieurs cellules → parcourir le segment
  (DDA sur la grille), mais **pas** au-dessus d'un trou de signal (au-delà de N s ou N m, on coupe).
- **Dérive à l'arrêt** : un cycliste immobile ne doit pas explorer les cellules voisines
  (détection d'arrêt sur fenêtre glissante, rayon de dérive).
- **Précision** : rejeter ou pondérer les fix au-delà d'un seuil (`AccuracyFilter` est le premier pas).
- **Vitesse anormale** : vitesse soutenue élevée = voiture/train. Seuil envisagé ~45 km/h,
  **paramétrable, à valider avec de vraies traces** — jamais une constante métier.
- **Anti-cheat** : nouvelles implémentations de `TrackFilter` ; aucune à J0.
- **Découverte de lieux** selon la géométrie : rayon pour `Point`, croisement/proximité pour
  `LineString`, entrée effective pour `Polygon`.
- **TRAVERSED → EXPLORED** : règle à définir (nombre de passages, densité de voisins, temps passé).

## Choix de la grille

Tuiles carrées Web Mercator en V1 : 20 lignes de Kotlin pur, alignées sur les tuiles de carte,
faciles à dessiner en overlay. Alternative : hexagones H3 (plus jolis pour le « brouillard »,
voisinage uniforme) mais pas de bibliothèque KMP officielle. Grâce à `CellGrid`/`CellId`,
le changement ne touche pas le domaine — seulement un recalcul.
