package jls.ui;

import static jls.ui.CircuitAssert.assertElementPresent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import jls.Circuit;
import jls.CircuitTextBuilder;
import jls.JLSInfo;
import jls.elem.AndGate;

/**
 * Pins the subcircuit-editing banner (issue #86 H2, resolved per the
 * 2026-07-17 decision comment: keep the wholesale disable, make the
 * explanation prominent). While a subcircuit tab is open the parent
 * editor shows a full-width banner naming the subcircuit and how to
 * resume editing; mutation gestures on the disabled parent are
 * rejected; re-enabling hides the banner and restores editing.
 *
 * Drives a real {@link jls.edit.Editor} through
 * {@link EditorGestureSupport}; same display tagging and headless
 * discipline as {@link EditorGestureTest}.
 */
@Tag("display")
@Timeout(60)
class SubcircuitDisabledBannerTest {

	@BeforeEach
	void requireDisplay() {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
				"display suite: skipped in headless runs");
	} // end of requireDisplay method

	/**
	 * Build a circuit with a single AND gate near the top-left of the
	 * canvas.
	 *
	 * @return the loaded circuit.
	 * @throws Exception if the circuit text fails to load.
	 */
	private static Circuit oneGate() throws Exception {
		CircuitTextBuilder cb = new CircuitTextBuilder();
		cb.gate("AndGate", 1, 2);
		Circuit circuit = new Circuit("golden");
		assertTrue(circuit.load(new Scanner(cb.build())),
				() -> "load: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
				() -> "finishLoad: " + JLSInfo.loadError);
		return circuit;
	} // end of oneGate method

	/**
	 * Find the banner label inside the editor's component tree by its
	 * component name.
	 *
	 * @param root The container to search below.
	 * @return the banner label, or null if absent.
	 */
	private static JLabel findBanner(Container root) {
		for (Component c : root.getComponents()) {
			if ("subcircuitDisabledBanner".equals(c.getName())
					&& c instanceof JLabel) {
				return (JLabel) c;
			}
			if (c instanceof Container) {
				JLabel found = findBanner((Container) c);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	} // end of findBanner method

	@Test
	void bannerShowsWhileDisabledAndMutationIsRejected() throws Exception {
		Circuit circuit = oneGate();
		try (EditorGestureSupport ui = new EditorGestureSupport(circuit)) {
			AndGate gate = assertElementPresent(circuit, AndGate.class);
			int fromX = gate.getX() + gate.getRect().width / 2;
			int fromY = gate.getY() + gate.getRect().height / 2;

			// the banner exists but is hidden while editing is enabled
			AtomicReference<JLabel> bannerRef = new AtomicReference<>();
			SwingUtilities.invokeAndWait(
					() -> bannerRef.set(findBanner(ui.editor)));
			JLabel banner = bannerRef.get();
			assertNotNull(banner, "banner label present in the editor");
			assertFalse(banner.isVisible(), "banner hidden while enabled");

			// disabling for a subcircuit shows a prominent explanation
			// naming both circuits
			SwingUtilities.invokeAndWait(
					() -> ui.editor.disableForSubcircuit("adder"));
			assertTrue(banner.isVisible(), "banner visible while disabled");
			assertTrue(banner.isShowing(), "banner showing on screen");
			assertTrue(banner.getText().contains("\"adder\""),
					() -> "banner names the subcircuit: " + banner.getText());
			assertTrue(banner.getText().contains("\"golden\""),
					() -> "banner names this circuit: " + banner.getText());
			assertTrue(banner.getText().contains("close that tab"),
					() -> "banner says how to resume: " + banner.getText());

			// mutation attempts on the disabled parent are rejected:
			// a drag that would move the gate leaves it in place
			int beforeX = gate.getX(), beforeY = gate.getY();
			ui.leftDrag(fromX, fromY, fromX + 120, fromY + 96);
			assertEquals(beforeX, gate.getX(), "x unchanged while disabled");
			assertEquals(beforeY, gate.getY(), "y unchanged while disabled");

			// re-enabling hides the banner and restores editing
			SwingUtilities.invokeAndWait(() -> ui.editor.enableEdits());
			assertFalse(banner.isVisible(), "banner hidden after re-enable");
			ui.leftDrag(fromX, fromY, fromX + 120, fromY + 96);
			ui.waitFor(() -> gate.getX() != beforeX || gate.getY() != beforeY,
					"gate moved after re-enable");
		}
	} // end of bannerShowsWhileDisabledAndMutationIsRejected method

} // end of SubcircuitDisabledBannerTest class
