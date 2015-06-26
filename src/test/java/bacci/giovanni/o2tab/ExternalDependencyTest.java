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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import bacci.giovanni.o2tab.pipeline.PipelineProcess;
import bacci.giovanni.o2tab.pipeline.PipelineProcess.PipelineResult;
import bacci.giovanni.o2tab.process.ClusteringOTU;
import bacci.giovanni.o2tab.process.MappingProcess;
import bacci.giovanni.o2tab.process.PANDAseqProcessBuilder;
import bacci.giovanni.o2tab.process.TableProcess;

@RunWith(BlockJUnit4ClassRunner.class)
public class ExternalDependencyTest extends TestCase {

	@Test
	public void testPandaSeq() {
		List<String> inputs = this.getTestResources("*.fastq");
		assertEquals(2, inputs.size());
		PipelineProcess panda = null;
		try {
			panda = new PANDAseqProcessBuilder("_1", "_2").setInputFile(inputs);
			PipelineResult out = panda.launch();
			assertEquals(PipelineResult.PASSED, out);
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

	@AfterClass
	public static void clean() {
		Path p = Paths.get(PipelineProcess.getDefaultOutput());
		DeleteVisitor visitor = new DeleteVisitor();
		try {
			Files.walkFileTree(p, visitor);
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

		try {
			PipelineProcess otu = new ClusteringOTU().setInputFile(inputs);
			assertEquals(PipelineResult.PASSED, otu.launch());
			PipelineProcess map = new MappingProcess().setInputFile(otu
					.getOutputFiles());
			assertEquals(PipelineResult.PASSED, map.launch());
			PipelineProcess tab = new TableProcess().setInputFile(map
					.getOutputFiles());
			assertEquals(PipelineResult.PASSED, tab.launch());
		} catch (FileNotFoundException e) {
			fail("Test files not found");
		} catch (Exception e) {
			fail(this.formatErrorMessage("usearch", e));
		}

	}

	private String formatErrorMessage(String program, Exception e) {
		String msg = String
				.format("Error launching %s%n message: %s%n Try to modify the "
						+ "config file or reinstall the program",
						program, e.getMessage());
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
