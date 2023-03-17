package eu.marekkon5.album_art_background;

import android.annotation.SuppressLint;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

import java.io.File;
import java.io.FileOutputStream;

public class AlbumArtBackground {
    public SharedPreferences sharedPreferences;
    public WallpaperManager wallpaperManager;
    Context context;

    AlbumArtBackground(Context context) {
        this.sharedPreferences = context.getSharedPreferences("FlutterSharedPreferences", Context.MODE_PRIVATE);
        this.wallpaperManager = WallpaperManager.getInstance(context);
        this.context = context;
    }

    /// Get path to the wallpaper backup
    public File getWallpaperBackupPath() {
        return new File(this.context.getFilesDir(), "wallpaper.png");
    }

    /// Set new wallpaper
    public void setWallpaper(Bitmap bitmap) throws Exception {
        boolean homescreen = this.sharedPreferences.getBoolean("flutter.homescreen", false);
        wallpaperManager.setBitmap(
                bitmap,
                null,
                false,
                homescreen ? (WallpaperManager.FLAG_LOCK | WallpaperManager.FLAG_SYSTEM) : WallpaperManager.FLAG_LOCK);
    }

    /// Restore backed-up wallpaper
    public void restoreBackup() throws Exception {
        Bitmap bitmap = BitmapFactory.decodeFile(this.getWallpaperBackupPath().getAbsolutePath());
        this.setWallpaper(bitmap);
    }

    public void backupWallpaper() throws Exception {
        @SuppressLint("MissingPermission")
        BitmapDrawable drawable = (BitmapDrawable) this.wallpaperManager.getDrawable();
        FileOutputStream fos = new FileOutputStream(this.getWallpaperBackupPath());
        drawable.getBitmap().compress(Bitmap.CompressFormat.PNG, 100, fos);
        fos.close();
    }

    // Blur image
    public Bitmap blur(Bitmap input) {
        Bitmap outputBitmap = Bitmap.createBitmap(input.getWidth(), input.getHeight(), Bitmap.Config.ARGB_8888);
        RenderScript renderScript = RenderScript.create(this.context);
        Allocation inputAllocation = Allocation.createFromBitmap(renderScript, input);
        Allocation outputAllocation = Allocation.createFromBitmap(renderScript, outputBitmap);

        ScriptIntrinsicBlur intrinsicBlur  = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
        intrinsicBlur.setRadius(sharedPreferences.getFloat("flutter.blurStrength", 25f));
        intrinsicBlur.setInput(inputAllocation);
        intrinsicBlur.forEach(outputAllocation);
        outputAllocation.copyTo(outputBitmap);
        renderScript.destroy();

        return outputBitmap;
    }

    public boolean isEnabled() {
        return sharedPreferences.getBoolean("flutter.enable", false);
    }

    public boolean isBlurEnabled() {
        return sharedPreferences.getBoolean("flutter.blur", false);
    }

}
