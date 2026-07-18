package jls.hdl.yosys;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * PATH search for the external Yosys dependency (issue #61): the
 * first executable named {@code yosys} wins, junk entries and
 * non-files are skipped the way a shell would skip them, and absence
 * is an empty result — the caller owns turning that into the
 * dependency dialog.
 */
class YosysLocatorTest {

	@Test
	void findsTheFirstExecutable(@TempDir Path first,
			@TempDir Path second) throws Exception {
		Path decoy = second.resolve("yosys");
		Files.writeString(decoy, "#!/bin/sh\n");
		makeExecutable(decoy);
		Path winner = first.resolve("yosys");
		Files.writeString(winner, "#!/bin/sh\n");
		makeExecutable(winner);

		String searchPath = "/nonexistent-dir" + File.pathSeparator
				+ first + File.pathSeparator + second;
		Optional<Path> found = YosysLocator.find(searchPath);
		assertTrue(found.isPresent());
		assertEquals(winner, found.get());
	}

	@Test
	void absenceIsEmpty(@TempDir Path empty) {
		assertTrue(YosysLocator.find((String) null).isEmpty());
		assertTrue(YosysLocator.find("").isEmpty());
		assertTrue(YosysLocator.find(empty.toString()).isEmpty());
	}

	@Test
	void aDirectoryNamedYosysIsNotABinary(@TempDir Path dir)
			throws Exception {
		Files.createDirectory(dir.resolve("yosys"));
		assertTrue(YosysLocator.find(dir.toString()).isEmpty());
	}

	@Test
	void aNonExecutableFileIsSkipped(@TempDir Path dir)
			throws Exception {
		Path plain = dir.resolve("yosys");
		Files.writeString(plain, "not a program");
		plain.toFile().setExecutable(false, false);
		// under some accounts (root) everything is executable;
		// the premise of this test then cannot be established
		assumeTrue(!Files.isExecutable(plain),
				"cannot make a non-executable file on this account");
		assertTrue(YosysLocator.find(dir.toString()).isEmpty());
	}

	/**
	 * Marks a fixture file executable, failing the test setup
	 * loudly if the platform refuses.
	 *
	 * @param file The fixture to mark.
	 */
	private static void makeExecutable(Path file) {
		assumeTrue(file.toFile().setExecutable(true, false)
				|| Files.isExecutable(file),
				"cannot mark fixtures executable on this platform");
	}

} // end of YosysLocatorTest class
