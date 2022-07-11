package cl.ismoy_belizaire.soundgood.exoplayer

import cl.ismoy_belizaire.soundgood.exoplayer.State.*

class FirebaseMusicSource {

  private val onReadyListener = mutableListOf<(Boolean)-> Unit>()

    private var state:State = STATE_CREATED
    set(value){
        if (value == STATE_INITIALIZED || value == STATE_ERROR){
            synchronized(onReadyListener){
                field = value
                onReadyListener.forEach { listener->
                    listener(state == STATE_INITIALIZED)
                }
            }
        }else{
            field = value
        }
    }
    fun whenReady(action:(Boolean)->Unit):Boolean{
        if (state ==STATE_CREATED || state == STATE_INITIALIZING){
            onReadyListener += action
            return false
        }else{
            action(state == STATE_INITIALIZED)
            return true
        }
    }
}

enum class State {
    STATE_CREATED,
    STATE_INITIALIZING,
    STATE_INITIALIZED,
    STATE_ERROR
}