package fi.gtrxac.discord;

import fi.gtrxac.discord.model.*;
import fi.gtrxac.bluewap.Util;

import java.util.*;

import cc.nnproject.json.*;

public class GuildMemberCache {
	private static Account account;
	private static Hashtable members;
	private static Vector keys;
	public static boolean activeRequest;

	public static void init(Account acc) {
		account = acc;
		members = new Hashtable();
		keys = new Vector();
	}

	private static void fetch(long guildId, Vector messages) {
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

			if (requestIds.indexOf(Long.toString(userId)) == -1 && !has(guildId, msg.reply.author)) {
				requestIds.add(Long.toString(userId));
			}
		}

		reqData.put("user_ids", requestIds);

		JSONObject msg = new JSONObject();
		msg.put("op", 8);
		msg.put("d", reqData);
		account.gateway.send(msg);
	}

	public static boolean active() {
		return account.gatewayActive();
	}

	public static GuildMember get(long guildId, long userId, Vector messages) {
		if (!active() || guildId == 0) return null;

		long key = userId + guildId;

		GuildMember result = (GuildMember) members.get(key);
		if (result != null) return result;

		if (!activeRequest) {
			activeRequest = true;
			fetch(guildId, messages);
		}
		return null;
	}

	public static GuildMember get(long guildId, User user, Vector messages) {
		GuildMember member = get(guildId, user.id, messages);
		if (member != null) return member;
		return new GuildMember(user);
	}

	public static void set(long key, GuildMember member) {
		Util.hashtablePutWithLimit(members, keys, key, member, 50);
	}

	public static boolean has(long guildId, User user, boolean def) {
		if (!active()) return def;
		return members.containsKey(user.id + guildId);
	}

	public static boolean has(long guildId, User user) {
		return has(guildId, user, false);
	}
}
