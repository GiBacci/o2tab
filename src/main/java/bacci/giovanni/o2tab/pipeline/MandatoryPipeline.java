package bacci.giovanni.o2tab.pipeline;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import bacci.giovanni.o2tab.pipeline.PipelineProcess.PipelineResult;

/**
 * This pipeline executes all process in the order they were added. If a process
 * stops this pipeline stops too.
 * 
 * @author <a href="http://www.unifi.it/dblage/CMpro-v-p-65.html">Giovanni
 *         Bacci</a>
 *
 */
public class MandatoryPipeline extends PipelineProcessQueue {

	public MandatoryPipeline() {
		super();
	}

	public MandatoryPipeline(int queueSize, Logger logger) {
		super(queueSize, logger);
	}

	public MandatoryPipeline(int queueSize) {
		super(queueSize);
	}

	public MandatoryPipeline(Logger logger) {
		super(logger);
	}

	@Override
	public void run() {
		List<String> inputFiles = null;
		for (int i = 0; i < super.processList.size(); i++) {
			boolean first = (i == 0);
			PipelineProcess pp = super.processList.get(i);
			PipelineResult res = PipelineResult.FAILED;
			if (!first && inputFiles != null)
				pp.setInputFile(inputFiles);

			try {
				res = new MonitoredPipelineProcess(pp).launch();
			} catch (AccessDeniedException e) {
				super.log(Level.SEVERE, "Access denied " + e.getMessage());
				System.exit(-1);
			} catch (IOException e) {
				super.log(Level.SEVERE, e.getMessage());
				System.exit(-1);
			} catch (InterruptedException e) {
				return;
			}

			if (res == PipelineResult.FAILED) {
				String msg = String.format(
						"Pipeline process failed%n    name: %s", pp.getClass()
								.getName());
				super.log(Level.SEVERE, msg);
				System.exit(-1);
			}
			inputFiles = pp.getOutputFiles();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * bacci.giovanni.o2tab.pipeline.PipelineProcessQueue#addPipelineProcess
	 * (bacci.giovanni.o2tab.pipeline.PipelineProcess)
	 */
	@Override
	public void addPipelineProcess(PipelineProcess process) {
		ProcessType type = process.getProcessType();
		PipelineProcess subs = null;
		for (PipelineProcess p : super.processList) {
			if (p.getProcessType().equals(type)) {
				subs = p;
				break;
			}
		}
		if (subs != null) {
			int index = super.processList.indexOf(subs);
			super.processList.set(index, process);
		} else {
			super.addPipelineProcess(process);
		}
	}
}
