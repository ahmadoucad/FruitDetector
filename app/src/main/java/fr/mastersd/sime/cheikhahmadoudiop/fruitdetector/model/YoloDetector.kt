// Fichier : app/src/main/java/fr/mastersd/sime/cheikhahmadoudiop/fruitdetector/model/YoloDetector.kt

package fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.model

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
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
 * Détecteur d'objets basé sur YOLOv8 (TFLite).
 *
 * Contrairement au classificateur (FruitDetector) qui identifie UN seul fruit,
 * YOLO détecte PLUSIEURS objets dans une image et donne leur position (boîtes).
 *
 * Modèle attendu dans assets/ :
 *   - yolo.tflite       : entrée [1,320,320,3], sortie [1,84,2100]
 *   - labels_yolo.txt   : 80 classes COCO
 *
 * Le format de sortie [1, 84, 2100] se décompose ainsi :
 *   - 84 = 4 (boîte: cx, cy, w, h) + 80 (scores des classes)
 *   - 2100 = nombre de boîtes candidates
 *
 * Logs : filtrer "YoloDebug" dans Logcat.
 */
class YoloDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "YoloDebug"

        private const val MODEL_FILE = "yolo_fruit_model.tflite"
        private const val LABELS_FILE = "yolo_fruit_labels.txt"

        // Taille d'entrée du modèle (640x640)
        private const val INPUT_SIZE = 640

        // Dimensions de la sortie [1, 39, 8400]
        private const val NUM_VALUES = 39      // 4 boîte + 35 classes
        private const val NUM_BOXES = 8400     // nombre de boîtes candidates
        private const val NUM_CLASSES = 35

        // Seuils de filtrage
        private const val CONFIDENCE_THRESHOLD = 0.40f  // score minimum pour garder une boîte
        private const val IOU_THRESHOLD = 0.45f         // seuil pour éliminer les doublons (NMS)
    }

    private val interpreter: Interpreter by lazy {
        Log.d(TAG, "Chargement du modele YOLO : $MODEL_FILE")
        Interpreter(loadModelFile())
    }

    private val labels: List<String> by lazy {
        loadLabels()
    }

    private fun loadModelFile(): ByteBuffer {
        val fd = context.assets.openFd(MODEL_FILE)
        val inputStream = FileInputStream(fd.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            fd.startOffset,
            fd.declaredLength
        )
    }

    private fun loadLabels(): List<String> {
        val list = mutableListOf<String>()
        context.assets.open(LABELS_FILE).use { input ->
            BufferedReader(InputStreamReader(input)).useLines { lines ->
                lines.forEach { if (it.isNotBlank()) list.add(it.trim()) }
            }
        }
        Log.d(TAG, "Labels YOLO charges : ${list.size}")
        return list
    }

    /**
     * Détecte les objets dans un Bitmap.
     * Retourne la liste des objets trouvés (boîtes + labels + scores).
     */
    suspend fun detect(bitmap: Bitmap): List<DetectionBox> =
        withContext(Dispatchers.Default) {

            Log.d(TAG, "=========================================")
            Log.d(TAG, "detect() YOLO appele")
            Log.d(TAG, "Bitmap : ${bitmap.width}x${bitmap.height}")

            try {
                // 1. Préparer l'image (resize 320x320 + format)
                val safeBitmap = if (bitmap.config == Bitmap.Config.HARDWARE) {
                    bitmap.copy(Bitmap.Config.ARGB_8888, false)
                } else {
                    bitmap
                }
                val resized = Bitmap.createScaledBitmap(safeBitmap, INPUT_SIZE, INPUT_SIZE, true)
                val inputBuffer = convertBitmapToByteBuffer(resized)

                // 2. Préparer la sortie [1, 84, 2100]
                val output = Array(1) { Array(NUM_VALUES) { FloatArray(NUM_BOXES) } }

                // 3. Inférence
                interpreter.run(inputBuffer, output)
                Log.d(TAG, "Inference YOLO terminee")

                // 4. Décoder les boîtes
                val boxes = decoderSorties(output[0])
                Log.d(TAG, "Boites au-dessus du seuil : ${boxes.size}")

                // 5. NMS : éliminer les boîtes en double
                val boxesFinales = appliquerNMS(boxes)
                Log.d(TAG, "Boites finales apres NMS : ${boxesFinales.size}")

                boxesFinales.forEach {
                    Log.d(TAG, "  -> ${it.label} : ${"%.0f".format(it.score * 100)}%")
                }
                Log.d(TAG, "=========================================")

                boxesFinales

            } catch (e: Exception) {
                Log.e(TAG, "ERREUR YOLO : ${e.message}", e)
                emptyList()
            }
        }

    /**
     * Décode la sortie brute [84, 2100] en liste de boîtes.
     * Pour chaque boîte candidate, on lit ses coordonnées et le meilleur score de classe.
     */
    private fun decoderSorties(output: Array<FloatArray>): List<DetectionBox> {
        val boxes = mutableListOf<DetectionBox>()

        // Pour chaque boîte candidate (colonne)
        for (i in 0 until NUM_BOXES) {
            // Les 4 premières valeurs : coordonnées du centre + taille (normalisées 0-1)
            val cx = output[0][i]   // centre X
            val cy = output[1][i]   // centre Y
            val w = output[2][i]    // largeur
            val h = output[3][i]    // hauteur

            // Cherche la classe avec le meilleur score (parmi les 80)
            var meilleurScore = 0f
            var meilleurClasse = -1
            for (c in 0 until NUM_CLASSES) {
                val score = output[4 + c][i]
                if (score > meilleurScore) {
                    meilleurScore = score
                    meilleurClasse = c
                }
            }

            // On garde seulement si le score dépasse le seuil
            if (meilleurScore >= CONFIDENCE_THRESHOLD && meilleurClasse >= 0) {
                // Conversion centre+taille -> coins (x1,y1,x2,y2)
                val x1 = cx - w / 2f
                val y1 = cy - h / 2f
                val x2 = cx + w / 2f
                val y2 = cy + h / 2f

                boxes.add(
                    DetectionBox(
                        x1 = x1, y1 = y1, x2 = x2, y2 = y2,
                        label = labels.getOrElse(meilleurClasse) { "?" },
                        score = meilleurScore
                    )
                )
            }
        }
        return boxes
    }

    /**
     * NMS (Non-Maximum Suppression) : élimine les boîtes qui se chevauchent trop
     * et qui détectent le même objet, en gardant celle au meilleur score.
     */
    private fun appliquerNMS(boxes: List<DetectionBox>): List<DetectionBox> {
        // Trie par score décroissant
        val triees = boxes.sortedByDescending { it.score }.toMutableList()
        val resultat = mutableListOf<DetectionBox>()

        while (triees.isNotEmpty()) {
            val meilleure = triees.removeAt(0)
            resultat.add(meilleure)

            // Retire les boîtes qui chevauchent trop la meilleure
            triees.removeAll { autre ->
                calculerIoU(meilleure, autre) > IOU_THRESHOLD
            }
        }
        return resultat
    }

    /**
     * Calcule l'IoU (Intersection over Union) entre deux boîtes.
     * Mesure leur taux de chevauchement (0 = pas de chevauchement, 1 = identiques).
     */
    private fun calculerIoU(a: DetectionBox, b: DetectionBox): Float {
        val xLeft = maxOf(a.x1, b.x1)
        val yTop = maxOf(a.y1, b.y1)
        val xRight = minOf(a.x2, b.x2)
        val yBottom = minOf(a.y2, b.y2)

        if (xRight < xLeft || yBottom < yTop) return 0f

        val intersection = (xRight - xLeft) * (yBottom - yTop)
        val aireA = (a.x2 - a.x1) * (a.y2 - a.y1)
        val aireB = (b.x2 - b.x1) * (b.y2 - b.y1)

        return intersection / (aireA + aireB - intersection)
    }

    /**
     * Convertit un Bitmap 320x320 en ByteBuffer normalisé (0-1) pour YOLO.
     * YOLO attend des valeurs entre 0 et 1 (division par 255).
     */
    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4 * INPUT_SIZE * INPUT_SIZE * 3)
        byteBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        for (pixel in pixels) {
            val r = (pixel shr 16 and 0xFF) / 255f
            val g = (pixel shr 8 and 0xFF) / 255f
            val b = (pixel and 0xFF) / 255f
            byteBuffer.putFloat(r)
            byteBuffer.putFloat(g)
            byteBuffer.putFloat(b)
        }
        return byteBuffer
    }
}
