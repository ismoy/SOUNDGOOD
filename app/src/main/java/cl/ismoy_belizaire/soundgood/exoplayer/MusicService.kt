package cl.ismoy_belizaire.soundgood.exoplayer


import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat
import cl.ismoy_belizaire.soundgood.exoplayer.callbacks.MusicPlaybackPreparer
import cl.ismoy_belizaire.soundgood.exoplayer.callbacks.MusicPlayerEventListener
import cl.ismoy_belizaire.soundgood.exoplayer.callbacks.MusicPlayerNotificationListener
import cl.ismoy_belizaire.soundgood.other.Constants.MEDIA_ROOT_ID
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

private const val SERVICE_TAG ="MusicService"
@AndroidEntryPoint
class MusicService:MediaBrowserServiceCompat() {
    @Inject
    lateinit var dataSourceFactory: DefaultDataSource.Factory

    @Inject
    lateinit var exoPlayer: ExoPlayer
    @Inject
    lateinit var firebaseMusicSource: FirebaseMusicSource
    private val servicejob = Job()
    private val  serviceScope =CoroutineScope(Dispatchers.Main +servicejob)
    private lateinit var medisSession:MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector
    private lateinit var musicNotificationManager: MusicNotificationManager
    var isForegroundService = false
    private var curPlayingSong : MediaMetadataCompat? =null
    private var isPlayerInitialized = false
    private lateinit var musicPlayerEventListener: MusicPlayerEventListener

    companion object{
        var curSongDuration =0L
        private set
    }
    override fun onCreate() {
        super.onCreate()
        serviceScope.launch {
            firebaseMusicSource.fetchMediaData()
        }
        val activityIntent =packageManager?.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this,0,it,0)
        }
        medisSession = MediaSessionCompat(this,SERVICE_TAG).apply {
            setSessionActivity(activityIntent)
            isActive =true
        }
        sessionToken =medisSession.sessionToken
        musicNotificationManager =
            MusicNotificationManager(this,medisSession.sessionToken,MusicPlayerNotificationListener(this)){
            curSongDuration = exoPlayer.duration
            }
        val musicplaybackpreparer = MusicPlaybackPreparer(firebaseMusicSource){
            curPlayingSong = it
            preparePlayer(firebaseMusicSource.songs,it,true)
        }
        mediaSessionConnector = MediaSessionConnector(medisSession)
        mediaSessionConnector.setPlaybackPreparer(musicplaybackpreparer)
        mediaSessionConnector.setQueueNavigator(MusicQueueNavigator())
        mediaSessionConnector.setPlayer(exoPlayer)
        MusicPlayerEventListener(this)
        exoPlayer.addListener(musicPlayerEventListener)
        musicNotificationManager.showNotification(exoPlayer)
    }

    private inner class MusicQueueNavigator:TimelineQueueNavigator(medisSession){
        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
            return firebaseMusicSource.songs[windowIndex].description
        }

    }

    private fun preparePlayer(
        songs:List<MediaMetadataCompat>,
        itemToPlay:MediaMetadataCompat?,
        playNow:Boolean
    ){
     val curSongIndex = if (curPlayingSong == null) 0 else songs.indexOf(itemToPlay)
        exoPlayer.prepare(firebaseMusicSource.asMediaSource(dataSourceFactory))
        exoPlayer.seekTo(curSongIndex,0)
        exoPlayer.playWhenReady = playNow
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        exoPlayer.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        exoPlayer.removeListener(musicPlayerEventListener)
        exoPlayer.release()
    }
    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?): BrowserRoot? {
       return BrowserRoot(MEDIA_ROOT_ID,null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {

        when(parentId){
            MEDIA_ROOT_ID->{
                 val resultsSent = firebaseMusicSource.whenReady {isInitialized->
                    if (isInitialized){
                        result.sendResult(firebaseMusicSource.asMediaItems())
                        if (!isPlayerInitialized && firebaseMusicSource.songs.isNotEmpty()){
                            preparePlayer(firebaseMusicSource.songs,firebaseMusicSource.songs[0],false)
                            isPlayerInitialized =true
                        }

                    }else{
                        result.sendResult(null)
                    }
                 }
                if (!resultsSent){
                    result.detach()
                }
            }
        }
    }
}