package jls.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import javax.swing.AbstractButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import jls.Circuit;
import jls.CircuitTextBuilder;
import jls.JLSInfo;
import jls.sim.InteractiveSimulator;

/**
 * Layer-2 interactive-simulator smoke (issue #162, prediction P3):
 * load a fixture, then drive Step, Step again, and Stop through the
 * real toolbar buttons on the EDT, exactly as a student would. This is
 * the first test on the {@code "Runner"}-thread handshake - the
 * volatile pause/step flags and semaphore between the EDT and the sim
 * thread (the issue #49 H7/H8 discipline, until now enforced only by
 * review) - and on the trace window's live setup/draw path.
 *
 * The choreography is deterministic, not timing-based: a Step with the
 * next event beyond the step target always parks the Runner thread on
 * the pause semaphore with the clock at exactly the step boundary, so
 * the test can assert "Time: 1" then "Time: 2" and finally that Stop
 * wakes the parked thread into its EDT epilogue and lets it exit.
 *
 * Runs under the #162 display substrate ({@code display} tag): the
 * trace window's setup path reads real FontMetrics from a displayable
 * component. {@link EdtViolationDetector} is installed for the whole
 * run, so any off-EDT Swing access on the sim thread fails the test
 * with the offending stack (prediction P2 applied to this path).
 */
@Tag("display")
class InteractiveSimulatorSmokeTest {

	/** Poll ceiling for each waited-for UI state, in milliseconds. */
	private static final long WAIT_MILLIS = 15_000;

	private EdtViolationDetector detector;
	private boolean batchBefore;

	@BeforeEach
	void requireDisplay() {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
				"display suite: skipped in headless runs");
		batchBefore = JLSInfo.batch;
		JLSInfo.batch = false; // the GUI path is the subject under test
		detector = EdtViolationDetector.install();
	}

	@AfterEach
	void disposeWindowsAndCheckEdt() throws Exception {
		JLSInfo.batch = batchBefore;
		SwingUtilities.invokeAndWait(() -> {
			for (Window w : Window.getWindows()) {
				w.dispose();
			}
		});
		if (detector != null) {
			detector.uninstall();
			detector.assertClean("interactive-simulator smoke (#162 P3)");
		}
	}

	/**
	 * The fixture: a free-running clock driving a watched output pin,
	 * so the event queue never drains and every run pauses exactly at
	 * its step target.
	 *
	 * @return the circuit save-format text.
	 */
	private static String fixture() {
		CircuitTextBuilder cb = new CircuitTextBuilder();
		int clk = cb.clock(20, 10);
		int z = cb.outputPin("z", 1);
		cb.wire(clk, "output", z, "input");
		return cb.build();
	} // end of fixture method

	/**
	 * Load the fixture through the real loader.
	 *
	 * @return the loaded circuit.
	 * @throws Exception If the loader fails unexpectedly.
	 */
	private static Circuit load() throws Exception {
		Circuit circuit = new Circuit("golden");
		assertTrue(circuit.load(new Scanner(fixture())),
				"fixture load failed");
		assertTrue(circuit.finishLoad(null), "fixture finishLoad failed");
		return circuit;
	} // end of load method

	/**
	 * Drive load, Step, Step, Stop end to end and assert the clock,
	 * message, trace-window, and Runner-thread state at each stage.
	 *
	 * @throws Exception If EDT dispatch fails.
	 */
	@Test
	void stepStepStopDrivesTheRunnerThreadHandshake() throws Exception {
		Circuit circuit = load();
		AtomicReference<JFrame> frameRef = new AtomicReference<>();
		SwingUtilities.invokeAndWait(() -> {
			InteractiveSimulator sim = new InteractiveSimulator();
			sim.setCircuit(circuit);
			JFrame frame = new JFrame("sim-smoke");
			frame.add(sim.getWindow());
			frame.setSize(1200, 500);
			frame.setLocation(0, 0);
			frame.setVisible(true);
			frameRef.set(frame);
		});
		JFrame frame = frameRef.get();

		// Step: the Runner thread starts, runs to the step target, and
		// parks on the pause semaphore with the clock at exactly 1
		clickButton(frame, "Step");
		waitForLabel(frame, "Simulation Paused");
		waitForLabel(frame, "Time: 1");
		assertTrue(runnerThreadAlive(),
				"the Runner thread should stay parked while paused");

		// the trace window really populated: header plus the watched
		// pin's trace panel
		AtomicInteger traceCount = new AtomicInteger();
		SwingUtilities.invokeAndWait(() -> traceCount.set(
				countComponents(frame, InteractiveSimulator.Traces.class)));
		assertTrue(traceCount.get() >= 1,
				"the trace window should be in the component tree");

		// Step again: the parked thread wakes, advances one more unit,
		// and parks again
		clickButton(frame, "Step");
		waitForLabel(frame, "Time: 2");
		waitForLabel(frame, "Simulation Paused");
		assertTrue(runnerThreadAlive(),
				"the Runner thread should park again after the second step");

		// Stop: wakes the parked thread into the EDT epilogue and exit
		clickButton(frame, "Stop");
		waitForLabel(frame, "Simulation Stopped");
		waitFor(() -> !runnerThreadAlive(),
				"the Runner thread to exit after Stop");

		// the toolbar is back to the fresh-run button set
		waitFor(() -> buttonShowing(frame, "Step"),
				"the Step button to return after the run ends");
	} // end of stepStepStopDrivesTheRunnerThreadHandshake method

	// ------------------------------------------------------------------
	// UI lookup and waiting helpers
	// ------------------------------------------------------------------

	/**
	 * Click the showing button with the given (trimmed) text, on the
	 * EDT, failing if it is not there.
	 *
	 * @param root Where to search.
	 * @param text The button text, trimmed.
	 * @throws Exception If EDT dispatch fails.
	 */
	private static void clickButton(Container root, String text)
			throws Exception {
		waitFor(() -> buttonShowing(root, text),
				"button '" + text + "' to be showing");
		SwingUtilities.invokeAndWait(() -> {
			AbstractButton button = findButton(root, text);
			if (button == null) {
				throw new AssertionError("button vanished: " + text);
			}
			button.doClick();
		});
	} // end of clickButton method

	/**
	 * Whether a showing button with the given trimmed text exists,
	 * checked on the EDT.
	 *
	 * @param root Where to search.
	 * @param text The button text, trimmed.
	 * @return true if the button is showing.
	 */
	private static boolean buttonShowing(Container root, String text) {
		AtomicReference<Boolean> found = new AtomicReference<>(false);
		try {
			SwingUtilities.invokeAndWait(
					() -> found.set(findButton(root, text) != null));
		} catch (Exception e) {
			throw new AssertionError("EDT lookup failed", e);
		}
		return found.get();
	} // end of buttonShowing method

	/**
	 * Poll until a label with exactly the given text is present.
	 *
	 * @param root Where to search.
	 * @param text The label text to wait for.
	 */
	private static void waitForLabel(Container root, String text) {
		waitFor(() -> {
			AtomicReference<Boolean> found = new AtomicReference<>(false);
			try {
				SwingUtilities.invokeAndWait(
						() -> found.set(findLabel(root, text) != null));
			} catch (Exception e) {
				throw new AssertionError("EDT lookup failed", e);
			}
			return found.get();
		}, "label '" + text + "'");
	} // end of waitForLabel method

	/**
	 * Poll a condition until it holds or the wait ceiling passes.
	 *
	 * @param condition The condition to wait for.
	 * @param what Description for the timeout failure.
	 */
	private static void waitFor(BooleanSupplier condition, String what) {
		long deadline = System.currentTimeMillis() + WAIT_MILLIS;
		while (System.currentTimeMillis() < deadline) {
			if (condition.getAsBoolean()) {
				return;
			}
			try {
				Thread.sleep(25);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		throw new AssertionError("timed out waiting for " + what);
	} // end of waitFor method

	/**
	 * Whether a live thread named "Runner" (the sim thread) exists.
	 *
	 * @return true if the Runner thread is alive.
	 */
	private static boolean runnerThreadAlive() {
		for (Thread t : Thread.getAllStackTraces().keySet()) {
			if ("Runner".equals(t.getName()) && t.isAlive()) {
				return true;
			}
		}
		return false;
	} // end of runnerThreadAlive method

	/**
	 * Find a showing button by trimmed text.
	 *
	 * @param root Where to search.
	 * @param text The trimmed text to match.
	 * @return the button, or null.
	 */
	private static AbstractButton findButton(Container root, String text) {
		for (Component c : root.getComponents()) {
			if (c instanceof AbstractButton button && button.isShowing()
					&& button.getText() != null
					&& text.equals(button.getText().trim())) {
				return button;
			}
			if (c instanceof Container inner) {
				AbstractButton found = findButton(inner, text);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	} // end of findButton method

	/**
	 * Find a label by exact text.
	 *
	 * @param root Where to search.
	 * @param text The text to match.
	 * @return the label, or null.
	 */
	private static JLabel findLabel(Container root, String text) {
		for (Component c : root.getComponents()) {
			if (c instanceof JLabel label && text.equals(label.getText())) {
				return label;
			}
			if (c instanceof Container inner) {
				JLabel found = findLabel(inner, text);
				if (found != null) {
					return found;
				}
			}
		}
		return null;
	} // end of findLabel method

	/**
	 * Count components of a type in a tree.
	 *
	 * @param root Where to search.
	 * @param type The component class to count.
	 * @return how many were found.
	 */
	private static int countComponents(Container root, Class<?> type) {
		int count = 0;
		for (Component c : root.getComponents()) {
			if (type.isInstance(c)) {
				count += 1;
			}
			if (c instanceof Container inner) {
				count += countComponents(inner, type);
			}
		}
		return count;
	} // end of countComponents method

} // end of InteractiveSimulatorSmokeTest class
