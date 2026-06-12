// Fichier : app/src/main/java/fr.mastersd.sime.cheikhahmadoudiop.fruitdetector/model/NutritionRepository.kt

package fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.model

import javax.inject.Inject

/**
 * Interface du repository nutritionnel.
 * Séparation interface/implantation comme dans le cours (chapitre 5 - Hilt).
 */
interface NutritionRepository {
    /**
     * Récupère les données nutritionnelles d'un fruit/légume par son nom.
     * @param fruitName nom du fruit détecté (ex: "Pomme", "Apple Granny Smith")
     * @return NutritionInfo ou null si non trouvé
     */
    suspend fun getNutritionInfo(fruitName: String): NutritionInfo?
}

// =========================================================================
// Implantation
// =========================================================================

/**
 * Implantation du NutritionRepository.
 * Interroge l'API Open Food Facts via Retrofit.
 *
 * Règle de dépendance (cours chapitre 5) :
 * ViewModel --> NutritionRepository (interface) <-- NutritionRepositoryImpl
 */
class NutritionRepositoryImpl @Inject constructor(
    private val api: OpenFoodFactsApi
) : NutritionRepository {

    /**
     * Traduit le nom du fruit en anglais pour l'API Open Food Facts
     * et récupère les données nutritionnelles.
     */
    override suspend fun getNutritionInfo(fruitName: String): NutritionInfo? {
        return try {
            // Traduction du nom en anglais pour l'API
            val searchTerm = traduireEnAnglais(fruitName)

            // Appel API (suspend fun, s'exécute sur Dispatchers.IO via Retrofit)
            val response = api.searchProduct(searchTerm)

            // Récupération du premier produit valide
            val product = response.products?.firstOrNull { product ->
                product.nutriments != null
            } ?: return null

            val nutriments = product.nutriments ?: return null

            // Conversion kJ → kcal (1 kcal = 4.184 kJ)
            val caloriesKcal = (nutriments.energy_100g ?: 0f) / 4.184f

            NutritionInfo(
                name = fruitName,
                calories = caloriesKcal,
                carbohydrates = nutriments.carbohydrates_100g ?: 0f,
                fibers = nutriments.fiber_100g ?: 0f,
                vitaminC = (nutriments.vitamin_c_100g ?: 0f) * 1000f, // g → mg
                proteins = nutriments.proteins_100g ?: 0f,
                fats = nutriments.fat_100g ?: 0f
            )
        } catch (e: Exception) {
            // En cas d'erreur réseau → retour null, la vue affichera les données statiques
            null
        }
    }

    /**
     * Extrait le terme de recherche le plus simple pour l'API Open Food Facts.
     *
     * Les noms du dataset Fruits 360 sont trop précis pour l'API
     * (ex: "Banana Lady Finger", "Apple Golden 1"). L'API cherche des
     * produits emballés et ne trouve rien avec ces noms botaniques.
     *
     * On extrait donc le NOM DE BASE (premier mot significatif),
     * ce qui donne de bien meilleurs résultats :
     *   "Banana Lady Finger" -> "banana"
     *   "Apple Golden 1"     -> "apple"
     *   "Onion Red 2"        -> "onion"
     */
    private fun traduireEnAnglais(fruitName: String): String {
        // On garde uniquement le premier mot (le nom de base du fruit/légume),
        // en minuscules, sans les chiffres ni les variétés.
        val premierMot = fruitName
            .lowercase()
            .trim()
            .split(" ")
            .firstOrNull { it.isNotBlank() && !it.matches(Regex("\\d+")) }
            ?: fruitName.lowercase()

        // Traduction français -> anglais au cas où le nom arrive en français
        return when (premierMot) {
            "pomme" -> "apple"
            "banane" -> "banana"
            "fraise" -> "strawberry"
            "raisin" -> "grape"
            "cerise" -> "cherry"
            "poire" -> "pear"
            "pêche" -> "peach"
            "ananas" -> "pineapple"
            "mangue" -> "mango"
            "tomate" -> "tomato"
            "carotte" -> "carrot"
            "courgette" -> "zucchini"
            "concombre" -> "cucumber"
            "poivron" -> "pepper"
            "aubergine" -> "eggplant"
            "oignon" -> "onion"
            else -> premierMot  // déjà en anglais (dataset Fruits 360)
        }
    }
}