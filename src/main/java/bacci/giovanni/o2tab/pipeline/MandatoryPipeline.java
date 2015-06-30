package bacci.giovanni.o2tab.pipeline;

import java.io.FileNotFoundException;
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
		// TODO Auto-generated constructor stub
	}

	public MandatoryPipeline(int queueSize, Logger logger) {
		super(queueSize, logger);
		// TODO Auto-generated constructor stub
	}

	public MandatoryPipeline(int queueSize) {
		super(queueSize);
		// TODO Auto-generated constructor stub
	}

	public MandatoryPipeline(Logger logger) {
		super(logger);
		// TODO Auto-generated constructor stub
	}

	@Override
	public void run() {
		List<String> inputFiles = null;
		for (int i = 0; i < super.processList.size(); i++) {
			boolean first = i == 0;
			PipelineProcess pp = super.processList.get(i);
			PipelineResult res = null;
			try {
				if (!first && inputFiles != null)
					pp.setInputFile(inputFiles);
				res = new MonitoredPipelineProcess(pp).launch();
			} catch (FileNotFoundException e1) {
				super.log(Level.SEVERE, "Input files not found", e1);
				return;
			} catch (Exception e) {
				String msg = String.format(
						"Error in %s process%n    message: %s",
						pp.getProcessType(), e.getMessage());
				super.log(Level.SEVERE, msg, e);
				return;
			}

			if (res == null || res == PipelineResult.FAILED) {
				String msg = String.format(
						"Pipeline process failed%n    name: %s", pp.getClass()
								.getName());
				super.log(Level.SEVERE, msg);
				return;
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
