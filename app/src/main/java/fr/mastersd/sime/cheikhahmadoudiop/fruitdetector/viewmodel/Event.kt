// Fichier : app/src/main/java/fr/mastersd/sime/cheikhahmadoudiop/fruitdetector/viewmodel/Event.kt

package fr.mastersd.sime.cheikhahmadoudiop.fruitdetector.viewmodel


open class Event<out T>(private val content: T) {

    private var hasBeenHandled = false


    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }
}
