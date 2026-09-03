package fi.gtrxac.bluewap.http;

import java.io.*;

public class StandardHTTP extends HTTP {

	public StandardHTTP(String method, String url) {
		super(method, url);
	}

	private String getMockResponse() {
		responseCode = 200;
		responseHeaders.put("Content-Type", "application/json");

		if (requestHeaders.get("Authorization") == null) {
			responseCode = 401;
			return "{\"message\": \"401: Unauthorized\", \"code\": 0}";
		}
		if (url.equals("https://discord.com/api/v9/")) {
			return "";
		}

		responseCode = 404;
		return "";
	}

	protected InputStream makeRequest() throws Exception {
		try {
			Thread.sleep(1000);
		}
		catch (Exception e) {}

		String resp = getMockResponse();
		return new ByteArrayInputStream(resp.getBytes());
	}

	protected void closeTransport() {
	}
}
