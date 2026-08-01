# Consignes pour les agents

## Environnement Android

- Travailler depuis la racine du dépôt sous PowerShell.
- Utiliser le JDK 17 installé dans
  `C:\Users\Thomas\AppData\Local\Programs\jdk-17`.
- Le binaire vérifié est
  `C:\Users\Thomas\AppData\Local\Programs\jdk-17\bin\java.exe`
  (Temurin OpenJDK 17.0.19).
- Le projet compile avec Java 17 et Kotlin/JVM 17. Ne pas laisser un ancien
  `JAVA_HOME` ou un Java 8 prendre la priorité.

Configurer la session avant toute commande Gradle :

```powershell
$env:JAVA_HOME = 'C:\Users\Thomas\AppData\Local\Programs\jdk-17'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
& "$env:JAVA_HOME\bin\java.exe" -version
```

Le SDK Android est renseigné localement dans `local.properties`. Le projet
utilise `compileSdk` 37, `targetSdk` 35 et `minSdk` 26.

## Build et validation

La validation locale de référence doit exécuter les tests unitaires, produire
l'APK debug et lancer Android Lint :

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug --offline --no-daemon --no-parallel
```

Cette commande a été validée avec le JDK ci-dessus et Gradle 9.6.1. Conserver
`--offline` pour les validations reproductibles lorsque les dépendances sont
déjà en cache. Si une dépendance nouvelle manque réellement du cache, relancer
ponctuellement sans `--offline` avec l'accès réseau approprié.

Pour un contrôle ciblé pendant le développement :

```powershell
.\gradlew.bat testDebugUnitTest --offline --no-daemon --no-parallel
.\gradlew.bat assembleDebug --offline --no-daemon --no-parallel
.\gradlew.bat lintDebug --offline --no-daemon --no-parallel
```

Les pull requests et les releases exécutent aussi un test instrumenté sur un
appareil Android virtuel API 35. Ce test installe l'APK debug, lance
`MainActivity` et vérifie le parcours de l'accueil jusqu'à la préparation
d'une partie. Pour le reproduire sur une machine capable de lancer
l'émulateur Android :

```powershell
.\gradlew.bat pixel2Api35DebugAndroidTest `
  "-Pandroid.testoptions.manageddevices.emulator.gpu=swiftshader_indirect" `
  --no-daemon --no-parallel
```

Le passage de ce test dans GitHub Actions est nécessaire pour valider une mise
à niveau Android, Gradle, Kotlin ou Compose proposée par Dependabot.

L'APK produit se trouve dans
`app\build\outputs\apk\debug\app-debug.apk`. La variante debug utilise le
package `fr.leretourdelabete.debug` et peut cohabiter avec la version publiée.

Ne pas considérer un build comme validé si l'une des trois tâches de référence
échoue. Pour une publication signée, suivre également `docs/RELEASE.md` et ne
pas enregistrer de secrets de signature dans le dépôt.
