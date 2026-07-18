package jls.elem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.junit.jupiter.api.Test;

import jls.Circuit;
import jls.JLSInfo;
import jls.LoadError;

/**
 * The frozen save-tag table (issue #79): {@link SaveTags} owns the
 * save format's element type namespace, decoupled from Java class
 * names. These tests pin the table's integrity - every tag resolves
 * to a distinct concrete loadable class, the alias map stays hygienic,
 * and the loader really routes through the table (a class name that
 * is not a tag no longer reaches the class).
 */
class SaveTagsTest {

	@Test
	void everyTagResolvesToADistinctConcreteLoadableClass() {
		Set<String> all = new HashSet<String>(SaveTags.writableTags());
		all.addAll(SaveTags.loadableOnlyTags());
		assertEquals(SaveTags.writableTags().size()
						+ SaveTags.loadableOnlyTags().size(), all.size(),
				"writable and loadable-only tag sets must not overlap");

		List<String> broken = new ArrayList<String>();
		Map<Class<? extends Element>, String> seen =
				new HashMap<Class<? extends Element>, String>();
		for (String tag : all) {
			Class<? extends Element> c = SaveTags.resolve(tag);
			if (c == null) {
				broken.add(tag + " (does not resolve)");
				continue;
			}
			if (Modifier.isAbstract(c.getModifiers())) {
				broken.add(tag + " (abstract)");
			}
			try {
				c.getConstructor(Circuit.class);
			} catch (NoSuchMethodException ex) {
				broken.add(tag + " (no public (Circuit) constructor)");
			}
			String other = seen.put(c, tag);
			if (other != null) {
				broken.add(tag + " and " + other
						+ " (map to the same class " + c.getName() + ")");
			}
		}
		assertTrue(broken.isEmpty(), "tag table integrity: " + broken);
	}

	@Test
	void nonTagsDoNotResolve() {
		// classes that exist in jls.elem but are not save tags were
		// reachable under the old reflective routing; the table must
		// not resolve them, nor near-miss spellings
		for (String notATag : new String[] {"Wire", "Attribute",
				"Element", "SMUtil", "SaveTags", "andgate", "ANDGATE",
				"FluxCapacitor", "jls.elem.AndGate", ""}) {
			assertNull(SaveTags.resolve(notATag),
					"'" + notATag + "' must not resolve to a class");
		}
	}

	@Test
	void aliasMapIsHygienic() {
		for (Map.Entry<String, String> alias
				: SaveTags.aliases().entrySet()) {
			assertFalse(SaveTags.writableTags().contains(alias.getKey())
							|| SaveTags.loadableOnlyTags()
									.contains(alias.getKey()),
					"alias '" + alias.getKey()
							+ "' collides with a canonical tag");
			assertTrue(SaveTags.writableTags().contains(alias.getValue())
							|| SaveTags.loadableOnlyTags()
									.contains(alias.getValue()),
					"alias '" + alias.getKey()
							+ "' points at unknown canonical tag '"
							+ alias.getValue() + "'");
			assertEquals(SaveTags.resolve(alias.getValue()),
					SaveTags.resolve(alias.getKey()),
					"alias '" + alias.getKey()
							+ "' must resolve like its canonical tag");
		}
	}

	@Test
	void loaderRoutesThroughTheTableNotReflection() {
		// Wire is a real concrete class in jls.elem (rebuilt from
		// WireEnd refs, never saved as an ELEMENT). Under the old
		// Class.forName routing the tag 'Wire' reached the class and
		// failed on its missing (Circuit) constructor; under the table
		// it is simply not a tag, and the diagnostic says so
		Circuit circuit = new Circuit("fixture");
		assertFalse(circuit.load(new Scanner(
						"CIRCUIT fixture\nELEMENT Wire\nEND\nENDCIRCUIT\n")),
				"a non-tag element type must be rejected");
		assertTrue(JLSInfo.lastLoadError != null
						&& JLSInfo.lastLoadError.getCategory()
								== LoadError.Category.UNKNOWN_ELEMENT,
				"category must be UNKNOWN_ELEMENT, got: "
						+ JLSInfo.loadError);
		assertTrue(JLSInfo.loadError.contains("Wire"),
				"the unrecognized tag must be named, got: "
						+ JLSInfo.loadError);
	}

} // end of SaveTagsTest class
