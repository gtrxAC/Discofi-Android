package fi.gtrxac.discord;

import fi.gtrxac.discord.model.*;

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

    private Vector<Guild> guilds;
    private Vector<DMChannel> dmChannels;
    private Vector<Message> inbox;

    public Account() {
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

    public String getDisplayTitle() {
        if (username != null) return username;
        return getUserID();
    }

    public String getUserID() {
        String tokenIDPart = token.split("\\.")[0];
        byte[] decodeBytes = Base64.decodeFast(tokenIDPart);
        return new String(decodeBytes);
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

    private JSONArray fetchGuildsOrDms(String route) throws Exception {
        return (JSONArray) fetchJSON(route);
    }

    /**
     * Perform an authenticated GET request and parse the response as JSON.
     */
    public AbstractJSON fetchJSON(String route) throws Exception {
        HTTP h = createApiRequest("GET", route);
        int responseCode = h.getResponseCode();
        String responseText = h.getResponseString();

        if (responseCode < 200 || responseCode >= 300) {
            throw new Exception(getApiErrorMessage(responseCode, responseText));
        }

        return JSON.get(responseText);
    }

    /**
     * Perform an authenticated POST request with a JSON body and parse the response as JSON.
     */
    public AbstractJSON postJSON(String route, JSONObject body) throws Exception {
        HTTP h = createApiRequest("POST", route);
        h.setData(body.build());
        int responseCode = h.getResponseCode();
        String responseText = h.getResponseString();

        if (responseCode < 200 || responseCode >= 300) {
            throw new Exception(getApiErrorMessage(responseCode, responseText));
        }

        return JSON.get(responseText);
    }

    public Vector<Guild> getGuilds(boolean refresh) throws Exception {
        if (!refresh && guildsAvailable()) return guilds;

        JSONArray guildsJson = fetchGuildsOrDms("/users/@me/guilds");

        guilds.clear();

        for (int i = 0; i < guildsJson.size(); i++) {
            JSONObject guildJson = guildsJson.getObject(i);
            guilds.add(new Guild(guildJson));
        }

        return guilds;
    }

    public Vector<DMChannel> getDmChannels(boolean refresh) throws Exception {
        if (!refresh && dmChannelsAvailable()) return dmChannels;

        JSONArray dmsJson = fetchGuildsOrDms("/users/@me/channels");
        Vector<DMChannel> unsorted = new Vector<>();

        for (int i = 0; i < dmsJson.size(); i++) {
            JSONObject dmJson = dmsJson.getObject(i);
            int type = dmJson.getInt("type", 1);
            if (type != 1 && type != 3) continue;
            unsorted.add(new DMChannel(dmJson));
        }

        dmChannels.clear();
        sortDMChannels(unsorted);

        return dmChannels;
    }

    /**
     * Sort DMs by recency, like official clients do (highest last message ID first).
     */
    private void sortDMChannels(Vector<DMChannel> unsorted) {
        while (unsorted.size() > 0) {
            long highestID = -1;
            int highestIndex = 0;

            for (int i = 0; i < unsorted.size(); i++) {
                DMChannel ch = unsorted.elementAt(i);
                if (ch.lastMessageID > highestID) {
                    highestID = ch.lastMessageID;
                    highestIndex = i;
                }
            }
            dmChannels.add(unsorted.remove(highestIndex));
        }
    }

    /**
     * Get the user's mentions (inbox). Returns a Vector of Messages.
     */
    public Vector<Message> getInbox(boolean refresh) throws Exception {
        if (!refresh && inboxAvailable()) return inbox;

        JSONArray inboxJson = (JSONArray) fetchJSON("/users/@me/mentions");
        inbox.clear();

        for (int i = 0; i < inboxJson.size(); i++) {
            JSONObject mentionJson = inboxJson.getObject(i);
            inbox.add(new Message(mentionJson));
        }

        return inbox;
    }

    /**
     * Get a guild's channels grouped into sorted categories.
     * Returns a Vector of ChannelCategory.
     */
    public Vector getGuildChannels(String guildId) throws Exception {
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
    public Vector getMessages(String channelId, String before, String after, int limit) throws Exception {
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
    public Message sendMessage(String channelId, String content) throws Exception {
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
    public void fetchUserProfile() throws Exception {
        JSONObject me = (JSONObject) fetchJSON("/users/@me");

        userId = me.getString("id", null);
        String globalName = me.getString("global_name", null);
        username = globalName != null ? globalName : me.getString("username", null);
    }
}
