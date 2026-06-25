// Fichier : app/src/main/java/fr.mastersd.sime.cheikhahmadoudiop.fruitdetector/model/FruitDetectionResult.kt

package fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize


sealed interface FruitDetectionResult : Parcelable {


    @Parcelize
    data object NotDetected : FruitDetectionResult


    @Parcelize
    data class Detected(
        val fruitName: String,
        val confidence: Float,
        val topResults: List<FruitScore>
    ) : FruitDetectionResult


    @Parcelize
    data object BelowThreshold : FruitDetectionResult
}


@Parcelize
data class FruitScore(
    val fruitName: String,
    val confidence: Float
) : Parcelable
