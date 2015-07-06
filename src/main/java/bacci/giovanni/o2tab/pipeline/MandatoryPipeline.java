package bacci.giovanni.o2tab.pipeline;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.NoSuchFileException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import bacci.giovanni.o2tab.pipeline.ProcessResult.PipelineResult;

/**
 * This pipeline executes all process in the order they were added. If a process
 * stops this pipeline stops too.
 * 
 * @author <a href="http://www.unifi.it/dblage/CMpro-v-p-65.html">Giovanni
 *         Bacci</a>
 *
 */
public class MandatoryPipeline extends PipelineProcessQueue {
	
	public MandatoryPipeline(int queueSize, Logger logger, String output)
			throws IOException {
		super(queueSize, logger, output);
	}

	public MandatoryPipeline() throws IOException {
		super();
	}

	public MandatoryPipeline(int queueSize, Logger logger) throws IOException {
		super(queueSize, logger);
	}

	public MandatoryPipeline(int queueSize) throws IOException {
		super(queueSize);
	}

	public MandatoryPipeline(Logger logger) throws IOException {
		super(logger);
	}

	@Override
	public void run() {
		List<String> inputFiles = null;
		boolean first = true;
		for (PipelineProcess pp : super.processList) {

			ProcessResult res = null;
			if (!first && inputFiles != null)
				pp.setInputFiles(inputFiles);

			try {
				res = new MonitoredPipelineProcess(pp).launch();
			} catch (AccessDeniedException e) {
				super.log(Level.SEVERE, "Access denied " + e.getMessage());
				System.exit(-1);
			} catch (NoSuchFileException e) {
				super.log(Level.SEVERE,
						"No file/directoy found: " + e.getMessage());
				System.exit(-1);
			} catch (IOException e) {
				super.log(Level.SEVERE, e.getMessage());
				System.exit(-1);
			} 
			
			if (res.getRes() == PipelineResult.INTERRUPTED)
				return;

			if (res.getRes() == PipelineResult.FAILED
					|| res.getRes() == PipelineResult.FAILED_WITH_WARNINGS) {
				return;
			}

			first = false;
			inputFiles = pp.getOutputFiles();
		}
	}

}
