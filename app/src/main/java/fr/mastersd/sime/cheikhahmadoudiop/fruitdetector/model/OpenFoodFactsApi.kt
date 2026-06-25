

package fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.model

import retrofit2.http.GET
import retrofit2.http.Query


interface OpenFoodFactsApi {


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
