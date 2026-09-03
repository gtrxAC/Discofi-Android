package fi.gtrxac.bluewap.http;

import java.io.*;

import util.Base64;

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
        if (requestHeaders.get("X-Super-Properties") == null) {
            responseCode = 400;
            return "{\"message\": \"Super properties missing\", \"code\": 0}";
        }

        try {
            Base64.decodeFast(requestHeaders.get("X-Super-Properties"));
        }
        catch (Exception e) {
            responseCode = 400;
            return "{\"message\": \"Super properties invalid: '" + e.toString() + "'\", \"code\": 0}";
        }
		}

		responseCode = 404;
		return "{\"message\": \"Unknown route '" + url + "'\", \"code\": 0}";
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
