package bacci.giovanni.o2tab.pipeline;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Process results object. This object stores all the informations concerning
 * the exit status of a pipeline process.
 * 
 * @author <a href="http://www.unifi.it/dblage/CMpro-v-p-65.html">Giovanni
 *         Bacci</a>
 *
 */
public class ProcessResult {

	/**
	 * The result
	 */
	private PipelineResult res;

	/**
	 * Warning set
	 */
	private Set<String> warnings;

	/**
	 * Fail set
	 */
	private Set<String> fails;

	/**
	 * Constructor
	 * 
	 * @param res
	 *            the result
	 */
	public ProcessResult(PipelineResult res) {
		this.res = res;
		this.warnings = new LinkedHashSet<String>();
		this.fails = new LinkedHashSet<String>();
	}

	/**
	 * @param message
	 *            a warning message to add at this result object
	 */
	public void addWarning(String message) {
		this.warnings.add(message);
	}

	/**
	 * @param error
	 *            an error message to add at this result object
	 */
	public void addFail(String error) {
		this.fails.add(error);
	}

	/**
	 * Adds all warning messages to this object
	 * 
	 * @param warnings
	 *            the messages
	 */
	public void addAllWarnings(List<String> warnings) {
		this.warnings.addAll(warnings);
	}

	/**
	 * Adds all error messages to this object
	 * 
	 * @param errors
	 *            the messages
	 */
	public void addAllFails(List<String> errors) {
		this.fails.addAll(errors);
	}

	/**
	 * @return the warnings
	 */
	public Set<String> getWarnings() {
		return warnings;
	}

	/**
	 * @return the fails
	 */
	public Set<String> getFails() {
		return fails;
	}

	/**
	 * @return the res
	 */
	public PipelineResult getRes() {
		return res;
	}

	/**
	 * Pipeline result tokens
	 * 
	 * @author <a href="http://www.unifi.it/dblage/CMpro-v-p-65.html">Giovanni
	 *         Bacci</a>
	 *
	 */
	public enum PipelineResult {
		FAILED, PASSED, PASSED_WITH_WARNINGS, FAILED_WITH_WARNINGS, INTERRUPTED;
	}

}
