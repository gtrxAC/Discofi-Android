package fi.gtrxac.discord.model;

import java.util.Vector;

import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;

/**
 * Created by Leap on 4.10.2026.
 */

public class Embed {
    public String title;
    public Type type;
    public String description;
    public String url;
    public String timestamp; // ISO-8601
    public int color;
    public Footer footer;
    public Attachment image;
    public Attachment thumbnail;
    public Attachment video;
    public Provider provider;
    public Author author;
    public Vector<Field> fields;

    public Embed(JSONObject data) {
        title = data.getString("title", null);
        type = Type.valueOf(data.getString("type").toUpperCase());
        description = data.getString("description", null);
        url = data.getString("url", null);
        timestamp = data.getString("timestamp", null);
        color = data.getInt("color", 0);
        if (data.has("footer")) footer = new Footer(data.getObject("footer"));
        if (data.has("image")) image = new Attachment(data.getObject("image"), true);
        if (data.has("thumbnail")) thumbnail = new Attachment(data.getObject("thumbnail"), true);
        if (data.has("video")) video = new Attachment(data.getObject("video"), true);
        if (data.has("provider")) provider = new Provider(data.getObject("provider"));
        if (data.has("author")) author = new Author(data.getObject("author"));
        if (data.has("footer")) footer = new Footer(data.getObject("footer"));
        if (data.has("fields")) {
            try {
                JSONArray fieldArray = data.getArray("fields");

                for (int i = 0; i < fieldArray.size(); i++) {
                    try {
                        Field field = new Field(fieldArray.getObject(i));
                        if (fields == null) fields = new Vector();
                        fields.addElement(field);
                    }
                    catch (Exception e) {
                    }
                }
            }
            catch (Exception e) {
            }
        }
    }

    public enum Type {
        UNKNOWN, RICH, IMAGE, VIDEO, GIFV, ARTICLE, LINK,
    }

    public class Footer {
        public String text;
        public String iconUrl;
        public String proxyIconUrl;

        public Footer(JSONObject data) {
            text = data.getString("text", null);
            iconUrl = data.getString("icon_url", null);
            proxyIconUrl = data.getString("proxy_icon_url", null);
        }
    }

    public class Provider {
        public String name;
        public String url;

        public Provider(JSONObject data) {
            name = data.getString("name", null);
            url = data.getString("url", null);
        }
    }

    public class Author {
        public String name;
        public String url;
        public String iconUrl;
        public String proxyIconUrl;

        public Author(JSONObject data) {
            name = data.getString("name", null);
            url = data.getString("url", null);
            iconUrl = data.getString("icon_url", null);
            proxyIconUrl = data.getString("proxy_icon_url", null);
        }
    }

    public class Field {
        public String name;
        public String value;

        public Field(JSONObject data) {
            name = data.getString("name", null);
            value = data.getString("value", null);
        }
    }
}
