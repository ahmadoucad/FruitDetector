

package fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.view

import androidx.fragment.app.activityViewModels
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.R
import fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.databinding.FragmentCameraBinding
import fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.model.FruitDetectionResult
import fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.viewmodel.FruitViewModel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


@AndroidEntryPoint
class CameraFragment : Fragment() {

    private lateinit var binding: FragmentCameraBinding
    private val fruitViewModel: FruitViewModel by activityViewModels()

    // Executor dédié pour l'analyse des images
    private lateinit var cameraExecutor: ExecutorService

    // Indique si une analyse est déjà en cours
    private var isAnalyzing = false

    // Demande de permission caméra
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
        binding = FragmentCameraBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialisation de l'executor pour CameraX
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Vérification et demande de permission caméra
        if (ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        // Bouton "Voir la fiche complète" → navigation vers ResultFragment
        binding.cameraViewDetailsButton.setOnClickListener {
            findNavController().navigate(
                R.id.action_cameraFragment_to_resultFragment
            )
        }

        // Observation du résultat de détection (cours chapitre 3 - MVVM)
        fruitViewModel.detectionResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is FruitDetectionResult.Detected -> {
                    // Affiche le résultat dans le bandeau
                    binding.cameraNoDetectionText.visibility = View.GONE
                    binding.cameraFruitNameText.visibility = View.VISIBLE
                    binding.cameraConfidenceText.visibility = View.VISIBLE
                    binding.cameraViewDetailsButton.visibility = View.VISIBLE

                    binding.cameraFruitNameText.text = result.fruitName
                    binding.cameraConfidenceText.text = getString(
                        R.string.confidence_text, result.confidence * 100
                    )
                    // Réactive l'analyse après affichage
                    isAnalyzing = false
                }
                is FruitDetectionResult.BelowThreshold -> {
                    binding.cameraFruitNameText.visibility = View.GONE
                    binding.cameraConfidenceText.visibility = View.GONE
                    binding.cameraViewDetailsButton.visibility = View.GONE
                    binding.cameraNoDetectionText.visibility = View.VISIBLE
                    isAnalyzing = false
                }
                is FruitDetectionResult.NotDetected -> {
                    binding.cameraFruitNameText.visibility = View.GONE
                    binding.cameraConfidenceText.visibility = View.GONE
                    binding.cameraViewDetailsButton.visibility = View.GONE
                    binding.cameraNoDetectionText.visibility = View.VISIBLE
                }
            }
        }

        // Observation du chargement
        fruitViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.cameraLoadingProgress.visibility =
                if (isLoading) View.VISIBLE else View.GONE
        }
    }


    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Cas d'utilisation 1 : aperçu caméra dans le PreviewView
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
            }

            // Cas d'utilisation 2 : analyse des images en temps réel
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { analysis ->
                    analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        analyzeImage(imageProxy)
                    }
                }

            // Caméra arrière par défaut
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


    private fun analyzeImage(imageProxy: ImageProxy) {
        if (!isAnalyzing) {
            isAnalyzing = true
            val bitmap = imageProxy.toBitmap()
            fruitViewModel.detect(bitmap)
        }
        imageProxy.close()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
    }
}
