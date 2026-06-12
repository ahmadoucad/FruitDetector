// Fichier : app/src/main/java/fr.mastersd.sime.cheikhahmadoudiop.fruitdetector/view/HomeFragment.kt

package fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.view


import androidx.fragment.app.activityViewModels
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
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.R
import fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.databinding.FragmentHomeBinding
import fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.viewmodel.FruitViewModel

/**
 * Fragment de l'écran d'accueil.
 *
 * Respecte l'architecture MVVM du cours (chapitre 6 - Fragments) :
 * - onCreateView() : gonfle le layout avec View Binding
 * - onViewCreated() : communication avec le ViewModel et gestion des événements
 *
 * @AndroidEntryPoint pour Hilt (cours chapitre 5)
 * by viewModels() pour récupérer le ViewModel (cours chapitre 6)
 */
@AndroidEntryPoint
class HomeFragment : Fragment() {

    // View Binding : propriété de la classe car utilisée dans onViewCreated()
    // comme dans le cours chapitre 6
    private lateinit var binding: FragmentHomeBinding

    // Injection du ViewModel par conteneur Hilt (cours chapitre 5 et 6)
    private val fruitViewModel: FruitViewModel by activityViewModels()
    // Lanceur pour sélectionner une image depuis la galerie
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                // Conversion de l'URI en Bitmap
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
                // Envoi du Bitmap au ViewModel pour détection
                fruitViewModel.detect(bitmap)
            }
        }
    }

    /**
     * Gonfle le layout du fragment.
     * Comme dans le cours chapitre 6 : onCreateView() retourne la vue racine.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater)
        return binding.root
    }

    /**
     * Communication avec le ViewModel et gestion des clics.
     * Comme dans le cours chapitre 6 : tout se passe dans onViewCreated().
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bouton galerie : ouvre le sélecteur d'image
        binding.galleryButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            }
            galleryLauncher.launch(intent)
        }

        // Bouton caméra : navigation vers CameraFragment
        binding.cameraButton.setOnClickListener {
            findNavController().navigate(
                R.id.action_homeFragment_to_cameraFragment
            )
        }

        // Bouton historique : navigation vers HistoryFragment
        binding.historyButton.setOnClickListener {
            findNavController().navigate(
                R.id.action_homeFragment_to_historyFragment
            )
        }

        // Observation de l'événement de navigation (one-shot)
        // Le ViewModel déclenche la navigation une seule fois quand la
        // détection est terminée. getContentIfNotHandled() garantit qu'on
        // ne navigue pas en boucle au retour sur l'accueil.
        fruitViewModel.navigateToResult.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                findNavController().navigate(
                    R.id.action_homeFragment_to_resultFragment
                )
            }
        }

        // Observation de l'état de chargement
        fruitViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.galleryButton.isEnabled = !isLoading
            binding.cameraButton.isEnabled = !isLoading
        }
    }
}
