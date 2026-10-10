package fi.gtrxac.bluewap.http;

import android.os.Build;

import java.io.*;
import java.net.URL;
import java.util.Enumeration;

import javax.microedition.io.HttpConnection;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLException;

import tech.alicesworld.ModernConnector.ModernConnector;

public class StandardHTTP extends HTTP {
	private HttpsURLConnection conn;
	private HttpConnection modernConn;

	public StandardHTTP(String method, String url) {
		super(method, url);
	}

	protected InputStream makeRequest() throws Exception {
		try {
			return makePlatformRequest();
		}
		catch (Exception e) {
			// Fallback to Modernconnector for older Android versions
			closeTransport();
			return makeModernConnectorRequest();
		}
	}

	private InputStream makePlatformRequest() throws Exception {
		String currentUrl = url;
		String currentMethod = method;

		for (int redirects = 0; redirects < 5; redirects++) {
			URL parsed = new URL(currentUrl);
			conn = (HttpsURLConnection) parsed.openConnection();
			// Enable TLS 1.2 on Android 4.1-4.4
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN &&
			    Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
				conn.setSSLSocketFactory(new TLSSocketFactory());
			}
			conn.setInstanceFollowRedirects(false);
			conn.setRequestMethod(currentMethod);

			for (Enumeration e = requestHeaders.keys(); e.hasMoreElements();) {
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
				hc.setRequestMethod(method);

				for (Enumeration e = requestHeaders.keys(); e.hasMoreElements();) {
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
				catch (Exception ignored) {}
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
			catch (IOException e) {}
			modernConn = null;
		}
	}
}
