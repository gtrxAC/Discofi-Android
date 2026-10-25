package fi.gtrxac.discord.model;

import fi.gtrxac.discord.DiscordUtil;
import cc.nnproject.json.*;
import java.util.*;

public class Message {
    public static final int TYPE_ADDED = 1;  // user added another user to group DM
    public static final int TYPE_REMOVED = 2;  // user left (or removed someone) from group DM
    public static final int TYPE_CALL = 3;
    public static final int TYPE_CHANNEL_NAME_CHANGE = 4;  // changed name of group DM
    public static final int TYPE_CHANNEL_ICON_CHANGE = 5;
    public static final int TYPE_PINNED = 6;
    public static final int TYPE_JOINED = 7;  // user joined server
    public static final int TYPE_BOOSTED = 8;  // user boosted server
    public static final int TYPE_BOOSTED_LEVEL_1 = 9;
    public static final int TYPE_BOOSTED_LEVEL_2 = 10;
    public static final int TYPE_BOOSTED_LEVEL_3 = 11;

    public long id;
    public long channelId;
    public User author;
    public String content;
    public boolean showAuthor;
    public String timestamp;
    public Vector attachments;
    public Vector embeds;
    public Hashtable mentions;
    public boolean mentionEveryone;

    public boolean isEdited;
    public boolean isStatus;
    public boolean isForwarded;
    public boolean isReply;
    public Message reply;

    public Message(JSONObject data) {
        id = Long.parseLong(data.getString("id"));
        channelId = Long.parseLong(data.getString("channel_id"));
        author = new User(data.getObject("author"));
        timestamp = DiscordUtil.formatTimestamp(id, false);
        mentionEveryone = data.getBoolean("mention_everyone", false);

        isEdited = (data.getString("edited_timestamp", null) != null);

        JSONObject contentObject = getContentObject(data);

        int type = contentObject.getInt("type", 0);
        isStatus = (type >= TYPE_ADDED && type <= TYPE_BOOSTED_LEVEL_3);

        if (isStatus) {
            content = parseStatusMessage(contentObject, type);
        }
        else {
            content = contentObject.getString("content", "");
            parseReply(contentObject);
            parseAttachments(contentObject);
            parseEmbeds(contentObject);
            parseMentions(contentObject);
        }
    }

    /**
     * Get JSON object to use for filling out message content and related fields.
     * For forwarded messages, this is an inner object inside the message object.
     */
    private JSONObject getContentObject(JSONObject data) {
        JSONObject result = data;
        isForwarded = false;

        try {
            result = data.getArray("message_snapshots").getObject(0).getObject("message");
            isForwarded = true;
        }
        catch (Exception e) {}

        return result;
    }

    private void parseReply(JSONObject data) {
        try {
            reply = new Message(data.getObject("referenced_message"));
            isReply = true;
        }
        catch (Exception e) {}
    }

    private void parseAttachments(JSONObject data) {
        JSONArray attachArray = data.getArray("attachments", null);
        if (attachArray == null) return;

        for (int i = 0; i < attachArray.size(); i++) {
            try {
                JSONObject attachJson = attachArray.getObject(i);
                Attachment attachment = new Attachment(attachJson);

                if (attachments == null) {
                    attachments = new Vector();
                }
                attachments.addElement(attachment);
            }
            catch (Exception e) {}
        }
    }

    private void parseEmbeds(JSONObject data) {
        JSONArray embedArray = data.getArray("embeds", null);
        if (embedArray == null) return;

        for (int i = 0; i < embedArray.size(); i++) {
            try {
                JSONObject embedJson = embedArray.getObject(i);
                Embed embed = new Embed(embedJson);

                if (embeds == null) {
                    embeds = new Vector();
                }
                embeds.addElement(embed);
            }
            catch (Exception e) {}
        }
    }

    private void parseMentions(JSONObject data) {
        JSONArray mentionArray = data.getArray("mentions", null);
        if (mentionArray == null) return;

        for (int i = 0; i < mentionArray.size(); i++) {
            try {
                JSONObject mention = mentionArray.getObject(i);

                String id = mention.getString("id", null);
                String name = null;

                try {
                    JSONObject user = mention.getObject("user");
                    id = user.getString("id", id);
                    name = user.getString("global_name", null);
                    if (name == null) name = user.getString("username", null);
                }
                catch (Exception e) {}

                if (name == null) name = mention.getString("global_name", null);
                if (name == null) name = mention.getString("username", null);

                try {
                    String nick = mention.getObject("member").getString("nick", null);
                    if (nick != null) name = nick;
                }
                catch (Exception e) {}

                if (id == null || name == null) continue;
                if (mentions == null) {
                    mentions = new Hashtable();
                }
                mentions.put(id, name);
            }
            catch (Exception e) {}
        }
    }

    private String parseStatusMessage(JSONObject data, int type) {
        // Status message -> determine content by message type
        String target = "(unknown)";
        long targetID = 0;

        try {
            JSONObject targetData = data.getArray("mentions").getObject(0);
            User targetUser = new User(targetData);
            target = targetUser.name;
            targetID = targetUser.id;
        }
        catch (Exception e) {}

        switch (type) {
            case TYPE_ADDED: {
                return "added " + target + " to the group";
            }
            case TYPE_REMOVED: {
                if (author.id == targetID) {
                    return "left the group";
                }
                return "removed " + target + " from the group";
            }
            case TYPE_CALL: {
                return "started a call";
            }
            case TYPE_CHANNEL_NAME_CHANGE: {
                return "changed the group name";
            }
            case TYPE_CHANNEL_ICON_CHANGE: {
                return "changed the group icon";
            }
            case TYPE_PINNED: {
                return "pinned a message";
            }
            case TYPE_JOINED: {
                return "joined the server";
            }
            case TYPE_BOOSTED: {
                return "boosted the server";
            }
            default: {
                return "boosted the server to level " + (type - TYPE_BOOSTED);
            }
        }
    }

    public boolean shouldShowAuthor(Message above, long clusterStart) {
        // Different authors -> true
        if (above.author.id != author.id) return true;

        // This message is a reply -> true
        if (reply != null) return true;

        // This message only consists of an attachment/embed -> true
        if ((attachments != null || embeds != null) && content.length() == 0) return true;

        // This message or above message is a status message -> true
        if (isStatus || above.isStatus) return true;

        // Finally, check if message was sent more than 7 minutes after the first message of the cluster
        long thisMsgTime = id >> 22;
        long firstMsgTime = clusterStart >> 22;
        return (thisMsgTime - firstMsgTime > 7*60*1000);
    }
}
