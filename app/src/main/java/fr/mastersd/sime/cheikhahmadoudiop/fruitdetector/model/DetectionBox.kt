

package fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.model


data class DetectionBox(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val label: String,
    val score: Float
)
