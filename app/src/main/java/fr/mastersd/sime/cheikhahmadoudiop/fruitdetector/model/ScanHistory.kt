// Fichier : app/src/main/java/fr.mastersd.sime.cheikhahmadoudiop.fruitdetector/model/ScanHistory.kt

package fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entité Room représentant un scan sauvegardé dans la base de données locale.
 * Chaque scan correspond à une détection de fruit ou légume.
 */
@Entity(tableName = "scan_history")
data class ScanHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fruitName: String,          // Nom du fruit/légume détecté (ex: "Pomme")
    val confidence: Float,          // Score de confiance (ex: 0.97f)
    val timestamp: Long,            // Date du scan en millisecondes
    val imageUri: String = ""       // URI de l'image scannée (optionnel)
)
