package fr.leretourdelabete.domain

import fr.leretourdelabete.model.GameMode
import fr.leretourdelabete.model.NightColor

enum class CueKind {
    VOICE,
    SOUND_EFFECT,
    TIMER,
}

data class GameCue(
    val id: String,
    val title: String,
    val text: String,
    val audioResource: String,
    val fallbackDurationMillis: Long,
    val kind: CueKind = CueKind.VOICE,
    val loopAudio: Boolean = false,
    val skippable: Boolean = true,
    val replayable: Boolean = true,
)

object GameSequenceFactory {
    fun intro(mode: GameMode): List<GameCue> = buildList {
        add(
            voice(
                id = "intro_synopsis",
                title = "Le retour de la Bête",
                audio = "aides_401_synopsis",
                seconds = 72,
                text = "Nous pensions les loups-garous décimés. Mais cette nuit, " +
                    "une nouvelle Bête est venue transmettre son mal à l'un d'entre nous. " +
                    "Villageois, restez unis : reconnaissez le loup-garou de sang avant " +
                    "qu'il ne transforme tout le village.",
            ),
        )
        if (mode == GameMode.BEGINNER) {
            add(
                voice(
                    id = "intro_roles",
                    title = "Les rôles restent secrets",
                    audio = "aides_402_roles_principe",
                    seconds = 35,
                    text = "Votre pierre indique votre rôle et reste cachée. Votre rôle peut " +
                        "changer plusieurs fois pendant la partie.",
                ),
            )
            add(
                voice(
                    id = "intro_turn",
                    title = "Un tour : une nuit et un jour",
                    audio = "aides_410_deroulement_nuit",
                    seconds = 55,
                    text = "La nuit, la meute se réunit et choisit une victime. Le jour, " +
                        "le village débat puis tente une guérison.",
                ),
            )
        }
        if (mode == GameMode.BEGINNER) {
            add(
                voice(
                    id = "intro_ready",
                    title = "La partie peut commencer",
                    audio = "commun_000_partie_prete",
                    seconds = 6,
                    text = "Vérifiez les pierres, le sac de guérison et la boîte des loups. " +
                        "Quand tout le monde est prêt, lancez la première nuit.",
                ),
            )
        }
    }

    fun night(
        round: Int,
        color: NightColor?,
        mode: GameMode,
        departureSeconds: Int,
        packCallVillagerLimit: Int,
    ): List<GameCue> = buildList {
        val departureAudio = if (departureSeconds == 30) {
            "commun_001_nuit_depart_30"
        } else {
            "commun_001_nuit_depart_45"
        }
        val useConfirmedFirstNightAudio =
            round == 1 && mode == GameMode.CONFIRMED
        add(
            timer(
                id = "night_departure_timer",
                title = "Regagnez vos habitations",
                text = "C'est la nuit, regagnez vos habitations.",
                audio = departureAudio,
                seconds = departureSeconds,
            ),
        )

        if (round == 1) {
            if (mode == GameMode.BEGINNER) {
                addAll(firstNightBeginnerCues())
            }
        } else {
            val resolvedColor = requireNotNull(color) {
                "Une nuit après la première doit avoir une couleur."
            }
            add(wakePackCue(resolvedColor))
            if (mode == GameMode.BEGINNER) {
                addAll(laterNightBeginnerCues(packCallVillagerLimit))
            }
        }

        add(
            timer(
                id = "night_council_timer",
                title = if (round == 1) {
                    "Première nuit"
                } else {
                    "Conseil des loups"
                },
                text = if (round == 1) {
                    "Le loup garou de sang se réveille et choisit sa première victime."
                } else {
                    "La meute s'identifie, choisit le mordeur puis sa victime."
                },
                audio = if (useConfirmedFirstNightAudio) {
                    "confirm_premiere_nuit"
                } else {
                    "commun_012_ambiance_nuit_boucle"
                },
                seconds = 115,
                loopAudio = !useConfirmedFirstNightAudio,
                replayable = !useConfirmedFirstNightAudio,
            ),
        )
        add(
            sound(
                id = "night_second_beeps",
                title = "La nuit s'achève",
                text = "Onze bips annoncent la fin du conseil.",
                audio = "commun_002_bips_11",
                seconds = 11,
            ),
        )
        add(
            voice(
                id = "night_sleep_again",
                title = "Rendormez-vous",
                text = "Tous les joueurs encore réveillés partent se rendormir.",
                audio = "commun_004_tous_se_rendorment",
                seconds = 6,
            ),
        )
        add(
            sound(
                id = "night_rooster",
                title = "Le jour se lève",
                text = "Cocorico !",
                audio = "commun_005_cocorico",
                seconds = 5,
            ),
        )
        add(
            voice(
                id = "night_village_wake",
                title = "Réveil du village",
                text = "Le village se réveille.",
                audio = "commun_006_reveil_village",
                seconds = 5,
            ),
        )
    }

    fun helpCues(packCallVillagerLimit: Int): List<GameCue> = listOf(
        voice(
            id = "help_setup",
            title = "Préparer la partie",
            audio = "aides_450_preparation",
            seconds = 90,
            text = "Préparation des lieux, des pierres, du sac de guérison, de la boîte des " +
                "loups et des cartes Nuit.",
        ),
        voice(
            id = "help_roles",
            title = "Principe des rôles",
            audio = "aides_402_roles_principe",
            seconds = 35,
            text = "Les rôles restent secrets et peuvent changer pendant la partie.",
        ),
        voice(
            id = "help_night",
            title = "Déroulement de la nuit",
            audio = "aides_410_deroulement_nuit",
            seconds = 60,
            text = "Réveil de la meute, conseil des loups, morsure et retour au sommeil.",
        ),
        voice(
            id = "help_day",
            title = "Déroulement du jour",
            audio = "aides_411_deroulement_jour",
            seconds = 40,
            text = "Concertation, conseil des villageois, vote et guérison.",
        ),
        voice(
            id = "help_bite",
            title = "Partir mordre",
            audio = "aides_420_partir_mordre",
            seconds = 35,
            text = "Prendre une pierre violette, mordre, échanger les pierres et ranger.",
        ),
        voice(
            id = "help_bit",
            title = "Être mordu",
            audio = "aides_421_etre_mordu",
            seconds = 28,
            text = "Reconnaître le mordeur, échanger sa pierre puis refermer les yeux.",
        ),
        voice(
            id = "help_end",
            title = "Conditions de fin",
            audio = packCallAudio("aides_441_appel_de_la_meute", packCallVillagerLimit),
            seconds = 45,
            text = "Le loup-garou de sang est trouvé le jour, ou appelle sa meute la nuit " +
                "s'il pense qu'il reste $packCallVillagerLimit villageois ou moins.",
        ),
        voice(
            id = "help_night_safety",
            title = "Rester discret la nuit",
            audio = "aides_451_securite_nuit",
            seconds = 30,
            text = "Gardez les yeux fermés et évitez tout bruit permettant de vous identifier.",
        ),
    )

    fun dayCue(stageId: String): GameCue = when (stageId) {
        "discussion" -> voice(
            id = "day_discussion",
            title = "Concertation",
            audio = "commun_007_concertation_jour",
            seconds = 8,
            text = "Tous les joueurs se réveillent et se concertent, en groupe ou séparément.",
        )
        "council" -> voice(
            id = "day_council",
            title = "Conseil des villageois",
            audio = "commun_008_conseil_villageois",
            seconds = 8,
            text = "Tous les joueurs se réunissent pour le conseil des villageois.",
        )
        else -> voice(
            id = "day_healing",
            title = "Rituel de guérison",
            audio = "commun_009_vote_guerison",
            seconds = 14,
            text = "Votez pour choisir un joueur à guérir, puis procédez au rituel.",
        )
    }

    fun endCue(reasonId: String, packCallVillagerLimit: Int): GameCue = when (reasonId) {
        "blood_wolf_found" -> voice(
            id = "end_day",
            title = "La Bête est vaincue",
            audio = "aides_440_fin_a",
            seconds = 70,
            text = "Le loup-garou de sang a été choisi pour la guérison. Les villageois gagnent.",
        )
        "pack_success" -> voice(
            id = "end_pack_success",
            title = "La meute triomphe",
            audio = packCallAudio("aides_442_fin_b1", packCallVillagerLimit),
            seconds = 55,
            text = "Il restait $packCallVillagerLimit villageois ou moins. L'appel était juste : " +
                "le loup-garou de sang et les loups gagnent.",
        )
        else -> voice(
            id = "end_pack_failure",
            title = "L'appel était une erreur",
            audio = packCallAudio("aides_443_fin_b2", packCallVillagerLimit),
            seconds = 60,
            text = "Il restait plus de $packCallVillagerLimit villageois. " +
                "Le village se soulève et gagne.",
        )
    }

    private fun firstNightBeginnerCues(): List<GameCue> = listOf(
        voice(
            "first_take_stone",
            "Prenez une pierre violette",
            "Le loup-garou de sang prend une pierre violette dans la boîte des loups.",
            "debutant_102_premiere_nuit_prendre_pierre",
            8,
        ),
        voice(
            "first_choose_victim",
            "Choisissez une victime",
            "Choisissez un villageois endormi et partez le mordre.",
            "debutant_103_premiere_nuit_choisir_victime",
            8,
        ),
        voice(
            "first_exchange",
            "Échangez les pierres",
            "La victime ouvre les yeux, donne sa pierre bleue et reçoit la pierre violette.",
            "debutant_104_premiere_nuit_victime",
            12,
        ),
        voice(
            "first_store",
            "Rangez la pierre",
            "Placez la pierre bleue dans le sac de guérison, puis rendormez-vous.",
            "debutant_105_premiere_nuit_rangement",
            11,
        ),
    )

    private fun laterNightBeginnerCues(packCallVillagerLimit: Int): List<GameCue> = listOf(
        voice(
            "council_identify",
            "Identifiez-vous",
            "Au conseil des loups, chacun montre sa pierre.",
            "debutant_110_identification_conseil",
            15,
        ),
        voice(
            "council_end_option",
            "Fin de partie possible",
            "Le loup-garou de sang peut appeler sa meute s'il pense qu'il reste " +
                "$packCallVillagerLimit villageois ou moins.",
            packCallAudio(
                "debutant_111_option_fin_partie",
                packCallVillagerLimit,
            ),
            15,
        ),
        voice(
            "council_transfer",
            "Transfert de sang",
            "Le loup-garou de sang peut échanger sa pierre avec un loup-garou.",
            "debutant_112_transfert_de_sang",
            13,
        ),
        voice(
            "council_biter",
            "Désignez le mordeur",
            "Le loup-garou de sang désigne celui qui va mordre.",
            "debutant_113_designer_mordeur",
            10,
        ),
        voice(
            "council_victim",
            "Choisissez la victime",
            "Choisissez un joueur qui dort ou une goule présente au conseil.",
            "debutant_114_choisir_victime",
            12,
        ),
        voice(
            "council_stone",
            "Prenez la pierre violette",
            "Le mordeur prend une pierre violette puis rejoint la victime.",
            "debutant_115_prendre_pierre_violette",
            10,
        ),
        voice(
            "council_exchange",
            "Procédez à la morsure",
            "La victime reconnaît son mordeur et échange sa pierre contre la pierre violette.",
            "debutant_117_echange_pierres",
            13,
        ),
        voice(
            "council_store",
            "Rangez puis rendormez-vous",
            "Remettez la pierre récupérée dans le sac de guérison et retournez vous coucher.",
            "debutant_118_rangement_apres_morsure",
            12,
        ),
    )

    private fun wakePackCue(color: NightColor): GameCue =
        if (color == NightColor.YELLOW) {
            voice(
                id = "wake_pack_yellow",
                title = "Nuit jaune",
                text = "Les loups-garous et les goules jaunes se réveillent et se rendent au conseil des loups.",
                audio = "jaune_303_reveil_meute_jaune",
                seconds = 9,
            )
        } else {
            voice(
                id = "wake_pack_green",
                title = "Nuit verte",
                text = "Les loups-garous et les goules vertes se réveillent et se rendent au conseil des loups.",
                audio = "vert_303_reveil_meute_verte",
                seconds = 9,
            )
        }

    private fun packCallAudio(baseName: String, packCallVillagerLimit: Int): String {
        require(packCallVillagerLimit in 1..5) {
            "Le seuil de l'appel de la meute doit être compris entre 1 et 5."
        }
        return "${baseName}_seuil_$packCallVillagerLimit"
    }

    private fun voice(
        id: String,
        title: String,
        text: String,
        audio: String,
        seconds: Int,
    ) = GameCue(
        id = id,
        title = title,
        text = text,
        audioResource = audio,
        fallbackDurationMillis = seconds * 1_000L,
    )

    private fun sound(
        id: String,
        title: String,
        text: String,
        audio: String,
        seconds: Int,
    ) = GameCue(
        id = id,
        title = title,
        text = text,
        audioResource = audio,
        fallbackDurationMillis = seconds * 1_000L,
        kind = CueKind.SOUND_EFFECT,
    )

    private fun timer(
        id: String,
        title: String,
        text: String,
        audio: String,
        seconds: Int,
        loopAudio: Boolean = true,
        replayable: Boolean = true,
    ) = GameCue(
        id = id,
        title = title,
        text = text,
        audioResource = audio,
        fallbackDurationMillis = seconds * 1_000L,
        kind = CueKind.TIMER,
        loopAudio = loopAudio,
        replayable = replayable,
    )
}
