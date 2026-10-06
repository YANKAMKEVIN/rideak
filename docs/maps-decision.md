# Décision cartographique

**Statut : recommandation, aucun SDK intégré à J0.** À confirmer par un spike (prototype de style).

## Contrainte de départ

La maquette montre une carte *illustrée*. Aucun fournisseur ne produit cette esthétique.
Objectif réaliste :

```
carte vectorielle fortement stylisée (palette RIDEAK)
+ markers illustrés + overlay des cellules explorées / brouillard
+ textures ponctuelles + animations
```

La planche est une **direction artistique**, pas une promesse pixel-perfect.

## Comparaison

| Critère | Google Maps | Mapbox | MapLibre + OSM |
|---|---|---|---|
| KMP / Compose Multiplatform | ❌ Android (maps-compose) et iOS séparés | ❌ pas de SDK KMP officiel | ✅ bibliothèque Compose Multiplatform (maplibre-compose, à valider en spike) |
| Personnalisation graphique | Limitée (cloud styling) | Excellente (Studio) | Excellente (même style spec que Mapbox ; Maputnik) |
| Style vectoriel | Partiel | ✅ | ✅ |
| Tracés, markers custom, overlays | ✅ | ✅ | ✅ |
| Coût | Au chargement de carte au-delà du gratuit | Gratuit jusqu'à un quota de MAU, puis payant | SDK gratuit ; tuiles : gratuites (OpenFreeMap), payantes (MapTiler, Stadia) ou auto-hébergées (Protomaps/PMTiles) |
| Licence | Propriétaire, CGU restrictives (pas de mélange avec d'autres données carto) | Propriétaire depuis v2 | BSD-2 ; données ODbL |
| Attribution | Google | Mapbox + OSM | « © OpenStreetMap contributors » (+ fournisseur de tuiles) |
| Offline futur | Très limité | ✅ (payant au-delà) | ✅ (PMTiles/MBTiles locaux) |
| Cohérence avec nos données OSM (lieux, cellules praticables) | ❌ | ✅ (données OSM) | ✅ (même source partout) |
| Potentiel DA manga | Faible | Fort | Fort |

## Recommandation

**MapLibre + tuiles vectorielles OSM**, avec un style RIDEAK maison :
- seul candidat avec une intégration Compose Multiplatform → une seule implémentation de carte ;
- aucun coût de SDK ni dépendance à un fournisseur ; le fournisseur de tuiles reste interchangeable ;
- même source de données (OSM) pour le fond de carte, les lieux et les cellules praticables ;
- offline possible plus tard.

Risques : maturité de la bibliothèque Compose Multiplatform (→ spike : style + tracé + overlay de
1 000 cellules + marker custom, sur S24 et iPhone), hébergement des tuiles à dimensionner.
Repli : MapLibre natif encapsulé via `expect/actual` (Android View / UIKit).

## Prochaine étape (proposée)

Spike « carte stylisée » : style Maputnik palette RIDEAK + overlay de cellules issues de
`SlippyTileGrid` + un tracé + un marker, mesuré en FPS sur le S24.
