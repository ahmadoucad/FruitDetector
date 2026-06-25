

package fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.model

import android.graphics.Bitmap


interface FruitDetector {


    suspend fun detect(bitmap: Bitmap): FruitDetectionResult
}
