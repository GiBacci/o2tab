package bacci.giovanni.o2tab.process;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Pipeline process class.
 * 
 * @author <a href="http://www.unifi.it/dblage/CMpro-v-p-65.html">Giovanni
 *         Bacci</a>
 *
 */
public abstract class CallableProcess implements Callable<Integer> {

	/**
	 * Main command
	 */
	private String mainCmd;

	/**
	 * The path to the main command. Useful for stand-alone executable
	 */
	private String mainCmdPath = null;

	/**
	 * Argument commands
	 */
	private Map<String, String> commands;

	/**
	 * Single commands (or flags)
	 */
	private List<String> flags;

	/**
	 * Running directory (optional)
	 */
	private String runningDir = null;

	/**
	 * Error redirection (optional)
	 */
	private Redirect error = null;

	/**
	 * Output redirectoion (optional)
	 */
	private Redirect output = null;

	/**
	 * Constructor.
	 * 
	 * @param mainCmd
	 *            the main command to execute
	 */
	public CallableProcess(String mainCmd) {
		this.mainCmd = mainCmd;
		this.commands = new LinkedHashMap<String, String>();
		this.flags = new ArrayList<String>();
	}

	/**
	 * Adds a new argument command to this process
	 * 
	 * @param cmd
	 *            the command
	 * @param value
	 *            the value
	 */
	public void addArgumentCommand(String cmd, String value) {
		if (cmd == null || value == null)
			throw new NullPointerException();
		commands.put(cmd, value);
	}

	/**
	 * Add a single command to this process
	 * 
	 * @param command
	 *            the command
	 */
	public void addSingleCommand(String command) {
		if (command == null)
			throw new NullPointerException();
		flags.add(command);
	}

	/**
	 * @return all the commands given to this process
	 */
	protected String[] getCommands() {
		List<String> cmd = new ArrayList<String>();
		if (mainCmdPath != null) {
			Path one = Paths.get(mainCmdPath);
			cmd.add(one.resolve(mainCmd).toString());
		} else {
			cmd.add(mainCmd);
		}
		for (String s : commands.keySet()) {
			cmd.add(s);
			cmd.add(commands.get(s));
		}
		for (String s : flags)
			cmd.add(s);
		return cmd.toArray(new String[cmd.size()]);
	}

	/**
	 * @param error
	 *            the error to set
	 */
	public void setError(Redirect error) {
		this.error = error;
	}

	/**
	 * @param output
	 *            the output to set
	 */
	public void setOutput(Redirect output) {
		this.output = output;
	}

	/**
	 * @param runningDir
	 *            the runningDir to set
	 */
	public void setRunningDir(String runningDir) {
		this.runningDir = runningDir;
	}

	/**
	 * @return the mainCmd
	 */
	public String getMainCmd() {
		return mainCmd;
	}

	/**
	 * Sets the main cmd for this process
	 */
	public void setMainCmd(String mainCmd) {
		this.mainCmd = mainCmd;
	}

	/**
	 * @param mainCmdPath
	 *            the mainCmdPath to set
	 */
	public void setMainCmdPath(String mainCmdPath) {
		this.mainCmdPath = mainCmdPath;
	}
	
	

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.concurrent.Callable#call()
	 */
	@Override
	public Integer call() throws IOException, InterruptedException {
		ProcessBuilder pb = new ProcessBuilder(getCommands());
		if (runningDir != null)
			pb.directory(new File(runningDir));
		if (error != null)
			pb.redirectError(error);
		if (output != null)
			pb.redirectOutput(output);
		Process p = pb.start();
		return p.waitFor();
	}

}
