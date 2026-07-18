package jls.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Window;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import jls.Tutorial;

/**
 * Display-suite smoke for the navigable tutorial dialog (issue #73):
 * the dialog opens at the requested page, the Previous/Next buttons
 * walk the four-page sequence with the correct enabled state at both
 * ends, and the title tracks the page. Runs in the display surefire
 * execution ({@code xvfb-run -a mvn verify -Djls.test.headless=false});
 * self-skips headless like the rest of the {@code display} tag.
 */
@Tag("display")
class TutorialNavigationTest {

	/** The dialog under test, built on the EDT per test. */
	private Tutorial tutorial;

	@BeforeEach
	void requireDisplay() {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
				"display suite: skipped in headless runs");
	}

	@AfterEach
	void disposeWindows() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			for (Window w : Window.getWindows()) {
				w.dispose();
			}
		});
	}

	/**
	 * Find the unique JButton whose text starts with the given prefix.
	 *
	 * @param root Container to search below.
	 * @param prefix Button label prefix ("Next" or "&lt; Previous").
	 * @return the matching button, or null if absent.
	 */
	private static JButton findButton(Container root, String prefix) {
		for (Component c : root.getComponents()) {
			if (c instanceof JButton
					&& ((JButton) c).getText().contains(prefix)) {
				return (JButton) c;
			}
			if (c instanceof Container) {
				JButton nested = findButton((Container) c, prefix);
				if (nested != null) {
					return nested;
				}
			}
		}
		return null;
	}

	@Test
	void nextAndPreviousWalkTheSequence() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			tutorial = new Tutorial(null, 0);
			JButton previous = findButton(tutorial, "Previous");
			JButton next = findButton(tutorial, "Next");
			assertNotNull(previous, "Previous button missing");
			assertNotNull(next, "Next button missing");

			// first page: can only go forward
			assertTrue(tutorial.getTitle().contains("Introduction"));
			assertFalse(previous.isEnabled(),
					"Previous must be disabled on the first page");
			assertTrue(next.isEnabled());

			// walk forward to the last page
			next.doClick();
			assertTrue(tutorial.getTitle().contains("4-Bit Counter"));
			assertTrue(previous.isEnabled(),
					"Previous must enable once off the first page");
			next.doClick();
			assertTrue(tutorial.getTitle().contains("Full Adder"));
			next.doClick();
			assertTrue(tutorial.getTitle().contains("Sign Extension"));
			assertFalse(next.isEnabled(),
					"Next must be disabled on the last page");

			// and back
			previous.doClick();
			assertTrue(tutorial.getTitle().contains("Full Adder"));
			assertTrue(next.isEnabled());
		});
	}

	@Test
	void menuEntryPointsOpenTheirPage() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			// Help->Tutorial->Full Adder constructs Tutorial(frame, 2)
			tutorial = new Tutorial(null, 2);
			assertTrue(tutorial.getTitle().contains("Full Adder"));
			assertTrue(tutorial.isResizable(),
					"the tutorial dialog must stay resizable");
		});
	}
}
