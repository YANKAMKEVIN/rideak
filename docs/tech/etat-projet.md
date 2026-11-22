# État du projet RIDEAK

Mis à jour le 22 novembre 2026. Point d'entrée pour reprendre le travail dans une session neuve.

## Où on en est

Branche `main`, à jour avec `origin`, CI verte. Tout ce qui suit est fusionné.

| Domaine | État | Où lire |
|---|---|---|
| Enregistrement de sortie (GPS Android/iOS, Room, reprise) | ✅ | `docs/ride-recording.md` |
| Carte MapLibre stylisée, brouillard d'exploration | ✅ | `docs/spikes/maplibre.md` |
| Moteur d'exploration V1 (interpolation, dérive, vitesse motorisée) | ✅ | `docs/exploration-model.md` |
| « +N zones dévoilées » en fin de sortie | ✅ | `docs/exploration-model.md` |
| Région Cergy : vrai %, 391 cellules praticables, contour sur la carte | ✅ | `docs/osm-data-strategy.md` |
| 45 lieux à découvrir (onglet Lieux, « NOUVEL ENDROIT ! ») | ✅ | `docs/exploration-model.md` |
| Zones privées (suggestions, centre décalé, mode partage) | ✅ | `docs/privacy.md` |
| XP, niveaux, 6 badges illustrés, compteurs par catégorie | ✅ | `docs/exploration-model.md` |
| Carte hors ligne (Profil, ~45 Mo pour Cergy) | ✅ | `docs/spikes/maplibre.md` |
| « Ma carte » : épingles des lieux découverts, cartes du carrousel = filtres par catégorie | ✅ | `MapViewModel.kt` |
| Onboarding au premier lancement (3 pages, « déjà vu » mémorisé) | ✅ | `OnboardingScreen.kt`, `AppSettings.kt` |
| Politique de confidentialité, crédits, « Effacer toutes mes données » | ✅ | `docs/privacy.md` |
| Licence ODbL des packs de régions | ✅ (choix) | `docs/licenses/region-packs-ODbL.md` |
| iOS 17.0 minimum | ✅ | `docs/spikes/maplibre.md` |

## En attente de Kevin (bloquant pour une publication)

1. **Éditeur et contact** : remplacer « [à compléter] » dans
   `shared/src/commonMain/composeResources/files/legal/privacy_policy.md` et `credits.md`
   (affiché tel quel dans l'app tant que ce n'est pas fait).
2. **Pack Cergy public** : le dépôt est privé ; publier `cergy.region` à une adresse publique
   (licence ODbL) et mettre le lien dans `credits.md`.
3. **Tests sur iPhone** : carte hors ligne en mode avion ; app sous iOS 17 (avertissement de liaison
   restant sur `libicu.icudtl_dat.o`, données seules, risque faible).
4. **PR n° 1** (`claude/stoic-hofstadter-5e01cb`, filtres de carte) : remplacée par ce qui est sur `main` ;
   ses trois apports utiles ont été repris sur `claude/suite`. Elle peut être fermée sans fusion.

## Non vérifié en vrai (seulement par tests)

- Bloc « NOUVEL ENDROIT ! » et récompenses de fin de sortie (« +N XP », badge débloqué) :
  une sortie simulée complète (Profil → Outils de développement, ~10 min) suffit à les voir.
- Filtres de « Ma carte » : message « Aucun … découvert pour l'instant » quand la catégorie choisie est vide,
  et masquage en mode partage d'un lieu dont le contour touche une zone privée : couverts par les tests seulement.
- Lieu ouvert depuis « Lieux » alors qu'une carte du carrousel en masque la catégorie : le filtre repasse
  sur « Tout » pour que l'épingle soit visible (`MapViewModel.reveal`), vérifié par les tests seulement.
- Bouton de « Mes sorties » vide pendant une sortie : « RETOUR À LA SORTIE » / « SORTIE INTERROMPUE »,
  comme l'accueil (`rideButtonLabel`), vérifié par les tests seulement.
- Filtre des curiosités (`isBusiness`) : écrit d'après les usages OSM, sans les vraies balises d'« Aeroscoot »
  et « Baby Park » (OSM inaccessible depuis la session) ; ces deux-là sont écartés par identifiant. À la
  prochaine régénération du pack, vérifier la liste « LANDMARK » affichée par l'outil.
- Tous les seuils du moteur (dérive 15 m, 45 km/h, interpolation 60 s / 400 m, découverte 40 / 30 m)
  sont des premières estimations à valider sur de vraies sorties.

## Prochaines tâches possibles

- `CLBackgroundActivitySession` (iOS 17) pour fiabiliser la localisation en arrière-plan : à faire sur Mac.
  Dans `IosLocationSource`, créer la session juste avant `startUpdatingLocation()` et appeler `invalidate()`
  dans `awaitClose`. Non fait depuis Linux : le nom Kotlin/Native de la fabrique
  (`CLBackgroundActivitySession()` ou `backgroundActivitySession()`) ne peut pas être vérifié sans compiler.

- Coûts de calcul à mesurer sur de vraies données avant d'optimiser (décision à prendre) : l'exploration
  est recalculée depuis tous les points à chaque changement des sorties (accueil, carte, lieux, profil,
  « Mes sorties »), et la découverte en direct rejoue toute la trace de la sortie toutes les 3 s.
  La copie de la trace en direct à chaque seconde, elle, est supprimée (`GrowingTrack`).

## Repères techniques

- Build et tests : voir `README.md` (JDK = JBR d'Android Studio).
- Suite complète : `./gradlew :core:exploration:jvmTest :core:ride:jvmTest :core:data:jvmTest :tools:regionpack:jvmTest :shared:iosSimulatorArm64Test :androidApp:assembleDebug :androidApp:lintDebug`
- Régénérer un pack région : `docs/osm-data-strategy.md` (requêtes Overpass + `:tools:regionpack:run`).
- Machine de dev chargée en mémoire : compilations lentes si deux simulateurs iOS tournent avec Android Studio.
