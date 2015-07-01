package bacci.giovanni.o2tab.exceptions;

import java.io.IOException;

/**
 * Signals that a config file is not well formatted
 * 
 * @author <a href="http://www.unifi.it/dblage/CMpro-v-p-65.html">Giovanni
 *         Bacci</a>
 *
 */
public class BadConfigFileFormatException extends IOException {

	/**
	 * Automatically generated serial
	 */
	private static final long serialVersionUID = -2066442900151572015L;

	public BadConfigFileFormatException() {
		super();
	}

	public BadConfigFileFormatException(String message, Throwable cause) {
		super(message, cause);
	}

	public BadConfigFileFormatException(String message) {
		super(message);
	}

	public BadConfigFileFormatException(Throwable cause) {
		super(cause);
	}

}
