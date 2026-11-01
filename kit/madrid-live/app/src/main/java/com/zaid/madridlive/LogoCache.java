package com.zaid.madridlive;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.File;
import java.io.FileOutputStream;

/** Team crests: downloaded once per team, scaled to at most 160 px, kept as PNG in the app's files dir. */
public final class LogoCache {
    private LogoCache() {}

    public static final int MAX_PX = 160;

    private static File dir(Context c) {
        File d = new File(c.getApplicationContext().getFilesDir(), "crests");
        if (!d.isDirectory()) d.mkdirs();
        return d;
    }

    /** The cache file for a team (the id is reduced to safe characters, so it cannot escape the folder). */
    public static File file(Context c, String teamId) {
        return new File(dir(c), teamId.replaceAll("[^A-Za-z0-9_-]", "_") + ".png");
    }

    /** Largest power-of-two shrink that still leaves the long side at {@code max} px or more. */
    public static int sampleSize(int width, int height, int max) {
        int longSide = Math.max(width, height);
        int s = 1;
        while (max > 0 && longSide / (s * 2) >= max) s *= 2;
        return s;
    }

    /** Make sure the crest is cached; downloads it if not. True if the file exists afterwards. Never throws. */
    public static synchronized boolean ensure(Context c, String teamId, String url) {
        if (teamId == null || teamId.isEmpty()) return false;
        File f = file(c, teamId);
        if (f.isFile() && f.length() > 0) return true;
        File tmp = new File(f.getPath() + ".tmp");
        try {
            byte[] raw = Http.getImage(url == null || url.isEmpty() ? Espn.crestUrl(teamId) : url);
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(raw, 0, raw.length, o);
            if (o.outWidth <= 0 || o.outHeight <= 0) return false;
            o.inJustDecodeBounds = false;
            o.inSampleSize = sampleSize(o.outWidth, o.outHeight, MAX_PX);
            Bitmap full = BitmapFactory.decodeByteArray(raw, 0, raw.length, o);
            if (full == null) return false;
            Bitmap scaled = full;
            int longSide = Math.max(full.getWidth(), full.getHeight());
            if (longSide > MAX_PX) {
                float k = MAX_PX / (float) longSide;
                scaled = Bitmap.createScaledBitmap(full, Math.max(1, Math.round(full.getWidth() * k)),
                        Math.max(1, Math.round(full.getHeight() * k)), true);
            }
            try (FileOutputStream out = new FileOutputStream(tmp)) {
                scaled.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
            if (scaled != full) scaled.recycle();
            full.recycle();
            return tmp.renameTo(f);
        } catch (Throwable t) {
            tmp.delete();
            return false;
        }
    }

    /** The cached crest as a bitmap, or null. */
    public static Bitmap bitmap(Context c, String teamId) {
        try {
            File f = file(c, teamId);
            return f.isFile() ? BitmapFactory.decodeFile(f.getPath()) : null;
        } catch (Throwable t) {
            return null;
        }
    }
}
