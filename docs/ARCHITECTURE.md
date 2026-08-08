# Architecture de l'application

## Principe

Un seul téléphone sert de conducteur de partie. Il est posé près du matériel
de jeu et peut envoyer le son vers une enceinte. Les joueurs continuent à
gérer physiquement leurs pierres, leurs déplacements et leurs rôles secrets.

L'application persiste uniquement des données techniques :

- mode débutant ou confirmé ;
- nombre de joueurs pour la liste du matériel et seuil de l'appel de la meute,
  calculé une fois au lancement ;
- mode de tirage et cartes Nuit restantes ;
- numéro du tour, couleur courante et couleur suivante ;
- écran, étape et temps restant ;
- position dans la séquence audio.

Aucun nom, rôle ou historique individuel n'existe dans le modèle.

## Couches

```text
Interface Compose plein écran
        │ actions / état observable
        ▼
GameViewModel — automate et minuteries
        ├── GameSequenceFactory — scripts dynamiques jaune / vert
        ├── NightDeck — paquet 4 jaunes + 4 vertes sans remise
        ├── AudioEngine — deux MediaPlayer, focus audio et repli silencieux
        ├── AudioRouteMonitor — téléphone / Bluetooth / USB / filaire
        ├── GitHubReleaseUpdateChecker — contrôle non bloquant de version
        ├── AppUpdateDownloadManager — téléchargement et installateur Android
        └── GameSessionRepository — reprise locale via SharedPreferences
```

La résolution audio est volontairement dynamique. Chaque séquence indique un
basename tel que `jaune_303_reveil_meute_jaune`. `AudioEngine` cherche la
ressource correspondante dans `res/raw`. Si elle n'existe pas, l'application
reste utilisable avec le texte et une minuterie silencieuse.

Le moteur conserve deux lecteurs indépendants sous un même focus audio :

- un lecteur d'ambiance stéréo atténué à 28 %, utilisé pour la nuit et pour les
  boucles propres à chaque étape du jour ;
- un lecteur de premier plan à plein volume pour les voix, les bips, le
  cocorico et les autres effets.

Le passage au Jour arrête la boucle nocturne. Si l'ambiance de jour est activée,
la concertation utilise `jour_8min`, le conseil enchaîne `gong` puis
`conseil_villageois_votes_boucle`, et la guérison utilise `guerison_boucle`.
L'ambiance diurne historique reste active pendant le tirage, jusqu'au lancement
de la nuit suivante.

## Automate principal

```text
ACCUEIL
  ├── AIDES
  └── CONFIGURATION
          ▼
      INTRODUCTION
          ▼
      PREMIÈRE NUIT ── aucune couleur, seul le loup-garou de sang se réveille
          ▼
         JOUR
          ├── concertation
          ├── conseil
          └── guérison
                 ├── loup-garou de sang découvert ──► FIN A
                 └── partie continue
                           ▼
                    TIRAGE JAUNE / VERT
                           ▼
                    NUIT SUIVANTE
                           ├── réveil jaune ou vert selon le tirage
                           ├── appel juste de la meute ──► FIN B1
                           └── appel erroné ─────────────► FIN B2
```

Une nuit commencée, un jour commencé ou un tirage en attente peut être mis en
pause et repris après redémarrage. Une déconnexion de l'enceinte met
automatiquement la lecture guidée en pause.

## Séquence d'une nuit

1. Première nuit confirmée : piste continue de 2 minutes 32, découpée en six
   présentations visuelles. L'action « Avancer » remplace la piste en cours par
   l'une des quatre reprises fournies et positionne le minuteur au seuil associé.
2. Autres nuits : départ fixe de 45 secondes accompagné par l'ambiance nocturne.
3. Nuits suivantes : réveil des loups et des goules de la couleur tirée.
4. En mode débutant, consignes détaillées de conseil et de morsure.
5. Conseil nocturne de 1 minute 55, puis onze bips, rendormissement, chant du coq
   et réveil du village. Ces quatre dernières étapes sont incluses dans la piste
   continue de la première nuit confirmée et n'y sont donc pas rejouées.

## Choix techniques

- Kotlin, Jetpack Compose et Material 3.
- API minimale 26, cible Android 35.
- Deux `MediaPlayer` natifs pour mixer ambiance et consignes sans dépendre d'une bibliothèque réseau
  supplémentaire.
- `SharedPreferences` pour un petit état local et déterministe.
- Permission Internet limitée au contrôle HTTPS de la dernière release GitHub
  au démarrage. Un échec, une absence de réseau ou une réponse invalide sont
  ignorés et n'empêchent jamais l'utilisation hors ligne.
- Orientation paysage et écran maintenu allumé pendant la partie.
