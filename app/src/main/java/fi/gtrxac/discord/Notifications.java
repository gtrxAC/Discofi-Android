package fi.gtrxac.discord;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Hashtable;
import java.util.Vector;

import cc.nnproject.json.JSONObject;
import fi.gtrxac.discord.model.Attachment;
import fi.gtrxac.discord.model.Channel;
import fi.gtrxac.discord.model.DMChannel;
import fi.gtrxac.discord.model.Embed;
import fi.gtrxac.discord.model.Guild;
import fi.gtrxac.discord.model.Message;

public class Notifications {
    private static final int MAX_TEXT_LENGTH = 120;

    public static Context context;
    private static final Hashtable<Long, Integer> messageCounts = new Hashtable<>();
    private static volatile long openChannelId;

    public static void setOpenChannel(long channelId) {
        openChannelId = channelId;
    }

    public static void clearOpenChannel(long channelId) {
        if (openChannelId == channelId) openChannelId = 0;
    }

    public static synchronized void onMessageCreate(Account account, JSONObject data,
        boolean mentioned) {

        if (!Settings.notifications || context == null) return;

        try {
            JSONObject author = data.getObject("author", null);
            if (author == null) return;

            String authorId = author.getString("id", null);
            if (authorId == null || Long.parseLong(authorId) == account.getUserID()) return;

            long channelId = Long.parseLong(data.getString("channel_id"));

            // Don't notify about messages that are already being read
            if (channelId == openChannelId) return;

            String guildIdString = data.getString("guild_id", null);
            boolean isDM = (guildIdString == null);

            if (!shouldNotify(isDM, mentioned)) return;

            long guildId = isDM ? 0 : Long.parseLong(guildIdString);
            Message message = new Message(data);

            send(account, message, guildId, isDM, channelId);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean shouldNotify(boolean isDM, boolean mentioned) {
        if (Settings.notifyAllMessages) return true;
        if (isDM) return Settings.notifyDirectMessages;
        return mentioned && Settings.notifyMentions;
    }

    private static void send(Account account, Message message, long guildId, boolean isDM,
        long channelId) {
        String authorName = message.author != null ? message.author.name : "(unknown)";
        String content = getMessageText(context, message);

        DMChannel dm = isDM ? getDMChannel(account, channelId) : null;
        boolean isGroupDM = dm != null && dm.isGroup;

        String location;
        String channelName;
        String title;
        String text;
        String ticker;

        if (isDM) {
            if (isGroupDM) {
                location = dm.name;
                channelName = dm.name;
                title = dm.name;
                text = authorName + ": " + content;
                ticker =
                    context.getString(R.string.notification_server, authorName, location, content);
            }
            else {
                channelName = "@" + (dm != null ? dm.name : authorName);
                title = dm != null ? dm.name : authorName;
                text = content;
                ticker = context.getString(R.string.notification_dm, authorName, content);
            }
        }
        else {
            String guildName = null;
            try {
                Guild guild = account.getGuild(guildId);
            }
            catch (Exception e) {
                e.printStackTrace();
                guildName = context.getString(R.string.unknown);
            }

            Channel channel = account.channelCache.getChannelByID(guildId, channelId);
            if (channel != null && channel.name != null) {
                channelName = channel.name;
            }
            else {
                channelName = context.getString(R.string.unknown);
            }

            location = guildName + " " + channelName;
            title = location;
            text = authorName + ": " + content;
            ticker = context.getString(R.string.notification_server, authorName, location, content);
        }

        long when = (message.id >> 22) + DiscordUtil.DISCORD_EPOCH;
        int id = getNotificationId(channelId);
        int count = addMessageCount(channelId);

        int icon = R.drawable.ic_stat_discofi;

        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("fromNotification", true);
        intent.putExtra("guildId", guildId);
        intent.putExtra("channelId", channelId);
        intent.putExtra("channelName", channelName);
        intent.putExtra("messageId", message.id);

        PendingIntent pendingIntent =
            PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_CANCEL_CURRENT);

        Notification notification =
            buildNotification(context, icon, ticker, when, title, text, pendingIntent, count,
                Long.toString(channelId));

        NotificationManager manager =
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notifChannel =
                new NotificationChannel(Long.toString(channelId), channelName,
                    NotificationManager.IMPORTANCE_DEFAULT);
            manager.createNotificationChannel(notifChannel);
        }

        manager.notify(id, notification);
    }

    private static Notification buildNotification(Context context, int icon, String ticker,
        long when, String title, String text, PendingIntent pendingIntent, int count,
        String notifChannelId) {
        int defaults = 0;
        if (Settings.notificationSound) defaults |= Notification.DEFAULT_SOUND;
        if (Settings.notificationVibrate) defaults |= Notification.DEFAULT_VIBRATE;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                Notification.Builder builder =
                    new Notification.Builder(context).setSmallIcon(icon).setTicker(ticker)
                        .setWhen(when).setContentTitle(title).setContentText(text)
                        .setContentIntent(pendingIntent).setAutoCancel(true).setDefaults(defaults);

                if (count > 1) builder.setNumber(count);

                // Required on Android 8.0 and later
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    builder.setChannelId(notifChannelId);
                }

                return builder.getNotification();
            }
            else {
                Notification notification = new Notification(icon, ticker, when);
                notification.flags |= Notification.FLAG_AUTO_CANCEL;
                notification.defaults |= defaults;

                if (count > 1) notification.number = count;

                // setLatestEventInfo was removed in API 23
                Notification.class.getMethod("setLatestEventInfo",
                        new Class[]{Context.class, CharSequence.class, CharSequence.class,
                            PendingIntent.class})
                    .invoke(notification, new Object[]{context, title, text, pendingIntent});
                return notification;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private static String getMessageText(Context context, Message message) {
        String content = message.content == null ? "" : message.content.trim();
        content = content.replace('\n', ' ').replace('\r', ' ');
        StringBuffer attachments = new StringBuffer();

        int images = 0;
        int files = 0;

        if (message.attachments != null) {
            for (int i = 0; i < message.attachments.size(); i++) {
                Attachment attachment = (Attachment) message.attachments.elementAt(i);

                if (attachment.previewable || attachment.animated) {
                    images++;
                }
                else {
                    files++;
                }
            }
        }

        if (images > 0) {
            attachments.append(context.getString(
                images == 1 ? R.string.notification_images_single : R.string.notification_images_multiple,
                new Integer(images)));
        }
        if (files > 0) {
            if (attachments.length() > 0) attachments.append(' ');
            attachments.append(context.getString(
                files == 1 ? R.string.notification_files_single : R.string.notification_files_multiple,
                new Integer(files)));
        }

        if (attachments.length() > 0) {
            if (content.length() > 0) content += " ";
            content += attachments.toString();
        }

        if (content.length() == 0 && message.embeds != null && message.embeds.size() > 0) {
            Embed embed = (Embed) message.embeds.firstElement();

            if (embed.title != null) {
                content = embed.title;
            }
            else if (embed.description != null) content = embed.description;

            if (content != null) {
                content = content.trim().replace('\n', ' ').replace('\r', ' ');
            }
        }

        if (content == null || content.length() == 0) {
            content = context.getString(R.string.notification_no_content);
        }

        return truncate(content);
    }

    private static String truncate(String text) {
        if (text.length() <= MAX_TEXT_LENGTH) return text;

        int end = MAX_TEXT_LENGTH;
        char last = text.charAt(end - 1);

        // Don't split a surrogate pair (emoji)
        if (last >= 0xD800 && last <= 0xDBFF) end--;

        return text.substring(0, end) + "...";
    }

    private static Guild getGuild(Account account, long guildId) {
        try {
            return account.getGuild(guildId);
        }
        catch (Exception e) {
            return null;
        }
    }

    private static String getGuildName(Account account, long guildId) {
        Guild guild = getGuild(account, guildId);
        return guild != null ? guild.name : null;
    }

    private static DMChannel getDMChannel(Account account, long channelId) {
        if (!account.dmChannelsAvailable()) return null;

        try {
            Vector dmChannels = account.getDmChannels(false);

            for (int i = 0; i < dmChannels.size(); i++) {
                DMChannel dm = (DMChannel) dmChannels.elementAt(i);
                if (dm.id == channelId) return dm;
            }
        }
        catch (Exception e) {
        }
        return null;
    }

    private static int getNotificationId(long channelId) {
        int id = (int) (channelId ^ (channelId >>> 32)) & 0x7fffffff;
        return id == 0 ? 1 : id;
    }

    private static int addMessageCount(long channelId) {
        Long key = new Long(channelId);
        Integer oldCount = messageCounts.get(key);
        int result = (oldCount == null ? 0 : oldCount.intValue()) + 1;
        messageCounts.put(key, new Integer(result));
        return result;
    }

    /**
     * Remove the notification for a channel, e.g. when it was opened.
     */
    public static synchronized void clear(long channelId) {
        messageCounts.remove(new Long(channelId));

        if (context == null) return;

        NotificationManager manager =
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.cancel(getNotificationId(channelId));
    }

    /**
     * Remove all notifications, e.g. when logging out.
     */
    public static synchronized void clearAll() {
        messageCounts.clear();
        openChannelId = 0;

        if (context == null) return;

        NotificationManager manager =
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) manager.cancelAll();
    }
}
