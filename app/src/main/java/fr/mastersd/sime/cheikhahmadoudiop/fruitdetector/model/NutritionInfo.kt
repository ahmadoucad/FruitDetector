// Fichier : app/src/main/java/fr.mastersd.sime.cheikhahmadoudiop.fruitdetector/model/NutritionInfo.kt

package fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.model

/**
 * Données nutritionnelles d'un fruit ou légume.
 * Remplies par l'API Open Food Facts.
 */
data class NutritionInfo(
    val name: String,
    val calories: Float,        // kcal pour 100g
    val carbohydrates: Float,   // glucides en g
    val fibers: Float,          // fibres en g
    val vitaminC: Float,        // vitamine C en mg
    val proteins: Float,        // protéines en g
    val fats: Float             // lipides en g
)

// =========================================================================
// Modèles de réponse JSON de l'API Open Food Facts
// Utilisés par Gson pour désérialiser la réponse
// =========================================================================

/**
 * Réponse globale de l'API Open Food Facts (recherche par nom)
 */
data class OpenFoodFactsResponse(
    val products: List<OpenFoodProduct>?
)

/**
 * Un produit retourné par l'API
 */
data class OpenFoodProduct(
    val product_name: String?,
    val nutriments: OpenFoodNutriments?
)

/**
 * Valeurs nutritionnelles pour 100g
 */
data class OpenFoodNutriments(
    val energy_100g: Float?,                    // en kJ
    val carbohydrates_100g: Float?,
    val fiber_100g: Float?,
    val vitamin_c_100g: Float?,                 // en mg
    val proteins_100g: Float?,
    val fat_100g: Float?
)
