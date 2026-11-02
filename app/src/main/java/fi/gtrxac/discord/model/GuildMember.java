package fi.gtrxac.discord.model;

import java.util.Vector;

import cc.nnproject.json.JSONObject;

public class GuildMember extends User {
    public String nickname;
    public Vector<Role> roles;
    public long permissions;

    public GuildMember(JSONObject data, Guild g) {
        super(data.getObject("user"));

        JSONObject user = data.getObject("user");
        name = user.getString("display_name", user.getString("global_name", username));
        if (name == null) name = "(unknown)";
        bot = user.getBoolean("bot", false);

        if (data.has("nick"))
            nickname = data.getString("nick", null);

        try {
            if (data.has("roles"))
                roles = Role.parseGuildMemberRoles(g, data.getArray("roles"));
        } catch (Exception e) {
            e.printStackTrace();
            roles = new Vector<Role>();
        }

        iconHash = data.getString("avatar", null);
    }
}
