package bacci.giovanni.o2tab.process;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import bacci.giovanni.o2tab.exceptions.BadConfigFileFormatException;

/**
 * Config file reader for external process
 * 
 * @author <a href="http://www.unifi.it/dblage/CMpro-v-p-65.html">Giovanni
 *         Bacci</a>
 *
 * @param <T>
 *            the type of process
 */
public class ConfigFileReader<T extends CallableProcess> {

	/**
	 * The name of the config file in the config dir
	 */
	private final String input;

	/**
	 * Token for argument options
	 */
	private final static String ARG = "ARG";

	/**
	 * Token for flag options
	 */
	private final static String VAL = "VAL";

	/**
	 * Token for environment directory (the directory where the executable is
	 * located)
	 */
	private final static String ENV = "ENV";

	/**
	 * Token for process command
	 */
	private final static String CMD = "CMD";

	/**
	 * Comment
	 */
	private final static String COMMENT = "\\s*#";

	/**
	 * Field separator
	 */
	private final static String SEP = "\\s";

	/**
	 * The name of the folder containing the config files
	 */
	private final static String CONFIG_FOLDER = "config";

	/**
	 * @param input
	 *            the name of the config file
	 */
	public ConfigFileReader(String input) {
		this.input = input;
	}

	/**
	 * Set the external parameters for the given process and return it
	 * 
	 * @param callableProcess
	 *            the process to set
	 * @return the process with the all the options added
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public T setExternalArguments(T callableProcess) throws IOException {
		BufferedReader reader = this.getReader();
		String line = null;
		while ((line = reader.readLine()) != null) {
			String[] arr = line.split(COMMENT);
			if (arr.length == 0)
				continue;
			String filterLine = line.split(COMMENT)[0];
			if (filterLine.isEmpty())
				continue;
			if (filterLine.startsWith(ARG)) {
				String[] args = getArgs(filterLine, 3);
				callableProcess.addArgumentCommand(args[1], args[2]);
				continue;
			}
			if (filterLine.startsWith(ENV)) {
				String[] args = getArgs(filterLine, 2);
				callableProcess.setMainCmdPath(args[1]);
				continue;
			}
			if (filterLine.startsWith(CMD)) {
				String[] args = getArgs(filterLine, 2);
				callableProcess.setMainCmd(args[1]);
			}
			if (filterLine.startsWith(VAL)) {
				String[] args = getArgs(filterLine, 2);
				callableProcess.addSingleCommand(args[1]);
				continue;
			}
		}
		reader.close();
		return callableProcess;
	}

	/**
	 * @return a reader pointing to the config file
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	private BufferedReader getReader() throws IOException {
		Path path = Paths.get(System.getProperty("user.dir"));
		FolderFind ff = new FolderFind();
		Files.walkFileTree(path, ff);
		if (ff.getFound() == null)
			throw new FileNotFoundException("Cannot find config file: " + input);
		return new BufferedReader(new FileReader(ff.getFound().toString()));
	}

	/**
	 * @param line
	 *            the line of the config file
	 * @param argsLenght
	 *            the length of the arguments
	 * @return an array with the parsed arguments
	 * @throws BadConfigFileFormatException
	 *             if the file is not well formatted
	 */
	private String[] getArgs(String line, int argsLenght)
			throws BadConfigFileFormatException {
		String[] args = line.split(SEP);
		if (args.length != argsLenght) {
			String error = String.format(
					"VAL must have %d arguments but found %d", argsLenght,
					args.length);
			throw new BadConfigFileFormatException(error);
		}
		return args;
	}

	/**
	 * Searches for the config file
	 * 
	 * @author <a href="http://www.unifi.it/dblage/CMpro-v-p-65.html">Giovanni
	 *         Bacci</a>
	 *
	 */
	private class FolderFind extends SimpleFileVisitor<Path> {
		Path found = null;

		@Override
		public FileVisitResult preVisitDirectory(Path dir,
				BasicFileAttributes attrs) throws IOException {
			if (dir.getFileName().toString().equals(CONFIG_FOLDER)
					&& Files.isDirectory(dir) && Files.isReadable(dir)) {
				found = Paths.get(input).getFileName();
				found = dir.resolve(found);
				return FileVisitResult.TERMINATE;
			}
			return FileVisitResult.CONTINUE;
		}

		/**
		 * @return the found
		 */
		public Path getFound() {
			return found;
		}

	}
}
