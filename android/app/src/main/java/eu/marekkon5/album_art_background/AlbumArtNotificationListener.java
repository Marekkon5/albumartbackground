package eu.marekkon5.album_art_background;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.os.Handler;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.media.session.MediaController;

import java.util.List;

public class AlbumArtNotificationListener extends NotificationListenerService {

    AlbumArtBackground albumArtBackground;
    SharedPreferences preferences;
    Context context;

    @Override
    public IBinder onBind(Intent intent) {
        context = this;
        albumArtBackground = new AlbumArtBackground(context);
        preferences = albumArtBackground.sharedPreferences;

        // Listen to AudioState and check if audio is still playing
        AudioManager audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        audioManager.registerAudioPlaybackCallback(new AudioManager.AudioPlaybackCallback() {
            @Override
            public void onPlaybackConfigChanged(List<AudioPlaybackConfiguration> configs) {
                // Delay because just checking will return wrong state
                Handler handler = new Handler();
                handler.postDelayed(() -> {
                    // Restore wallpaper
                    boolean active = audioManager.isMusicActive();
                    if (!active && preferences.contains("metadataHash")) {
                        try {
                            albumArtBackground.restoreBackup();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        preferences.edit().remove("metadataHash").apply();
                    }
                }, 500);
                super.onPlaybackConfigChanged(configs);
            }
        }, null);

        return super.onBind(intent);
    }

    @Override
    public void onNotificationPosted(StatusBarNotification notification) {
        if (!albumArtBackground.isEnabled()) return;

        // Check if notification has audio session
        if (!notification.getNotification().extras.containsKey(Notification.EXTRA_MEDIA_SESSION))
            return;
        MediaSession.Token token = (MediaSession.Token) notification.getNotification().extras.get(Notification.EXTRA_MEDIA_SESSION);
        MediaController mediaController = new MediaController(this, token);
        MediaMetadata metadata = mediaController.getMetadata();
        if (metadata == null) return;

        // Check if changed
        int hash = metadata.hashCode();
        if (preferences.getInt("metadataHash", 0) == hash) return;

        // Backup
        if (!preferences.contains("metadataHash")) {
            try {
                albumArtBackground.backupWallpaper();
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        }

        // Get art
        Bitmap art = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
        if (art == null) {
            art = metadata.getBitmap(MediaMetadata.METADATA_KEY_ART);
        }
        if (art == null) {
            return;
        }

        // Blur
        if (albumArtBackground.isBlurEnabled()) {
            art = albumArtBackground.blur(art);
        }

        // Set
        try {
            albumArtBackground.setWallpaper(art);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Save hash
        preferences.edit().putInt("metadataHash", hash).apply();
        super.onNotificationPosted(notification);
    }


}
