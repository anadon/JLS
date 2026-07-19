package jls;

/**
 * Main JLS class.
 * Sets up the exception handler, then has JLSStart do the rest of the work.
 *
 * The XML plugin loader that used to live here was removed (issue #80):
 * it activated only when a literal {@code JLS.jar} was on the classpath,
 * a name no artifact of this project has ever shipped under, so the code
 * path was unreachable in every build. A future extension mechanism, if
 * one is wanted, should be a ServiceLoader-based registry (see the design
 * sketch recorded in issue #80).
 *
 * @author David A. Poplawski, Nick Lanam (test)
 */
public final class JLS  {

	/**
	 * Create a JLS instance. The class is stateless: all work happens
	 * in the static {@link #main} entry point.
	 */
	public JLS() {
	} // end of constructor

	/**
	 * Set up exception handler, then start up JLS.
	 *
	 * @param args Command line arguments.
	 */
	public static void main(String[] args) {

		// set handler for unexpected exceptions
		DefaultExceptionHandler exHandler = new DefaultExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler(exHandler);

		// JLS is GPLv3 (see LICENSE and pop_GPLv3.pdf); the superseded
		// MTU EULA acceptance gate is gone (issue #40)
		JLSStart.parseCommandLine(args);

		// startup toolkit policy (issue #105): on a Wayland-only session
		// select the Wayland toolkit — or fail with one honest line —
		// after parsing (so headless one-shot modes are known and
		// untouched) but before anything can initialize AWT, because
		// awt.toolkit.name is read exactly once, at toolkit creation
		ToolkitPolicy.apply();

		JLSStart.start(exHandler);
	} // end of main method

} // end of JLS class
