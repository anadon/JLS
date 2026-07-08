package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.File;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.io.FileOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Regression tests for issue #2: zip-format circuit files were truncated
 * on load because the ZipFile was closed before its entry stream was
 * drained. Small payloads fit inside the Scanner's already-filled buffer
 * and survived; anything larger lost its tail.
 */
class ZipLoadingTest {

	@TempDir
	Path tmp;

	@Test
	void largeZipEntryIsReadCompletely() throws Exception {
		// Build a payload far larger than any internal Scanner buffer so
		// truncation (the #2 failure mode) is unmissable.
		StringBuilder payload = new StringBuilder();
		for (int i = 0; i < 100_000; i++) {
			payload.append("line ").append(i).append('\n');
		}
		String content = payload.toString();

		File file = tmp.resolve("bigcircuit.jls").toFile();
		FileFormatSupport.writeZip(file, content);

		Scanner scanner = FileFormatSupport.openAsZip(file);
		assertNotNull(scanner, "zip file should be recognized");
		assertEquals(content, FileFormatSupport.drain(scanner),
				"entire zip entry must survive the load, not just the buffered prefix");
	}

	@Test
	void zipWithoutCircuitEntryIsRejectedNotCrashed() throws Exception {
		File file = tmp.resolve("nocircuit.jls").toFile();
		try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(file))) {
			zip.putNextEntry(new ZipEntry("SomethingElse"));
			zip.write("not a circuit".getBytes());
			zip.closeEntry();
		}

		assertNull(FileFormatSupport.openAsZip(file),
				"a zip without a JLSCircuit entry must be rejected so the sniffer can move on");
	}

	@Test
	void nonZipFileIsRejected() throws Exception {
		File file = tmp.resolve("plain.jls").toFile();
		FileFormatSupport.writeText(file, "CIRCUIT foo\nENDCIRCUIT\n");

		assertNull(FileFormatSupport.openAsZip(file),
				"plain text is not a zip; the zip probe must return null");
	}
}
