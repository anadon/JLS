package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import jls.module.ExtensionPoint;

/**
 * Pins the {@link ExtensionPoint} contract (issue #223): the record
 * validates its identity on construction — non-blank id, non-null
 * contract token — and carries id and contract through verbatim, so a
 * point is a safe heterogeneous-container key the moment it exists.
 */
class ExtensionPointTest {

	@Test
	void idAndContractSurviveConstructionVerbatim() {
		ExtensionPoint<Runnable> point =
				new ExtensionPoint<Runnable>("app.command",
						Runnable.class);
		assertEquals("app.command", point.id());
		assertEquals(Runnable.class, point.contract());
	}

	@Test
	void blankIdRejected() {
		assertThrows(IllegalArgumentException.class,
				() -> new ExtensionPoint<Runnable>("", Runnable.class));
		assertThrows(IllegalArgumentException.class,
				() -> new ExtensionPoint<Runnable>("  ",
						Runnable.class));
	}

	@Test
	void identityIsIdPlusContract() {
		assertEquals(
				new ExtensionPoint<Runnable>("app.command",
						Runnable.class),
				new ExtensionPoint<Runnable>("app.command",
						Runnable.class));
		assertNotEquals(
				new ExtensionPoint<Runnable>("app.command",
						Runnable.class),
				new ExtensionPoint<Thread>("app.command",
						Thread.class),
				"same id over a different contract is a different "
						+ "point, never a silent match");
	}

} // end of ExtensionPointTest class
