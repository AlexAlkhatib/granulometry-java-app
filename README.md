# 🔬 **Application de Granulométrie — Outil d’analyse d’images scientifiques**

**Application de Granulométrie** est un logiciel Java permettant d’**extraire**, **quantifier** et **visualiser** les particules présentes dans des images scientifiques.
Le projet vise à automatiser l’analyse d’images de grains, à générer des visualisations statistiques et à offrir un outil fonctionnel pour la recherche ou l’industrie.


## 🎯 Objectifs du projet

* Concevoir un outil d’analyse d’images automatisé capable de détecter des grains/particules dans des images
* Quantifier leurs tailles, distributions et propriétés morphologiques
* Générer automatiquement des visualisations statistiques (histogrammes, courbes, etc.)
* Utiliser Java + JavaFX pour créer une application GUI utilisable sur poste desktop
* Appliquer des méthodes d’imagerie scientifique et de traitement de données


## 🧰 Stack technique et architecture

* **Langage** : Java
* **Interface graphique** : JavaFX (module-path `javafx-sdk-17.0.1\lib`, modules `javafx.controls, javafx.fxml`)
* **Traitement d’image** : bibliothèques Java pour lecture/traitement/segmentation des particules
* **Analyse & statistiques** : extraction de caractéristiques, calcul de distributions, génération de graphiques
* **Packaging** : génération d’un fichier `.jar` exécutable depuis Eclipse
* **Licence** : Open Source


## ✅ Fonctionnalités principales

* Chargement d’images contenant des grains ou particules
* Segmentation et détection des particules
* Extraction de métriques morphologiques (taille, forme, nombre)
* Visualisation des résultats via histogrammes ou graphiques statistiques
* Export possible des résultats ou rapports (selon implémentation)
* Mises à jour fréquentes (version active)
* Collaboration : remerciements aux contributeurs Noé, Hreshvik, Quentin, Joey


## 📂 Structure typique du projet

```
Application-de-Granulometrie/
 ├── src/
 │    ├── main/java/...         # code Java (traitement d’image, GUI JavaFX)
 │    ├── main/resources/...    # fichiers FXML, graphismes, styles
 ├── lib/                       # librairies externes (JavaFX SDK)
 ├── build/                     # jar ou builds générés
 ├── README.md
 └── licence.txt                # licence open source
```


## 🚀 Installation & lancement

### 1. Pré-requis

* Java 17+
* JavaFX SDK 17.0.1 (ou compatible)
* Eclipse (ou un autre IDE Java compatible)

### 2. Compilation et exécution

Depuis Eclipse :

* Importer le projet
* Configurer le module-path :

  ```
  --module-path "javafx-sdk-17.0.1\lib" --add-modules javafx.controls,javafx.fxml
  ```
* Lancer la compilation : un fichier `.jar` sera généré
* Exécuter l’application :

  ```bash
  java --module-path "javafx-sdk-17.0.1\lib" --add-modules javafx.controls,javafx.fxml -jar ApplicationDeGranulometrie.jar
  ```


## 🧠 Compétences démontrées

- ✔ Traitement d’image et segmentation automatique
- ✔ Extraction de données scientifiques (granulométrie)
- ✔ Création d’interface utilisateur en JavaFX
- ✔ Packaging d’un logiciel Java exécutable
- ✔ Travail en équipe (collaborateurs mentionnés)
- ✔ Application de méthodes d’analyse des distributions de taille de particules (granulométrie) ([Wikipédia][1])


## 🔧 Pistes d’amélioration

* Ajouter une interface d’import/export de données (CSV, Excel)
* Implémenter des filtres avancés ou segmentation basée sur l’apprentissage automatique
* Visualisations interactives (zoom, sélection de particules)
* Support multi-plateforme amélioré (macOS, Linux)
* Intégrer des tests unitaires et d’intégration pour les pipelines d’analyse
* Packager en version exécutable native pour Windows/macOS


## 👤 À propos

Développeur passionné par l’analyse d’images, le traitement de données et les applications Java de qualité. Ce projet m’a permis de combiner mes compétences en programmation avec des besoins scientifiques concrets.

GitHub : **[https://github.com/AlexAlkhatib/](https://github.com/AlexAlkhatib)**


## 📄 Licence
MIT License
Copyright (c) 2025 Alex Alkhatib
