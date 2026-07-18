package jls.ui;

import static jls.ui.CircuitAssert.assertElementPresent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import jls.Circuit;
import jls.FileAbstractor;
import jls.JLSInfo;
import jls.elem.AndGate;
import jls.elem.Element;

/**
 * Editor-level open-&gt;edit-&gt;save and clipboard smoke coverage
 * (issue #56, on the issue #91 Layer-2 gesture harness). The audit
 * behind #56 found both critical bugs in exactly the editor flows the
 * suite never exercised; these tests drive those flows end to end
 * against the real {@link jls.edit.Editor}:
 *
 * <ul>
 * <li>open a circuit file through the sniffing loader, edit it with a
 * mouse gesture, save through {@link jls.edit.Editor#save()}, and
 * reload the written file - pinning that an editor edit survives the
 * full disk round trip;</li>
 * <li>Cut/Copy/Paste driven from the editor's real popup menus,
 * asserting the clipboard circuit and the edited model.</li>
 * </ul>
 *
 * Popup actions are fired with {@code doClick} on the real menu items
 * (synchronous dispatch of the item's action, per the issue's guidance
 * to avoid robot-driven key events); the one genuine pointer read in
 * the paste path is satisfied by
 * {@link EditorGestureSupport#warpPointerTo(int, int)}.
 *
 * Tagged {@code display}: runs under the #162 substrate
 * (xvfb-run -a mvn -B verify -Djls.test.headless=false), self-skips
 * headless.
 */
@Tag("display")
@Timeout(60)
class EditorSaveAndClipboardTest {

	/**
	 * Skip the whole class in headless runs; the display surefire
	 * execution supplies the virtual display these tests need.
	 */
	@BeforeEach
	void requireDisplay() {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
				"display suite: skipped in headless runs");
	}

	/**
	 * The fixture text: one hand-positioned AND gate, named circuit
	 * "golden" so {@link jls.edit.Editor#save()} writes
	 * {@code golden.jls}.
	 *
	 * @return the circuit in the on-disk text format
	 */
	private static String oneGateText() {
		return "CIRCUIT golden\n"
				+ "ELEMENT AndGate\n int id 0\n int x 120\n int y 120\n"
				+ " int width 48\n int height 24\n int bits 1\n"
				+ " int numInputs 2\n String orientation \"right\"\n"
				+ " int delay 10\nEND\n"
				+ "ENDCIRCUIT\n";
	}

	/**
	 * Load circuit text through the model load path.
	 *
	 * @param text the circuit in the on-disk text format
	 * @return the loaded, finished circuit
	 * @throws Exception on any load failure
	 */
	private static Circuit load(String text) throws Exception {
		Circuit circuit = new Circuit("");
		assertTrue(circuit.load(new Scanner(text)),
				() -> "load: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
				() -> "finishLoad: " + JLSInfo.loadError);
		return circuit;
	}

	/**
	 * The horizontal center of an element's bounding box.
	 *
	 * @param el the element
	 * @return the center x coordinate
	 */
	private static int centerX(Element el) {
		return el.getX() + el.getRect().width / 2;
	}

	/**
	 * The vertical center of an element's bounding box.
	 *
	 * @param el the element
	 * @return the center y coordinate
	 */
	private static int centerY(Element el) {
		return el.getY() + el.getRect().height / 2;
	}

	/**
	 * The open-&gt;edit-&gt;save integration test #56 asked for: write a
	 * fixture file, open it through the sniffing loader
	 * ({@link FileAbstractor#openCircuit}), move the gate with a drag
	 * gesture in a real editor, save with the editor's own save action,
	 * and reload the written file - the moved position must be what was
	 * persisted, and the unsaved-changes flag must stand down.
	 *
	 * @param tmp per-test scratch directory
	 * @throws Exception on any harness failure
	 */
	@Test
	void openEditSaveRoundTripPersistsTheEdit(@TempDir Path tmp)
			throws Exception {
		// "open": a real file on disk, read back through the sniffer
		File file = tmp.resolve("golden.jls").toFile();
		FileAbstractor.writeCircuit(file, oneGateText());
		Scanner input = FileAbstractor.openCircuit(file.getAbsolutePath());
		assertNotNull(input, "sniffing loader must open the fixture");
		Circuit circuit = new Circuit("");
		assertTrue(circuit.load(input), () -> "load: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
				() -> "finishLoad: " + JLSInfo.loadError);
		circuit.setDirectory(tmp.toString());

		int savedX;
		int savedY;
		try (EditorGestureSupport ui = new EditorGestureSupport(circuit)) {
			// "edit": drag the gate somewhere else
			AndGate gate = assertElementPresent(circuit, AndGate.class);
			int beforeX = gate.getX(), beforeY = gate.getY();
			ui.leftDrag(centerX(gate), centerY(gate),
					centerX(gate) + 120, centerY(gate) + 96);
			ui.waitFor(() -> gate.getX() != beforeX || gate.getY() != beforeY,
					"gate moved");
			assertTrue(circuit.hasChanged(),
					"the edit must raise the unsaved-changes flag");
			savedX = gate.getX();
			savedY = gate.getY();

			// "save": the editor's own save path, on the EDT like the
			// menu action that normally invokes it
			AtomicBoolean saved = new AtomicBoolean();
			SwingUtilities.invokeAndWait(
					() -> saved.set(ui.editor.save()));
			assertTrue(saved.get(), "editor save must succeed");
			assertFalse(circuit.hasChanged(),
					"save must clear the unsaved-changes flag");
		}

		// reload the file the editor wrote and find the edit in it
		Scanner reread = FileAbstractor.openCircuit(file.getAbsolutePath());
		assertNotNull(reread, "saved file must reopen through the sniffer");
		Circuit reloaded = new Circuit("");
		assertTrue(reloaded.load(reread),
				() -> "reload: " + JLSInfo.loadError);
		assertTrue(reloaded.finishLoad(null),
				() -> "refinish: " + JLSInfo.loadError);
		AndGate persisted = assertElementPresent(reloaded, AndGate.class);
		assertEquals(savedX, persisted.getX(),
				"saved file must hold the moved x position");
		assertEquals(savedY, persisted.getY(),
				"saved file must hold the moved y position");
	}

	/**
	 * Copy from the element popup: the clipboard circuit gains a copy of
	 * the gate and the source circuit keeps its own.
	 *
	 * @throws Exception on any harness failure
	 */
	@Test
	void copyViaPopupPutsACopyOnTheClipboard() throws Exception {
		Circuit circuit = load(oneGateText());
		try (EditorGestureSupport ui = new EditorGestureSupport(circuit)) {
			AndGate gate = assertElementPresent(circuit, AndGate.class);
			ui.rightPress(centerX(gate), centerY(gate));
			ui.clickPopupItem("Copy");
			ui.waitFor(() -> ui.clipboardCircuit.getElements().stream()
					.anyMatch(el -> el instanceof AndGate),
					"gate copied to the clipboard");
			// copy, not cut: the original stays put
			assertElementPresent(circuit, AndGate.class);
		}
	}

	/**
	 * Cut from the element popup: the gate moves to the clipboard and
	 * leaves the circuit, and the model registers the change.
	 *
	 * @throws Exception on any harness failure
	 */
	@Test
	void cutViaPopupMovesTheElementToTheClipboard() throws Exception {
		Circuit circuit = load(oneGateText());
		try (EditorGestureSupport ui = new EditorGestureSupport(circuit)) {
			AndGate gate = assertElementPresent(circuit, AndGate.class);
			ui.rightPress(centerX(gate), centerY(gate));
			ui.clickPopupItem("Cut");
			ui.waitFor(() -> circuit.getElements().stream()
					.noneMatch(el -> el instanceof AndGate),
					"gate cut from the circuit");
			assertTrue(ui.clipboardCircuit.getElements().stream()
					.anyMatch(el -> el instanceof AndGate),
					"cut gate must land on the clipboard");
			assertTrue(circuit.hasChanged(),
					"cut must raise the unsaved-changes flag");
		}
	}

	/**
	 * Paste from the empty-canvas popup: after a copy, paste enters the
	 * placing state with a second gate in the circuit, and a left click
	 * fixes it in place (placement done exactly when nothing is left
	 * highlighted). The real pointer is parked over the canvas first
	 * because the paste path reads {@code getMousePosition()} - see
	 * {@link EditorGestureSupport#warpPointerTo(int, int)}.
	 *
	 * @throws Exception on any harness failure
	 */
	@Test
	void pasteViaPopupPlacesAClipboardCopy() throws Exception {
		Circuit circuit = load(oneGateText());
		try (EditorGestureSupport ui = new EditorGestureSupport(circuit)) {
			AndGate gate = assertElementPresent(circuit, AndGate.class);
			ui.rightPress(centerX(gate), centerY(gate));
			ui.clickPopupItem("Copy");
			ui.waitFor(() -> ui.clipboardCircuit.getElements().stream()
					.anyMatch(el -> el instanceof AndGate),
					"gate copied to the clipboard");

			// park the pointer well away from both the source gate and
			// the popup location, then paste from the empty-canvas menu
			ui.warpPointerTo(360, 300);
			ui.rightPress(700, 600);
			ui.clickPopupItem("Paste");
			ui.waitFor(() -> circuit.getElements().stream()
					.filter(el -> el instanceof AndGate).count() == 2,
					"pasted gate added to the circuit");

			// drop it: a left click in the placing state fixes positions
			ui.leftClick(360, 300);
			ui.waitFor(() -> circuit.getElements().stream()
					.filter(el -> el instanceof AndGate)
					.noneMatch(Element::isHighlighted),
					"pasted gate placed (selection cleared)");
			assertEquals(2, circuit.getElements().stream()
					.filter(el -> el instanceof AndGate).count(),
					"both gates must remain after placement");
		}
	}
}
