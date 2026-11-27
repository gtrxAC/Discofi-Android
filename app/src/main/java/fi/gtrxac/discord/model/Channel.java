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

    public static final int FLAG_CHANNEL_OBFUSCATED = 1 << 17;
    public static final int FLAG_CHANNEL_SPOILER = 1 << 21;

    public long id;
    protected String name;
    public int type;
    public String parentId;
    public int position;
    public long lastMessageID;
    public int flags;
    public Vector overwrites;
    public long myPermissions = -1;

    public Channel() {
    }

    public Channel(JSONObject data) {
        id = Long.parseLong(data.getString("id"));
        type = data.getInt("type", 0);
        setName(data.getString("name", null));
        parentId = data.getString("parent_id", null);
        position = data.getInt("position", 0);
        flags = data.getInt("flags", 0);

        String msgIdStr = data.getString("last_message_id");
        if (msgIdStr != null) {
            lastMessageID = Long.parseLong(msgIdStr);
        }

        JSONArray overwritesJson = data.getArray("permission_overwrites", null);
        if (overwritesJson != null) {
            overwrites = new Vector(overwritesJson.size());
            for (int i = 0; i < overwritesJson.size(); i++) {
                overwrites.addElement(new Permissions.Overwrite(overwritesJson.getObject(i)));
            }
        }
    }

    /**
     * Whether this channel's metadata has been obfuscated because the current
     * user cannot view it. Obfuscated channels are only received over the
     * Gateway (the HTTP API omits channels the user can't view), and they must
     * always be hidden. Only the flags field can reliably tell whether a
     * channel is obfuscated.
     * https://discord.com/developers/resources/channel#channel-object-obfuscated-channels
     */
    public boolean isObfuscated() {
        return (flags & FLAG_CHANNEL_OBFUSCATED) != 0;
    }

    public boolean isSpoiler() {
        return (flags & FLAG_CHANNEL_SPOILER) != 0;
    }

    public boolean canView() {
        if (isObfuscated()) return false;
        return myPermissions == -1 || Permissions.has(myPermissions, Permissions.VIEW_CHANNEL);
    }

    /**
     * Calculate a guild member's permissions in this channel.
     */
    public long computePermissions(long guildId, GuildMember member) {
        return computePermissions(guildId, member.permissions, member.roles, member.id);
    }

    /**
     * Calculate permissions without role overwrites, for when the user's
     * roles in the guild are not known.
     */
    public long computePermissions(long guildId, long basePermissions, long userId) {
        return computePermissions(guildId, basePermissions, null, userId);
    }

    /**
     * Calculate a user's permissions in this channel, from the user's total
     * permissions in the guild (e.g. from the user's guild list) and this
     * channel's permission overwrites.
     * <p>
     * https://discord.com/developers/docs/topics/permissions#permission-overwrites
     */
    public long computePermissions(long guildId, long basePermissions, Vector roleIds,
        long userId) {
        long permissions = basePermissions;

        // Administrator overrides all permission overwrites
        if (Permissions.has(permissions, Permissions.ADMINISTRATOR)) {
            return Permissions.ALL;
        }

        if (overwrites != null) {
            // Apply the @everyone role's overwrite (its ID is the same as the guild's ID)
            Permissions.Overwrite overwrite =
                Permissions.Overwrite.findBySnowflake(overwrites, guildId);
            if (overwrite != null) {
                permissions &= ~overwrite.deny;
                permissions |= overwrite.allow;
            }

            // Apply overwrites of the user's roles
            if (roleIds != null) {
                long allow = 0;
                long deny = 0;

                for (int i = 0; i < roleIds.size(); i++) {
                    Role role = (Role) roleIds.elementAt(i);
                    Permissions.Overwrite roleOverwrite =
                        Permissions.Overwrite.findBySnowflake(overwrites, role.id);
                    if (roleOverwrite == null) continue;

                    allow |= roleOverwrite.allow;
                    deny |= roleOverwrite.deny;
                }

                permissions &= ~deny;
                permissions |= allow;
            }

            // Apply the user's member-specific overwrite, if it exists
            overwrite = Permissions.Overwrite.findBySnowflake(overwrites, userId);
            if (overwrite != null) {
                permissions &= ~overwrite.deny;
                permissions |= overwrite.allow;
            }
        }

        return permissions;
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

    public void setName(String newName) {
        name = newName;

        // For the purposes of our client, a blank name (like an unnamed group chat)
        // will have the same behavior as no name, so we can easily replace with "(unknown)"
        if (name != null && name.trim().length() == 0) {
            name = null;
        }
    }

    /**
     * Get raw unformatted name of the channel that is used for loading/saving data.
     * The raw name may be null, but the other getName methods will always return a valid string.
     */
    public String getRawName() {
        return name;
    }

    /**
     * Get display name of the channel that is shown in title bar.
     */
    public String getNameForTitle() {
        if (name == null) return "(unknown)";
        return getNamePrefix() + name;
    }

    /**
     * Get display name of the channel that is shown in messages mentioning this channel.
     */
    public String getNameForMention() {
        if (name == null) return "(unknown)";
        // Always with a #, even for voice channels
        return "#" + name;
    }

    /**
     * Get display name of the channel that is shown in notifications.
     */
    public String getNameForNotification() {
        return getNameForTitle();
    }

    /**
     * Get display name of the channel that is shown in lists.
     */
    public String getNameForList() {
        return getNameForTitle();
    }

    protected String getNamePrefix() {
        // Voice channels don't have a #. Category channels also wouldn't have a #,
        // but we don't create Channel objects from those, so they are not checked here.
        return (type != TYPE_GUILD_VOICE) ? "#" : "";
    }

    /**
     * Whether clicking this channel can open a chat screen.
     */
    public boolean isMessageable() {
        return type == TYPE_GUILD_TEXT || type == TYPE_GUILD_NEWS || type == TYPE_GUILD_VOICE;
    }
}
