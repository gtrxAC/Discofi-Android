package fi.gtrxac.discord;

import java.util.Enumeration;
import java.util.Vector;

import cc.nnproject.json.JSONArray;
import fi.gtrxac.discord.model.Channel;
import fi.gtrxac.discord.model.ChannelCategory;
import fi.gtrxac.discord.util.LRUCache;
import fi.gtrxac.discord.util.LRUCacheEntry;

/**
 * Maintains an LRU cache of up to CACHE_SIZE mappings of guild ID to
 * the guild's channel list (with category structure).
 */
public class ChannelCache extends LRUCache {
    private static final int CACHE_SIZE = 100;

    public ChannelCache() {
        super(CACHE_SIZE);
    }

    public void fromJSON(JSONArray data) {
        cache.clear();

        for (int i = 0; i < data.size(); i++) {
            JSONArray entryData = data.getArray(i);

            long guildID = entryData.getLong(0);
            long lastUsed = entryData.getLong(1);

            JSONArray categoriesData = entryData.getArray(2);
            Vector categories = new Vector(categoriesData.size());

            for (int j = 0; j < categoriesData.size(); j++) {
                JSONArray categoryData = categoriesData.getArray(j);

                ChannelCategory category = new ChannelCategory(categoryData.getString(1, null));
                category.id = categoryData.getLong(0);

                JSONArray childrenData = categoryData.getArray(2);
                category.children.ensureCapacity(childrenData.size());

                for (int k = 0; k < childrenData.size(); k++) {
                    JSONArray channelData = childrenData.getArray(k);

                    Channel channel = new Channel();
                    channel.id = channelData.getLong(0);
                    channel.type = channelData.getInt(1);
                    channel.name = channelData.getString(2, null);
                    channel.lastMessageID = channelData.getLong(3, 0);

                    category.children.addElement(channel);
                }

                categories.addElement(category);
            }

            LRUCacheEntry entry = new LRUCacheEntry(categories, lastUsed);
            setEntry(guildID, entry);
        }
    }

    public JSONArray toJSON() {
        JSONArray result = new JSONArray();

        for (Enumeration e = cache.keys(); e.hasMoreElements(); ) {
            long guildID = (long) e.nextElement();
            LRUCacheEntry entry = (LRUCacheEntry) cache.get(guildID);
            Vector categories = (Vector) entry.getObjectWithoutUpdate();

            JSONArray entryData = new JSONArray();
            entryData.add(guildID);

            // subtract current usage counter so the saved last-used values are
            // relative to the 0 usage counter of the next session
            entryData.add(entry.getLastUsed() - LRUCacheEntry.getUsageCounter());

            JSONArray categoriesData = new JSONArray();

            for (int i = 0; i < categories.size(); i++) {
                ChannelCategory category = (ChannelCategory) categories.elementAt(i);

                JSONArray categoryData = new JSONArray();
                categoryData.add(category.id);
                categoryData.add(category.name);

                JSONArray childrenData = new JSONArray();

                for (int j = 0; j < category.children.size(); j++) {
                    Channel channel = (Channel) category.children.elementAt(j);

                    JSONArray channelData = new JSONArray();
                    channelData.add(channel.id);
                    channelData.add(channel.type);
                    channelData.add(channel.name);
                    channelData.add(channel.lastMessageID);

                    childrenData.add(channelData);
                }

                categoryData.add(childrenData);
                categoriesData.add(categoryData);
            }

            entryData.add(categoriesData);
            result.add(entryData);
        }

        return result;
    }

    public Vector get(long guildID) {
        return (Vector) super.get(guildID);
    }

    public void setLastMessageID(long channelID, long lastMessageID) {
        for (Enumeration e = cache.keys(); e.hasMoreElements(); ) {
            long guildID = (long) e.nextElement();
            LRUCacheEntry entry = (LRUCacheEntry) cache.get(guildID);
            Vector categories = (Vector) entry.getObjectWithoutUpdate();

            for (int i = 0; i < categories.size(); i++) {
                ChannelCategory category = (ChannelCategory) categories.elementAt(i);

                for (int j = 0; j < category.children.size(); j++) {
                    Channel channel = (Channel) category.children.elementAt(j);

                    if (channel.id == channelID && lastMessageID > channel.lastMessageID) {
                        channel.lastMessageID = lastMessageID;
                        return;
                    }
                }
            }
        }
    }
}
