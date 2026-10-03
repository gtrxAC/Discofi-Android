package fi.gtrxac.bluewap.http;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Enumeration;

public class StandardHTTP extends HTTP {
	private HttpURLConnection conn;

	public StandardHTTP(String method, String url) {
		super(method, url);
	}

	protected InputStream makeRequest() throws Exception {
		String currentUrl = url;
		String currentMethod = method;

		for (int redirects = 0; redirects < 5; redirects++) {
			URL parsed = new URL(currentUrl);
			conn = (HttpURLConnection) parsed.openConnection();
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
			readResponseHeaders();

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

	private void readResponseHeaders() {
		responseHeaders.clear();
		for (int i = 0; ; i++) {
			String key = conn.getHeaderFieldKey(i);
			String value = conn.getHeaderField(i);
			if (key == null && value == null) break;
			if (key == null) continue;
			responseHeaders.put(key.toLowerCase(), value);
		}
	}

	protected void closeTransport() {
		if (conn != null) {
			conn.disconnect();
			conn = null;
		}
	}
}
