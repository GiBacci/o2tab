package bacci.giovanni.o2tab.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
}
