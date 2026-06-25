

package fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.model

import javax.inject.Inject


interface NutritionRepository {

    suspend fun getNutritionInfo(fruitName: String): NutritionInfo?
}


class NutritionRepositoryImpl @Inject constructor(
    private val api: OpenFoodFactsApi
) : NutritionRepository {


    override suspend fun getNutritionInfo(fruitName: String): NutritionInfo? {
        return try {

            val searchTerm = traduireEnAnglais(fruitName)


            val response = api.searchProduct(searchTerm)


            val product = response.products?.firstOrNull { product ->
                product.nutriments != null
            } ?: return null

            val nutriments = product.nutriments ?: return null


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

            null
        }
    }


    private fun traduireEnAnglais(fruitName: String): String {

        val premierMot = fruitName
            .lowercase()
            .trim()
            .split(" ")
            .firstOrNull { it.isNotBlank() && !it.matches(Regex("\\d+")) }
            ?: fruitName.lowercase()


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
            else -> premierMot  
        }
    }
}