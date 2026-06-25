

package fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.R
import fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.databinding.FragmentResultBinding
import fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.model.FruitDetectionResult
import fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.model.NutritionInfo
import fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.viewmodel.FruitViewModel


@AndroidEntryPoint
class ResultFragment : Fragment() {

    private lateinit var binding: FragmentResultBinding


    private val fruitViewModel: FruitViewModel by activityViewModels()


    private var fruitCourant: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentResultBinding.inflate(inflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        fruitViewModel.scannedImage.observe(viewLifecycleOwner) { bitmap ->
            if (bitmap != null) {
                binding.scannedImageView.setImageBitmap(bitmap)
            }
        }


        fruitViewModel.detectionResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is FruitDetectionResult.NotDetected -> {

                }
                is FruitDetectionResult.BelowThreshold -> {
                    afficherEchecDetection()
                }
                is FruitDetectionResult.Detected -> {
                    afficherResultat(result)
                }
            }
        }


        fruitViewModel.nutritionInfo.observe(viewLifecycleOwner) { nutritionInfo ->
            if (nutritionInfo != null) {
                afficherNutritionApi(nutritionInfo)
            }
        }


        fruitViewModel.isLoadingNutrition.observe(viewLifecycleOwner) { isLoading ->
            binding.nutritionLoadingProgress.visibility =
                if (isLoading) View.VISIBLE else View.GONE
        }


        binding.backToHomeButton.setOnClickListener {
            fruitViewModel.resetDetection()
            findNavController().navigate(R.id.homeFragment)
        }


        binding.scanAgainButton.setOnClickListener {
            fruitViewModel.resetDetection()
            findNavController().navigateUp()
        }
    }



    private fun afficherResultat(result: FruitDetectionResult.Detected) {

        fruitCourant = result.fruitName


        binding.detailsContent.visibility = View.VISIBLE
        binding.confidenceText.visibility = View.VISIBLE


        binding.fruitNameText.text = nomFrancaisSimple(result.fruitName)
        binding.fruitNameText.textSize = 28f
        binding.confidenceText.text = getString(
            R.string.confidence_text, result.confidence * 100
        )

        binding.topResult1Text.visibility = View.VISIBLE
        binding.topResult2Text.visibility = View.VISIBLE
        binding.topResult3Text.visibility = View.VISIBLE


        val topResults = result.topResults
        if (topResults.isNotEmpty()) {
            binding.topResult1Text.text =
                "1. ${topResults[0].fruitName} — ${"%.0f".format(topResults[0].confidence * 100)}%"
        }
        if (topResults.size > 1) {
            binding.topResult2Text.text =
                "2. ${topResults[1].fruitName} — ${"%.0f".format(topResults[1].confidence * 100)}%"
        }
        if (topResults.size > 2) {
            binding.topResult3Text.text =
                "3. ${topResults[2].fruitName} — ${"%.0f".format(topResults[2].confidence * 100)}%"
        }

        binding.benefitsText.text = getBienfaits(result.fruitName)

        binding.nutritionContent.visibility = View.VISIBLE
        afficherNutritionStatique(result.fruitName)
    }


    private fun afficherNutritionApi(info: NutritionInfo) {
        binding.nutritionContent.visibility = View.VISIBLE
        binding.nutritionLoadingProgress.visibility = View.GONE


        val (calStat, glucStat, fibStat, vitStat) = valeursCodees(fruitCourant)


        val calories = if (info.calories > 0f) info.calories else calStat
        val glucides = if (info.carbohydrates > 0f) info.carbohydrates else glucStat
        val fibres = if (info.fibers > 0f) info.fibers else fibStat
        val vitamineC = if (info.vitaminC > 0f) info.vitaminC else vitStat

        binding.caloriesText.text = getString(R.string.calories_text, calories)
        binding.carbsText.text = getString(R.string.carbs_text, glucides)
        binding.fibersText.text = getString(R.string.fibers_text, fibres)
        binding.vitaminCText.text = getString(R.string.vitamin_c_text, vitamineC)
    }


    private fun afficherEchecDetection() {

        binding.fruitNameText.text = getString(R.string.below_threshold_message)
        binding.fruitNameText.textSize = 18f


        binding.confidenceText.visibility = View.GONE
        binding.detailsContent.visibility = View.GONE
    }


    private fun valeursCodees(fruitName: String): List<Float> {
        return when {
            fruitName.contains("pomme", ignoreCase = true) ||
            fruitName.contains("apple", ignoreCase = true) ->
                listOf(52f, 14f, 2.4f, 5f)
            fruitName.contains("banane", ignoreCase = true) ||
            fruitName.contains("banana", ignoreCase = true) ->
                listOf(89f, 23f, 2.6f, 9f)
            fruitName.contains("orange", ignoreCase = true) ->
                listOf(47f, 12f, 2.4f, 53f)
            fruitName.contains("fraise", ignoreCase = true) ||
            fruitName.contains("strawberry", ignoreCase = true) ->
                listOf(32f, 8f, 2f, 59f)
            fruitName.contains("tomate", ignoreCase = true) ||
            fruitName.contains("tomato", ignoreCase = true) ->
                listOf(18f, 4f, 1.2f, 14f)
            fruitName.contains("carotte", ignoreCase = true) ||
            fruitName.contains("carrot", ignoreCase = true) ->
                listOf(41f, 10f, 2.8f, 6f)
            fruitName.contains("onion", ignoreCase = true) ||
            fruitName.contains("oignon", ignoreCase = true) ->
                listOf(40f, 9f, 1.7f, 7f)
            else -> listOf(50f, 12f, 2f, 10f)
        }
    }


    private fun afficherNutritionStatique(fruitName: String) {
        val (calories, glucides, fibres, vitamineC) = valeursCodees(fruitName)
        binding.caloriesText.text = getString(R.string.calories_text, calories)
        binding.carbsText.text = getString(R.string.carbs_text, glucides)
        binding.fibersText.text = getString(R.string.fibers_text, fibres)
        binding.vitaminCText.text = getString(R.string.vitamin_c_text, vitamineC)
    }


    private fun nomFrancaisSimple(label: String): String {
        val base = label.lowercase().trim()
            .split(" ")
            .firstOrNull { it.isNotBlank() && !it.matches(Regex("\\d+")) }
            ?: label.lowercase()

        return when (base) {
            "apple" -> "Pomme"
            "banana" -> "Banane"
            "orange" -> "Orange"
            "strawberry" -> "Fraise"
            "grape" -> "Raisin"
            "cherry" -> "Cerise"
            "pear" -> "Poire"
            "peach" -> "Pêche"
            "pineapple" -> "Ananas"
            "mango" -> "Mangue"
            "tomato" -> "Tomate"
            "carrot" -> "Carotte"
            "zucchini" -> "Courgette"
            "cucumber" -> "Concombre"
            "pepper" -> "Poivron"
            "eggplant" -> "Aubergine"
            "onion" -> "Oignon"
            "potato" -> "Pomme de terre"
            "lemon" -> "Citron"
            "limes" -> "Citron vert"
            "avocado" -> "Avocat"
            "kiwi" -> "Kiwi"
            "watermelon" -> "Pastèque"
            "cabbage" -> "Chou"
            "cauliflower" -> "Chou-fleur"
            "beetroot" -> "Betterave"
            "ginger" -> "Gingembre"
            "corn" -> "Maïs"
            "plum" -> "Prune"
            "apricot" -> "Abricot"
            "raspberry" -> "Framboise"
            "blueberry" -> "Myrtille"
            "blackberry" -> "Mûre"
            "pomegranate" -> "Grenade"
            "papaya" -> "Papaye"
            "guava" -> "Goyave"
            "walnut" -> "Noix"
            "hazelnut" -> "Noisette"
            "chestnut" -> "Châtaigne"
            else -> base.replaceFirstChar { it.uppercase() }
        }
    }

    private fun getBienfaits(fruitName: String): String {
        return when {
            fruitName.contains("pomme", ignoreCase = true) ||
            fruitName.contains("apple", ignoreCase = true) ->
                "✅ Bonne pour la digestion\n✅ Riche en Vitamine C\n✅ Faible en calories"
            fruitName.contains("banane", ignoreCase = true) ||
            fruitName.contains("banana", ignoreCase = true) ->
                "✅ Riche en potassium\n✅ Source d'énergie rapide\n✅ Bonne pour les muscles"
            fruitName.contains("orange", ignoreCase = true) ->
                "✅ Très riche en Vitamine C\n✅ Renforce l'immunité\n✅ Bonne hydratation"
            fruitName.contains("fraise", ignoreCase = true) ||
            fruitName.contains("strawberry", ignoreCase = true) ->
                "✅ Riche en antioxydants\n✅ Faible en calories\n✅ Bonne pour la peau"
            fruitName.contains("tomate", ignoreCase = true) ||
            fruitName.contains("tomato", ignoreCase = true) ->
                "✅ Riche en lycopène\n✅ Bonne pour le cœur\n✅ Source de Vitamine A"
            fruitName.contains("carotte", ignoreCase = true) ||
            fruitName.contains("carrot", ignoreCase = true) ->
                "✅ Riche en bêta-carotène\n✅ Bonne pour la vue\n✅ Renforce l'immunité"
            fruitName.contains("onion", ignoreCase = true) ||
            fruitName.contains("oignon", ignoreCase = true) ->
                "✅ Riche en antioxydants\n✅ Bon pour le cœur\n✅ Propriétés anti-inflammatoires"
            else ->
                "✅ Source de vitamines et minéraux\n✅ Recommandé dans une alimentation équilibrée"
        }
    }
}
