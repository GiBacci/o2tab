package bacci.giovanni.o2tab.pipeline;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Pipeline process
 * 
 * @author <a href="http://www.unifi.it/dblage/CMpro-v-p-65.html">Giovanni
 *         Bacci</a>
 *
 */
public abstract class PipelineProcess {

	/**
	 * Input file list
	 */
	private List<String> inputFiles = null;

	/**
	 * Output file list
	 */
	private List<String> outputFiles;

	/**
	 * Output directory
	 */
	private String outputDir;

	/**
	 * Name of the process
	 */
	private ProcessType processType;

	/**
	 * Constructor
	 */
	protected PipelineProcess(ProcessType processType) {
		this.processType = processType;
		this.outputFiles = new ArrayList<String>();
		this.outputDir = PipelineProcess.getDefaultOutput();
	}

	/**
	 * @return the default output path for all processes
	 */
	public static String getDefaultOutput() {
		return Paths.get(System.getProperty("user.dir")).resolve("output")
				.toString();
	}

	/**
	 * Set the input files and return the pipeline process
	 * 
	 * @param inputFiles
	 *            the input files
	 * @return this pipeline process
	 * @throws FileNotFoundException
	 *             if one (or more) file does not exist
	 * @throws NullPointerException
	 *             if the input files are <code>null</code>
	 */
	public PipelineProcess setInputFile(List<String> inputFiles)
			throws FileNotFoundException {
		if (inputFiles == null)
			throw new NullPointerException();
		for (String s : inputFiles)
			if (!Files.exists(Paths.get(s)))
				throw new FileNotFoundException();
		this.inputFiles = inputFiles;
		return this;
	}

	/**
	 * @return the output file list
	 */
	public List<String> getOutputFiles() {
		return outputFiles;
	}

	/**
	 * Sets the output directory for this process
	 * 
	 * @param output
	 *            the output directory
	 */
	protected void setOutputDir(String output) {
		this.outputDir = output;
	}

	/**
	 * @return the output directory
	 */
	protected String getOutputDir() {
		return outputDir;
	}

	/**
	 * Adds ad output file to the output file list
	 * 
	 * @param outputFile
	 *            the output file
	 */
	protected void addOuptuFile(String outputFile) {
		outputFiles.add(outputFile);
	}

	/**
	 * @return the input files
	 */
	protected List<String> getInputFiles() {
		return inputFiles;
	}

	/**
	 * @return the processName
	 */
	public ProcessType getProcessType() {
		return processType;
	}

	/**
	 * Launch this process and return a {@link PipelineResult}
	 * 
	 * @return {@link PipelineResult#PASSED} if the process has done everithing
	 *         correctly, otherwise this method will return
	 *         {@link PipelineResult#FAILED}
	 * @throws Exception
	 *             if an Exception occurs
	 */
	public abstract PipelineResult launch() throws Exception;

	/**
	 * Pipeline result tokens
	 * 
	 * @author <a href="http://www.unifi.it/dblage/CMpro-v-p-65.html">Giovanni
	 *         Bacci</a>
	 *
	 */
	public enum PipelineResult {
		FAILED, PASSED;
	}

}
