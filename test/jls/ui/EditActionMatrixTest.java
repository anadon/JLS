package jls.ui;

import static jls.ui.CircuitAssert.assertElementPresent;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import jls.Circuit;
import jls.CircuitTextBuilder;
import jls.DefaultExceptionHandler;
import jls.JLSInfo;
import jls.JLSStart;
import jls.edit.EditOp;
import jls.edit.Editor;
import jls.elem.AndGate;
import jls.elem.Element;
import jls.sim.Simulator;

/**
 * Shared-Action matrix (issue #75, H1). Proves the three editing
 * surfaces - the canvas key bindings, the right-click popup menus, and
 * the main window's Edit and Element menus - all dispatch through the
 * <em>same</em> {@link Action} object for each {@link EditOp}, rather
 * than three separate copies of the operation. The "action x path"
 * coverage is an object-identity check per surface: a regression that
 * re-split any surface into its own action would break the identity
 * assertion.
 *
 * <p>Each canvas binding is additionally pinned by resolving its
 * keystroke through the canvas maps: removing the InputMap stroke or the
 * ActionMap entry makes {@link EditorGestureSupport#canvasActionForStroke}
 * return null, failing the identity assertion. One binding (Delete) is
 * also driven end to end by a synthetic keystroke to confirm the wiring
 * actually mutates the model.</p>
 *
 * <p>Tagged {@code display}: boots real Swing components, so it runs
 * under the #162 substrate and self-skips headless.</p>
 */
@Tag("display")
@Timeout(90)
class EditActionMatrixTest {

	@BeforeEach
	void requireDisplay() {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
				"display suite: skipped in headless runs");
	}

	/** A circuit with a single AND gate near the top-left of the canvas. */
	private static Circuit oneGate() throws Exception {
		CircuitTextBuilder cb = new CircuitTextBuilder();
		cb.gate("AndGate", 1, 2);
		Circuit circuit = new Circuit("matrix");
		assertTrue(circuit.load(new Scanner(cb.build())),
				() -> "load: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
				() -> "finishLoad: " + JLSInfo.loadError);
		return circuit;
	}

	private static int centerX(Element el) {
		return el.getX() + el.getRect().width / 2;
	}

	private static int centerY(Element el) {
		return el.getY() + el.getRect().height / 2;
	}

	/**
	 * Every {@link EditOp} exposes one stable shared Action, and the
	 * canvas binding and the popup item for an operation are that same
	 * object - not copies.
	 *
	 * @throws Exception on EDT failure.
	 */
	@Test
	void canvasBindingsAndPopupsShareTheEditorActions() throws Exception {
		Circuit circuit = oneGate();
		String os = System.getProperty("os.name");
		try (EditorGestureSupport ui = new EditorGestureSupport(circuit)) {
			Editor ed = ui.editor;

			// one stable shared instance per operation
			for (EditOp op : EditOp.values()) {
				Action a = ed.editAction(op);
				assertNotNull(a, "no shared Action for " + op);
				assertSame(a, ed.editAction(op),
						"editAction must return a shared instance for " + op);
			}

			// each canvas binding resolves to that same shared object;
			// this fails if either the InputMap stroke or the ActionMap
			// entry were removed
			for (EditOp op : EditOp.values()) {
				KeyStroke ks = op.accelerator(os);
				assertSame(ed.editAction(op), ui.canvasActionForStroke(ks),
						"canvas binding " + ks + " must be the shared Action "
								+ "for " + op);
			}

			// the popup items reuse the shared Actions too
			AndGate gate = assertElementPresent(circuit, AndGate.class);
			ui.rightPress(centerX(gate), centerY(gate));
			assertSame(ed.editAction(EditOp.CUT),
					ui.popupItem("Cut").getAction(),
					"popup Cut must be the shared CUT Action");
			assertSame(ed.editAction(EditOp.ROTATE_CW),
					ui.popupItem("Rotate Clockwise").getAction(),
					"popup Rotate Clockwise must be the shared ROTATE_CW Action");
			assertSame(ed.editAction(EditOp.FLIP),
					ui.popupItem("Flip").getAction(),
					"popup Flip must be the shared FLIP Action");
		}
	}

	/**
	 * Driving the plain-Delete keystroke into the canvas removes the
	 * selection through the shared DELETE Action - the binding is wired
	 * end to end, not merely present in the map.
	 *
	 * @throws Exception on EDT failure.
	 */
	@Test
	void deleteKeystrokeRemovesTheSelectionThroughTheSharedAction()
			throws Exception {
		Circuit circuit = oneGate();
		try (EditorGestureSupport ui = new EditorGestureSupport(circuit)) {
			AndGate gate = assertElementPresent(circuit, AndGate.class);
			// rubber-band select the gate, then press Delete on the canvas
			int x0 = gate.getX() - 40, y0 = gate.getY() - 40;
			int x1 = gate.getX() + gate.getRect().width + 40;
			int y1 = gate.getY() + gate.getRect().height + 40;
			ui.leftDrag(x0, y0, x1, y1);
			ui.waitFor(gate::isHighlighted, "gate selected");

			ui.pressKey(KeyEvent.VK_DELETE);
			ui.waitFor(() -> ui.currentCircuit().getElements().stream()
					.noneMatch(el -> el instanceof AndGate),
					"gate deleted by the Delete key binding");
		}
	}

	/**
	 * The main window's Edit and Element menu items dispatch through the
	 * selected editor's shared Actions after a tab-change retarget - the
	 * third surface reuses the same objects as the popups and bindings.
	 *
	 * @throws Exception on reflection or EDT failure.
	 */
	@Test
	void menuBarItemsShareTheSelectedEditorActions() throws Exception {
		java.awt.Frame savedFrame = JLSInfo.frame;
		Simulator savedSim = JLSInfo.sim;
		try {
			JLSStart jls = bootMainWindow();
			AtomicReference<Editor> edRef = new AtomicReference<>();
			SwingUtilities.invokeAndWait(() -> {
				jls.setupEditor(new Circuit("menubar"), "menubar");
				edRef.set(jls.getVisibleEditor());
			});
			Editor ed = edRef.get();
			assertNotNull(ed, "setupEditor must make an editor visible");

			AtomicReference<Boolean> ok = new AtomicReference<>(Boolean.TRUE);
			AtomicReference<String> why = new AtomicReference<>("");
			SwingUtilities.invokeAndWait(() -> {
				JMenuBar bar = jls.getJMenuBar();
				for (EditOp op : new EditOp[] { EditOp.UNDO, EditOp.CUT,
						EditOp.COPY, EditOp.PASTE, EditOp.DELETE,
						EditOp.SELECT_ALL, EditOp.ROTATE_CW, EditOp.ROTATE_CCW,
						EditOp.FLIP, EditOp.WATCH, EditOp.PROBE, EditOp.MODIFY,
						EditOp.TIMING }) {
					JMenuItem item = menuItemFor(bar, op.label());
					if (item == null) {
						ok.set(Boolean.FALSE);
						why.set("no menu item labelled " + op.label());
						return;
					}
					if (item.getAction() != ed.editAction(op)) {
						ok.set(Boolean.FALSE);
						why.set("menu item " + op.label()
								+ " is not the shared Action");
						return;
					}
				}
			});
			assertTrue(ok.get().booleanValue(), why.get());
		}
		finally {
			SwingUtilities.invokeAndWait(() -> {
				for (Window w : Window.getWindows()) {
					w.dispose();
				}
			});
			JLSInfo.frame = savedFrame;
			JLSInfo.sim = savedSim;
		}
	}

	/**
	 * Find the first menu-bar item (in any top-level menu) with the given
	 * label. Call on the EDT.
	 *
	 * @param bar the menu bar to search.
	 * @param label the item label to match.
	 * @return the item, or null if none matches.
	 */
	private static JMenuItem menuItemFor(JMenuBar bar, String label) {
		for (int i = 0; i < bar.getMenuCount(); i++) {
			JMenu menu = bar.getMenu(i);
			if (menu == null) {
				continue;
			}
			for (int j = 0; j < menu.getItemCount(); j++) {
				JMenuItem item = menu.getItem(j);
				if (item != null && label.equals(item.getText())) {
					return item;
				}
			}
		}
		return null;
	}

	/**
	 * Construct the real JLS main window on the EDT, installing the
	 * static exception handler the constructor needs (mirrors
	 * {@link MenuBarSpecTest}'s harness).
	 *
	 * @return the constructed main window.
	 * @throws Exception on reflection or EDT failure.
	 */
	private static JLSStart bootMainWindow() throws Exception {
		Field handler = JLSStart.class.getDeclaredField("exHandler");
		handler.setAccessible(true);
		if (handler.get(null) == null) {
			handler.set(null, new DefaultExceptionHandler());
		}
		AtomicReference<JLSStart> ref = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> ref.set(new JLSStart()));
		return ref.get();
	}
}
