package bacci.giovanni.o2tab.process;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
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
 * Mapping process which maps the reads back to the OTUs. This process uses the
 * usearch command <code>-usearch_global</code>.
 * 
 * @author <a href="http://www.unifi.it/dblage/CMpro-v-p-65.html">Giovanni
 *         Bacci</a>
 *
 */
public class MappingProcess extends PipelineProcess {

	/**
	 * Output file name
	 */
	private static final String NAME = "map.uc";

	/**
	 * Log file name
	 */
	private static final String NAME_LOG = "mapping.log";

	/**
	 * The config file reader
	 */
	private final static ConfigFileReader<GlobalOTUProcess> CONFIG = new ConfigFileReader<GlobalOTUProcess>(
			"/usearchglobal.config");

	/**
	 * Constructor
	 */
	public MappingProcess() {
		super(ProcessType.MAPPING, "mapped");
	}

	@Override
	public ProcessResult launch() throws IOException {
		String warn = null;
		if (super.getInputFiles().size() < 2)
			throw new WrongInputFileNumberException(2, super.getInputFiles()
					.size());

		if (super.getInputFiles().size() > 2)
			warn = "too many input files found, only the first two"
					+ "elements will be included in the analysis";

		String input = super.getInputFiles().get(0);
		String[] outs = this.getOutputs();

		GlobalOTUProcess otu = new GlobalOTUProcess(super.getInputFiles()
				.get(1));
		otu.addArgumentCommand("-db", input);
		otu.addArgumentCommand("-uc", outs[0]);

		File error = new File(outs[1]);
		otu.setError(Redirect.to(error));

		ExecutorService ex = Executors.newFixedThreadPool(1);
		Future<Integer> res = ex.submit(CONFIG.setExternalArguments(otu));

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
	 * @return the outpu file names as an array of string. 0 - output file name;
	 *         1 - log file name
	 * @throws IOException
	 *             if an I/O error occurs generating the files
	 */
	private String[] getOutputs() throws IOException {
		Path p = Paths.get(super.getOutputDir());
		String out = p.resolve(NAME).toString();
		String log = p.resolve(NAME_LOG).toString();
		super.addOuptuFile(out);
		return new String[] { out, log };
	}

	/**
	 * usearch_global process
	 * 
	 * @author <a href="http://www.unifi.it/dblage/CMpro-v-p-65.html">Giovanni
	 *         Bacci</a>
	 *
	 */
	private class GlobalOTUProcess extends CallableProcess {

		public GlobalOTUProcess(String reads) {
			super("usearch");
			super.addArgumentCommand("-usearch_global", reads);
		}

	}

}
