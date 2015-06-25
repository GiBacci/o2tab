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
import java.util.TreeMap;

import org.biojavax.bio.seq.RichSequenceIterator;

import bacci.giovanni.o2tab.pipeline.PipelineProcess;
import bacci.giovanni.o2tab.pipeline.ProcessType;
import bacci.giovanni.o2tab.util.Utils;

public class DereplicationProcess extends PipelineProcess {

	private final long minCount;

	private final static Charset CS = Charset.defaultCharset();

	private Map<ByteSequence, Long> freq;

	private final static String SUBDIR = "dereplicated";

	private final static String NAME = "dereplicated.fasta";

	private final static String FLAG = ";size=";

	public DereplicationProcess(long minCount) {
		super(ProcessType.DEREPLICATION);
		this.minCount = minCount;
		this.freq = new HashMap<DereplicationProcess.ByteSequence, Long>();
	}

	@Override
	public PipelineResult launch() throws Exception {
		if (super.getInputFiles() == null)
			throw new NullPointerException("Input files are null");
		if (super.getInputFiles().size() > 1)
			throw new IllegalArgumentException("More than one input file: "
					+ super.getInputFiles().size());
		String s = super.getInputFiles().get(0);
		RichSequenceIterator it = Utils.getSequenceIterator(s);
		while (it != null && it.hasNext()) {
			this.addSequence(it.nextSequence().seqString());
		}
		this.dumpResults();
		// Adding pooled read file to the output. It will be needed by the
		// mapping process. It is important that this operation is performed
		// after the execution of the dumpResults method otherwise the reads
		// file will be the first in the output list
		super.addOuptuFile(s);
		return PipelineResult.PASSED;
	}

	private void addSequence(String sequence) {
		ByteSequence s = new ByteSequence(sequence);
		if (freq.get(s) == null) {
			freq.put(s, 1L);
		} else {
			freq.put(s, freq.get(s) + 1);
		}
	}

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

	private BufferedWriter getWriter() throws IOException {
		Path p = Paths.get(super.getOutputDir()).resolve(SUBDIR);
		if (!Files.isDirectory(p))
			Files.createDirectories(p);
		String out = p.resolve(NAME).toString();
		super.addOuptuFile(out);
		FileWriter fw = new FileWriter(out);
		return new BufferedWriter(fw);
	}

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
