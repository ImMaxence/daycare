https://adresse.data.gouv.fr/outils/csv

https://monenfant.fr/que-recherchez-vous

---

# Daycare Scraper – monenfant.fr

Mini-projet Java + Maven + Playwright qui va chercher, sur monenfant.fr, la liste des
crèches (EAJE), relais/lieux d'accueil parents-enfants (RPE;LAEP) et accueils de
loisirs (ALSH) autour de 7 villes (Aix-en-Provence, Salon-de-Provence, Pertuis,
Manosque, Marseille, Vitrolles, Trets), dans un rayon de 30 km.

Le script :
1. ouvre chaque URL de recherche dans Chromium (piloté par Playwright),
2. clique sur "J'ai compris" (bandeau cookies),
3. clique sur "Afficher la liste", ce qui déclenche le 1er appel à l'API JSON
   `.../api/monenfantmodedegardefront/v1/modedegarde/search?...&page=0`,
4. intercepte cette réponse JSON, récupère `mainResults`,
5. clique sur "page suivante" tant que le bouton n'est pas désactivé / que
   `lastPage` est `false`, en interceptant chaque nouvelle réponse,
6. écrit tous les résultats bruts dans `output/raw_all.json`,
7. supprime les doublons par `id` -> `output/deduped.json`,
8. exporte le résultat dédoublonné en CSV -> `output/daycares.csv`.

## Prérequis

- Java 21+ (le projet est configuré pour compiler avec `--release 21`, testé avec
  un JDK 26 installé en local).
- Maven (soit installé sur la machine, soit via le wrapper fourni `./mvnw`, qui ne
  nécessite qu'un JDK dans le PATH — il télécharge Maven tout seul au premier lancement).
- Dans IntelliJ : ouvrez simplement le dossier `data/` (ou le `pom.xml`), IntelliJ
  détecte le projet Maven automatiquement. Vous pouvez tout lancer depuis l'onglet
  Maven à droite, sans rien installer de plus (IntelliJ gère son propre JDK/Maven
  si besoin, via *File > Project Structure*).

## Installation des navigateurs Playwright (une seule fois)

Playwright a besoin de télécharger Chromium avant la 1ère exécution :

```bash
./mvnw compile
./mvnw exec:java -Dexec.mainClass="com.microsoft.playwright.CLI" -Dexec.args="install chromium"
```

(Depuis IntelliJ : Maven tool window > Plugins > exec > exec:java, en définissant
les propriétés `exec.mainClass` et `exec.args` ci-dessus, ou plus simplement lancez
la commande depuis le terminal intégré d'IntelliJ.)

## Lancer le scraping complet (scrape + dédoublonnage + export CSV)

```bash
./mvnw compile exec:java
```

Par défaut Chromium tourne en mode headless. Pour le voir travailler :

```bash
./mvnw compile exec:java -Dexec.args="all false"
```

## Lancer une seule étape

```bash
./mvnw compile exec:java -Dexec.args="scrape"   # uniquement le scraping -> output/raw_all.json
./mvnw compile exec:java -Dexec.args="dedupe"   # uniquement le dédoublonnage -> output/deduped.json
./mvnw compile exec:java -Dexec.args="csv"      # uniquement l'export CSV -> output/daycares.csv
```

## Résultats

- `output/raw_all.json` : toutes les fiches trouvées, sans dédoublonnage, avec 3
  champs de traçabilité ajoutés (`searchLabel`, `searchStructureType`, `searchUrl`)
  indiquant quelle recherche a trouvé la fiche.
- `output/deduped.json` : mêmes fiches, dédoublonnées par `id`.
- `output/daycares.csv` : export CSV du fichier dédoublonné (UTF-8, séparateur `,`).

## Structure du code

- `com.daycare.config.SearchTargets` : liste des 21 recherches (7 villes × 3 types
  de structure), générées à partir de coordonnées/URLs pour éviter les erreurs de
  copier-coller.
- `com.daycare.model` : modèles Jackson (`ApiSearchResponse`, `DaycareRecord`)
  calqués sur le JSON retourné par l'API.
- `com.daycare.scraper.DaycareScraper` : logique Playwright (cookies, bascule en
  vue liste, interception de l'API, pagination).
- `com.daycare.util` : lecture/écriture JSON, dédoublonnage, export CSV.
- `com.daycare.Main` : orchestration (scrape → dedupe → csv).

## Ajouter une ville / un type de recherche

Il suffit d'ajouter une entrée dans la liste `CITIES` de `SearchTargets.java`
(nom, `location` déjà encodée telle que fournie par monenfant.fr, x, y) — les 3
recherches (EAJE / RPE;LAEP / ALSH) seront générées automatiquement pour cette ville.
