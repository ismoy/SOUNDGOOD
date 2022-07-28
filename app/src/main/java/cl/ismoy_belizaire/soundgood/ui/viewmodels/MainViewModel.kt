package cl.ismoy_belizaire.soundgood.ui.viewmodels

import android.media.MediaMetadata.METADATA_KEY_MEDIA_ID
import android.support.v4.media.MediaBrowserCompat
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cl.ismoy_belizaire.soundgood.data.entities.Song
import cl.ismoy_belizaire.soundgood.exoplayer.MusicServiceConnection
import cl.ismoy_belizaire.soundgood.exoplayer.isPlayEnabled
import cl.ismoy_belizaire.soundgood.exoplayer.isPlaying
import cl.ismoy_belizaire.soundgood.exoplayer.isPrepared
import cl.ismoy_belizaire.soundgood.other.Constants.MEDIA_ROOT_ID
import cl.ismoy_belizaire.soundgood.other.Resource


class MainViewModel @ViewModelInject constructor(
    private val musicServiceConnection: MusicServiceConnection
):ViewModel() {

    private val _mediaItems = MutableLiveData<Resource<List<Song>>>()
    val isConnected = musicServiceConnection.isConnected
    val networkError = musicServiceConnection.networkError
    val curPlayingSong = musicServiceConnection.curPlayingSong
    val playbackState = musicServiceConnection.playbackState

    init {
        _mediaItems.postValue(Resource.loading(null))
        musicServiceConnection.subscribe(MEDIA_ROOT_ID, object :
            MediaBrowserCompat.SubscriptionCallback() {
            override fun onChildrenLoaded(
                parentId: String,
                children: MutableList<MediaBrowserCompat.MediaItem>
            ) {
                super.onChildrenLoaded(parentId, children)
                val item = children.map {
                    Song(
                        it.mediaId!!,
                        it.description.title.toString(),
                        it.description.subtitle.toString(),
                        it.description.mediaUri.toString(),
                        it.description.iconUri.toString()

                    )
                }
                _mediaItems.postValue(Resource.success(item))
            }
        })
    }
    fun skipToNextSong(){
        musicServiceConnection.transportControls.skipToNext()
    }

    fun skipToPreviousSong(){
        musicServiceConnection.transportControls.skipToPrevious()
    }

    fun seekTo(pos:Long){
        musicServiceConnection.transportControls.seekTo(pos)
    }

    fun playOrToggleSong(mediaItem:Song, toggle:Boolean = false){
        val isPrepared = playbackState.value?.isPrepared?: false
        if (isPrepared && mediaItem.mediaId == curPlayingSong.value?.getString(METADATA_KEY_MEDIA_ID)){
            playbackState.value?.let { playbackState->
                when{
                  playbackState.isPlaying -> if (toggle) musicServiceConnection.transportControls.pause()
                    playbackState.isPlayEnabled -> musicServiceConnection.transportControls.play()
                    else->Unit
                }
            }
        }else{
            musicServiceConnection.transportControls.playFromMediaId(mediaItem.mediaId,null)
        }
    }
    override fun onCleared() {
        super.onCleared()
        musicServiceConnection.unsubscribe(MEDIA_ROOT_ID,object  : MediaBrowserCompat.SubscriptionCallback(){})
    }
}