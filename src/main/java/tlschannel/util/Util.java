package tlschannel.util;

import javax.net.ssl.SSLEngineResult;

public class Util {

	public static void assertTrue(final boolean condition) {
		if (!condition)
			throw new AssertionError();
	}

	/**
	 * Convert a {@link SSLEngineResult} into a {@link String}, this is needed
	 * because the supplied method includes a log-breaking newline.
	 */
	public static String resultToString(final SSLEngineResult result) {
		return String.format("status=%s,handshakeStatus=%s,bytesConsumed=%d,bytesConsumed=%d", result.getStatus(),
				result.getHandshakeStatus(), result.bytesProduced(), result.bytesConsumed());
	}

}
