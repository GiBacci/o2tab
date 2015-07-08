package bacci.giovanni.o2tab.pipeline;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

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
			System.out.print(formatMsg(WAIT[WAIT.length - 1], false));
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
			System.out.println("[PASS]");
			break;
		case PASSED_WITH_WARNINGS:
			System.out.println("[PASS]");
			for (String w : res.getWarnings())
				System.out.println(" [WARN] " + w);
			break;
		case FAILED:
			System.out.println("[FAIL]");
			for (String e : res.getFails())
				System.out.println(" [ERROR] " + e);
			break;
		case FAILED_WITH_WARNINGS:
			System.out.println("[FAIL]");
			for (String e : res.getFails())
				System.out.println(" [ERROR] " + e);
			for (String w : res.getWarnings())
				System.out.println(" [WARN] " + w);
			break;
		case INTERRUPTED:
			System.out.println("[INTERRUPTED]");
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
		String msg = "[ERROR]";
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
			return String.format("[INFO] %s process started%s\r",
					this.process.getProcessType(), wait);
		} else {
			return String.format("[INFO] %s process started%s",
					this.process.getProcessType(), wait);
		}
	}



	/**
	 * @param inputFiles
	 * @return
	 * @see bacci.giovanni.o2tab.pipeline.PipelineProcess#setInputFiles(java.util.Collection)
	 */
	public PipelineProcess setInputFiles(Collection<String> inputFiles) {
		return process.setInputFiles(inputFiles);
	}



	/**
	 * @return
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return process.hashCode();
	}



	/**
	 * @return
	 * @see bacci.giovanni.o2tab.pipeline.PipelineProcess#getOutputFiles()
	 */
	public List<String> getOutputFiles() {
		return process.getOutputFiles();
	}



	/**
	 * @param output
	 * @see bacci.giovanni.o2tab.pipeline.PipelineProcess#setMainOutputDir(java.lang.String)
	 */
	public void setMainOutputDir(String output) {
		process.setMainOutputDir(output);
	}



	/**
	 * @param number
	 * @return
	 * @see bacci.giovanni.o2tab.pipeline.PipelineProcess#setProcessNumber(int)
	 */
	public PipelineProcess setProcessNumber(int number) {
		return process.setProcessNumber(number);
	}



	/**
	 * @return
	 * @see bacci.giovanni.o2tab.pipeline.PipelineProcess#getProcessType()
	 */
	public ProcessType getProcessType() {
		return process.getProcessType();
	}



	/**
	 * @param obj
	 * @return
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		return process.equals(obj);
	}



	/**
	 * @return
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return process.toString();
	}

}
