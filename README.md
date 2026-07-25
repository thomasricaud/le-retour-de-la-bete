# Le Retour de la Bête — conducteur Android

Application Android hors ligne destinée au téléphone qui diffuse le son de la
partie sur son haut-parleur ou sur une enceinte externe.

L'application ne connaît jamais les noms, les rôles ni les pierres des joueurs.
Elle pilote uniquement :

- l'introduction et les explications ;
- la succession nuit / jour / guérison ;
- les minuteries et les commandes lecture, pause, répéter, passer et arrêter ;
- la couleur jaune ou verte des nuits suivantes ;
- un paquet numérique de quatre nuits jaunes et quatre nuits vertes, tiré sans
  remise ;
- la sauvegarde technique de la phase en cours afin de reprendre une partie.

Elle ne contient aucun compte, aucun réseau, aucune génération vocale et aucune
synchronisation entre téléphones.

## État du prototype

- Interface Compose plein écran, verrouillée en paysage.
- Deux modes : débutant et confirmé.
- Tirage des nuits dans l'application ou avec les cartes physiques.
- Départ configurable à 30 ou 45 secondes.
- Concertation du jour libre ou minutée.
- Détection d'une sortie Bluetooth, USB ou filaire.
- Fonctionnement sans les enregistrements : texte affiché et minuterie
  silencieuse.

Le catalogue complet des 42 enregistrements à fournir se trouve dans
[`docs/AUDIO_ASSETS.md`](docs/AUDIO_ASSETS.md). Le manifeste exploitable par un
outil de production se trouve dans
[`app/src/main/assets/audio_manifest.json`](app/src/main/assets/audio_manifest.json).

## Ajouter les sons plus tard

1. Créer `app/src/main/res/raw/`.
2. Enregistrer ou produire les fichiers MP3 avec les noms exacts du catalogue.
3. Copier tous les MP3 directement dans `res/raw` — Android n'accepte pas de
   sous-dossiers dans ce répertoire.
4. Recompiler. Aucune modification Kotlin n'est nécessaire.

## Construire

Pré-requis : JDK 17 et Android SDK 35.

```powershell
.\gradlew.bat testDebugUnitTest --offline --no-daemon
.\gradlew.bat assembleDebug --offline --no-daemon
.\gradlew.bat lintDebug --offline --no-daemon
```

APK de développement :
`app/build/outputs/apk/debug/app-debug.apk`.

## Télécharger une version installable

La version `release` signée est produite automatiquement par GitHub Actions.

- Page publique :
  `https://thomasricaud.github.io/le-retour-de-la-bete/`
- APK direct :
  `https://thomasricaud.github.io/le-retour-de-la-bete/le-retour-de-la-bete.apk`
- Procédure et signature : [`docs/RELEASE.md`](docs/RELEASE.md)

## Visuels

Les illustrations originales conservées en haute définition sont dans
`artwork/source/`. Les ressources Android optimisées sont dans
`app/src/main/res/drawable-nodpi/` et `app/src/main/res/mipmap-*/`.

Le script `scripts/prepare_android_images.py` régénère les WebP et toutes les
tailles d'icône à partir des trois sources.

## Documentation

- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) : architecture et états.
- [`docs/AUDIO_ASSETS.md`](docs/AUDIO_ASSETS.md) : textes exacts à enregistrer.
- [`docs/IMAGE_ASSETS.md`](docs/IMAGE_ASSETS.md) : prompts et chemins des visuels.
- Les trois PDF d'origine restent la référence des règles.
