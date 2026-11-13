package fi.gtrxac.discord;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;
import fi.gtrxac.discord.model.Channel;
import fi.gtrxac.discord.model.ChannelCategory;
import fi.gtrxac.discord.model.DMChannel;
import fi.gtrxac.discord.model.Guild;
import fi.gtrxac.discord.model.Message;

public class UnreadManager {
    private final Account account;
    private final Hashtable<Long, Long> channels = new Hashtable<>();
    private final Hashtable<Long, Integer> mentionCounts = new Hashtable<>();
    private final Hashtable<Long, Vector<Long>> myRoles = new Hashtable<>();
    private final Vector<Long> fetchingRoles = new Vector<>();
    public boolean autoSave;
    public boolean needSave;
    public long lastUnreadTime;
    // previously unread message ID for the last channel that markRead() was used on
    public boolean lastHadUnreads;

    // parameters for ack/"mark as read" API call
    public long markReadChannelID;
    public long markReadMessageID;

    public UnreadManager(Account account) {
        this.account = account;
        autoSave = true;
        needSave = false;
        lastUnreadTime = 0;
        lastHadUnreads = false;

        // Default last read time for channels is the timestamp of when the RMS was first
        // initialized
        channels.put(0L, getCurrentTime());
    }

    public void fromJSON(JSONArray data) {
        channels.clear();
        mentionCounts.clear();

        for (int i = 0; i < data.size(); i++) {
            JSONArray entry = data.getArray(i);
            long channelID = entry.getLong(0);
            channels.put(channelID, entry.getLong(1));

            if (channelID != 0 && entry.size() >= 3) {
                int mentions = entry.getInt(2, 0);
                if (mentions > 0) mentionCounts.put(channelID, new Integer(mentions));
            }
        }

        if (!channels.containsKey(0L)) {
            channels.put(0L, getCurrentTime());
            needSave = true;
        }
    }

    public JSONArray toJSON() {
        JSONArray result = new JSONArray();

        for (Enumeration<Long> e = channels.keys(); e.hasMoreElements(); ) {
            long key = e.nextElement();
            long value = channels.get(key);

            JSONArray entry = new JSONArray();
            entry.add(key);
            entry.add(value);

            int mentions = getMentionCount(key);
            if (key != 0 && mentions > 0) entry.add(mentions);

            result.add(entry);
        }

        return result;
    }

    private static long getCurrentTime() {
        return System.currentTimeMillis() - DiscordUtil.DISCORD_EPOCH;
    }

    public void save() {
        if (markReadChannelID != 0) {
            final long channelID = markReadChannelID;
            final long messageID = markReadMessageID;
            markReadChannelID = 0;
            markReadMessageID = 0;

            new Thread() {
                @Override
                public void run() {
                    account.ackMessage(channelID, messageID);
                }
            }.start();
        }

        needSave = false;
    }

    public void manualSave() {
        if (needSave) save();
        needSave = false;
        autoSave = true;
    }

    private void put(long channelID, long lastReadTime) {
        if (lastReadTime == 0) return;
        channels.put(channelID, lastReadTime);

        changed();
    }

    private void changed() {
        if (autoSave) {
            save();
        }
        else {
            needSave = true;
        }
    }

    public boolean hasUnreads(long channelID, long lastMessageID) {
        long lastMessageTime = lastMessageID >> 22;

        Long lastReadTime = channels.get(channelID);
        if (lastReadTime == null) {
            lastReadTime = channels.get(0L);
        }

        return lastReadTime.longValue() < lastMessageTime;
    }

    public boolean hasUnreads(Channel ch) {
        return hasUnreads(ch.id, ch.lastMessageID);
    }

    public boolean hasUnreads(Guild guild) {
        Vector categories = guild.channels;

        if (categories == null) {
            categories = account.channelCache.get(guild.id);
        }
        if (categories == null) return false;

        for (int i = 0; i < categories.size(); i++) {
            ChannelCategory category = (ChannelCategory) categories.elementAt(i);

            for (int j = 0; j < category.children.size(); j++) {
                if (hasUnreads((Channel) category.children.elementAt(j))) return true;
            }
        }
        return false;
    }

    public int getMentionCount(long channelID) {
        Integer count = mentionCounts.get(channelID);
        return count == null ? 0 : count.intValue();
    }

    public int getMentionCount(Channel ch) {
        return getMentionCount(ch.id);
    }

    public int getMentionCount(Guild guild) {
        Vector categories = guild.channels;

        if (categories == null) {
            categories = account.channelCache.get(guild.id);
        }
        if (categories == null) return 0;

        int result = 0;

        for (int i = 0; i < categories.size(); i++) {
            ChannelCategory category = (ChannelCategory) categories.elementAt(i);

            for (int j = 0; j < category.children.size(); j++) {
                Channel ch = (Channel) category.children.elementAt(j);
                result += getMentionCount(ch.id);
            }
        }
        return result;
    }

    private void setMentionCount(long channelID, int count) {
        Integer oldCount = mentionCounts.get(channelID);

        if (count <= 0) {
            if (oldCount == null) return;
            mentionCounts.remove(channelID);
        }
        else {
            if (oldCount != null && oldCount.intValue() == count) return;
            mentionCounts.put(channelID, new Integer(count));
        }

        changed();
    }

    public void clearMentions(long channelID) {
        if (mentionCounts.remove(channelID) != null) changed();
    }

    public boolean mentionsUser(Message message, long guildId) {
        if (message.mentionEveryone) return true;

        if (message.mentions != null &&
            message.mentions.containsKey(Long.toString(account.userId))) {
            return true;
        }

        if (guildId != 0 && message.mentionRoles != null && message.mentionRoles.size() > 0) {
            Vector<Long> roles = myRoles.get(guildId);

            if (roles != null) {
                for (int i = 0; i < message.mentionRoles.size(); i++) {
                    if (roles.contains(message.mentionRoles.elementAt(i))) return true;
                }
            }
        }

        return false;
    }

    public void markRead(long channelID, long lastMessageID) {
        long lastMessageTime = lastMessageID >> 22;

        Long lastReadTime = channels.get(channelID);
        if (lastReadTime == null) {
            lastReadTime = channels.get(0L);
        }
        boolean isUnread = (lastReadTime.longValue() < lastMessageTime);

        if (autoSave) {
            lastUnreadTime = lastReadTime.longValue();
            lastHadUnreads = isUnread;
        }

        boolean hadMentions = mentionCounts.remove(channelID) != null;

        if (isUnread) put(channelID, lastMessageTime);
        if (hadMentions) changed();

        markReadChannelID = channelID;
        markReadMessageID = lastMessageID;
    }

    public void markRead(Channel ch) {
        markRead(ch.id, ch.lastMessageID);
    }

    private void updateChannelLastMessageID(long channelID, long messageID) {
        if (account.dmChannelsAvailable()) {
            try {
                Vector dmChannels = account.getDmChannels(false);

                for (int i = 0; i < dmChannels.size(); i++) {
                    DMChannel dm = (DMChannel) dmChannels.elementAt(i);

                    if (dm.id == channelID) {
                        if (messageID > dm.lastMessageID) dm.lastMessageID = messageID;
                        break;
                    }
                }
            }
            catch (Exception e) {
            }
        }

        account.channelCache.setLastMessageID(channelID, messageID);
    }

    public boolean onMessageCreate(JSONObject data) {
        long channelID = Long.parseLong(data.getString("channel_id"));
        long messageID = Long.parseLong(data.getString("id"));

        updateChannelLastMessageID(channelID, messageID);

        boolean mentioned = false;

        try {
            JSONObject author = data.getObject("author", null);

            if (author != null &&
                author.getString("id").equals(Long.toString(account.getUserID()))) {
                forceMarkRead(channelID, messageID);
            }

            long guildId = Long.parseLong(data.getString("guild_id", "0"));
            mentioned = isMentioned(data, guildId, channelID, messageID);

            if (mentioned) {
                setMentionCount(channelID, getMentionCount(channelID) + 1);
            }
        }
        catch (Exception e) {
        }

        return mentioned;
    }

    private boolean isMentioned(JSONObject data, long guildId, long channelID, long messageID) {
        try {
            Message message = new Message(data);

            if (message.mentionEveryone) return true;

            if (message.mentions != null &&
                message.mentions.containsKey(Long.toString(account.getUserID()))) {
                return true;
            }

            if (message.mentionRoles != null && message.mentionRoles.size() > 0 && guildId != 0) {
                Vector<Long> roles = myRoles.get(guildId);

                if (roles == null) {
                    fetchMyRoles(guildId, message.mentionRoles, channelID, messageID);
                    return false;
                }

                for (int i = 0; i < message.mentionRoles.size(); i++) {
                    if (roles.contains(message.mentionRoles.elementAt(i))) return true;
                }
            }
        }
        catch (Exception e) {
        }
        return false;
    }

    private void fetchMyRoles(final long guildId, final Vector<Long> roleMentions,
        final long channelID, final long messageID) {

        if (fetchingRoles.contains(guildId)) return;
        fetchingRoles.addElement(guildId);

        new Thread() {
            @Override
            public void run() {
                Vector<Long> roles = null;

                try {
                    JSONObject member =
                        (JSONObject) account.fetchJSON("/users/@me/guilds/" + guildId + "/member");
                    JSONArray roleArray = member.getArray("roles", null);

                    if (roleArray != null) {
                        roles = new Vector<Long>();
                        for (int i = 0; i < roleArray.size(); i++) {
                            roles.addElement(Long.parseLong(roleArray.getString(i)));
                        }
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

                fetchingRoles.removeElement(guildId);
                if (roles == null) return;

                myRoles.put(guildId, roles);

                for (int i = 0; i < roleMentions.size(); i++) {
                    if (!roles.contains(roleMentions.elementAt(i))) continue;

                    if (hasUnreads(channelID, messageID)) {
                        setMentionCount(channelID, getMentionCount(channelID) + 1);
                    }
                    break;
                }
            }
        }.start();
    }

    public void updateReadStatesFromReady(JSONObject readyData) {
        JSONArray entries = null;

        JSONObject readState = readyData.getObject("read_state", null);
        if (readState != null) {
            entries = readState.getArray("entries", null);
        }
        if (entries == null) {
            entries = readyData.getArray("read_state", null);
        }
        if (entries == null) return;

        autoSave = false;

        try {
            for (int i = 0; i < entries.size(); i++) {
                JSONObject entry = entries.getObject(i);

                // Non-channel read states use last_acked_id instead of last_message_id
                String lastMessageID = entry.getString("last_message_id", null);
                if (lastMessageID == null) continue;

                long channelID = Long.parseLong(entry.getString("id"));
                forceMarkRead(channelID, Long.parseLong(lastMessageID));

                if (entry.has("mention_count")) {
                    setMentionCount(channelID, entry.getInt("mention_count", 0));
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        autoSave = true;
        save();
    }

    public void updateReadState(JSONObject data) {
        try {
            long channelID = Long.parseLong(data.getString("channel_id"));
            long messageID = Long.parseLong(data.getString("message_id"));
            forceMarkRead(channelID, messageID);
            setMentionCount(channelID, data.getInt("mention_count", 0));
        }
        catch (Exception e) {
        }
    }

    public void updateChannelUnread(JSONObject data) {
        JSONArray updates = data.getArray("channel_unread_updates", null);

        if (updates == null) updates = data.getArray("updated_channels", null);
        if (updates == null) updates = data.getArray("channels", null);
        if (updates == null) return;

        for (int i = 0; i < updates.size(); i++) {
            try {
                JSONObject update = updates.getObject(i);
                JSONObject readState = update.getObject("read_state", null);
                JSONObject source = readState != null ? readState : update;

                long channelID = Long.parseLong(update.getString("id"));

                String lastMessageID = source.getString("last_message_id", null);
                if (lastMessageID != null) {
                    updateChannelLastMessageID(channelID, Long.parseLong(lastMessageID));
                }

                if (source.has("mention_count")) {
                    setMentionCount(channelID, source.getInt("mention_count", 0));
                }
            }
            catch (Exception e) {
            }
        }
    }

    // used for gateway read states
    public void forceMarkRead(long channelID, long lastMessageID) {
        long lastMessageTime = lastMessageID >> 22;

        Long lastReadTime = channels.get(channelID);
        boolean isUnread = true;

        if (lastReadTime != null) {
            isUnread = (lastReadTime.longValue() < lastMessageTime);
        }
        if (isUnread) {
            put(channelID, lastMessageTime);
        }
    }

    public void markDMsRead() {
        autoSave = false;
        if (account.dmChannelsAvailable()) {
            try {
                Vector dmChannels = account.getDmChannels(false);
                for (int i = 0; i < dmChannels.size(); i++) {
                    DMChannel dm = (DMChannel) dmChannels.elementAt(i);
                    markRead(dm);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        autoSave = true;
        save();
    }
}
