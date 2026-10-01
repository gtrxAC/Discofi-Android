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

		if (url.equals("https://discord.com/api/v9/users/@me")) {
			return "";  // Add your API mock here
		}
		if (url.equals("https://discord.com/api/v9/users/@me/guilds")) {
			return "";  // Add your API mock here
		}
		if (url.startsWith("https://discord.com/api/v9/guilds/") && url.endsWith("/channels")) {
			return "";  // Add your API mock here
        }
        if (url.equals("https://discord.com/api/v9/users/@me/channels")) {
			return "";  // Add your API mock here
        }
		if (url.equals("https://discord.com/api/v9/experiments?with_guild_experiments=true")) {
			return "{\"fingerprint\":\"FINGERPRINT\",\"assignments\":[]}";
		}
		if (url.equals("https://discord.com/api/v9/auth/login")) {
            if (!"FINGERPRINT".equals(requestHeaders.get("X-Fingerprint"))) {
                responseCode = 401;
                return "{\"message\": \"Invalid fingerprint: '" + requestHeaders.get("X-Fingerprint") + "'\", \"code\": 0}";
            }

			return "{\"user_id\":\"398878727938899977\",\"mfa\":true,\"sms\":true,\"ticket\":\"TICKET\",\"backup\":true,\"totp\":true,\"webauthn\":null,\"login_instance_id\":\"LOGIN_INSTANCE_ID\"}";
		}
		if (url.equals("https://discord.com/api/v9/auth/mfa/totp")) {
			return "{\"token\":\"TOKEN\",\"user_settings\":{\"locale\":\"en-GB\",\"theme\":\"midnight\"}}";
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
