package fi.gtrxac.discord;

import android.util.Base64;

/**
 * Created by gtrx on 29.8.2026.
 */

public class Account {
    public String token;
    public String username;

    public boolean useProxy;
    public boolean useGateway;
    public String apiUrl;
    public String gatewayUrl;
    public String cdnUrl;

    public Account(String token, String username) {
        this.token = token;
        this.username = username;
    }

    public String getDisplayTitle() {
        if (username != null) return username;
        return getUserID();
    }

    public String getUserID() {
        String tokenIDPart = token.split(".")[0];
        byte[] decodeBytes = Base64.decode(tokenIDPart, Base64.URL_SAFE);
        return new String(decodeBytes);
    }
}
