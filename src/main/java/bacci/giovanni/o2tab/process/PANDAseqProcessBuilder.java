package bacci.giovanni.o2tab.process;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import bacci.giovanni.o2tab.pipeline.PipelineProcess;
import bacci.giovanni.o2tab.pipeline.ProcessResult;
import bacci.giovanni.o2tab.pipeline.ProcessResult.PipelineResult;
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
	 * The output file suffix
	 */
	private final static String OUTSUFFIX = ".fasta";
	
	private final static String OUTPREFIX = "assembled_";

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
	 * List of warnings
	 */
	private List<String> warnings;

	/**
	 * List of warnings
	 */
	private List<String> errors;

	/**
	 * Constuctor
	 * 
	 * @param mate1
	 *            the forward file token
	 * @param mate2
	 *            the reverse file token
	 */
	public PANDAseqProcessBuilder(String mate1, String mate2) {
		super(ProcessType.ASSEMBLY, "assembled");
		if (mate1 == null || mate2 == null)
			throw new NullPointerException();
		this.mate1 = mate1;
		this.mate2 = mate2;
		this.warnings = new ArrayList<String>();
		this.errors = new ArrayList<String>();
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
	public ProcessResult launch() throws IOException {
		List<PANDAseqProcess> processList = new ArrayList<PANDAseqProcess>();
		List<File> errorFiles = new ArrayList<File>();

		for (Entry<Path, Path> p : this.pairFiles().entrySet()) {
			String out = formatOutputFile(p.getKey().getFileName().toString());
			PANDAseqProcess panda = new PANDAseqProcess(p.getKey().toString(),
					p.getValue().toString(), enc);
			panda.setOutput(Redirect.to(new File(out)));
			File error = new File(out + ".log");
			errorFiles.add(error);
			panda.setError(Redirect.to(error));
			processList.add(CONFIG.setExternalArguments(panda));
		}

		ExecutorService ex = Executors.newFixedThreadPool(numThread);

		ProcessResult pr = null;

		try {
			List<Future<Integer>> results = ex.invokeAll(processList);
			for (int i = 0; i < results.size(); i++) {
				Future<Integer> res = results.get(i);
				if (res.get() != 0) {
					this.errors
							.add("see " + errorFiles.get(i) + " for details");
				}
			}
		} catch (ExecutionException e) {
			throw new IOException(e.getMessage());
		} catch (InterruptedException e) {
			ex.shutdownNow();
			pr = new ProcessResult(PipelineResult.INTERRUPTED);
			return pr;
		}
		ex.shutdown();

		if (errors.size() == processList.size()) {
			if (!warnings.isEmpty()) {
				pr = new ProcessResult(PipelineResult.FAILED_WITH_WARNINGS);
				pr.addAllWarnings(warnings);
			} else {
				pr = new ProcessResult(PipelineResult.FAILED);
			}
			pr.addAllFails(errors);
		} else {
			if (!warnings.isEmpty() || !errors.isEmpty()) {
				warnings.addAll(errors);
				pr = new ProcessResult(PipelineResult.PASSED_WITH_WARNINGS);
				pr.addAllWarnings(warnings);
			} else {
				pr = new ProcessResult(PipelineResult.PASSED);
			}

		}
		return pr;
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
	private String formatOutputFile(String fileName) throws IOException {
		Path out = Paths.get(super.getOutputDir())
				.resolve(OUTPREFIX + fileName + OUTSUFFIX);
		super.addOuptuFile(out.toString());
		return out.toString();
	}

	/**
	 * @return a {@link LinkedHashMap} with forward read files as keys and
	 *         reverse res files as values.
	 */
	private Map<Path, Path> pairFiles() {
		List<Path> forward = new ArrayList<Path>();
		List<Path> reverse = new ArrayList<Path>();

		String cleanToken = "clean";

		for (String s : getInputFiles()) {
			Path p = Paths.get(s);
			if (p.getFileName().toString().contains(mate1))
				forward.add(p);
			if (p.getFileName().toString().contains(mate2))
				reverse.add(p);
		}

		Map<Path, Path> mateMap = new LinkedHashMap<Path, Path>();

		for (Path p : forward) {
			String clean = p.getFileName().toString()
					.replaceAll(mate1, cleanToken);
			Path val = null;
			for (Path pp : reverse) {
				if (clean.equals(pp.getFileName().toString()
						.replaceAll(mate2, cleanToken)))
					val = pp;
			}
			if (val != null) {
				mateMap.put(p, val);
				reverse.remove(val);
			}
		}

		List<Path> unpaired = new ArrayList<Path>();

		for (Path p : mateMap.keySet())
			forward.remove(p);

		unpaired.addAll(forward);
		unpaired.addAll(reverse);

		for (Path p : unpaired)
			warnings.add("did not found any mate for " + "the file: "
					+ p.toString());

		return mateMap;
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
