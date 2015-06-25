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
 * Mapping process which maps the reads back to the OTUs. This process uses the
 * usearch command <code>-usearch_global</code>.
 * 
 * @author <a href="http://www.unifi.it/dblage/CMpro-v-p-65.html">Giovanni
 *         Bacci</a>
 *
 */
public class MappingProcess extends PipelineProcess {

	/**
	 * Output directory
	 */
	private static final String SUBDIR = "mapped";

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
		super(ProcessType.MAPPING);
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
		String[] outs = this.getOutputs();

		GlobalOTUProcess otu = new GlobalOTUProcess(super.getInputFiles()
				.get(1));
		otu.addArgumentCommand("-db", input);
		otu.addArgumentCommand("-uc", outs[0]);
		otu.setError(Redirect.to(new File(outs[1])));
		int res = CONFIG.setExternalArguments(otu).call();

		if (res == 0) {
			return PipelineResult.PASSED;
		} else {
			return PipelineResult.FAILED;
		}
	}

	/**
	 * @return the outpu file names as an array of string. 0 - output file name;
	 *         1 - log file name
	 * @throws IOException
	 *             if an I/O error occurs generating the files
	 */
	private String[] getOutputs() throws IOException {
		Path p = Paths.get(super.getOutputDir()).resolve(SUBDIR);
		if (!Files.isDirectory(p))
			Files.createDirectories(p);

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
