# Confidentialité (privacy by design)

Les traces GPS révèlent le domicile, le travail et les habitudes. Une carte d'exploration
les révèle encore plus vite (la zone la plus explorée est presque toujours autour de chez soi).
Ce sont des données personnelles au sens du RGPD.

## Décisions prises (J0–J1)

- **Position uniquement pendant une sortie**, demandée au moment de partir, avec explication.
  Pas de localisation en arrière-plan (Android) ni « Toujours » (iOS) : le foreground service et le
  mode d'arrière-plan `location` couvrent l'écran éteint, avec indicateur visible pour l'utilisateur.
- **Traces, spots et sorties restent dans la base locale** de l'appareil. Seul réseau : le fond de carte
  (OpenFreeMap), qui voit les zones affichées mais jamais les traces ni les zones explorées.
- Position approximative : expliquée (distance et exploration fausses), l'utilisateur choisit.
- **Aucune sauvegarde système** : `data_extraction_rules.xml` et `backup_rules.xml` excluent
  toutes les données de la sauvegarde cloud Google et du transfert d'appareil. À rouvrir
  uniquement par une décision explicite et visible de l'utilisateur.
  Sur iOS, la base est dans `Application Support/RideakData`, marqué `NSURLIsExcludedFromBackupKey`
  (iCloud et sauvegarde sur ordinateur) ; les anciennes versions la gardaient dans `Documents`, déplacée une fois.
- **Politique de confidentialité** : `shared/src/commonMain/composeResources/files/legal/privacy_policy.md`, affichée
  dans l'app (Profil → Politique de confidentialité) et publiable telle quelle pour les stores.
  **À compléter avant publication : nom de l'éditeur et adresse de contact.**
- **Droit à l'effacement** : Profil → « Effacer toutes mes données » (sorties terminées, traces, spots, zones privées ;
  une sortie en cours est conservée).
- **Zones privées (implémentées)** : `PrivacyZone` + `PrivacyMask` (`:core:exploration`), table `privacy_zones`
  (Room v2, migration automatique), section « Zones privées » du Profil.
  - **Suggestions** : les lieux où au moins 2 sorties commencent ou finissent (à 150 m près) sont proposés
    (Domicile / Travail / Autre). Pas besoin de demander la position hors sortie.
  - **Anti-triangulation** : `PrivacyZone.around` décale le centre de 15–40 % du rayon dans une direction
    aléatoire et agrandit le rayon jusqu'à +25 % (400 m demandés → 400–500 m). **Seule la zone décalée est
    stockée**, jamais le point protégé.
  - **Masquage** : tracé coupé à l'entrée de la zone, spots retirés, et **cellules qui touchent la zone retirées**
    (sinon les cellules explorées dessineraient le contour du domicile).
  - **Mode partage** sur « Ma carte » : applique le masque et cache le dessin des zones elles-mêmes, pour une
    capture d'écran. Hors mode partage, l'utilisateur voit tout, zones comprises (voile gris).
- **La confidentialité précède le social** : aucune fonctionnalité de partage avant que les
  zones privées soient complètes.

## Questions ouvertes (à trancher avant le premier partage / la première sync)

| Donnée dans une zone privée | Proposition |
|---|---|
| Enregistrée localement | Oui, brute (nécessaire au recalcul), sur l'appareil uniquement |
| Affichée à l'utilisateur lui-même | Oui sur sa propre carte, avec un indicateur de zone |
| Affichée à d'autres | Jamais : tracé tronqué à l'entrée/sortie de la zone |
| Exportée (GPX…) | Tronquée par défaut, option explicite pour l'export complet |
| Synchronisée (cloud futur) | À décider : tronquée, ou chiffrée de bout en bout |
| Comptée pour l'exploration | Oui pour sa progression personnelle, sans exposer les cellules |

Points de vigilance : un rayon fixe centré sur le domicile se retrouve par triangulation —
préférer un décalage aléatoire du centre et/ou des rayons variables ; les cellules
explorées publiques peuvent trahir une zone même avec un tracé masqué.

## RGPD (à préparer avant toute collecte)

Base légale et consentement explicites, durée de conservation, droit à l'effacement
(suppression complète de l'historique), droit à la portabilité (export), registre des traitements
si backend. Rien n'est envoyé à un tiers sans le dire (y compris le fournisseur de tuiles,
qui voit la zone consultée).
