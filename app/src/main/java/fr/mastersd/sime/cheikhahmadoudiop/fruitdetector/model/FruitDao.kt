// Fichier : app/src/main/java/fr.mastersd.sime.cheikhahmadoudiop.fruitdetector/model/FruitDao.kt

package fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.model

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * DAO (Data Access Object) Room pour l'historique des scans.
 * Fournit toutes les opérations sur la table scan_history.
 *
 * Les fonctions retournant LiveData sont observées automatiquement
 * par le ViewModel comme dans le cours (chapitre 3 - MVVM).
 */
@Dao
interface FruitDao {

    /**
     * Insère un nouveau scan dans l'historique.
     * Fonction suspend car l'écriture en base est une opération lente
     * (Dispatcher.IO comme dans le cours chapitre 8 - Coroutines).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: ScanHistory)

    /**
     * Retourne tous les scans triés du plus récent au plus ancien.
     * LiveData permet à la vue d'observer les changements automatiquement.
     */
    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    fun getAllScans(): LiveData<List<ScanHistory>>

    /**
     * Retourne les N derniers scans pour les statistiques.
     */
    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentScans(limit: Int): LiveData<List<ScanHistory>>

    /**
     * Retourne le nombre total de scans effectués.
     */
    @Query("SELECT COUNT(*) FROM scan_history")
    fun getTotalScansCount(): LiveData<Int>

    /**
     * Retourne le fruit/légume le plus scanné avec son nombre de scans.
     */
    @Query("""
        SELECT fruitName, COUNT(*) as count 
        FROM scan_history 
        GROUP BY fruitName 
        ORDER BY count DESC 
        LIMIT 1
    """)
    fun getMostScannedFruit(): LiveData<FruitCount?>

    /**
     * Retourne le top 5 des fruits les plus scannés (pour les statistiques).
     */
    @Query("""
        SELECT fruitName, COUNT(*) as count 
        FROM scan_history 
        GROUP BY fruitName 
        ORDER BY count DESC 
        LIMIT 5
    """)
    fun getTopFruits(): LiveData<List<FruitCount>>

    /**
     * Supprime tout l'historique.
     */
    @Query("DELETE FROM scan_history")
    suspend fun deleteAllScans()
}

/**
 * Classe de résultat pour les requêtes d'agrégation (statistiques).
 */
data class FruitCount(
    val fruitName: String,
    val count: Int
)
