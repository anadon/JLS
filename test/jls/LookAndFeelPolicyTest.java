package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import javax.swing.LookAndFeel;
import javax.swing.UIManager;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Headless tests for the look-and-feel selection policy (issue #153).
 * JLS installs FlatLaf light by default (superseding the earlier
 * cross-platform Metal default), while the {@code -Djls.laf} JVM
 * property selects alternatives - {@code metal} as the escape hatch,
 * {@code system} for the native look, or any LookAndFeel class on the
 * classpath - with a guaranteed fallback: a broken explicit selection
 * warns and falls back to the cross-platform default rather than making
 * JLS unlaunchable. Installing a look-and-feel needs no display (FlatLaf
 * included), so these run in the default headless surefire execution.
 */
class LookAndFeelPolicyTest {

	/** The jls.laf property value before the test, or null if unset. */
	private String savedLaf;

	/** The look-and-feel installed before the test. */
	private LookAndFeel savedLookAndFeel;

	/**
	 * Save the jls.laf property and the installed look-and-feel so each
	 * test leaves the JVM exactly as it found it.
	 *
	 * @throws Exception if the saved look-and-feel state can't be read.
	 */
	@BeforeEach
	void saveState() throws Exception {

		savedLaf = System.getProperty("jls.laf");
		savedLookAndFeel = UIManager.getLookAndFeel();
	} // end of saveState method

	/**
	 * Restore the jls.laf property and the installed look-and-feel.
	 *
	 * @throws Exception if the saved look-and-feel can't be reinstalled.
	 */
	@AfterEach
	void restoreState() throws Exception {

		if (savedLaf == null) {
			System.clearProperty("jls.laf");
		}
		else {
			System.setProperty("jls.laf", savedLaf);
		}
		UIManager.setLookAndFeel(savedLookAndFeel);
	} // end of restoreState method

	/**
	 * With the property unset, the selection is FlatLaf light - the
	 * adopted default (issue #153).
	 */
	@Test
	void defaultSelectionIsFlatLafLight() {

		System.clearProperty("jls.laf");
		assertEquals("com.formdev.flatlaf.FlatLightLaf",
				JLSStart.lookAndFeelClassName());
	} // end of defaultSelectionIsFlatLafLight method

	/**
	 * A blank value is treated the same as unset: it selects the FlatLaf
	 * light default.
	 */
	@Test
	void blankSelectsTheFlatLafDefault() {

		System.setProperty("jls.laf", "  ");
		assertEquals("com.formdev.flatlaf.FlatLightLaf",
				JLSStart.lookAndFeelClassName());
	} // end of blankSelectsTheFlatLafDefault method

	/**
	 * "metal" names the cross-platform look-and-feel explicitly - the
	 * documented escape hatch back to the former default.
	 */
	@Test
	void metalSelectsTheCrossPlatformLookAndFeel() {

		System.setProperty("jls.laf", "metal");
		assertEquals(UIManager.getCrossPlatformLookAndFeelClassName(),
				JLSStart.lookAndFeelClassName());
	} // end of metalSelectsTheCrossPlatformLookAndFeel method

	/**
	 * "system" selects the platform's native look-and-feel.
	 */
	@Test
	void systemSelectsTheSystemLookAndFeel() {

		System.setProperty("jls.laf", "system");
		assertEquals(UIManager.getSystemLookAndFeelClassName(),
				JLSStart.lookAndFeelClassName());
	} // end of systemSelectsTheSystemLookAndFeel method

	/**
	 * Any other value is used verbatim as a LookAndFeel class name, so a
	 * third-party look (e.g. com.formdev.flatlaf.FlatLightLaf) can be
	 * evaluated by dropping its jar on the classpath - no rebuild.
	 */
	@Test
	void anyOtherValueIsUsedAsALookAndFeelClassName() {

		System.setProperty("jls.laf", "com.example.SomeLookAndFeel");
		assertEquals("com.example.SomeLookAndFeel",
				JLSStart.lookAndFeelClassName());
	} // end of anyOtherValueIsUsedAsALookAndFeelClassName method

	/**
	 * The default selection installs, and installs FlatLaf light. FlatLaf
	 * is on the classpath and sets cleanly with no display, so this needs
	 * no real display and runs in the headless execution.
	 */
	@Test
	void installingTheDefaultInstallsFlatLafLight() {

		System.clearProperty("jls.laf");
		assertTrue(JLSStart.installLookAndFeel());
		assertEquals("com.formdev.flatlaf.FlatLightLaf",
				UIManager.getLookAndFeel().getClass().getName());
	} // end of installingTheDefaultInstallsFlatLafLight method

	/**
	 * A broken explicit selection is not fatal: install still succeeds,
	 * the cross-platform default ends up installed, and exactly one
	 * "jls: warning:" line naming the broken class goes to stderr.
	 */
	@Test
	void brokenSelectionWarnsAndFallsBackToTheDefault() {

		System.setProperty("jls.laf", "com.example.NoSuchLookAndFeel");
		PrintStream realErr = System.err;
		ByteArrayOutputStream captured = new ByteArrayOutputStream();
		boolean installed;
		try {
			System.setErr(new PrintStream(captured, true,
					StandardCharsets.UTF_8));
			installed = JLSStart.installLookAndFeel();
		}
		finally {
			System.setErr(realErr);
		}
		assertTrue(installed);
		assertEquals(UIManager.getCrossPlatformLookAndFeelClassName(),
				UIManager.getLookAndFeel().getClass().getName());
		String warning = captured.toString(StandardCharsets.UTF_8);
		assertTrue(warning.startsWith("jls: warning: "), warning);
		assertTrue(warning.contains("com.example.NoSuchLookAndFeel"),
				warning);
		assertEquals(1, warning.lines().count(), warning);
	} // end of brokenSelectionWarnsAndFallsBackToTheDefault method

	/**
	 * A working non-default selection is actually honored, not silently
	 * replaced by the fallback: selecting Nimbus by class name installs
	 * Nimbus, with nothing on stderr.
	 */
	@Test
	void workingSelectionInstallsTheSelectedLookAndFeel() {

		System.setProperty("jls.laf",
				"javax.swing.plaf.nimbus.NimbusLookAndFeel");
		PrintStream realErr = System.err;
		ByteArrayOutputStream captured = new ByteArrayOutputStream();
		boolean installed;
		try {
			System.setErr(new PrintStream(captured, true,
					StandardCharsets.UTF_8));
			installed = JLSStart.installLookAndFeel();
		}
		finally {
			System.setErr(realErr);
		}
		assertTrue(installed);
		assertEquals("javax.swing.plaf.nimbus.NimbusLookAndFeel",
				UIManager.getLookAndFeel().getClass().getName());
		assertEquals("", captured.toString(StandardCharsets.UTF_8));
	} // end of workingSelectionInstallsTheSelectedLookAndFeel method

	/**
	 * The generated usage text documents the escape hatch, like the
	 * jls.toolkit property before it, and the flag-table lines are
	 * untouched (the property line does not start with "  -", so the
	 * usage drift test's flag parsing cannot pick it up).
	 */
	@Test
	void usageDocumentsTheLafProperty() {

		String usage = JLSStart.usageText();
		assertTrue(usage.contains(
				"-Djls.laf=metal|system|<class> selects the Swing look-and-feel"),
				usage);
		assertFalse(usage.contains("  -Djls.laf"), usage);
	} // end of usageDocumentsTheLafProperty method

} // end of LookAndFeelPolicyTest class
