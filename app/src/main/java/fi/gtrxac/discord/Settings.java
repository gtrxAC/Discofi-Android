package fi.gtrxac.discord;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Vector;

import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONArray;

/**
 * Created by gtrx on 29.8.2026.
 */

public class Settings {
    public static final int MESSAGE_LOAD_COUNT_MIN = 1;
    public static final int MESSAGE_LOAD_COUNT_MAX = 100;
    public static final int MESSAGE_LOAD_COUNT_DEFAULT = 25;

    public static boolean lightTheme = false;
    public static Theme.FontSize fontSize = Theme.FontSize.MEDIUM;
    public static boolean squareAvatars = false;
    public static boolean use12hTime = false;
    public static int messageLoadCount = MESSAGE_LOAD_COUNT_DEFAULT;
    public static boolean sendTyping = true;
    public static boolean autoReconnect = true;
    public static boolean showReplies = true;
    public static boolean useMarkdown = true;
    public static boolean showImages = true;
    public static boolean compactMode = false;

    /**
     * Device vendor ID used in Discord X-Super-Properties header (persistent random UUID for each device)
     */
    public static String deviceVendorID = DiscordUtil.generateRandomUUID();

    public static void load(Context c) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);

        int numAccounts = settings.getInt("numAccounts", 0);

        if (numAccounts > 0) {
            Accounts.list = new Vector<>();

            for (int i = 0; i < numAccounts; i++) {
                String accountStr = settings.getString("account" + i, null);
                if (accountStr == null) continue;

                JSONArray accountJson = JSON.getArray(accountStr);
                Account account = new Account(accountJson);
                Accounts.list.add(account);
            }

            int activeAccount = settings.getInt("activeAccount", -1);
            Accounts.setActive(activeAccount);
        }

        deviceVendorID = settings.getString("deviceVendorID", DiscordUtil.generateRandomUUID());

        lightTheme = settings.getString("theme", "dark").equals("light");
        fontSize = Theme.FontSize.valueOf(settings.getString("fontSize", "MEDIUM").toUpperCase());
        squareAvatars = settings.getString("avatarShape", "circle").equals("square");
        use12hTime = settings.getString("timeFormat", "24").equals("12");
        messageLoadCount = Integer.valueOf(settings.getString("messageLoadCount", Integer.toString(MESSAGE_LOAD_COUNT_DEFAULT)));
        sendTyping = settings.getBoolean("sendTyping", true);
        autoReconnect = settings.getBoolean("autoReconnect", true);
        showReplies = settings.getBoolean("showReplies", true);
        useMarkdown = settings.getBoolean("textFormatting", true);
        showImages = settings.getBoolean("showImages", true);
        compactMode = settings.getBoolean("compactMode", false);
    }

    public static void save(Context c) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(c);
        SharedPreferences.Editor editor = settings.edit();

        int numAccounts = Accounts.list.size();
        editor.putInt("numAccounts", numAccounts);

        for (int i = 0; i < numAccounts; i++) {
            Account acc = Accounts.list.elementAt(i);
            editor.putString("account" + i, acc.toJSON().build());
        }

        editor.putInt("activeAccount", Accounts.getActiveIndex());
        editor.putString("deviceVendorID", deviceVendorID);

        editor.putString("theme", lightTheme ? "light" : "dark");
        editor.putString("fontSize", fontSize.toString().toUpperCase());
        editor.putString("avatarShape", squareAvatars ? "square" : "circle");
        editor.putString("timeFormat", use12hTime ? "12" : "24");
        editor.putString("messageLoadCount", Integer.toString(messageLoadCount));
        editor.putBoolean("sendTyping", sendTyping);
        editor.putBoolean("autoReconnect", autoReconnect);
        editor.putBoolean("showReplies", showReplies);
        editor.putBoolean("useMarkdown", useMarkdown);
        editor.putBoolean("showImages", showImages);
        editor.putBoolean("compactMode", compactMode);

        editor.commit();
    }
}
