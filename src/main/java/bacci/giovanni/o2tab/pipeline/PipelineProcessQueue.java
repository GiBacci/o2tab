package bacci.giovanni.o2tab.pipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import bacci.giovanni.o2tab.process.CallableProcess;

/**
 * Queue for pipeline process
 * 
 * @author <a href="http://www.unifi.it/dblage/CMpro-v-p-65.html">Giovanni
 *         Bacci</a>
 *
 */
public abstract class PipelineProcessQueue implements Runnable {

	/**
	 * Process list
	 */
	protected List<PipelineProcess> processList;

	/**
	 * The main output directory
	 */
	private String outputDir = null;

	/**
	 * Process logger
	 */
	private Logger logger;

	/**
	 * Constructs a queue with the specified queue size and the logger
	 * 
	 * @param queueSize
	 *            the initial size of the queue
	 * @param logger
	 *            the logger
	 */
	public PipelineProcessQueue(int queueSize, Logger logger) {
		this.processList = new ArrayList<PipelineProcess>(queueSize);
		this.logger = logger;
	}

	/**
	 * Constructs a queue with the specified queue size and a <code>null</code>
	 * logger
	 * 
	 * @param queueSize
	 *            the initial size of the queue
	 */
	public PipelineProcessQueue(int queueSize) {
		this(queueSize, null);
	}

	/**
	 * Constructs a queue with the a queue size of 16 and the specified logger
	 * 
	 * @param logger
	 *            the logger
	 */
	public PipelineProcessQueue(Logger logger) {
		this(16, logger);
	}

	/**
	 * Creates a queue with the a queue size of 16 and a <code>null</code>
	 * logger
	 */
	public PipelineProcessQueue() {
		this(16, null);
	}

	/**
	 * Add a process to the pipeline
	 * 
	 * @param process
	 *            th process
	 */
	public void addPipelineProcess(PipelineProcess process) {
		if(outputDir != null)
			process.setOutputDir(outputDir);
		if (processList.indexOf(process) < 0)
			processList.add(process);
	}

	/**
	 * Remove a process to the pipeline
	 * 
	 * @param process
	 *            the process
	 */
	public void removePipelineProcess(CallableProcess process) {
		if (processList.indexOf(process) >= 0)
			processList.remove(process);
	}

	/**
	 * @return the number of process stored in the queue
	 */
	public int processNumber() {
		return processList.size();
	}

	/**
	 * the main output directory for this queue. All the process added to the
	 * queue will have the same main output directory
	 * 
	 * @param outputDir
	 *            the output directory
	 */
	public void setMainOutputDir(String outputDir) {
		this.outputDir = outputDir;
	}

	/**
	 * @param level
	 * @param msg
	 * @see java.util.logging.Logger#log(java.util.logging.Level,
	 *      java.lang.String)
	 */
	public void log(Level level, String msg) {
		if (logger != null) {
			logger.log(level, msg);
		} else {
			String error = String.format("%s error: %s", level.getName(), msg);
			System.err.println(error);
		}
	}

	/**
	 * @param level
	 * @param msg
	 * @param thrown
	 * @see java.util.logging.Logger#log(java.util.logging.Level,
	 *      java.lang.String, java.lang.Throwable)
	 */
	public void log(Level level, String msg, Throwable thrown) {
		if (logger != null) {
			logger.log(level, msg, thrown);
		} else {
			String error = String.format("%s error: %s%n    throwed: %s",
					level.getName(), msg, thrown.getClass().getName());
			System.err.println(error);
		}
	}

}
