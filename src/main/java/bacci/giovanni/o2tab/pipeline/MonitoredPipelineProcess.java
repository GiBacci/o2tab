package bacci.giovanni.o2tab.pipeline;

import java.io.IOException;

import bacci.giovanni.o2tab.pipeline.ProcessResult.PipelineResult;

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
	private ProcessResult res;

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
		super(process.getProcessType(), null);
		this.process = process;
	}

	@Override
	public ProcessResult launch() throws IOException {
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
				Thread.sleep(400);
			}
			System.out.println(formatMsg(WAIT[0], false));
		} catch (InterruptedException e) {
			res = new ProcessResult(PipelineResult.INTERRUPTED);
			t.interrupt();
		}

		this.checkException();

		this.displayResultMessage(res);

		return res;
	}

	private void displayResultMessage(ProcessResult res) {
		switch (res.getRes()) {
		case PASSED:
			System.out.println("[PASS] process finished correctly");
			break;
		case PASSED_WITH_WARNINGS:
			System.out.println("[PASS] process finished with warnings:");
			for (String w : res.getWarnings())
				System.out.println(" [WARN] " + w);
			break;
		case FAILED:
			System.out.println("[FAIL] process failed: ");
			for (String e : res.getFails())
				System.out.println(" [ERROR] " + e);
			break;
		case FAILED_WITH_WARNINGS:
			System.out.println("[FAIL] process failed with warnings: ");
			for (String e : res.getFails())
				System.out.println(" [ERROR] " + e);
			for (String w : res.getWarnings())
				System.out.println(" [WARN] " + w);
			break;
		case INTERRUPTED:
			System.out.println("[INTERRUPTED] process has been interrupted");
			break;
		default:
			break;
		}
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
			return String.format("[INFO] %s process started %s\r",
					this.process.getProcessType(), wait);
		} else {
			return String.format("[INFO] %s process started %s",
					this.process.getProcessType(), wait);
		}
	}

}
