package jls;

import java.awt.Component;

import javax.swing.JTextField;

/**
 * Validated reading of numeric text fields (issue #119).
 *
 * The interactive simulator's control fields (time scale, step amount,
 * time limit) fed Integer.parseInt directly from their action
 * listeners. TextFilter keeps most garbage out of those fields, but it
 * validates with BigInteger, so text that is numeric-but-not-an-int
 * (twenty digits, a pasted huge negative) still reached parseInt and
 * threw NumberFormatException off the EDT. This helper is the single
 * guarded replacement for those sites: empty text means the minimum,
 * valid text is clamped to the minimum (the pre-existing Math.max
 * behavior), and malformed text is reported through TellUser while the
 * previous value stays in effect. The field text is normalized to
 * whatever value was settled on, exactly as the old sites did with
 * setText(value+"").
 *
 * @author Josh Marshall
 */
public final class NumericField {

	// no instances; static helper in the TellUser/TextFilter family
	/**
	 * Prevents instantiation; this is a static-only helper.
	 */
	private NumericField() { }

	/**
	 * Read an int from a numeric text field without ever throwing.
	 *
	 * @param parent The component to parent an error dialog on, or null
	 *               for the application frame.
	 * @param field The text field to read.
	 * @param minimum The smallest acceptable value; empty text and
	 *                lesser values become this (clamping, not an error).
	 * @param previous The value currently in effect, kept (and reported
	 *                 back into the field) when the text does not parse.
	 * @param description What the field holds, in domain terms
	 *                    (e.g. "Scale factor"), for the error message.
	 *
	 * @return the parsed-and-clamped value, or previous if the text was
	 *         not a parsable int.
	 *
	 * @jls.testedby jls.NumericFieldTest#emptyTextMeansTheMinimum()
	 * @jls.testedby jls.NumericFieldTest#lonelyMinusSignKeepsThePreviousValueAndReports()
	 * @jls.testedby jls.NumericFieldTest#negativeValuesClampToTheMinimumWithoutError()
	 * @jls.testedby jls.NumericFieldTest#unparsableTextKeepsThePreviousValueAndReports()
	 * @jls.testedby jls.NumericFieldTest#validTextParsesAndNormalizesTheField()
	 * @jls.testedby jls.NumericFieldTest#valuesBelowTheMinimumClampSilently()
	 */
	public static int parse(Component parent, JTextField field, int minimum,
			int previous, String description) {

		String text = field.getText();
		int value;
		if (text.length() == 0)
			value = minimum;
		else {
			try {
				value = Math.max(minimum, Integer.parseInt(text));
			}
			catch (NumberFormatException ex) {
				TellUser.error(parent, description
						+ " must be a whole number between " + minimum
						+ " and " + Integer.MAX_VALUE, "Error");
				value = previous;
			}
		}
		field.setText(value + "");
		return value;
	} // end of parse method

} // end of NumericField class
