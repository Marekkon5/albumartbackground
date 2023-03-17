package eu.marekkon5.album_art_background;

import android.content.Intent;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.NonNull;

import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodChannel;

public class MainActivity extends FlutterActivity {
    public static final String CHANNEL = "eu.marekkon5.album_art_background/native";
    private AlbumArtBackground albumArtBackground;


    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        albumArtBackground = new AlbumArtBackground(this);
        super.configureFlutterEngine(flutterEngine);
        new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), CHANNEL)
                .setMethodCallHandler((call, result) -> {
                    switch (call.method) {

                        // Open the notification listener settings
                        case "notificationAccessSettings":
                            openSettings(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                            break;

                        // Open the battery optimization settings
                        case "batteryOptimizationSettings":
                            openSettings(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                            break;

                        // Restore backed up wallpaper
                        case "restoreBackup":
                            try {
                                albumArtBackground.restoreBackup();
                            } catch (Exception e) {
                                Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
                                e.printStackTrace();
                            }
                            break;

                        default:
                            // Do nothing
                    }
                });
    }

    void openSettings(String setting) {
        Intent intent = new Intent(setting);
        startActivity(intent);
    }

}
