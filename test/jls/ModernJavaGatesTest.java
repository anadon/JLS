package jls;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Modern-Java program closeout gates (#96): the two practices the
 * program left as advisory bullets become executable ratchets. #94
 * retired {@code Cloneable} from the codebase in favor of explicit
 * copy construction (the {@code Element.copy()} contract from #78),
 * and #93's NullAway rollout closed with zero suppressions. These
 * scans hold both counts at zero; a legitimately necessary exception
 * is added to {@link #ALLOWED} with a justification comment in the
 * same PR.
 */
class ModernJavaGatesTest {

	/**
	 * Files (as repo-relative paths) allowed to carry a NullAway
	 * suppression. Empty on purpose - #93 landed with zero
	 * suppressions, so this list is the visible escape hatch: an
	 * entry needs a justification comment beside it explaining why an
	 * honest {@code @Nullable} annotation or explicit check cannot
	 * express the contract.
	 */
	private static final List<String> ALLOWED = List.of();

	@Test
	void noCloneableInSources() throws IOException {
		// The clause pattern catches Cloneable anywhere in an
		// implements/extends type list ("implements Foo, Cloneable",
		// an interface extending it), not just first position; the
		// character class - names, dots, generics, commas, whitespace
		// - keeps the match inside a type list, so ordinary prose in
		// comments does not trip the gate.
		List<String> hits = scan(Pattern.compile(
				"(?:implements|extends)\\s[\\w.<>,\\s]*"
						+ "\\bCloneable\\b"));
		assertTrue(hits.isEmpty(),
				"Cloneable is retired (#94): value-shaped types are"
						+ " copied explicitly - a copy constructor or"
						+ " the #78 copy() contract - never clone():"
						+ " " + hits);
	}

	@Test
	void noNullAwaySuppressions() throws IOException {
		List<String> hits = scan(Pattern.compile(
				"SuppressWarnings\\s*\\([^)]*NullAway[^)]*\\)"
						+ "|castToNonNull"));
		hits.removeIf(hit -> ALLOWED.stream()
				.anyMatch(path -> hit.startsWith(path + ":")));
		assertTrue(hits.isEmpty(),
				"NullAway suppression (#93 closed with zero): prefer"
						+ " an honest @Nullable annotation and an"
						+ " explicit check; a truly unavoidable"
						+ " suppression is listed in ALLOWED with a"
						+ " justification: " + hits);
	}

	/**
	 * Scans every {@code .java} file under {@code src/} and
	 * {@code test/} for the pattern, returning {@code path:line}
	 * hits. Skips {@code package-info.java} (prose only) and this
	 * gate itself, whose patterns and messages name the constructs
	 * they hunt.
	 */
	private static List<String> scan(Pattern pattern)
			throws IOException {
		List<String> hits = new ArrayList<String>();
		for (Path root : List.of(Path.of("src"), Path.of("test"))) {
			List<Path> files;
			try (Stream<Path> walk = Files.walk(root)) {
				files = walk
						.filter(f -> f.toString().endsWith(".java"))
						.filter(f -> !f.getFileName().toString()
								.equals("package-info.java"))
						.filter(f -> !f.getFileName().toString()
								.equals("ModernJavaGatesTest.java"))
						.sorted()
						.toList();
			}
			for (Path file : files) {
				String text = Files.readString(file);
				Matcher m = pattern.matcher(text);
				while (m.find()) {
					long line = 1 + text.substring(0, m.start())
							.chars().filter(c -> c == '\n').count();
					hits.add(file + ":" + line);
				}
			}
		}
		return hits;
	}

} // end of ModernJavaGatesTest class
