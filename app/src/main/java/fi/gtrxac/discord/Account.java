package fi.gtrxac.discord;

import java.util.Hashtable;
import java.util.Vector;

import cc.nnproject.json.AbstractJSON;
import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONException;
import cc.nnproject.json.JSONObject;
import fi.gtrxac.bluewap.http.HTTP;
import fi.gtrxac.bluewap.http.StandardHTTP;
import fi.gtrxac.discord.model.Channel;
import fi.gtrxac.discord.model.DMChannel;
import fi.gtrxac.discord.model.Guild;
import fi.gtrxac.discord.model.GuildMember;
import fi.gtrxac.discord.model.Message;
import fi.gtrxac.discord.model.Role;
import util.Base64;

/**
 * Created by gtrx on 29.8.2026.
 */

public class Account {
    public static final String DEFAULT_CDN_PROXY_URL = "http://cdn.gtrxac.fi";
    public final ChannelCache channelCache = new ChannelCache();
    public final GuildMemberCache guildMemberCache = new GuildMemberCache(this);
    public final UnreadManager unreadManager = new UnreadManager(this);
    private final Vector<Guild> guildsUnsorted = new Vector();
    private final Vector<Guild> guilds = new Vector();
    private final Vector<DMChannel> dmChannels = new Vector();
    private final Vector<Message> inbox = new Vector();
    public String token;
    public String username;
    public long userId;
    public boolean useProxy;
    public boolean useGateway = true;
    public String apiUrl;
    public String gatewayUrl;
    public String cdnUrl;
    public String email;
    public String password;
    public GatewayThread gateway;
    public Vector<Long> subscribedGuilds;

    public Account() {
        cdnUrl = DEFAULT_CDN_PROXY_URL;
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

        if (cdnUrl == null || cdnUrl.trim().length() == 0) {
            cdnUrl = DEFAULT_CDN_PROXY_URL;
        }

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

        channelCache.fromJSON(data.getArray(11, new JSONArray()));
        unreadManager.fromJSON(data.getArray(12, new JSONArray()));
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

        result.add(channelCache.toJSON());
        result.add(unreadManager.toJSON());

        return result;
    }

    public String getDisplayTitle() throws DiscordException {
        if (username != null) return username;
        return Long.toString(getUserID());
    }

    public long getUserID() throws DiscordException {
        return DiscordUtil.getUserIDFromToken(token);
    }

    public String getApiUrl() {
        return useProxy ? apiUrl : "https://discord.com";
    }

    public String getCdnUrl() {
        return Settings.useDirectCdn ? "https://cdn.discordapp.com" : cdnUrl;
    }

    public String getCdnDownloadUrl(String url) {
        return rewriteCdnUrl(url, getCdnUrl());
    }

    public String getCdnAssetUrl(String url) {
        // TODO: configurable media proxy in addition to CDN proxy, like in Discofi J2ME
        return rewriteCdnUrl(url, "http://media.discordapp.net");
    }

    private String rewriteCdnUrl(String url, String newBase) {
        if (url == null) return null;

        String[] hosts = {"https://media.discordapp.net", "http://media.discordapp.net",
            "https://cdn.discordapp.com", "http://cdn.discordapp.com",};

        for (int i = 0; i < hosts.length; i++) {
            if (url.startsWith(hosts[i])) {
                return newBase + url.substring(hosts[i].length());
            }
        }
        return url;
    }

    public HTTP createApiRequest(String method, String route) throws Exception {
        HTTP result = HTTP.createRequest(method, getApiUrl() + "/api/v9" + route);
        if (token != null && token.length() > 0) {
            result.setHeader("Authorization", token);
        }
        byte[] propsBytes = SuperProperties.get().build().getBytes();
        result.setHeader("X-Super-Properties", Base64.encodeToString(propsBytes, false));
        result.setHeader("X-Discord-Locale", "en-US");
        return result;
    }

    public String getApiErrorMessage(int responseCode, String responseText) {
        try {
            JSONObject json = JSON.getObject(responseText);
            return json.getString("message");
        }
        catch (JSONException e) {
        }

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
    public AbstractJSON fetchJSON(String route, Hashtable<String, String> headers)
        throws DiscordException {
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
    public AbstractJSON postJSON(String route, JSONObject body, Hashtable<String, String> headers)
        throws DiscordException {
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

    /**
     * Throw exception if API response is an error, otherwise do nothing
     */
    public void checkApiResponse(int responseCode, String responseText) throws DiscordException {
        if (responseText == null) responseText = "";

        if (responseCode == 400 && responseText.indexOf("captcha") != -1) {
            throw new DiscordException("A CAPTCHA was received");
        }
        if (responseCode < 200 || responseCode >= 300) {
            throw new DiscordException(getApiErrorMessage(responseCode, responseText));
        }
    }

    private AbstractJSON handleJSONResponse(int responseCode, String responseText)
        throws DiscordException {
        checkApiResponse(responseCode, responseText);
        return JSON.get(responseText);
    }

    /**
     * Get user's guild (server) list. Returns vector of Guilds.
     * As the list is sorted by user-defined order, this makes up to two HTTP requests; the
     * second one is for the user's settings.
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
                long guildId = Long.parseLong(guildIds.getString(j));

                for (int k = 0; k < guildsUnsorted.size(); k++) {
                    Guild guild = (Guild) guildsUnsorted.elementAt(k);

                    if (guild.id == guildId) {
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
        for (int i = 0; i < dmChannelsUnsorted.size(); i++) {
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

        JSONArray inboxJson = (JSONArray) fetchJSON(
            "/users/@me/mentions?limit=25&roles=true&everyone=true");

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
    public Vector getGuildChannels(long guildId) throws DiscordException {
        JSONArray channelsJson = (JSONArray) fetchJSON("/guilds/" + guildId + "/channels");
        return Channel.parseChannels(channelsJson);
    }

    /**
     * Get a guild's roles. Returns a Vector of Role.
     */
    public Vector getGuildRoles(long guildId) throws DiscordException {
        JSONArray rolesJson = (JSONArray) fetchJSON("/guilds/" + guildId + "/roles");
        return Role.parseRoles(rolesJson);
    }

    /**
     * Fetch guild-specific information of a user. Returns a GuildMember.
     */
    public GuildMember getGuildMember(long userId, long guildId, Vector guildRoles)
        throws DiscordException {
        JSONObject guildMemberJson =
            (JSONObject) fetchJSON("/guilds/" + guildId + "/members/" + userId);
        return new GuildMember(guildMemberJson, guildRoles);
    }

    public GuildMember getGuildMember(long userId, Guild guild) throws DiscordException {
        Vector roles = guild.roles;
        if (roles == null) {
            roles = getGuildRoles(guild.id);
        }
        return getGuildMember(userId, guild.id, roles);
    }

    /**
     * Fetch messages from a channel. The returned Vector is ordered oldest
     * first, regardless of the order the API returned them in.
     */
    public Vector getMessages(long channelId, long before, long after, int limit)
        throws DiscordException {
        StringBuffer url =
            new StringBuffer("/channels/").append(channelId).append("/messages?limit=")
                .append(limit);

        if (useProxy) url.append("&droidcord=1");
        if (before != 0) {
            url.append("&before=").append(before);
        }
        else if (after != 0) {
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
    public Message sendMessage(long channelId, String content, Message recipientMessage,
        boolean mention) throws DiscordException {
        JSONObject body = new JSONObject();
        body.put("content", content);
        body.put("flags", 0);
        body.put("mobile_network_type", "unknown");
        body.put("tts", false);

        // Reply
        if (recipientMessage != null) {
            JSONObject ref = new JSONObject();

            ref.put("message_id", Long.toString(recipientMessage.id));
            body.put("message_reference", ref);

            if (!mention) {
                JSONObject ping = new JSONObject();
                ping.put("replied_user", false);
                body.put("allowed_mentions", ping);
            }
        }

        JSONObject created = (JSONObject) postJSON("/channels/" + channelId + "/messages", body);
        return new Message(created);
    }

    public void deleteMessage(Message message) throws DiscordException {
        try {
            HTTP h = createApiRequest("DELETE",
                "/channels/" + message.channelId + "/messages/" + message.id);

            int responseCode = h.getResponseCode();
            String responseText = h.getResponseString();
            checkApiResponse(responseCode, responseText);
        }
        catch (Exception e) {
            if (e == StandardHTTP.requestMethodException) {
                Vector tips = new Vector();
                tips.addElement("use a different connection mode");

                throw new DiscordException("Deleting messages is not supported",
                    "Your device does not support the HTTP DELETE method", tips);
            }
            throw new DiscordException(e);
        }
    }

    public Message editMessage(Message message, String content) throws DiscordException {
        try {
            HTTP h = createApiRequest("PATCH",
                "/channels/" + message.channelId + "/messages/" + message.id);

            JSONObject editData = new JSONObject();
            editData.put("content", content);

            h.setData(editData.build());

            int responseCode = h.getResponseCode();
            String responseText = h.getResponseString();
            return new Message((JSONObject) handleJSONResponse(responseCode, responseText));
        }
        catch (Exception e) {
            if (e == StandardHTTP.requestMethodException) {
                Vector tips = new Vector();
                tips.addElement("use a different connection mode");

                throw new DiscordException("Deleting messages is not supported",
                    "Your device does not support the HTTP DELETE method", tips);
            }
            throw new DiscordException(e);
        }
    }

    public void startGateway() {
        gateway = new GatewayThread(this);
        gateway.start();
    }

    public boolean gatewayActive() {
        return gateway != null && gateway.isAlive();
    }

    /**
     * Tell the API that the user is typing in a channel. Discord clears the
     * indicator automatically after about ten seconds.
     */
    public void sendTyping(long channelId) {
        try {
            HTTP h = createApiRequest("POST", "/channels/" + channelId + "/typing");
            h.setData("");
            h.getResponseCode();
            h.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Tell Discord that a channel has been read up to the given message.
     */
    public void ackMessage(long channelId, long messageId) {
        try {
            HTTP h = createApiRequest("POST",
                "/channels/" + channelId + "/messages/" + messageId + "/ack");

            JSONObject body = new JSONObject();
            body.put("manual", false);
            h.setData(body.build());

            h.getResponseCode();
            h.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Fetch the logged in user's profile and cache the username on this account.
     */
    public void fetchUserProfile() throws DiscordException {
        JSONObject me = (JSONObject) fetchJSON("/users/@me");

        userId = Long.parseLong(me.getString("id", null));
        String globalName = me.getString("global_name", null);
        username = globalName != null ? globalName : me.getString("username", null);
    }
}
