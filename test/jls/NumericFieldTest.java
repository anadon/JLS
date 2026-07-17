package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import javax.swing.JTextField;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the validated numeric-field reader (issue #119): the
 * shared replacement for the interactive simulator's raw
 * Integer.parseInt sites. Pins P3 of the issue: valid input behaves
 * exactly as the old sites did (empty means minimum, Math.max clamping
 * included), invalid input reports an error and leaves the previous
 * value in effect, and the field text is normalized either way.
 *
 * The fields here carry no TextFilter on purpose - the helper's
 * contract is to never throw regardless of what the filter let through
 * (BigInteger-validated text can still overflow an int; see
 * InteractiveSimulatorFieldTest for the filter-permeability proof).
 */
class NumericFieldTest {

	/** Run parse while capturing stderr (headless TellUser output). */
	private static String stderrOf(Runnable action) {

		PrintStream saved = System.err;
		ByteArrayOutputStream captured = new ByteArrayOutputStream();
		try {
			System.setErr(new PrintStream(captured, true,
					StandardCharsets.UTF_8));
			action.run();
		}
		finally {
			System.setErr(saved);
		}
		return captured.toString(StandardCharsets.UTF_8);
	}

	@Test
	void validTextParsesAndNormalizesTheField() {

		JTextField field = new JTextField("42");
		int value = NumericField.parse(null, field, 1, 7, "Scale factor");
		assertEquals(42, value);
		assertEquals("42", field.getText());
	}

	@Test
	void valuesBelowTheMinimumClampSilently() {

		JTextField field = new JTextField("0");
		String err = stderrOf(() -> assertEquals(1,
				NumericField.parse(null, field, 1, 7, "Scale factor")));
		assertEquals("1", field.getText(),
				"clamped value must be written back to the field");
		assertEquals("", err, "clamping is not an error");
	}

	@Test
	void emptyTextMeansTheMinimum() {

		JTextField field = new JTextField("");
		String err = stderrOf(() -> assertEquals(1,
				NumericField.parse(null, field, 1, 7, "Scale factor")));
		assertEquals("1", field.getText());
		assertEquals("", err, "empty text is not an error");
	}

	@Test
	void unparsableTextKeepsThePreviousValueAndReports() {

		// numeric by BigInteger's lights (what TextFilter admits), but
		// not an int - the exact text that used to throw off the EDT
		JTextField field = new JTextField("99999999999999999999");
		String err = stderrOf(() -> assertEquals(7,
				NumericField.parse(null, field, 1, 7, "Scale factor")));
		assertEquals("7", field.getText(),
				"the previous value must be restored into the field");
		assertTrue(err.contains("jls: error:"),
				"headless report must use the CLI error format, got: "
						+ err);
		assertTrue(err.contains("Scale factor"),
				"the message must name the field in domain terms, got: "
						+ err);
	}

	@Test
	void lonelyMinusSignKeepsThePreviousValueAndReports() {

		// reachable in the real fields: paste "-5" (a valid BigInteger),
		// then delete the digit - TextFilter's remove() passes through
		JTextField field = new JTextField("-");
		String err = stderrOf(() -> assertEquals(3,
				NumericField.parse(null, field, 1, 3, "Time limit")));
		assertEquals("3", field.getText());
		assertTrue(err.contains("jls: error:"), "got: " + err);
	}

	@Test
	void negativeValuesClampToTheMinimumWithoutError() {

		JTextField field = new JTextField("-5");
		String err = stderrOf(() -> assertEquals(1,
				NumericField.parse(null, field, 1, 7, "Step amount")));
		assertEquals("1", field.getText());
		assertEquals("", err, "a parsable negative clamps, not errors");
	}
}
