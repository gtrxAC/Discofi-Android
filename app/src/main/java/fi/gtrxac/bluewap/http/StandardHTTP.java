package fi.gtrxac.bluewap.http;

import android.os.Build;
import android.util.Log;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Enumeration;

import javax.microedition.io.HttpConnection;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;

import tech.alicesworld.ModernConnector.ModernConnector;

public class StandardHTTP extends HTTP {
    private static final String TAG = "StandardHTTP";

    public static final Exception requestMethodException = new Exception();

    private static boolean deviceMaySupportTls12 = true;

    private HttpURLConnection conn;
    private HttpConnection modernConn;

    public StandardHTTP(String method, String url) {
        super(method, url);
    }

    protected InputStream makeRequest() throws Exception {
        // Try to make this request using platform native HTTP stack if possible
        if (deviceMaySupportTls12 || !url.startsWith("https://")) {
            try {
                InputStream result = makePlatformRequest();
                Log.d(TAG, method + " " + url + " -> HTTP " + responseCode + " (platform)");
                return result;
            }
            catch (SSLHandshakeException e) {
                // Fallback to Modernconnector for older Android versions
                Log.w(TAG, method + " " + url + " failed on platform HTTP stack, " +
                    "falling back to ModernConnector", e);

                deviceMaySupportTls12 = false;
                closeTransport();
            }
        }

        InputStream result = makeModernConnectorRequest();
        Log.d(TAG, method + " " + url + " -> HTTP " + responseCode +
            " (ModernConnector)");
        return result;
    }

    private InputStream makePlatformRequest() throws Exception {
        String currentUrl = url;
        String currentMethod = method;

        for (int redirects = 0; redirects < 5; redirects++) {
            URL parsed = new URL(currentUrl);
            conn = (HttpURLConnection) parsed.openConnection();

            // Enable TLS 1.2 on Android 4.1-4.4 for HTTPS requests.
            // Plain HTTP requests (e.g. media proxy, user-configured API proxy) are not affected.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN &&
                Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT &&
                conn instanceof HttpsURLConnection) {

                ((HttpsURLConnection) conn).setSSLSocketFactory(new TLSSocketFactory());
            }

            conn.setInstanceFollowRedirects(false);

            try {
                conn.setRequestMethod(currentMethod);
            }
            catch (Exception e) {
                Log.w(TAG, "Platform HTTP stack does not support method " + currentMethod, e);
                throw requestMethodException;
            }

            for (Enumeration e = requestHeaders.keys(); e.hasMoreElements(); ) {
                String key = (String) e.nextElement();
                String value = (String) requestHeaders.get(key);
                conn.setRequestProperty(key, value);
            }

            if (data != null) {
                conn.setDoOutput(true);
                conn.setFixedLengthStreamingMode(data.length);
                OutputStream os = conn.getOutputStream();
                os.write(data);
                os.close();
            }

            responseCode = conn.getResponseCode();
            responseHeaders.clear();
            for (int i = 0; ; i++) {
                String key = conn.getHeaderFieldKey(i);
                String value = conn.getHeaderField(i);
                if (key == null && value == null) break;
                if (key == null) continue;
                responseHeaders.put(key.toLowerCase(), value);
            }

            if (responseCode >= 300 && responseCode < 400) {
                String location = (String) responseHeaders.get("location");
                conn.disconnect();
                if (location == null) break;

                currentUrl = new URL(parsed, location).toString();
                continue;
            }
            break;
        }

        url = currentUrl;
        method = currentMethod;

        if (responseCode >= 400) {
            InputStream err = conn.getErrorStream();
            if (err == null) {
                try {
                    err = conn.getInputStream();
                }
                catch (IOException e) {
                    Log.d(TAG, "No input stream for HTTP " + responseCode + ", using empty body");
                    err = new ByteArrayInputStream(new byte[0]);
                }
            }
            if (err == null) err = new ByteArrayInputStream(new byte[0]);
            return err;
        }
        return conn.getInputStream();
    }

    private InputStream makeModernConnectorRequest() throws Exception {
        String currentUrl = url;

        for (int redirects = 0; redirects < 5; redirects++) {
            HttpConnection hc = (HttpConnection) ModernConnector.open(currentUrl);

            try {
                try {
                    hc.setRequestMethod(method);
                }
                catch (Exception e) {
                    Log.w(TAG, "ModernConnector does not support method " + method, e);
                    throw requestMethodException;
                }

                for (Enumeration e = requestHeaders.keys(); e.hasMoreElements(); ) {
                    String key = (String) e.nextElement();
                    String value = (String) requestHeaders.get(key);
                    hc.setRequestProperty(key, value);
                }

                if (data != null) {
                    OutputStream os = hc.openOutputStream();
                    os.write(data);
                    os.close();
                }

                responseCode = hc.getResponseCode();
                responseHeaders.clear();
                for (int i = 0; ; i++) {
                    String key = hc.getHeaderFieldKey(i);
                    String value = hc.getHeaderField(i);
                    if (key == null && value == null) break;
                    if (key == null) continue;
                    responseHeaders.put(key.toLowerCase(), value);
                }

                String location = null;
                if (responseCode >= 300 && responseCode < 400) {
                    location = hc.getHeaderField("Location");
                }

                if (location == null) {
                    modernConn = hc;
                    url = currentUrl;
                    return hc.openInputStream();
                }

                hc.close();
                currentUrl = new URL(new URL(currentUrl), location).toString();
            }
            catch (Exception e) {
                try {
                    hc.close();
                }
                catch (Exception ignored) {
                    Log.d(TAG, "Failed to close ModernConnector request after error", ignored);
                }
                throw e;
            }
        }

        throw new IOException("too many redirects");
    }

    protected void closeTransport() {
        if (conn != null) {
            conn.disconnect();
            conn = null;
        }
        if (modernConn != null) {
            try {
                modernConn.close();
            }
            catch (IOException e) {
                Log.d(TAG, "Failed to close ModernConnector connection", e);
            }
            modernConn = null;
        }
    }
}
