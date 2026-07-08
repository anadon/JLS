package jls.edit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.Scanner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jls.FileAbstractor;

/**
 * The background checkpoint writer (issue #19): writes happen off the
 * calling thread, the newest queued content wins, and the resulting file
 * is loadable through the normal format-sniffing loader.
 */
class CheckpointWriterTest {

	@TempDir
	Path tmp;

	private static String drain(Scanner scanner) {
		StringBuilder sb = new StringBuilder();
		while (scanner.hasNextLine()) {
			sb.append(scanner.nextLine()).append('\n');
		}
		scanner.close();
		return sb.toString();
	}

	/** Wait for the writer thread to produce content for the file. */
	private static void awaitWritten(File file, String expected) throws Exception {
		long deadline = System.currentTimeMillis() + 10_000;
		while (System.currentTimeMillis() < deadline) {
			if (file.isFile()) {
				Scanner scanner = FileAbstractor.openCircuit(file.getAbsolutePath());
				if (scanner != null) {
					if (expected.equals(drain(scanner)))
						return;
				}
			}
			Thread.sleep(20);
		}
		throw new AssertionError("checkpoint never reached expected content in " + file);
	}

	@Test
	void checkpointIsWrittenAndLoadable() throws Exception {
		File file = tmp.resolve("async.jls~").toFile();
		String content = "CIRCUIT async\nENDCIRCUIT\n";
		SimpleEditor.writeCheckpointInBackground(file.getAbsolutePath(), content);
		awaitWritten(file, content);

		Scanner scanner = FileAbstractor.openCircuit(file.getAbsolutePath());
		assertNotNull(scanner, "checkpoint must load through the sniffer");
		assertEquals(content, drain(scanner));
	}

	@Test
	void newerCheckpointSupersedesQueuedOne() throws Exception {
		File file = tmp.resolve("coalesce.jls~").toFile();
		String last = "";
		for (int i = 0; i < 50; i++) {
			last = "CIRCUIT version" + i + "\nENDCIRCUIT\n";
			SimpleEditor.writeCheckpointInBackground(file.getAbsolutePath(), last);
		}
		// whatever intermediate states hit the disk, the writer must
		// converge on the newest content
		awaitWritten(file, last);
		assertTrue(file.isFile());
	}
}
