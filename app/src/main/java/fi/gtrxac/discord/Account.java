package fi.gtrxac.discord;

import java.util.Vector;

import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;
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

    private Vector<Guild> guilds;

    public Account() {
        guilds = new Vector<>();
    }

    public Account(JSONArray data) {
        this();
        token = data.getString(0);
        username = data.getString(1, "");
        useProxy = data.getBoolean(2);
        useGateway = data.getBoolean(3);
        apiUrl = data.getString(4, "");
        gatewayUrl = data.getString(5, "");
        cdnUrl = data.getString(6, "");

        JSONArray guildsJson = data.getArray(7, new JSONArray());

        for (int i = 0; i < guildsJson.size(); i++) {
            JSONArray guildData = guildsJson.getArray(i);
            guilds.add(new Guild(guildData));
        }
    }

    public JSONArray toJSON() {
        JSONArray result = new JSONArray();
        result.add(token);
        result.add(username);
        result.add(useProxy);
        result.add(useGateway);
        result.add(apiUrl);
        result.add(gatewayUrl);
        result.add(cdnUrl);

        JSONArray guildsJson = new JSONArray();

        for (int i = 0; i < guilds.size(); i++) {
            Guild g = guilds.elementAt(i);
            guildsJson.add(g.toJSON());
        }

        result.add(guildsJson);

        return result;
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
