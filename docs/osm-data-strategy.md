# Stratégie de données OpenStreetMap

**Aucune donnée OSM n'est dans le dépôt à J0.**

## Usages prévus

Voies praticables à vélo (→ cellules explorables), lieux (parcs, forêts, lacs, rivières,
points de vue, monuments), géométries, frontières de régions.

## Pas d'Overpass en production

L'API Overpass publique est une ressource partagée avec une politique d'usage stricte :
pas d'appels massifs depuis chaque téléphone. Elle reste utile pour explorer les données en dev.

## Pipeline envisagé

```
Extrait régional (Geofabrik, .osm.pbf)
   → outil de précalcul (JVM, réutilise :core:exploration pour CellGrid)
   → « pack région » versionné :
        - frontière de la région
        - gridId + ensemble des cellules praticables
        - lieux (id, nom, catégorie, géométrie simplifiée)
   → CDN / stockage objet
   → l'app télécharge et met en cache le pack des régions qui l'intéressent
```

Backend géospatial (PostGIS) seulement si les packs deviennent insuffisants
(recherche, mises à jour fréquentes, régions à la demande).

## Tags de départ (à affiner)

- **Praticable** : `highway=cycleway|path|track|living_street|residential|unclassified|tertiary|secondary|service`,
  `bicycle=yes|designated` ; exclure `highway=motorway|trunk`, `bicycle=no`, `access=private|no`.
- **Lieux** : `leisure=park`, `landuse=forest` / `natural=wood`, `natural=water` + `water=lake|pond`,
  `waterway=river`, `tourism=viewpoint`, `historic=monument|memorial`, `tourism=attraction`.

## Licence ODbL — obligations (synthèse, pas un avis juridique)

- **Attribution** visible partout où la carte ou les données sont montrées :
  « © OpenStreetMap contributors », avec un lien vers la licence.
- Nos packs (lieux, cellules praticables) sont une **base de données dérivée** : si on les
  distribue publiquement, ils doivent être disponibles sous ODbL (partage à l'identique).
  Une « œuvre produite » (une image de carte, l'app) n'est pas soumise au partage à l'identique,
  mais doit porter l'attribution.
- Mélanger OSM avec des données sous licence incompatible (ex. Google) est interdit.
- À faire valider avant la publication du premier pack.
