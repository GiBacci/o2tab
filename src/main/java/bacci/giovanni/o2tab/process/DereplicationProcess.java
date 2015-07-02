package bacci.giovanni.o2tab.process;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.TreeMap;

import org.biojava.bio.BioException;
import org.biojavax.bio.seq.RichSequenceIterator;

import bacci.giovanni.o2tab.exceptions.WrongInputFileNumberException;
import bacci.giovanni.o2tab.pipeline.PipelineProcess;
import bacci.giovanni.o2tab.pipeline.ProcessType;
import bacci.giovanni.o2tab.util.Utils;

/**
 * Dereplication class
 * 
 * @author <a href="http://www.unifi.it/dblage/CMpro-v-p-65.html">Giovanni
 *         Bacci</a>
 *
 */
public class DereplicationProcess extends PipelineProcess {

	/**
	 * The lowest number of times that a sequence has to be found in the file
	 * for considering it a good sequence
	 */
	private final long minCount;

	/**
	 * The default charset
	 */
	private final static Charset CS = Charset.defaultCharset();

	/**
	 * The frequency map
	 */
	private Map<ByteSequence, Long> freq;

	/**
	 * Name of the output folder
	 */
	private final static String SUBDIR = "dereplicated";

	/**
	 * Name of the output file
	 */
	private final static String NAME = "dereplicated.fasta";

	/**
	 * Size flag
	 */
	private final static String FLAG = ";size=";

	/**
	 * Constructor
	 * 
	 * @param minCount
	 *            the minimum size needed to include a sequence in the final
	 *            output
	 */
	public DereplicationProcess(long minCount) {
		super(ProcessType.DEREPLICATION);
		this.minCount = minCount;
		this.freq = new HashMap<DereplicationProcess.ByteSequence, Long>();
	}

	@Override
	public PipelineResult launch() throws IOException {
		if (super.getInputFiles().size() > 1)
			throw new WrongInputFileNumberException(1, super.getInputFiles()
					.size());

		String s = super.getInputFiles().get(0);
		RichSequenceIterator it = Utils.getSequenceIterator(s);
		try {
			while (it != null && it.hasNext()) {
				this.addSequence(it.nextSequence().seqString());
			}
		} catch (NoSuchElementException e) {
			throw new IOException("Cannot read inside sequence file: " + s);
		} catch (BioException e) {
			throw new IOException("Sequence file is not well formatted " + s);
		}
		this.dumpResults();
		// Adding pooled read file to the output. It will be needed by the
		// mapping process. It is important that this operation is performed
		// after the execution of the dumpResults method otherwise the reads
		// file will be the first in the output list
		super.addOuptuFile(s);
		return PipelineResult.PASSED;
	}

	/**
	 * Add a sequence to the frequency table
	 * 
	 * @param sequence
	 *            the sequence to add
	 */
	private void addSequence(String sequence) {
		ByteSequence s = new ByteSequence(sequence);
		if (freq.get(s) == null) {
			freq.put(s, 1L);
		} else {
			freq.put(s, freq.get(s) + 1);
		}
	}

	/**
	 * Write all results in an output file
	 * 
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	private void dumpResults() throws IOException {
		BufferedWriter wr = this.getWriter();
		Map<ByteSequence, Long> orderedFreq = new TreeMap<ByteSequence, Long>(
				new CountComparator());
		orderedFreq.putAll(freq);
		freq.clear();

		long numSeq = 0;
		for (Entry<ByteSequence, Long> e : orderedFreq.entrySet()) {
			if (e.getValue() < minCount)
				continue;
			numSeq++;
			wr.write(String.format(">Dereplicated_sequence_%d%s%d", numSeq,
					FLAG, e.getValue()));
			String seq = Utils.formatFASTA(e.getKey().getSequence(), 80);
			wr.newLine();
			wr.write(seq);
			wr.newLine();
			wr.flush();
		}
		wr.close();
	}

	/**
	 * @return a writer for the output file
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	private BufferedWriter getWriter() throws IOException {
		Path p = Paths.get(super.getOutputDir()).resolve(SUBDIR);
		if (!Files.isDirectory(p))
			Files.createDirectories(p);
		String out = p.resolve(NAME).toString();
		super.addOuptuFile(out);
		FileWriter fw = new FileWriter(out);
		return new BufferedWriter(fw);
	}

	/**
	 * Private class representing a byte sequence
	 * 
	 * @author <a href="http://www.unifi.it/dblage/CMpro-v-p-65.html">Giovanni
	 *         Bacci</a>
	 *
	 */
	private class ByteSequence {

		private byte[] seq;

		private ByteSequence(String sequence) {
			this.seq = sequence.getBytes(CS);
		}

		private String getSequence() {
			return new String(seq, CS);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + Arrays.hashCode(seq);
			return result;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ByteSequence other = (ByteSequence) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (!Arrays.equals(seq, other.seq))
				return false;
			return true;
		}

		private DereplicationProcess getOuterType() {
			return DereplicationProcess.this;
		}

	}

	/**
	 * Private comparator for ordering the entries in the frequency table based
	 * on their frequency.
	 * 
	 * @author <a href="http://www.unifi.it/dblage/CMpro-v-p-65.html">Giovanni
	 *         Bacci</a>
	 *
	 */
	private class CountComparator implements Comparator<ByteSequence> {

		@Override
		public int compare(ByteSequence o1, ByteSequence o2) {
			if (o1 == null && o2 == null)
				return 0;
			if (o1 == null)
				return 1;
			if (o2 == null)
				return -1;
			Long l1 = freq.get(o1);
			Long l2 = freq.get(o2);
			if (l1 == null && l2 == null)
				return 0;
			if (l1 == null)
				return 1;
			if (l2 == null)
				return -1;
			return l2.compareTo(l1);
		}

	}

}
