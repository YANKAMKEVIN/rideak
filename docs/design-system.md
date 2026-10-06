# Design System v0.1

Code : `shared/src/commonMain/kotlin/com/rideak/shared/designsystem/`.
Accès : `RideakTheme.colors / typography / spacing / shapes` (+ `MaterialTheme` mappé dessus).

## Palette et contrastes mesurés (WCAG 2.1)

| Token | Hex | sur Night | sur Surface | texte blanc dessus | texte Night dessus |
|---|---|---|---|---|---|
| Exploration Blue | #238CFF | 5.51 | 4.43 ⚠️ | 3.35 ⚠️ | 5.51 |
| Nature Green | #35D66F | 9.66 | 7.78 | 1.91 ❌ | 9.66 |
| Action Red | #FF4B45 | 5.57 | 4.49 ⚠️ | 3.31 ⚠️ | 5.57 |
| Reward Yellow | #FFD43B | 12.94 | 10.42 | 1.43 ❌ | 12.94 |
| Light Surface | #A7B3C2 | 8.67 | 6.98 | — | — |
| Manga White | #F7F4EA | 16.77 | 13.50 | — | — |

Seuils AA : 4.5 texte normal, 3.0 texte large (≥ 18,66 sp gras ou ≥ 24 sp).

**Règles qui en découlent**
1. Texte sur une couleur saturée = **Night** (`colors.onAccent`).
2. Exception : blanc sur Action Red/Blue **uniquement** en texte large et gras
   (`typography.action`, 20 sp ExtraBold) — c'est le cas d'`AdventureButton`.
3. Bleu/rouge en *texte* sur Surface : réservé au texte large (4.43 / 4.49 < 4.5).
4. Une information n'est jamais portée par la couleur seule (le % est toujours écrit, etc.).

`surfaceRaised` (#1C3A4C) est un token ajouté pour les pistes de progression et les élévations.

## Typographie — deux voix

- **UI** (`typography.ui` = Material `Typography`) : navigation, stats, réglages. Cible : **Inter** (SIL OFL).
- **Impact / manga** (`typography.impact`) : « NOUVEL ENDROIT ! », « GO! », milestones. Cible : **Bangers** (SIL OFL).
- `typography.metric` : grandes valeurs de sortie. `typography.action` : libellé des CTA.

⚠️ **Les fichiers de police ne sont pas encore intégrés** (téléchargement à valider) :
les deux voix utilisent la sans-serif système. Le remplacement se fait en un seul endroit
(`Type.kt`, `UiFontFamily` / `DisplayFontFamily`) via Compose Resources.

## Espacements et formes

`spacing` : 4 / 8 / 16 / 24 / 32 dp, `touchTarget` 48 dp, `rideTouchTarget` 72 dp (mode Ride).
`shapes` : 8 / 16 / 24 dp, pill.

## Composants

| Composant | État |
|---|---|
| `AdventureButton` | ✅ CTA principal, hauteur min 64 dp |
| `RideMetric` | ✅ valeur + unité + libellé, lu comme une phrase par TalkBack/VoiceOver |
| `ExplorationProgressBar` | ✅ consomme `ExplorationProgress` du domaine, sémantique de progression |
| `RideakBottomBar` | ✅ libellés toujours visibles |
| `RideStatCard`, `LocationChip`, `DiscoveryCard`, `MangaBanner`, `MapMarker`, `AchievementBadge` | prévus, non implémentés |

Les icônes Material sont temporaires (le set illustré RIDEAK les remplacera).

## Trois niveaux d'intensité

- **Exploration** : calme, la carte domine.
- **Ride** : lisibilité et sécurité d'abord — vitesse, distance, durée, carte, Pause, ❤️. Cibles ≥ 72 dp, pas de colonne de petits boutons.
- **Moments** : DA manga expressive, animations courtes, puis retour au calme.

## Accessibilité (dès J0)

Cibles ≥ 48 dp, `heading()` sur les titres, sémantique fusionnée pour les métriques,
pas de hauteur fixe sur du texte (font scaling), info jamais seulement colorée.
À venir : respect du réglage système « réduire les animations » pour les Moments.

## Hors périmètre de J0

Thème clair (la DA repose sur Night), polices, set d'icônes, illustrations.
