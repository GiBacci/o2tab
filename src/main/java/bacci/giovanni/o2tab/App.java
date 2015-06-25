package bacci.giovanni.o2tab;

import bacci.giovanni.o2tab.context.MicrobiomeContext;

public class App {
	public static void main(String[] args) {
		MicrobiomeContext cntx = new MicrobiomeContext(args);
		Thread t = cntx.getProcess();
		t.start();
		try {
			t.join();
		} catch (InterruptedException e) {
			System.exit(-1);
		}
	}
}
