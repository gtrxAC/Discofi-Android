package fi.gtrxac.discord.model;

import java.util.Locale;

import cc.nnproject.json.JSONObject;

/**
 * Created by Leap on 4.10.2026.
 */

public class Attachment {
    private static final String[] VIDEO_EXTENSIONS = {
        ".mp4", ".3gp", ".webm", ".mov", ".m4v", ".mkv", ".avi", ".gifv"
    };

    // requesting a proxy-scaled preview wider than this is a waste of data
    private static final int MAX_PREVIEW_WIDTH = 512;

    public String id;
    public String filename;
    public String contentType;
    public int size;

    public int width;
    public int height;

    public String url;
    public String proxyUrl;

    // images and videos can be previewed (videos as their first frame)
    public boolean previewable;
    public boolean isVideo;
    // previewable gifs that can be played back as an animation
    public boolean animated;

    public Attachment(JSONObject data) {
        this(data, false);
    }

    public Attachment(JSONObject data, boolean isEmbed) {
        width = data.getInt("width", 0);
        height = data.getInt("height", 0);
        contentType = data.getString("content_type", null);

        url = data.getString("url", null);
        proxyUrl = data.getString("proxy_url", null);

        if (!isEmbed) {
            id = data.getString("id", null);
            filename = data.getString("filename", "attachment");
            size = data.getInt("size", 0);
        }

        isVideo = (isContentType("video/"))
                || (filename != null && endsWithAny(filename.toLowerCase(), VIDEO_EXTENSIONS));

        // Anything with dimensions can be rendered by the media proxy
        previewable = (proxyUrl != null && width > 0);

        animated = (isContentType("image/gif"))
                || (filename != null && filename.toLowerCase().endsWith(".gif"));
    }

    private boolean isContentType(String check) {
        return contentType != null && contentType.startsWith(check);
    }

    public String getPreviewUrl(int maxWidth) {
        if (!previewable) return null;
        if (animated) return appendQuery(proxyUrl, "format=gif&width=" + getPreviewWidth(maxWidth));
        return getStillPreviewUrl(maxWidth);
    }

    /**
     * Still-frame preview URL. Also used as a fallback when an animated
     * preview cannot be played back.
     */
    public String getStillPreviewUrl(int maxWidth) {
        if (!previewable) return null;
        return appendQuery(proxyUrl, "format=jpeg&width=" + getPreviewWidth(maxWidth));
    }

    private static int getPreviewWidth(int maxWidth) {
        int width = maxWidth > 0 ? maxWidth : 512;
        if (width > MAX_PREVIEW_WIDTH) width = MAX_PREVIEW_WIDTH;
        return width;
    }

    private static String appendQuery(String url, String query) {
        if (url == null) return null;
        return url + (url.indexOf('?') == -1 ? "?" : "&") + query;
    }

    private static boolean endsWithAny(String name, String[] extensions) {
        for (int i = 0; i < extensions.length; i++) {
            if (name.endsWith(extensions[i])) return true;
        }
        return false;
    }

    public static String formatSize(int size) {
        if (size >= 1000000) return (size/1000000) + " MB";
        if (size >= 1000) return (size/1000) + " kB";
        return size + " bytes";
    }
}
