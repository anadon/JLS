package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Pins issue #130's fix at the call sites: no GUI code may seed a file
 * chooser or a directory fallback from {@code System.getProperty
 * ("user.dir")} - the process working directory is meaningless in a
 * desktop-launched session (deb/rpm/msi/dmg/AppImage, issue #82).
 * GUI paths go through {@link Util#defaultDirectory()} /
 * {@link Util#seedDirectory(String)} instead.
 *
 * <p>Exactly one production read of {@code user.dir} is allowed, in
 * {@code JLSStart.open(String)}: when a start file is named on the
 * command line as a bare relative filename, it was resolved against
 * the working directory, so the working directory IS the file's real
 * parent - that site is CWD-load-bearing and excluded by issue #130's
 * H1. If you add a new {@code user.dir} read, either route it through
 * the Util helpers (GUI seeding) or extend ALLOWED with a comment
 * saying why the working directory is genuinely meaningful there.</p>
 *
 * <p>Like HeadlessCoreRatchetTest, sources are read straight from the
 * repo tree relative to user.dir, which maven sets to the module base
 * directory - the repository root.</p>
 */
class UserDirSeedTest {

	/** Files allowed to read user.dir, with the exact occurrence
	 *  count each is allowed. */
	private static final Map<String, Integer> ALLOWED = Map.of(
			// the CLI start-file parent fallback in open(String)
			"src/jls/JLSStart.java", 1);

	@Test
	void guiCodeDoesNotSeedFromUserDir() throws IOException {

		Path srcRoot = Path.of("src");
		assertTrue(Files.isDirectory(srcRoot),
				"expected to run from the repository root");

		Map<String, Integer> found = new TreeMap<>();
		try (Stream<Path> files = Files.walk(srcRoot)) {
			files.filter(p -> p.toString().endsWith(".java"))
					.forEach(p -> {
						String text;
						try {
							text = Files.readString(p, StandardCharsets.UTF_8);
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
						int count = 0;
						int at = -1;
						while ((at = text.indexOf("\"user.dir\"", at + 1)) != -1)
							count += 1;
						if (count > 0)
							found.put(p.toString().replace('\\', '/'), count);
					});
		}

		assertEquals(new TreeMap<>(ALLOWED), found,
				"user.dir reads outside the allowed set - GUI chooser "
						+ "seeds and directory fallbacks must use "
						+ "Util.defaultDirectory()/seedDirectory() "
						+ "(issue #130)");
	}
} // end of UserDirSeedTest class
