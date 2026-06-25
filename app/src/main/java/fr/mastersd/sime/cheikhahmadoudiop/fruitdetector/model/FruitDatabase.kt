

package fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.model

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database(
    entities = [ScanHistory::class],
    version = 1,
    exportSchema = false
)
abstract class FruitDatabase : RoomDatabase() {


    abstract fun fruitDao(): FruitDao

    companion object {
        private const val DATABASE_NAME = "fruit_detector_database"

        @Volatile
        private var INSTANCE: FruitDatabase? = null


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
