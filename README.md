# 🔬 Granulométrie Studio

**Outil d'analyse granulométrique d'images scientifiques** — JavaFX 17 / Java 17

Granulométrie Studio mesure automatiquement les particules (grains, sable, poudres,
inclusions…) présentes sur une image de microscopie ou de photographie, puis produit
les distributions de tailles, les statistiques de synthèse (D10 / D50 / D90,
circularité, rapport d'aspect) et les exports nécessaires à un rapport scientifique.

---

## Sommaire

1. [Fonctionnalités](#fonctionnalités)
2. [Aperçu de l'interface](#aperçu-de-linterface)
3. [Installation](#installation)
4. [Lancement](#lancement)
5. [Mode d'emploi](#mode-demploi)
6. [Architecture](#architecture)
7. [Algorithmes et formules](#algorithmes-et-formules)
8. [Base de données locale](#base-de-données-locale)
9. [Exports](#exports)
10. [Tests](#tests)
11. [Packaging / distribution](#packaging--distribution)
12. [Ce qui a changé par rapport à la version 2.5](#ce-qui-a-changé-par-rapport-à-la-version-25)
13. [Limites connues](#limites-connues)
14. [Licence et crédits](#licence-et-crédits)

---

## Fonctionnalités

- **Import d'images** par sélecteur de fichiers ou **glisser-déposer** (PNG, JPG, BMP, TIFF, GIF).
- **Segmentation automatique** par seuillage d'**Otsu**, ou seuil **manuel** réglable (0–255).
- **Détection de polarité** : particules sombres sur fond clair ou l'inverse, avec inversion manuelle.
- **Filtre moyenneur** optionnel (rayon 0–5 px) pour atténuer le bruit avant seuillage.
- **Filtres morphologiques** : diamètre minimum / maximum, circularité minimale, exclusion des particules coupées par le bord.
- **Calibration spatiale** (µm/pixel) : toutes les longueurs sont exprimées en µm, sinon en pixels.
- **Calque de contrôle** : contours (ellipse d'inertie) et numéros des particules dessinés sur l'aperçu.
- **Distributions** : histogramme des diamètres, courbe cumulative des passants, histogramme de la surface occupée.
- **Statistiques** : effectif, min/max, moyenne, médiane, écart type, **D10 / D50 / D90**, circularité moyenne, rapport d'aspect, surface couverte.
- **Tableau des particules** triable : diamètre équivalent, aire, grand/petit axe, circularité, aspect, coordonnées, bbox.
- **Exports** : CSV complet (métadonnées + une ligne par particule), PNG des graphiques.
- **Historique local** : enregistrement des sessions (image, paramètres, particules) dans une base **H2 embarquée**, rechargement et suppression.
- **Traitement hors interface** : l'analyse est exécutée dans un thread de fond (interface fluide, barre de progression).

---

## Aperçu de l'interface

| Vue | Contenu |
| --- | --- |
| **Analyse** | Panneau de paramètres (image, segmentation, filtres, calibration) + aperçu avec calque de détection |
| **Résultats** | Indicateurs D10/D50/D90, onglets Distribution / Particules / Statistiques / Paramètres |
| **Historique** | Liste des sessions enregistrées, aperçu et détails, ouverture ou suppression |

> La documentation PDF et la présentation PowerPoint se trouvent dans [`docs/`](docs/). Les figures
> qu'elles contiennent sont produites par le moteur d'analyse de l'application (mode hors interface)
> sur l'image d'exemple fournie.

---

## Installation

### Prérequis

| Outil | Version |
| --- | --- |
| JDK | **17 ou supérieur** (testé avec Temurin 17.0.13) |
| Maven | 3.8+ (uniquement pour construire depuis les sources) |
| Système | Windows, Linux ou macOS avec affichage graphique |

Aucune base de données externe n'est nécessaire : la base d'historique H2 est
embarquée et créée automatiquement.

### Construire le projet

```bash
cd granulometry-studio
mvn clean package
```

Le build produit :

```
target/granulometrie-studio.jar     # application
target/lib/javafx/*.jar             # modules JavaFX (module path)
target/lib/h2-2.2.224.jar           # base embarquée (classpath)
```

Les tests unitaires sont exécutés pendant le build (`mvn test` pour les lancer seuls).

> Les bibliothèques JavaFX comprennent des binaires natifs **propres à chaque système**.
> Sur un poste Windows, exécutez `mvn clean package` sous Windows pour obtenir les
> bons binaires (ou utilisez `mvn javafx:run`).

---

## Lancement

### Depuis les sources (développement)

```bash
mvn javafx:run
```

### Depuis le paquet construit

**Linux / macOS**

```bash
./lancer.sh
```

**Windows**

```bat
lancer.bat
```

Ou manuellement :

```bash
java --module-path target/lib/javafx --add-modules javafx.controls,javafx.swing \
     -cp "target/granulometrie-studio.jar:target/lib/h2-2.2.224.jar" \
     fr.granulometrie.Launcher
```

Sur Windows, remplacez `:` par `;` dans le classpath.

---

## Mode d'emploi

1. **Onglet Analyse** — cliquez sur *Importer une image…* ou glissez un fichier sur l'aperçu.
   Le bouton *Charger l'exemple fourni* ouvre une image de test intégrée.
2. **Calibration** — saisissez la taille d'un pixel (µm/px). Laissez `1,0` pour travailler en pixels.
   Les libellés d'unités basculent automatiquement en µm.
3. **Segmentation** — laissez *Automatique (Otsu)* dans la majorité des cas. Le seuil proposé
   s'affiche sous le sélecteur ; passez en *Manuel* pour l'ajuster avec le curseur.
   Cochez *Inverser la polarité* si les particules sont claires sur fond sombre.
4. **Filtres** — bornez le diamètre (0 = pas de limite), imposez une circularité minimale,
   excluez les particules tronquées par le bord de l'image.
5. **Analyser l'image** — l'analyse s'exécute en arrière-plan ; le calque de détection
   se superpose à l'image et la vue *Résultats* s'ouvre.
6. **Résultats** — ajustez les largeurs de classe puis *Recalculer* ; triez le tableau des
   particules ; consultez les statistiques et les paramètres exacts du calcul.
7. **Exporter** — CSV pour les données, PNG pour les graphiques.
8. **Enregistrer dans l'historique** — ajoutez un commentaire, la session est conservée
   dans la base locale et reste consultable dans l'onglet *Historique*.

---

## Architecture

```
granulometry-studio/
├── pom.xml
├── lancer.sh / lancer.bat
├── docs/
│   ├── Documentation-Granulometrie-Studio.pdf
│   ├── Presentation-Granulometrie-Studio.pptx
│   └── captures/                      # captures d'écran
├── samples/                           # images de démonstration
└── src/
    ├── main/java/fr/granulometrie/
    │   ├── Launcher.java              # point d'entrée neutre (classpath)
    │   ├── GranulometryApp.java       # Application JavaFX, thème, icône
    │   ├── core/                      # moteur d'analyse (sans JavaFX)
    │   │   ├── AnalysisParameters.java
    │   │   ├── AnalysisResult.java
    │   │   ├── ImageAnalyzer.java     # lecture, seuillage, filtrage
    │   │   ├── Segmentation.java      # Otsu, flou, composantes connexes
    │   │   ├── Particle.java          # mesures d'une particule
    │   │   ├── SizeStatistics.java    # synthèse statistique
    │   │   ├── SizeClassifier.java    # classes granulométriques
    │   │   └── StatisticsUtils.java   # moyenne, écart type, percentiles
    │   ├── io/                        # exports
    │   │   ├── CsvExporter.java
    │   │   └── OverlayRenderer.java
    │   ├── repo/                      # persistance locale
    │   │   ├── AnalysisRepository.java
    │   │   └── StoredSession.java
    │   └── ui/                        # vues JavaFX
    │       ├── MainWindow.java
    │       ├── AnalyzeView.java
    │       ├── ResultView.java
    │       ├── HistoryView.java
    │       └── UiFactory.java
    └── main/resources/
        ├── css/app.css
        ├── img/icon.jpg
        └── samples/exemple-grains.jpg
```

**Séparation des responsabilités** : `core` ne dépend pas de JavaFX (testable en
batch), `io` ne fait que produire des fichiers, `repo` encapsule le SQL, `ui`
ne contient que de la présentation. Aucun état statique partagé entre les vues
(contrairement à la version 2.5).

---

## Algorithmes et formules

### Segmentation

1. Conversion en niveaux de gris : `Y = 0,299·R + 0,587·V + 0,114·B`.
2. Filtrage moyenneur séparable optionnel (rayon *r*).
3. **Seuil d'Otsu** : on cherche *t* qui maximise la variance inter-classes
   `σ²(t) = ω₀(t)·ω₁(t)·[µ₀(t) − µ₁(t)]²`.
4. **Polarité** : le premier plan est la classe minoritaire (particules plus
   petites que le fond), inversable par l'opérateur.
5. Binarisation puis **étiquetage en composantes connexes 8-connexes**
   (parcours en largeur sur une file d'indices).
6. Les composantes de moins de 4 pixels sont éliminées comme bruit.

### Mesures par particule

| Grandeur | Formule |
| --- | --- |
| Aire *A* | nombre de pixels de la composante |
| Périmètre *P* | somme des côtés de pixels exposés au fond |
| Barycentre (x̄, ȳ) | moyenne des coordonnées de la composante |
| Moments centraux | μ₂₀, μ₀₂, μ₁₁ calculés sur la composante |
| Grand axe | 4·√λ₁ avec λ₁ la plus grande valeur propre de la matrice des moments |
| Petit axe | 4·√λ₂ |
| Orientation | ½·atan2(2μ₁₁, μ₂₀ − μ₀₂) |
| **Diamètre équivalent** | d = 2·√(A/π) |
| **Circularité** | C = 4πA / P² (1 = disque parfait) |
| Rapport d'aspect | petit axe / grand axe |

### Statistiques

- **D10, D50, D90** : percentiles calculés par interpolation linéaire sur les
  diamètres équivalents triés — D50 est le diamètre médian.
- Écart type : estimateur non biaisé (n−1).
- **Surface occupée** : Σ aire des particules / (largeur × hauteur) × 100.
- Classes granulométriques : largeur réglable, fréquence relative en % et
  courbe cumulative des passants.

---

## Base de données locale

- Moteur : **H2 2.2.224** en mode fichier, sans serveur.
- Emplacement : `~/.granulometrie-studio/history.mv.db`.
- Tables :
  - `session` : image (BLOB PNG), dimensions, calibration, paramètres, seuil,
    effectif, D10/D50/D90, commentaire, horodatage ;
  - `grain` : une ligne par particule (aire, périmètre, barycentre, bbox, axes,
    orientation, circularité, diamètre équivalent, contact bord) avec clé
    étrangère vers `session`.
- Les écritures sont transactionnelles (rollback en cas d'erreur).

Pour repartir de zéro, supprimez simplement le dossier `~/.granulometrie-studio`.

---

## Exports

### CSV (`;` — UTF-8 avec BOM, ouvrable directement dans Excel)

```
# Granulometrie Studio - export du 11/11/2026 14:32:05
# Image;exemple-grains.jpg
# Dimensions;2048 x 1536 px
# Calibration;0,50 um/px
# Seuil applique;128 (Otsu automatique : 128)
# Particules retenues;412 / detectees : 587
# Diametre moyen (µm);38,21
# D10 / D50 / D90 (µm);12,04 ; 31,77 ; 68,90
#
N°;Diametre equivalent (µm);Aire (µm²);Perimetre (px);...
1;24,8600;485,5600;78,0000;...
```

### Graphiques

Les trois graphiques de l'onglet *Distribution* sont exportés dans un seul PNG
(histogramme des tailles, courbe cumulative, surface occupée).

---

## Tests

```bash
mvn test
```

Couverture fonctionnelle (JUnit 5) :

| Classe de test | Vérifie |
| --- | --- |
| `ImageAnalyzerTest` | détection de 3 disques de Ø connus, seuil d'Otsu, polarité inversée, filtres min/max, exclusion des bords, calibration, seuil manuel, cohérence des statistiques |
| `StatisticsUtilsTest` | moyenne, écart type, médiane, percentiles interpolés, listes vides |
| `SizeClassifierTest` | regroupement en classes, somme des fréquences = 100 %, courbe cumulative, conversion d'unités |
| `CsvExporterTest` | bloc de métadonnées, en-têtes, une ligne par particule, unités calibrées |
| `AnalysisRepositoryTest` | enregistrement, liste triée, rechargement (image + particules), suppression |

Les tests s'exécutent sans interface graphique.

---

## Packaging / distribution

- **Jar + lib** (par défaut) : `mvn clean package` puis `lancer.sh` / `lancer.bat`.
- **Exécutable natif** (facultatif, JDK 17+) : `jpackage` peut empaqueter
  l'application avec son runtime Java :

  ```bash
  jpackage --name GranulometrieStudio --input target/lib --main-jar granulometrie-studio.jar \
           --main-class fr.granulometrie.Launcher \
           --module-path target/lib/javafx --add-modules javafx.controls,javafx.swing
  ```

- **IDE** : projet Maven standard, importable dans IntelliJ IDEA, Eclipse ou VS Code.

---

## Ce qui a changé par rapport à la version 2.5

| Axe | Version 2.5 (d'origine) | Version 3.0.0 |
| --- | --- | --- |
| Build | Projet Eclipse (`.classpath`), JavaFX SDK local, jar manuel | **Maven**, Java 17, dépendances déclaratives |
| Traitement d'image | Sources d'ImageJ (351 fichiers, ~20 Mo) copiées dans le dépôt | **Moteur Java autonome** (~400 lignes), Otsu + composantes connexes |
| Base de données | MySQL local obligatoire (root, mot de passe, script SQL, `localhost:3306`) | **H2 embarquée**, création automatique, aucune installation |
| Interface | FXML + contrôleurs à état statique, alertes anglicisées, une fenêtre par vue | Interface **programmatique** modernisée (thème CSS), navigation par onglets, libellés français |
| Calibration | Absente (pixels uniquement) | **µm/pixel**, unités et filtres convertis |
| Analyse | Seuil fixe, résultats non reproductibles | Seuil **Otsu** ou manuel, paramètres archivés avec chaque session |
| Mesures | Aire, bbox, circularité approchée | Aire, périmètre, barycentre, **moments d'ordre 2**, diamètre équivalent, aspect |
| Statistiques | Histogrammes seulement | **D10/D50/D90**, médiane, écart type, surface couverte |
| Contrôle visuel | Aucun | **Calque de détection** (contours + numéros) |
| Robustesse | État statique, exceptions non gérées, `NullPointerException` potentielles | Threads de fond, validation des saisies, messages d'erreur explicites |
| Tests | Un test JUnit incomplet | **23 tests** JUnit 5 sur le moteur, les exports et la persistance |
| Documentation | README seul | README + **documentation PDF** + **présentation PowerPoint** |

---

## Limites connues

- Le seuillage global suppose un **éclairage homogène** ; sur les images très
  contrastées localement, un seuillage adaptatif serait préférable.
- Les particules qui se **chevauchent** sont comptées comme une seule composante
  (pas de séparation par ligne de partage des eaux).
- La circularité est calculée sur un périmètre estimé par contours de pixels :
  elle est légèrement sous-estimée pour les très petits objets (< 6 px).
- Les images sont chargées entièrement en mémoire (adapté aux images de
  microscopie jusqu'à ~50 mégapixels).
- Les binaires JavaFX étant spécifiques au système, un paquet construit sous
  Linux ne se lance pas sous Windows (reconstruire sur la plateforme cible).

---

## Licence et crédits

Projet distribué sous licence **MIT** (voir `LICENSE`).

*Application de Granulométrie* — Alex Alkhatib.
