// Fichier : app/src/main/java/fr/mastersd/sime/cheikhahmadoudiop/fruitdetector/view/LoginActivity.kt

package fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.view

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.databinding.ActivityLoginBinding

/**
 * Écran de connexion (point d'entrée de l'application).
 *
 * Au démarrage, vérifie si un utilisateur est déjà connecté :
 *   - Si oui  -> va directement à MainActivity
 *   - Si non  -> affiche le formulaire de connexion
 *
 * Utilise Firebase Authentication (email + mot de passe).
 */
@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        // Si déjà connecté, on saute la connexion
        if (auth.currentUser != null) {
            navigateToMain()
            return
        }

        // Bouton de connexion
        binding.loginButton.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.loginButton.isEnabled = false
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    binding.loginButton.isEnabled = true
                    if (task.isSuccessful) {
                        navigateToMain()
                    } else {
                        Toast.makeText(
                            this,
                            "Échec de la connexion : ${task.exception?.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        }

        // Lien vers l'inscription
        binding.registerLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
