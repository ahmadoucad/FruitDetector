

package fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.model

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import javax.inject.Inject


class FruitDetectorImpl @Inject constructor(
    private val context: Context
) : FruitDetector {

    companion object {
        private const val TAG = "FruitDebug"

        private const val CONFIDENCE_THRESHOLD = 0.60f


        private const val MODEL_FILE = "model.tflite"
        private const val LABELS_FILE = "labels.txt"


        private const val INPUT_SIZE = 224


        private const val MAX_RESULTS = 3
    }


    private val interpreter: Interpreter by lazy {
        Log.d(TAG, "Chargement du modele : $MODEL_FILE")
        Interpreter(loadModelFile())
    }


    private val labels: List<String> by lazy {
        Log.d(TAG, "Chargement des labels : $LABELS_FILE")
        loadLabels()
    }


    private fun loadModelFile(): ByteBuffer {
        val assetFileDescriptor = context.assets.openFd(MODEL_FILE)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            startOffset,
            declaredLength
        )
    }


    private fun loadLabels(): List<String> {
        val labelsList = mutableListOf<String>()
        context.assets.open(LABELS_FILE).use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).useLines { lines ->
                lines.forEach { line ->
                    if (line.isNotBlank()) {
                        labelsList.add(line.trim())
                    }
                }
            }
        }
        Log.d(TAG, "Nombre de labels charges : ${labelsList.size}")
        return labelsList
    }


    override suspend fun detect(bitmap: Bitmap): FruitDetectionResult =
        withContext(Dispatchers.Default) {

            Log.d(TAG, "=========================================")
            Log.d(TAG, "detect() appele")
            Log.d(TAG, "Bitmap recu : ${bitmap.width}x${bitmap.height}, config = ${bitmap.config}")

            try {
                // 1. Conversion en ARGB_8888 si nécessaire (format HARDWARE non lisible)
                val safeBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
                    Log.d(TAG, "Conversion HARDWARE -> ARGB_8888")
                    bitmap.copy(Bitmap.Config.ARGB_8888, false)
                } else {
                    bitmap
                }

                // 2. Redimensionnement en 224x224
                val resized = Bitmap.createScaledBitmap(
                    safeBitmap, INPUT_SIZE, INPUT_SIZE, true
                )

                // 3. Conversion du Bitmap en ByteBuffer (entrée du modèle)
                val inputBuffer = convertBitmapToByteBuffer(resized)

                // 4. Préparation du tableau de sortie [1, nombre_de_classes]
                val output = Array(1) { FloatArray(labels.size) }

                // 5. Inférence : le modèle analyse l'image
                interpreter.run(inputBuffer, output)

                val scores = output[0]
                Log.d(TAG, "Inference terminee, ${scores.size} scores obtenus")

                // 6. Construction de la liste (label + score), triée par score décroissant
                val resultats = scores.mapIndexed { index, score ->
                    FruitScore(
                        fruitName = formatFruitName(labels[index]),
                        confidence = score
                    )
                }.sortedByDescending { it.confidence }

                // Affiche le top 5 dans les logs pour diagnostic
                resultats.take(5).forEachIndexed { i, r ->
                    Log.d(TAG, "  #$i : '${r.fruitName}' -> ${"%.1f".format(r.confidence * 100)}%")
                }

                val meilleur = resultats.first()
                val deuxieme = if (resultats.size > 1) resultats[1] else null

                // Écart entre le 1er et le 2e résultat
                val ecart = meilleur.confidence - (deuxieme?.confidence ?: 0f)

                Log.d(
                    TAG,
                    "Meilleur : '${meilleur.fruitName}' a ${"%.1f".format(meilleur.confidence * 100)}% " +
                            "| ecart avec 2e = ${"%.1f".format(ecart * 100)}%"
                )

                // 7a. NOUVEAU : si le modèle a classé l'image comme "Autre"
                //     (= ce n'est pas un fruit/légume), on rejette directement.
                //     C'est la classe spéciale ajoutée lors du ré-entraînement.
                if (meilleur.fruitName.equals("Autre", ignoreCase = true)) {
                    Log.d(TAG, "DECISION : BelowThreshold (classe 'Autre' = pas un fruit)")
                    return@withContext FruitDetectionResult.BelowThreshold
                }

                // 7b. Décision selon le seuil de confiance.
                if (meilleur.confidence < CONFIDENCE_THRESHOLD) {
                    Log.d(TAG, "DECISION : BelowThreshold (score sous le seuil)")
                    return@withContext FruitDetectionResult.BelowThreshold
                }

                Log.d(TAG, "DECISION : Detected -> '${meilleur.fruitName}'")
                Log.d(TAG, "=========================================")

                FruitDetectionResult.Detected(
                    fruitName = meilleur.fruitName,
                    confidence = meilleur.confidence,
                    topResults = resultats.take(MAX_RESULTS)
                )

            } catch (e: Exception) {
                Log.e(TAG, "ERREUR pendant l'analyse : ${e.message}", e)
                FruitDetectionResult.BelowThreshold
            }
        }


    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        // 4 octets par float, 3 canaux (RGB), INPUT_SIZE x INPUT_SIZE pixels
        val byteBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        for (pixel in pixels) {
            // Extraction des composantes R, G, B (valeurs 0-255)
            val r = (pixel shr 16 and 0xFF).toFloat()
            val g = (pixel shr 8 and 0xFF).toFloat()
            val b = (pixel and 0xFF).toFloat()

            // On passe les valeurs brutes : le Rescaling est dans le modèle
            byteBuffer.putFloat(r)
            byteBuffer.putFloat(g)
            byteBuffer.putFloat(b)
        }

        return byteBuffer
    }


    private fun formatFruitName(label: String): String {
        // Retire les chiffres isolés en fin de nom (numéros de variété)
        return label.trim()
            .split(" ")
            .filter { !it.matches(Regex("\\d+")) }  // enlève les "1", "10", etc.
            .joinToString(" ")
            .ifEmpty { label }  // sécurité si tout était des chiffres
    }
}
