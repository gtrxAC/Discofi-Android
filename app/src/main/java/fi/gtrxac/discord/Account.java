package fi.gtrxac.discord;

import fi.gtrxac.discord.model.*;

import java.util.Hashtable;
import java.util.Vector;

import cc.nnproject.json.AbstractJSON;
import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONException;
import cc.nnproject.json.JSONObject;
import fi.gtrxac.bluewap.http.HTTP;
import util.Base64;
/**
 * Created by gtrx on 29.8.2026.
 */

public class Account {
    public String token;
    public String username;
    public String userId;

    public boolean useProxy;
    public boolean useGateway = true;
    public String apiUrl;
    public String gatewayUrl;
    public String cdnUrl;

    public String email;
    public String password;

    private Vector<Guild> guildsUnsorted;
    private Vector<Guild> guilds;
    private Vector<DMChannel> dmChannels;
    private Vector<Message> inbox;

    public Account() {
        guildsUnsorted = new Vector<>();
        guilds = new Vector<>();
        dmChannels = new Vector<>();
        inbox = new Vector<>();
    }

    public Account(JSONArray data) {
        this();
        token = data.getString(0);
        username = data.getString(1, "");
        useProxy = data.getBoolean(2);
        useGateway = data.getBoolean(3);
        apiUrl = data.getString(4, "");
        gatewayUrl = data.getString(5, "");
        cdnUrl = data.getString(6, "");
        email = data.getString(9, "");
        password = data.getString(10, "");

        JSONArray guildsJson = data.getArray(7, new JSONArray());

        for (int i = 0; i < guildsJson.size(); i++) {
            JSONArray guildData = guildsJson.getArray(i);
            guilds.add(new Guild(guildData));
        }

        JSONArray dmChannelsJson = data.getArray(8, new JSONArray());

        for (int i = 0; i < dmChannelsJson.size(); i++) {
            JSONArray dmChannelData = dmChannelsJson.getArray(i);
            dmChannels.add(new DMChannel(dmChannelData));
        }
    }

    public JSONArray toJSON() {
        JSONArray result = new JSONArray();
        result.add(token);
        result.add(username);
        result.add(useProxy);
        result.add(useGateway);
        result.add(apiUrl);
        result.add(gatewayUrl);
        result.add(cdnUrl);

        JSONArray guildsJson = new JSONArray();

        for (int i = 0; i < guilds.size(); i++) {
            Guild g = guilds.elementAt(i);
            guildsJson.add(g.toJSON());
        }

        result.add(guildsJson);

        JSONArray dmChannelsJson = new JSONArray();

        for (int i = 0; i < dmChannels.size(); i++) {
            DMChannel d = dmChannels.elementAt(i);
            dmChannelsJson.add(d.toJSON());
        }

        result.add(dmChannelsJson);

        result.add(email);
        result.add(password);

        return result;
    }

    public String getDisplayTitle() throws DiscordException {
        if (username != null) return username;
        return getUserID();
    }

    public String getUserID() throws DiscordException {
        return DiscordUtil.getUserIDFromToken(token);
    }

    public String getApiUrl() {
        return useProxy ? apiUrl : "https://discord.com";
    }

    public String getCdnUrl() {
        return useProxy ? cdnUrl : "https://cdn.discordapp.com";
    }

    /**
     * Rewrite a Discord CDN/media URL (attachment preview) to the account's
     * configured CDN base when using a proxy, mirroring how avatars are
     * resolved through getCdnUrl(). Discord hosts are unreachable directly
     * in that setup; the proxy CDN serves the same paths.
     */
    public String getCdnAssetUrl(String url) {
        if (url == null) return null;
        if (useProxy && cdnUrl != null && cdnUrl.length() > 0) {
            String[] hosts = {
                "https://media.discordapp.net",
                "http://media.discordapp.net",
                "https://cdn.discordapp.com",
                "http://cdn.discordapp.com",
            };
            for (int i = 0; i < hosts.length; i++) {
                if (url.startsWith(hosts[i])) return cdnUrl + url.substring(hosts[i].length());
            }
        }
        return url;
    }

    public HTTP createApiRequest(String method, String route) throws Exception {
        HTTP result = HTTP.createRequest(method, getApiUrl() + "/api/v9" + route);
        if (token != null && token.length() > 0) {
            result.setHeader("Authorization", token);
        }
        result.setHeader("X-Super-Properties", SuperProperties.get());
        result.setHeader("X-Discord-Locale", "en-US");
        return result;
    }

    public String getApiErrorMessage(int responseCode, String responseText) {
        try {
            JSONObject json = JSON.getObject(responseText);
            return json.getString("message");
        }
        catch (JSONException e) {}

        return "HTTP error " + responseCode;
    }

    public boolean guildsAvailable() {
        return guilds.size() != 0;
    }

    public boolean dmChannelsAvailable() {
        return dmChannels.size() != 0;
    }

    public boolean inboxAvailable() {
        return inbox.size() != 0;
    }

    /**
     * Perform an authenticated GET request and parse the response as JSON.
     */
    public AbstractJSON fetchJSON(String route, Hashtable<String, String> headers) throws DiscordException {
        int responseCode;
        String responseText;

        try {
            HTTP h = createApiRequest("GET", route);
            if (headers != null) h.setHeader(headers);
            responseCode = h.getResponseCode();
            responseText = h.getResponseString();
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new DiscordException(e);
        }

        return handleJSONResponse(responseCode, responseText);
    }

    public AbstractJSON fetchJSON(String route) throws DiscordException {
        return fetchJSON(route, null);
    }

    /**
     * Perform an authenticated POST request with a JSON body and parse the response as JSON.
     */
    public AbstractJSON postJSON(String route, JSONObject body, Hashtable<String, String> headers) throws DiscordException {
        int responseCode;
        String responseText;

        try {
            HTTP h = createApiRequest("POST", route);
            if (headers != null) h.setHeader(headers);
            h.setData(body.build());
            responseCode = h.getResponseCode();
            responseText = h.getResponseString();
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new DiscordException(e);
        }

        return handleJSONResponse(responseCode, responseText);
    }

    public AbstractJSON postJSON(String route, JSONObject body) throws DiscordException {
        return postJSON(route, body, null);
    }

    private AbstractJSON handleJSONResponse(int responseCode, String responseText) throws DiscordException {
        if (responseCode < 200 || responseCode >= 300) {
            throw new DiscordException(getApiErrorMessage(responseCode, responseText));
        }

        return JSON.get(responseText);
    }

    /**
     * Get user's guild (server) list. Returns vector of Guilds.
     * As the list is sorted by user-defined order, this makes up to two HTTP requests; the second one is for the user's settings.
     */
    public Vector getGuilds(boolean refresh) throws DiscordException {
        if (!refresh && guildsAvailable()) return guilds;

        if (refresh || guildsUnsorted.size() == 0) {
            JSONArray guildsJson = (JSONArray) fetchJSON("/users/@me/guilds");
            guildsUnsorted.setSize(0);

            for (int i = 0; i < guildsJson.size(); i++) {
                JSONObject guildJson = guildsJson.getObject(i);
                guildsUnsorted.addElement(new Guild(guildJson));
            }
        }

        // fetch user's guild folders which we can use to sort guilds by
        // user-defined order like in official clients
        JSONObject userSettings = (JSONObject) fetchJSON("/users/@me/settings");
        JSONArray guildFolders = userSettings.getArray("guild_folders");

        guilds.setSize(0);

        for (int i = 0; i < guildFolders.size(); i++) {
            JSONArray guildIds = guildFolders.getObject(i).getArray("guild_ids");

            for (int j = 0; j < guildIds.size(); j++) {
                String guildId = guildIds.getString(j);

                for (int k = 0; k < guildsUnsorted.size(); k++) {
                    Guild guild = (Guild) guildsUnsorted.elementAt(k);

                    if (guild.id.equals(guildId)) {
                        guilds.addElement(guild);
                        guildsUnsorted.removeElementAt(k);
                        break;
                    }
                }
            }
        }

        // If the user doesn't have guild folders, some or all of the guilds may have been
        // missed by the above sort, so add them at the end of the list
        for (int i = 0; i < guildsUnsorted.size(); i++) {
            guilds.addElement(guildsUnsorted.elementAt(i));
        }
        guildsUnsorted.setSize(0);

        return guilds;
    }

    public Vector getDmChannels(boolean refresh) throws DiscordException {
        if (!refresh && dmChannelsAvailable()) return dmChannels;

        JSONArray dmChannelsJson = (JSONArray) fetchJSON("/users/@me/channels");
        Vector dmChannelsUnsorted = new Vector();

        for (int i = 0; i < dmChannelsJson.size(); i++) {
            JSONObject dmChannelJson = dmChannelsJson.getObject(i);
            dmChannelsUnsorted.addElement(new DMChannel(dmChannelJson));
        }

        dmChannels.setSize(0);
        sortDMChannels(dmChannelsUnsorted);
        return dmChannels;
    }

    private void sortDMChannels(Vector dmChannelsUnsorted) {
        // Get the 20 latest DMs (add into another vector sorted by highest to lowest last message ID)
        int count = Math.min(20, dmChannelsUnsorted.size());

        for (int i = 0; i < count; i++) {
            long highestID = 0;
            int highestIndex = 0;

            for (int u = 0; u < dmChannelsUnsorted.size(); u++) {
                DMChannel ch = (DMChannel) dmChannelsUnsorted.elementAt(u);

                if (ch.lastMessageID <= highestID) continue;

                // Don't repeat ones that we've already added
                boolean alreadyHave = false;

                for (int l = 0; l < dmChannels.size(); l++) {
                    DMChannel compare = (DMChannel) dmChannels.elementAt(l);

                    if (compare.lastMessageID == ch.lastMessageID) {
                        alreadyHave = true;
                        break;
                    }
                }
                if (alreadyHave) continue;

                highestID = ch.lastMessageID;
                highestIndex = u;
            }
            dmChannels.addElement((DMChannel) dmChannelsUnsorted.elementAt(highestIndex));
        }
    }

    public Vector getInbox(boolean refresh) throws DiscordException {
        if (!refresh && inboxAvailable()) return inbox;

        JSONArray inboxJson = (JSONArray) fetchJSON("/users/@me/mentions");

        for (int i = 0; i < inboxJson.size(); i++) {
            JSONObject mentionJson = inboxJson.getObject(i);
            inbox.addElement(new Message(mentionJson));
        }

        return inbox;
    }

    /**
     * Get a guild's channels grouped into sorted categories.
     * Returns a Vector of ChannelCategory.
     */
    public Vector getGuildChannels(String guildId) throws DiscordException {
        JSONArray channelsJson = (JSONArray) fetchJSON("/guilds/" + guildId + "/channels");
        return parseGuildChannels(channelsJson);
    }

    /**
     * Parse fetched channel list into sorted categories and channels.
     */
    private Vector parseGuildChannels(JSONArray channelsJson) {
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

    private void groupChannelsIntoCategories(Vector channels, Vector categories) {
        for (int i = 0; i < channels.size(); i++) {
            JSONObject channelJson = (JSONObject) channels.elementAt(i);
            String parentID = channelJson.getString("parent_id", null);

            // Fallback to default (topmost) category
            ChannelCategory parentCategory = (ChannelCategory) categories.elementAt(0);

            // Find the parent category
            for (int j = 1; j < categories.size(); j++) {
                ChannelCategory category = (ChannelCategory) categories.elementAt(j);

                if (category.id != null && category.id.equals(parentID)) {
                    parentCategory = category;
                    break;
                }
            }

            Channel channel = new Channel(channelJson);
            parentCategory.children.addElement(channel);
        }
    }

    /**
     * Fetch messages from a channel. The returned Vector is ordered oldest
     * first, regardless of the order the API returned them in.
     */
    public Vector getMessages(String channelId, String before, String after, int limit) throws DiscordException {
        StringBuffer url = new StringBuffer("/channels/").append(channelId)
                .append("/messages?limit=").append(limit);

        if (useProxy) url.append("&droidcord=1");
        if (before != null) {
            url.append("&before=").append(before);
        } else if (after != null) {
            url.append("&after=").append(after);
        }

        JSONArray messagesJson = (JSONArray) fetchJSON(url.toString());
        Vector result = new Vector();

        // The API returns messages newest first; insert at the front to get
        // oldest-first order (works for both initial load and prepending).
        for (int i = 0; i < messagesJson.size(); i++) {
            result.insertElementAt(new Message(messagesJson.getObject(i)), 0);
        }

        return result;
    }

    /**
     * Send a message to a channel. Returns the created message.
     */
    public Message sendMessage(String channelId, String content) throws DiscordException {
        JSONObject body = new JSONObject();
        body.put("content", content);
        body.put("flags", 0);
        body.put("mobile_network_type", "unknown");
        body.put("tts", false);

        JSONObject created = (JSONObject) postJSON("/channels/" + channelId + "/messages", body);
        return new Message(created);
    }

    /**
     * Fetch the logged in user's profile and cache the username on this account.
     */
    public void fetchUserProfile() throws DiscordException {
        JSONObject me = (JSONObject) fetchJSON("/users/@me");

        userId = me.getString("id", null);
        String globalName = me.getString("global_name", null);
        username = globalName != null ? globalName : me.getString("username", null);
    }
}
