package bacci.giovanni.o2tab.pipeline;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Pipeline process
 * 
 * @author <a href="http://www.unifi.it/dblage/CMpro-v-p-65.html">Giovanni
 *         Bacci</a>
 *
 */
public abstract class PipelineProcess {

	protected final static String DEFAULT_OUT = Paths
			.get(System.getProperty("user.dir")).resolve("output").toString();

	/**
	 * Input file list
	 */
	private Set<String> inputFiles = null;

	/**
	 * Output file list
	 */
	private Set<String> outputFiles;

	/**
	 * Output directory
	 */
	private String mainOutputDir;

	/**
	 * Name of the process
	 */
	private ProcessType processType;

	/**
	 * Sub folder for output files
	 */
	private String subDir;

	/**
	 * Process number in the pipeline
	 */
	private int processNumber = -1;

	/**
	 * Constructor
	 */
	protected PipelineProcess(ProcessType processType, String subDir) {
		this.processType = processType;
		this.outputFiles = new LinkedHashSet<String>();
		this.subDir = subDir;
		this.mainOutputDir = System.getProperty("user.dir");
	}

	/**
	 * Set the input files and return the pipeline process
	 * 
	 * @param inputFiles
	 *            the input files
	 * @return this pipeline process
	 */
	public PipelineProcess setInputFiles(Collection<String> inputFiles) {
		this.inputFiles = new LinkedHashSet<String>(inputFiles);
		return this;
	}

	/**
	 * @return the output file list
	 */
	public List<String> getOutputFiles() {
		return new ArrayList<String>(outputFiles);
	}

	/**
	 * Sets the output directory for this process
	 * 
	 * @param output
	 *            the output directory
	 */
	public void setMainOutputDir(String output) {
		this.mainOutputDir = output;
	}

	/**
	 * @return the output directory
	 * @throws IOException
	 */
	protected String getOutputDir() throws IOException {
		String folderName = (processNumber > 0) ? String.format("%d.%s",
				processNumber, subDir) : subDir;
		Path out = Paths.get(mainOutputDir).resolve(folderName);
		synchronized (this) {
			if (!Files.isDirectory(out))
				Files.createDirectory(out);
		}
		return out.toString();
	}

	/**
	 * Build method
	 * 
	 * @param number
	 *            the number of this porcess
	 * @return this prcess with the correct process number
	 */
	public PipelineProcess setProcessNumber(int number) {
		this.processNumber = number;
		return this;
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
		return new ArrayList<String>(inputFiles);
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
	 * @throws IOException
	 *             if an I/O error occurs
	 * @throws InterruptedException
	 *             if a process has been interrupted
	 */
	public abstract ProcessResult launch() throws IOException;

}
