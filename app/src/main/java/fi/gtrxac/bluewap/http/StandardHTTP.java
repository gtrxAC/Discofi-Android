package fi.gtrxac.bluewap.http;

import fi.gtrxac.bluewap.*;
import java.io.*;
import java.util.*;

public class StandardHTTP extends HTTP {
	public static final Exception redirectException = new Exception();

	private OutputStream os;

	public StandardHTTP(String method, String url) {
		super(method, url);
	}

	private InputStream makeRequestFinal(String finalUrl) throws Exception {
		

//#ifndef NO_HTTP_REDIRECT_SUPPORT
		if (responseCode >= 300 && responseCode < 400) {
			String redir = hc.getHeaderField("Location");
			if (redir == null) throw new Exception("received redirect with no location");

			url = new URL(redir, new URL(url)).toString(false);
			throw redirectException;
		}
//#endif
		return result;
	}

	private InputStream makeRequestWithCloseOnError(String finalUrl) throws Exception {
		try {
			return makeRequestFinal(finalUrl);
		}
		catch (Exception e) {
			close();
			throw e;
		}
	}

	protected InputStream makeRequest() throws Exception {
		for (int redirs = 0; redirs < 5; redirs++) {
			try {
				return makeRequestWithCloseOnError();
			}
			catch (Exception e) {
				if (e != redirectException) throw e;
			}
		}
		throw new Exception("too many redirects");
	}

	protected void closeTransport() {
	}

	public Hashtable getResponseHeaders() throws Exception {
		checkMakeRequest();
		
	}

	/**
	 * Get the value of a HTTP response header sent by the server.
	 */
	public String getResponseHeader(String name) throws Exception {
		checkMakeRequest();
		
	}
}
