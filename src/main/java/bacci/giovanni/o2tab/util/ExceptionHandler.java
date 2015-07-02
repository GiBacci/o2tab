package bacci.giovanni.o2tab.util;

import java.util.concurrent.ExecutorService;

/**
 * Exception handler for ExecutorSevice.
 * 
 * @author <a href="http://www.unifi.it/dblage/CMpro-v-p-65.html">Giovanni
 *         Bacci</a>
 *
 * @param <T>
 *            the type of exception to handle
 */
public class ExceptionHandler<T extends Exception> {

	/**
	 * The executo service instance
	 */
	private ExecutorService ex;

	/**
	 * The exception
	 */
	private T e = null;

	/**
	 * Constructor
	 * 
	 * @param ex
	 *            the executo service
	 */
	public ExceptionHandler(ExecutorService ex) {
		this.ex = ex;
	}

	/**
	 * If an execption has been thrown the executo service is terminated
	 * 
	 * @param e
	 *            the exception
	 */
	public void sendException(T e) {
		this.e = e;
		ex.shutdownNow();
	}

	/**
	 * If an exception has been thrown this method will thorwn that exception
	 * 
	 * @throws T
	 *             the exception
	 */
	public void throwIfAny() throws T {
		if (this.e != null)
			throw this.e;
	}

}
