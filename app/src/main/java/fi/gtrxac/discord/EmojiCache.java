package fi.gtrxac.discord;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.TextView;

import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.Hashtable;
import java.util.Vector;

import fi.gtrxac.bluewap.http.HTTP;

public class EmojiCache {
    private static final String PATH = "twemoji/";
    private static final String VARIATION_SELECTOR = "-fe0f";
    private static final int MAX_ENTRIES = 64;

    private static final Hashtable<String, Bitmap> cache = new Hashtable<String, Bitmap>(); // code points or url -> Bitmap
    private static final Hashtable<String, Vector<WeakReference<TextView>>> pending = new Hashtable<String, Vector<WeakReference<TextView>>>(); // url -> views to redraw once it finishes downloading

    /**
     * @return the bundled emoji image, or null when there is none, in which
     *         case the caller should fall back to the system font
     */
    public static Bitmap get(Context context, String codepoints) {
        if (context == null || codepoints == null || codepoints.length() == 0) return null;
        Bitmap cached = cache.get(codepoints);
        if (cached != null) return cached;

        Bitmap bitmap = decode(context, PATH + codepoints + ".png");
        if (bitmap == null && codepoints.indexOf(VARIATION_SELECTOR) != -1) {
            // Twemoji drops the variation selectors from most file names
            bitmap = decode(context, PATH + codepoints.replace(VARIATION_SELECTOR, "") + ".png");
        }
        if (bitmap != null) put(codepoints, bitmap);
        return bitmap;
    }

    /** @return the downloaded custom emoji image, or null when it isn't cached yet */
    public static Bitmap getCustom(String url) {
        if (url == null) return null;
        return cache.get(url);
    }

    /**
     * Downloads a custom emoji image unless it is cached or already
     * downloading.
     */
    public static void loadCustom(Context context, final String url, TextView view) {
        if (context == null || url == null) return;
        if (cache.get(url) != null) return;

        // One download per url, no matter how often the emoji shows up
        boolean first;
        synchronized (pending) {
            if (cache.get(url) != null) return;
            Vector<WeakReference<TextView>> views = pending.get(url);
            first = (views == null);
            if (first) {
                views = new Vector<WeakReference<TextView>>();
                pending.put(url, views);
            }
            if (view != null) views.addElement(new WeakReference<TextView>(view));
        }
        if (!first) return; // a download for this url is already running

        new Thread() {
            @Override
            public void run() {
                Bitmap bitmap = download(url);

                Vector<WeakReference<TextView>> views;
                synchronized (pending) {
                    views = pending.remove(url);
                }
                if (bitmap != null) put(url, bitmap);
                if (views == null) return;

                for (int i = 0; i < views.size(); i++) {
                    TextView v = views.elementAt(i).get();
                    if (v != null) v.postInvalidate();
                }
            }
        }.start();
    }

    private static void put(String key, Bitmap bitmap) {
        if (cache.size() >= MAX_ENTRIES) {
            cache.clear();
        }
        cache.put(key, bitmap);
    }

    private static Bitmap download(String url) {
        try {
            HTTP h = HTTP.createRequest("GET", url);
            byte[] bytes = h.getResponseBytes();
            h.close();
            return decode(bytes);
        } catch (Exception e) {
            return null;
        }
    }

    private static Bitmap decode(byte[] bytes) {
        try {
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (OutOfMemoryError e) {
            return null;
        }
    }

    private static Bitmap decode(Context context, String path) {
        try {
            InputStream in = context.getAssets().open(path);
            try {
                return BitmapFactory.decodeStream(in);
            } finally {
                in.close();
            }
        } catch (Exception e) {
            return null; // no such emoji in the assets
        } catch (OutOfMemoryError e) {
            return null;
        }
    }
}
