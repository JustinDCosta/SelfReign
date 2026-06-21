package com.aldrenstudios.selfreign.audio

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.aldrenstudios.selfreign.MainActivity
import com.aldrenstudios.selfreign.R
import com.aldrenstudios.selfreign.util.Notifications

/**
 * Plays the selected ambient music track on a loop as a media-style foreground
 * service, so playback continues correctly when the app is minimised. Uses a
 * [MediaSessionCompat] so OS media controls and hardware buttons behave properly.
 *
 * The track is referenced by a res/raw resource *name* passed in the start intent
 * and resolved at runtime; if the asset is absent the service simply stops, so the
 * feature degrades gracefully with no bundled audio.
 */
class AmbientAudioService : Service() {

    private var player: MediaPlayer? = null
    private var mediaSession: MediaSessionCompat? = null

    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSessionCompat(this, "ReclaimAmbient").apply { isActive = true }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PLAY -> {
                val rawName = intent.getStringExtra(EXTRA_RAW_NAME)
                if (rawName.isNullOrBlank() || !startPlayback(rawName)) {
                    stopSelf()
                }
            }
            ACTION_STOP -> stopSelf()
        }
        return START_NOT_STICKY
    }

    /** Resolves the raw asset by name and begins looping playback. Returns false if absent. */
    private fun startPlayback(rawName: String): Boolean {
        @Suppress("DiscouragedApi")
        val resId = resources.getIdentifier(rawName, "raw", packageName)
        if (resId == 0) return false

        player?.release()
        player = MediaPlayer.create(this, resId)?.apply {
            isLooping = true
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            setVolume(0.5f, 0.5f)
            start()
        } ?: return false

        mediaSession?.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1f)
                .setActions(PlaybackStateCompat.ACTION_STOP)
                .build()
        )
        // On Android 10+ declare the foreground service type explicitly.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, buildNotification())
        }
        return true
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, Notifications.AUDIO_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.audio_playing))
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        player?.release()
        player = null
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? = null

    companion object {
        const val ACTION_PLAY = "com.aldrenstudios.selfreign.action.PLAY"
        const val ACTION_STOP = "com.aldrenstudios.selfreign.action.STOP"
        const val EXTRA_RAW_NAME = "raw_name"
        private const val NOTIFICATION_ID = 2002

        /** Starts looping playback of the named res/raw track. */
        fun play(context: Context, rawName: String) {
            val intent = Intent(context, AmbientAudioService::class.java).apply {
                action = ACTION_PLAY
                putExtra(EXTRA_RAW_NAME, rawName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /** Stops playback and dismisses the service. */
        fun stop(context: Context) {
            context.startService(
                Intent(context, AmbientAudioService::class.java).apply { action = ACTION_STOP }
            )
        }
    }
}
