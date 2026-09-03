package fi.gtrxac.discord;

import java.util.Vector;

import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONException;
import cc.nnproject.json.JSONObject;
import fi.gtrxac.bluewap.http.HTTP;
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
    private Vector<DMChannel> dmChannels;

    public Account() {
        guilds = new Vector<>();
        dmChannels = new Vector<>();
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

        JSONArray dmChannelsJson = data.getArray(8, new JSONArray());

        for (int i = 0; i < dmChannelsJson.size(); i++) {
            JSONArray dmChannelData = dmChannelsJson.getArray(i);
            dmChannels.add(new DMChannel(dmChannelData));
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

        JSONArray dmChannelsJson = new JSONArray();

        for (int i = 0; i < dmChannels.size(); i++) {
            DMChannel d = dmChannels.elementAt(i);
            dmChannelsJson.add(d.toJSON());
        }

        result.add(dmChannelsJson);

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

    public String getApiUrl() {
        return useProxy ? apiUrl : "https://discord.com";
    }

    public String getCdnUrl() {
        return useProxy ? cdnUrl : "https://cdn.discordapp.com";
    }

    public HTTP createApiRequest(String method, String route) throws Exception {
        HTTP result = HTTP.createRequest(method, getApiUrl() + "/api/v9" + route);
        result.setHeader("Authorization", token);
        result.setHeader("X-Super-Properties", SuperProperties.get());
        return result;
    }

    public String getApiErrorMessage(int responseCode, String responseText) {
        try {
            JSONObject json = JSON.getObject(responseText);
            return json.getString("message");
        }
        catch (JSONException e) {}

        return "HTTP error " + responseCode;
    }

    public boolean guildsAvailable() {
        return guilds.size() != 0;
    }

    public boolean dmChannelsAvailable() {
        return dmChannels.size() != 0;
    }

    private JSONArray fetchGuildsOrDms(String route) throws Exception {
        HTTP h = createApiRequest("GET", route);
        int responseCode = h.getResponseCode();
        String responseText = h.getResponseString();

        if (responseCode < 200 || responseCode >= 300) {
            throw new Exception(getApiErrorMessage(responseCode, responseText));
        }

        return JSON.getArray(responseText);
    }

    public Vector<Guild> getGuilds(boolean refresh) throws Exception {
        if (!refresh && guildsAvailable()) return guilds;

        JSONArray guildsJson = fetchGuildsOrDms("/users/@me/guilds");

        for (int i = 0; i < guildsJson.size(); i++) {
            JSONObject guildJson = guildsJson.getObject(i);
            guilds.add(new Guild(guildJson));
        }

        return guilds;
    }

    public Vector<DMChannel> getDmChannels(boolean refresh) throws Exception {
        if (!refresh && dmChannelsAvailable()) return dmChannels;

        JSONArray dmsJson = fetchGuildsOrDms("/users/@me/channels");

        for (int i = 0; i < dmsJson.size(); i++) {
            JSONObject dmJson = dmsJson.getObject(i);
            dmChannels.add(new DMChannel(dmJson));
        }

        return dmChannels;
    }
}
