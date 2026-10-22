package fi.gtrxac.discord;

import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;

import javax.microedition.io.SocketConnection;

import tech.alicesworld.ModernConnector.ModernConnector;
import tech.alicesworld.ModernConnector.WebSocketClient;

public class GatewayThread extends Thread {
    public static final String DEFAULT_GATEWAY_URL =
            "wss://gateway.discord.gg/?v=9&encoding=json";

    private static final Vector<GatewayListener> listeners = new Vector<GatewayListener>();

    private Account account;

    volatile boolean stop;
    volatile String stopMessage;

    private HeartbeatThread hbThread;

    private SocketConnection sc;
    private WebSocketClient ws;
    private InputStream is;
    private OutputStream os;

    private static int reconnectAttempts;

	public GatewayThread(Account account) {
	    this.account = account;
		account.subscribedGuilds = new Vector();
		reconnectAttempts++;
	}

    public static void addListener(GatewayListener listener) {
        if (listener == null) return;
        synchronized (listeners) {
            if (!listeners.contains(listener)) {
                listeners.addElement(listener);
            }
        }
    }

    public static void removeListener(GatewayListener listener) {
        synchronized (listeners) {
            listeners.removeElement(listener);
        }
    }

    private static GatewayListener[] getListeners() {
        synchronized (listeners) {
            GatewayListener[] result = new GatewayListener[listeners.size()];
            listeners.copyInto(result);
            return result;
        }
    }

    private static void notifyConnected() {
        GatewayListener[] targets = getListeners();
        for (int i = 0; i < targets.length; i++) {
            try {
                targets[i].onGatewayConnected();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void notifyDisconnected(String reason) {
        GatewayListener[] targets = getListeners();
        for (int i = 0; i < targets.length; i++) {
            try {
                targets[i].onGatewayDisconnected(reason);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void notifyReady(JSONObject data) {
        GatewayListener[] targets = getListeners();
        for (int i = 0; i < targets.length; i++) {
            try {
                targets[i].onGatewayReady(data);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void notifyMessageCreate(JSONObject data) {
        GatewayListener[] targets = getListeners();
        for (int i = 0; i < targets.length; i++) {
            try {
                targets[i].onMessageCreate(data);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void notifyMessageUpdate(JSONObject data) {
        GatewayListener[] targets = getListeners();
        for (int i = 0; i < targets.length; i++) {
            try {
                targets[i].onMessageUpdate(data);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void notifyMessageDelete(JSONObject data) {
        GatewayListener[] targets = getListeners();
        for (int i = 0; i < targets.length; i++) {
            try {
                targets[i].onMessageDelete(data);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void notifyTypingStart(JSONObject data) {
        GatewayListener[] targets = getListeners();
        for (int i = 0; i < targets.length; i++) {
            try {
                targets[i].onTypingStart(data);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void notifyGuildMembersChunk(JSONObject data) {
        GatewayListener[] targets = getListeners();
        for (int i = 0; i < targets.length; i++) {
            try {
                targets[i].onGuildMembersChunk(data);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void notifyPresenceUpdate(JSONObject data) {
        GatewayListener[] targets = getListeners();
        for (int i = 0; i < targets.length; i++) {
            try {
                targets[i].onPresenceUpdate(data);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void notifyReadStates(JSONArray entries) {
        GatewayListener[] targets = getListeners();
        for (int i = 0; i < targets.length; i++) {
            try {
                targets[i].onReadStates(entries);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void disconnect() {
		if (hbThread != null) hbThread.stop = true;
		try { is.close(); } catch (Exception e) {}
		try { os.close(); } catch (Exception e) {}
		try { ws.close(); } catch (Exception e) {}
		try { sc.close(); } catch (Exception e) {}
    }

    public void disconnected(String message) {
        disconnect();
        stopMessage = message;
        notifyDisconnected(message);

        if ((!stop || stopMessage != null) && reconnectAttempts < 3) {
            reconnectAttempts++;
            new Thread() {
                public void run() {
                    try {
                        Thread.sleep(2000L * reconnectAttempts);
                    }
                    catch (InterruptedException e) {}
                    if (!stop && account.useGateway) {
                        account.startGateway();
                    }
                }
            }.start();
        }
    }

    public void send(JSONObject msg) {
        if (ws != null) {
            try {
                ws.sendMessage(msg.build());
            }
            catch (Exception e) {}
        } else {
            send(os, msg);
        }
    }

    public static void send(OutputStream os, JSONObject msg) {
        try {
            os.write((msg.build() + "\n").getBytes("UTF-8"));
            os.flush();
        } catch (Exception e) {}
    }

    public void run() {
        if (account == null || !account.useGateway) return;
        try {
            String url = account.gatewayUrl;

            if (!account.useProxy || url == null || url.length() == 0) {
                url = DEFAULT_GATEWAY_URL;
            } else if (!url.startsWith("socket://") || !url.startsWith("ws://") || !url.startsWith("wss://")) {
                url = "socket://" + url; // assume proxied gateway
            }

            Object conn = ModernConnector.open(url);
            if (conn instanceof WebSocketClient) {
                ws = (WebSocketClient) conn;
            } else {
                sc = (SocketConnection) conn;
                sc.setSocketOption(SocketConnection.KEEPALIVE, 1);
			    is = sc.openInputStream();
			    os = sc.openOutputStream();
            }

            notifyConnected();

            StringBuffer sb = new StringBuffer();
            String msgStr;

            while (true) {
                // Get message
                while (true) {
                    if (stop) {
                        if (stopMessage != null) disconnected(stopMessage);
                        else disconnect();
                        return;
                    }

                    if (ws != null) {
                        msgStr = ws.receiveMessageString();
                        if (msgStr != null) break;
                    } else {
                        int ch = is.read();
                        if (ch == '\n' || ch == -1) {
                            if (sb.length() > 0) {
                                // This message has been fully received, start processing it
                                msgStr = new String(sb.toString().getBytes("ISO-8859-1"), "UTF-8");
                                sb = new StringBuffer();
                                break;
                            }
                        } else {
                            sb.append((char) ch);
                        }
                    }
                }

                // Process message
                JSONObject message = JSON.getObject(msgStr);
                String op = message.getString("t", "");

				// Save message sequence number (used for heartbeats)
				int seq = message.getInt("s", -1);
				if (hbThread != null && seq > hbThread.lastReceived) {
					hbThread.lastReceived = seq;
				}

                if (op != null) {
					if (op.equals("GATEWAY_HELLO")) {
						// Connect to gateway
						JSONArray events = new JSONArray();
						events.add("MESSAGE_CREATE");
						events.add("MESSAGE_DELETE");
						events.add("MESSAGE_UPDATE");
						events.add("TYPING_START");
						events.add("GUILD_MEMBERS_CHUNK");
						events.add("READY");
						events.add("READ_STATES");

						JSONObject connData = new JSONObject();
						connData.put("supported_events", events);
						connData.put("url", DEFAULT_GATEWAY_URL);

						JSONObject connMsg = new JSONObject();
						connMsg.put("op", -1);
						connMsg.put("t", "GATEWAY_CONNECT");
						connMsg.put("d", connData);
						send(connMsg);
					}
					else if (op.equals("GATEWAY_DISCONNECT")) {
						String reason = message.getObject("d").getString("message");
						disconnected(reason);
						return;
					}
                    else if (op.equals("MESSAGE_CREATE")) {
                        JSONObject data = message.getObject("d");
                        notifyMessageCreate(data);
                    }
                    else if (op.equals("MESSAGE_UPDATE")) {
                        JSONObject data = message.getObject("d");
                        notifyMessageUpdate(data);
                    }
                    else if (op.equals("MESSAGE_DELETE")) {
                        JSONObject data = message.getObject("d");
                        notifyMessageDelete(data);
                    }
                    else if (op.equals("TYPING_START")) {
                        JSONObject data = message.getObject("d");
                        notifyTypingStart(data);
                    }
                    else if (op.equals("GUILD_MEMBERS_CHUNK")) {
                        JSONObject data = message.getObject("d");
                        notifyGuildMembersChunk(data);
                    }
                    else if (op.equals("PRESENCE_UPDATE")) {
                        JSONObject data = message.getObject("d");
                        notifyPresenceUpdate(data);
                    }
                    else if (op.equals("READY")) {
                        JSONObject data = message.getObject("d");
                        reconnectAttempts = 0;
                        if (account != null && account.userId == 0) {
                            try {
                                account.userId = Long.parseLong(data.getString("id"));
                            }
                            catch (Exception e) {
                                try {
                                    account.userId = Long.parseLong(data.getObject("user").getString("id"));
                                }
                                catch (Exception ignored) {}
                            }
                        }
                        notifyReady(data);
                    }
                    else if (op.equals("READ_STATES")) {
                        JSONArray entries = message.getArray("d");
                        notifyReadStates(entries);
                    }
                }
				else if (message.getInt("op", 0) == 10) {
					int heartbeatInterval = message.getObject("d").getInt("heartbeat_interval");
					hbThread = new HeartbeatThread(this, heartbeatInterval);
					hbThread.start();

					JSONObject idData = new JSONObject();
					idData.put("token", account.token);
					idData.put("capabilities", 30717);
					idData.put("properties", SuperProperties.get());

					JSONObject idMsg = new JSONObject();
					idMsg.put("op", 2);
					idMsg.put("d", idData);
					send(idMsg);
				}
            }
        }
        catch (Exception e) {
            disconnected(e.toString());
            e.printStackTrace();
        }
    }
}
