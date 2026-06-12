// Fichier : app/src/main/java/fr/mastersd/sime/cheikhahmadoudiop/fruitdetector/viewmodel/Event.kt

package fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.viewmodel

/**
 * Enveloppe un contenu pour qu'il ne soit consommé qu'UNE SEULE FOIS.
 *
 * Pattern standard Android pour les "événements" (navigation, messages...)
 * exposés via LiveData. Sans ça, une LiveData re-livre sa dernière valeur
 * à chaque réabonnement (ex: retour sur un fragment), ce qui re-déclenche
 * une navigation déjà effectuée.
 *
 * Utilisation :
 *   - Le ViewModel poste un Event(valeur)
 *   - La vue lit getContentIfNotHandled() : retourne la valeur la 1re fois,
 *     puis null lors des observations suivantes.
 */
open class Event<out T>(private val content: T) {

    private var hasBeenHandled = false

    /**
     * Retourne le contenu une seule fois.
     * Les appels suivants retournent null.
     */
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }
}
