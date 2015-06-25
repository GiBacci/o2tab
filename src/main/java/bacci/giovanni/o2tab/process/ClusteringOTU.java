package bacci.giovanni.o2tab.process;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import bacci.giovanni.o2tab.pipeline.PipelineProcess;
import bacci.giovanni.o2tab.pipeline.ProcessType;

/**
 * Clustering process. This process uses the uparse <code>-cluster_otus</code>
 * command.
 * 
 * @author <a href="http://www.unifi.it/dblage/CMpro-v-p-65.html">Giovanni
 *         Bacci</a>
 *
 */
public class ClusteringOTU extends PipelineProcess {

	/**
	 * Sub directory for output files
	 */
	private static final String SUBDIR = "clustered";

	/**
	 * Name of the main output file
	 */
	private static final String NAME = "otus.fasta";

	/**
	 * Name of the log file
	 */
	private static final String NAME_LOG = "clusterotu.log";

	/**
	 * Name of the uparse output file
	 */
	private static final String NAME_UP = "uparseout.up";

	/**
	 * Label for OTUs
	 */
	private static final String LABEL = "OTU_";		

	/**
	 * The config file reader
	 */
	private final static ConfigFileReader<UparseOTUProcess> CONFIG = new ConfigFileReader<ClusteringOTU.UparseOTUProcess>(
			"/otucluster.config");	

	/**
	 * Constructor
	 */
	public ClusteringOTU() {
		super(ProcessType.OTUCLUST);
	}

	@Override
	public PipelineResult launch() throws Exception {
		if (super.getInputFiles() == null)
			throw new NullPointerException("Input files are null");
		if (super.getInputFiles().size() != 2) {
			String error = String.format(
					"Input files should be 2 but %d found", super
							.getInputFiles().size());
			throw new IllegalArgumentException(error);
		}

		String input = super.getInputFiles().get(0);
		String[] outputs = this.getOutput();

		// Adding pooled read file to the output. It will be needed by the
		// mapping process
		super.addOuptuFile(super.getInputFiles().get(1));

		UparseOTUProcess clusteringProcess = new UparseOTUProcess(input);
		clusteringProcess.addArgumentCommand("-otus", outputs[0]);
		clusteringProcess.addArgumentCommand("-uparseout", outputs[1]);
		clusteringProcess.addArgumentCommand("-relabel", LABEL);
		clusteringProcess.setError(Redirect.to(new File(outputs[2])));

		int res = CONFIG.setExternalArguments(clusteringProcess).call();

		if (res == 0) {
			return PipelineResult.PASSED;
		} else {
			return PipelineResult.FAILED;
		}
	}

	/**
	 * @return and array of string with three output file: 0 - main output file;
	 *         1 - uparse output file; 2 - log output file
	 * @throws IOException
	 *             if an I/O error occurs creating the output
	 */
	private String[] getOutput() throws IOException {
		Path p = Paths.get(super.getOutputDir()).resolve(SUBDIR);
		if (!Files.isDirectory(p))
			Files.createDirectories(p);
		String mainOut = p.resolve(NAME).toString();
		String upOut = p.resolve(NAME_UP).toString();
		String logOut = p.resolve(NAME_LOG).toString();
		super.addOuptuFile(mainOut);
		return new String[] { mainOut, upOut, logOut };
	}

	/**
	 * Uparse process.
	 * 
	 * @author <a href="http://www.unifi.it/dblage/CMpro-v-p-65.html">Giovanni
	 *         Bacci</a>
	 *
	 */
	private class UparseOTUProcess extends CallableProcess {

		public UparseOTUProcess(String inputFile) {
			super("usearch");
			super.addArgumentCommand("-cluster_otus", inputFile);
		}

	}

}
