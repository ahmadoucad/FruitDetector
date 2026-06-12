// Fichier : app/src/main/java/fr.mastersd.sime.cheikhahmadoudiop.fruitdetector/model/FruitDetectionResult.kt

package fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Sealed interface représentant le résultat d'une détection de fruit/légume.
 * Inspiré du DiceResult du cours (chapitre 3 - MVVM).
 *
 * Trois états possibles :
 * - NotDetected : aucune détection en cours
 * - Detected    : un fruit/légume a été identifié avec un score suffisant
 * - BelowThreshold : score de confiance inférieur à 60%, pas de résultat fiable
 */
sealed interface FruitDetectionResult : Parcelable {

    // Aucune détection en cours (état initial)
    @Parcelize
    data object NotDetected : FruitDetectionResult

    // Fruit/légume détecté avec succès (confiance >= 60%)
    @Parcelize
    data class Detected(
        val fruitName: String,      // Nom du fruit détecté (ex: "Pomme")
        val confidence: Float,      // Score de confiance entre 0 et 1 (ex: 0.97f)
        val topResults: List<FruitScore> // Top 3 des résultats pour affichage
    ) : FruitDetectionResult

    // Confiance trop faible (< 60%) : aucun fruit/légume reconnu
    @Parcelize
    data object BelowThreshold : FruitDetectionResult
}

/**
 * Représente un résultat de classification avec son score.
 * Utilisé pour afficher le top 3 des résultats (ex: Pomme 97%, Orange 2%, Citron 1%)
 */
@Parcelize
data class FruitScore(
    val fruitName: String,
    val confidence: Float
) : Parcelable
