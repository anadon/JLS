package jls;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.swing.JTextField;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the numeric-field document filter (issue #159): the
 * last untested headless-tractable class in the jls core package.
 * Every edit is driven through a real JTextField's document, exactly
 * as Swing routes user edits - setText goes through
 * AbstractDocument.replace and thus the installed filter, direct
 * document inserts go through insertString, deletions through
 * remove - so these tests pin the filter's observable contract:
 * inserts are blocked, removals pass through, and replacements land
 * only when the resulting text is numeric in the configured base and
 * within the configured maximum.
 *
 * Also pins the issue #51 regression: setMax takes a VALUE, not
 * digits. The old code re-parsed the max's decimal rendering in the
 * field's base, so a hex field's setMax(255) meant 0x255 (597) and a
 * binary field's setMax(5) threw NumberFormatException on the EDT.
 */
class TextFilterTest {

	private JTextField field;
	private TextFilter filter;

	@BeforeEach
	void installFilter() {

		field = new JTextField();
		filter = new TextFilter(field);
		((AbstractDocument)field.getDocument()).setDocumentFilter(filter);
	}

	/** Replace directly through the document, as a caret edit would. */
	private void replace(int offset, int length, String str)
			throws BadLocationException {

		((AbstractDocument)field.getDocument()).replace(offset, length,
				str, null);
	}

	// ------------------------------------------------------------------
	// insertString: blocked; remove: pass-through
	// ------------------------------------------------------------------

	@Test
	void directInsertsAreBlocked() throws BadLocationException {

		field.setText("42");
		field.getDocument().insertString(1, "9", null);
		assertEquals("42", field.getText(),
				"insertString must be a no-op; edits arrive via replace");
	}

	@Test
	void removalsPassThroughEvenWhenTheRemainderIsNotNumeric()
			throws BadLocationException {

		// paste "-5" (BigInteger-valid), then delete the digit: the
		// lone "-" is not numeric, but deletions can never grow the
		// value, so the filter lets them through unchanged
		field.setText("-5");
		assertEquals("-5", field.getText());
		field.getDocument().remove(1, 1);
		assertEquals("-", field.getText());
	}

	// ------------------------------------------------------------------
	// replace, insertion path (length == 0)
	// ------------------------------------------------------------------

	@Test
	void numericInsertionIntoAnEmptyFieldIsAccepted() {

		field.setText("123");
		assertEquals("123", field.getText());
	}

	@Test
	void nonNumericInsertionIsIgnored() {

		field.setText("12a");
		assertEquals("", field.getText(),
				"non-numeric text must never enter the field");
	}

	@Test
	void insertionMayReachButNotExceedTheMaximum()
			throws BadLocationException {

		filter.setMax(100);
		field.setText("10");
		replace(2, 0, "0");
		assertEquals("100", field.getText(),
				"a value equal to the maximum is allowed");
		replace(3, 0, "0");
		assertEquals("100", field.getText(),
				"a keystroke that would exceed the maximum is ignored");
	}

	@Test
	void insertionInTheMiddleChecksTheResultingValue()
			throws BadLocationException {

		filter.setMax(100);
		field.setText("10");
		replace(1, 0, "5");
		assertEquals("10", field.getText(),
				"150 exceeds the maximum even though the typed digit "
						+ "is small");
	}

	// ------------------------------------------------------------------
	// replace, replacement path (length > 0)
	// ------------------------------------------------------------------

	@Test
	void settingTextOverExistingContentNormalizesTheValue() {

		field.setText("42");
		field.setText("007");
		assertEquals("7", field.getText(),
				"a full replacement re-renders through BigInteger");
	}

	@Test
	void settingEmptyTextCannotClearTheField() {

		// only remove() can empty the field (see the pass-through
		// test); an empty replacement is not numeric and is ignored
		field.setText("42");
		field.setText("");
		assertEquals("42", field.getText());
	}

	@Test
	void nonNumericReplacementKeepsTheOldContent() {

		field.setText("42");
		field.setText("4x2");
		assertEquals("42", field.getText());
	}

	@Test
	void replacementBeyondTheMaximumIsIgnored() {

		filter.setMax(100);
		field.setText("99");
		field.setText("101");
		assertEquals("99", field.getText());
	}

	@Test
	void withoutAMaximumArbitrarilyLargeValuesAreAccepted() {

		// the guard for the fields that feed Integer.parseInt lives in
		// NumericField, not here (see InteractiveSimulatorFieldTest)
		field.setText("99999999999999999999");
		assertEquals("99999999999999999999", field.getText());
	}

	// ------------------------------------------------------------------
	// bases 2 and 16, and the issue #51 setMax regression
	// ------------------------------------------------------------------

	@Test
	void base16AcceptsHexDigitsAndNormalizesCase() {

		filter.setBase(16);
		field.setText("ff");
		assertEquals("ff", field.getText());
		field.setText("FF");
		assertEquals("ff", field.getText(),
				"replacement re-renders via toString(16), lowercase");
		field.setText("fg");
		assertEquals("ff", field.getText(),
				"g is not a hex digit");
	}

	@Test
	void hexMaximumIsAValueNotDigits() {

		// issue #51: setMax(255) once meant "255 read in base 16",
		// i.e. 597 - which admitted 0x100 (256) into a byte field
		filter.setBase(16);
		filter.setMax(255);
		field.setText("ff");
		assertEquals("ff", field.getText(),
				"0xff is exactly the maximum");
		field.setText("100");
		assertEquals("ff", field.getText(),
				"0x100 (256) exceeds a maximum of 255");
	}

	@Test
	void binaryMaximumWithNonBinaryDigitsDoesNotThrow() {

		// issue #51: setMax(5) once parsed "5" in base 2 and threw
		// NumberFormatException on the EDT
		filter.setBase(2);
		assertDoesNotThrow(() -> filter.setMax(5));
		field.setText("101");
		assertEquals("101", field.getText(),
				"binary 101 is exactly the maximum");
		field.setText("110");
		assertEquals("101", field.getText(),
				"binary 110 (6) exceeds a maximum of 5");
		field.setText("102");
		assertEquals("101", field.getText(),
				"2 is not a binary digit");
	}
}
