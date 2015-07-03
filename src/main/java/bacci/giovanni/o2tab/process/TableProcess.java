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
import bacci.giovanni.o2tab.pipeline.ProcessType;
import bacci.giovanni.o2tab.pipeline.ProcessResult.PipelineResult;

/**
 * Otu table generator. This process uses the uc2otutab.py python script.
 * 
 * @author <a href="http://www.unifi.it/dblage/CMpro-v-p-65.html">Giovanni
 *         Bacci</a>
 *
 */
public class TableProcess extends PipelineProcess {

	/**
	 * Output file name
	 */
	private static final String NAME = "otu_table.csv";

	/**
	 * Log file name
	 */
	private static final String NAME_LOG = "table.log";

	/**
	 * The config file reader
	 */
	private final static ConfigFileReader<Uc2otutabProcess> CONFIG = new ConfigFileReader<Uc2otutabProcess>(
			"/uc2otutab.config");

	/**
	 * Constructor
	 */
	public TableProcess() {
		super(ProcessType.TABLING, "tabled");
	}

	@Override
	public ProcessResult launch() throws IOException {
		if (super.getInputFiles().size() > 1)
			throw new WrongInputFileNumberException(1, super.getInputFiles()
					.size());

		String input = super.getInputFiles().get(0);
		String[] outs = this.getOutputs();

		Uc2otutabProcess tab = new Uc2otutabProcess();
		tab.addSingleCommand(input);
		
		File error = new File(outs[1]);
		tab.setOutput(Redirect.to(new File(outs[0])));
		tab.setError(Redirect.to(error));

		ExecutorService ex = Executors.newFixedThreadPool(1);
		Future<Integer> res = ex.submit(CONFIG.setExternalArguments(tab));

		ProcessResult pr = null;
		
		try {
			if (res.get() == 0) {
				pr = new ProcessResult(PipelineResult.PASSED);
				return pr;
			} else {
				pr = new ProcessResult(PipelineResult.FAILED);
				pr.addFail("see " + error.toString() + " for details");
				return pr;
			}
		} catch (ExecutionException e) {
			throw new IOException(e);
		} catch (InterruptedException e) {
			ex.shutdownNow();
			pr = new ProcessResult(PipelineResult.INTERRUPTED);
			return pr;
		}
	}

	/**
	 * @return a String array contining all the output files: 0 - table file; 1
	 *         - log file
	 * @throws IOException
	 *             if an I/O error occurs generating output files and
	 *             directories
	 */
	private String[] getOutputs() throws IOException {
		Path p = Paths.get(super.getOutputDir());
		String out = p.resolve(NAME).toString();
		String log = p.resolve(NAME_LOG).toString();
		return new String[] { out, log };

	}

	/**
	 * Process that uses the python script uc2otutab.py.
	 * 
	 * @author <a href="http://www.unifi.it/dblage/CMpro-v-p-65.html">Giovanni
	 *         Bacci</a>
	 *
	 */
	private class Uc2otutabProcess extends CallableProcess {

		public Uc2otutabProcess() {
			super("uc2otutab.py");
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see bacci.giovanni.o2tab.process.CallableProcess#getCommands()
		 */
		@Override
		protected String[] getCommands() {
			String[] cmd = super.getCommands();
			String[] newCmd = new String[cmd.length + 1];
			System.arraycopy(cmd, 0, newCmd, 1, cmd.length);
			newCmd[0] = "python";
			return newCmd;
		}

	}

}
