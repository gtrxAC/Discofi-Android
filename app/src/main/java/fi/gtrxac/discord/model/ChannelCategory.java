package fi.gtrxac.discord.model;

import java.util.Vector;

import cc.nnproject.json.JSONObject;

public class ChannelCategory {
	public long id;
	public String name;
	public Vector children;

	public ChannelCategory(JSONObject data) {
		this(data.getString("name"));
		this.id = Long.parseLong(data.getString("id"));
	}

	public ChannelCategory(String name) {
		this.name = name;
		this.children = new Vector();
	}
}
