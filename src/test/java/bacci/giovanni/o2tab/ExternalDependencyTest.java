package bacci.giovanni.o2tab;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.TestCase;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import bacci.giovanni.o2tab.pipeline.PipelineProcess;
import bacci.giovanni.o2tab.pipeline.ProcessResult;
import bacci.giovanni.o2tab.pipeline.ProcessResult.PipelineResult;
import bacci.giovanni.o2tab.process.ClusteringOTU;
import bacci.giovanni.o2tab.process.MappingProcess;
import bacci.giovanni.o2tab.process.PANDAseqProcessBuilder;
import bacci.giovanni.o2tab.process.TableProcess;

@RunWith(BlockJUnit4ClassRunner.class)
public class ExternalDependencyTest extends TestCase {

	private static Path outTest = Paths.get(System.getProperty("user.dir"))
			.resolve("test");

	@Test
	public void testPandaSeq() {
		List<String> inputs = this.getTestResources("*.fastq");
		assertEquals(2, inputs.size());
		PipelineProcess panda = null;
		try {
			panda = new PANDAseqProcessBuilder("_1", "_2")
					.setInputFiles(inputs);
			panda.setMainOutputDir(outTest.toString());
			ProcessResult out = panda.launch();
			assertEquals(PipelineResult.PASSED, out.getRes());
		} catch (FileNotFoundException e) {
			fail("Test files not found");
		} catch (Exception e) {
			fail(this.formatErrorMessage("pandaseq", e));
		}
	}

	private List<String> getTestResources(String glob) {
		Path res = null;
		try {
			res = Paths.get(this.getClass().getResource("/").toURI());
		} catch (URISyntaxException e) {
			fail("Syntax error reading test resources");
		}
		assertNotNull("Unable to get test resources", res);
		List<String> inputs = new ArrayList<String>();
		try {
			for (Path p : Files.newDirectoryStream(res, glob))
				inputs.add(p.toString());
		} catch (IOException e) {
			fail("I/O exception reading test resources");
		}
		return inputs;
	}

	@BeforeClass
	public static void build() {
		if (!Files.isDirectory(outTest))
			try {
				Files.createDirectories(outTest);
			} catch (IOException e) {
				String msg = String.format(
						"Cannot create one or more test output file/s%n    %s",
						e.getMessage());
				fail(msg);
			}
	}

	@AfterClass
	public static void clean() {
		DeleteVisitor visitor = new DeleteVisitor();
		try {
			Files.walkFileTree(outTest, visitor);
		} catch (IOException e) {
			String msg = String.format(
					"Cannot delete one or more test output file/s%n    %s",
					e.getMessage());
			fail(msg);
		}
	}

	@Test
	public void testUserach() {
		List<String> inputs = getTestResources("*.fasta");
		assertEquals(2, inputs.size());
		Collections.sort(inputs);
		String processName = "usearch";
		try {
			PipelineProcess otu = new ClusteringOTU().setInputFiles(inputs);
			otu.setMainOutputDir(outTest.toString());
			processName = otu.getProcessType().toString();
			assertEquals(formatFailMessage(processName), PipelineResult.PASSED,
					otu.launch().getRes());
			PipelineProcess map = new MappingProcess().setInputFiles(otu
					.getOutputFiles());
			map.setMainOutputDir(outTest.toString());
			processName = map.getProcessType().toString();
			assertEquals(formatFailMessage(processName), PipelineResult.PASSED,
					map.launch().getRes());
			PipelineProcess tab = new TableProcess().setInputFiles(map
					.getOutputFiles());
			tab.setMainOutputDir(outTest.toString());
			processName = tab.getProcessType().toString();
			assertEquals(formatFailMessage(processName), PipelineResult.PASSED,
					tab.launch().getRes());
		} catch (FileNotFoundException e) {
			fail("Test files not found");
		} catch (IOException e) {
			fail(formatErrorMessage(processName, e));
		}

	}

	private String formatErrorMessage(String program, Exception e) {
		String msg = String.format(
				"Error launching %s%n message: %s%n Try to modify the "
						+ "config file or reinstall the program", program,
				e.getMessage());
		return msg;
	}

	private String formatFailMessage(String program) {
		String msg = String.format("Process %s failed: try to modify the "
				+ "config file or reinstall the program", program);
		return msg;
	}

	private static class DeleteVisitor extends SimpleFileVisitor<Path> {

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.nio.file.SimpleFileVisitor#visitFile(java.lang.Object,
		 * java.nio.file.attribute.BasicFileAttributes)
		 */
		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
				throws IOException {
			Files.deleteIfExists(file);
			return FileVisitResult.CONTINUE;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * java.nio.file.SimpleFileVisitor#postVisitDirectory(java.lang.Object,
		 * java.io.IOException)
		 */
		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc)
				throws IOException {
			Files.deleteIfExists(dir);
			return FileVisitResult.CONTINUE;
		}

	}
}
