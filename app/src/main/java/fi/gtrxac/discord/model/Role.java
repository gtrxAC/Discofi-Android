package fi.gtrxac.discord.model;

import java.util.Vector;

import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;

public class Role {
    public long id;
    public String name;
    public int color;
    public long permissions;
    public int position;

    public Role(JSONObject data) {
        id = Long.parseLong(data.getString("id"));
        name = data.getString("name");
        color = data.getInt("color");
        if (data.has("colors")) {
            color = data.getObject("colors").getInt("primary_color");
        }
        permissions = Long.parseLong(data.getString("permissions"));
        position = data.getInt("position");
    }

    public static Vector parseRoles(JSONArray arr) {
        Vector result = new Vector();
        boolean[] used = new boolean[arr.size()];

        // Sort by position, highest first.
        for (int i = 0; i < arr.size(); i++) {
            int highestIndex = -1;
            int highestPosition = Integer.MIN_VALUE;

            for (int a = 0; a < arr.size(); a++) {
                if (used[a]) continue;

                int position = arr.getObject(a).getInt("position", -1);
                if (position > highestPosition) {
                    highestPosition = position;
                    highestIndex = a;
                }
            }

            if (highestIndex == -1) break;

            used[highestIndex] = true;
            result.addElement(new Role(arr.getObject(highestIndex)));
        }

        return result;
    }

    public static Vector<Role> parseGuildMemberRoles(Vector guildRoles, JSONArray arr) {
        Vector<Role> result = new Vector<Role>();
        if (guildRoles == null) return result;

        for (int i = 0; i < guildRoles.size(); i++) {
            Role role = (Role) guildRoles.elementAt(i);
            if (arr.indexOf(Long.toString(role.id)) != -1) {
                result.addElement(role);
            }
        }

        return result;
    }
}
