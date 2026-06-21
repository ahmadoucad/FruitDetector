// Fichier : app/src/main/java/fr/mastersd/sime/cheikhahmadoudiop/fruitdetector/view/DetectionCameraFragment.kt

package fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.view

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.R
import fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.databinding.FragmentDetectionCameraBinding
import fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.model.YoloDetector
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject

/**
 * Détection YOLO en TEMPS RÉEL via la caméra.
 *
 * Chaque image du flux caméra est analysée par YOLO, et les cadres des
 * objets détectés sont dessinés en direct par-dessus le flux.
 *
 * Pour rester fluide, on n'analyse pas toutes les images simultanément :
 * une nouvelle analyse n'est lancée que si la précédente est terminée.
 *
 * @AndroidEntryPoint pour Hilt (cours chapitre 5)
 */
@AndroidEntryPoint
class DetectionCameraFragment : Fragment() {

    private lateinit var binding: FragmentDetectionCameraBinding

    @Inject
    lateinit var yoloDetector: YoloDetector

    private lateinit var cameraExecutor: ExecutorService

    // Indique si une analyse est en cours (évite les analyses simultanées)
    private var isAnalyzing = false

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Snackbar.make(
                binding.root,
                getString(R.string.camera_permission_denied),
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDetectionCameraBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()

        // Permission caméra
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Aperçu caméra
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.detectionCameraPreview.surfaceProvider)
            }

            // Analyse en temps réel
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        analyzeImage(imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    viewLifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    /**
     * Analyse une image du flux caméra avec YOLO.
     */
    private fun analyzeImage(imageProxy: ImageProxy) {
        if (isAnalyzing) {
            imageProxy.close()
            return
        }
        isAnalyzing = true

        // Convertit l'image en bitmap et corrige la rotation
        val rotation = imageProxy.imageInfo.rotationDegrees
        val bitmap = imageProxy.toBitmap().pivoter(rotation)
        imageProxy.close()

        // Lance la détection YOLO
        viewLifecycleOwner.lifecycleScope.launch {
            val boxes = yoloDetector.detect(bitmap)

            // Dessine les cadres
            binding.detectionCameraOverlay.setBoxes(boxes)

            // Met à jour le texte
            binding.detectionCameraText.text = if (boxes.isEmpty()) {
                getString(R.string.detection_none)
            } else {
                getString(R.string.detection_count, boxes.size)
            }

            // Prêt pour la prochaine analyse
            isAnalyzing = false
        }
    }

    /**
     * Fait pivoter un Bitmap selon l'angle de rotation de la caméra.
     * Nécessaire car la caméra envoie souvent l'image tournée de 90°.
     */
    private fun Bitmap.pivoter(degres: Int): Bitmap {
        if (degres == 0) return this
        val matrix = Matrix().apply { postRotate(degres.toFloat()) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
    }
}
