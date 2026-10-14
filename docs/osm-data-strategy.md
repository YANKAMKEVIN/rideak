# Stratégie de données OpenStreetMap

Premier pack : **Cergy** (`shared/src/commonMain/composeResources/files/regions/cergy.region`, 6 Ko,
391 cellules praticables). C'est une base de données dérivée d'OSM : voir la section Licence avant toute publication.

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

## Pipeline actuel (V1, sans backend)

```
Overpass (une requête, depuis la machine de dev)
   → ./gradlew :tools:regionpack:run --args="cergy.json shared/…/regions/cergy.region"
        assemble la frontière (relation admin), simplifie (Douglas–Peucker 15 m),
        filtre les voies praticables, échantillonne tous les 10 m → cellules dans la frontière
   → pack texte embarqué dans l'app (RegionPackCodec, :core:exploration)
```

Requête utilisée (Cergy, INSEE 95127) :

```
[out:json][timeout:120];
area["ref:INSEE"="95127"]["boundary"="administrative"]->.a;
rel["ref:INSEE"="95127"]["boundary"="administrative"];
out geom;
way(area.a)["highway"~"^(cycleway|path|track|living_street|residential|unclassified|tertiary|secondary|primary|service|pedestrian|footway|bridleway)$"];
out geom;
```

Pour plusieurs régions ou des mises à jour régulières, passer à l'extrait Geofabrik du pipeline ci-dessous.

## Tags de départ (à affiner)

- **Praticable** (implémenté dans `tools/regionpack`, `isRideable`) :
  `highway=cycleway|path|track|living_street|residential|unclassified|tertiary|secondary|primary|service` ;
  `footway|pedestrian|bridleway|steps` seulement avec `bicycle=yes|designated|permissive` (piéton par défaut en France) ;
  exclus : `motorway|trunk`, `bicycle=no|dismount`, `access=private|no` (sauf vélo explicitement autorisé), `service=driveway`.
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
