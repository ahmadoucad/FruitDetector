// Fichier : app/src/main/java/fr/mastersd/sime/cheikhahmadoudiop/fruitdetector/view/BoxOverlayView.kt

package fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.model.DetectionBox

/**
 * Vue personnalisée qui dessine les boîtes de détection YOLO par-dessus l'image.
 *
 * On la superpose à l'ImageView : l'image en dessous, les cadres au-dessus.
 *
 * Les coordonnées des boîtes sont normalisées (0 à 1), donc on les multiplie
 * par la taille de la vue pour obtenir les pixels à dessiner.
 */
class BoxOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Liste des boîtes à dessiner
    private var boxes: List<DetectionBox> = emptyList()

    // Pinceau pour les rectangles
    private val boxPaint = Paint().apply {
        color = Color.parseColor("#C4B5F0")  // violet (cohérent avec le design)
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }

    // Pinceau pour le fond du texte (étiquette)
    private val textBackgroundPaint = Paint().apply {
        color = Color.parseColor("#C4B5F0")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    // Pinceau pour le texte
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 36f
        isFakeBoldText = true
        isAntiAlias = true
    }

    /**
     * Met à jour les boîtes à dessiner et redessine la vue.
     */
    fun setBoxes(nouvellesBoxes: List<DetectionBox>) {
        boxes = nouvellesBoxes
        invalidate()  // demande à Android de redessiner
    }

    /**
     * Efface toutes les boîtes.
     */
    fun clear() {
        boxes = emptyList()
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val largeur = width.toFloat()
        val hauteur = height.toFloat()

        for (box in boxes) {
            // Conversion des coordonnées normalisées (0-1) en pixels
            val left = box.x1 * largeur
            val top = box.y1 * hauteur
            val right = box.x2 * largeur
            val bottom = box.y2 * hauteur

            // Dessine le rectangle
            canvas.drawRect(RectF(left, top, right, bottom), boxPaint)

            // Prépare l'étiquette (nom + score)
            val texte = "${box.label} ${"%.0f".format(box.score * 100)}%"
            val largeurTexte = textPaint.measureText(texte)
            val hauteurTexte = textPaint.textSize

            // Dessine le fond de l'étiquette
            canvas.drawRect(
                left,
                top - hauteurTexte - 8f,
                left + largeurTexte + 16f,
                top,
                textBackgroundPaint
            )

            // Dessine le texte de l'étiquette
            canvas.drawText(texte, left + 8f, top - 8f, textPaint)
        }
    }
}
