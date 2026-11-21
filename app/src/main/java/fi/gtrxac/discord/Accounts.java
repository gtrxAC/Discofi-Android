package fi.gtrxac.discord;

import android.util.Log;

import java.util.Vector;

/**
 * Created by gtrx on 29.8.2026.
 */

public class Accounts {
    private static final String TAG = "Accounts";

    public static Vector<Account> list = new Vector<>();
    private static int activeIndex = -1;

    public static int getActiveIndex() {
        return activeIndex;
    }

    public static Account getActive() {
        if (activeIndex == -1 || activeIndex >= list.size()) {
            return null;
        }
        return list.elementAt(activeIndex);
    }

    public static void setActive(int index) {
        Log.d(TAG, "Active account index set to " + index + " (" + list.size() + " accounts)");
        activeIndex = index;
    }
}
