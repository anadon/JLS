package jls.edit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import jls.Circuit;
import jls.elem.SubCircuit;

/**
 * Save As duplicate-name predicate tests (issue #117). saveAs used to
 * scan every sibling editor tab without skipping the editor being
 * saved, so saving a circuit under its own current name collided with
 * itself and was refused with "Circuit with this name already being
 * edited". The predicate is now extracted to
 * {@link Editor#nameUsedByAnotherEditor(String, Circuit, Iterable)}
 * with the same self-exclusion the import-menu loop in saveAs already
 * had; these tests pin the issue's P1 (self is not a collision) and P2
 * (a genuine duplicate still is).
 */
class SaveAsNameCheckTest {

	/**
	 * P1: Save As keeping the circuit's own current name is not a
	 * duplicate, whether it is the only circuit open or one of several.
	 */
	@Test
	void savingUnderOwnCurrentNameIsNotACollision() {
		Circuit foo = new Circuit("foo");
		assertFalse(Editor.nameUsedByAnotherEditor("foo", foo,
				Collections.singletonList(foo)),
				"a circuit must not collide with itself");

		Circuit bar = new Circuit("bar");
		assertFalse(Editor.nameUsedByAnotherEditor("foo", foo,
				Arrays.asList(foo, bar)),
				"siblings with different names must not block a "
						+ "save under the current name");
	}

	/**
	 * P2: with foo and bar open, Save As on bar choosing the name foo
	 * is still refused.
	 */
	@Test
	void savingUnderAnotherEditorsNameIsACollision() {
		Circuit foo = new Circuit("foo");
		Circuit bar = new Circuit("bar");
		List<Circuit> edited = Arrays.asList(foo, bar);
		assertTrue(Editor.nameUsedByAnotherEditor("foo", bar, edited),
				"another editor's name must still be refused");
		assertTrue(Editor.nameUsedByAnotherEditor("bar", foo, edited),
				"...in either direction");
	}

	/**
	 * The exclusion is by identity, not by name: a distinct sibling
	 * circuit that happens to share the saved circuit's name is a
	 * genuine duplicate and must still be refused.
	 */
	@Test
	void distinctSiblingWithSameNameIsStillACollision() {
		Circuit self = new Circuit("foo");
		Circuit other = new Circuit("foo");
		assertTrue(Editor.nameUsedByAnotherEditor("foo", self,
				Arrays.asList(self, other)));
	}

	/**
	 * Pre-existing behavior kept by the extraction: imported circuits
	 * (open subcircuit views, which can't be saved) never count as
	 * duplicates.
	 */
	@Test
	void importedCircuitsAreSkipped() {
		Circuit self = new Circuit("bar");
		Circuit imported = new Circuit("foo");
		imported.setImported(new SubCircuit(imported));
		assertFalse(Editor.nameUsedByAnotherEditor("foo", self,
				Arrays.asList(self, imported)));
	}

	/** An unused name is never a collision. */
	@Test
	void unusedNameIsNotACollision() {
		Circuit foo = new Circuit("foo");
		Circuit bar = new Circuit("bar");
		assertFalse(Editor.nameUsedByAnotherEditor("baz", foo,
				Arrays.asList(foo, bar)));
	}
}
