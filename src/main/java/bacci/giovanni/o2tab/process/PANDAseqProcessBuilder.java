package bacci.giovanni.o2tab.process;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import bacci.giovanni.o2tab.pipeline.PipelineProcess;
import bacci.giovanni.o2tab.pipeline.ProcessType;
import bacci.giovanni.o2tab.util.QualityEncoding;

/**
 * Builder for {@link PANDAseqProcess}.
 * 
 * @author <a href="http://www.unifi.it/dblage/CMpro-v-p-65.html">Giovanni
 *         Bacci</a>
 *
 */
public class PANDAseqProcessBuilder extends PipelineProcess {

	/**
	 * The output directory
	 */
	private final static String SUBDIR = "assembled";

	/**
	 * The output file suffix
	 */
	private final static String OUTSUFFIX = "_assembled.fasta";

	/**
	 * The config file reader
	 */
	private final static ConfigFileReader<PANDAseqProcess> CONFIG = new ConfigFileReader<PANDAseqProcess>(
			"/pandaseq.config");

	/**
	 * Quality encoding
	 */
	private QualityEncoding enc = QualityEncoding.PHRED33;

	/**
	 * Number of simultaneous process allowed
	 */
	private int numThread = 1;

	/**
	 * Regex for mat1
	 */
	private String mate1;

	/**
	 * regex for mate2
	 */
	private String mate2;

	/**
	 * Constuctor
	 * 
	 * @param mate1
	 *            the forward file token
	 * @param mate2
	 *            the reverse file token
	 */
	public PANDAseqProcessBuilder(String mate1, String mate2) {
		super(ProcessType.ASSEMBLY);
		if (mate1 == null || mate2 == null)
			throw new NullPointerException();
		this.mate1 = mate1;
		this.mate2 = mate2;
	}

	/**
	 * Build method. This method set the maximun number of simultaneous
	 * processes allowed.
	 * 
	 * @param numThread
	 *            the maximun number of process
	 * @return this process
	 */
	public PANDAseqProcessBuilder thread(int numThread) {
		this.numThread = numThread;
		return this;
	}

	/**
	 * Build method. This method set the quality encoding used by pandaseq
	 * 
	 * @param enc
	 *            the quality encoding
	 * @return this process
	 */
	public PANDAseqProcessBuilder enc(QualityEncoding enc) {
		this.enc = enc;
		return this;
	}

	@Override
	public PipelineResult launch() throws IOException {
		List<Path> forward = new ArrayList<Path>();
		List<Path> reverse = new ArrayList<Path>();

		for (String s : getInputFiles()) {
			if (s.contains(mate1))
				forward.add(Paths.get(s));
			if (s.contains(mate2))
				reverse.add(Paths.get(s));
		}

		if (forward.size() != reverse.size())
			throw new IOException("Mate pairs differ in number");

		Collections.sort(forward);
		Collections.sort(reverse);

		List<PANDAseqProcess> processList = new ArrayList<PANDAseqProcess>();

		for (int i = 0; i < forward.size(); i++) {
			String out = getOutputPath(forward.get(i).getFileName().toString());
			PANDAseqProcess panda = new PANDAseqProcess(forward.get(i)
					.toString(), reverse.get(i).toString(), enc);
			panda.setOutput(Redirect.to(new File(out)));
			panda.setError(Redirect.to(new File(out + ".log")));
			processList.add(CONFIG.setExternalArguments(panda));
		}

		ExecutorService ex = Executors.newFixedThreadPool(numThread);
		try {
			List<Future<Integer>> results = ex.invokeAll(processList);
			for (Future<Integer> res : results) {
				if (res.get() != 0) {
					ex.shutdownNow();
					return PipelineResult.FAILED;
				}
			}
		} catch (ExecutionException e) {
			throw new IOException(e.getMessage());
		} catch (InterruptedException e) {
			ex.shutdownNow();
			return PipelineResult.INTERRUPTED;
		}
		ex.shutdown();
		return PipelineResult.PASSED;
	}

	/**
	 * Formats output file
	 * 
	 * @param file
	 *            the input file
	 * @return the output file
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	private String getOutputPath(String fileName) throws IOException {
		Path o = Paths.get(super.getOutputDir());
		Path out = o.resolve(SUBDIR).resolve(fileName + OUTSUFFIX);
		if (!Files.isDirectory(out.getParent()))
			Files.createDirectories(out.getParent());
		super.addOuptuFile(out.toString());
		return out.toString();
	}

	/**
	 * PANDAseqProcess
	 * 
	 * @author <a href="http://www.unifi.it/dblage/CMpro-v-p-65.html">Giovanni
	 *         Bacci</a>
	 *
	 */
	private class PANDAseqProcess extends CallableProcess {

		public PANDAseqProcess(String forward, String reverse,
				QualityEncoding enc) {
			super("pandaseq");
			addArgumentCommand("-f", forward);
			addArgumentCommand("-r", reverse);
			if (enc == QualityEncoding.PHRED64)
				addSingleCommand("-6");
		}

	}

}
