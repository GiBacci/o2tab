package bacci.giovanni.o2tab.exceptions;

import java.io.IOException;

/**
 * Signals that a pipeline process has received a number of input files
 * different from the one required
 * 
 * @author <a href="http://www.unifi.it/dblage/CMpro-v-p-65.html">Giovanni
 *         Bacci</a>
 *
 */
public class WrongInputFileNumberException extends IOException {

	/**
	 * Automatically generated serial
	 */
	private static final long serialVersionUID = -6614115847075433034L;

	/**
	 * The error message
	 */
	private static final String ERROR = "Wrong file number: %d but %d found";

	/**
	 * Number of files expected
	 */
	private int expected;

	/**
	 * Number of files received
	 */
	private int found;

	/**
	 * @param expected
	 *            number of files expected
	 * @param found
	 *            number of files received
	 */
	public WrongInputFileNumberException(int expected, int found) {
		super();
		this.expected = expected;
		this.found = found;
	}

	/**
	 * 
	 * @param cause
	 *            the cause of the exception
	 * @param expected
	 *            number of files expected
	 * @param found
	 *            number of files received
	 */
	public WrongInputFileNumberException(Throwable cause, int expected,
			int found) {
		super(cause);
		this.expected = expected;
		this.found = found;
	}

	@Override
	public String getMessage() {
		return String.format(ERROR, expected, found);
	}
}
