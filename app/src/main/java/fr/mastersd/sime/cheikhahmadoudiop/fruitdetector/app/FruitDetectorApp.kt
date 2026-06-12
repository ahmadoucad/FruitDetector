// Fichier : app/src/main/java/fr.mastersd.sime.cheikhahmadoudiop.fruitdetector/app/FruitDetectorApp.kt

package fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Classe Application annotée avec @HiltAndroidApp.
 * Obligatoire pour initialiser Hilt dans l'application.
 * Comme dans le cours chapitre 5 (Injection avec Hilt) :
 *
 *   @HiltAndroidApp
 *   class DiceApp : Application()
 *
 * À déclarer dans AndroidManifest.xml :
 *   android:name=".app.FruitDetectorApp"
 */
@HiltAndroidApp
class FruitDetectorApp : Application()
