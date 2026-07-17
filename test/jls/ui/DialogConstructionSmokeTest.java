package jls.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import jls.Circuit;
import jls.elem.Element;

/**
 * Layer-2 dialog-construction smoke (issue #162): every element
 * create/edit dialog is constructed for real - the {@code setup()}
 * entry the editor's palette click runs - under a display, and
 * dismissed through the close-box path (WINDOW_CLOSING, which
 * ElementDialog wires to cancel). This exercises the full dialog
 * build: form layout, field defaults, key bindings, help wiring.
 *
 * These tests are tagged {@code display} and run in a separate
 * surefire execution with {@code java.awt.headless} controlled by the
 * {@code jls.test.headless} property: plain builds keep it true and
 * this suite self-skips; the CI leg (and local runs) execute it under
 * {@code xvfb-run -a mvn -B verify -Djls.test.headless=false}. The
 * suite must never run with the shared headless execution - with a
 * display present, TellUser becomes interactive and a stray warning
 * in an unrelated test would block on a modal dialog.
 *
 * A watcher thread dispatches WINDOW_CLOSING to every visible dialog
 * for the whole test, so a modal dialog opened on the EDT (which
 * pumps its own event loop) always comes back down; it also swats
 * any stray JOptionPane a dialog might raise on the way out.
 */
@Tag("display")
class DialogConstructionSmokeTest {

	/** Per-dialog time allowance before the run is declared stuck. */
	private static final long DIALOG_TIMEOUT_SECONDS = 30;

	private volatile boolean closing;
	private Thread closer;

	@BeforeEach
	void requireDisplayAndStartCloser() {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
				"display suite: skipped in headless runs");
		closing = true;
		closer = new Thread(() -> {
			while (closing) {
				SwingUtilities.invokeLater(() -> {
					for (Window w : Window.getWindows()) {
						if (w.isVisible() && w instanceof JDialog) {
							w.dispatchEvent(new WindowEvent(w,
									WindowEvent.WINDOW_CLOSING));
						}
					}
				});
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					return;
				}
			}
		}, "dialog-closer");
		closer.setDaemon(true);
		closer.start();
	}

	@AfterEach
	void stopCloserAndDisposeStrays() throws Exception {
		closing = false;
		if (closer != null) {
			closer.join(1000);
		}
		SwingUtilities.invokeAndWait(() -> {
			for (Window w : Window.getWindows()) {
				w.dispose();
			}
		});
	}

	/**
	 * Instantiate the element and run its setup() - the create-dialog
	 * entry point - on the EDT, as the editor does. The watcher cancels
	 * the dialog; setup returning (either verdict) is the assertion.
	 */
	private void constructAndCancel(String elementClass) throws Exception {
		Circuit circuit = new Circuit("golden");
		Element el = (Element) Class.forName("jls.elem." + elementClass)
				.getConstructor(Circuit.class).newInstance(circuit);

		BufferedImage img = new BufferedImage(400, 300,
				BufferedImage.TYPE_INT_RGB);
		CountDownLatch done = new CountDownLatch(1);
		Throwable[] thrown = new Throwable[1];
		SwingUtilities.invokeLater(() -> {
			try {
				el.setup(img.createGraphics(), new JPanel(), 100, 100);
			} catch (Throwable t) {
				thrown[0] = t;
			} finally {
				done.countDown();
			}
		});
		assertTrue(done.await(DIALOG_TIMEOUT_SECONDS, TimeUnit.SECONDS),
				elementClass + ": dialog did not come back down - stuck"
						+ " modal window");
		if (thrown[0] != null) {
			fail(elementClass + ": setup threw", thrown[0]);
		}
	}

	// One test per dialog family, so a failure names its element and
	// the rest still run. Families sharing one dialog class are
	// represented once (AndGate stands in for the Gate dialog used by
	// all five two-input gate kinds and NotGate).

	@Test
	void gateDialog() throws Exception {
		constructAndCancel("AndGate");
	}

	@Test
	void delayGateDialog() throws Exception {
		constructAndCancel("DelayGate");
	}

	@Test
	void extendDialog() throws Exception {
		constructAndCancel("Extend");
	}

	@Test
	void adderDialog() throws Exception {
		constructAndCancel("Adder");
	}

	@Test
	void clockDialog() throws Exception {
		constructAndCancel("Clock");
	}

	@Test
	void constantDialog() throws Exception {
		constructAndCancel("Constant");
	}

	@Test
	void decoderDialog() throws Exception {
		constructAndCancel("Decoder");
	}

	@Test
	void displayDialog() throws Exception {
		constructAndCancel("Display");
	}

	@Test
	void memoryDialog() throws Exception {
		constructAndCancel("Memory");
	}

	@Test
	void muxDialog() throws Exception {
		constructAndCancel("Mux");
	}

	@Test
	void registerDialog() throws Exception {
		constructAndCancel("Register");
	}

	@Test
	void shiftRegisterDialog() throws Exception {
		constructAndCancel("ShiftRegister");
	}

	@Test
	void sigGenDialog() throws Exception {
		constructAndCancel("SigGen");
	}

	@Test
	void stateMachineEditor() throws Exception {
		// the big one: setup opens the full StateEditor window
		constructAndCancel("StateMachine");
	}

	@Test
	void textDialog() throws Exception {
		constructAndCancel("Text");
	}

	@Test
	void triStateDialog() throws Exception {
		constructAndCancel("TriState");
	}

	@Test
	void truthTableEditor() throws Exception {
		constructAndCancel("TruthTable");
	}

	@Test
	void jumpStartDialog() throws Exception {
		constructAndCancel("JumpStart");
	}

	@Test
	void jumpEndDialog() throws Exception {
		constructAndCancel("JumpEnd");
	}

	@Test
	void binderRangesDialog() throws Exception {
		constructAndCancel("Binder");
	}

	@Test
	void splitterRangesDialog() throws Exception {
		constructAndCancel("Splitter");
	}

	@Test
	void inputPinDialog() throws Exception {
		constructAndCancel("InputPin");
	}

	@Test
	void outputPinDialog() throws Exception {
		constructAndCancel("OutputPin");
	}

	@Test
	void subCircuitCreateDialog() throws Exception {
		// cancelled at the SubCreate dialog, before any file chooser
		constructAndCancel("SubCircuit");
	}
}
