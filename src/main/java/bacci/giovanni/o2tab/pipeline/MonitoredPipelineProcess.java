package bacci.giovanni.o2tab.pipeline;

import java.io.IOException;

/**
 * Pipeline process wrapper.
 * 
 * @author <a href="http://www.unifi.it/dblage/CMpro-v-p-65.html">Giovanni
 *         Bacci</a>
 *
 */
public class MonitoredPipelineProcess extends PipelineProcess {

	/**
	 * The process
	 */
	private final PipelineProcess process;

	/**
	 * The result
	 */
	private PipelineResult res;

	/**
	 * A possible IOExcpetion
	 */
	private IOException ioex = null;

	/**
	 * String array for monitoring the process
	 */
	private final static String[] WAIT = { "   ", ".  ", ".. ", "..." };

	/**
	 * Constructor
	 * 
	 * @param process
	 *            the process to wrap
	 */
	public MonitoredPipelineProcess(PipelineProcess process) {
		super(process.getProcessType());
		this.process = process;
	}

	@Override
	public PipelineResult launch() throws IOException {
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					res = process.launch();
				} catch (IOException e) {
					ioex = e;
				}
			}
		});
		t.start();
		int index = 0;

		try {
			while (t.isAlive()) {
				System.out.print(formatMsg(WAIT[index++], true));
				if (index == WAIT.length)
					index = 0;
				Thread.sleep(200);
			}
			System.out.print(formatMsg(WAIT[WAIT.length - 1], false));
		} catch (InterruptedException e) {
			System.out.println(" process interrupted");
			t.interrupt();
		}

		this.checkException();

		switch (res) {
		case PASSED:
			System.out.println(" process finished correctly");
			break;
		case INTERRUPTED:
			System.out.println(" process interrupted");
			break;
		case FAILED:
			System.out.println(" process fail");
			break;
		default:
			break;
		}
		
		return res;
	}

	/**
	 * Checks if the exceptions are not <code>null</code> throwing them
	 * 
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	private void checkException() throws IOException {
		String msg = " process exited with error/s";
		if (ioex != null) {
			System.out.println(msg);
			throw ioex;
		}
	}

	/**
	 * Formats monitoring string
	 * 
	 * @param wait
	 *            the string
	 * @param carriage
	 *            if the carriage return character has to be placed at the end
	 *            of the string
	 * @return a formatted string
	 */
	private String formatMsg(String wait, boolean carriage) {
		if (carriage) {
			return String.format("%s process started %s\r",
					this.process.getProcessType(), wait);
		} else {
			return String.format("%s process started %s",
					this.process.getProcessType(), wait);
		}
	}

}
