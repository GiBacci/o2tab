package bacci.giovanni.o2tab.context;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import bacci.giovanni.o2tab.pipeline.MandatoryPipeline;
import bacci.giovanni.o2tab.pipeline.PipelineProcess;
import bacci.giovanni.o2tab.pipeline.PipelineProcessQueue;
import bacci.giovanni.o2tab.process.ClusteringOTU;
import bacci.giovanni.o2tab.process.DereplicationProcess;
import bacci.giovanni.o2tab.process.MappingProcess;
import bacci.giovanni.o2tab.process.PANDAseqProcessBuilder;
import bacci.giovanni.o2tab.process.PANDAseqProcessBuilder.QualityEncoding;
import bacci.giovanni.o2tab.process.PoolingProcess;
import bacci.giovanni.o2tab.process.TableProcess;

/**
 * Context class for microbiome pipeline
 * 
 * @author <a href="http://www.unifi.it/dblage/CMpro-v-p-65.html">Giovanni
 *         Bacci</a>
 *
 */
public class MicrobiomeContext {

	/**
	 * The pipeline
	 */
	private PipelineProcessQueue queue;

	/**
	 * The error logger
	 */
	private Logger log = Logger.getLogger(this.getClass().getName());

	/**
	 * constructor
	 * 
	 * @param args
	 *            program arguments
	 */
	public MicrobiomeContext(String[] args) {
		this.queue = new MandatoryPipeline(log);
		try {
			this.parseArgs(args);
		} catch (NoSuchFileException e) {
			log.log(Level.SEVERE, "File not found " + e.getMessage());
			System.exit(-1);
		} catch (IOException e) {
			log.log(Level.SEVERE, e.getMessage());
			System.exit(-1);
		}
	}

	/**
	 * @return a Thread with the pipeline
	 */
	public Thread getProcess() {
		return new Thread(queue);
	}

	/**
	 * Private method for parsing arguments and configuring the pipeline
	 * 
	 * @param args
	 *            the arguments
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	private void parseArgs(String[] args) throws IOException {
		OptionParser parser = new OptionParser();

		// Defining options
		OptionSpec<String> mate = parser
				.accepts(
						"mate",
						"mate pairs identifiers, one for forward read files and the other for reverse read  files")
				.withRequiredArg().withValuesSeparatedBy(",")
				.ofType(String.class);

		OptionSpec<String> input = parser.accepts("in", "the input folder")
				.withRequiredArg().ofType(String.class).required();

		OptionSpec<String> output = parser.accepts("out", "the output folder")
				.withRequiredArg().ofType(String.class);

		OptionSpec<Integer> thread = parser
				.accepts("thread", "number of thread").withRequiredArg()
				.ofType(Integer.class);

		OptionSpec<Integer> minSize = parser
				.accepts("min_size", "minimum size of replicated sequence")
				.withRequiredArg().ofType(Integer.class);

		OptionSpec<Integer> truncate = parser
				.accepts("trunc", "truncates the reads to a fixed length")
				.withRequiredArg().ofType(Integer.class);

		OptionSpec<String> filter = parser
				.accepts("filter",
						"filter files in input folder based on this string")
				.withRequiredArg().ofType(String.class);

		OptionSpec<Void> enc64 = parser.accepts("64",
				"set the Phred+64 quality encoding");

		OptionSpec<String> log = parser.accepts("log", "path to log file")
				.withRequiredArg().ofType(String.class);

		OptionSpec<Void> help = parser.accepts("help", "print the help")
				.forHelp();

		// Parsing processes
		OptionSet set = null;
		try {
			set = parser.parse(args);
		} catch (OptionException e) {
			String msg = String
					.format("Error parsing arguments: %s%nLaunch with %s option to get help.",
							e.getMessage(), help.toString());
			System.err.println(msg);
			System.exit(-1);
		}

		// Help
		if (set.has(help))
			this.printHelpAndExit(parser);

		// Retriving params

		// Adjusting inputs
		Path in = Paths.get(set.valueOf(input));
		List<String> inputs = new ArrayList<String>();
		String glob = (set.has(filter)) ? String.format("*%s*",
				set.valuesOf(filter)) : "*";
		for (Path p : Files.newDirectoryStream(in, glob))
			inputs.add(p.toString());

		if (inputs.size() == 0)
			throw new FileNotFoundException(
					"No files found with the given filter");

		// Log file
		Path logPath = (set.has(log)) ? Paths.get(set.valueOf(log)) : Paths
				.get(System.getProperty("user.dir")).resolve("o2tab.log");
		FileHandler handler = new FileHandler(logPath.toString());
		handler.setFormatter(new SimpleFormatter());
		this.log.addHandler(handler);

		// Adjusting outputs
		Path o = (set.has(output)) ? Paths.get(set.valueOf(output)) : Paths
				.get(PipelineProcess.getDefaultOutput());
		queue.setMainOutputDir(o.toString());

		// Assambly process
		int threadNum = (set.has(thread)) ? set.valueOf(thread) : 1;
		boolean assembly = false;

		if (set.has(mate)) {
			String mate1 = set.valuesOf(mate).get(0);
			String mate2 = set.valuesOf(mate).get(1);
			QualityEncoding enc = (set.has(enc64)) ? QualityEncoding.PHRED64
					: QualityEncoding.PHRED33;
			queue.addPipelineProcess(new PANDAseqProcessBuilder(mate1, mate2)
					.enc(enc).thread(threadNum).setInputFile(inputs));
			assembly = true;
		}

		// Pooling process
		int trun = (set.has(truncate)) ? set.valueOf(truncate) : -1;
		if (assembly) {
			queue.addPipelineProcess(new PoolingProcess().truncate(trun));
		} else {
			queue.addPipelineProcess(new PoolingProcess().truncate(trun)
					.setInputFile(inputs));
		}

		// Dereplication process
		int min = (set.has(minSize)) ? set.valueOf(minSize) : 1;
		queue.addPipelineProcess(new DereplicationProcess(min));

		// Clustering, mapping and tabling
		queue.addPipelineProcess(new ClusteringOTU());
		queue.addPipelineProcess(new MappingProcess());
		queue.addPipelineProcess(new TableProcess());
	}

	/**
	 * Prints help and exit the program
	 * 
	 * @param parser
	 *            the argument parser
	 */
	private void printHelpAndExit(OptionParser parser) {
		try {
			System.out
					.println("Usage: java -jar o2tab-[version].jar --in <folder> <options>");
			System.out
					.println("--------------------------------------------------------------------");
			parser.printHelpOn(System.out);
			System.out
					.println("--------------------------------------------------------------------");
			System.out
					.println("Developed by: Giovanni Bacci - giovanni.bacci[AT]unifi.it");
		} catch (IOException e) {
			System.err.println("Cannot write help");
			System.exit(-1);
		} finally {
			System.exit(0);
		}
	}
}
