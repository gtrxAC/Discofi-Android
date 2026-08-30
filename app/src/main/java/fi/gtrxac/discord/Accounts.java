package fi.gtrxac.discord;

import java.util.Vector;

/**
 * Created by gtrx on 29.8.2026.
 */

public class Accounts {
    public static Vector<Account> list = new Vector<>();
    private static int activeIndex = -1;

    public static void setActive(int index) {
        activeIndex = index;
    }

    public static int getActiveIndex() {
        return activeIndex;
    }

    public static Account getActive() {
        if (activeIndex == -1 || activeIndex >= list.size()) {
            return null;
        }
        return list.elementAt(activeIndex);
    }
}
