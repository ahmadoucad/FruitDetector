// Fichier : app/src/main/java/fr.mastersd.sime.cheikhahmadoudiop.fruitdetector/model/FruitDetector.kt

package fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.model

import android.graphics.Bitmap

/**
 * Interface du backend de détection de fruits et légumes.
 * Séparation interface/implantation comme enseigné dans le cours (chapitre 5 - Hilt).
 *
 * Règle de dépendance (Clean Architecture) :
 * View --> ViewModel --> FruitDetector (interface) <-- FruitDetectorImpl
 *
 * Le ViewModel ne dépend que de cette interface, jamais de l'implantation.
 */
interface FruitDetector {

    /**
     * Analyse un Bitmap et retourne le résultat de la classification.
     * Fonction suspend car l'inférence TFLite peut être lente
     * (Dispatcher.Default comme dans le cours chapitre 8 - Coroutines).
     *
     * @param bitmap Image à analyser
     * @return FruitDetectionResult avec le fruit détecté ou BelowThreshold si confiance < 60%
     */
    suspend fun detect(bitmap: Bitmap): FruitDetectionResult
}
