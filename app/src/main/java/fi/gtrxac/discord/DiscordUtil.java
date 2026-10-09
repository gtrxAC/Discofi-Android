package fi.gtrxac.discord;

import java.util.*;
import util.Base64;

public class DiscordUtil {
    public static final Random random = new Random();

    private static Vector getInvalidTokenTips() {
        Vector result = new Vector();
        result.addElement("use password login instead of your token");
        result.addElement("if you want to use your token, make sure you have retrieved it correctly");
        result.addElement("to avoid typos, import your token from a text file or insert it into the JAR (see login screen for details)");
        return result;
    }

    public static String getUserIDFromToken(String token) throws DiscordException {
        // StringBuffer id = new StringBuffer();
        int b64idLength = token.indexOf('.');

        if (b64idLength < 20) {
            throw new DiscordException(
                "Token is written incorrectly",
                "Token's ID segment is missing or too short",
                getInvalidTokenTips());
        }

        String b64id = token.substring(0, b64idLength);
        byte[] idBytes = Base64.decodeFast(b64id);
        String id = new String(idBytes);

        try {
            return Long.toString(Long.parseLong(id));
        }
        catch (Exception e) {
            throw new DiscordException(
                "Token is written incorrectly",
                "Failed to decode ID: " + e.toString(),
                getInvalidTokenTips());
        }
    }

    public static String generateRandomUUID() {
        byte[] bytes = new byte[16];

        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (random.nextInt() & 0xff);
        }

        bytes[6] = (byte) ((bytes[6] & 0x0f) | 0x40);
        bytes[8] = (byte) ((bytes[8] & 0x3f) | 0x80);

        StringBuffer uuid = new StringBuffer(36);
        for (int i = 0; i < bytes.length; i++) {
            if (i == 4 || i == 6 || i == 8 || i == 10) {
                uuid.append('-');
            }

            int value = bytes[i] & 0xff;
            if (value < 0x10) {
                uuid.append('0');
            }
            uuid.append(Integer.toHexString(value));
        }
        return uuid.toString();
    }

    public static final long DISCORD_EPOCH = 1420070400000L;

    public static String formatTimestamp(long id, boolean use12hTime) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        int todayDay = cal.get(Calendar.DAY_OF_MONTH);
        int todayMonth = cal.get(Calendar.MONTH);
        int todayYear = cal.get(Calendar.YEAR);

        cal.setTime(new Date((id >> 22) + DISCORD_EPOCH));
        int stampDay = cal.get(Calendar.DAY_OF_MONTH);
        int stampMonth = cal.get(Calendar.MONTH);
        int stampYear = cal.get(Calendar.YEAR);

        StringBuffer out = new StringBuffer();

        if (todayDay != stampDay || todayMonth != stampMonth || todayYear != stampYear) {
            // Timestamp does not point to today -> show date in day/month format
            // Leading zeros in numbers are optional, here I left them out for the sake of compactness

            out.append(stampDay).append('/').append(stampMonth + 1);

            // With leading zeros:
            // stampMonth++;
            // if (stampDay < 10) time.append('0');
            // out.append(stampDay).append('/');
            // if (stampMonth < 10) time.append('0');
            // time.append(stampMonth);
        } else {
            // Today -> show time in hour:minute format
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            int minute = cal.get(Calendar.MINUTE);

            if (use12hTime) {
                char period = hour < 12 ? 'A' : 'P';

                // Convert hours to 12-hour format
                hour = hour % 12;
                if (hour == 0) {
                    hour = 12; // 12 AM or 12 PM
                }

                out.append(hour).append(':');
                if (minute < 10) out.append('0');
                out.append(minute).append(period);
            } else {
                out.append(hour).append(':');
                if (minute < 10) out.append('0');
                out.append(minute);
            }
        }
        return out.toString();
    }
}
