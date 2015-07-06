package bacci.giovanni.o2tab.pipeline;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;
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

	public static final String OUT_FOLDER = "o2tab_output";

	/**
	 * Process list
	 */
	protected Set<PipelineProcess> processList;

	/**
	 * The main output directory
	 */
	private String outputDir;

	/**
	 * Process logger
	 */
	private Logger logger;

	/**
	 * Constructs a queue with the specified queue size and the logger. The
	 * output directory here specified will be the folder in which the main
	 * outputdirectory will be created.
	 * 
	 * @param queueSize
	 *            the initial size of the queue
	 * @param logger
	 *            the logger
	 * @param the
	 *            output directory
	 * @throws IOException
	 *             if an I/O error occurs creating output folder
	 */
	public PipelineProcessQueue(int queueSize, Logger logger, String output)
			throws IOException {
		this.processList = new LinkedHashSet<PipelineProcess>(queueSize);
		this.logger = logger;
		this.checkOutputFolder(output);
	}

	/**
	 * Constructs a queue with the specified queue size and the logger. Output
	 * files will be saved in the same folder in which the program is started.
	 * 
	 * @param queueSize
	 *            the initial size of the queue
	 * @param logger
	 *            the logger
	 * @throws IOException
	 *             if an I/O error occurs creating output folder
	 */
	public PipelineProcessQueue(int queueSize, Logger logger)
			throws IOException {
		this.processList = new LinkedHashSet<PipelineProcess>(queueSize);
		this.logger = logger;
		Path out = Paths.get(System.getProperty("user.dir"));
		this.checkOutputFolder(out.getParent().toString());
	}

	/**
	 * Constructs a queue with the specified queue size and a <code>null</code>
	 * logger
	 * 
	 * @param queueSize
	 *            the initial size of the queue
	 * @throws IOException
	 *             if an I/O error occurs creating output folder
	 */
	public PipelineProcessQueue(int queueSize) throws IOException {
		this(queueSize, null);
	}

	/**
	 * Constructs a queue with the a queue size of 16 and the specified logger
	 * 
	 * @param logger
	 *            the logger
	 * @throws IOException
	 *             if an I/O error occurs creating output folder
	 */
	public PipelineProcessQueue(Logger logger) throws IOException {
		this(16, logger);
	}

	/**
	 * Creates a queue with the a queue size of 16 and a <code>null</code>
	 * logger
	 * 
	 * @throws IOException
	 *             if an I/O error occurs creating output folder
	 */
	public PipelineProcessQueue() throws IOException {
		this(16, null);
	}

	/**
	 * @param output
	 *            the output directory
	 * @throws IOException
	 *             if an I/O error occurs creating the folder
	 */
	private void checkOutputFolder(String output) throws IOException {
		Path p = Paths.get(output);
		if (!Files.isDirectory(p))
			throw new NoSuchFileException(p.toString());
		Path out = p.resolve(OUT_FOLDER);
		if (!Files.isDirectory(out))
			Files.createDirectory(out);
		this.outputDir = out.toString();
	}

	/**
	 * Add a process to the pipeline
	 * 
	 * @param process
	 *            th process
	 */
	public void addPipelineProcess(PipelineProcess process) {
		process.setMainOutputDir(outputDir);
		processList.add(process);
	}

	/**
	 * Remove a process to the pipeline
	 * 
	 * @param process
	 *            the process
	 */
	public void removePipelineProcess(CallableProcess process) {
		if (processList.contains(process))
			processList.remove(process);
	}

	/**
	 * @return the number of process stored in the queue
	 */
	public int processNumber() {
		return processList.size();
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

	/**
	 * @return the outputDir
	 */
	public String getOutputDir() {
		return outputDir;
	}

}
