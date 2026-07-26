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
6. publie l’APK comme artifact GitHub Actions ;
7. crée une GitHub Release pour le tag ;
8. déploie une page GitHub Pages avec un lien APK stable.

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
