// Fichier : app/src/main/java/fr.mastersd.sime.cheikhahmadoudiop.fruitdetector/view/MainActivity.kt

package fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.databinding.ActivityMainBinding


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }
}
