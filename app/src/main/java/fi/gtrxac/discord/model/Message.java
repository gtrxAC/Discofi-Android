package fi.gtrxac.discord.model;

import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;

public class Message {
    public static final int TYPE_ADDED = 1; // user added another user to group DM
    public static final int TYPE_REMOVED = 2; // user left (or was removed) from group DM
    public static final int TYPE_CALL = 3;
    public static final int TYPE_CHANNEL_NAME_CHANGE = 4; // changed name of group DM
    public static final int TYPE_CHANNEL_ICON_CHANGE = 5;
    public static final int TYPE_PINNED = 6;
    public static final int TYPE_JOINED = 7; // user joined server
    public static final int TYPE_BOOSTED = 8; // user boosted server
    public static final int TYPE_BOOSTED_LEVEL_1 = 9;
    public static final int TYPE_BOOSTED_LEVEL_2 = 10;
    public static final int TYPE_BOOSTED_LEVEL_3 = 11;

    public static final long SNOWFLAKE_EPOCH = 1420070400000L;

    public long id;
    public String channelId;
    public User author;
    public String content;
    public String timestamp;

    // is status message? (user joined/left/boosted) - affects rendering
    public boolean isStatus;

    // reply rendering
    public User replyAuthor;
    public String replyContent;

    // attachments (images, videos and other files) shown under the content
    public Vector attachments;
    // embeds (link previews and other rich content) shown under the attachments
    public Vector embeds;

    public boolean showAuthor;

    public Message(JSONObject data) {
        id = Long.parseLong(data.getString("id"));
        channelId = data.getString("channel_id", null);
        author = new User(data.getObject("author"));
        content = data.getString("content", "");

        int t = data.getInt("type", 0);
        if (t >= TYPE_ADDED && t <= TYPE_BOOSTED_LEVEL_3) {
            isStatus = true;
        }

        if (isStatus) {
            // Status message -> determine content by message type
            String target = "(unknown)";
            try {
                JSONObject targetData = data.getArray("mentions").getObject(0);
                target = new User(targetData).name;
            }
            catch (Exception e) {}

            switch (t) {
                case TYPE_ADDED:
                    content = "added " + target + " to the group";
                    break;
                case TYPE_REMOVED:
                    if (target.equals(author.name)) {
                        content = "left the group";
                    } else {
                        content = "removed " + target + " from the group";
                    }
                    break;
                case TYPE_CALL:
                    content = "started a call";
                    break;
                case TYPE_CHANNEL_NAME_CHANGE:
                    content = "changed the group name";
                    break;
                case TYPE_CHANNEL_ICON_CHANGE:
                    content = "changed the group icon";
                    break;
                case TYPE_PINNED:
                    content = "pinned a message";
                    break;
                case TYPE_JOINED:
                    content = "joined the server";
                    break;
                case TYPE_BOOSTED:
                    content = "boosted the server";
                    break;
                case TYPE_BOOSTED_LEVEL_1:
                case TYPE_BOOSTED_LEVEL_2:
                case TYPE_BOOSTED_LEVEL_3:
                    content = "boosted the server to level " + (t - TYPE_BOOSTED);
                    break;
            }
        } else {
            try {
                JSONObject refObj = data.getObject("referenced_message");
                replyAuthor = new User(refObj.getObject("author"));
                replyContent = refObj.getString("content", null);
                if (replyContent == null || replyContent.length() == 0) {
                    replyContent = "(no content)";
                }
            }
            catch (Exception e) {}

            try {
                JSONArray attachArray = data.getArray("attachments");

                for (int i = 0; i < attachArray.size(); i++) {
                    try {
                        Attachment attachment = new Attachment(attachArray.getObject(i));
                        if (attachments == null) attachments = new Vector();
                        attachments.addElement(attachment);
                    }
                    catch (Exception e) {}
                }
            }
            catch (Exception e) {}

            try {
                JSONArray embedArray = data.getArray("embeds");

                for (int i = 0; i < embedArray.size(); i++) {
                    try {
                        Embed embed = new Embed(embedArray.getObject(i));
                        if (embeds == null) embeds = new Vector();
                        embeds.addElement(embed);
                    }
                    catch (Exception e) {}
                }
            }
            catch (Exception e) {}

            try {
                JSONArray stickers = data.getArray("sticker_items");
                if (stickers.size() >= 1) {
                    if (content.length() > 0) content += "\n";
                    content += "(sticker: " + stickers.getObject(0).getString("name", "unknown") + ")";
                }
            }
            catch (Exception e) {}
        }

        timestamp = formatTimestamp();

        if (content.length() == 0 && attachments == null && embeds == null && !isStatus) {
            isStatus = true;
            content = "(unsupported message)";
        }
    }

    private String formatTimestamp() {
        Date messageDate = new Date((id >> 22) + SNOWFLAKE_EPOCH);
        String messageDay = messageDate.toString().substring(0, 10);
        String currentDay = new Date().toString().substring(0, 10);

        Calendar cal = Calendar.getInstance();
        cal.setTime(messageDate);
        StringBuffer time = new StringBuffer();

        if (currentDay.equals(messageDay)) {
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            int minute = cal.get(Calendar.MINUTE);

            time.append(hour);
            time.append(":");
            if (minute < 10) time.append("0");
            time.append(minute);
        } else {
            int day = cal.get(Calendar.DAY_OF_MONTH);
            if (day < 10) time.append("0");
            time.append(day);
            time.append("/");
            int month = cal.get(Calendar.MONTH) + 1;
            if (month < 10) time.append("0");
            time.append(month);
        }
        return time.toString();
    }

    /**
     * Determine whether or not the author/timestamp row should be shown for
     * this message.
     *
     * @param above        The message shown above this message.
     * @param clusterStart The ID of the top-most message in this message cluster.
     * @return true if author should be shown, false if messages are "merged"
     */
    public boolean shouldShowAuthor(Message above, long clusterStart) {
        // Different authors -> true
        if (!above.author.id.equals(author.id)) return true;

        // This message is a reply -> true
        if (replyAuthor != null) return true;

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
