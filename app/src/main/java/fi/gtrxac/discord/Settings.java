package fi.gtrxac.discord;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.Vector;

import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONArray;

/**
 * Created by gtrx on 29.8.2026.
 */

public class Settings {
    private static final String PREFS_NAME = "s";

    /**
     * Device vendor ID used in Discord X-Super-Properties header (persistent random UUID for each device)
     */
    public static String deviceVendorID = DiscordUtil.generateRandomUUID();

    public static void load(Context c) {
        SharedPreferences settings = c.getSharedPreferences(PREFS_NAME, 0);

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
    }

    public static void save(Context c) {
        SharedPreferences settings = c.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();

        int numAccounts = Accounts.list.size();
        editor.putInt("numAccounts", numAccounts);

        for (int i = 0; i < numAccounts; i++) {
            Account acc = Accounts.list.elementAt(i);
            editor.putString("account" + i, acc.toJSON().build());
        }

        editor.putInt("activeAccount", Accounts.getActiveIndex());
        editor.putString("deviceVendorID", deviceVendorID);

        editor.commit();
    }
}
