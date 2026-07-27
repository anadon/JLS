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
import jls.elem.Register;
import jls.elem.Wire;

/**
 * Right-click popup editing operations, verified behaviorally (issue #75
 * mouse non-regression, the highest-risk shared-Action gap). After the H1
 * refactor the popup Rotate/Flip/Watch items all point at the same shared
 * {@link javax.swing.Action} as the canvas key bindings.
 * {@link EditActionMatrixTest} pins object <em>identity</em> for a few of
 * them but never fires them, and Watch/Probe/Modify/Timing had no popup
 * coverage at all - so a regression that left a popup item wired to a
 * disabled or no-op Action would pass. Issue #86 (H3) closed the
 * non-modal half of that gap: watch and probe dispatch are driven below,
 * while Modify/Timing stay uncovered here because they open modal
 * dialogs. Which items each element's menu contains is pinned headless
 * by {@code jls.edit.OptionMenuPolicyTest}. This drives the popup items the way a
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
		return el.getX() + el.getRect().width() / 2;
	}

	private static int centerY(Element el) {
		return el.getY() + el.getRect().height() / 2;
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

			openPopupTo(ui, centerX(gate), centerY(gate), itemText);
			ui.clickPopupItem(itemText);
			ui.waitFor(() -> !save(soleGate(ui)).equals(original),
					"popup '" + itemText + "' mutated the gate");
			assertNotEquals(original, save(soleGate(ui)),
					"popup '" + itemText + "' changed the persisted form");
		}
	}

	/**
	 * Right-press a point to raise its popup, retrying the press until the
	 * target item is actually showing. A single right-press occasionally
	 * fails to materialize the popup under the WM-less #162 Xvfb (a
	 * popup-show timing flake, the same class as {@link EditorGestureTest}'s
	 * known popup flake); re-pressing only while no popup is up recovers it
	 * without stacking popups. Each attempt waits a bounded ~2s for the popup
	 * to appear before the next press, so a genuinely broken popup still
	 * fails fast rather than hanging to the outer timeout.
	 *
	 * @param ui the gesture harness.
	 * @param x the circuit x coordinate to right-press.
	 * @param y the circuit y coordinate to right-press.
	 * @param itemText the popup item that must become visible.
	 * @throws Exception on EDT failure.
	 */
	private static void openPopupTo(EditorGestureSupport ui, int x, int y,
			String itemText) throws Exception {
		for (int attempt = 0; attempt < 6; attempt++) {
			ui.rightPress(x, y);
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

	// ------------------------------------------------------------------
	// watch and probe dispatch (issue #86 H3): the non-modal half of the
	// coverage gap this class's javadoc names - Modify/Timing open modal
	// dialogs, so they stay out of scope here
	// ------------------------------------------------------------------

	/** A circuit with a single watched D register (Watchable). */
	private static Circuit oneRegister() throws Exception {
		CircuitTextBuilder cb = new CircuitTextBuilder();
		cb.register(4, 0, "D");
		Circuit circuit = new Circuit("popup-watch");
		assertTrue(circuit.load(new Scanner(cb.build())),
				() -> "load: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
				() -> "finishLoad: " + JLSInfo.loadError);
		return circuit;
	}

	/**
	 * A circuit with one wire long enough to right-click: Wire.contains
	 * rejects points within POINT_DIAMETER (6px) of either end, so the
	 * ends sit 240px apart - the builder's fixed 12px wire-end spacing
	 * would make the wire unclickable, hence the explicit text.
	 */
	private static Circuit oneLongWire() throws Exception {
		String text = """
				CIRCUIT popup-probe
				ELEMENT Constant
				 int id 0
				 int x 60
				 int y 60
				 int width 24
				 int height 24
				 Int value 1
				 int base 10
				 String orient "RIGHT"
				END
				ELEMENT OutputPin
				 int id 1
				 int x 480
				 int y 60
				 int width 24
				 int height 24
				 String name "out"
				 int bits 1
				 int watch 0
				 String orient "RIGHT"
				END
				ELEMENT WireEnd
				 int id 2
				 int x 120
				 int y 300
				 int width 8
				 int height 8
				 String put "output"
				 ref attach 0
				 ref wire 3
				END
				ELEMENT WireEnd
				 int id 3
				 int x 360
				 int y 300
				 int width 8
				 int height 8
				 String put "input"
				 ref attach 1
				 ref wire 2
				END
				ENDCIRCUIT
				""";
		Circuit circuit = new Circuit("popup-probe");
		assertTrue(circuit.load(new Scanner(text)),
				() -> "load: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
				() -> "finishLoad: " + JLSInfo.loadError);
		return circuit;
	}

	/**
	 * The popup Un-watch item on a watched register dispatches the shared
	 * WATCH Action and clears the watched flag. Watching is non-modal
	 * (ToggleWatched flips model state directly), unlike Modify/Timing.
	 *
	 * @throws Exception on EDT failure.
	 */
	@Test
	void popupUnwatchClearsTheRegisterWatchFlag() throws Exception {
		Circuit circuit = oneRegister();
		try (EditorGestureSupport ui = new EditorGestureSupport(circuit)) {
			Register reg = assertElementPresent(circuit, Register.class);
			assertTrue(reg.isWatched(), "builder loads the register watched");

			openPopupTo(ui, centerX(reg), centerY(reg), "Un-watch Element");
			ui.clickPopupItem("Un-watch Element");
			ui.waitFor(() -> !reg.isWatched(),
					"popup un-watch cleared the watched flag");
		}
	}

	/**
	 * The popup Remove Probe item on a probed wire dispatches the shared
	 * PROBE Action and removes the probe. The remove direction is the
	 * non-modal one (attach prompts for a probe name), so the test
	 * pre-attaches a probe to the model before driving the popup.
	 *
	 * @throws Exception on EDT failure.
	 */
	@Test
	void popupRemoveProbeRemovesTheWireProbe() throws Exception {
		Circuit circuit = oneLongWire();
		Wire wire = assertElementPresent(circuit, Wire.class);
		wire.attachProbe("p0");
		try (EditorGestureSupport ui = new EditorGestureSupport(circuit)) {
			int midX = (wire.getEnd().getX() + wire.getEnd2().getX()) / 2;
			int midY = (wire.getEnd().getY() + wire.getEnd2().getY()) / 2;

			openPopupTo(ui, midX, midY, "Remove Probe");
			ui.clickPopupItem("Remove Probe");
			ui.waitFor(() -> !wire.hasProbe(),
					"popup remove-probe detached the probe");
		}
	}
}
