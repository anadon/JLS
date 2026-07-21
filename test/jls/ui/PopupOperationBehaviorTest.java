package jls.ui;

import static jls.ui.CircuitAssert.assertElementPresent;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Scanner;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import jls.Circuit;
import jls.CircuitTextBuilder;
import jls.JLSInfo;
import jls.elem.AndGate;
import jls.elem.Element;
import jls.elem.Gate;

/**
 * Right-click popup editing operations, verified behaviorally (issue #75
 * mouse non-regression, the highest-risk shared-Action gap). After the H1
 * refactor the popup Rotate/Flip/Watch items all point at the same shared
 * {@link javax.swing.Action} as the canvas key bindings.
 * {@link EditActionMatrixTest} pins object <em>identity</em> for a few of
 * them but never fires them, and Watch/Probe/Modify/Timing had no popup
 * coverage at all - so a regression that left a popup item wired to a
 * disabled or no-op Action would pass. This drives the popup items the way a
 * user's click does ({@code doClick} on the shown menu item, after the
 * right-press that selects the element) and observes the element's persisted
 * form change, proving the op actually ran through the popup surface.
 *
 * <p>Tagged {@code display}: needs a display for the popup {@code show()}
 * and font metrics, so it runs under the #162 substrate
 * (xvfb-run -a mvn -B verify -Djls.test.headless=false).</p>
 */
@Tag("display")
@Timeout(120)
class PopupOperationBehaviorTest {

	@BeforeEach
	void requireDisplay() {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
				"display suite: skipped in headless runs");
	}

	/** A circuit with a single, unattached AND gate that can rotate/flip. */
	private static Circuit oneGate() throws Exception {
		CircuitTextBuilder cb = new CircuitTextBuilder();
		cb.gate("AndGate", 1, 2);
		Circuit circuit = new Circuit("popup-ops");
		assertTrue(circuit.load(new Scanner(cb.build())),
				() -> "load: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
				() -> "finishLoad: " + JLSInfo.loadError);
		return circuit;
	}

	private static String save(Element el) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		el.save(pw);
		pw.flush();
		return sw.toString();
	}

	private static Gate soleGate(EditorGestureSupport ui) {
		for (Element el : ui.currentCircuit().getElements()) {
			if (el instanceof Gate g) {
				return g;
			}
		}
		throw new AssertionError("no Gate in the circuit");
	}

	private static int centerX(Element el) {
		return el.getX() + el.getRect().width / 2;
	}

	private static int centerY(Element el) {
		return el.getY() + el.getRect().height / 2;
	}

	/**
	 * Fire one popup item on the sole gate and assert its persisted form
	 * changed - the op ran through the popup surface's shared Action. A
	 * single popup interaction per test keeps it robust against the
	 * popup-re-show timing that makes back-to-back popups flaky.
	 *
	 * @param itemText the popup menu item label to click.
	 * @throws Exception on EDT failure.
	 */
	private void assertPopupItemMutatesTheGate(String itemText)
			throws Exception {
		Circuit circuit = oneGate();
		try (EditorGestureSupport ui = new EditorGestureSupport(circuit)) {
			AndGate gate = assertElementPresent(circuit, AndGate.class);
			String original = save(gate);

			openPopupTo(ui, gate, itemText);
			ui.clickPopupItem(itemText);
			ui.waitFor(() -> !save(soleGate(ui)).equals(original),
					"popup '" + itemText + "' mutated the gate");
			assertNotEquals(original, save(soleGate(ui)),
					"popup '" + itemText + "' changed the persisted form");
		}
	}

	/**
	 * Right-press the gate to raise its popup, retrying the press until the
	 * target item is actually showing. A single right-press occasionally
	 * fails to materialize the popup under the WM-less #162 Xvfb (a
	 * popup-show timing flake, the same class as {@link EditorGestureTest}'s
	 * known popup flake); re-pressing only while no popup is up recovers it
	 * without stacking popups. Each attempt waits a bounded ~2s for the popup
	 * to appear before the next press, so a genuinely broken popup still
	 * fails fast rather than hanging to the outer timeout.
	 *
	 * @param ui the gesture harness.
	 * @param gate the gate to right-press.
	 * @param itemText the popup item that must become visible.
	 * @throws Exception on EDT failure.
	 */
	private static void openPopupTo(EditorGestureSupport ui, Gate gate,
			String itemText) throws Exception {
		for (int attempt = 0; attempt < 6; attempt++) {
			ui.rightPress(centerX(gate), centerY(gate));
			for (int i = 0; i < 40; i++) {
				if (ui.isPopupItemShowing(itemText)) {
					return;
				}
				ui.pause(50);
			}
		}
		throw new AssertionError("popup item '" + itemText
				+ "' never appeared after 6 right-presses");
	}

	/**
	 * The popup Rotate Clockwise item rotates the selected gate through its
	 * shared Action.
	 *
	 * @throws Exception on EDT failure.
	 */
	@Test
	void popupRotateClockwiseRotatesTheGate() throws Exception {
		assertPopupItemMutatesTheGate("Rotate Clockwise");
	}

	/**
	 * The popup Rotate Counter-Clockwise item rotates the selected gate
	 * through its (distinct) shared Action.
	 *
	 * @throws Exception on EDT failure.
	 */
	@Test
	void popupRotateCounterClockwiseRotatesTheGate() throws Exception {
		assertPopupItemMutatesTheGate("Rotate Counter-Clockwise");
	}

	/**
	 * The popup Flip item flips the selected gate through its shared Action.
	 *
	 * @throws Exception on EDT failure.
	 */
	@Test
	void popupFlipFlipsTheGate() throws Exception {
		assertPopupItemMutatesTheGate("Flip");
	}
}
