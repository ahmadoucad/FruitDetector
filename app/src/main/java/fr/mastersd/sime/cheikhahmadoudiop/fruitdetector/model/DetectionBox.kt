// Fichier : app/src/main/java/fr/mastersd/sime/cheikhahmadoudiop/fruitdetector/model/DetectionBox.kt

package fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.model

/**
 * Représente un objet détecté par YOLO : sa boîte englobante, son nom et son score.
 *
 * Les coordonnées sont NORMALISÉES (entre 0 et 1) par rapport à la taille de
 * l'image, ce qui permet de les dessiner facilement quelle que soit la taille
 * d'affichage.
 *
 * @param x1 coin haut-gauche X (0 à 1)
 * @param y1 coin haut-gauche Y (0 à 1)
 * @param x2 coin bas-droite X (0 à 1)
 * @param y2 coin bas-droite Y (0 à 1)
 * @param label nom de l'objet (ex: "banana")
 * @param score confiance (0 à 1)
 */
data class DetectionBox(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val label: String,
    val score: Float
)
