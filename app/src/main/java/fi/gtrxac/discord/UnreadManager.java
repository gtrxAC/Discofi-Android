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

public class UnreadManager {
    private final Account account;
    private final Hashtable<Long, Long> channels = new Hashtable<>();
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

        for (int i = 0; i < data.size(); i++) {
            JSONArray entry = data.getArray(i);
            channels.put(entry.getLong(0), entry.getLong(1));
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
        if (isUnread) {
            put(channelID, lastMessageTime);
        }

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

    public void onMessageCreate(JSONObject data) {
        long channelID = Long.parseLong(data.getString("channel_id"));
        long messageID = Long.parseLong(data.getString("id"));

        updateChannelLastMessageID(channelID, messageID);

        try {
            JSONObject author = data.getObject("author", null);

            if (author != null && account.userId != 0 &&
                author.getString("id").equals(Long.toString(account.getUserID()))) {
                forceMarkRead(channelID, messageID);
            }
        }
        catch (Exception e) {
        }
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
                String lastMessageID = update.getString("last_message_id", null);
                if (lastMessageID == null) continue;

                long channelID = Long.parseLong(update.getString("id"));
                updateChannelLastMessageID(channelID, Long.parseLong(lastMessageID));
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
        autoSave = true;
        save();
    }
}
