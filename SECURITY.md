# Politique de sécurité

## Versions prises en charge

Seule la dernière version publiée de l’application reçoit les correctifs de
sécurité. Les APK officiels sont disponibles dans les releases GitHub et sur la
page de téléchargement du projet.

## Signaler une vulnérabilité

Utilisez le bouton **Report a vulnerability** de l’onglet **Security** du dépôt
GitHub afin d’envoyer un rapport privé. N’ouvrez pas d’issue publique contenant
des instructions d’exploitation, des secrets ou des données personnelles.

Indiquez si possible :

- la version concernée ;
- les étapes de reproduction ;
- l’impact observé ou supposé ;
- une proposition de correction, si vous en avez une.

## Vérifier une APK officielle

Chaque release contient :

- l’APK signé ;
- son empreinte SHA-256 ;
- l’empreinte SHA-256 du certificat de signature Android ;
- une nomenclature CycloneDX des dépendances d’exécution ;
- un rapport OSV lisible, son résumé JSON et les résultats JSON/SARIF complets ;
- des attestations GitHub reliant l’APK au dépôt, au commit et au workflow qui
  l’ont produit.

Le certificat attendu est enregistré dans
`distribution/release-signing-certificate.sha256`. Les builds locaux et GitHub
Actions échouent si la clé utilisée ne correspond pas à cette empreinte. Cette
protection évite de publier une APK que les téléphones déjà équipés refuseraient
comme mise à jour.

La provenance peut être vérifiée après téléchargement avec :

```text
gh attestation verify le-retour-de-la-bete.apk \
  --repo thomasricaud/le-retour-de-la-bete
```

La signature et les attestations prouvent l’origine et l’intégrité d’un fichier.
Elles ne garantissent pas à elles seules l’absence de vulnérabilité. Les builds
sont donc également contrôlés par Android Lint, CodeQL, la revue des dépendances,
les alertes Dependabot et OSV-Scanner. L’analyse OSV est relancée à chaque
changement de `main`, chaque semaine et avant chaque release ; une vulnérabilité
connue ou une analyse incomplète bloque la publication.
