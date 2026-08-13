package fr.leretourdelabete.domain

import fr.leretourdelabete.model.AiVoice

enum class SetupPreviewPermission {
    NONE,
    PLAYER_COUNT,
    PLAYER_COUNT_AND_DRAW_MODE,
}

data class BeginnerSetupGuidanceStep(
    val number: Int,
    val spokenText: String,
    val showCancel: Boolean = false,
    val showPlaybackControls: Boolean = true,
    val previewPermission: SetupPreviewPermission = SetupPreviewPermission.NONE,
    val showPreview: Boolean = previewPermission != SetupPreviewPermission.NONE,
) {
    val canPreview: Boolean
        get() = showPreview

    fun audioResource(voice: AiVoice): String {
        val voiceName = if (voice == AiVoice.MALE) "homme" else "femme"
        return "guidage_${voiceName}_debutant$number"
    }
}

object BeginnerSetupGuidance {
    val steps = listOf(
        BeginnerSetupGuidanceStep(
            number = 1,
            showCancel = true,
            showPlaybackControls = false,
            spokenText = "L'application « Le retour de la Bête » est un conducteur sonore de partie. En niveau de guidage débutant, inutile de connaître les règles, vous serez entièrement guidé par la voix et le texte. Utilisez ce niveau pour vous familiariser au jeu. En niveau de guidage confirmé, rassurez-vous, l'aide textuelle est maintenue à chaque étape du jeu, aucun risque de se tromper. Annuler pour revenir au niveau de guidage confirmé ou pour tester une autre voix.",
        ),
        BeginnerSetupGuidanceStep(
            number = 2,
            previewPermission = SetupPreviewPermission.PLAYER_COUNT_AND_DRAW_MODE,
            spokenText = "« Le retour de la Bête » est un jeu immersif parfaitement adapté pour 8 à 10 joueurs. Vous allez incarner les habitants d'un petit village dont la vie va être bouleversée par l'apparition d'un « loup-garou de sang ». Pour jouer, vous devez définir une pièce principale avec une petite table de réunion sur laquelle vous poserez : le téléphone connecté au système audio, un sac de pioche nommé « sac de guérison » et une petite boite nommée « boîte des loups ». Si vous avez choisi d'utiliser les cartes physiques pour le tirage des nuits, formez aussi la pile des 8 cartes nuits (4 jaunes et 4 vertes) en les mélangeant et en les posant faces cachées sur la table. Une fois la table prête, appuyez sur SUITE.",
        ),
        BeginnerSetupGuidanceStep(
            number = 3,
            spokenText = "Le jeu va se dérouler sur plusieurs tour de jeu. Chaque tour est divisé en deux phases : la phase de nuit et la phase de jour. Pendant la phase de nuit, une voix va vous demander de regagner vos habitations pour vous endormir. Une habitation est simplement un lieu suffisamment éloigné de la table de réunion où il est possible de s'assoir. S'endormir consiste simplement à s'assoir puis à fermer les yeux. Avant de jouer, il faut donc définir autant d'habitations que de joueurs. Si vous jouez en intérieur, vous pouvez utiliser les différentes pièces de la maison et placer si besoin plusieurs chaises dans une même pièce. Dans ce cas, vérifiez que l'accompagnement sonore reste audible dans toutes les pièces en laissant les portes ouvertes par exemple. Si vous jouez en extérieur ou si vous ne disposez que d'un grand salon, disposez les chaises en les éloignant suffisamment de la table de réunion et en les écartant suffisamment les unes des autres. Définir les habitations afin de permettre la discrétion des joueurs qui devront se réveiller pendant la nuit : un joueur, s'il se montre raisonnablement discret, doit pouvoir se lever de manière silencieuse et se diriger vers la table de réunion sans se faire identifier par un autre joueur assis à proximité qui a gardé les yeux fermés. Les joueurs réveillés doivent pouvoir chuchoter autour de la table de réunion sans se faire reconnaître par la voix ; si c'est impossible, ils devront rester muet. Une fois les habitations définies et identifiées par tous les joueurs, appuyez sur SUITE.",
        ),
        BeginnerSetupGuidanceStep(
            number = 4,
            previewPermission = SetupPreviewPermission.PLAYER_COUNT,
            spokenText = "Pendant le jeu, votre rôle est défini par la couleur d'une pierre que vous gardez toujours sur vous, dans votre poche par exemple. Il faut garder votre rôle secret tant que le jeu ne vous demande pas de le révéler. Nous allons maintenant utiliser le sac de guérison pour « tirer les rôles ». Pour préparer le sac, suivez les instructions à l'écran. Pour cela, appuyez sur « VOIR »… Commencer par définir le nombre de joueurs puis, comme indiqué, placez dans le sac autant de pierres que de joueurs : l'une rouge et les autres bleues. Chaque joueur vient tirer secrètement du sac son rôle de départ : pierre bleue, vous êtes villageois, pierre rouge vous êtes le loup-garou de sang. Une fois ce tirage au sort effectué, appuyez sur SUITE.",
        ),
        BeginnerSetupGuidanceStep(
            number = 5,
            showPreview = true,
            spokenText = "Préparons maintenant le sac de guérison et la boîte des loups pour la partie. Suivez les instructions à l'écran, pour cela, appuyez sur « VOIR »… Dans le sac, placez le nombre de pierres bleues, jaunes et vertes demandées. Dans la boîte des loups, placez le nombre de pierres violettes demandées. Une fois la table prête pour la partie, appuyez sur SUITE.",
        ),
        BeginnerSetupGuidanceStep(
            number = 6,
            spokenText = "Avant de lancer la partie, quelques explications sur les rôles mais rassurez-vous, toutes les instructions seront données au fur et à mesure. Votre rôle est défini par la couleur de votre pierre. Votre rôle peut changer plusieurs fois au cours de la partie. La nuit, un villageois va être mordu et devenir loup-garou, sa pierre bleue retournera dans le sac de guérison tandis qu'il recevra en échange une pierre violette de la boîte des loups. Le jour, les villageois vont se réunir pour voter la guérison d'un joueur. Si ce joueur est un loup-garou, sa pierre violette retournera dans la boîte des loups et il piochera une nouvelle pierre dans le sac de guérison : cette pierre sera bleue, jaune ou verte. Si la pierre est bleue, le joueur est guéri et redevient villageois, si la pierre est jaune ou verte, le corps du joueur est guéri mais pas son âme, il devient une goule jaune ou verte… Appuyez sur SUITE pour comprendre les rôles, leurs objectifs et conditions de victoire.",
        ),
        BeginnerSetupGuidanceStep(
            number = 7,
            showPreview = true,
            spokenText = "Pierre rouge, vous êtes le loup-garou de sang. Votre objectif est de constituer une nouvelle meute de loup-garou en transformant suffisamment de villageois. Pour gagner, la nuit, lors du conseil des loups, vous devez lancer un APPEL en hurlant « Venez à moi, ma meute, mes adorateurs ! » mais uniquement si vous estimez qu'il ne reste plus que 1, 2 ou 3 villageois à la pierre bleue parmi les joueurs endormis. Le seuil de villageois à atteindre pour que l'APPEL vous conduise à la victoire dépend du nombre de joueurs. Vous pouvez voir cette information à l'écran sous « Objectifs et conditions de victoire ».",
        ),
        BeginnerSetupGuidanceStep(
            number = 8,
            spokenText = "Pierre bleue, vous êtes villageois, votre objectif est de sauver tout le village en tuant le loup-garou de sang à la pierre rouge puis en guérissant tous les autres. Pour gagner, le jour, lors du conseil des villageois, il faut voter pour la guérison du loup-garou de sang afin qu'il se révèle et qu'il soit abattu.",
        ),
        BeginnerSetupGuidanceStep(
            number = 9,
            spokenText = "Pierre violette, vous êtes un loup-garou. Vous devenez loup-garou quand on vous mord la nuit et jusqu'à une éventuelle guérison le jour. Tant que vous êtes loup-garou, votre objectif est d'aider le loup-garou de sang à gagner pour gagner avec lui. Si vous redevenez villageois, vous retrouvez votre objectif de villageois et vous pouvez utiliser toutes vos connaissances sur l'identité des joueurs lors des concertations le jour.",
        ),
        BeginnerSetupGuidanceStep(
            number = 10,
            spokenText = "Pierre jaune ou verte, vous êtes une goule jaune ou verte. Vous devenez goule à la suite d'une guérison qui s'est mal passée vous concernant. Vous étiez loup-garou et vous êtes devenu goule après avoir pioché votre nouveau rôle dans le sac de guérison. Votre corps est guéri, plus de transformation en loup-garou la nuit, mais votre âme est torturée et vous restez un adorateur de la Bête. Tant que vous êtes goule, votre objectif est d'aider le loup-garou de sang à gagner pour espérer gagner avec lui. Votre objectif final est d'être remordu pour faire partie de la meute. Les goules jaunes ne se réveillent que les nuits jaunes et les goules vertes ne se réveillent que les nuits vertes. Si vous redevenez villageois, vous retrouvez votre objectif de villageois et vous pouvez utiliser toutes vos connaissances sur l'identité des joueurs lors des concertations le jour.",
        ),
        BeginnerSetupGuidanceStep(
            number = 11,
            showPlaybackControls = false,
            spokenText = "Tout est prêt pour lancer une partie. Gardez bien votre rôle secret.",
        ),
    )
}
