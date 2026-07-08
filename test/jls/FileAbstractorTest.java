package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for the consolidated file I/O path (issue #15): errors must be
 * propagated to JLSInfo.loadError with a diagnosable reason instead of
 * being swallowed, and writes must be atomic (temp file + rename).
 */
class FileAbstractorTest {

	@TempDir
	Path tmp;

	@Test
	void missingFileReportsWhy() {
		JLSInfo.loadError = "";
		assertNull(FileAbstractor.openCircuit(tmp.resolve("ghost.jls").toString()));
		assertTrue(JLSInfo.loadError.contains("no such file"),
				"expected a missing-file diagnosis, got: " + JLSInfo.loadError);
	}

	@Test
	void unreadableFileReportsPerFormatReasons() throws Exception {
		// whitespace only: rejected by the XZ and zip probes (bad magic)
		// and by the text probe (no tokens), so every reason is reported
		File file = tmp.resolve("blank.jls").toFile();
		Files.writeString(file.toPath(), "   \n\n  \n");

		JLSInfo.loadError = "";
		assertNull(FileAbstractor.openCircuit(file.getAbsolutePath()));
		assertTrue(JLSInfo.loadError.contains("not XZ")
						&& JLSInfo.loadError.contains("not zip")
						&& JLSInfo.loadError.contains("text"),
				"expected per-format reasons, got: " + JLSInfo.loadError);
	}

	@Test
	void invalidCircuitNameIsRejected() {
		JLSInfo.loadError = "";
		assertNull(FileAbstractor.openCircuit(tmp.resolve("9bad name!.jls").toString()));
		assertTrue(JLSInfo.loadError.contains("not a valid circuit file name"),
				"expected a name diagnosis, got: " + JLSInfo.loadError);
	}

	@Test
	void emptyFileIsRejectedNotReturnedEmpty() throws Exception {
		File file = tmp.resolve("empty.jls").toFile();
		Files.write(file.toPath(), new byte[0]);

		JLSInfo.loadError = "";
		assertNull(FileAbstractor.openCircuit(file.getAbsolutePath()));
		assertTrue(JLSInfo.loadError.contains("no content"),
				"expected an empty-file diagnosis, got: " + JLSInfo.loadError);
	}

	@Test
	void writeCircuitRoundTripsThroughTheSniffer() throws Exception {
		String content = "CIRCUIT written\nENDCIRCUIT\n";
		File file = tmp.resolve("written.jls").toFile();
		FileAbstractor.writeCircuit(file, content);

		Scanner scanner = FileAbstractor.openCircuit(file.getAbsolutePath());
		assertNotNull(scanner, "written file must reopen: " + JLSInfo.loadError);
		assertEquals(content, FileFormatSupport.drain(scanner));
	}

	@Test
	void writeCircuitReplacesExistingFileAndLeavesNoTemp() throws Exception {
		File file = tmp.resolve("replace.jls").toFile();
		FileAbstractor.writeCircuit(file, "CIRCUIT old\nENDCIRCUIT\n");
		FileAbstractor.writeCircuit(file, "CIRCUIT newer\nENDCIRCUIT\n");

		Scanner scanner = FileAbstractor.openCircuit(file.getAbsolutePath());
		assertNotNull(scanner);
		assertEquals("CIRCUIT newer\nENDCIRCUIT\n", FileFormatSupport.drain(scanner));
		assertEquals(0, tmp.toFile().listFiles((d, n) -> n.endsWith(".tmp")).length,
				"temp file must not survive a successful write");
	}
}
