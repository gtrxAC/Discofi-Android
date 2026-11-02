package fi.gtrxac.discord.model;

import java.util.Vector;

import cc.nnproject.json.JSONArray;
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

    public long id;
    public String name;
    public int type;
    public String parentId;
    public int position;

    public Channel() {}

    public Channel(JSONObject data) {
        id = Long.parseLong(data.getString("id"));
        type = data.getInt("type", 0);
        name = data.getString("name", null);
        parentId = data.getString("parent_id", null);
        position = data.getInt("position", 0);

        if (name != null && nameHasHashtag()) name = "#" + name;
        if (name == null) name = "(unknown)";
    }

    /**
     * Parse fetched channel list into sorted categories and channels.
     */
    public static Vector parseChannels(JSONArray channelsJson) {
        int numChannels = channelsJson.size();

        Vector sortedChannels = new Vector();
        Vector sortedPositions = new Vector();

        // Insertion sort by position
        for (int i = 0; i < numChannels; i++) {
            JSONObject channelJson = channelsJson.getObject(i);
            int position = channelJson.getInt("position", i);
            int insertAt = sortedChannels.size();

            while (insertAt > 0) {
                Integer previousPosition = (Integer) sortedPositions.elementAt(insertAt - 1);
                if (previousPosition.intValue() <= position) break;
                insertAt--;
            }

            sortedChannels.insertElementAt(channelJson, insertAt);
            sortedPositions.insertElementAt(new Integer(position), insertAt);
        }

        Vector textChannelsJson = new Vector();
        Vector voiceChannelsJson = new Vector();
        Vector categories = new Vector();

        ChannelCategory defaultCategory = new ChannelCategory((String) null);
        categories.addElement(defaultCategory);

        // Divide all channels into text/voice channels and channel categories.
        // Voice channels are separate so that they can be added last.
        // If we were to go just by position numbers, VC's would show first.
        // Channels with unsupported types are discarded.
        for (int i = 0; i < sortedChannels.size(); i++) {
            JSONObject channelJson = (JSONObject) sortedChannels.elementAt(i);
            int type = channelJson.getInt("type", 0);

            switch (type) {
                case Channel.TYPE_GUILD_CATEGORY: {
                    categories.addElement(new ChannelCategory(channelJson));
                    break;
                }
                case Channel.TYPE_GUILD_TEXT:
                case Channel.TYPE_GUILD_NEWS:
                case Channel.TYPE_GUILD_FORUM:
                case Channel.TYPE_GUILD_MEDIA: {
                    textChannelsJson.addElement(channelJson);
                    break;
                }
                case Channel.TYPE_GUILD_VOICE: {
                    voiceChannelsJson.addElement(channelJson);
                    break;
                }
            }
        }

        groupChannelsIntoCategories(textChannelsJson, categories);
        groupChannelsIntoCategories(voiceChannelsJson, categories);

        if (defaultCategory.children.size() == 0) {
            categories.removeElementAt(0);
        }

        return categories;
    }

    private static void groupChannelsIntoCategories(Vector channels, Vector categories) {
        for (int i = 0; i < channels.size(); i++) {
            JSONObject channelJson = (JSONObject) channels.elementAt(i);
            String parentID = channelJson.getString("parent_id", null);

            // Fallback to default (topmost) category
            ChannelCategory parentCategory = (ChannelCategory) categories.elementAt(0);

            // Find the parent category
            for (int j = 1; j < categories.size(); j++) {
                ChannelCategory category = (ChannelCategory) categories.elementAt(j);

                if (Long.toString(category.id).equals(parentID)) {
                    parentCategory = category;
                    break;
                }
            }

            Channel channel = new Channel(channelJson);
            parentCategory.children.addElement(channel);
        }
    }

    protected boolean nameHasHashtag() {
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
