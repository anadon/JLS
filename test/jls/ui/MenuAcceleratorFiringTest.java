package jls.ui;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JButton;
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
import jls.MenuAcceleratorPolicy;
import jls.edit.EditOp;
import jls.edit.Editor;
import jls.elem.Element;
import jls.sim.Simulator;

/**
 * Window-scoped menu accelerator firing (issue #75 verification hardening).
 * The Edit and Element menu items carry {@code WHEN_IN_FOCUSED_WINDOW}
 * accelerators so a user's Ctrl+A / Delete fires the op no matter which
 * component in the window holds focus - the whole reason the menu binding
 * is window-scoped rather than the canvas' {@code WHEN_FOCUSED} binding.
 * Nothing proved that firing before: {@link MenuBarSpecTest} reads the
 * displayed accelerator and {@link EditActionMatrixTest} drives one key
 * (Delete) straight into the canvas via {@code canvas.dispatchEvent}, which
 * exercises the canvas map, never the window-scoped menu accelerator, and
 * bypasses focus entirely.
 *
 * <p>This boots the real {@link JLSStart} main window, gives focus to a
 * NON-canvas component (a tool-bar palette button), and dispatches the
 * accelerator through the live {@link KeyboardFocusManager} focus owner -
 * the same path the AWT pipeline uses, which routes a focus owner's key
 * event up to the window's {@code WHEN_IN_FOCUSED_WINDOW} bindings. Select
 * All then highlights the circuit and Delete removes it, proving the
 * accelerator fired its shared Action with focus off the canvas. A
 * regression that dropped the accelerator when {@code retargetEditMenus}
 * swaps the Action, never added the menu, or broke window scope, turns this
 * red.</p>
 *
 * <p>Both accelerators are idempotent (Select All twice selects the same
 * set; Delete-all twice leaves nothing), so this stays robust even if the
 * focus-owner dispatch delivered more than one event.</p>
 *
 * <p>Tagged {@code display}: {@code new JLSStart()} is a visible frame, so
 * it runs under the #162 substrate
 * (xvfb-run -a mvn -B verify -Djls.test.headless=false).</p>
 */
@Tag("display")
@Timeout(120)
class MenuAcceleratorFiringTest {

	@BeforeEach
	void requireDisplay() {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
				"display suite: skipped in headless runs");
	}

	/**
	 * With focus on a tool-bar palette button, Ctrl+A selects the whole
	 * circuit and Delete removes it - the window-scoped menu accelerators
	 * fire their shared Actions from a non-canvas focus owner.
	 *
	 * @throws Exception on reflection or EDT failure.
	 */
	@Test
	void windowScopedAcceleratorsFireFromANonCanvasFocusOwner()
			throws Exception {
		Frame savedFrame = JLSInfo.frame;
		Simulator savedSim = JLSInfo.sim;
		String os = System.getProperty("os.name");
		try {
			JLSStart jls = bootMainWindow();
			Circuit circuit = twoGates();
			AtomicReference<Editor> edRef = new AtomicReference<>();
			SwingUtilities.invokeAndWait(() -> {
				jls.setupEditor(circuit, "accel");
				edRef.set(jls.getVisibleEditor());
			});
			Editor ed = edRef.get();
			assertNotNull(ed, "setupEditor made an editor visible");

			// focus a NON-canvas component: a tool-bar palette button
			JButton palette = findToolbarButton(ed, "AND gate");
			assertNotNull(palette, "the visible editor has an AND gate button");
			giveFocusTo(palette);
			assertTrue(focusOwner() instanceof JButton,
					"focus is on the tool-bar button, not the canvas");

			// Ctrl+A (Select All) through the window's focus owner
			pressThroughFocusOwner(EditOp.SELECT_ALL.accelerator(os));
			waitFor(() -> ed.getCircuit().getElements().stream()
					.anyMatch(Element::isHighlighted),
					"Ctrl+A from the tool-bar selected the circuit");

			// Delete through the window's focus owner removes the selection
			pressThroughFocusOwner(EditOp.DELETE.accelerator(os));
			waitFor(() -> ed.getCircuit().getElements().stream()
					.noneMatch(el -> el.getClass().getSimpleName()
							.endsWith("Gate")),
					"Delete from the tool-bar removed the selected gates");
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
	 * With focus on a tool-bar palette button, Ctrl/Cmd+S saves the visible
	 * circuit - the File menu's window-scoped accelerator fires from a
	 * non-canvas focus owner, the literal #75 P1 prediction ("Ctrl/Cmd+S
	 * saves from any focus location"). The circuit is pointed at a temp
	 * directory and marked dirty; firing Save clears the dirty flag and
	 * writes the .jls file. A regression that dropped the File accelerator or
	 * scoped it to the canvas turns this red. Save writes directly (no file
	 * chooser) once the circuit has a directory + name, so this stays a
	 * deterministic, dialog-free check.
	 *
	 * @throws Exception on reflection, I/O, or EDT failure.
	 */
	@Test
	void fileSaveAcceleratorSavesFromANonCanvasFocusOwner() throws Exception {
		Frame savedFrame = JLSInfo.frame;
		Simulator savedSim = JLSInfo.sim;
		String os = System.getProperty("os.name");
		Path dir = Files.createTempDirectory("jls-save-test");
		File saved = new File(dir.toFile(), "savetest.jls");
		try {
			JLSStart jls = bootMainWindow();
			Circuit circuit = twoGates();
			circuit.setName("savetest");
			circuit.setDirectory(dir.toString());
			AtomicReference<Editor> edRef = new AtomicReference<>();
			SwingUtilities.invokeAndWait(() -> {
				jls.setupEditor(circuit, "savetest");
				edRef.set(jls.getVisibleEditor());
			});
			Editor ed = edRef.get();
			assertNotNull(ed, "setupEditor made an editor visible");

			// dirty the circuit so the save has an observable effect to clear
			SwingUtilities.invokeAndWait(circuit::markChanged);
			assertTrue(ed.getCircuit().hasChanged(),
					"the circuit starts dirty (unsaved changes)");

			// focus a NON-canvas component: a tool-bar palette button
			JButton palette = findToolbarButton(ed, "AND gate");
			assertNotNull(palette, "the visible editor has an AND gate button");
			giveFocusTo(palette);
			assertTrue(focusOwner() instanceof JButton,
					"focus is on the tool-bar button, not the canvas");

			// Ctrl/Cmd+S (File > Save) through the window's focus owner
			pressThroughFocusOwner(MenuAcceleratorPolicy.save(os));
			waitFor(() -> !ed.getCircuit().hasChanged(),
					"Ctrl+S from the tool-bar saved the circuit (dirty flag "
							+ "cleared)");
			waitFor(saved::isFile,
					"Ctrl+S from the tool-bar wrote the .jls file to disk");
		}
		finally {
			SwingUtilities.invokeAndWait(() -> {
				for (Window w : Window.getWindows()) {
					w.dispose();
				}
			});
			JLSInfo.frame = savedFrame;
			JLSInfo.sim = savedSim;
			Files.deleteIfExists(saved.toPath());
			Files.deleteIfExists(dir);
		}
	}

	/** A circuit with two unattached AND gates. */
	private static Circuit twoGates() throws Exception {
		CircuitTextBuilder cb = new CircuitTextBuilder();
		cb.gate("AndGate", 1, 2);
		cb.gate("AndGate", 1, 2);
		Circuit circuit = new Circuit("accel");
		assertTrue(circuit.load(new Scanner(cb.build())),
				() -> "load: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
				() -> "finishLoad: " + JLSInfo.loadError);
		return circuit;
	}

	/** The live keyboard-focus owner, read on the EDT. */
	private static Component focusOwner() throws Exception {
		AtomicReference<Component> owner = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> owner.set(KeyboardFocusManager
				.getCurrentKeyboardFocusManager().getFocusOwner()));
		return owner.get();
	}

	/** Move focus to the target and poll the KFM until it lands. */
	private static void giveFocusTo(Component target) throws Exception {
		SwingUtilities.invokeAndWait(target::requestFocusInWindow);
		waitFor(() -> {
			try {
				return focusOwner() == target;
			} catch (Exception e) {
				throw new AssertionError(e);
			}
		}, "focus to move to the palette button");
	}

	/**
	 * Dispatch the accelerator keystroke as a KEY_PRESSED to the live focus
	 * owner on the EDT. Dispatching to the focus owner runs its
	 * {@code processKeyBindings}, which walks up to the window and fires the
	 * {@code WHEN_IN_FOCUSED_WINDOW} menu accelerator - exactly the path a
	 * real keypress takes.
	 */
	private static void pressThroughFocusOwner(KeyStroke ks) throws Exception {
		Component owner = focusOwner();
		if (owner == null) {
			throw new AssertionError("no focus owner to route the accelerator "
					+ "to; a real keystroke would have nowhere to go either");
		}
		KeyEvent e = new KeyEvent(owner, KeyEvent.KEY_PRESSED,
				System.currentTimeMillis(), ks.getModifiers(), ks.getKeyCode(),
				KeyEvent.CHAR_UNDEFINED);
		SwingUtilities.invokeAndWait(() -> owner.dispatchEvent(e));
	}

	/** Poll a condition to a ~10s bound (loaded-CI tolerant). */
	private static void waitFor(java.util.function.BooleanSupplier cond,
			String what) {
		for (int i = 0; i < 400; i++) {
			if (cond.getAsBoolean()) {
				return;
			}
			try {
				Thread.sleep(25);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				fail("interrupted waiting for " + what);
			}
		}
		fail("timed out waiting for " + what);
	}

	/** The first JButton with the given tooltip under root (the palette). */
	private static JButton findToolbarButton(Container root, String tooltip) {
		for (Component c : root.getComponents()) {
			if (c instanceof JButton button
					&& tooltip.equals(button.getToolTipText())) {
				return button;
			}
			if (c instanceof Container inner) {
				JButton found = findToolbarButton(inner, tooltip);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	}

	/**
	 * Construct the real JLS main window on the EDT, installing the static
	 * exception handler the constructor needs (mirrors {@link MenuBarSpecTest}
	 * and {@link EditActionMatrixTest}).
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
