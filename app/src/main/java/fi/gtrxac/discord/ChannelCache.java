package fi.gtrxac.discord;

import android.util.Log;

import java.util.Enumeration;
import java.util.Vector;

import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;
import fi.gtrxac.discord.model.Channel;
import fi.gtrxac.discord.model.ChannelCategory;
import fi.gtrxac.discord.model.Permissions;
import fi.gtrxac.discord.model.Role;
import fi.gtrxac.discord.util.LRUCache;
import fi.gtrxac.discord.util.LRUCacheEntry;

/**
 * Maintains an LRU cache of up to CACHE_SIZE mappings of guild ID to
 * the guild's channel list (with category structure).
 */
public class ChannelCache extends LRUCache {
    private static final String TAG = "ChannelCache";
    private static final int CACHE_SIZE = 100;

    public ChannelCache() {
        super(CACHE_SIZE);
    }

    public void fromJSON(JSONArray data) {
        cache.clear();

        Log.d(TAG, "Loading channel cache with " + data.size() + " guild entries");

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
                    channel.setName(channelData.getString(2, null));
                    channel.lastMessageID = channelData.getLong(3, 0);
                    channel.myPermissions = channelData.getLong(4, 0);

                    if (channelData.size() > 5) {
                        channel.overwrites = new Vector();

                        for (int l = 5; l + 3 < channelData.size(); l += 4) {
                            Permissions.Overwrite overwrite = new Permissions.Overwrite();
                            overwrite.id = channelData.getLong(l);
                            overwrite.allow = channelData.getLong(l + 1);
                            overwrite.deny = channelData.getLong(l + 2);
                            overwrite.isMember = channelData.getInt(l + 3) == 1;
                            channel.overwrites.addElement(overwrite);
                        }
                    }

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
                    if (!channel.canView()) continue;

                    JSONArray channelData = new JSONArray();
                    channelData.add(channel.id);
                    channelData.add(channel.type);
                    channelData.add(channel.getRawName());
                    channelData.add(channel.lastMessageID);
                    channelData.add(channel.myPermissions);
                    if (channel.overwrites != null) {
                        for (int k = 0; k < channel.overwrites.size(); k++) {
                            Permissions.Overwrite overwrite =
                                (Permissions.Overwrite) channel.overwrites.elementAt(k);

                            channelData.add(overwrite.id);
                            channelData.add(overwrite.allow);
                            channelData.add(overwrite.deny);
                            channelData.add(overwrite.isMember ? 1 : 0);
                        }
                    }

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

    public void onGatewayReady(JSONObject data, long userId) {
        if (data == null || !data.has("guilds")) return;

        JSONArray guilds = data.getArray("guilds");
        JSONArray mergedMembers = data.getArray("merged_members", null);
        int updated = 0;

        for (int i = 0; i < guilds.size(); i++) {
            JSONObject guildJSON = guilds.getObject(i);
            long guildID = Long.parseLong(guildJSON.getString("id"));
            if (!guildJSON.has("channels")) continue;

            Vector channels = Channel.parseChannels(guildJSON.getArray("channels"));

            try {
                JSONObject myMember = findMyMember(mergedMembers, i,
                    guildJSON.getArray("members", null), userId);
                Log.d(TAG, "READY guild " + guildID + ": " +
                    (myMember == null ? "no member data" : "found member data"));

                if (myMember != null) {
                    JSONArray roleArray = myMember.getArray("roles", null);
                    if (roleArray != null) {
                        Vector myRoles = Role.parseRoleIDs(roleArray);
                        long basePermissions = computeBasePermissions(
                            guildJSON.getArray("roles", null), guildID, myRoles);

                        for (int j = 0; j < channels.size(); j++) {
                            ChannelCategory category = (ChannelCategory) channels.elementAt(j);
                            for (int k = 0; k < category.children.size(); k++) {
                                Channel channel = (Channel) category.children.elementAt(k);
                                channel.myPermissions = channel.computePermissions(
                                    guildID, basePermissions, myRoles, userId);
                            }
                        }
                    }
                }
            }
            catch (Exception e) {
                Log.w(TAG, "Failed to calculate permissions for guild " + guildID, e);
            }

            set(guildID, channels);
            updated++;
        }

        Log.d(TAG, "Updated channel cache for " + updated + " of " + guilds.size() +
            " guilds from READY");
    }

    private static JSONObject findMyMember(JSONArray mergedMembers, int guildIndex,
        JSONArray guildMembers, long userId) {
        if (mergedMembers != null && guildIndex < mergedMembers.size()) {
            JSONArray members = mergedMembers.getArray(guildIndex, null);
            if (members != null) {
                JSONObject member = findMemberById(members, userId);
                if (member == null && members.size() == 1) {
                    member = members.getObject(0);
                }
                if (member != null) return member;
            }
        }
        if (guildMembers != null) return findMemberById(guildMembers, userId);
        return null;
    }

    private static JSONObject findMemberById(JSONArray members, long userId) {
        for (int i = 0; i < members.size(); i++) {
            JSONObject member = members.getObject(i);
            JSONObject user = member.getObject("user", null);
            String idStr = user != null ? user.getString("id", null)
                : member.getString("user_id", null);
            if (idStr != null && Long.parseLong(idStr) == userId) return member;
        }
        return null;
    }

    private static long computeBasePermissions(JSONArray rolesJson, long guildId, Vector myRoles) {
        if (rolesJson == null) return 0;

        long permissions = 0;

        // @everyone has the same ID as the guild
        for (int i = 0; i < rolesJson.size(); i++) {
            JSONObject role = rolesJson.getObject(i);
            if (Long.parseLong(role.getString("id")) == guildId) {
                permissions = Long.parseLong(role.getString("permissions", "0"));
                break;
            }
        }

        for (int i = 0; i < rolesJson.size(); i++) {
            JSONObject role = rolesJson.getObject(i);
            long roleId = Long.parseLong(role.getString("id"));
            if (roleId == guildId) continue;

            for (int j = 0; j < myRoles.size(); j++) {
                if (((Role) myRoles.elementAt(j)).id == roleId) {
                    permissions |= Long.parseLong(role.getString("permissions", "0"));
                    break;
                }
            }
        }

        return permissions;
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
                        Log.d(TAG, "Channel " + channelID + " last message updated to " +
                            lastMessageID);
                        channel.lastMessageID = lastMessageID;
                        return;
                    }
                }
            }
        }

        Log.d(TAG, "Channel " + channelID + " not found in cache while updating last message");
    }

    /**
     * Find a channel by its ID in all cached guilds, first searching the primary guild if
     * specified.
     */
    public Channel getChannelByID(long primaryGuildID, long channelID) {
        if (channelID == 0) return null;

        if (primaryGuildID != 0) {
            Channel result = getChannelByIDInGuild(primaryGuildID, channelID);
            if (result != null) return result;
        }

        for (Enumeration e = cache.keys(); e.hasMoreElements(); ) {
            long guildID = (long) e.nextElement();

            if (primaryGuildID != 0 && guildID == primaryGuildID) continue;

            Channel result = getChannelByIDInGuild(guildID, channelID);
            if (result != null) return result;
        }

        return null;
    }

    /**
     * Find a channel by ID in the specified guild.
     */
    public Channel getChannelByIDInGuild(long guildID, long channelID) {
        if (guildID == 0 || channelID == 0) return null;

        Vector categories = get(guildID);
        if (categories == null) {
            Log.d(TAG, "Guild " + guildID + " not in channel cache while looking up channel " +
                channelID);
            return null;
        }

        for (int i = 0; i < categories.size(); i++) {
            ChannelCategory category = (ChannelCategory) categories.elementAt(i);

            for (int j = 0; j < category.children.size(); j++) {
                Channel channel = (Channel) category.children.elementAt(j);

                if (channel.id == channelID) return channel;
            }
        }

        return null;
    }

    /**
     * Find the guild the specified channel is from.
     */
    public long getGuildIDByChannel(long channelID) {
        if (channelID == 0) return 0;

        for (Enumeration e = cache.keys(); e.hasMoreElements(); ) {
            long guildID = (long) e.nextElement();
            Channel result = getChannelByIDInGuild(guildID, channelID);
            if (result != null) return guildID;
        }

        return 0;
    }

    public long getGuildIDByChannel(Channel channel) {
        if (channel == null) return 0;
        return getGuildIDByChannel(channel.id);
    }
}
