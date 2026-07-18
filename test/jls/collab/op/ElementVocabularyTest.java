package jls.collab.op;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import jls.Circuit;
import jls.elem.Element;

/**
 * The element-type allowlist contract of issue #170. The vocabulary is
 * only as safe as it is correct, in both directions: a missing token
 * breaks legitimate collaboration (a peer's valid snapshot rejected),
 * and an extra token recreates the hole the allowlist closes (a
 * peer-controlled string reaching reflective instantiation). So the
 * list is pinned against both independent sources of truth - the
 * save-format writer literals in {@code jls.elem} (what snapshots can
 * legitimately contain) and the palette contract in
 * {@code HelpTopicsTest} (what users can create) - and the rejection
 * behavior is pinned as typed, total, and strictly tighter than the
 * file loader's reflective gate.
 */
class ElementVocabularyTest {

	/**
	 * Every token in the vocabulary must be a real, concrete
	 * {@code jls.elem} element class with the public (Circuit)
	 * constructor the loader contract requires - a stale token after
	 * a rename would otherwise linger as a confusing "legal" name
	 * that can never instantiate.
	 */
	@Test
	void everyVocabularyTokenIsACreatableElement() throws Exception {

		for (String type : new TreeSet<>(
				ElementVocabulary.allowedTypes())) {
			Class<?> c = Class.forName("jls.elem." + type);
			assertTrue(Element.class.isAssignableFrom(c),
					type + " is in the vocabulary but is not an Element");
			assertFalse(Modifier.isAbstract(c.getModifiers()),
					type + " is in the vocabulary but is abstract");
			c.getConstructor(Circuit.class);
		}
	}

	/**
	 * The vocabulary equals the set of tokens the save-format writers
	 * actually produce: the {@code "ELEMENT <Type>"} literals plus the
	 * gate {@code Kind} save names (gates write
	 * {@code "ELEMENT " + saveName}). Scanned from the sources so a
	 * new element's writer cannot land without its vocabulary row,
	 * and a removed writer cannot leave a dead token behind.
	 */
	@Test
	void vocabularyEqualsTheWriterTokenSet() throws IOException {

		Path elem = Path.of(System.getProperty("user.dir"),
				"src", "jls", "elem");
		assertTrue(Files.isDirectory(elem),
				"element sources not found at " + elem
						+ " (run tests from the repository root)");

		Pattern literal = Pattern.compile("\"ELEMENT ([A-Za-z0-9]+)\"");
		Pattern kind = Pattern.compile(
				"new Kind\\(\\s*\"[^\"]*\",\\s*\"([A-Za-z0-9]+)\"");
		Set<String> written = new TreeSet<>();
		try (Stream<Path> files = Files.list(elem)) {
			files.filter(p -> p.toString().endsWith(".java"))
					.forEach(p -> {
						String text;
						try {
							text = new String(Files.readAllBytes(p),
									StandardCharsets.UTF_8);
						} catch (IOException ex) {
							throw new RuntimeException(ex);
						}
						for (Pattern pattern
								: new Pattern[] { literal, kind }) {
							Matcher m = pattern.matcher(text);
							while (m.find()) {
								written.add(m.group(1));
							}
						}
					});
		}

		assertEquals(written,
				new TreeSet<>(ElementVocabulary.allowedTypes()),
				"the vocabulary must equal the save-format writer set");
	}

	/**
	 * The issue #170 registry cross-check: the vocabulary is the
	 * palette-visible element set (the same list HelpTopicsTest pins
	 * against the help system, read from there so the two cannot
	 * drift) plus WireEnd, the one non-palette save token - wires are
	 * save-file structure, not palette elements. A vocabulary that
	 * admitted anything else would be admitting a class users cannot
	 * create, which is exactly the helper-class hole.
	 */
	@Test
	void vocabularyIsThePaletteSetPlusWireEnd() throws Exception {

		Field palette = Class.forName("jls.HelpTopicsTest")
				.getDeclaredField("PALETTE_ELEMENT_TOPICS");
		palette.setAccessible(true);
		Set<String> expected = new TreeSet<>(
				((Map<?, ?>) palette.get(null)).keySet().stream()
						.map(Object::toString).toList());
		expected.add("WireEnd");

		assertEquals(expected,
				new TreeSet<>(ElementVocabulary.allowedTypes()),
				"the vocabulary must be the palette set plus WireEnd");
	}

	/**
	 * The issue #170 P1 witness, recorded against the
	 * allowlist-disabled configuration (the raw reflective gate) as
	 * the plan directs: TestGen satisfies every check the file
	 * loader's reflection path applies - it resolves in jls.elem, is
	 * a concrete Element subclass, and has the public (Circuit)
	 * constructor - yet it is not a save token, so a peer naming it
	 * would be selecting a class no legitimate payload contains. The
	 * allowlist must be strictly tighter than the reflective gate:
	 * the same token that passes reflection dies here as a typed
	 * rejection.
	 */
	@Test
	void allowlistIsStrictlyTighterThanTheReflectiveGate()
			throws Exception {

		// the pre-change hazard: the loader's own checks admit TestGen
		Class<?> c = Class.forName("jls.elem.TestGen");
		assertTrue(Element.class.isAssignableFrom(c),
				"witness invalidated: TestGen is no longer an Element");
		assertFalse(Modifier.isAbstract(c.getModifiers()),
				"witness invalidated: TestGen went abstract");
		c.getConstructor(Circuit.class);

		// the post-change behavior: the allowlist rejects it, typed
		assertFalse(ElementVocabulary.isAllowed("TestGen"));
		OpRejected rejection = assertThrows(OpRejected.class,
				() -> ElementVocabulary.requireAllowed("TestGen"));
		assertTrue(rejection.getMessage().contains("TestGen"),
				"the rejection should name the rejected token");
	}

	/**
	 * Hostile tokens are total, typed rejections: null, empty,
	 * near-miss, traversal-shaped, and qualified-name tokens all
	 * throw OpRejected, and an oversized token is clipped in the
	 * message so a hostile payload cannot bloat logs through its own
	 * rejection.
	 */
	@Test
	void hostileTokensAreTypedRejections() {

		for (String hostile : new String[] { null, "", " ", "AndGate ",
				"andgate", "Wire", "Gate", "jls.elem.AndGate",
				"../AndGate", "Element", "TellUser" }) {
			assertFalse(ElementVocabulary.isAllowed(hostile),
					"'" + hostile + "' should not be allowed");
			assertThrows(OpRejected.class,
					() -> ElementVocabulary.requireAllowed(hostile));
		}

		String huge = "A".repeat(10_000);
		OpRejected rejection = assertThrows(OpRejected.class,
				() -> ElementVocabulary.requireAllowed(huge));
		assertTrue(rejection.getMessage().length() < 200,
				"rejection messages must clip hostile tokens");
	}
}
