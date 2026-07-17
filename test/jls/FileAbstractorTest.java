package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

	// ------------------------------------------------------------------
	// opt-in plain-text container (issue #129)
	// ------------------------------------------------------------------

	/** The XZ stream magic bytes (spec docs/file-format.md section 1). */
	private static final byte[] XZ_MAGIC =
			{(byte) 0xFD, '7', 'z', 'X', 'Z', 0};

	private static boolean startsWithXZMagic(File file) throws Exception {
		byte[] head = Files.readAllBytes(file.toPath());
		if (head.length < XZ_MAGIC.length) {
			return false;
		}
		for (int i = 0; i < XZ_MAGIC.length; i++) {
			if (head[i] != XZ_MAGIC[i]) {
				return false;
			}
		}
		return true;
	}

	@Test
	void aFreshCircuitDefaultsToTheXZContainer() {
		// issue #129 P3: the GUI save path takes its container from the
		// circuit, so the circuit's default pins the default save format
		assertEquals(FileAbstractor.Container.XZ,
				new Circuit("fresh").getSaveContainer(),
				"a circuit must default to the XZ save container");
	}

	@Test
	void defaultWriteIsStillXZCompressed() throws Exception {
		// issue #129 P3: the default container must not change
		File file = tmp.resolve("dflt.jls").toFile();
		FileAbstractor.writeCircuit(file, "CIRCUIT dflt\nENDCIRCUIT\n");
		assertTrue(startsWithXZMagic(file),
				"a default save must still be an XZ stream");
	}

	@Test
	void plainTextWriteIsTheBareCircuitText() throws Exception {
		// issue #129 H1: the plain-text container is the identical text
		// payload minus the XZ wrapper -- byte-for-byte on disk
		String content = "CIRCUIT plain\nENDCIRCUIT\n";
		File file = tmp.resolve("plain.jls").toFile();
		FileAbstractor.writeCircuit(file, content,
				FileAbstractor.Container.PLAIN_TEXT);

		assertEquals(content, Files.readString(file.toPath()),
				"the file must hold the bare UTF-8 circuit text");
		Scanner scanner = FileAbstractor.openCircuit(file.getAbsolutePath());
		assertNotNull(scanner, "plain-text save must reopen: " + JLSInfo.loadError);
		assertEquals(content, FileFormatSupport.drain(scanner));
		assertEquals(0, tmp.toFile().listFiles((d, n) -> n.endsWith(".tmp")).length,
				"temp file must not survive a successful write");
	}

	@Test
	void bothContainersLoadTheSameCircuitText() throws Exception {
		// cross-container equivalence: the same circuit saved both ways
		// loads identically through the sniffer
		String content = "CIRCUIT both\nENDCIRCUIT\n";
		File xz = tmp.resolve("both_xz.jls").toFile();
		File text = tmp.resolve("both_text.jls").toFile();
		FileAbstractor.writeCircuit(xz, content, FileAbstractor.Container.XZ);
		FileAbstractor.writeCircuit(text, content,
				FileAbstractor.Container.PLAIN_TEXT);

		assertTrue(startsWithXZMagic(xz), "XZ save must be an XZ stream");
		assertFalse(startsWithXZMagic(text),
				"plain-text save must not be an XZ stream");
		Scanner fromXz = FileAbstractor.openCircuit(xz.getAbsolutePath());
		Scanner fromText = FileAbstractor.openCircuit(text.getAbsolutePath());
		assertNotNull(fromXz);
		assertNotNull(fromText);
		assertEquals(FileFormatSupport.drain(fromXz),
				FileFormatSupport.drain(fromText),
				"both containers must yield the same circuit text");
	}

	@Test
	void plainTextWriteReplacesAnXZFileAtomically() throws Exception {
		// re-saving an XZ file as plain text is the interop conversion
		// (issue #129); it must go through the same temp/rename machinery
		File file = tmp.resolve("convert.jls").toFile();
		FileAbstractor.writeCircuit(file, "CIRCUIT old\nENDCIRCUIT\n");
		FileAbstractor.writeCircuit(file, "CIRCUIT convert\nENDCIRCUIT\n",
				FileAbstractor.Container.PLAIN_TEXT);

		assertEquals("CIRCUIT convert\nENDCIRCUIT\n",
				Files.readString(file.toPath()));
		assertEquals(0, tmp.toFile().listFiles((d, n) -> n.endsWith(".tmp")).length,
				"temp file must not survive a successful write");
	}
}
