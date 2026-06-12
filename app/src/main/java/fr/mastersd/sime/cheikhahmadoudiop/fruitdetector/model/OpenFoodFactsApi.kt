// Fichier : app/src/main/java/fr.mastersd.sime.cheikhahmadoudiop.fruitdetector/model/OpenFoodFactsApi.kt

package fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.model

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Interface Retrofit pour l'API Open Food Facts.
 *
 * URL de base : https://world.openfoodfacts.org
 *
 * Exemple d'appel :
 * https://world.openfoodfacts.org/cgi/search.pl
 *     ?search_terms=apple
 *     &search_simple=1
 *     &action=process
 *     &json=true
 *     &page_size=1
 *     &fields=product_name,nutriments
 */
interface OpenFoodFactsApi {

    /**
     * Recherche un produit par nom (ex: "apple", "banana", "carrot").
     * Retourne le premier résultat avec ses données nutritionnelles.
     *
     * @param searchTerms nom du fruit/légume en anglais
     * @param pageSize nombre de résultats (1 suffit)
     * @param fields champs à récupérer (optimise la réponse)
     */
    @GET("cgi/search.pl")
    suspend fun searchProduct(
        @Query("search_terms") searchTerms: String,
        @Query("search_simple") searchSimple: Int = 1,
        @Query("action") action: String = "process",
        @Query("json") json: Boolean = true,
        @Query("page_size") pageSize: Int = 1,
        @Query("fields") fields: String = "product_name,nutriments"
    ): OpenFoodFactsResponse
}
