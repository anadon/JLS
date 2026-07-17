package jls;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The circuit-open path must not hold the file open behind the Scanner
 * it returns (issue #111): on Windows an open handle blocks deletion,
 * which both broke the test suite's @TempDir cleanup and would keep a
 * user's circuit file locked for as long as it is open in the editor.
 * The reader drains every container into memory (bounded, issue #38),
 * so the handle's lifetime ends inside the open call.
 *
 * The observation is Linux-specific (/proc/self/fd), which is fine: the
 * leak was platform-independent, only its delete-blocking symptom was
 * Windows-only.
 */
class FileHandleReleaseTest {

	@TempDir
	Path tmp;

	private static final String CIRCUIT_TEXT =
			"CIRCUIT handles\n"
			+ "ELEMENT Constant\n"
			+ " int id 0\n int x 60\n int y 60\n int width 24\n int height 24\n"
			+ " Int value 5\n int base 10\n String orient \"RIGHT\"\nEND\n"
			+ "ENDCIRCUIT\n";

	/** Whether any of this process's fds points at the given file. */
	private static boolean fdOpenOn(Path file) throws IOException {
		Path fdDir = Path.of("/proc/self/fd");
		assumeTrue(Files.isDirectory(fdDir),
				"fd observation needs /proc (Linux)");
		String target = file.toAbsolutePath().toString();
		try (Stream<Path> fds = Files.list(fdDir)) {
			return fds.anyMatch(fd -> {
				try {
					return Files.readSymbolicLink(fd).toString()
							.equals(target);
				} catch (IOException gone) {
					return false; // fd closed while listing
				}
			});
		}
	}

	private void assertOpenReleasesTheFile(File file) throws Exception {
		Scanner scanner = FileAbstractor.openCircuit(file.getAbsolutePath());
		assertNotNull(scanner, () -> "open failed: " + JLSInfo.loadError);
		try {
			assertTrue(!fdOpenOn(file.toPath()),
					"no file descriptor may remain open on "
							+ file.getName()
							+ " once openCircuit has returned");

			// and the drained content still loads normally
			Circuit circuit = new Circuit("handles");
			assertTrue(circuit.load(scanner),
					() -> "load failed: " + JLSInfo.loadError);
			assertTrue(circuit.finishLoad(null),
					() -> "finishLoad failed: " + JLSInfo.loadError);
		} finally {
			scanner.close();
		}
	}

	@Test
	void plainTextOpenHoldsNoHandle() throws Exception {
		File file = tmp.resolve("text.jls").toFile();
		FileFormatSupport.writeText(file, CIRCUIT_TEXT);
		assertOpenReleasesTheFile(file);
	}

	@Test
	void xzOpenHoldsNoHandle() throws Exception {
		File file = tmp.resolve("xz.jls").toFile();
		FileFormatSupport.writeXZ(file, CIRCUIT_TEXT);
		assertOpenReleasesTheFile(file);
	}

	@Test
	void zipOpenHoldsNoHandle() throws Exception {
		File file = tmp.resolve("zip.jls").toFile();
		FileFormatSupport.writeZip(file, CIRCUIT_TEXT);
		assertOpenReleasesTheFile(file);
	}
}
