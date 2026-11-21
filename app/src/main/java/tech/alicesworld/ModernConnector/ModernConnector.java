/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tech.alicesworld.ModernConnector;

import android.util.Log;

import java.io.IOException;

import javax.microedition.io.Connector;
import javax.microedition.io.SecureConnection;
import javax.microedition.io.SocketConnection;

/**
 * @author NealShah
 */
public class ModernConnector {
    private static final String TAG = "ModernConnector";

    public static Object socketImplementation;

    public static Object open(String connectionURL) throws IOException {
        String protocol = Utils.split(connectionURL, ':')[0];
        Log.d(TAG, "Opening connection, protocol=" + protocol);

        if (protocol.equals("ssl") || protocol.equals("tls")) {
            String endpoint = Utils.split(connectionURL.substring(6), '/')[0];
            String[] endpointData = Utils.split(endpoint, ':');
            String host = endpointData[0];
            int port = 443;
            try {
                port = Integer.parseInt(endpointData[1]);
            }
            catch (Exception e) {
                Log.d(TAG, "No port in " + protocol + " endpoint, using default " + port);
            }

            return new ModernSecureConnection(host, port);
        }
        if (protocol.equals("ws")) {
            String endpoint = Utils.split(connectionURL.substring(5), '/')[0];
            String[] endpointData = Utils.split(endpoint, ':');
            String host = endpointData[0];
            int port = 80;
            try {
                port = Integer.parseInt(endpointData[1]);
            }
            catch (Exception e) {
                Log.d(TAG, "No port in ws endpoint, using default " + port);
            }
            SocketConnection socket =
                (SocketConnection) ModernConnector.open("socket://" + host + ":" + port);
            WebSocketClient ws = new WebSocketClient(host, port);
            ws.connectWithSocket(socket);
            return ws;
        }
        if (protocol.equals("wss")) {
            String endpoint = Utils.split(connectionURL.substring(6), '/')[0];

            String[] endpointData = Utils.split(endpoint, ':');
            String host = endpointData[0];
            int port = 443;
            try {
                port = Integer.parseInt(endpointData[1]);
            }
            catch (Exception e) {
                Log.d(TAG, "No port in wss endpoint, using default " + port);
            }
            SocketConnection socket =
                (SocketConnection) ModernConnector.open("tls://" + host + ":" + port);
            WebSocketClient ws = new WebSocketClient(host, port);
            ws.connectWithSocket(socket);
            return ws;
        }
        if (protocol.equals("https")) {
            String endpoint = Utils.split(connectionURL.substring(8), '/')[0];

            String[] endpointData = Utils.split(endpoint, ':');
            String host = endpointData[0];
            int port = 443;
            try {
                port = Integer.parseInt(endpointData[1]);
            }
            catch (Exception e) {
                Log.d(TAG, "No port in https endpoint, using default " + port);
            }
            SecureConnection socket =
                (SecureConnection) ModernConnector.open("tls://" + host + ":" + port);
            return new ModernHTTPSConnection(socket, connectionURL);
        }

        if (protocol.equals("socket") && ModernConnector.socketImplementation != null) {
            String endpoint = Utils.split(connectionURL.substring(8), '/')[0];

            String[] endpointData = Utils.split(endpoint, ':');
            String host = endpointData[0];
            int port = 443;
            try {
                port = Integer.parseInt(endpointData[1]);
            }
            catch (Exception e) {
                Log.d(TAG, "No port in socket endpoint, using default " + port);
            }

            return ((ModernConnector) ModernConnector.socketImplementation).openSocket(host, port);
        }

        return Connector.open(connectionURL);
    }

    public Object openSocket(String host, int port) {
        throw new RuntimeException("Only supported when new upstream is set");
    }


}
