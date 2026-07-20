package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import jls.elem.Element;
import jls.elem.ElementRegistry;
import jls.elem.ElementType;

/**
 * The registry-integrity test for issue #78: the {@link ElementRegistry}
 * must be a total, unambiguous table of the loadable element types, so
 * that forgetting to register a new element (or registering it wrongly)
 * fails the build instead of surfacing as a "no element type named ..."
 * load error in front of a user.
 *
 * Totality is checked against the source tree (like the other ratchets,
 * read relative to user.dir, which maven sets to the repository root):
 * every concrete {@code jls.elem} class that extends {@link Element} and
 * has a public {@code (Circuit)} constructor is loadable and must be
 * registered - that pair of properties is exactly what the pre-registry
 * {@code Class.forName} loader required. The reverse direction (every
 * registered factory produces its declared class, every tag resolves,
 * no tag collisions) is checked directly on the registry. Save fidelity
 * through the registry-routed loader is the round-trip suite's job
 * ({@link AllElementsRoundTripTest}).
 */
class ElementRegistryTest {

	@Test
	void everyLoadableElementClassIsRegistered() throws Exception {

		Set<String> loadable = loadableClassNames();
		Set<String> registered = new TreeSet<>();
		for (ElementType type : ElementRegistry.all()) {
			registered.add(type.elementClass().getSimpleName());
		}
		assertEquals(loadable, registered,
				"the registry must list exactly the concrete Element "
						+ "classes with a public (Circuit) constructor; "
						+ "add the missing ElementType line to "
						+ "ElementRegistry (or remove the stale one)");
	}

	@Test
	void everyFactoryProducesItsDeclaredClassAndEveryTagResolves() {

		Circuit circuit = new Circuit("registrycheck");
		for (ElementType type : ElementRegistry.all()) {
			Element made = type.create(circuit);
			assertNotNull(made, "factory for tag '" + type.tag()
					+ "' returned null");
			assertSame(type.elementClass(), made.getClass(),
					"factory for tag '" + type.tag() + "' must produce "
							+ "exactly " + type.elementClass().getName());
			assertSame(type, ElementRegistry.forTag(type.tag()),
					"tag '" + type.tag() + "' must resolve to its own "
							+ "descriptor");
			for (String alias : type.aliases()) {
				assertSame(type, ElementRegistry.forTag(alias),
						"alias '" + alias + "' must resolve to the '"
								+ type.tag() + "' descriptor");
			}
		}
	}

	@Test
	void tagsAndAliasesNeverCollide() {

		Set<String> seen = new HashSet<>();
		for (ElementType type : ElementRegistry.all()) {
			assertTrue(seen.add(type.tag()),
					"tag '" + type.tag() + "' is registered twice");
			for (String alias : type.aliases()) {
				assertTrue(seen.add(alias), "alias '" + alias
						+ "' collides with another tag or alias");
			}
		}
	}

	@Test
	void unknownTagIsACleanLoadError() {

		Circuit circuit = new Circuit("unknowntag");
		assertTrue(!circuit.load(new Scanner(
				"CIRCUIT unknowntag\nELEMENT Wire\nEND\nENDCIRCUIT\n")),
				"'Wire' has no (Circuit) constructor and must not be a "
						+ "loadable tag");
		assertEquals(LoadError.Category.UNKNOWN_ELEMENT,
				JLSInfo.lastLoadError.category(),
				"an unregistered tag must report UNKNOWN_ELEMENT");
	}

	@Test
	void everyRegisteredTagLoadsThroughTheRegistry() {

		for (ElementType type : ElementRegistry.all()) {
			Circuit circuit = new Circuit("tagload");
			// load() alone (no finishLoad) creates the element via the
			// registry and reads its (empty) attribute list; that is
			// the resolution path this test pins
			assertTrue(circuit.load(new Scanner("CIRCUIT tagload\nELEMENT "
					+ type.tag() + "\n int id 0\n int x 60\n int y 60\n"
					+ "END\nENDCIRCUIT\n")),
					() -> "tag '" + type.tag() + "' failed to load: "
							+ JLSInfo.loadError);
		}
	}

	/**
	 * The simple names of every concrete class in src/jls/elem that
	 * extends Element and has a public (Circuit) constructor - the
	 * classes the pre-registry reflective loader could instantiate.
	 *
	 * @return the loadable class names, sorted.
	 *
	 * @throws IOException if the source tree cannot be read.
	 * @throws ClassNotFoundException if a source file's class is missing
	 *             from the test classpath.
	 */
	private static Set<String> loadableClassNames()
			throws IOException, ClassNotFoundException {

		Path elem = Path.of(System.getProperty("user.dir"), "src", "jls",
				"elem");
		assertTrue(Files.isDirectory(elem),
				"source tree not found at " + elem
						+ " (run tests from the repository root)");
		Set<String> names = new TreeSet<>();
		try (Stream<Path> files = Files.list(elem)) {
			for (Path p : (Iterable<Path>) files::iterator) {
				String file = p.getFileName().toString();
				if (!file.endsWith(".java")
						|| file.equals("package-info.java")) {
					continue;
				}
				String name = file.substring(0,
						file.length() - ".java".length());
				Class<?> cl = Class.forName("jls.elem." + name);
				if (!Element.class.isAssignableFrom(cl)
						|| Modifier.isAbstract(cl.getModifiers())) {
					continue;
				}
				// the Element base class is "concrete" only because its
				// obligations are runtime-throw stubs (issue #78 H2 will
				// make them abstract); it is not a loadable type
				if (cl == Element.class) {
					continue;
				}
				try {
					cl.getConstructor(Circuit.class);
				} catch (NoSuchMethodException ex) {
					continue;
				}
				names.add(name);
			}
		}
		return names;
	} // end of loadableClassNames method

} // end of ElementRegistryTest class
