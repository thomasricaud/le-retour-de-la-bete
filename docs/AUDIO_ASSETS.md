# Ressources audio

Ce document est le contrat de production et d'intégration des 71 ressources audio
livrées avec l'application.

Les 66 fichiers audio du manifeste sont fournis dans le dépôt. Les textes
ci-dessous sont les textes français exacts utilisés pour les voix. Ne pas
ajouter à l'enregistrement les indications de production signalées comme non
parlées.

## État d'intégration

Les 66 entrées du manifeste ont exactement un MP3 correspondant dans
`app/src/main/res/raw/`. Les voix produites par synthèse utilisent
`fr-FR-RemyMultilingualNeural`, conformément à la validation. Cinq des six
montages locaux précédemment retenus sont conservés. Le synopsis et les deux
minuteries de départ 30/45 secondes ont été remplacés par les pistes validées
fournies pour cette version. Une quatrième piste dédiée au conseil de la
première nuit en mode confirmé a été ajoutée.

L'ambiance de jour validée représente une place de village détendue avec des
oiseaux, une fontaine, des pas et de petits bruits d'activité. Elle ne contient
aucune voix. Le script `scripts/build_day_village_ambience.py` permet de la
régénérer. `scripts/build_complete_audio_catalog.py` reconstruit le catalogue
complet dans `artifacts/` pour écoute avant remplacement. Le script
`scripts/build_local_audio_candidates.ps1` reproduit séparément les six montages
issus des longues pistes sources.

## Contrat Android

- Répertoire unique : `app/src/main/res/raw/`.
- Aucun sous-dossier n'est autorisé dans `res/raw`.
- Format attendu : MP3, avec l'extension `.mp3`.
- Le nom du fichier sans extension doit être strictement identique au `basename`
  documenté ci-dessous.
- Les noms utilisent uniquement les caractères Android valides : `a-z`, `0-9` et `_`.
- Exemple : le basename `commun_000_partie_prete` devient
  `app/src/main/res/raw/commun_000_partie_prete.mp3`.
- Ne pas renommer les ressources dans le code Kotlin : `AudioEngine` les résout par
  `getIdentifier(basename, "raw", packageName)`.

## Sémantique des durées moteur

Les durées indiquées sont les valeurs configurées dans le moteur au moment de la
rédaction de ce document.

- Pour une voix ou un effet sonore disponible, la durée réelle du MP3 fournie par
  `MediaPlayer` est utilisée.
- Si une voix de séquence manque, le moteur conserve le texte à l'écran et limite
  l'attente silencieuse à cinq secondes.
- Les deux niveaux de guidage utilisent temporairement le même conducteur audio
  confirmé. Les anciennes pistes débutant restent présentes pour une réutilisation
  future, mais ne sont plus jouées pendant une partie.
- La première nuit est une piste continue de 152 secondes. Les quatre
  pistes `*_avance_*` remplacent la lecture en cours lorsque le joueur avance vers
  un seuil précis.
- Les nuits suivantes utilisent la piste continue de 258 secondes et ses extraits
  dédiés aux actions « Avancer ».
- L'option « Ambiance sonore le jour » est activée par défaut. Pendant la
  concertation, `jour_8min` démarre immédiatement en durée libre ou avec le
  minuteur en durée imposée. Le conseil joue `gong` une fois puis
  `conseil_villageois_votes_boucle` en boucle. Le rituel joue
  `guerison_boucle` en boucle.
- `commun_013_ambiance_jour_boucle` reste l'ambiance du tirage de la prochaine
  nuit lorsque l'option de jour est activée.
- Les voix et effets utilisent un second `MediaPlayer` : ils ne coupent donc pas
  l'ambiance de phase et sont mixés par-dessus à plein volume. L'ambiance est
  atténuée à 28 %.
- `fin_partie_a` est joué automatiquement lorsque le loup-garou de sang est
  révélé. Le récit `aides_440` reste distinct et n'est lu que via l'action
  « Écouter la fin ». Les récits `aides_442` et `aides_443` restent également à
  la demande.

## Recommandations d'enregistrement

- Voix française claire, posée et théâtrale, intelligible sur une enceinte Bluetooth.
- Même voix, même distance de micro et même niveau sonore pour toutes les consignes.
- Éviter toute musique sous les consignes parlées.
- Supprimer les silences de tête et de fin ; le moteur gère les temps de jeu.
- Pour les voix : mono, 44,1 kHz ou 48 kHz, 96 à 128 kbit/s.
- Normaliser toutes les voix au même niveau perçu et éviter l'écrêtage.
- `commun_012_ambiance_nuit_boucle.mp3`,
  `commun_013_ambiance_jour_boucle.mp3`, `jour_8min.mp3`,
  `conseil_villageois_votes_boucle.mp3` et `guerison_boucle.mp3` doivent former
  des boucles sans raccord audible.

## Inventaire exhaustif

### Commun

#### `commun_000_partie_prete.mp3`

- Type : voix.
- Durée moteur : 6 secondes.
- Texte exact :

> Vérifiez les pierres, le sac de guérison et la boîte des loups. Quand tout le monde est prêt, lancez la première nuit.

#### `commun_002_bips_11.mp3`

- Type : effet sonore.
- Durée moteur : 11 secondes.
- Texte parlé : aucun.
- Production exacte : onze bips distincts, à raison d'un bip par seconde.

#### `commun_003_endormissement.mp3`

- Type : voix.
- Durée moteur : 7 secondes.
- Texte exact :

> Tous les villageois s'endorment. Fermez les yeux et gardez-les fermés jusqu'au réveil du village.

#### `commun_004_tous_se_rendorment.mp3`

- Type : voix.
- Durée moteur : 6 secondes.
- Texte exact :

> Tous les joueurs encore réveillés partent se rendormir.

#### `commun_005_cocorico.mp3`

- Type : effet sonore vocal ou chant de coq.
- Durée moteur : 5 secondes.
- Texte exact si l'effet est vocalisé :

> Cocorico !

#### `commun_006_reveil_village.mp3`

- Type : voix.
- Durée moteur : 5 secondes.
- Texte exact :

> Le village se réveille.

#### `commun_007_concertation_jour.mp3`

- Type : voix.
- Durée moteur : 8 secondes.
- Texte exact :

> Tous les joueurs se réveillent et se concertent, en groupe ou séparément.

#### `commun_008_conseil_villageois.mp3`

- Type : voix.
- Durée moteur : 8 secondes.
- Texte exact :

> Tous les joueurs se réunissent pour le conseil des villageois.

#### `commun_009_vote_guerison.mp3`

- Type : voix.
- Durée moteur : 14 secondes.
- Texte exact :

> Au conseil des villageois, votez pour choisir un joueur à guérir, puis procédez au rituel de guérison.

#### `commun_012_ambiance_nuit_boucle.mp3`

- Type : ambiance en boucle.
- Durée moteur : toute la phase Nuit ; utilisée seule au premier plan pour les
  conseils de 115 secondes qui n'ont pas de piste dédiée.
- Texte parlé : aucun.
- Production exacte : ambiance nocturne discrète, sombre et parfaitement bouclable,
  sans voix, sans signal de fin et sans événement sonore susceptible de masquer une
  consigne.

#### `commun_013_ambiance_jour_boucle.mp3`

- Type : ambiance en boucle.
- Durée moteur : tirage de la prochaine nuit.
- Texte parlé : aucun.
- Production exacte : ambiance diurne claire, plus légère que la nuit et parfaitement
  bouclable, sans voix ni événement susceptible de masquer une consigne.

#### `jour_8min.mp3`

- Type : ambiance en boucle.
- Durée moteur : toute la concertation du village.
- Démarrage : immédiat en durée libre ; au démarrage du minuteur en durée
  imposée.
- Texte parlé : aucun.

#### `gong.mp3`

- Type : effet sonore unique.
- Durée moteur : durée réelle du MP3.
- Usage : introduction du conseil des villageois.

#### `conseil_villageois_votes_boucle.mp3`

- Type : ambiance en boucle.
- Durée moteur : du terme du gong jusqu'à l'action « Procéder à la guérison ».
- Texte parlé : aucun.

#### `guerison_boucle.mp3`

- Type : ambiance en boucle.
- Durée moteur : toute la phase « Rituel de guérison ».
- Texte parlé : aucun.

#### `fin_partie_a.mp3`

- Type : effet sonore unique.
- Durée moteur : durée réelle du MP3.
- Usage : lecture automatique lorsque le loup-garou de sang est révélé.
- Le bouton « Écouter la fin » conserve `aides_440_fin_a.mp3`.

### Débutant

#### `debutant_101_premiere_nuit_reveil_sang.mp3`

- Type : voix.
- Durée moteur : 6 secondes.
- Texte exact :

> Première nuit. Le loup-garou de sang se réveille.

#### `debutant_102_premiere_nuit_prendre_pierre.mp3`

- Type : voix.
- Durée moteur : 8 secondes.
- Texte exact :

> Le loup-garou de sang prend une pierre violette dans la boîte des loups.

#### `debutant_103_premiere_nuit_choisir_victime.mp3`

- Type : voix.
- Durée moteur : 8 secondes.
- Texte exact :

> Le loup-garou de sang choisit un villageois endormi et part le mordre.

#### `debutant_104_premiere_nuit_victime.mp3`

- Type : voix.
- Durée moteur : 12 secondes.
- Texte exact :

> Le joueur mordu ouvre les yeux pour reconnaître le loup-garou de sang. Il donne sa pierre bleue et reçoit en échange la pierre violette.

#### `debutant_105_premiere_nuit_rangement.mp3`

- Type : voix.
- Durée moteur : 11 secondes.
- Texte exact :

> Le loup-garou de sang place la pierre bleue récupérée dans le sac de guérison, puis retourne vers son habitation pour se rendormir. Le joueur mordu referme les yeux.

#### `debutant_110_identification_conseil.mp3`

- Type : voix.
- Durée moteur : 15 secondes.
- Texte exact :

> Au conseil des loups, chacun montre sa pierre pour se faire identifier. Le loup-garou de sang montre sa pierre rouge. Les loups-garous montrent leur pierre violette. Les goules présentes montrent la pierre de leur couleur.

#### Variantes `debutant_111_option_fin_partie_seuil_1.mp3` à `_seuil_5.mp3`

- Type : voix.
- Durée moteur : 15 secondes.
- Une seule variante est sélectionnée pour toute la partie d'après le seuil
  calculé au lancement.
- Textes exacts :

| Seuil | Texte |
|---:|---|
| 1 | Le seuil de cette partie est d'un villageois. Si le loup-garou de sang estime qu'il reste un villageois ou moins, il peut maintenant mettre fin à la partie en appelant : « Venez à moi, ma meute, mes adorateurs… » Sinon, le conseil continue. |
| 2 | Le seuil de cette partie est de deux villageois. Si le loup-garou de sang estime qu'il reste deux villageois ou moins, il peut maintenant mettre fin à la partie en appelant : « Venez à moi, ma meute, mes adorateurs… » Sinon, le conseil continue. |
| 3 | Le seuil de cette partie est de trois villageois. Si le loup-garou de sang estime qu'il reste trois villageois ou moins, il peut maintenant mettre fin à la partie en appelant : « Venez à moi, ma meute, mes adorateurs… » Sinon, le conseil continue. |
| 4 | Le seuil de cette partie est de quatre villageois. Si le loup-garou de sang estime qu'il reste quatre villageois ou moins, il peut maintenant mettre fin à la partie en appelant : « Venez à moi, ma meute, mes adorateurs… » Sinon, le conseil continue. |
| 5 | Le seuil de cette partie est de cinq villageois. Si le loup-garou de sang estime qu'il reste cinq villageois ou moins, il peut maintenant mettre fin à la partie en appelant : « Venez à moi, ma meute, mes adorateurs… » Sinon, le conseil continue. |

#### `debutant_112_transfert_de_sang.mp3`

- Type : voix.
- Durée moteur : 13 secondes.
- Texte exact :

> S'il le souhaite, le loup-garou de sang échange son rôle et sa pierre rouge avec un loup-garou, qui lui donne sa pierre violette. Le nouveau loup-garou de sang décide de la suite.

#### `debutant_113_designer_mordeur.mp3`

- Type : voix.
- Durée moteur : 10 secondes.
- Texte exact :

> Le loup-garou de sang désigne quel loup-garou va mordre cette nuit. Il peut se désigner lui-même.

#### `debutant_114_choisir_victime.mp3`

- Type : voix.
- Durée moteur : 12 secondes.
- Texte exact :

> Le loup-garou de sang choisit, ou laisse choisir, quel joueur sera mordu cette nuit : un joueur qui dort ou une goule présente au conseil des loups.

#### `debutant_115_prendre_pierre_violette.mp3`

- Type : voix.
- Durée moteur : 10 secondes.
- Texte exact :

> Le loup-garou désigné prend une pierre violette dans la boîte des loups et part mordre le joueur choisi.

#### `debutant_117_echange_pierres.mp3`

- Type : voix.
- Durée moteur : 13 secondes.
- Texte exact :

> Le joueur mordu ouvre les yeux pour reconnaître le loup-garou qui l'a mordu. Il donne sa pierre actuelle, bleue, jaune ou verte, et reçoit en échange la pierre violette. Il devient loup-garou.

#### `debutant_118_rangement_apres_morsure.mp3`

- Type : voix.
- Durée moteur : 12 secondes.
- Texte exact :

> Le loup-garou remet la pierre récupérée dans le sac de guérison, puis part se recoucher. Le joueur mordu referme les yeux.

### Confirmé

#### `premiere_nuit.mp3`

- Type : minuterie sonorisée complète.
- Mode : première nuit en guidage confirmé.
- Durée moteur : 2 minutes 32.
- Boucle : non.
- Répétition manuelle : disponible uniquement pendant les 30 premières secondes.
- Séquences affichées :
  1. 02:32 à 02:02 : `Regagnez vos habitations`.
  2. 02:02 à 01:47 : `Restez calme, préparez-vous à dormir`.
  3. 01:47 à 01:33 : `Fermez les yeux`.
  4. 01:33 à 00:30 : `Le loup-garou de sang se réveille`.
  5. 00:30 à 00:05 : `Le jour va bientôt se lever`.
  6. 00:05 à 00:00 : `Réveillez-vous`.

#### Pistes d'avance de la première nuit confirmée

| Fichier | Temps restant ciblé | Action |
|---|---:|---|
| `premiere_nuit_avance_gong.mp3` | 02:02 | Avance depuis la séquence 1. |
| `premiere_nuit_avance_fermez_yeux.mp3` | 01:47 | Avance depuis la séquence 2. |
| `nuit_avance_fin_nuit.mp3` | 00:30 | Avance depuis les séquences 3 ou 4. |
| `nuit_avance_cocorico.mp3` | 00:05 | Avance depuis la séquence 5. |

#### `confirme_201_premiere_nuit_reveil_sang.mp3`

- Type : voix.
- Durée moteur : 6 secondes.
- État : conservée dans le catalogue, mais l'étape séparée a été retirée de la
  séquence active à partir de la version 0.4.2.
- Texte exact :

> Le loup-garou de sang se réveille.

#### `nuit.mp3`

- Type : minuterie sonorisée complète.
- Mode : nuits confirmées à partir de la deuxième nuit.
- Durée moteur : 4 minutes 18.
- Boucle : non.
- Répétition manuelle : disponible uniquement pendant les 30 premières secondes.
- Séquences affichées :
  1. 04:18 à 03:48 : `Regagnez vos habitations`.
  2. 03:48 à 03:33 : `Restez calme, préparez-vous à dormir`.
  3. 03:33 à 03:13 : `Fermez les yeux`.
  4. 03:13 à 02:50 : `Le loup-garou de sang se réveille et appelle sa meute, ses adorateurs`.
  5. 02:50 à 00:30 : `Conseil des loups`.
  6. 00:30 à 00:05 : `Le jour va bientôt se lever`.
  7. 00:05 à 00:00 : `Réveillez-vous`.

#### Pistes d'avance des nuits confirmées suivantes

| Fichier | Temps restant ciblé | Action |
|---|---:|---|
| `nuit_avance_gong.mp3` | 03:48 | Avance depuis la séquence 1. |
| `nuit_avance_fermez_yeux.mp3` | 03:33 | Avance depuis la séquence 2. |
| `nuit_avance_conseil_loups.mp3` | 02:50 | Avance depuis les séquences 3 ou 4. |
| `nuit_avance_fin_nuit.mp3` | 00:30 | Avance depuis la séquence 5. |
| `nuit_avance_cocorico.mp3` | 00:05 | Avance depuis la séquence 6. |

L'appel est proposé uniquement pendant les séquences 3 à 5. Sa confirmation ne
met pas en pause la minuterie ni la piste en cours.

### Jaune

#### `jaune_302_annonce_prochaine_nuit_jaune.mp3`

- Type : voix.
- Durée moteur configurée dans le ViewModel : 7 secondes.
- Texte exact :

> Cette nuit, les goules jaunes vont se réveiller.

#### `jaune_303_reveil_meute_jaune.mp3`

- Type : voix.
- Durée moteur : 9 secondes.
- Texte exact :

> Les loups-garous et les goules jaunes se réveillent et se rendent au conseil des loups.

### Vert

#### `vert_302_annonce_prochaine_nuit_verte.mp3`

- Type : voix.
- Durée moteur configurée dans le ViewModel : 7 secondes.
- Texte exact :

> Cette nuit, les goules vertes vont se réveiller.

#### `vert_303_reveil_meute_verte.mp3`

- Type : voix.
- Durée moteur : 9 secondes.
- Texte exact :

> Les loups-garous et les goules vertes se réveillent et se rendent au conseil des loups.

### Aides

#### `aides_401_synopsis.mp3`

- Type : voix.
- Durée moteur : 72 secondes.
- Texte exact :

> Nous pensions les loups-garous décimés, mais cette nuit, tous les villageois ont reconnu les hurlements d'une nouvelle bête, née des péchés de ce monde : un Alpha, venu transmettre son mal à l'un d'entre nous. Triste compagnon d'infortune, l'un de nous est maintenant devenu un loup-garou de sang qui va chercher à tous nous transformer pour constituer une nouvelle meute. Mais, villageois ! Restons unis et confiants ! Depuis les tragiques événements de Thiercelieux, nous avons appris à reconnaître l'apparition du mal et à enrayer sa propagation. La guérison est possible. Il faudra juste reconnaître le loup-garou de sang parmi nous… et te tuer, avant que tu ne t'empares de toutes nos âmes.

#### `aides_402_roles_principe.mp3`

- Type : voix.
- Durée moteur : 35 secondes.
- Texte exact :

> Vous cachez en permanence une pierre de couleur sur vous. Elle identifie votre rôle. Votre rôle doit rester secret. Vous ne montrez votre pierre que la nuit, au conseil des loups-garous, la nuit si vous êtes mordu, ou le jour si l'on vous guérit. Votre rôle, et donc votre objectif, peut changer plusieurs fois au cours de la partie. Tout le monde commence villageois avec une pierre bleue, sauf un joueur : le loup-garou de sang, qui commence avec une pierre rouge.

#### `aides_410_deroulement_nuit.mp3`

- Type : voix.
- Durées moteur : 55 secondes dans l'introduction et 60 secondes dans l'aide.
- Texte exact :

> Un tour comprend une nuit et un jour. La nuit, tout le monde part dormir. Le loup-garou de sang, les loups-garous et les goules de la couleur de la nuit se réveillent et rejoignent le conseil des loups. Chacun montre sa pierre. Le loup-garou de sang peut tenter de mettre fin à la partie, transférer son sang à un loup-garou, désigner celui qui va mordre, puis choisir ou laisser choisir la victime. Le loup-garou désigné prend une pierre violette, mord la victime, échange cette pierre contre l'ancienne pierre de la victime, remet la pierre récupérée dans le sac de guérison, puis retourne se coucher. Tous les joueurs encore réveillés se rendorment. Le cocorico annonce le réveil du village.

#### `aides_411_deroulement_jour.mp3`

- Type : voix.
- Durée moteur : 40 secondes.
- Texte exact :

> Le jour, tous les joueurs se réveillent et se concertent. Ils se réunissent ensuite pour le conseil des villageois. Ils votent pour choisir un joueur à guérir, puis procèdent au rituel de guérison. Quand la guérison est terminée, une carte Nuit est tirée pour définir la couleur jaune ou verte de la prochaine nuit.

#### `aides_420_partir_mordre.mp3`

- Type : voix.
- Durée moteur : 35 secondes.
- Texte exact :

> Le loup-garou qui part mordre prend une pierre violette dans la boîte des loups. Il mord le joueur choisi. Il échange la pierre violette contre l'ancienne pierre du joueur, bleue, jaune ou verte. Il repose cette ancienne pierre dans le sac de guérison, puis retourne se coucher.

#### `aides_421_etre_mordu.mp3`

- Type : voix.
- Durée moteur : 28 secondes.
- Texte exact :

> Le joueur mordu ouvre les yeux pour reconnaître le loup-garou qui l'a mordu. Il donne sa pierre actuelle, bleue, jaune ou verte, et reçoit en échange une pierre violette. Il devient loup-garou, puis referme les yeux.

#### `aides_440_fin_a.mp3`

- Type : voix.
- Durée moteur : 70 secondes.
- Texte exact :

> Fin de partie A. Le joueur choisi pour la guérison est le loup-garou de sang. Les villageois tuent la bête. Tous les joueurs encore villageois gagnent. Le loup-garou de sang perd. Les villageois tentent maintenant de guérir tous les loups-garous et toutes les goules. Chaque loup-garou remet sa pierre violette dans la boîte des loups. Chaque goule remet sa pierre jaune ou verte dans le sac de guérison. Chacun tire ensuite une dernière pierre dans le sac. Une pierre bleue signifie qu'il gagne avec les villageois. Une pierre jaune ou verte signifie qu'il perd avec le loup-garou de sang.

#### Variantes `aides_441_appel_de_la_meute_seuil_1.mp3` à `_seuil_5.mp3`

- Type : voix.
- Durée moteur : 45 secondes.
- Textes exacts :

| Seuil | Texte |
|---:|---|
| 1 | Le seuil de cette partie est d'un villageois. Si le loup-garou de sang estime qu'il reste un villageois ou moins, il peut prononcer : « Venez à moi, ma meute, mes adorateurs… » Toutes les goules jaunes et vertes rejoignent alors le conseil, quelle que soit la couleur de la nuit. Comparez le nombre de villageois encore endormis au seuil d'un. |
| 2 | Le seuil de cette partie est de deux villageois. Si le loup-garou de sang estime qu'il reste deux villageois ou moins, il peut prononcer : « Venez à moi, ma meute, mes adorateurs… » Toutes les goules jaunes et vertes rejoignent alors le conseil, quelle que soit la couleur de la nuit. Comparez le nombre de villageois encore endormis au seuil de deux. |
| 3 | Le seuil de cette partie est de trois villageois. Si le loup-garou de sang estime qu'il reste trois villageois ou moins, il peut prononcer : « Venez à moi, ma meute, mes adorateurs… » Toutes les goules jaunes et vertes rejoignent alors le conseil, quelle que soit la couleur de la nuit. Comparez le nombre de villageois encore endormis au seuil de trois. |
| 4 | Le seuil de cette partie est de quatre villageois. Si le loup-garou de sang estime qu'il reste quatre villageois ou moins, il peut prononcer : « Venez à moi, ma meute, mes adorateurs… » Toutes les goules jaunes et vertes rejoignent alors le conseil, quelle que soit la couleur de la nuit. Comparez le nombre de villageois encore endormis au seuil de quatre. |
| 5 | Le seuil de cette partie est de cinq villageois. Si le loup-garou de sang estime qu'il reste cinq villageois ou moins, il peut prononcer : « Venez à moi, ma meute, mes adorateurs… » Toutes les goules jaunes et vertes rejoignent alors le conseil, quelle que soit la couleur de la nuit. Comparez le nombre de villageois encore endormis au seuil de cinq. |

#### Variantes `aides_442_fin_b1_seuil_1.mp3` à `_seuil_5.mp3`

- Type : voix.
- Durée moteur : 55 secondes.
- Textes exacts :

| Seuil | Texte |
|---:|---|
| 1 | Fin de partie B un. Le seuil de cette partie était d'un villageois et il n'est pas dépassé. L'appel du loup-garou de sang était juste. Les loups-garous tuent les derniers villageois. Le loup-garou de sang et les loups-garous gagnent. Chaque goule tire maintenant une carte Nuit au hasard. Si la carte est de sa couleur, elle est transformée en loup-garou et gagne avec la meute. Si la carte n'est pas de sa couleur, elle est tuée et perd. |
| 2 | Fin de partie B un. Le seuil de cette partie était de deux villageois et il n'est pas dépassé. L'appel du loup-garou de sang était juste. Les loups-garous tuent les derniers villageois. Le loup-garou de sang et les loups-garous gagnent. Chaque goule tire maintenant une carte Nuit au hasard. Si la carte est de sa couleur, elle est transformée en loup-garou et gagne avec la meute. Si la carte n'est pas de sa couleur, elle est tuée et perd. |
| 3 | Fin de partie B un. Le seuil de cette partie était de trois villageois et il n'est pas dépassé. L'appel du loup-garou de sang était juste. Les loups-garous tuent les derniers villageois. Le loup-garou de sang et les loups-garous gagnent. Chaque goule tire maintenant une carte Nuit au hasard. Si la carte est de sa couleur, elle est transformée en loup-garou et gagne avec la meute. Si la carte n'est pas de sa couleur, elle est tuée et perd. |
| 4 | Fin de partie B un. Le seuil de cette partie était de quatre villageois et il n'est pas dépassé. L'appel du loup-garou de sang était juste. Les loups-garous tuent les derniers villageois. Le loup-garou de sang et les loups-garous gagnent. Chaque goule tire maintenant une carte Nuit au hasard. Si la carte est de sa couleur, elle est transformée en loup-garou et gagne avec la meute. Si la carte n'est pas de sa couleur, elle est tuée et perd. |
| 5 | Fin de partie B un. Le seuil de cette partie était de cinq villageois et il n'est pas dépassé. L'appel du loup-garou de sang était juste. Les loups-garous tuent les derniers villageois. Le loup-garou de sang et les loups-garous gagnent. Chaque goule tire maintenant une carte Nuit au hasard. Si la carte est de sa couleur, elle est transformée en loup-garou et gagne avec la meute. Si la carte n'est pas de sa couleur, elle est tuée et perd. |

#### Variantes `aides_443_fin_b2_seuil_1.mp3` à `_seuil_5.mp3`

- Type : voix.
- Durée moteur : 60 secondes.
- Textes exacts :

| Seuil | Texte |
|---:|---|
| 1 | Fin de partie B deux. Le seuil de cette partie était d'un villageois et il est dépassé. Le loup-garou de sang s'est trompé et annonce son erreur à voix haute. Les villageois se réveillent, puis tuent le loup-garou de sang et les loups-garous. Les villageois gagnent. Ils tentent maintenant de guérir toutes les goules. Chaque goule remet sa pierre jaune ou verte dans le sac de guérison, puis tire une dernière pierre. Une pierre bleue signifie qu'elle gagne avec les villageois. Une pierre jaune ou verte signifie qu'elle perd avec les loups-garous. |
| 2 | Fin de partie B deux. Le seuil de cette partie était de deux villageois et il est dépassé. Le loup-garou de sang s'est trompé et annonce son erreur à voix haute. Les villageois se réveillent, puis tuent le loup-garou de sang et les loups-garous. Les villageois gagnent. Ils tentent maintenant de guérir toutes les goules. Chaque goule remet sa pierre jaune ou verte dans le sac de guérison, puis tire une dernière pierre. Une pierre bleue signifie qu'elle gagne avec les villageois. Une pierre jaune ou verte signifie qu'elle perd avec les loups-garous. |
| 3 | Fin de partie B deux. Le seuil de cette partie était de trois villageois et il est dépassé. Le loup-garou de sang s'est trompé et annonce son erreur à voix haute. Les villageois se réveillent, puis tuent le loup-garou de sang et les loups-garous. Les villageois gagnent. Ils tentent maintenant de guérir toutes les goules. Chaque goule remet sa pierre jaune ou verte dans le sac de guérison, puis tire une dernière pierre. Une pierre bleue signifie qu'elle gagne avec les villageois. Une pierre jaune ou verte signifie qu'elle perd avec les loups-garous. |
| 4 | Fin de partie B deux. Le seuil de cette partie était de quatre villageois et il est dépassé. Le loup-garou de sang s'est trompé et annonce son erreur à voix haute. Les villageois se réveillent, puis tuent le loup-garou de sang et les loups-garous. Les villageois gagnent. Ils tentent maintenant de guérir toutes les goules. Chaque goule remet sa pierre jaune ou verte dans le sac de guérison, puis tire une dernière pierre. Une pierre bleue signifie qu'elle gagne avec les villageois. Une pierre jaune ou verte signifie qu'elle perd avec les loups-garous. |
| 5 | Fin de partie B deux. Le seuil de cette partie était de cinq villageois et il est dépassé. Le loup-garou de sang s'est trompé et annonce son erreur à voix haute. Les villageois se réveillent, puis tuent le loup-garou de sang et les loups-garous. Les villageois gagnent. Ils tentent maintenant de guérir toutes les goules. Chaque goule remet sa pierre jaune ou verte dans le sac de guérison, puis tire une dernière pierre. Une pierre bleue signifie qu'elle gagne avec les villageois. Une pierre jaune ou verte signifie qu'elle perd avec les loups-garous. |

#### `aides_450_preparation.mp3`

- Type : voix.
- Durée moteur : 90 secondes.
- Texte exact :

> Définissez les lieux où les joueurs pourront dormir et un lieu de réunion avec une table. Définissez comment une victime reconnaîtra qu'elle est mordue, comment se déroulera la guérison, combien de temps durera la concertation du jour et comment aura lieu le vote, y compris en cas d'égalité. Pour le tirage des rôles de départ, préparez une pierre rouge et une pierre bleue pour chaque autre joueur. Chaque joueur tire secrètement une pierre. Préparez ensuite le sac de guérison avec une pierre bleue par joueur autre que le loup-garou de sang, puis autant de pierres jaunes et vertes au total, en équilibrant les deux couleurs autant que possible. Placez dans la boîte des loups une pierre violette par joueur. Mélangez les huit cartes Nuit : quatre jaunes et quatre vertes. Placez sur la table le dispositif sonore, le sac de guérison, la boîte des loups, les cartes Nuit et les aides de jeu. Choisissez enfin le niveau débutant ou confirmé, puis lancez la partie.

#### `aides_451_securite_nuit.mp3`

- Type : voix.
- Durée moteur : 30 secondes.
- Texte exact :

> Pendant la nuit, gardez les yeux fermés tant que vous ne devez pas vous réveiller. Évitez les voix, les rires et les bruits qui permettraient de vous reconnaître. Les bruits inévitables peuvent cependant servir à tromper les villageois.

## Procédure d'intégration

1. Créer `app/src/main/res/raw/` s'il n'existe pas.
2. Exporter chaque enregistrement en MP3 avec le basename exact de cet inventaire.
3. Copier tous les MP3 directement dans `res/raw`, sans créer de dossier
   `commun`, `debutant`, `confirme`, `jaune`, `vert` ou `aides`.
4. Vérifier qu'il existe exactement un fichier par entrée de
   `app/src/main/assets/audio_manifest.json`.
5. Vérifier qu'aucun nom ne contient d'espace, de tiret, d'accent ou de majuscule.
6. Construire l'application avec :

   ```powershell
   .\gradlew.bat :app:assembleDebug
   ```

7. Sur l'appareil, lancer le test d'enceinte puis une partie débutant et une partie
   confirmé. Contrôler les variantes de départ 30 et 45 secondes, une nuit jaune,
   une nuit verte, les boutons pause, reprise, suivant et stop, ainsi que la reprise
   après déconnexion de l'enceinte.
8. Pour toute ressource absente, l'application affiche actuellement son basename
   suivi de `.mp3`. Corriger le fichier dans `res/raw` ; ne pas modifier le Kotlin
   pour contourner un nom incorrect.
