package bacci.giovanni.o2tab.process;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import bacci.giovanni.o2tab.exceptions.WrongInputFileNumberException;
import bacci.giovanni.o2tab.pipeline.PipelineProcess;
import bacci.giovanni.o2tab.pipeline.ProcessResult;
import bacci.giovanni.o2tab.pipeline.ProcessResult.PipelineResult;
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
		super(ProcessType.OTUCLUST, "clustered");
	}

	@Override
	public ProcessResult launch() throws IOException {
		String warn = null;
		if (super.getInputFiles().size() < 2)
			throw new WrongInputFileNumberException(2, super.getInputFiles()
					.size());

		if (super.getInputFiles().size() > 2)
			warn = "too many input files found, only the first "
					+ "two elements will be included in the analysis";

		String input = super.getInputFiles().get(0);
		String[] outputs = this.getOutput();

		// Adding pooled read file to the output. It will be needed by the
		// mapping process
		super.addOuptuFile(super.getInputFiles().get(1));

		UparseOTUProcess clusteringProcess = new UparseOTUProcess(input);
		clusteringProcess.addArgumentCommand("-otus", outputs[0]);
		clusteringProcess.addArgumentCommand("-uparseout", outputs[1]);
		clusteringProcess.addArgumentCommand("-relabel", LABEL);

		File error = new File(outputs[2]);
		clusteringProcess.setError(Redirect.to(error));

		ExecutorService ex = Executors.newFixedThreadPool(1);

		Future<Integer> res = ex.submit(CONFIG
				.setExternalArguments(clusteringProcess));

		ProcessResult pr = null;
		try {
			switch (res.get()) {
			case 0:
				if (warn == null) {
					pr = new ProcessResult(PipelineResult.PASSED);
				} else {
					pr = new ProcessResult(PipelineResult.PASSED_WITH_WARNINGS);
					pr.addWarning(warn);
				}
				break;
			default:
				if (warn == null) {
					pr = new ProcessResult(PipelineResult.FAILED);
				} else {
					pr = new ProcessResult(PipelineResult.FAILED_WITH_WARNINGS);
					pr.addWarning(warn);
				}
				pr.addFail("see " + error.toString() + " for details");
				break;
			}
		} catch (InterruptedException e) {
			ex.shutdownNow();
			pr = new ProcessResult(PipelineResult.INTERRUPTED);
		} catch (ExecutionException e) {
			throw new IOException(e.getMessage());
		}
		return pr;
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
