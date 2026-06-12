// Fichier : app/src/main/java/fr.mastersd.sime.cheikhahmadoudiop.fruitdetector/viewmodel/FruitViewModel.kt
// VERSION COMPLÈTE avec NutritionRepository + API Open Food Facts

package fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.model.FruitCount
import fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.model.FruitDao
import fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.model.FruitDetectionResult
import fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.model.FruitDetector
import fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.model.NutritionInfo
import fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.model.NutritionRepository
import fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.model.ScanHistory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * ViewModel complet de l'application FruitDetector.
 *
 * Respecte l'architecture MVVM du cours (chapitre 3) :
 *   View --> FruitViewModel --> FruitDetector + NutritionRepository (backends)
 *
 * Règles du cours :
 * - Le ViewModel ne possède JAMAIS de référence à la vue
 * - La vue ne fait JAMAIS d'appel direct au modèle
 * - Les LiveDatas sont observées par la vue
 * - Dispatchers.Default pour calcul intensif (TFLite)
 * - Dispatchers.IO pour accès réseau et base de données
 *
 * @HiltViewModel et @Inject : injection automatique par Hilt (chapitre 5)
 * viewModelScope + Coroutines : opérations asynchrones (chapitre 8)
 */
@HiltViewModel
class FruitViewModel @Inject constructor(
    private val state: SavedStateHandle,
    private val fruitDetector: FruitDetector,
    private val fruitDao: FruitDao,
    private val nutritionRepository: NutritionRepository
) : ViewModel() {

    companion object {
        private const val STATE_KEY_RESULT = "detection_result"
        private const val STATE_KEY_LOADING = "is_loading"
    }

    // =========================================================================
    // LiveData : Résultat de détection TFLite
    // =========================================================================

    private val _detectionResult: MutableLiveData<FruitDetectionResult> =
        state.getLiveData(STATE_KEY_RESULT, FruitDetectionResult.NotDetected)

    val detectionResult: LiveData<FruitDetectionResult> = _detectionResult

    // =========================================================================
    // LiveData : Image scannée (pour l'afficher sur l'écran résultat)
    // Note : non sauvegardée dans SavedStateHandle car un Bitmap est trop
    // volumineux ; elle est simplement gardée en mémoire le temps de l'affichage.
    // =========================================================================

    private val _scannedImage: MutableLiveData<Bitmap?> = MutableLiveData(null)
    val scannedImage: LiveData<Bitmap?> = _scannedImage

    // =========================================================================
    // LiveData : État de chargement de la détection TFLite
    // =========================================================================

    private val _isLoading: MutableLiveData<Boolean> =
        state.getLiveData(STATE_KEY_LOADING, false)

    val isLoading: LiveData<Boolean> = _isLoading

    // =========================================================================
    // LiveData : Données nutritionnelles (API Open Food Facts)
    // =========================================================================

    private val _nutritionInfo: MutableLiveData<NutritionInfo?> = MutableLiveData(null)
    val nutritionInfo: LiveData<NutritionInfo?> = _nutritionInfo

    private val _isLoadingNutrition: MutableLiveData<Boolean> = MutableLiveData(false)
    val isLoadingNutrition: LiveData<Boolean> = _isLoadingNutrition

    // =========================================================================
    // LiveData : Historique et statistiques depuis Room
    // =========================================================================

    val scanHistory: LiveData<List<ScanHistory>> = fruitDao.getAllScans()
    val totalScansCount: LiveData<Int> = fruitDao.getTotalScansCount()
    val mostScannedFruit: LiveData<FruitCount?> = fruitDao.getMostScannedFruit()
    val topFruits: LiveData<List<FruitCount>> = fruitDao.getTopFruits()

    // =========================================================================
    // LiveData : Messages d'erreur
    // =========================================================================

    private val _errorMessage: MutableLiveData<String?> = MutableLiveData(null)
    val errorMessage: LiveData<String?> = _errorMessage

    // =========================================================================
    // Événement de navigation one-shot vers l'écran résultat
    // Utilise Event pour ne se déclencher qu'UNE SEULE FOIS par détection,
    // ce qui évite les navigations en boucle au retour sur l'accueil.
    // =========================================================================

    private val _navigateToResult: MutableLiveData<Event<Boolean>> = MutableLiveData()
    val navigateToResult: LiveData<Event<Boolean>> = _navigateToResult

    // =========================================================================
    // Méthodes appelées par la vue
    // =========================================================================

    /**
     * Lance la détection TFLite sur un Bitmap.
     * Puis récupère automatiquement les données nutritionnelles si détection réussie.
     *
     * Dispatchers.Default pour l'inférence TFLite (cours chapitre 8)
     */
    fun detect(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.Default) {
            _isLoading.postValue(true)
            _nutritionInfo.postValue(null)
            // On garde l'image pour l'afficher sur l'écran résultat
            _scannedImage.postValue(bitmap)

            // Inférence TFLite sur Dispatchers.Default
            val result = fruitDetector.detect(bitmap)

            // IMPORTANT : on met à jour le résultat ET on déclenche la navigation
            // sur le thread principal, dans le bon ordre, pour éviter tout
            // problème de timing (ResultFragment doit lire la BONNE valeur).
            withContext(Dispatchers.Main) {
                _detectionResult.value = result
                _isLoading.value = false
                // La navigation part APRÈS que detectionResult soit réellement à jour
                _navigateToResult.value = Event(true)
            }

            // Si détection réussie : sauvegarde + appel API nutrition
            if (result is FruitDetectionResult.Detected) {
                saveScanToHistory(result)
                fetchNutritionInfo(result.fruitName)
            }
        }
    }

    /**
     * Récupère les données nutritionnelles depuis l'API Open Food Facts.
     * Appelé automatiquement après une détection réussie.
     *
     * Dispatchers.IO pour l'appel réseau (cours chapitre 8)
     */
    fun fetchNutritionInfo(fruitName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingNutrition.postValue(true)

            val info = nutritionRepository.getNutritionInfo(fruitName)
            _nutritionInfo.postValue(info)
            _isLoadingNutrition.postValue(false)
        }
    }

    /**
     * Remet tout à l'état initial.
     * Appelé quand l'utilisateur revient à l'accueil.
     */
    fun resetDetection() {
        _detectionResult.value = FruitDetectionResult.NotDetected
        _nutritionInfo.value = null
        _errorMessage.value = null
    }

    /**
     * Efface le message d'erreur après affichage.
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Supprime tout l'historique des scans.
     * Dispatchers.IO car écriture base de données (cours chapitre 8)
     */
    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            fruitDao.deleteAllScans()
        }
    }

    // =========================================================================
    // Méthodes privées
    // =========================================================================

    /**
     * Sauvegarde un scan réussi dans Room.
     * Dispatchers.IO pour l'écriture base de données (cours chapitre 8)
     */
    private suspend fun saveScanToHistory(result: FruitDetectionResult.Detected) {
        val scan = ScanHistory(
            fruitName = result.fruitName,
            confidence = result.confidence,
            timestamp = System.currentTimeMillis()
        )
        withContext(Dispatchers.IO) {
            fruitDao.insertScan(scan)
        }
    }
}