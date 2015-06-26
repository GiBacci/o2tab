package bacci.giovanni.o2tab.pipeline;

/**
 * Pipeline process wrapper.
 * 
 * @author <a href="http://www.unifi.it/dblage/CMpro-v-p-65.html">Giovanni
 *         Bacci</a>
 *
 */
public class MonitoredPipelineProcess extends PipelineProcess {

	/**
	 * The process
	 */
	private final PipelineProcess process;

	private PipelineResult res;

	private Exception ex;

	private final static String[] WAIT = { ".", "..", "..." };

	/**
	 * Constructor
	 * 
	 * @param process
	 *            the process to wrap
	 */
	public MonitoredPipelineProcess(PipelineProcess process) {
		super(process.getProcessType());
		this.process = process;
	}

	@Override
	public PipelineResult launch() throws Exception {
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					res = process.launch();
				} catch (Exception e) {
					ex = e;
				}
			}
		});
		t.start();
		int index = 0;
		try {
			while (t.isAlive()) {
				System.out.print(formatMsg(WAIT[index++]));
				if (index == WAIT.length)
					index = 0;
				Thread.sleep(200);
			}
		} catch (InterruptedException e) {
			System.out.println("process interrupted");
			t.interrupt();
		}
		if (ex != null) {
			System.out.println("process exited with error/s");
			throw ex;
		}
		if (res == PipelineResult.PASSED) {
			System.out.println("process finished correctly");
		} else {
			String msg = String
					.format("process finished with error/s.%n    Check log file in: %s%n",
							process.getOutputDir());
			System.out.println(msg);
		}
		return res;
	}

	private String formatMsg(String wait) {
		return String.format("%s process started %s\r",
				this.process.getProcessType(), wait);
	}

}
