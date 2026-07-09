package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * The forbidden-API ratchet for issues #103/#104 (umbrella #100):
 * global pointer position and absolute screen geometry are concepts
 * Wayland does not expose to clients, and the AWT APIs that read them
 * are defective by contract everywhere (MouseInfo.getPointerInfo() is
 * specified to return null when no pointer is present). Placement is
 * expressed event-locally (jls.util.Placement) or owner-relatively
 * (setLocationRelativeTo), so application code has no business
 * mentioning:
 * <ul>
 * <li>{@code MouseInfo} - replaced by event-local coordinates the
 * editor already tracks (#103),</li>
 * <li>{@code getLocationOnScreen} - replaced by owner-relative window
 * placement (#104),</li>
 * <li>{@code getScreenSize} - replaced by the owner's
 * GraphicsConfiguration bounds (#104).</li>
 * </ul>
 *
 * Like the other ratchets, sources are read straight from the repo
 * tree relative to user.dir, which maven sets to the module base
 * directory - the repository root.
 */
class PointerApiRatchetTest {

	@Test
	void noMouseInfoAnywhere() throws IOException {

		assertEquals(Set.of(), offenders(text -> text.contains("MouseInfo")),
				"global pointer read found; derive coordinates from the "
						+ "triggering event (see jls.util.Placement) instead");
	}

	@Test
	void noGetLocationOnScreenAnywhere() throws IOException {

		assertEquals(Set.of(),
				offenders(text -> text.contains("getLocationOnScreen")),
				"absolute window coordinates read found; position windows "
						+ "with setLocationRelativeTo(owner) instead");
	}

	@Test
	void getScreenSizeNeverUsed() throws IOException {

		Set<String> found = offenders(text -> text.contains("getScreenSize"));
		assertEquals(Set.of(), found,
				"whole-screen sizing is banned; size from the owner's "
						+ "GraphicsConfiguration bounds instead");
	}

	/** Every file under src/ whose text matches the predicate. */
	private static Set<String> offenders(Predicate<String> banned)
			throws IOException {

		Path src = Path.of(System.getProperty("user.dir"), "src");
		assertTrue(Files.isDirectory(src),
				"source tree not found at " + src
						+ " (run tests from the repository root)");

		Set<String> found = new TreeSet<>();
		try (Stream<Path> files = Files.walk(src)) {
			files.filter(p -> p.toString().endsWith(".java"))
					.forEach(p -> {
						String text;
						try {
							text = new String(Files.readAllBytes(p),
									StandardCharsets.UTF_8);
						} catch (IOException ex) {
							throw new RuntimeException(ex);
						}
						if (banned.test(text)) {
							found.add(relativize(p));
						}
					});
		}
		return found;
	}

	/** A path relative to the repo root, with forward slashes. */
	private static String relativize(Path p) {

		Path root = Path.of(System.getProperty("user.dir"));
		return root.relativize(p.toAbsolutePath().normalize())
				.toString().replace('\\', '/');
	}
}
