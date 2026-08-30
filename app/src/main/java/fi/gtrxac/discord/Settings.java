package fi.gtrxac.discord;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import java.util.Vector;

/**
 * Created by gtrx on 29.8.2026.
 */

public class Settings {
    private static final String PREFS_NAME = "s";

    public static void load(Context c) {
        SharedPreferences settings = c.getSharedPreferences(PREFS_NAME, 0);

        int numAccounts = settings.getInt("numAccounts", 0);

        if (numAccounts > 0) {
            Accounts.list = new Vector<>();

            for (int i = 0; i < numAccounts; i++) {
                Account acc = new Account();

                acc.token = settings.getString("accountToken" + i, "");
                acc.username = settings.getString("accountUsername" + i, null);
                acc.useProxy = settings.getBoolean("accountUseProxy" + i, false);
                acc.useGateway = settings.getBoolean("accountUseGateway" + i, true);
                acc.apiUrl = settings.getString("accountApiUrl" + i, null);
                acc.gatewayUrl = settings.getString("accountGatewayUrl" + i, null);
                acc.cdnUrl = settings.getString("accountCdnUrl" + i, null);

                Accounts.list.add(acc);
            }

            int activeAccount = settings.getInt("activeAccount", -1);
            Accounts.setActive(activeAccount);
        }
    }

    public static void save(Context c) {
        SharedPreferences settings = c.getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();

        int numAccounts = Accounts.list.size();
        editor.putInt("numAccounts", numAccounts);

        for (int i = 0; i < numAccounts; i++) {
            Account acc = Accounts.list.elementAt(i);

            editor.putString("accountToken" + i, acc.token);
            editor.putString("accountUsername" + i, acc.username);
            editor.putBoolean("accountUseProxy" + i, acc.useProxy);
            editor.putBoolean("accountUseGateway" + i, acc.useGateway);
            editor.putString("accountApiUrl" + i, acc.apiUrl);
            editor.putString("accountGatewayUrl" + i, acc.gatewayUrl);
            editor.putString("accountCdnUrl" + i, acc.cdnUrl);
        }

        editor.putInt("activeAccount", Accounts.getActiveIndex());

        editor.commit();
    }
}
