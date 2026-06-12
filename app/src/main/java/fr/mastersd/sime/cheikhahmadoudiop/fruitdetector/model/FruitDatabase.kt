// Fichier : app/src/main/java/fr.mastersd.sime.cheikhahmadoudiop.fruitdetector/model/FruitDatabase.kt

package fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base de données Room locale de l'application.
 * Contient la table scan_history via l'entité ScanHistory.
 *
 * Singleton pour éviter plusieurs instances ouvertes simultanément.
 */
@Database(
    entities = [ScanHistory::class],
    version = 1,
    exportSchema = false
)
abstract class FruitDatabase : RoomDatabase() {

    /**
     * Accès au DAO pour les opérations sur l'historique des scans.
     */
    abstract fun fruitDao(): FruitDao

    companion object {
        private const val DATABASE_NAME = "fruit_detector_database"

        @Volatile
        private var INSTANCE: FruitDatabase? = null

        /**
         * Retourne l'instance unique de la base de données (pattern Singleton).
         * @Volatile garantit que INSTANCE est toujours à jour entre les threads.
         */
        fun getInstance(context: Context): FruitDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FruitDatabase::class.java,
                    DATABASE_NAME
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
