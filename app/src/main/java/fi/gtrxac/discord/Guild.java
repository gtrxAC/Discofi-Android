package fi.gtrxac.discord;

import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;

/**
 * Created by gtrx on 30.8.2026.
 */

public class Guild {
    public String id;
    public String name;
    public String iconHash;

    public Guild(JSONObject data) {
        id = data.getString("id");

        if (data.has("name")) {
            name = data.getString("name");
        }
        else if (data.has("properties")) {
            name = data.getObject("properties").getString("name");
        }
        else {
            name = "(unknown)";
        }

        iconHash = data.getString("icon", null);
    }

    public Guild(JSONArray saveData) {
        id = saveData.getString(0);
        name = saveData.getString(1);
        iconHash = saveData.getString(2, null);
    }

    public JSONArray toJSON() {
        JSONArray result = new JSONArray();
        result.add(id);
        result.add(name);
        if (iconHash != null) result.add(iconHash);
        return result;
    }
}
