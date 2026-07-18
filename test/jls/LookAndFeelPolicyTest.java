package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.swing.UIManager;

import org.junit.jupiter.api.Test;

/**
 * The look-and-feel policy for issue #76: JLS keeps cross-platform
 * Metal as the default (continuity), but {@code -Djls.laf=system}
 * opts into the platform look, and a failure to install a look and
 * feel no longer terminates the application (the historical
 * {@code System.exit(1)} on this path is gone - the resolver below is
 * the only L&amp;F decision left, and the caller merely warns on
 * stderr and falls through).
 */
class LookAndFeelPolicyTest {

	@Test
	void unsetPropertyKeepsTheCrossPlatformLook() {
		assertEquals(UIManager.getCrossPlatformLookAndFeelClassName(),
				JLSStart.lookAndFeelClassName(null));
	}

	@Test
	void metalKeepsTheCrossPlatformLook() {
		assertEquals(UIManager.getCrossPlatformLookAndFeelClassName(),
				JLSStart.lookAndFeelClassName("metal"));
	}

	@Test
	void systemSelectsThePlatformLook() {
		assertEquals(UIManager.getSystemLookAndFeelClassName(),
				JLSStart.lookAndFeelClassName("system"));
	}

	@Test
	void unknownValuesFallBackToTheCrossPlatformLook() {
		assertEquals(UIManager.getCrossPlatformLookAndFeelClassName(),
				JLSStart.lookAndFeelClassName("gtk-or-typo"));
	}

} // end of LookAndFeelPolicyTest class
