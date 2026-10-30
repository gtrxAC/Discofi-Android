package fi.gtrxac.discord;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.util.DisplayMetrics;

import java.util.WeakHashMap;

public class Theme {
    public enum FontSize {
        SMALL(0.75f),
        MEDIUM(1.0f),
        LARGE(1.25f);

        private float size;

        private FontSize(float size) {
            this.size = size;
        }

        public float getSize() {
            return size;
        }
    };

    public static int embedBackground;
    public static int codeBackground;
    public static int quoteColor;
    public static int messageMentionedBackground;
    public static int mentionBackground;
    public static int mentionForeground;
    public static int badgeBackground;

    /**
     * Appearance values that each activity was created with, used to recreate
     * activities when the theme or font size changes.
     */
    private static final WeakHashMap<Activity, Object[]> appliedAppearances = new WeakHashMap<>();

    public static void loadLightTheme(Context c) {
        embedBackground = c.getResources().getColor(R.color.embed_background_light);
        codeBackground = c.getResources().getColor(R.color.code_background_light);
        quoteColor = c.getResources().getColor(R.color.quote_color_light);
        messageMentionedBackground = c.getResources().getColor(R.color.message_mentioned_background);
        mentionBackground = c.getResources().getColor(R.color.mention_background);
        mentionForeground = c.getResources().getColor(R.color.mention_foreground_light);
        badgeBackground = c.getResources().getColor(R.color.badge_background);
    }

    public static void loadDarkTheme(Context c) {
        embedBackground = c.getResources().getColor(R.color.embed_background_dark);
        codeBackground = c.getResources().getColor(R.color.code_background_dark);
        quoteColor = c.getResources().getColor(R.color.quote_color_dark);
        messageMentionedBackground = c.getResources().getColor(R.color.message_mentioned_background);
        mentionBackground = c.getResources().getColor(R.color.mention_background);
        mentionForeground = c.getResources().getColor(R.color.mention_foreground_dark);
        badgeBackground = c.getResources().getColor(R.color.badge_background);
    }

    public static void load(Activity activity) {
        if (Settings.lightTheme) {
            loadLightTheme(activity);
            activity.setTheme(R.style.AppTheme_Light);
        }
        else {
            loadDarkTheme(activity);
            activity.setTheme(R.style.AppTheme);
        }
        applyFontScale(activity);

        appliedAppearances.put(activity, new Object[] { Settings.lightTheme, Settings.fontSize });
    }

    public static void applyFontScale(Activity activity) {
        Configuration config = activity.getResources().getConfiguration();
        config.fontScale = Settings.fontSize.getSize();
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        activity.getResources().updateConfiguration(config, metrics);
    }

    public static boolean checkAppearanceChanged(Activity activity) {
        Object[] applied = appliedAppearances.get(activity);
        if (applied == null) return false;

        if ((boolean) applied[0] == Settings.lightTheme && (FontSize) applied[1] == Settings.fontSize) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            activity.recreate();
        }
        else {
            // Activity.recreate() is not available before Android 3.0
            Intent intent = activity.getIntent();
            activity.finish();
            activity.startActivity(intent);
        }
        return true;
    }
}
