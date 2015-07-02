package bacci.giovanni.o2tab.process;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.collections.IteratorUtils;
import org.biojava.bio.program.fastq.Fastq;
import org.biojava.bio.program.fastq.FastqBuilder;
import org.biojava.bio.program.fastq.FastqReader;
import org.biojava.bio.program.fastq.FastqTools;
import org.biojava.bio.program.fastq.FastqWriter;
import org.biojava.bio.program.fastq.SangerFastqReader;
import org.biojava.bio.program.fastq.SangerFastqWriter;
import org.biojava.bio.program.fastq.SolexaFastqReader;
import org.biojava.bio.program.fastq.SolexaFastqWriter;
import org.biojava.bio.program.fastq.StreamListener;

import bacci.giovanni.o2tab.pipeline.PipelineProcess;
import bacci.giovanni.o2tab.pipeline.ProcessType;
import bacci.giovanni.o2tab.util.ExceptionHandler;
import bacci.giovanni.o2tab.util.QualityEncoding;

public class StreamingTrimLight extends PipelineProcess {

	/**
	 * The subdir of the process
	 */
	private static final String SUBDIR = "trimmed";

	/**
	 * The cutoff
	 */
	private int cutoff = 20;

	/**
	 * The number of threads
	 */
	private int thread = 1;

	/**
	 * Exception handler
	 */
	private ExceptionHandler<IOException> handler = null;

	/**
	 * Reader for fastq files
	 */
	private FastqReader reader = new SangerFastqReader();

	/**
	 * Writer for fastq files
	 */
	private FastqWriter writer = new SangerFastqWriter();

	/**
	 * Constructor
	 */
	public StreamingTrimLight() {
		super(ProcessType.TRIMMING);
	}

	/**
	 * Build method
	 * 
	 * @param cutoff
	 *            the cutoff to set
	 * @return this object with the cutoff set
	 */
	public StreamingTrimLight cutoff(int cutoff) {
		this.cutoff = cutoff;
		return this;
	}

	/**
	 * Build method
	 * 
	 * @param numThread
	 *            the number of threads to set
	 * @return this object with the number of threads set
	 */
	public StreamingTrimLight thread(int numThread) {
		this.thread = numThread;
		return this;
	}

	/**
	 * Build method
	 * 
	 * @param enc
	 *            the quality encode to set
	 * @return this object with the quality encode set
	 */
	public StreamingTrimLight enc(QualityEncoding enc) {
		if (enc.equals(QualityEncoding.PHRED64)) {
			this.reader = new SolexaFastqReader();
			this.writer = new SolexaFastqWriter();
		}
		return this;
	}

	/**
	 * @return a {@link BufferedWriter}
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	private OutputStream createOutptuStream(String file, boolean gzipped)
			throws IOException {
		Path o = Paths.get(super.getOutputDir()).resolve(SUBDIR);
		if (!Files.isDirectory(o))
			Files.createDirectories(o);
		String in = Paths.get(file).getFileName().toString();
		String out = o.resolve(in).toString();
		super.addOuptuFile(out);

		OutputStream os = null;
		FileOutputStream fos = new FileOutputStream(out);
		if (gzipped) {
			os = new GZIPOutputStream(fos);
		} else {
			os = fos;
		}
		return os;
	}

	/**
	 * @param file
	 *            the input file
	 * @param gzipped
	 *            if the file is gzipped
	 * @return an {@link InputStreamReader}
	 * @throws IOException
	 */
	private InputStreamReader createInputStream(String file, boolean gzipped)
			throws IOException {
		InputStream is = new BufferedInputStream(new FileInputStream(file));
		InputStreamReader isr = (gzipped) ? new InputStreamReader(
				new GZIPInputStream(is)) : new InputStreamReader(is);
		return isr;
	}

	@Override
	public PipelineResult launch() throws IOException {
		ExecutorService ex = Executors.newFixedThreadPool(thread);
		this.handler = new ExceptionHandler<IOException>(ex);
		for (String input : super.getInputFiles()) {
			ex.submit(new Trimmer(input));
		}
		ex.shutdown();
		try {
			ex.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
		} catch (InterruptedException e) {
			ex.shutdownNow();
			return PipelineResult.INTERRUPTED;
		}
		this.handler.throwIfAny();
		return PipelineResult.PASSED;
	}

	/**
	 * Private method for checking if an input stream contains gzipped data
	 * 
	 * @param stream
	 *            the input stream
	 * @return <code>true</code> if the input strream is gzipped otherwise it
	 *         returns <code>false</code>
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	private static boolean isGzipStream(InputStream stream) throws IOException {
		byte[] bytes = new byte[2];
		stream.read(bytes);
		int head = ((int) bytes[0] & 0xff) | ((bytes[1] << 8) & 0xff00);
		return GZIPInputStream.GZIP_MAGIC == head;
	}

	/**
	 * A runnable trimmer
	 * 
	 * @author <a href="http://www.unifi.it/dblage/CMpro-v-p-65.html">Giovanni
	 *         Bacci</a>
	 *
	 */
	private class Trimmer implements Runnable {

		/**
		 * The input file
		 */
		private String input;

		/**
		 * Constructor
		 * 
		 * @param input
		 *            the input file
		 * @throws IOException
		 *             if an I/O error occurs
		 */
		public Trimmer(String input) throws IOException {
			this.input = input;
		}

		@Override
		public void run() {
			try {
				InputStream is = new FileInputStream(input);
				boolean gzipped = isGzipStream(is);
				is.close();
				InputStreamReader isr = createInputStream(input, gzipped);
				OutputStream out = createOutptuStream(input, gzipped);
				reader.stream(isr, new QualityListener(out));
				out.close();
			} catch (FileNotFoundException e) {
				handler.sendException(e);
			} catch (IOException e) {
				handler.sendException(e);
			}

		}
	}

	/**
	 * Stream listener implementation for checking quality values of a fastq
	 * sequence
	 * 
	 * @author <a href="http://www.unifi.it/dblage/CMpro-v-p-65.html">Giovanni
	 *         Bacci</a>
	 *
	 */
	private class QualityListener implements StreamListener {

		/**
		 * Output file
		 */
		private OutputStream out;

		/**
		 * Constructor
		 * 
		 * @param outputStream
		 *            the output file
		 */
		public QualityListener(OutputStream outputStream) {
			this.out = outputStream;
		}

		@Override
		public void fastq(Fastq fastq) {
			Iterable<Integer> qual = FastqTools.qualityScores(fastq);
			Integer[] q = (Integer[]) IteratorUtils.toArray(qual.iterator(),
					Integer.class);
			int cutIndex = q.length;
			int cumQuality = 0;

			for (int i = (q.length - 1); i >= 0; i--) {
				int cumCutoff = cutoff * (cutIndex - i);
				cumQuality += q[i];

				if ((cumQuality - cumCutoff) < 0) {
					cutIndex = i;
					cumQuality = 0;
				}
			}

			String s = fastq.getSequence().substring(0, cutIndex);
			String fq = fastq.getQuality().substring(0, cutIndex);
			String id = fastq.getDescription();

			Fastq f = new FastqBuilder().withSequence(s).withQuality(fq)
					.withDescription(id).build();
			try {
				writer.write(out, f);
			} catch (IOException e) {
				handler.sendException(e);
			}
		}

	}

}
