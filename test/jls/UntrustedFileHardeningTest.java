package jls;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;

import jls.elem.Memory;

/**
 * Hostile circuit files must be rejected cleanly instead of exhausting
 * memory or leaking file descriptors (issue #38, audit findings M2/M3;
 * SECURITY.md documents live malicious-attachment campaigns against
 * this repository).
 */
class UntrustedFileHardeningTest {

	@org.junit.jupiter.api.io.TempDir
	Path tmp;

	@Test
	void oversizedZipEntryIsRejected() throws Exception {
		// a small archive that inflates past the circuit size limit
		File bomb = tmp.resolve("bomb.jls").toFile();
		byte[] chunk = new byte[1 << 20]; // 1 MiB of zeros, compresses tiny
		try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(bomb))) {
			zip.putNextEntry(new ZipEntry("JLSCircuit"));
			for (int i = 0; i <= (FileAbstractor.MAX_CIRCUIT_TEXT_BYTES >> 20); i++) {
				zip.write(chunk);
			}
			zip.closeEntry();
		}
		long start = System.currentTimeMillis();
		Scanner scanner = FileAbstractor.openCircuit(bomb.getAbsolutePath());
		assertNull(scanner, "an over-limit zip entry must be rejected");
		assertTrue(JLSInfo.loadError.contains("limit"),
				"the rejection must name the size limit: " + JLSInfo.loadError);
		assertTrue(System.currentTimeMillis() - start < 30_000,
				"rejection must be prompt");
	}

	@Test
	void rleIsBoundedByDeclaredCapacity() {
		Circuit circuit = new Circuit("mem");
		boolean ok = circuit.load(new Scanner(
				"CIRCUIT mem\n"
				+ "ELEMENT Memory\n"
				+ " int id 0\n int x 60\n int y 60\n int width 32\n int height 32\n"
				+ " String name \"mem\"\n int bits 8\n int cap 4\n"
				+ " String initrle \"0:1:7fffff\"\n"
				+ "END\n"
				+ "ENDCIRCUIT\n"));
		assertFalse(ok, "an RLE run past the declared capacity must fail the load");
		assertTrue(JLSInfo.loadError.contains("capacity"),
				"got: " + JLSInfo.loadError);
	}

	@Test
	void sniffingCascadeDoesNotLeakFileDescriptors() throws Exception {
		// every plain-text open probes (and rejects) the XZ path first;
		// the raw stream used to leak once per open
		File file = tmp.resolve("plain.jls").toFile();
		java.nio.file.Files.writeString(file.toPath(),
				"CIRCUIT plain\nENDCIRCUIT\n", StandardCharsets.UTF_8);

		File fdDir = new File("/proc/self/fd");
		org.junit.jupiter.api.Assumptions.assumeTrue(fdDir.isDirectory(),
				"needs /proc (Linux)");
		int before = fdDir.list().length;
		for (int i = 0; i < 512; i++) {
			Scanner scanner = FileAbstractor.openCircuit(file.getAbsolutePath());
			assertNotNull(scanner);
			scanner.close();
		}
		int after = fdDir.list().length;
		assertTrue(after - before < 32,
				"file descriptors leaked: " + before + " -> " + after);
	}
}
