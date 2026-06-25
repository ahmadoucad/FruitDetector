

package fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.model

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query


@Dao
interface FruitDao {


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: ScanHistory)


    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    fun getAllScans(): LiveData<List<ScanHistory>>


    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentScans(limit: Int): LiveData<List<ScanHistory>>


    @Query("SELECT COUNT(*) FROM scan_history")
    fun getTotalScansCount(): LiveData<Int>


    @Query("""
        SELECT fruitName, COUNT(*) as count 
        FROM scan_history 
        GROUP BY fruitName 
        ORDER BY count DESC 
        LIMIT 1
    """)
    fun getMostScannedFruit(): LiveData<FruitCount?>


    @Query("""
        SELECT fruitName, COUNT(*) as count 
        FROM scan_history 
        GROUP BY fruitName 
        ORDER BY count DESC 
        LIMIT 5
    """)
    fun getTopFruits(): LiveData<List<FruitCount>>


    @Query("DELETE FROM scan_history")
    suspend fun deleteAllScans()
}


data class FruitCount(
    val fruitName: String,
    val count: Int
)
