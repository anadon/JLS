package jls.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.text.BadLocationException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jls.JLSInfo;

/**
 * Regression tests for issue #119: the interactive simulator's control
 * fields (time scale, step amount, time limit) fed Integer.parseInt
 * directly from their action listeners. The fields do carry a
 * TextFilter, but the filter validates with BigInteger, so text that
 * is numeric-but-not-an-int still reaches parseInt: twenty typed
 * digits overflow the unbounded scale/step fields, and the time-limit
 * field (max = Integer.MAX_VALUE) admits a pasted huge negative. Each
 * test drives the real component - text set through the installed
 * DocumentFilter, listener fired exactly as the button would - so a
 * raw parseInt at any of the 8 sites fails these tests with the
 * original NumberFormatException.
 *
 * Everything runs headless: the simulator panel is plain Swing
 * components (no Window is ever realized), and TellUser reports to
 * stderr in headless mode, which the tests capture.
 */
class InteractiveSimulatorFieldTest {

	private boolean savedBatch;
	private InteractiveSimulator simulator;
	private JTextField scaleField;   // 10 columns
	private JTextField stepField;    // 6 columns
	private JTextField timeLimit;    // 7 columns

	@BeforeEach
	void buildSimulatorPanel() {

		// the constructor only builds its GUI outside batch mode
		savedBatch = JLSInfo.batch;
		JLSInfo.batch = false;
		simulator = new InteractiveSimulator();
		scaleField = fieldWithColumns(10);
		stepField = fieldWithColumns(6);
		timeLimit = fieldWithColumns(7);
	}

	@AfterEach
	void restoreBatchFlag() {

		JLSInfo.batch = savedBatch;
	}

	// ------------------------------------------------------------------
	// component plumbing
	// ------------------------------------------------------------------

	/** The unique text field with the given column count. */
	private JTextField fieldWithColumns(int columns) {

		JTextField found = find(simulator.getWindow(), JTextField.class,
				f -> f.getColumns() == columns);
		assertNotNull(found, columns + "-column field not found");
		return found;
	}

	/** The unique button with the given (trimmed) label. */
	private JButton button(String label) {

		JButton found = find(simulator.getWindow(), JButton.class,
				b -> label.equals(b.getText().trim()));
		assertNotNull(found, "button '" + label + "' not found");
		return found;
	}

	private static <T extends Component> T find(Container root,
			Class<T> type, java.util.function.Predicate<T> matches) {

		for (Component child : root.getComponents()) {
			if (type.isInstance(child) && matches.test(type.cast(child)))
				return type.cast(child);
			if (child instanceof Container) {
				T found = find((Container)child, type, matches);
				if (found != null)
					return found;
			}
		}
		return null;
	}

	/** Fire a button's listeners as a click on it would. */
	private static String fire(JButton button) {

		PrintStream saved = System.err;
		ByteArrayOutputStream captured = new ByteArrayOutputStream();
		try {
			System.setErr(new PrintStream(captured, true,
					StandardCharsets.UTF_8));
			for (ActionListener listener : button.getActionListeners()) {
				listener.actionPerformed(new ActionEvent(button,
						ActionEvent.ACTION_PERFORMED, button.getText()));
			}
		}
		finally {
			System.setErr(saved);
		}
		return captured.toString(StandardCharsets.UTF_8);
	}

	// ------------------------------------------------------------------
	// filter permeability: the H1 check (issue #119 §4)
	// ------------------------------------------------------------------

	@Test
	void textFilterAdmitsIntOverflowingDigits() {

		// setText routes through AbstractDocument.replace and thus the
		// installed TextFilter, exactly like a paste over a selection;
		// BigInteger accepts what parseInt cannot
		scaleField.setText("99999999999999999999");
		assertEquals("99999999999999999999", scaleField.getText(),
				"TextFilter admits BigInteger-valid text beyond int "
						+ "range, so the listeners must guard parseInt");
	}

	@Test
	void timeLimitFilterAdmitsHugeNegativePaste() {

		// the tlimit filter caps values at Integer.MAX_VALUE, but the
		// cap is an upper bound only - a pasted huge negative passes
		timeLimit.setText("-99999999999999");
		assertEquals("-99999999999999", timeLimit.getText());
	}

	// ------------------------------------------------------------------
	// the 8 unguarded sites, by field (issue #119 §5 P1-P3)
	// ------------------------------------------------------------------

	@Test
	void scaleApplyRejectsOverflowAndKeepsPreviousValue() {

		// establish a previous value through the real listener
		scaleField.setText("7");
		assertEquals("", fire(button("Apply")));
		assertEquals("7", scaleField.getText());

		// overflow past int range: pre-#119 this threw
		// NumberFormatException out of actionPerformed
		scaleField.setText("99999999999999999999");
		String err = fire(button("Apply"));
		assertEquals("7", scaleField.getText(),
				"invalid input must leave the previous value in effect");
		assertTrue(err.contains("jls: error:"),
				"the rejection must be reported, got: " + err);
		assertTrue(err.contains("Scale factor"), "got: " + err);
	}

	@Test
	void scaleApplyStillClampsValidInputToOne() {

		scaleField.setText("0");
		String err = fire(button("Apply"));
		assertEquals("1", scaleField.getText(),
				"the pre-existing Math.max(1,...) clamp must survive");
		assertEquals("", err, "clamping is not an error");
	}

	@Test
	void scaleApplyTreatsClearedFieldAsOne() throws BadLocationException {

		// setText("") cannot empty the field (the filter rejects the
		// empty replacement), but backspacing can: remove() passes
		// through - so drive the document the way a user would
		scaleField.setText("7");
		fire(button("Apply"));
		scaleField.getDocument().remove(0,
				scaleField.getDocument().getLength());
		assertEquals("", scaleField.getText());
		String err = fire(button("Apply"));
		assertEquals("1", scaleField.getText(),
				"empty text must mean the minimum, as before");
		assertEquals("", err);
	}

	@Test
	void scaleApplySurvivesLonelyMinusSign() throws BadLocationException {

		// paste "-5" (BigInteger-valid, clamps to 1 if applied), then
		// delete the digit: remove() passes through the filter and
		// leaves "-", which parseInt cannot take
		scaleField.setText("-5");
		assertEquals("-5", scaleField.getText());
		scaleField.getDocument().remove(1, 1);
		assertEquals("-", scaleField.getText());
		String err = fire(button("Apply"));
		assertEquals("1", scaleField.getText(),
				"previous value (1) must stay in effect");
		assertTrue(err.contains("jls: error:"), "got: " + err);
	}

	@Test
	void stepButtonRejectsOverflowingStepAmount() {

		// the Step listener parses both scale and step; with no circuit
		// loaded runSim() returns immediately, so this is safe headless
		stepField.setText("99999999999999999999");
		String err = fire(button("Step"));
		assertEquals("1", stepField.getText(),
				"invalid step must fall back to the previous value (1)");
		assertTrue(err.contains("jls: error:"),
				"the rejection must be reported, got: " + err);
		assertTrue(err.contains("Step amount"), "got: " + err);
	}

	@Test
	void setMaxTimeRejectsHugeNegativeAndKeepsPreviousLimit() {

		long before = simulator.maxTime;
		timeLimit.setText("-99999999999999");

		PrintStream saved = System.err;
		ByteArrayOutputStream captured = new ByteArrayOutputStream();
		try {
			System.setErr(new PrintStream(captured, true,
					StandardCharsets.UTF_8));
			simulator.setMaxTime();
		}
		finally {
			System.setErr(saved);
		}
		String err = captured.toString(StandardCharsets.UTF_8);

		assertEquals(before, simulator.maxTime,
				"invalid input must leave the previous limit in effect");
		assertEquals(before + "", timeLimit.getText());
		assertTrue(err.contains("jls: error:"),
				"the rejection must be reported, got: " + err);
		assertTrue(err.contains("Time limit"), "got: " + err);
	}

	@Test
	void setMaxTimeStillAcceptsValidLimits() {

		timeLimit.setText("500");
		simulator.setMaxTime();
		assertEquals(500L, simulator.maxTime);
		assertEquals("500", timeLimit.getText());
	}
}
