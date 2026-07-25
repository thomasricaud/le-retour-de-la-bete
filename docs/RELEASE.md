# Publication Android

## Résultat

Un tag Git `v*` déclenche `.github/workflows/android-release.yml`.

Le workflow :

1. exécute les tests unitaires et Android Lint ;
2. construit un APK `release` signé ;
3. vérifie cryptographiquement la signature ;
4. publie l’APK comme artifact GitHub Actions ;
5. crée une GitHub Release pour le tag ;
6. déploie une page GitHub Pages avec un lien APK stable.

Adresse de téléchargement prévue :

`https://thomasricaud.github.io/le-retour-de-la-bete/`

Adresse directe de l’APK :

`https://thomasricaud.github.io/le-retour-de-la-bete/le-retour-de-la-bete.apk`

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
