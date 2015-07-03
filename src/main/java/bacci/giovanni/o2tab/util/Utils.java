package bacci.giovanni.o2tab.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.biojavax.bio.seq.RichSequence;
import org.biojavax.bio.seq.RichSequenceIterator;

public class Utils {

	/**
	 * @param file
	 *            the input file
	 * @return a {@link RichSequenceIterator} pointing to the input file
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public static RichSequenceIterator getSequenceIterator(String file)
			throws IOException {
		Path p = Paths.get(file);
		if (Files.size(p) <= 0)
			return null;
		FileReader fr = new FileReader(file);
		BufferedReader br = new BufferedReader(fr);
		return RichSequence.IOTools.readFastaDNA(br, null);
	}

	/**
	 * @param seq
	 *            the sequence
	 * @param basePerLine
	 *            number of bases befor new line
	 * @return a formatted sequence
	 */
	public static String formatFASTA(String seq, int basePerLine) {
		String rgx = String.format("(?<=\\G.{%d})", basePerLine);
		return seq.replaceAll(rgx, System.lineSeparator()).toUpperCase();
	}

	/**
	 * @param dir
	 *            the input directory
	 * @param contains
	 *            files containing this string will be collected. If
	 *            <code>null</code> all files will be collected
	 * @return a list of files
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public static List<String> getInputs(String dir, final String contains)
			throws IOException {
		List<String> inputs = new ArrayList<String>();
		DirectoryStream.Filter<Path> myFilter = new DirectoryStream.Filter<Path>() {
			@Override
			public boolean accept(Path entry) throws IOException {
				if (contains == null)
					return true;
				String name = entry.getFileName().toString();
				return !Files.isDirectory(entry) && name.contains(contains);
			}
		};
		for (Path p : Files.newDirectoryStream(Paths.get(dir), myFilter))
			inputs.add(p.toString());
		return inputs;
	}

	/**
	 * @param sequence
	 *            the sequence to trim
	 * @param min
	 *            if <code>sequence.length() < min</code> this method will
	 *            return <code>null</code>. If this value is less than 0 it will
	 *            not be considered in the trimming.
	 * @param max
	 *            if <code>sequence.length() > max</code> this method will
	 *            return <code>sequence.substring(0, max)</code>. If this value
	 *            is less than 0 it will not be considered in the trimming.
	 * @return a sequence trimmed according to the given values or
	 *         <code>null</code>
	 */
	public static String truncate(String sequence, int min, int max) {
		int size = sequence.length();
		if (min >= 0 && size < min)
			return null;
		if (max >= 0 && size > max)
			return sequence.substring(0, max);
		return sequence;
	}

}
