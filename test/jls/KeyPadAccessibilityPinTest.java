package jls;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * Pins the KeyPad accessibility wiring (issue #75) in headless runs.
 * The KeyPad's toggle button (the KeyPad itself, down.gif) and hide
 * button (up.gif) are icon-only, so without an explicit accessible
 * name a screen reader focusing them announces nothing, and the reset
 * button's bare "C" announces as a letter rather than an action.
 *
 * The GUI cannot be constructed under surefire's
 * {@code java.awt.headless=true}, so the constructor wiring is pinned
 * by reading the source from the repo tree — the same
 * compensating-control pattern as MenuAcceleratorPolicyTest. The real
 * accessibility channel is asserted by the display-tagged
 * {@code jls.ui.KeyPadAccessibilityTest} under the #162 substrate.
 */
class KeyPadAccessibilityPinTest {

	/**
	 * The icon-only toggle button (the KeyPad itself) sets an
	 * accessible name, a tooltip, and its harness component name.
	 */
	@Test
	void toggleButtonIsWiredForAccessibility() throws IOException {
		String text = readSource("src/jls/KeyPad.java");

		assertTrue(text.contains(
				"getAccessibleContext().setAccessibleName(\"Show keypad\")"),
				"the icon-only toggle button must set an accessible name");
		assertTrue(text.contains("setToolTipText(\"Show keypad\")"),
				"the toggle button must have a tooltip");
		assertTrue(text.contains("setName(\"keypad.toggle\")"),
				"the toggle button must carry its #91 harness name");
	}

	/**
	 * The icon-only hide button sets an accessible name, a tooltip,
	 * and its harness component name.
	 */
	@Test
	void hideButtonIsWiredForAccessibility() throws IOException {
		String text = readSource("src/jls/KeyPad.java");

		assertTrue(text.contains(
				"hide.getAccessibleContext().setAccessibleName(\"Hide keypad\")"),
				"the icon-only hide button must set an accessible name");
		assertTrue(text.contains("hide.setToolTipText(\"Hide keypad\")"),
				"the hide button must have a tooltip");
		assertTrue(text.contains("hide.setName(\"keypad.hide\")"),
				"the hide button must carry its #91 harness name");
	}

	/**
	 * The reset button announces its action ("Clear to default value")
	 * instead of the bare letter C, and carries a tooltip and its
	 * harness component name.
	 */
	@Test
	void resetButtonAnnouncesItsAction() throws IOException {
		String text = readSource("src/jls/KeyPad.java");

		assertTrue(text.contains("reset.getAccessibleContext()"),
				"the reset button must set an explicit accessible name");
		assertTrue(text.contains("\"Clear to default value\")"),
				"the reset button's accessible name must describe the"
						+ " action, not the bare letter C");
		assertTrue(text.contains(
				"reset.setToolTipText(\"Clear to default value\")"),
				"the reset button must have a tooltip");
		assertTrue(text.contains("reset.setName(\"keypad.reset\")"),
				"the reset button must carry its #91 harness name");
	}

	/**
	 * Read a source file from the repo tree (maven runs tests with
	 * user.dir at the repo root).
	 *
	 * @param relative repo-relative path of the source file.
	 *
	 * @return the file's text.
	 *
	 * @throws IOException if the file cannot be read.
	 */
	private static String readSource(String relative) throws IOException {
		Path source = Path.of(System.getProperty("user.dir"), relative);
		assertTrue(Files.isRegularFile(source),
				"run from the repo root (maven sets user.dir there): "
						+ source);
		return Files.readString(source, StandardCharsets.UTF_8);
	} // end of readSource method

} // end of KeyPadAccessibilityPinTest class
