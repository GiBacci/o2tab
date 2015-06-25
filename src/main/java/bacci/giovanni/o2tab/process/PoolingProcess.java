package bacci.giovanni.o2tab.process;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.biojava.bio.BioException;
import org.biojava.bio.seq.Sequence;
import org.biojavax.bio.seq.RichSequenceIterator;

import bacci.giovanni.o2tab.pipeline.PipelineProcess;
import bacci.giovanni.o2tab.pipeline.ProcessType;
import bacci.giovanni.o2tab.util.Utils;

/**
 * Pooling class for multiple FASTA files
 * 
 * @author <a href="http://www.unifi.it/dblage/CMpro-v-p-65.html">Giovanni
 *         Bacci</a>
 *
 */
public class PoolingProcess extends PipelineProcess {

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
	 * If > 0 sequences will be truncated at the specified index.
	 */
	private int truncate = -1;	

	/**
	 * Constructor
	 */
	public PoolingProcess() {
		super(ProcessType.POOLING);
	}

	@Override
	public PipelineResult launch() throws Exception {
		if (getInputFiles() == null)
			throw new NullPointerException("Input files are null");
		BufferedWriter writer = createWriter();
		for (String file : getInputFiles()) {
			String barcode = getBarcode(file);
			try {
				RichSequenceIterator it = Utils.getSequenceIterator(file);
				while (it != null && it.hasNext()) {
					Sequence s = it.nextSequence();
					String id = String.format(">%s%s%s", s.getName(), BARCODE,
							barcode);
					String seq = Utils.formatFASTA(s.seqString(), 80);
					if (truncate > 0 && truncate < seq.length())
						seq = seq.substring(0, truncate);
					writer.write(id);
					writer.newLine();
					writer.write(seq);
					writer.newLine();
					writer.flush();
				}
			} catch (BioException e) {
				throw new BioException("Could not read sequence in file: "
						+ barcode);
			}
		}
		writer.close();
		return PipelineResult.PASSED;
	}

	public PoolingProcess truncate(int truncate) {
		this.truncate = truncate;
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

}
