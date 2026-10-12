package fi.gtrxac.discord.model;

import cc.nnproject.json.JSONObject;

public class User {
    public long id;
    public String username;
    public String name;
    public String iconHash;
    public boolean bot;
    public boolean system;

    public User() {}

    public User(JSONObject data) {
        id = Long.parseLong(data.getString("id"));
        username = data.getString("username", null);
        name = data.getString("global_name", null);
        if (name == null) name = username;
        if (name == null) name = "(no name)";
        iconHash = data.getString("avatar", null);
        bot = data.getBoolean("bot", false);
        system = data.getBoolean("system", false);
    }

    public String getAvatarUrl(String cdnUrl) {
        if (iconHash == null || cdnUrl == null) return null;
        return cdnUrl + "/avatars/" + id + "/" + iconHash + ".png?size=64";
    }
}
