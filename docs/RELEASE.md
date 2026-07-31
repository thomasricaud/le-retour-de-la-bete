# Publication Android

## Résultat

Un tag Git `v*` déclenche `.github/workflows/android-release.yml`.

Le workflow :

1. valide un tag au format `vMAJEUR.MINEUR.CORRECTIF` et injecte cette version
   dans l'APK ;
2. calcule un `versionCode` Android croissant à partir des trois composantes ;
3. exécute les tests unitaires et Android Lint ;
4. construit un APK `release` signé ;
5. vérifie cryptographiquement la signature ;
6. produit une nomenclature CycloneDX des dépendances d’exécution ;
7. analyse le SBOM avec OSV-Scanner et bloque la publication en cas de
   vulnérabilité connue ou d’analyse incomplète ;
8. génère un rapport public lisible ainsi que les résultats JSON/SARIF ;
9. génère des attestations cryptographiques de provenance et de SBOM ;
10. publie l’APK, son empreinte, l’empreinte du certificat, son SBOM et les
    rapports de sécurité comme artifact GitHub Actions ;
11. crée une GitHub Release immuable pour le tag ;
12. déploie une page GitHub Pages avec un lien APK stable.

## Contrôles de chaîne d’approvisionnement

- Le wrapper Gradle vérifie le SHA-256 officiel de la distribution Gradle.
- `gradle/verification-metadata.xml` vérifie les SHA-256 des plugins et
  dépendances, y compris les dépendances transitives.
- Toutes les actions GitHub sont référencées par leur SHA de commit complet.
- CodeQL analyse le code Kotlin/Java à chaque changement de `main`, pull request
  et exécution hebdomadaire.
- Dependabot et la revue des dépendances signalent ou refusent les versions
  affectées par une vulnérabilité connue. Le graphe soumis à GitHub est limité
  à `:app:releaseRuntimeClasspath`, c’est-à-dire aux composants destinés à
  l’APK public, et non aux outils utilisés seulement pour construire le projet.
- `.github/workflows/security-report.yml` analyse le SBOM avec OSV-Scanner à
  chaque changement de `main`, sur les pull requests et chaque semaine. Il
  publie un artifact lisible et envoie le résultat SARIF dans GitHub Security.
- Le workflow de release refait la même analyse sur le SBOM exact de l’APK et
  publie `security-report.md`, `security-report.json`, `osv-results.json` et
  `osv-results.sarif` avec la release et sur GitHub Pages.
- La release est d’abord assemblée en brouillon avec tous ses fichiers, puis
  publiée. Une exécution ultérieure ne remplace jamais un fichier publié : elle
  vérifie que les octets sont identiques ou échoue.
- L’empreinte du certificat doit correspondre exactement à
  `distribution/release-signing-certificate.sha256`. Une autre clé fait échouer
  le build avant publication afin de préserver les mises à jour des
  installations existantes.

Après téléchargement, vérifier la provenance de l’APK avec :

```powershell
gh attestation verify .\le-retour-de-la-bete.apk `
  --repo thomasricaud/le-retour-de-la-bete
```

Adresse de téléchargement prévue :

`https://thomasricaud.github.io/le-retour-de-la-bete/`

Adresse directe de l’APK :

`https://thomasricaud.github.io/le-retour-de-la-bete/le-retour-de-la-bete.apk`

## Détection des mises à jour dans l'application

À chaque démarrage, l'application interroge en arrière-plan la dernière release
publique via l'API GitHub. Elle compare son `versionName` au `tag_name` de la
release, au format `vMAJEUR.MINEUR.CORRECTIF`.

Le numéro installé est lu dynamiquement depuis le build : aucune version
particulière n'est codée en dur. Toute version plus ancienne peut donc proposer
directement la release la plus récente, même si plusieurs versions
intermédiaires ont été ignorées.

Lors d'une publication, le workflow injecte automatiquement le numéro du tag
dans l'APK. Les valeurs par défaut de `versionName` et `versionCode` peuvent
être alignées dans le commit de release pour les builds locaux, mais le tag
reste la source d'autorité de l'APK publié. Pour conserver un ordre strict des
`versionCode`, les composantes MINEUR et CORRECTIF doivent rester comprises
entre 0 et 999.

Si la release est plus récente, un dialogue propose de télécharger l'asset
`le-retour-de-la-bete.apk` via le `DownloadManager` Android. L'application suit
la progression, vérifie la fin du téléchargement, puis propose d'ouvrir
l'installateur Android. Le navigateur n'intervient plus dans ce flux.

Au premier essai, Android peut demander d'autoriser temporairement
l'installation depuis Le Retour de la Bête. L'application ouvre le réglage
ciblé puis reprend l'installation au retour. Aucune installation n'est
silencieuse.

La requête utilise des délais courts et toute erreur réseau, API ou de format
est ignorée : le démarrage et la partie restent entièrement utilisables hors
ligne.

## Secrets GitHub nécessaires

- `ANDROID_SIGNING_KEY_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

La clé privée et ses mots de passe ne doivent jamais être ajoutés à Git. Une
copie locale chiffrée est conservée dans le dossier ignoré `.signing/`.

Cette clé doit être sauvegardée durablement : sans elle, une future version ne
pourra pas remplacer l’application déjà installée sur un téléphone.

- Construction locale signée :
  `.\scripts\build_signed_release.ps1`
- Affichage volontaire des identifiants pour les placer dans un gestionnaire
  de mots de passe :
  `.\scripts\show_release_signing_credentials.ps1`

Le fichier `credentials.dpapi.xml` est chiffré pour le compte Windows actuel.
Il ne suffit donc pas, seul, pour restaurer les mots de passe sur un autre
ordinateur. Sauvegarder le fichier JKS et ses identifiants dans un emplacement
privé distinct.

## Lancer une publication

```powershell
git tag -a v0.1.0 -m "Version 0.1.0"
git push origin v0.1.0
```

Un lancement manuel depuis l’onglet Actions reconstruit et redéploie l’APK,
mais seule une exécution déclenchée par un tag crée une GitHub Release.

## Actualiser uniquement la page de téléchargement

Une modification de `distribution/index.html` ou de ses trois images déclenche
`.github/workflows/pages-refresh.yml`. Ce workflow ne construit aucune APK et
ne crée ni tag ni release. Il télécharge les fichiers inchangés de la dernière
release publique, vérifie leur empreinte, le certificat, le rapport OSV, le
SBOM et le résultat CodeQL du commit publié, puis redéploie seulement GitHub
Pages.

La page lit `security-report.json` dans le navigateur pour afficher la version,
la date de l’analyse, le nombre de vulnérabilités connues et le nombre de
composants. Si ce rapport est absent ou invalide, le bandeau passe dans un état
d’avertissement et ne présente pas les valeurs précédentes comme actuelles.
