package fi.gtrxac.discord;

import fi.gtrxac.discord.model.*;
import fi.gtrxac.bluewap.Util;

import java.util.*;

import cc.nnproject.json.*;

public class NameColorCache {
	private static Account account;
	private static Hashtable colors;
	private static Vector keys;
	public static boolean activeRequest;

	public static void init(Account acc) {
		account = acc;
		colors = new Hashtable();
		keys = new Vector();
	}

	private static void fetch(long guildId, Vector messages) {
		JSONObject reqData = new JSONObject();
		reqData.put("guild_id", guildId);

		JSONArray requestIds = new JSONArray();

		// Populate requestIds with authors and recipients of all messages that
		// are currently loaded and that don't have name colors already fetched
		for (int i = 0; i < messages.size(); i++) {
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
		return Settings.useNameColors &&
				// name colors cannot be fetched without gateway (technically can but isn't practical)
				account.gatewayActive();
	}

	public static int get(long guildId, long userId, Vector messages) {
		if (!active() || guildId == 0) return 0;

		long key = userId + guildId;

		Integer result = (Integer) colors.get(key);
		if (result != null) return result.intValue();

		if (!activeRequest) {
			activeRequest = true;
			fetch(guildId, messages);
		}
		return 0;
	}

	public static int get(long guildId, User user, Vector messages) {
		return get(guildId, user.id, messages);
	}

	public static void set(long key, int color) {
		// Discord role colors are RGB; Android needs an opaque ARGB value
		if (color != 0) color |= 0xFF000000;
		Util.hashtablePutWithLimit(colors, keys, key, new Integer(color), 50);
	}

	public static boolean has(long guildId, User user, boolean def) {
		if (!active()) return def;
		return colors.containsKey(user.id + guildId);
	}

	public static boolean has(long guildId, User user) {
		return has(guildId, user, false);
	}
}
