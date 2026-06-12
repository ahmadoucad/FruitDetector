// Fichier : app/src/main/java/fr/mastersd/sime/cheikhahmadoudiop/fruitdetector/model/FruitDetectorImpl.kt

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

/**
 * Implantation du backend de détection via TensorFlow Lite.
 *
 * Cette version utilise l'Interpreter TFLite classique (et non l'ImageClassifier
 * de la Task Library), ce qui ne nécessite PAS de metadata dans le modèle.
 *
 * - Le modèle (model.tflite) est dans assets/
 * - Les labels (labels.txt) sont dans assets/, un nom par ligne
 *
 * Séparation interface/implantation comme dans le cours (chapitre 5 - Hilt) :
 *   interface FruitDetector <-- FruitDetectorImpl (cette classe)
 *
 * Logs de diagnostic : filtrer "FruitDebug" dans Logcat.
 */
class FruitDetectorImpl @Inject constructor(
    private val context: Context
) : FruitDetector {

    companion object {
        private const val TAG = "FruitDebug"

        // Seuil de confiance minimum : 60% comme défini dans le projet
        private const val CONFIDENCE_THRESHOLD = 0.60f

        // Fichiers dans assets/
        private const val MODEL_FILE = "model.tflite"
        private const val LABELS_FILE = "labels.txt"

        // Taille d'entrée attendue par le modèle (MobileNetV2 = 224x224)
        private const val INPUT_SIZE = 224

        // Nombre de résultats à afficher (top 3)
        private const val MAX_RESULTS = 3
    }

    // Interpreter TFLite, initialisé une seule fois (lazy)
    private val interpreter: Interpreter by lazy {
        Log.d(TAG, "Chargement du modele : $MODEL_FILE")
        Interpreter(loadModelFile())
    }

    // Liste des labels, chargée une seule fois depuis labels.txt
    private val labels: List<String> by lazy {
        Log.d(TAG, "Chargement des labels : $LABELS_FILE")
        loadLabels()
    }

    /**
     * Charge le fichier modèle depuis assets/ en mémoire.
     */
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

    /**
     * Charge les labels depuis assets/labels.txt (un nom par ligne).
     */
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

    /**
     * Analyse un Bitmap avec TFLite et retourne le résultat de détection.
     * Exécutée sur Dispatchers.Default (cours chapitre 8 - Coroutines).
     */
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
                Log.d(
                    TAG,
                    "Meilleur : '${meilleur.fruitName}' a ${"%.1f".format(meilleur.confidence * 100)}% " +
                            "(seuil ${"%.0f".format(CONFIDENCE_THRESHOLD * 100)}%)"
                )

                // 7. Décision selon le seuil de confiance
                if (meilleur.confidence < CONFIDENCE_THRESHOLD) {
                    Log.d(TAG, "DECISION : BelowThreshold")
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

    /**
     * Convertit un Bitmap 224x224 en ByteBuffer pour le modèle.
     *
     * Le modèle attend des float32. La normalisation (Rescaling) est
     * intégrée DANS le modèle, donc on passe les valeurs brutes 0-255.
     */
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

    /**
     * Nettoie le nom du label pour l'affichage.
     * Ex: "Apple Golden 1" -> "Apple Golden", "Tomato 5" -> "Tomato"
     * Retire les numéros de variété en fin de nom.
     */
    private fun formatFruitName(label: String): String {
        // Retire les chiffres isolés en fin de nom (numéros de variété)
        return label.trim()
            .split(" ")
            .filter { !it.matches(Regex("\\d+")) }  // enlève les "1", "10", etc.
            .joinToString(" ")
            .ifEmpty { label }  // sécurité si tout était des chiffres
    }
}
