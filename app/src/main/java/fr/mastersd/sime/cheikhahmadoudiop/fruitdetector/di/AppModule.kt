// Fichier : app/src/main/java/fr.mastersd.sime.cheikhahmadoudiop.fruitdetector/di/AppModule.kt
// VERSION COMPLÈTE avec Retrofit + NutritionRepository

package fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.model.FruitDao
import fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.model.FruitDatabase
import fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.model.FruitDetector
import fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.model.FruitDetectorImpl
import fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.model.NutritionRepository
import fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.model.NutritionRepositoryImpl
import fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.model.OpenFoodFactsApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

/**
 * Module Hilt complet avec toutes les dépendances du projet.
 * Comme dans le cours chapitre 5 (Injection avec Hilt).
 *
 * Règle de dépendance :
 * View --> ViewModel --> Interfaces <-- AppModule fournit les implantations
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // =========================================================================
    // Room : Base de données locale
    // =========================================================================

    @Provides
    @Singleton
    fun provideFruitDatabase(
        @ApplicationContext context: Context
    ): FruitDatabase {
        return FruitDatabase.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideFruitDao(database: FruitDatabase): FruitDao {
        return database.fruitDao()
    }

    // =========================================================================
    // TFLite : Détection de fruits et légumes
    // =========================================================================

    @Provides
    @Singleton
    fun provideFruitDetector(
        @ApplicationContext context: Context
    ): FruitDetector {
        return FruitDetectorImpl(context)
    }

    // =========================================================================
    // Retrofit : API Open Food Facts
    // =========================================================================

    /**
     * Fournit le client OkHttp avec un intercepteur de logs.
     * Utile pour déboguer les appels API pendant le développement.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
    }

    /**
     * Fournit l'instance Retrofit configurée pour Open Food Facts.
     */
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://world.openfoodfacts.org/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    /**
     * Fournit l'interface API Open Food Facts.
     */
    @Provides
    @Singleton
    fun provideOpenFoodFactsApi(retrofit: Retrofit): OpenFoodFactsApi {
        return retrofit.create(OpenFoodFactsApi::class.java)
    }

    /**
     * Fournit l'implantation NutritionRepositoryImpl pour l'interface NutritionRepository.
     * Le ViewModel dépend uniquement de NutritionRepository (interface).
     */
    @Provides
    @Singleton
    fun provideNutritionRepository(
        api: OpenFoodFactsApi
    ): NutritionRepository {
        return NutritionRepositoryImpl(api)
    }
}
