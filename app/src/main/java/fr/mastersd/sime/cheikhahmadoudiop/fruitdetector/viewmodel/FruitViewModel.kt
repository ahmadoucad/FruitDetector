

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



    private val _detectionResult: MutableLiveData<FruitDetectionResult> =
        state.getLiveData(STATE_KEY_RESULT, FruitDetectionResult.NotDetected)

    val detectionResult: LiveData<FruitDetectionResult> = _detectionResult



    private val _scannedImage: MutableLiveData<Bitmap?> = MutableLiveData(null)
    val scannedImage: LiveData<Bitmap?> = _scannedImage



    private val _isLoading: MutableLiveData<Boolean> =
        state.getLiveData(STATE_KEY_LOADING, false)

    val isLoading: LiveData<Boolean> = _isLoading



    private val _nutritionInfo: MutableLiveData<NutritionInfo?> = MutableLiveData(null)
    val nutritionInfo: LiveData<NutritionInfo?> = _nutritionInfo

    private val _isLoadingNutrition: MutableLiveData<Boolean> = MutableLiveData(false)
    val isLoadingNutrition: LiveData<Boolean> = _isLoadingNutrition



    val scanHistory: LiveData<List<ScanHistory>> = fruitDao.getAllScans()
    val totalScansCount: LiveData<Int> = fruitDao.getTotalScansCount()
    val mostScannedFruit: LiveData<FruitCount?> = fruitDao.getMostScannedFruit()
    val topFruits: LiveData<List<FruitCount>> = fruitDao.getTopFruits()



    private val _errorMessage: MutableLiveData<String?> = MutableLiveData(null)
    val errorMessage: LiveData<String?> = _errorMessage



    private val _navigateToResult: MutableLiveData<Event<Boolean>> = MutableLiveData()
    val navigateToResult: LiveData<Event<Boolean>> = _navigateToResult



    fun detect(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.Default) {
            _isLoading.postValue(true)
            _nutritionInfo.postValue(null)

            _scannedImage.postValue(bitmap)


            val result = fruitDetector.detect(bitmap)


            withContext(Dispatchers.Main) {
                _detectionResult.value = result
                _isLoading.value = false

                _navigateToResult.value = Event(true)
            }


            if (result is FruitDetectionResult.Detected) {
                saveScanToHistory(result)
                fetchNutritionInfo(result.fruitName)
            }
        }
    }


    fun fetchNutritionInfo(fruitName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isLoadingNutrition.postValue(true)

            val info = nutritionRepository.getNutritionInfo(fruitName)
            _nutritionInfo.postValue(info)
            _isLoadingNutrition.postValue(false)
        }
    }


    fun resetDetection() {
        _detectionResult.value = FruitDetectionResult.NotDetected
        _nutritionInfo.value = null
        _errorMessage.value = null
    }


    fun clearError() {
        _errorMessage.value = null
    }


    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            fruitDao.deleteAllScans()
        }
    }




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