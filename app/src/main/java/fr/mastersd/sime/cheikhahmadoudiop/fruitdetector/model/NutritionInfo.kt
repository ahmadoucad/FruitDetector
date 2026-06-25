

package fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.model


data class NutritionInfo(
    val name: String,
    val calories: Float,        // kcal pour 100g
    val carbohydrates: Float,   // glucides en g
    val fibers: Float,          // fibres en g
    val vitaminC: Float,        // vitamine C en mg
    val proteins: Float,        // protéines en g
    val fats: Float             // lipides en g
)


data class OpenFoodFactsResponse(
    val products: List<OpenFoodProduct>?
)


data class OpenFoodProduct(
    val product_name: String?,
    val nutriments: OpenFoodNutriments?
)


data class OpenFoodNutriments(
    val energy_100g: Float?,
    val carbohydrates_100g: Float?,
    val fiber_100g: Float?,
    val vitamin_c_100g: Float?,
    val proteins_100g: Float?,
    val fat_100g: Float?
)
