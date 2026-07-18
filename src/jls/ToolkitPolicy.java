package jls;

import java.util.Locale;
import java.util.function.BooleanSupplier;

/**
 * Startup toolkit selection policy (issue #105).
 *
 * AWT picks its toolkit the first time anything initializes it, from the
 * {@code awt.toolkit.name} system property; once a toolkit exists it can
 * never be swapped. On Linux, stock OpenJDK only ships the X11 toolkit,
 * so on a Wayland-only session (no XWayland) the JVM decides it is
 * headless and the GUI dies with a misleading "headless remote session?"
 * message on a machine with a perfectly good display. JetBrains Runtime
 * additionally ships OpenJDK's experimental Wayland toolkit (Project
 * Wakefield, {@code sun.awt.wl.WLToolkit}).
 *
 * This class decides, before any AWT class is initialized, whether to:
 * <ul>
 * <li>select {@code WLToolkit} (Wayland-only session, runtime has it),</li>
 * <li>leave the platform default alone (X11/XWayland, Windows, macOS,
 *     and every headless one-shot mode: batch, image/HDL export,
 *     printing), or</li>
 * <li>fail with one actionable {@code jls: error:} line and exit 1
 *     (Wayland-only session, runtime has no Wayland toolkit) instead of
 *     the misleading generic headless message.</li>
 * </ul>
 *
 * The decision itself ({@link #decide decide}) is a pure function of the
 * environment facts so the whole matrix is unit-testable; only
 * {@link #apply()} touches system state. The {@code -Djls.toolkit}
 * property is the user-facing escape hatch: {@code default} forces the
 * platform default, {@code wayland} forces {@code WLToolkit}, both
 * bypassing detection entirely.
 *
 * A session with XWayland present sets both {@code WAYLAND_DISPLAY} and
 * {@code DISPLAY}; the policy deliberately treats that as X11 (no
 * behavior change) until #100 declares Wayland parity — flipping that
 * preference is a one-line change here.
 */
final class ToolkitPolicy {

	/** The JBR/Wakefield Wayland toolkit class, probed without initializing it. */
	static final String WLTOOLKIT_CLASS = "sun.awt.wl.WLToolkit";

	/** The property AWT reads (once, at toolkit creation) to pick a toolkit. */
	static final String AWT_TOOLKIT_PROPERTY = "awt.toolkit.name";

	/** The user-facing escape hatch: -Djls.toolkit=default|wayland. */
	static final String OVERRIDE_PROPERTY = "jls.toolkit";

	/** What the policy decided to do. */
	enum Action {
		/** Set awt.toolkit.name=WLToolkit before AWT initializes. */
		SET_WLTOOLKIT,
		/** Leave toolkit selection entirely alone. */
		DEFAULT,
		/** Print the decision's single-line message to stderr and exit 1. */
		FAIL_WITH_MESSAGE
	}

	/** A decision: an action, plus the error line when the action is
	 * FAIL_WITH_MESSAGE (null otherwise). */
	static final class Decision {

		final Action action;
		final String message;

		/**
		 * @param action  what the policy decided to do.
		 * @param message the single-line error, or null unless the action
		 *                is FAIL_WITH_MESSAGE.
		 */
		private Decision(Action action, String message) {
			this.action = action;
			this.message = message;
		}

		/**
		 * A non-failing decision that carries no message.
		 *
		 * @param action the action to take (never FAIL_WITH_MESSAGE).
		 * @return the decision.
		 */
		private static Decision of(Action action) {
			return new Decision(action, null);
		}

		/**
		 * A FAIL_WITH_MESSAGE decision carrying the line to print.
		 *
		 * @param message the single actionable jls: error: line.
		 * @return the decision.
		 */
		private static Decision fail(String message) {
			return new Decision(Action.FAIL_WITH_MESSAGE, message);
		}
	} // end of Decision class

	/** Not instantiable: the policy is static-use only. */
	private ToolkitPolicy() {
		// static use only
	}

	/**
	 * The pure decision function: all environment facts come in as
	 * parameters, nothing global is consulted, nothing is mutated.
	 *
	 * @param osName         value of the os.name system property.
	 * @param waylandDisplay value of the WAYLAND_DISPLAY environment
	 *                       variable (null/empty = not a Wayland session).
	 * @param display        value of the DISPLAY environment variable
	 *                       (null/empty = no X11/XWayland server).
	 * @param override       value of the jls.toolkit system property, or
	 *                       null when the user gave none.
	 * @param headlessRun    true when this invocation never needs a
	 *                       display: java.awt.headless already set, or a
	 *                       headless one-shot mode (-b, -i, -export,
	 *                       printing) was requested. Those flows are
	 *                       contractually untouched (#105 P3).
	 * @param hasWlToolkit   probe for the Wayland toolkit class in the
	 *                       running JDK; a supplier so it is only
	 *                       consulted when the answer matters.
	 *
	 * @return the decision.
	 *
	 * @see jls.ToolkitPolicyTest#decide()
	 */
	static Decision decide(String osName, String waylandDisplay,
			String display, String override, boolean headlessRun,
			BooleanSupplier hasWlToolkit) {

		// the explicit escape hatch forces the branch both ways (#105 P4)
		if (override != null) {
			if (override.equals("default")) {
				return Decision.of(Action.DEFAULT);
			}
			if (override.equals("wayland")) {
				if (hasWlToolkit.getAsBoolean()) {
					return Decision.of(Action.SET_WLTOOLKIT);
				}
				return Decision.fail("jls: error: -Djls.toolkit=wayland was"
						+ " given but this Java runtime has no Wayland"
						+ " toolkit (" + WLTOOLKIT_CLASS + "); use a"
						+ " JBR/Wakefield build (see README, \"Wayland\")");
			}
			return Decision.fail("jls: error: unrecognized -D"
					+ OVERRIDE_PROPERTY + " value '" + override
					+ "' (expected 'default' or 'wayland')");
		}

		// headless one-shot modes never need a display: untouched, so
		// batch grading on a Wayland-only box keeps working exactly as
		// it does today
		if (headlessRun) {
			return Decision.of(Action.DEFAULT);
		}

		// the policy is Linux-only; Win32 and Cocoa stay untouched
		if (osName == null
				|| !osName.toLowerCase(Locale.ROOT).contains("linux")) {
			return Decision.of(Action.DEFAULT);
		}

		// not a Wayland session at all (plain X11, or genuinely headless
		// where the old HeadlessException message is the honest one)
		if (waylandDisplay == null || waylandDisplay.isEmpty()) {
			return Decision.of(Action.DEFAULT);
		}

		// XWayland is available: deliberately keep the X11 default until
		// #100 declares Wayland parity (see class comment)
		if (display != null && !display.isEmpty()) {
			return Decision.of(Action.DEFAULT);
		}

		// Wayland-only session
		if (hasWlToolkit.getAsBoolean()) {
			return Decision.of(Action.SET_WLTOOLKIT);
		}
		return Decision.fail("jls: error: this is a Wayland-only session"
				+ " and this Java runtime has no Wayland toolkit; run"
				+ " under XWayland by setting DISPLAY, or run JLS on a"
				+ " JBR/Wakefield build that ships WLToolkit (see README,"
				+ " \"Wayland\")");
	} // end of decide method

	/**
	 * Whether the running JDK ships the Wayland toolkit. The probe uses
	 * Class.forName with initialize=false, so it can never start AWT (or
	 * anything else): the class is located and loaded but no static
	 * initializer runs.
	 *
	 * @return true if the WLToolkit class exists in this runtime.
	 *
	 * @see jls.ToolkitPolicyTest#wlToolkitProbeNeverInitializesAwtAndAnswersFalseOnStockJdk()
	 * @see jls.WaylandStartupCliTest#assumeStockJdk()
	 */
	static boolean runtimeHasWlToolkit() {

		try {
			Class.forName(WLTOOLKIT_CLASS, false,
					ToolkitPolicy.class.getClassLoader());
			return true;
		} catch (ClassNotFoundException | LinkageError e) {
			return false;
		}
	} // end of runtimeHasWlToolkit method

	/**
	 * Gather the real environment facts, decide, and act. Must run after
	 * command-line parsing (so headless one-shot modes are known) but
	 * before anything initializes AWT — awt.toolkit.name is read exactly
	 * once, at toolkit creation.
	 */
	static void apply() {

		boolean headlessRun = Boolean.getBoolean("java.awt.headless")
				|| !JLSStart.guiSessionRequested();
		Decision d = decide(System.getProperty("os.name"),
				System.getenv("WAYLAND_DISPLAY"),
				System.getenv("DISPLAY"),
				System.getProperty(OVERRIDE_PROPERTY),
				headlessRun,
				ToolkitPolicy::runtimeHasWlToolkit);
		switch (d.action) {
		case SET_WLTOOLKIT:
			System.setProperty(AWT_TOOLKIT_PROPERTY, "WLToolkit");
			break;
		case FAIL_WITH_MESSAGE:
			// CLI contract (#42): one line on stderr, exit 1 for a
			// runtime failure, no stack trace, no dialog
			System.err.println(d.message);
			System.exit(1);
			break;
		case DEFAULT:
		default:
			break;
		}
	} // end of apply method

} // end of ToolkitPolicy class
