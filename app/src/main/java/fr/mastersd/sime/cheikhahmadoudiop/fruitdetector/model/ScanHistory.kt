

package fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.model

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "scan_history")
data class ScanHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fruitName: String,
    val confidence: Float,
    val timestamp: Long,
    val imageUri: String = ""
)
