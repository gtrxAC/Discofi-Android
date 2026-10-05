package fi.gtrxac.discord.model;

import cc.nnproject.json.JSONObject;

public class Channel {
    // https://docs.discord.food/resources/channel#channel-type
    public static final int TYPE_GUILD_TEXT = 0;
    public static final int TYPE_DM = 1;
    public static final int TYPE_GUILD_VOICE = 2;
    public static final int TYPE_GROUP_DM = 3;
    public static final int TYPE_GUILD_CATEGORY = 4;
    public static final int TYPE_GUILD_NEWS = 5;
    public static final int TYPE_GUILD_FORUM = 15;
    public static final int TYPE_GUILD_MEDIA = 16;

    public String id;
    public String name;
    public int type;
    public String parentId;
    public int position;

    public Channel() {}

    public Channel(JSONObject data) {
        id = data.getString("id");
        type = data.getInt("type", 0);
        name = data.getString("name", null);
        parentId = data.getString("parent_id", null);
        position = data.getInt("position", 0);

        if (name != null && nameHasHashtag()) name = "#" + name;
        if (name == null) name = "(unknown)";
    }

    private boolean nameHasHashtag() {
        // Voice channels are displayed without a hashtag
        return type != TYPE_GUILD_VOICE;
    }

    /**
     * Whether clicking this channel can open a chat screen.
     */
    public boolean isMessageable() {
        return type == TYPE_GUILD_TEXT || type == TYPE_GUILD_NEWS || type == TYPE_GUILD_VOICE;
    }
}
