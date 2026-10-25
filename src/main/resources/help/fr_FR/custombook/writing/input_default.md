# Texte brut par défaut

Contenu initial de la zone avant que le joueur enregistre sa propre valeur. Laissez vide pour une zone initialement vide. Exemple : `Je m’engage à livrer 10 émeraudes.`

Il s’agit d’un véritable contenu initial, pas d’une indication temporaire. Dès qu’une valeur est enregistrée sur l’exemplaire du livre, elle est prioritaire, même si elle est vide. Modifier le texte par défaut ne remplace donc pas les réponses déjà enregistrées.

Le texte est brut : les balises [b] ou [color=...] ne sont pas interprétées. Il respecte la longueur maximale de la zone (jusqu’à 1 024 caractères) et le livre peut enregistrer au total 4 096 caractères dans ses zones de saisie.

En PRESET, ce texte doit correspondre exactement à un choix prédéfini, sinon il est effacé. En FREE et BOTH, le joueur peut le modifier si l’écriture est autorisée et si le livre n’est pas signé.
