package fi.gtrxac.discord;

import util.Base64;
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

    public String email;
    public String password;

    public Account() {
    }

    public String getDisplayTitle() {
        if (username != null) return username;
        return getUserID();
    }

    public String getUserID() {
        String tokenIDPart = token.split("\\.")[0];
        byte[] decodeBytes = Base64.decodeFast(tokenIDPart);
        return new String(decodeBytes);
    }
}
