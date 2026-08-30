package fi.gtrxac.discord;

import android.app.Activity;
import android.content.SharedPreferences;

/**
 * Created by gtrx on 29.8.2026.
 */

public class Settings {
    private static final String PREFS_NAME = "s";

    public static void load(Activity activity) {
        SharedPreferences settings = activity.getSharedPreferences(PREFS_NAME, 0);

        int numAccounts = settings.getInt("numAccounts", 0);

        if (numAccounts > 0) {
            for (int i = 0; i < numAccounts; i++) {
                String token = settings.getString("accountToken" + i, "");
                String username = settings.getString("accountUsername" + i, null);
                Accounts.add(token, username);
            }

            int activeAccount = settings.getInt("activeAccount", -1);
            Accounts.setActive(activeAccount);
        }
    }

    public static void save(Activity activity) {
        SharedPreferences settings = activity.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();

        int numAccounts = Accounts.list.size();
        editor.putInt("numAccounts", numAccounts);

        for (int i = 0; i < numAccounts; i++) {
            Account acc = Accounts.list.elementAt(i);
            editor.putString("accountToken" + i, acc.getToken());
            editor.putString("accountUsername" + i, acc.getUsername());
        }

        editor.putInt("activeAccount", Accounts.getActiveIndex());

        editor.commit();
    }
}
