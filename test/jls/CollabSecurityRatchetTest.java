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
 * The prohibition ratchets of issue #170: the collaboration threat
 * model requires that network input can never mean class selection,
 * code, or I/O - only validated circuit operations or typed rejection.
 * Three source-text invariants make that structural, pinned here in
 * the NotificationRatchetTest idiom (scan the source tree, compare the
 * offender set to an explicit allowlist) so a violation names its file
 * in the failure message. ArchitectureRulesTest carries the bytecode
 * half of the first two, which a text scan cannot see through fully
 * qualified references; this scan in turn polices comments and strings
 * that would normalize new uses.
 *
 * The invariants: (1) Java object serialization is banned everywhere,
 * forever - the collab wire format is the textual save-format grammar,
 * and ObjectInputStream deserialization of hostile bytes is the
 * classic remote-execution surface; (2) socket construction is
 * confined to the (future) jls.collab.net package, so the network
 * cannot leak into the model, editor, or simulator, and batch/GUI
 * starts cannot grow a listener by accident; (3) reflection stays out
 * of jls.collab entirely, and Class.forName in the application stays
 * pinned to its three pre-collab sites - element instantiation for a
 * network payload must route through the ElementVocabulary allowlist
 * before any of those sites see the token.
 */
class CollabSecurityRatchetTest {

	/**
	 * Nothing may mention Java object serialization streams. Clean at
	 * the #170 baseline (commit 2c0ee59 audit); keep it clean forever,
	 * especially under jls.collab.
	 */
	@Test
	void javaObjectSerializationIsBannedEverywhere()
			throws IOException {

		assertEquals(Set.of(),
				offenders(text -> text.contains("ObjectInputStream")
						|| text.contains("ObjectOutputStream"),
						Set.of()),
				"Java object serialization found; the collaboration "
						+ "wire format is the textual save-format "
						+ "grammar (issue #170) - never object streams");
	}

	/**
	 * Socket construction (and any other socket API mention) belongs
	 * only under src/jls/collab/net/, the one package the #163
	 * architecture reserves for transport. Zero-tolerance from before
	 * the package exists: no sockets anywhere means batch mode and
	 * the default GUI start bind no listener (issue #170 P4).
	 */
	@Test
	void socketsAppearOnlyUnderCollabNet() throws IOException {

		Set<String> offenders = offenders(
				text -> text.contains("Socket"), Set.of());
		offenders.removeIf(
				path -> path.startsWith("src/jls/collab/net/"));
		assertEquals(Set.of(), offenders,
				"socket use found outside jls.collab.net; transport "
						+ "code lives only there (issues #163/#170)");
	}

	/**
	 * The collaboration layer is data-only: no reflection of any kind
	 * under src/jls/collab/. A network payload that needs an element
	 * class passes its type token through the ElementVocabulary
	 * allowlist and hands off to the loader - collab code itself
	 * never selects or invokes members reflectively.
	 */
	@Test
	void collabDoesNoReflection() throws IOException {

		Set<String> offenders = offenders(
				text -> text.contains("Class.forName")
						|| text.contains("java.lang.reflect")
						|| text.contains("getConstructor")
						|| text.contains(".newInstance("),
				Set.of());
		offenders.removeIf(
				path -> !path.startsWith("src/jls/collab/"));
		assertEquals(Set.of(), offenders,
				"reflection found under jls.collab; the vocabulary is "
						+ "data-only (issue #170)");
	}

	/**
	 * Class.forName stays pinned to its three pre-collab sites: the
	 * toolkit probe, the element loader, and the CLI's element-name
	 * check. Shrink this list if one disappears; never grow it - a
	 * new site is a new place a peer-controlled string could select
	 * a class, and needs the #170 allowlist analysis first.
	 */
	@Test
	void classForNameStaysAtItsPinnedSites() throws IOException {

		assertEquals(
				Set.of("src/jls/Circuit.java", "src/jls/JLSStart.java",
						"src/jls/ToolkitPolicy.java"),
				offenders(text -> text.contains("Class.forName"),
						Set.of()),
				"Class.forName appeared at a new site; route network "
						+ "payloads through ElementVocabulary and keep "
						+ "class selection at the pinned sites "
						+ "(issue #170)");
	}

	/**
	 * Scan the source tree for files matching a predicate.
	 *
	 * @param bad Whether a file's text violates the invariant.
	 * @param allowed Repo-relative paths exempt from the invariant.
	 *
	 * @return the sorted repo-relative paths of the violating files.
	 *
	 * @throws IOException if the tree cannot be read.
	 */
	private static Set<String> offenders(
			java.util.function.Predicate<String> bad,
			Set<String> allowed) throws IOException {

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
						String path = relativize(p);
						if (bad.test(text) && !allowed.contains(path)) {
							found.add(path);
						}
					});
		}
		return found;
	}

	/**
	 * A path relative to the repo root, with forward slashes.
	 *
	 * @param p The path to relativize.
	 *
	 * @return the repo-relative form.
	 */
	private static String relativize(Path p) {

		Path root = Path.of(System.getProperty("user.dir"));
		return root.relativize(p.toAbsolutePath().normalize())
				.toString().replace('\\', '/');
	}
}
