package cl.ismoy_belizaire.soundgood.exoplayer


import android.app.PendingIntent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat
import cl.ismoy_belizaire.soundgood.exoplayer.callbacks.MusicPlaybackPreparer
import cl.ismoy_belizaire.soundgood.exoplayer.callbacks.MusicPlayerEventListener
import cl.ismoy_belizaire.soundgood.exoplayer.callbacks.MusicPlayerNotificationListener
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
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
    override fun onCreate() {
        super.onCreate()
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

            }
        val musicplaybackpreparer =MusicPlaybackPreparer(firebaseMusicSource){
            curPlayingSong = it
            preparePlayer(firebaseMusicSource.songs,it,true)
        }
        mediaSessionConnector = MediaSessionConnector(medisSession)
        mediaSessionConnector.setPlaybackPreparer(musicplaybackpreparer)
        mediaSessionConnector.setPlayer(exoPlayer)
        exoPlayer.addListener(MusicPlayerEventListener(this))
        musicNotificationManager.showNotification(exoPlayer)
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
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        TODO("Not yet implemented")
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        TODO("Not yet implemented")
    }
}