package fi.gtrxac.discord;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import java.util.Hashtable;

import fi.gtrxac.bluewap.http.HTTP;

/**
 * Small async image loader for avatars and attachment previews.
 */
public class ImageCache {
    private static final Hashtable cache = new Hashtable();
    private static final int MAX_ENTRIES = 100;
    private static final int MAX_DIMENSION = 512;

    public static Bitmap get(String url) {
        if (url == null) return null;
        return (Bitmap) cache.get(url);
    }

    private static void put(String url, Bitmap bitmap) {
        if (cache.size() >= MAX_ENTRIES) {
            cache.clear();
        }
        cache.put(url, bitmap);
    }

    public static void load(final ImageView view, final String url) {
        if (url == null) {
            view.setTag(null);
            view.setImageDrawable(null);
            return;
        }

        Bitmap cached = get(url);
        if (cached != null) {
            view.setTag(url);
            view.setImageBitmap(cached);
            return;
        }

        view.setTag(url);
        view.setImageDrawable(null);

        new Thread() {
            @Override
            public void run() {
                try {
                    HTTP h = HTTP.createRequest("GET", url);
                    byte[] bytes = h.getResponseBytes();
                    h.close();

                    final Bitmap bitmap = decodeSampled(bytes);
                    if (bitmap != null) {
                        put(url, bitmap);
                    }

                    view.post(new Runnable() {
                        @Override
                        public void run() {
                            if (url.equals(view.getTag())) {
                                view.setImageBitmap(bitmap);
                            }
                        }
                    });
                }
                catch (Exception e) {}
            }
        }.start();
    }

    /**
     * Decode a bitmap, downscaled so that neither dimension exceeds
     * MAX_DIMENSION. Keeps large attachment previews from running old devices
     * out of memory.
     */
    private static Bitmap decodeSampled(byte[] bytes) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(bytes, 0, bytes.length, bounds);

            int sample = 1;
            while (bounds.outWidth / sample > MAX_DIMENSION || bounds.outHeight / sample > MAX_DIMENSION) {
                sample *= 2;
            }

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sample;
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        }
        catch (OutOfMemoryError e) {
            return null;
        }
    }
}
