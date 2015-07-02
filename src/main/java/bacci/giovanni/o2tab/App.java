package bacci.giovanni.o2tab;

import java.io.IOException;

import bacci.giovanni.o2tab.context.MicrobiomeContext;

public class App {
	public static void main(String[] args) throws IOException, InterruptedException {
		MicrobiomeContext cntx = new MicrobiomeContext(args);
		Thread t = cntx.getProcess();
		t.start();
		try {
			t.join();
		} catch (InterruptedException e) {
			t.interrupt();
			System.exit(-1);
		}
		System.exit(0);
	}
}
