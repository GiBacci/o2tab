package bacci.giovanni.o2tab.context;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
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
import bacci.giovanni.o2tab.pipeline.PipelineProcessQueue;
import bacci.giovanni.o2tab.process.ClusteringOTU;
import bacci.giovanni.o2tab.process.DereplicationProcess;
import bacci.giovanni.o2tab.process.MappingProcess;
import bacci.giovanni.o2tab.process.PANDAseqProcessBuilder;
import bacci.giovanni.o2tab.process.PANDAseqProcessBuilder.QualityEncoding;
import bacci.giovanni.o2tab.process.PoolingProcess;
import bacci.giovanni.o2tab.process.TableProcess;

public class MicrobiomeContext {

	private PipelineProcessQueue queue;

	private Logger log = Logger.getLogger(this.getClass().getName());

	public MicrobiomeContext(String[] args) {
		this.queue = new MandatoryPipeline(log);
		try {
			this.parseArgs(args);
		} catch (IOException e1) {
			queue.log(Level.SEVERE, "I/O error occurs");
			System.exit(-1);
		}
	}

	public Thread getProcess() {
		return new Thread(queue);
	}

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
			try {
				System.out
						.println("Usage: java -jar o2tab.jar --in <folder> <options>");
				System.out
						.println("--------------------------------------------------------------------");
				parser.printHelpOn(System.out);
				System.out
						.println("--------------------------------------------------------------------");
				System.out
						.println("Developed by: Giovanni Bacci - giovanni.bacci@unifi.it");
			} catch (IOException e) {
				System.err.println("Cannot write help");
			} finally {
				System.exit(-1);
			}

		// Retriving params

		// Adjusting inputs
		Path in = Paths.get(set.valueOf(input));
		if (!Files.isDirectory(in))
			throw new FileNotFoundException(
					"Input folder not found or it is not a folder");
		if (Files.isDirectory(in) && !Files.isReadable(in))
			throw new FileNotFoundException("Cannot read inside input folder");

		List<String> inputs = new ArrayList<String>();
		String glob = (set.has(filter)) ? String.format("*%s*",
				set.valuesOf(filter)) : "*";
		for (Path p : Files.newDirectoryStream(in, glob))
			inputs.add(p.toString());

		if (inputs.size() < 1)
			throw new FileNotFoundException(
					"No files found with the given filter");

		// Log file
		Path logPath = (set.has(log)) ? Paths.get(set.valueOf(log)) : in
				.resolve("o2tab.log");
		FileHandler handler = new FileHandler(logPath.toString());
		handler.setFormatter(new SimpleFormatter());
		this.log.addHandler(handler);

		// Adjusting outputs
		if (set.has(output)) {
			Path o = Paths.get(set.valueOf(output));
			if (!Files.isDirectory(o))
				throw new FileNotFoundException(
						"Output directory does not exist or it is not a directory");
			if (!Files.isWritable(in))
				throw new IllegalArgumentException(
						"Cannot write inside output folder");
			queue.setMainOutputDir(set.valueOf(output).toString());
		} else {
			if (!Files.isWritable(in))
				throw new IllegalArgumentException(
						"Cannot write inside output folder");
			queue.setMainOutputDir(in.toString());
		}

		// Assambly process
		int threadNum = (set.has(thread)) ? set.valueOf(thread) : 1;
		boolean assembly = false;
		if (set.has(mate)) {
			for (String s : set.valuesOf(mate))
				System.out.println(s);

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
}
