package fi.gtrxac.discord.model;

import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;

/**
 * Created by gtrx on 30.8.2026.
 */

public class Guild {
    public long id;
    public String name;
    public String iconHash;

    public Guild(JSONObject data) {
        id = Long.parseLong(data.getString("id"));

        if (data.has("name")) {
            name = data.getString("name");
        }
        // else if (data.has("properties")) {
        //     name = data.getObject("properties").getString("name");
        // }
        else {
            name = "(unknown)";
        }

        iconHash = data.getString("icon", null);
    }

    public Guild(JSONArray saveData) {
        id = saveData.getLong(0);
        name = saveData.getString(1);
        iconHash = saveData.getString(2, null);
    }

    public String getIconUrl(String cdnUrl) {
        if (iconHash == null || cdnUrl == null) return null;
        return cdnUrl + "/icons/" + Long.toString(id) + "/" + iconHash + ".png?size=64";
    }

    public JSONArray toJSON() {
        JSONArray result = new JSONArray();
        result.add(id);
        result.add(name);
        if (iconHash != null) result.add(iconHash);
        return result;
    }
}
