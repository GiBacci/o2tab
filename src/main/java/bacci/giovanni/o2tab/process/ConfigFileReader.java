package bacci.giovanni.o2tab.process;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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
	 * @throws BadConfigFileFormatException
	 *             if the config file is not well formatted
	 */
	public T setExternalArguments(T callableProcess) throws IOException,
			BadConfigFileFormatException {
		InputStream in = getClass().getResourceAsStream(input);
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		String line = null;
		while ((line = reader.readLine()) != null) {
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
			if(filterLine.startsWith(CMD)){
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
}