package jls.hdl;

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
 * Headless, cross-platform coverage of {@link ToolLocator} (issue
 * #111 Stage W2). These run on Linux by construction: the
 * {@code PATH}/{@code PATHEXT} seam is injected, so Windows-style
 * {@code .exe}/{@code PATHEXT} resolution is asserted without a
 * Windows runner — a real fixture file {@code iverilog.exe} in a temp
 * directory is resolved via a {@code PATHEXT} that names {@code .EXE}.
 */
class ToolLocatorTest {

	@Test
	void unixFindsTheBareName(@TempDir Path dir) throws Exception {
		Path tool = dir.resolve("iverilog");
		writeExecutable(tool);

		Optional<Path> found = ToolLocator.find("iverilog",
				dir.toString(), null);
		assertTrue(found.isPresent(), "bare name should resolve");
		assertEquals(tool.getFileName(), found.get().getFileName());
	}

	@Test
	void windowsStyleFindsDotExeViaPathext(@TempDir Path dir)
			throws Exception {
		// A lowercase .exe fixture resolved through an uppercase .EXE
		// PATHEXT entry: the case-insensitive directory scan is what
		// makes this pass on a case-sensitive Linux filesystem.
		Path tool = dir.resolve("iverilog.exe");
		writeExecutable(tool);

		Optional<Path> found = ToolLocator.find("iverilog",
				dir.toString(), ".COM;.EXE;.BAT;.CMD");
		assertTrue(found.isPresent(),
				".exe should resolve via PATHEXT");
		assertEquals("iverilog.exe",
				found.get().getFileName().toString());
	}

	@Test
	void windowsDefaultPathextIsUsedWhenUnset(@TempDir Path dir)
			throws Exception {
		// PATHEXT null => the .COM;.EXE;.BAT;.CMD default arms .EXE.
		Path tool = dir.resolve("yosys.exe");
		writeExecutable(tool);

		Optional<Path> found = ToolLocator.find("yosys",
				dir.toString(), null);
		assertTrue(found.isPresent(),
				"default PATHEXT should include .EXE");
		assertEquals("yosys.exe",
				found.get().getFileName().toString());
	}

	@Test
	void theBareNameWinsOverAnExtensionVariant(@TempDir Path dir)
			throws Exception {
		Path bare = dir.resolve("ghdl");
		writeExecutable(bare);
		Path exe = dir.resolve("ghdl.exe");
		writeExecutable(exe);

		Optional<Path> found = ToolLocator.find("ghdl",
				dir.toString(), ".EXE");
		assertTrue(found.isPresent());
		assertEquals("ghdl", found.get().getFileName().toString(),
				"bare name has priority over PATHEXT variants");
	}

	@Test
	void earlierPathEntryWins(@TempDir Path first, @TempDir Path second)
			throws Exception {
		Path winner = first.resolve("iverilog.exe");
		writeExecutable(winner);
		Path decoy = second.resolve("iverilog.exe");
		writeExecutable(decoy);

		String path = first + File.pathSeparator + second;
		Optional<Path> found = ToolLocator.find("iverilog", path, null);
		assertTrue(found.isPresent());
		assertEquals(winner.toAbsolutePath(),
				found.get().toAbsolutePath());
	}

	@Test
	void notFoundReturnsEmpty(@TempDir Path empty) {
		assertTrue(ToolLocator.find("iverilog", empty.toString(), null)
				.isEmpty(), "empty directory finds nothing");
		assertTrue(ToolLocator.find("iverilog", null, null).isEmpty(),
				"null PATH finds nothing");
		assertTrue(ToolLocator.find(null, empty.toString(), null)
				.isEmpty(), "null tool finds nothing");
		assertTrue(ToolLocator.find("iverilog", "", null).isEmpty(),
				"empty PATH finds nothing");
	}

	@Test
	void aDirectoryNamedLikeTheToolIsNotABinary(@TempDir Path dir)
			throws Exception {
		Files.createDirectory(dir.resolve("iverilog.exe"));

		assertTrue(ToolLocator.find("iverilog", dir.toString(), null)
				.isEmpty(), "a directory is never a match");
	}

	@Test
	void aNonExecutableFileIsSkipped(@TempDir Path dir) throws Exception {
		Path plain = dir.resolve("iverilog.exe");
		Files.writeString(plain, "not a program");
		plain.toFile().setExecutable(false, false);
		// under some accounts (root) everything is executable; the
		// premise of this test then cannot be established
		assumeTrue(!Files.isExecutable(plain),
				"cannot make a non-executable file on this account");

		assertTrue(ToolLocator.find("iverilog", dir.toString(), null)
				.isEmpty(), "non-executable file is not a match");
	}

	@Test
	void junkPathEntriesAreSkipped(@TempDir Path dir) throws Exception {
		Path tool = dir.resolve("iverilog.exe");
		writeExecutable(tool);

		String path = "" + File.pathSeparator
				+ "/nonexistent-dir" + File.pathSeparator
				+ dir;
		Optional<Path> found = ToolLocator.find("iverilog", path, null);
		assertTrue(found.isPresent(),
				"empty and missing entries are skipped, real one wins");
	}

	/**
	 * Writes a fixture and marks it executable, skipping the test if
	 * the platform refuses to set the bit.
	 *
	 * @param file The fixture to create.
	 *
	 * @throws Exception on write failure.
	 */
	private static void writeExecutable(Path file) throws Exception {
		Files.writeString(file, "#!/bin/sh\n");
		assumeTrue(file.toFile().setExecutable(true, false)
				|| Files.isExecutable(file),
				"cannot mark fixtures executable on this platform");
	}

} // end of ToolLocatorTest class
