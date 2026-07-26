# Règles appliquées par l'application

Ce document précise les évolutions de règle appliquées par l'application par
rapport aux PDF d'origine.

## Appel « Venez à moi, ma meute… »

À partir de la deuxième nuit, le loup-garou de sang peut lancer l'appel s'il
pense que le nombre de villageois restants est inférieur ou égal à :

```text
N / 4, arrondi au plus proche
```

`N` est le nombre initial de joueurs configuré pour la partie. Lorsqu'une
division tombe exactement sur une demi-unité, l'arrondi se fait vers le haut.
L'application effectue ce calcul une seule fois au lancement de la partie,
enregistre le résultat dans la session puis utilise directement ce nombre dans
les écrans et les annonces. Le seuil ne change pas pendant la partie.

| Nombre de joueurs N | Villageois restants maximum |
|---:|---:|
| 4 à 5 | 1 |
| 6 à 9 | 2 |
| 10 à 13 | 3 |
| 14 à 17 | 4 |
| 18 à 20 | 5 |

Après l'appel, toutes les goules rejoignent le conseil. Si le nombre réel de
villageois restants ne dépasse pas le seuil, l'appel est juste et la meute
gagne. S'il dépasse le seuil, l'appel est erroné et les villageois gagnent.
