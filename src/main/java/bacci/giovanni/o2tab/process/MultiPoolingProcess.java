package bacci.giovanni.o2tab.process;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.biojava.nbio.core.sequence.DNASequence;
import org.biojava.nbio.core.sequence.compound.DNACompoundSet;
import org.biojava.nbio.core.sequence.compound.NucleotideCompound;
import org.biojava.nbio.core.sequence.io.DNASequenceCreator;
import org.biojava.nbio.core.sequence.io.FastaReader;
import org.biojava.nbio.core.sequence.io.PlainFastaHeaderParser;

import bacci.giovanni.o2tab.pipeline.PipelineProcess;
import bacci.giovanni.o2tab.pipeline.ProcessType;
import bacci.giovanni.o2tab.util.ExceptionHandler;
import bacci.giovanni.o2tab.util.Utils;

public class MultiPoolingProcess extends PipelineProcess {

	/**
	 * The barcode flag
	 */
	private static final String BARCODE = ";barcodelabel=";

	/**
	 * The subdir of the process
	 */
	private static final String SUBDIR = "pooled";

	/**
	 * The name of the output file
	 */
	private static final String NAME = "pooled.fasta";

	/**
	 * Pooled sequence writer
	 */
	private BufferedWriter writer = null;

	/**
	 * Number of reading threads
	 */
	private int thread = 1;

	/**
	 * The size of the sequence buffer
	 */
	private int bufferSize = 10;

	/**
	 * Truncation parameters
	 * 
	 * @see Utils#truncate(String, int, int)
	 */
	private int truncate[] = { -1, -1 };

	/**
	 * IOException handler
	 */
	private ExceptionHandler<IOException> handler = null;

	/**
	 * Construcotr
	 */
	public MultiPoolingProcess() {
		super(ProcessType.POOLING);
	}

	@Override
	public PipelineResult launch() throws IOException {
		this.writer = createWriter();
		ExecutorService ex = Executors.newFixedThreadPool(thread);
		this.handler = new ExceptionHandler<IOException>(ex);

		for (String file : super.getInputFiles()) {
			ex.submit(new Pool(file));
		}
		
		ex.shutdown();
		try {
			ex.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			ex.shutdownNow();
			return PipelineResult.INTERRUPTED;
		}
		this.handler.throwIfAny();

		writer.close();
		return PipelineResult.PASSED;
	}

	public MultiPoolingProcess thread(int thread) {
		this.thread = thread;
		return this;
	}

	public MultiPoolingProcess buffer(int bufferSize) {
		this.bufferSize = bufferSize;
		return this;
	}

	public MultiPoolingProcess truncate(int min, int max) {
		this.truncate = new int[] { min, max };
		return this;
	}

	/**
	 * @param file
	 *            the input file path
	 * @return the file name
	 */
	private String getBarcode(String file) {
		Path p = Paths.get(file);
		return p.getFileName().toString();
	}

	/**
	 * @return a {@link BufferedWriter}
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	private BufferedWriter createWriter() throws IOException {
		Path o = Paths.get(super.getOutputDir()).resolve(SUBDIR);
		if (!Files.isDirectory(o))
			Files.createDirectories(o);
		String out = o.resolve(NAME).toString();
		super.addOuptuFile(out);
		FileWriter fw = new FileWriter(out);
		return new BufferedWriter(fw);
	}

	private synchronized void writeSequence(String id, String sequence,
			String barcode) throws IOException {
		String truncSeq = Utils.truncate(sequence, truncate[0], truncate[1]);
		String seq = Utils.formatFASTA(truncSeq, 80);
		String idFmt = String.format(">%s%s%s", id, BARCODE, barcode);
		writer.write(idFmt);
		writer.newLine();
		writer.write(seq);
		writer.newLine();
		writer.flush();
	}

	/**
	 * Pooling class. This class read inside a sequence file writing each
	 * sequence inside a single file
	 * 
	 * @author <a href="http://www.unifi.it/dblage/CMpro-v-p-65.html">Giovanni
	 *         Bacci</a>
	 *
	 */
	private class Pool implements Runnable {

		/**
		 * The sequence file
		 */
		private String input;

		/**
		 * Constructor
		 * 
		 * @param input
		 *            the sequence file
		 */
		public Pool(String input) {
			this.input = input;
		}

		@Override
		public void run() {
			String barcode = getBarcode(input);
			FastaReader<DNASequence, NucleotideCompound> reader = null;
			try {
				reader = new FastaReader<DNASequence, NucleotideCompound>(
						new FileInputStream(input),
						new PlainFastaHeaderParser<DNASequence, NucleotideCompound>(),
						new DNASequenceCreator(DNACompoundSet
								.getDNACompoundSet()));
				LinkedHashMap<String, DNASequence> seq = null;
				while ((seq = reader.process(bufferSize)) != null) {
					for (String id : seq.keySet())
						writeSequence(id, seq.get(id).getSequenceAsString(),
								barcode);
				}
			} catch (FileNotFoundException e) {
				handler.sendException(e);
			} catch (IOException e) {
				handler.sendException(e);
			}
		}

	}

}
