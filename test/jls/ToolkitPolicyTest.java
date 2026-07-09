package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the startup toolkit policy's pure decision function
 * (issue #105): the whole environment matrix — headless batch, X11,
 * Wayland-only with and without WLToolkit, mixed WAYLAND_DISPLAY+DISPLAY,
 * forced overrides, non-Linux — is decided from injected facts, so this
 * is the compensating control for the platforms CI cannot exercise
 * (#105 section 10).
 */
class ToolkitPolicyTest {

	private static final BooleanSupplier HAS_WLTOOLKIT = () -> true;
	private static final BooleanSupplier NO_WLTOOLKIT = () -> false;

	/** A probe that must not be consulted: the probe loads a JDK class,
	 * so branches whose answer cannot depend on it must not pay for it. */
	private static final BooleanSupplier MUST_NOT_PROBE = () -> {
		fail("the WLToolkit probe must not be consulted on this branch");
		return false;
	};

	private static ToolkitPolicy.Decision decide(String os, String wayland,
			String display, String override, boolean headless,
			BooleanSupplier probe) {
		return ToolkitPolicy.decide(os, wayland, display, override,
				headless, probe);
	}

	/** Assert the error line honors the CLI contract (#42): exactly one
	 * line, "jls: error:" prefix, no stack trace. */
	private static void assertErrorLine(ToolkitPolicy.Decision d) {
		assertEquals(ToolkitPolicy.Action.FAIL_WITH_MESSAGE, d.action);
		assertTrue(d.message.startsWith("jls: error: "), d.message);
		assertFalse(d.message.contains("\n"),
				"error must be a single line: " + d.message);
	}

	// ---- Wayland-only sessions (the case this policy exists for) ----

	@Test
	void waylandOnlyWithWlToolkitSelectsIt() {
		ToolkitPolicy.Decision d = decide("Linux", "wayland-1", null,
				null, false, HAS_WLTOOLKIT);
		assertEquals(ToolkitPolicy.Action.SET_WLTOOLKIT, d.action);
		assertNull(d.message);
	}

	@Test
	void waylandOnlyWithoutWlToolkitFailsActionably() {
		ToolkitPolicy.Decision d = decide("Linux", "wayland-1", null,
				null, false, NO_WLTOOLKIT);
		assertErrorLine(d);
		// the two actual ways out, by name: XWayland via DISPLAY, or a
		// Wakefield-capable runtime
		assertTrue(d.message.contains("DISPLAY"), d.message);
		assertTrue(d.message.contains("XWayland"), d.message);
		assertTrue(d.message.contains("JBR")
				|| d.message.contains("Wakefield"), d.message);
	}

	@Test
	void emptyDisplayCountsAsWaylandOnly() {
		ToolkitPolicy.Decision d = decide("Linux", "wayland-1", "",
				null, false, HAS_WLTOOLKIT);
		assertEquals(ToolkitPolicy.Action.SET_WLTOOLKIT, d.action);
	}

	// ---- sessions the policy must leave alone ----

	@Test
	void x11SessionIsUntouched() {
		ToolkitPolicy.Decision d = decide("Linux", null, ":0",
				null, false, MUST_NOT_PROBE);
		assertEquals(ToolkitPolicy.Action.DEFAULT, d.action);
	}

	@Test
	void mixedWaylandPlusX11PrefersX11Default() {
		// XWayland present: deliberately no behavior change until #100
		// declares Wayland parity
		ToolkitPolicy.Decision d = decide("Linux", "wayland-1", ":0",
				null, false, MUST_NOT_PROBE);
		assertEquals(ToolkitPolicy.Action.DEFAULT, d.action);
	}

	@Test
	void trulyHeadlessSessionKeepsTheHonestHeadlessPath() {
		// no WAYLAND_DISPLAY, no DISPLAY: an ssh session, where the old
		// HeadlessException message is the truthful one
		ToolkitPolicy.Decision d = decide("Linux", null, null,
				null, false, MUST_NOT_PROBE);
		assertEquals(ToolkitPolicy.Action.DEFAULT, d.action);
	}

	@Test
	void emptyWaylandDisplayIsNotAWaylandSession() {
		ToolkitPolicy.Decision d = decide("Linux", "", null,
				null, false, MUST_NOT_PROBE);
		assertEquals(ToolkitPolicy.Action.DEFAULT, d.action);
	}

	@Test
	void headlessBatchRunIsUntouchedEvenOnWaylandOnly() {
		// -b/-i/-export/java.awt.headless=true never need a display, so
		// a Wayland-only grading box must keep working with stock
		// OpenJDK (#105 P3)
		ToolkitPolicy.Decision d = decide("Linux", "wayland-1", null,
				null, true, MUST_NOT_PROBE);
		assertEquals(ToolkitPolicy.Action.DEFAULT, d.action);
	}

	@Test
	void windowsIsUntouchedEvenWithWaylandVariables() {
		ToolkitPolicy.Decision d = decide("Windows 11", "wayland-1", null,
				null, false, MUST_NOT_PROBE);
		assertEquals(ToolkitPolicy.Action.DEFAULT, d.action);
	}

	@Test
	void macosIsUntouchedEvenWithWaylandVariables() {
		ToolkitPolicy.Decision d = decide("Mac OS X", "wayland-1", null,
				null, false, MUST_NOT_PROBE);
		assertEquals(ToolkitPolicy.Action.DEFAULT, d.action);
	}

	@Test
	void unknownOsIsUntouched() {
		ToolkitPolicy.Decision d = decide(null, "wayland-1", null,
				null, false, MUST_NOT_PROBE);
		assertEquals(ToolkitPolicy.Action.DEFAULT, d.action);
	}

	// ---- the -Djls.toolkit escape hatch forces the branch (#105 P4) ----

	@Test
	void forcedDefaultBypassesDetectionOnWaylandOnly() {
		ToolkitPolicy.Decision d = decide("Linux", "wayland-1", null,
				"default", false, MUST_NOT_PROBE);
		assertEquals(ToolkitPolicy.Action.DEFAULT, d.action);
	}

	@Test
	void forcedWaylandSelectsWlToolkitEvenOnX11() {
		ToolkitPolicy.Decision d = decide("Linux", null, ":0",
				"wayland", false, HAS_WLTOOLKIT);
		assertEquals(ToolkitPolicy.Action.SET_WLTOOLKIT, d.action);
	}

	@Test
	void forcedWaylandOverridesTheHeadlessCarveOut() {
		ToolkitPolicy.Decision d = decide("Linux", "wayland-1", null,
				"wayland", true, HAS_WLTOOLKIT);
		assertEquals(ToolkitPolicy.Action.SET_WLTOOLKIT, d.action);
	}

	@Test
	void forcedWaylandWithoutWlToolkitFailsAndNamesTheProperty() {
		ToolkitPolicy.Decision d = decide("Linux", null, ":0",
				"wayland", false, NO_WLTOOLKIT);
		assertErrorLine(d);
		assertTrue(d.message.contains("jls.toolkit"), d.message);
		assertTrue(d.message.contains(ToolkitPolicy.WLTOOLKIT_CLASS),
				d.message);
	}

	@Test
	void unrecognizedOverrideValueFailsAndEchoesIt() {
		ToolkitPolicy.Decision d = decide("Linux", null, ":0",
				"cocoa", false, MUST_NOT_PROBE);
		assertErrorLine(d);
		assertTrue(d.message.contains("cocoa"), d.message);
		assertTrue(d.message.contains("default"), d.message);
		assertTrue(d.message.contains("wayland"), d.message);
	}

	// ---- the runtime probe ----

	@Test
	void wlToolkitProbeNeverInitializesAwtAndAnswersFalseOnStockJdk() {
		// Class.forName(..., false, ...) locates without initializing;
		// whatever this JVM is, calling the probe must be safe under
		// java.awt.headless=true (the suite's setting). On stock
		// OpenJDK/Temurin it answers false; on a JBR it would answer
		// true, so only the safety (no throw) is asserted unconditionally.
		boolean has = ToolkitPolicy.runtimeHasWlToolkit();
		// consistency: asking twice gives the same answer
		assertEquals(has, ToolkitPolicy.runtimeHasWlToolkit());
	}
}
