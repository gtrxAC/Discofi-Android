package fi.gtrxac.discord;

import java.util.Vector;

import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;
import fi.gtrxac.discord.model.GuildMember;
import fi.gtrxac.discord.model.Message;
import fi.gtrxac.discord.model.User;
import fi.gtrxac.discord.util.LRUCache;

public class GuildMemberCache extends LRUCache {
    private static final int CACHE_SIZE = 10;
    public boolean activeRequest;
    private Account account;

    public GuildMemberCache(Account acc) {
        super(CACHE_SIZE);
        account = acc;
    }

    private void fetch(long guildId, Vector messages) {
        JSONObject reqData = new JSONObject();
        reqData.put("guild_id", guildId);

        JSONArray requestIds = new JSONArray();

        // Populate requestIds with authors and recipients of all messages that
        // are currently loaded and that don't have guild member data fetched
        for (int i = 0; messages != null && i < messages.size(); i++) {
            Message msg = (Message) messages.elementAt(i);
            long userId = msg.author.id;

            if (requestIds.indexOf(Long.toString(userId)) == -1 && !has(guildId, msg.author)) {
                requestIds.add(Long.toString(userId));
            }

            if (msg.reply == null) continue;
            userId = msg.reply.author.id;

            if (requestIds.indexOf(Long.toString(userId)) == -1 &&
                !has(guildId, msg.reply.author)) {
                requestIds.add(Long.toString(userId));
            }
        }

        reqData.put("user_ids", requestIds);

        JSONObject msg = new JSONObject();
        msg.put("op", 8);
        msg.put("d", reqData);
        account.gateway.send(msg);
    }

    public boolean active() {
        return account.gatewayActive();
    }

    public GuildMember get(long guildId, long userId, Vector messages) {
        if (!active() || guildId == 0) return null;

        long key = userId + guildId;

        GuildMember result = (GuildMember) super.get(key);
        if (result != null) return result;

        if (!activeRequest) {
            activeRequest = true;
            fetch(guildId, messages);
        }
        return null;
    }

    public GuildMember get(long guildId, User user, Vector messages) {
        GuildMember member = get(guildId, user.id, messages);
        if (member != null) return member;
        return new GuildMember(user);
    }

    public boolean has(long guildId, User user, boolean def) {
        if (!active()) return def;
        return cache.containsKey(user.id + guildId);
    }

    public boolean has(long guildId, User user) {
        return has(guildId, user, false);
    }
}
