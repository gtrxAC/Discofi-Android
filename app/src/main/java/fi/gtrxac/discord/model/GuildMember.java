package fi.gtrxac.discord.model;

import fi.gtrxac.discord.Settings;

import java.util.Vector;

import cc.nnproject.json.JSONObject;
import cc.nnproject.json.JSONArray;

public class GuildMember extends User {
    public String nickname;
    public int color;
    public Vector<Role> roles;
    public long permissions;
    public String guildIconHash;

    public GuildMember(JSONObject data, Guild guild) {
        this(data, guild.roles);
    }

    public GuildMember(JSONObject data, Vector guildRoles) {
        super(data.getObject("user"));

        if (data.has("nick")) nickname = data.getString("nick", null);

        try {
            if (data.has("roles")) {
                JSONArray roleArray = data.getArray("roles");
                roles = Role.parseGuildMemberRoles(guildRoles, roleArray);
                for (int i = 0; i < guildRoles.size(); i++) {
                    Role role = (Role) guildRoles.elementAt(i);
                    if (roleArray.indexOf(Long.toString(role.id)) == -1) continue;

                    color = role.color;
                    if (color != 0) color |= 0xFF000000;
                    break;
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            roles = new Vector<Role>();
        }

        guildIconHash = data.getString("avatar", null);
    }

    public GuildMember(User user) {
        this.id = user.id;
        this.username = user.username;
        this.name = user.name;
        this.iconHash = user.iconHash;
        this.bot = user.bot;
        this.system = user.system;
        this.roles = new Vector<Role>();
    }

    public String getAvatarUrl(String cdnUrl, long guildId) {
        if (guildIconHash != null && cdnUrl != null) {
            return cdnUrl + "/guilds/" + guildId + "/users/" + id + "/avatars/" + guildIconHash +
                ".png?size=64";
        }
        return super.getAvatarUrl(cdnUrl);
    }
}
