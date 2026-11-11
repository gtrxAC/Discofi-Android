package fi.gtrxac.discord.model;

import cc.nnproject.json.*;

/**
 * Created by gtrx on 3.9.2026.
 */

public class DMChannel extends Channel {
    public String username;
    public boolean isGroup;
    public long iconID;  // for groups, group ID. for users, recipient ID (not DM channel ID)
    public String iconHash;

    public DMChannel(JSONObject data) {
        super(data);
        isGroup = data.getInt("type") == 3;

        if (isGroup) {
            populateGroupInfo(data);
        }
        else {
            populateRecipientInfo(data);
        }
    }

    public DMChannel(JSONArray saveData) {
        id = saveData.getLong(0);
        name = saveData.getString(1);
        username = saveData.getString(2, null);
        isGroup = saveData.getBoolean(3, false);
        lastMessageID = saveData.getLong(4, 0);
        iconID = saveData.getLong(5, 0);
        iconHash = saveData.getString(6, null);
    }

    protected boolean nameHasHashtag() {
        return false;
    }

    public boolean isMessageable() {
        return true;
    }

    private void populateGroupInfo(JSONObject data) {
        // No group name -> list the names of up to 3 participants
        if (name == null) {
            JSONArray recipients = data.getArray("recipients");
            StringBuffer nameBuf = new StringBuffer();

            int i = 0;
            while (i < recipients.size()) {
                JSONObject recipient = recipients.getObject(i);
                String name = recipient.getString("global_name", null);
                if (name == null) {
                    name = recipient.getString("username");
                }
                nameBuf.append(name);
                i++;
                if (i >= 3 || i >= recipients.size()) break;
                nameBuf.append(", ");
            }
            name = nameBuf.toString();
        }
        iconID = id;
        iconHash = data.getString("icon", null);
    }

    private void populateRecipientInfo(JSONObject data) {
        try {
            JSONObject recipient = data.getArray("recipients").getObject(0);

            name = recipient.getString("global_name", null);
            if (name == null) {
                name = recipient.getString("username");
            }
            else {
                username = recipient.getString("username", null);
            }
            iconID = Long.parseLong(recipient.getString("id"));
            iconHash = recipient.getString("avatar", null);
        }
        catch (Exception e) {
        }
    }

    public String getIconUrl(String cdnUrl) {
        if (iconHash == null || iconID == 0 || cdnUrl == null) return null;
        return cdnUrl + (isGroup ? "/channel-icons/" : "/avatars/") + iconID + "/" + iconHash +
            ".png?size=64";
    }

    public JSONArray toJSON() {
        JSONArray result = new JSONArray();
        result.add(id);
        result.add(name);
        result.add(username);
        result.add(isGroup);
        result.add(lastMessageID);
        if (iconID != 0 || iconHash != null) result.add(iconID);
        if (iconHash != null) result.add(iconHash);
        return result;
    }
}
