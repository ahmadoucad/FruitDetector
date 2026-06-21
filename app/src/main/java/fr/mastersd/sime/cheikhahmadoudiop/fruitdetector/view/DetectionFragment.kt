// Fichier : app/src/main/java/fr/mastersd/sime/cheikhahmadoudiop/fruitdetector/view/DetectionFragment.kt

package fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.view

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.R
import fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.databinding.FragmentDetectionBinding
import fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.model.YoloDetector
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Fragment de DÉTECTION (partie 2 du projet).
 *
 * Contrairement à la classification (1 fruit), la détection YOLO trouve
 * PLUSIEURS objets dans une image et dessine un cadre autour de chacun.
 *
 * Flux :
 *   1. L'utilisateur choisit une image
 *   2. YoloDetector analyse l'image -> liste d'objets (boîtes)
 *   3. BoxOverlayView dessine les cadres par-dessus l'image
 *
 * @AndroidEntryPoint pour Hilt (cours chapitre 5)
 */
@AndroidEntryPoint
class DetectionFragment : Fragment() {

    private lateinit var binding: FragmentDetectionBinding

    // Le détecteur YOLO, injecté par Hilt
    @Inject
    lateinit var yoloDetector: YoloDetector

    // Lanceur pour sélectionner une image depuis la galerie
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(
                        requireContext().contentResolver, uri
                    )
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(
                        requireContext().contentResolver, uri
                    )
                }
                // Affiche l'image puis lance la détection
                lancerDetection(bitmap)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDetectionBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bouton galerie : ouvre le sélecteur d'image
        binding.detectionGalleryButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            }
            galleryLauncher.launch(intent)
        }

        // Bouton caméra : détection en temps réel
        binding.detectionCameraButton.setOnClickListener {
            findNavController().navigate(
                R.id.action_detectionFragment_to_detectionCameraFragment
            )
        }

        // Bouton retour à l'accueil
        binding.detectionBackButton.setOnClickListener {
            findNavController().navigate(R.id.homeFragment)
        }
    }

    /**
     * Affiche l'image et lance la détection YOLO en arrière-plan.
     */
    private fun lancerDetection(bitmap: Bitmap) {
        // Affiche l'image tout de suite
        binding.detectionImageView.setImageBitmap(bitmap)
        binding.boxOverlay.clear()
        binding.detectionResultText.text = getString(R.string.detection_in_progress)

        // Lance la détection dans une coroutine (cours chapitre 8)
        viewLifecycleOwner.lifecycleScope.launch {
            val boxes = yoloDetector.detect(bitmap)

            // Affiche les cadres
            binding.boxOverlay.setBoxes(boxes)

            // Affiche le nombre d'objets détectés
            binding.detectionResultText.text = if (boxes.isEmpty()) {
                getString(R.string.detection_none)
            } else {
                getString(R.string.detection_count, boxes.size)
            }
        }
    }
}
