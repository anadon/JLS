package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * The forbidden-import ratchet for issue #81: every user notification
 * must flow through the TellUser reporter, which is headless-aware and
 * stream-correct (batch diagnostics to stderr as "jls: error:" lines,
 * dialogs consistently parented in GUI mode). Raw JOptionPane use in
 * application code bypasses all of that, so any mention of JOptionPane
 * outside the allowlist below is a test failure.
 *
 * The sources are read straight from the repo tree (the same way the
 * surefire run finds them to compile), relative to user.dir, which
 * maven sets to the module base directory - the repository root.
 */
class NotificationRatchetTest {

	/**
	 * The only files allowed to mention JOptionPane. TellUser is the
	 * reporter itself - the single place dialogs are created. Nothing
	 * else has earned an exemption; additions here need a comment
	 * explaining why the use is genuinely not a notification.
	 */
	private static final Set<String> ALLOWED = Set.of(
			"src/jls/TellUser.java");

	@Test
	void onlyTellUserMentionsJOptionPane() throws IOException {

		Path src = Path.of(System.getProperty("user.dir"), "src");
		assertTrue(Files.isDirectory(src),
				"source tree not found at " + src
						+ " (run tests from the repository root)");

		Set<String> offenders = new TreeSet<>();
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
						if (text.contains("JOptionPane")) {
							offenders.add(relativize(p));
						}
					});
		}

		assertEquals(ALLOWED, offenders,
				"raw JOptionPane use found outside the reporter; route "
						+ "user notifications through jls.TellUser instead");
	}

	/** A path relative to the repo root, with forward slashes. */
	private static String relativize(Path p) {

		Path root = Path.of(System.getProperty("user.dir"));
		return root.relativize(p.toAbsolutePath().normalize())
				.toString().replace('\\', '/');
	}
}
