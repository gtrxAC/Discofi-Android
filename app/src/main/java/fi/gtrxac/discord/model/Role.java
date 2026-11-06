package fi.gtrxac.discord.model;

import java.util.Vector;

import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;

public class Role {
	public long id;
	public int color;
	public long permissions;
	public int position;

	public Role(JSONObject data) {
		id = Long.parseLong(data.getString("id"));
		color = data.getInt("color");
		if (data.has("colors")) {
			color = data.getObject("colors").getInt("primary_color");
		}
		permissions = Long.parseLong(data.getString("permissions"));
		position = data.getInt("position");
	}

	public static Vector parseRoles(JSONArray arr) {
		Vector result = new Vector();

		for (int i = arr.size() - 1; i >= 0; i--) {
			for (int a = arr.size() - 1; a >= 0; a--) {
				JSONObject data = arr.getObject(a);

				if (data.getInt("position", -1) != i)
					continue;

				if (data.getInt("color") == 0)
					continue;

				result.addElement(new Role(data));
			}
		}

		return result;
	}

	public static Vector<Role> parseGuildMemberRoles(Vector guildRoles, JSONArray arr) {
		Vector<Role> result = new Vector<Role>();

		for (int i = arr.size() - 1; i >= 0; i--) {
			for (int j = 0; j < guildRoles.size(); j++) {
				Role role = (Role) guildRoles.elementAt(j);
				if (role.id == Long.parseLong(arr.getString(i))) {
					if (role.position != i)
						continue;

					result.addElement(role);
				}
			}
		}

		return result;
	}
}
