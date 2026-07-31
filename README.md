# Le Retour de la Bête — conducteur Android

[![Analyse OSV](https://github.com/thomasricaud/le-retour-de-la-bete/actions/workflows/security-report.yml/badge.svg?branch=main)](https://github.com/thomasricaud/le-retour-de-la-bete/actions/workflows/security-report.yml)
[![CodeQL](https://github.com/thomasricaud/le-retour-de-la-bete/actions/workflows/codeql.yml/badge.svg?branch=main)](https://github.com/thomasricaud/le-retour-de-la-bete/actions/workflows/codeql.yml)

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

Elle ne contient aucun compte, aucune fonctionnalité de jeu en réseau, aucune
génération vocale et aucune synchronisation entre téléphones. Une requête HTTPS
non bloquante vérifie uniquement la dernière release GitHub au démarrage.

## Aperçu

<table>
  <tr>
    <td>
      <img
        src="app/src/main/res/drawable-nodpi/bg_village_night.webp"
        alt="Le village et la pleine lune pendant la phase de nuit"
      >
    </td>
    <td>
      <img
        src="app/src/main/res/drawable-nodpi/bg_village_day.webp"
        alt="Le village et son clocher pendant la phase de jour"
      >
    </td>
  </tr>
  <tr>
    <td align="center"><strong>La nuit</strong></td>
    <td align="center"><strong>Le jour</strong></td>
  </tr>
</table>

## État du prototype

- Interface Compose plein écran, verrouillée en paysage.
- Deux modes : débutant et confirmé.
- Tirage des nuits dans l'application ou avec les cartes physiques.
- Départ configurable à 30 ou 45 secondes.
- Concertation du jour libre ou minutée.
- Détection d'une sortie Bluetooth, USB ou filaire.
- Téléchargement suivi par le gestionnaire Android lorsqu'une release GitHub
  plus récente existe, puis proposition d'ouvrir l'installateur ; sans
  connexion, l'application démarre normalement et n'affiche aucune erreur.
- Le catalogue prévoit 66 ressources audio. L'ambiance nocturne tourne en
  continu pendant toute la nuit et l'ambiance de village diurne, sans voix,
  pendant tout le jour. Les consignes et effets (notamment le cocorico) sont
  joués simultanément au premier plan. Une ressource absente est remplacée par
  son texte affiché, sans bloquer la partie.

Le catalogue complet des 66 enregistrements attendus se trouve dans
[`docs/AUDIO_ASSETS.md`](docs/AUDIO_ASSETS.md). Le manifeste exploitable par un
outil de production se trouve dans
[`app/src/main/assets/audio_manifest.json`](app/src/main/assets/audio_manifest.json).
Les vingt annonces suffixées `_seuil_1` à `_seuil_5` énoncent directement le
seuil fixé au lancement de la partie et sont intégrées dans `res/raw`.

## Régénérer les sons

Le catalogue complet peut être régénéré dans `artifacts/`, sans modifier
automatiquement les ressources intégrées :

```powershell
python -m venv artifacts/tts-production/.venv
artifacts/tts-production/.venv/Scripts/python.exe -m pip install -r scripts/audio_requirements.txt
artifacts/tts-production/.venv/Scripts/python.exe scripts/build_day_village_ambience.py
artifacts/tts-production/.venv/Scripts/python.exe scripts/build_complete_audio_catalog.py
```

La commande de génération du catalogue envoie uniquement les textes manquants
du manifeste au service Microsoft Edge TTS. Elle n'envoie ni les enregistrements
sources, ni les autres fichiers du dépôt. Les résultats restent dans
`artifacts/` afin de permettre une nouvelle écoute avant tout remplacement.

1. Produire les fichiers MP3 avec les noms exacts du catalogue.
2. Après validation, copier tous les MP3 directement dans
   `app/src/main/res/raw/` — Android
   n'accepte pas de sous-dossiers dans ce répertoire.
3. Recompiler. Aucune modification Kotlin n'est nécessaire.

## Construire

Pré-requis : JDK 17 et Android SDK 35.

```powershell
.\gradlew.bat testDebugUnitTest --offline --no-daemon
.\gradlew.bat assembleDebug --offline --no-daemon
.\gradlew.bat lintDebug --offline --no-daemon
```

APK de développement :
`app/build/outputs/apk/debug/app-debug.apk`.

La variante debug utilise le package distinct `fr.leretourdelabete.debug` et le
libellé « Le Retour de la Bête (Debug) ». Elle peut rester installée à côté de la
version publique ; ses données sont réservées aux essais et peuvent être effacées
sans toucher aux parties de production. La recherche et l'installation des mises
à jour publiques sont désactivées dans cette variante.

## Télécharger une version installable

La version `release` signée est produite automatiquement par GitHub Actions.

- Page publique :
  `https://thomasricaud.github.io/le-retour-de-la-bete/`
- APK direct :
  `https://thomasricaud.github.io/le-retour-de-la-bete/le-retour-de-la-bete.apk`
- Procédure et signature : [`docs/RELEASE.md`](docs/RELEASE.md)

## Sécurité et dépendances

Le dépôt ne contient pas de bibliothèque d’IA embarquée ni de binaire Android
opaque. Les dépendances d’exécution sont résolues depuis Google Maven et Maven
Central, puis vérifiées par SHA-256 pendant chaque build.

Chaque release publique fournit l’APK signé, son empreinte SHA-256, l’empreinte
de son certificat de signature, une nomenclature CycloneDX des composants et
deux attestations GitHub : provenance du build et association entre l’APK et
son SBOM. CodeQL, Dependabot et la revue des dépendances complètent ces
contrôles. OSV-Scanner vérifie également le SBOM à chaque changement, chaque
semaine et avant publication : une vulnérabilité connue bloque la release. Le
rapport lisible et les résultats JSON/SARIF sont publics dans chaque release et
sur la page de téléchargement. Les modalités de signalement privé et les
limites de ces garanties sont décrites dans [`SECURITY.md`](SECURITY.md).

Le certificat public attendu est conservé dans
[`distribution/release-signing-certificate.sha256`](distribution/release-signing-certificate.sha256).
Les builds de release sont interrompus si une autre clé est utilisée, afin que
les utilisateurs existants puissent toujours installer les mises à jour.

## Visuels

Les illustrations originales conservées en haute définition sont dans
`artwork/source/`. Les ressources Android optimisées sont dans
`app/src/main/res/drawable-nodpi/` et `app/src/main/res/mipmap-*/`.

Le script `scripts/prepare_android_images.py` régénère les WebP et toutes les
tailles d'icône à partir des trois sources.

## Créateurs et licence

- **Game designer : Louis-Philippe Marcelino**
- **Développeur : Thomas Ricaud**

Le code source est distribué sous licence MIT. Le contenu original du jeu est
distribué sous licence
[Creative Commons Attribution 4.0 International](https://creativecommons.org/licenses/by/4.0/).
L'utilisation, la modification et les forks sont autorisés à condition de
conserver l'attribution des créateurs et d'indiquer les modifications
effectuées. Les conditions complètes figurent dans [`LICENSE`](LICENSE).

## Documentation

- [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) : architecture et états.
- [`docs/RULES.md`](docs/RULES.md) : évolutions de règle appliquées par
  l'application.
- [`docs/AUDIO_ASSETS.md`](docs/AUDIO_ASSETS.md) : textes exacts à enregistrer.
- [`docs/IMAGE_ASSETS.md`](docs/IMAGE_ASSETS.md) : prompts et chemins des visuels.
- Les trois PDF d'origine restent la référence des règles, complétée par les
  évolutions consignées dans [`docs/RULES.md`](docs/RULES.md).
