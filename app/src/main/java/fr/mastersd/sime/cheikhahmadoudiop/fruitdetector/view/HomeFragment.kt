// Fichier : app/src/main/java/fr.mastersd.sime.cheikhahmadoudiop.fruitdetector/view/HomeFragment.kt

package fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.view



import androidx.fragment.app.activityViewModels
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
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


@AndroidEntryPoint
class HomeFragment : Fragment() {


    private lateinit var binding: FragmentHomeBinding


    private val fruitViewModel: FruitViewModel by activityViewModels()

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

                fruitViewModel.detect(bitmap)
            }
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        binding.galleryButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            }
            galleryLauncher.launch(intent)
        }


        binding.cameraButton.setOnClickListener {
            findNavController().navigate(
                R.id.action_homeFragment_to_cameraFragment
            )
        }



        binding.detectionButton.setOnClickListener {
            findNavController().navigate(
                R.id.action_homeFragment_to_detectionFragment
            )
        }


        binding.historyButton.setOnClickListener {
            findNavController().navigate(
                R.id.action_homeFragment_to_historyFragment
            )
        }


        binding.logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()

            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }


        fruitViewModel.navigateToResult.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let {
                findNavController().navigate(
                    R.id.action_homeFragment_to_resultFragment
                )
            }
        }


        fruitViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.galleryButton.isEnabled = !isLoading
            binding.cameraButton.isEnabled = !isLoading
        }
    }
}
