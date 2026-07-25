# Visuels générés

Les trois illustrations sont originales et ont été générées pour cette
application. Les fichiers maîtres sont conservés afin de permettre de nouvelles
déclinaisons.

## Fond de nuit

- Sortie originale :
  `C:\Users\Thomas\.codex\generated_images\019f9ad5-97e0-7872-a07c-1747104db993\call_Y59gcb8inyRZ53dyPISRSdVu.png`
- Copie pérenne :
  `artwork/source/village_night_master.png`
- Ressource Android :
  `app/src/main/res/drawable-nodpi/bg_village_night.webp`

Prompt final :

> Use case: stylized-concept
> Asset type: Android game companion full-screen background, landscape
> Primary request: create an original atmospheric background for a dark folk-horror werewolf party game app, showing a small old European village at night beneath a huge pale full moon, framed by twisted forest silhouettes and drifting ground fog
> Style/medium: polished painterly digital illustration, dark gothic storybook, subtle canvas texture, cinematic but readable behind a mobile UI
> Composition/framing: 3:2 landscape, wide scene, village and moon concentrated toward the upper third and outer edges, generous dark low-detail negative space through the center and lower middle for buttons and text, no important subject at the extreme edges
> Lighting/mood: moonlit midnight, mysterious and ominous without gore, deep blue-black shadows, muted ivory moonlight, very restrained dark crimson accents
> Constraints: original imagery, no characters in the foreground, no visible wolf, no text, no symbols, no interface elements, no watermark, no border, avoid excessive fine detail in the central UI-safe area

## Fond de jour

- Sortie originale :
  `C:\Users\Thomas\.codex\generated_images\019f9ad5-97e0-7872-a07c-1747104db993\call_lF12RR826XRK70emt6VmAwAc.png`
- Copie pérenne :
  `artwork/source/village_day_master.png`
- Ressource Android :
  `app/src/main/res/drawable-nodpi/bg_village_day.webp`

Prompt final :

> Use case: stylized-concept
> Asset type: Android game companion full-screen daytime phase background, landscape
> Primary request: create an original atmospheric background for the daytime council phase of a dark folk-horror werewolf party game, showing the same kind of small old European village at cold dawn, with a stone meeting square, distant church tower, mist and bare forest
> Style/medium: polished painterly digital illustration, dark gothic storybook, subtle canvas texture, cinematic but readable behind a mobile UI
> Composition/framing: 3:2 landscape, architecture concentrated in the upper third and outer edges, generous low-detail shadowed negative space in the center and lower middle for large buttons and text, no foreground characters
> Lighting/mood: overcast blue-grey dawn with weak amber sunlight, uneasy calm after the night, no gore
> Color palette: slate blue, charcoal, weathered stone, muted amber and parchment highlights
> Constraints: original imagery, no people, no wolves, no text, no symbols, no interface elements, no watermark, no border, avoid excessive central detail

## Emblème et icône

- Sortie originale :
  `C:\Users\Thomas\.codex\generated_images\019f9ad5-97e0-7872-a07c-1747104db993\call_JHBCTdk5l6k6g7FHJtqtZyWn.png`
- Copie pérenne :
  `artwork/source/wolf_moon_emblem_master.png`
- Emblème Android :
  `app/src/main/res/drawable-nodpi/app_emblem.webp`
- Icônes Android :
  `app/src/main/res/mipmap-*/ic_launcher.png` et
  `app/src/main/res/mipmap-*/ic_launcher_round.png`

Prompt final :

> Use case: logo-brand
> Asset type: square Android launcher icon and splash emblem
> Primary request: create a bold original emblem of a werewolf head in three-quarter silhouette howling upward across a large blood-red full moon, for a dark folk-horror game companion app
> Style/medium: polished painterly emblem with strong simplified shapes, engraved gothic storybook influence, readable at very small icon sizes
> Composition/framing: perfectly centered square composition, wolf head and red moon fill roughly 75 percent of canvas, generous safe padding around all edges, symmetrical visual balance, no border
> Lighting/mood: ominous but elegant, deep charcoal wolf silhouette with subtle cool-blue rim light, textured dark navy background, luminous muted crimson moon
> Constraints: no text, no letters, no gore, no extra animals, no human figures, no watermark, no tiny details, no transparent background, keep the silhouette unmistakable at 48 pixels

Le script `scripts/prepare_android_images.py` transforme les maîtres en WebP et
génère les icônes rectangulaires arrondies et circulaires pour toutes les
densités Android.
