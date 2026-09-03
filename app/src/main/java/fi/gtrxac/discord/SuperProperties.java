package fi.gtrxac.discord;

import java.util.UUID;

import cc.nnproject.json.JSONObject;
import util.Base64;

/**
 * Created by gtrx on 1.9.2026.
 */

public class SuperProperties {
    private static final String clientLaunchID = UUID.randomUUID().toString();

    private static String heartbeatSessionID;
    private static long heartbeatSessionIDExpiry;

    private static String getHeartbeatSessionID() {
        if (heartbeatSessionID == null || System.currentTimeMillis() >= heartbeatSessionIDExpiry) {
            heartbeatSessionID = UUID.randomUUID().toString();
            heartbeatSessionIDExpiry = System.currentTimeMillis() + 30*60*1000;
        }
        return heartbeatSessionID;
    }

    public static String get() {
        JSONObject propsJson = new JSONObject();

        propsJson.put("os", "Android");
        propsJson.put("browser", "Discord Android");
        propsJson.put("device", "a20e");
        propsJson.put("system_locale", "en-US");
        propsJson.put("has_client_mods", false);
        propsJson.put("client_version", "262.5 - rn");  // should match the user agent in HTTPConfig
        propsJson.put("release_channel", "alpha");
        propsJson.put("device_vendor_id", "17503929-a4b8-4490-87bf-0222adfdadc8");
        propsJson.put("design_id", 2);
        propsJson.put("browser_user_agent", "");
        propsJson.put("browser_version", "");
        propsJson.put("os_version", "34");
        propsJson.put("client_build_number", 3463);
        propsJson.put("client_event_source", (Object) null);
        propsJson.put("client_launch_id", clientLaunchID);
        propsJson.put("launch_signature", Long.toString(System.nanoTime()));
        propsJson.put("client_heartbeat_session_id", getHeartbeatSessionID());

        byte[] propsBytes = propsJson.build().getBytes();
        return Base64.encodeToString(propsBytes, false);
    }
}
