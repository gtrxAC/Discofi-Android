package fi.gtrxac.discord;

import android.util.Log;

import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONObject;

public class HeartbeatThread extends Thread {
    private static final String TAG = "Heartbeat";

    GatewayThread gateway;
    int lastReceived;
    volatile boolean stop;
    private int interval;

    public HeartbeatThread(GatewayThread gateway, int interval) {
        this.gateway = gateway;
        this.interval = interval -
            3000;  // Discord already more or less accounts for network latency but this is 2G
        // we're talking about
        this.lastReceived = -1;

        Log.d(TAG, "Heartbeat interval set to " + this.interval + " ms");
    }

    public void run() {
        try {
            while (true) {
                if (stop) break;

                JSONObject hbMsg = new JSONObject();
                hbMsg.put("op", 1);
                if (lastReceived >= 0) {
                    hbMsg.put("d", lastReceived);
                }
                else {
                    hbMsg.put("d", JSON.json_null);
                }
                Log.d(TAG, "Sending heartbeat (seq " + lastReceived + ")");
                gateway.send(hbMsg);

                Thread.sleep(interval);
            }
            Log.d(TAG, "Heartbeat stopped");
        }
        catch (Exception e) {
            Log.e(TAG, "Heartbeat failed", e);
            gateway.stopMessage = "Heartbeat thread error: " + e.toString();
            gateway.stop = true;
        }
    }
}
