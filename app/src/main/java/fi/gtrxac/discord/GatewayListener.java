package fi.gtrxac.discord;

import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;

/**
 * Receives real-time events from {@link GatewayThread}.
 * Callbacks are invoked on the gateway thread, so UI code must switch to the
 * main thread before touching views.
 */
public interface GatewayListener {
	void onGatewayConnected();

	void onGatewayDisconnected(String reason);

	void onGatewayReady(JSONObject data);

	void onMessageCreate(JSONObject message);

	void onMessageUpdate(JSONObject message);

	void onMessageDelete(JSONObject message);

	void onTypingStart(JSONObject data);

	void onGuildMembersChunk(JSONObject data);

	void onPresenceUpdate(JSONObject data);

	void onReadStates(JSONArray entries);
}
