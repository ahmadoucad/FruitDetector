// Fichier : app/src/main/java/fr.mastersd.sime.cheikhahmadoudiop.fruitdetector/view/MainActivity.kt

package fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.databinding.ActivityMainBinding

/**
 * Activité principale unique : hôte du NavHostFragment.
 * Contient tous les fragments via la Navigation (cours chapitre 6).
 *
 * @AndroidEntryPoint obligatoire pour Hilt (cours chapitre 5) :
 *   "les activités doivent être annotées avec @AndroidEntryPoint"
 *
 * Règle MVVM : MainActivity ne contient pas de logique métier.
 * Elle se contente d'héberger les fragments.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    // View Binding comme dans tous les exemples du cours (chapitre 2)
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // La navigation est gérée automatiquement par le NavHostFragment
        // déclaré dans activity_main.xml
    }
}
