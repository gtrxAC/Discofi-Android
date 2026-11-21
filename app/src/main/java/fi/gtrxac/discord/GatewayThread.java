package fi.gtrxac.discord;

import android.util.Log;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;

import javax.microedition.io.SocketConnection;

import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;
import tech.alicesworld.ModernConnector.ModernConnector;
import tech.alicesworld.ModernConnector.WebSocketClient;

public class GatewayThread extends Thread {
    private static final String TAG = "Gateway";
    public static final String DEFAULT_GATEWAY_URL = "wss://gateway.discord.gg/?v=9&encoding=json";

    private static final Vector<GatewayListener> listeners = new Vector<GatewayListener>();
    private static int reconnectAttempts;
    volatile boolean stop;
    volatile String stopMessage;
    private Account account;
    private HeartbeatThread hbThread;
    private SocketConnection sc;
    private WebSocketClient ws;
    private InputStream is;
    private OutputStream os;

    public GatewayThread(Account account) {
        this.account = account;
        account.subscribedGuilds = new Vector();
        reconnectAttempts++;
        Log.d(TAG, "Gateway thread created, reconnect attempt " + reconnectAttempts);
    }

    public static void addListener(GatewayListener listener) {
        if (listener == null) return;
        synchronized (listeners) {
            if (!listeners.contains(listener)) {
                listeners.addElement(listener);
                Log.d(TAG, "Listener added: " + listener.getClass().getSimpleName() +
                    " (total " + listeners.size() + ")");
            }
        }
    }

    public static void removeListener(GatewayListener listener) {
        synchronized (listeners) {
            if (listeners.removeElement(listener)) {
                Log.d(TAG, "Listener removed: " + listener.getClass().getSimpleName() +
                    " (total " + listeners.size() + ")");
            }
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
                Log.e(TAG, "Listener failed handling gateway connect", e);
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
                Log.e(TAG, "Listener failed handling gateway disconnect", e);
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
                Log.e(TAG, "Listener failed handling READY", e);
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
                Log.e(TAG, "Listener failed handling MESSAGE_CREATE", e);
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
                Log.e(TAG, "Listener failed handling MESSAGE_UPDATE", e);
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
                Log.e(TAG, "Listener failed handling MESSAGE_DELETE", e);
            }
        }
    }

    private static void notifyMessageAck(JSONObject data) {
        GatewayListener[] targets = getListeners();
        for (int i = 0; i < targets.length; i++) {
            try {
                targets[i].onMessageAck(data);
            }
            catch (Exception e) {
                Log.e(TAG, "Listener failed handling MESSAGE_ACK", e);
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
                Log.e(TAG, "Listener failed handling TYPING_START", e);
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
                Log.e(TAG, "Listener failed handling GUILD_MEMBERS_CHUNK", e);
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
                Log.e(TAG, "Listener failed handling PRESENCE_UPDATE", e);
            }
        }
    }

    private static void notifyChannelUnread(JSONObject data) {
        GatewayListener[] targets = getListeners();
        for (int i = 0; i < targets.length; i++) {
            try {
                targets[i].onChannelUnread(data);
            }
            catch (Exception e) {
                Log.e(TAG, "Listener failed handling CHANNEL_UNREAD_UPDATE", e);
            }
        }
    }

    private static void notifyPassiveUpdateV1(JSONObject data) {
        GatewayListener[] targets = getListeners();
        for (int i = 0; i < targets.length; i++) {
            try {
                targets[i].onPassiveUpdateV1(data);
            }
            catch (Exception e) {
                Log.e(TAG, "Listener failed handling PASSIVE_UPDATE_V1", e);
            }
        }
    }

    private static void notifyPassiveUpdateV2(JSONObject data) {
        GatewayListener[] targets = getListeners();
        for (int i = 0; i < targets.length; i++) {
            try {
                targets[i].onPassiveUpdateV2(data);
            }
            catch (Exception e) {
                Log.e(TAG, "Listener failed handling PASSIVE_UPDATE_V2", e);
            }
        }
    }

    public static void send(OutputStream os, JSONObject msg) {
        try {
            os.write((msg.build() + "\n").getBytes("UTF-8"));
            os.flush();
        }
        catch (Exception e) {
            Log.e(TAG, "Failed to send gateway payload over socket", e);
        }
    }

    private void disconnect() {
        Log.d(TAG, "Closing gateway connection");

        if (hbThread != null) hbThread.stop = true;
        try {
            if (is != null) is.close();
        }
        catch (Exception e) {
            Log.w(TAG, "Failed to close gateway input stream", e);
        }
        try {
            if (os != null) os.close();
        }
        catch (Exception e) {
            Log.w(TAG, "Failed to close gateway output stream", e);
        }
        try {
            if (ws != null) ws.close();
        }
        catch (Exception e) {
            Log.w(TAG, "Failed to close WebSocket", e);
        }
        try {
            if (sc != null) sc.close();
        }
        catch (Exception e) {
            Log.w(TAG, "Failed to close gateway socket", e);
        }
    }

    public void disconnected(String message) {
        Log.w(TAG, "Gateway disconnected: " + message);

        disconnect();
        stopMessage = message;
        notifyDisconnected(message);

        if (Settings.autoReconnect && (!stop || stopMessage != null) && reconnectAttempts < 3) {
            reconnectAttempts++;
            final int delay = 2000 * reconnectAttempts;
            Log.i(TAG, "Reconnecting gateway in " + delay + " ms (attempt " + reconnectAttempts +
                ")");

            new Thread() {
                public void run() {
                    try {
                        Thread.sleep(delay);
                    }
                    catch (InterruptedException e) {
                        Log.w(TAG, "Gateway reconnect sleep interrupted", e);
                    }
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
            catch (Exception e) {
                Log.e(TAG, "Failed to send gateway payload over WebSocket", e);
            }
        }
        else {
            send(os, msg);
        }
    }

    public void run() {
        if (account == null || !account.useGateway) {
            Log.d(TAG, "Gateway disabled for this account");
            return;
        }

        try {
            String url = account.gatewayUrl;

            if (!account.useProxy || url == null || url.length() == 0) {
                url = DEFAULT_GATEWAY_URL;
            }
            else if (!url.startsWith("socket://") && !url.startsWith("ws://") &&
                !url.startsWith("wss://")) {
                url = "socket://" + url; // assume proxied gateway
            }

            Log.i(TAG, "Connecting to gateway: " + url);

            Object conn = ModernConnector.open(url);
            if (conn instanceof WebSocketClient) {
                Log.d(TAG, "Gateway using WebSocket transport");
                ws = (WebSocketClient) conn;
            }
            else {
                Log.d(TAG, "Gateway using raw socket transport");
                sc = (SocketConnection) conn;
                sc.setSocketOption(SocketConnection.KEEPALIVE, 1);
                is = sc.openInputStream();
                os = sc.openOutputStream();
            }

            Log.i(TAG, "Gateway connected");
            notifyConnected();

            StringBuffer sb = new StringBuffer();
            String msgStr;

            while (true) {
                // Get message
                while (true) {
                    if (stop) {
                        if (stopMessage != null) {
                            disconnected(stopMessage);
                        }
                        else {
                            disconnect();
                        }
                        return;
                    }

                    if (ws != null) {
                        msgStr = ws.receiveMessageString();
                        if (msgStr != null) break;
                    }
                    else {
                        int ch = is.read();
                        if (ch == '\n' || ch == -1) {
                            if (sb.length() > 0) {
                                // This message has been fully received, start processing it
                                msgStr = new String(sb.toString().getBytes("ISO-8859-1"), "UTF-8");
                                sb = new StringBuffer();
                                break;
                            }
                        }
                        else {
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

                if (op != null && op.length() > 0) {
                    Log.d(TAG, "Gateway event: " + op + " (seq " + seq + ")");
                }

                if (op != null) {
                    if (op.equals("GATEWAY_HELLO")) {
                        Log.d(TAG, "Gateway proxy sent HELLO, subscribing to events");

                        // Connect to gateway
                        JSONArray events = new JSONArray();
                        events.add("MESSAGE_CREATE");
                        events.add("MESSAGE_DELETE");
                        events.add("MESSAGE_UPDATE");
                        events.add("MESSAGE_ACK");
                        events.add("TYPING_START");
                        events.add("GUILD_MEMBERS_CHUNK");
                        events.add("READY");
                        events.add("CHANNEL_UNREAD_UPDATE");
                        events.add("PASSIVE_UPDATE_V2");

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
                        Log.w(TAG, "Gateway proxy requested disconnect: " + reason);
                        disconnected(reason);
                        return;
                    }
                    else if (op.equals("MESSAGE_CREATE")) {
                        JSONObject data = message.getObject("d");
                        Log.d(TAG, "MESSAGE_CREATE in channel " + data.getString("channel_id", "?") +
                            " (message " + data.getString("id", "?") + ")");

                        boolean mentioned = account.unreadManager.onMessageCreate(data);
                        Notifications.onMessageCreate(account, data, mentioned);
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
                    else if (op.equals("MESSAGE_ACK")) {
                        JSONObject data = message.getObject("d");
                        account.unreadManager.updateReadState(data);
                        notifyMessageAck(data);
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
                                Log.w(TAG, "READY has no top-level user ID, trying user object", e);
                                try {
                                    account.userId =
                                        Long.parseLong(data.getObject("user").getString("id"));
                                }
                                catch (Exception ignored) {
                                    Log.w(TAG, "READY did not contain a usable user ID", ignored);
                                }
                            }
                        }
                        Log.i(TAG, "Gateway READY for user " + account.userId);
                        account.channelCache.onGatewayReady(data);
                        account.unreadManager.updateReadStatesFromReady(data);
                        notifyReady(data);
                    }
                    else if (op.equals("CHANNEL_UNREAD_UPDATE")) {
                        JSONObject data = message.getObject("d");
                        account.unreadManager.updateChannelUnread(data);
                        notifyChannelUnread(data);
                    }
                    else if (op.equals("PASSIVE_UPDATE_V1")) {
                        JSONObject data = message.getObject("d");
                        account.unreadManager.updateChannelUnread(data);
                        notifyPassiveUpdateV1(data);
                    }
                    else if (op.equals("PASSIVE_UPDATE_V2")) {
                        JSONObject data = message.getObject("d");
                        account.unreadManager.updateChannelUnread(data);
                        notifyPassiveUpdateV2(data);
                    }
                }
                else if (message.getInt("op", 0) == 10) {
                    int heartbeatInterval = message.getObject("d").getInt("heartbeat_interval");
                    Log.i(TAG, "Gateway HELLO, heartbeat interval " + heartbeatInterval + " ms");
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
            Log.e(TAG, "Gateway connection failed", e);
            disconnected(e.toString());
        }
    }
}
