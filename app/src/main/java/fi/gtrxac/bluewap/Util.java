package fi.gtrxac.bluewap;

import java.io.*;
import java.util.*;

public class Util {
    // _________________________________________________________________________
    //
    //  Text encoding
    // _________________________________________________________________________
    //

	public static byte[] stringToBytes(String str) {
		return stringToBytes(str, null);
	}

	public static byte[] stringToBytes(String str, String charset) {
		if (charset == null || charset.length() == 0) {
			charset = "UTF-8";
		}
		try {
			return str.getBytes(charset);
		}
		catch (Exception e) {
			return str.getBytes();
		}
	}

	public static String bytesToString(byte[] bytes) {
		return bytesToString(bytes, null);
	}

	public static String bytesToString(byte[] bytes, String charset) {
		if (charset == null || charset.length() == 0) {
			charset = "UTF-8";
		}
		try {
			return new String(bytes, charset);
		}
		catch (Exception e) {
			return new String(bytes);
		}
	}

	/**
	 * Get the name of an (ASCII-compatible) encoding that is supported by the platform, preferring the encoding given as input.
	 */
    public static String getSupportedEncoding(String preferredEncoding) {
        String[] testEncs = { preferredEncoding, "UTF-8", "ISO-8859-1", "US_ASCII" };
		byte[] testBytes = "a".getBytes();

        for (int i = 0; i < testEncs.length; i++) {
            try {
				new String(testBytes, testEncs[i]);
                return testEncs[i];
            }
            catch (UnsupportedEncodingException e) {}
        }
        return "ASCII";  // last resort
    }

	/**
	 * Get the character set/encoding specified in an HTTP Content-Type header.
	 */
	public static String getCharsetFromContentType(String contentType) {
		if (contentType == null) return null;

		int charsetIndex = contentType.toLowerCase().indexOf("charset=");
		if (charsetIndex == -1) return null;

		int start = charsetIndex + "charset=".length();
		int end = contentType.length();
		int semicolonIndex = contentType.indexOf(';', start);
		if (semicolonIndex != -1) end = semicolonIndex;

		String charset = contentType.substring(start, end).trim();
		if (charset.length() > 1 && charset.startsWith("\"") && charset.endsWith("\"")) {
			charset = charset.substring(1, charset.length() - 1);
		}
		return charset;
	}

    // _________________________________________________________________________
    //
    //  String utilities
    // _________________________________________________________________________
    //

	public static String replace(String str, String from, String to) {
		int j = str.indexOf(from);
		if (j == -1)
			return str;
		final StringBuffer sb = new StringBuffer();
		int k = 0;
		for (int i = from.length(); j != -1; j = str.indexOf(from, k)) {
			sb.append(str.substring(k, j)).append(to);
			k = j + i;
		}
		sb.append(str.substring(k, str.length()));
		return sb.toString();
	}

	public static String[] split(String str, String delimiter) {
		if (str == null || str.length() == 0) {
			return new String[0];
		}
		Vector parts = _split(str, delimiter);
		String[] result = new String[parts.size()];
		parts.copyInto(result);
		return result;
	}

	public static Vector splitVec(String str, String delimiter) {
		if (str == null || str.length() == 0) {
			return new Vector();
		}
		return _split(str, delimiter);
	}

	private static Vector _split(String str, String delimiter) {
		Vector parts = new Vector();
		int start = 0;
		int index;
		
		while ((index = str.indexOf(delimiter, start)) != -1) {
			parts.addElement(str.substring(start, index));
			start = index + delimiter.length();
		}
		
		// Add the last part
		parts.addElement(str.substring(start));

		return parts;
	}

    public static String sanitizeWml(String text) {
        text = Util.replace(text, "&", "&amp;");
        text = Util.replace(text, "'", "&apos;");
        text = Util.replace(text, "\"", "&quot;");
        text = Util.replace(text, "<", "&lt;");
        return Util.replace(text, ">", "&gt;");
    }


    // _________________________________________________________________________
    //
    //  Misc
    // _________________________________________________________________________
    //

	// https://github.com/gtrxAC/discord-j2me/pull/5/commits/193c63f6a00b8e24da7a3582e9d1a92522f9940e
	public static byte[] readBytes(InputStream inputStream, int initialSize, int bufferSize, int expandSize) throws IOException {
		if (initialSize <= 0) initialSize = bufferSize;
		byte[] buf = new byte[initialSize];
		int count = 0;
		byte[] readBuf = new byte[bufferSize];
		int readLen;
		while ((readLen = inputStream.read(readBuf)) != -1) {
			if(count + readLen > buf.length) {
				byte[] newbuf = new byte[count + expandSize];
				System.arraycopy(buf, 0, newbuf, 0, count);
				buf = newbuf;
			}
			System.arraycopy(readBuf, 0, buf, count, readLen);
			count += readLen;
		}
		if(buf.length == count) {
			return buf;
		}
		byte[] res = new byte[count];
		System.arraycopy(buf, 0, res, 0, count);
		return res;
	}

	public static byte[] readBytes(InputStream inputStream) throws IOException {
		return readBytes(inputStream, 0, 1024, 2048);
	}

	public static void sleep(int ms) {
		try {
			Thread.sleep(ms);
		}
		catch (Exception e) {}
	}
}