package jls;

import javax.swing.*;
import java.math.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/**
 * Make sure everything in a text field is numeric.
 * 
 * @author David A. Poplawski
 */
public final class TextFilter extends DocumentFilter {

	// properties
	private JTextField target;
	private int base = 10;
	private BigInteger max = null; // null means no maximum

	/**
	 * Construct filter.
	 * 
	 * @param target The text field being checked.
	 */
	public TextFilter(JTextField target) {

		this.target = target;
	} // end of constructor

	/**
	 * Set the maximum value for numbers allowed in this text field.
	 *
	 * @param max The maximum value.
	 */
	public void setMax(long max) {

		// the max is a value, not digits: parsing its decimal rendering
		// in the field's base made a hex field's setMax(255) mean 0x255
		// and a binary field's setMax(5) throw on the EDT (issue #51)
		this.max = BigInteger.valueOf(max);
	} // end of setMax method

	/**
	 * Set the base of numbers allowed in this text field.
	 * 
	 * @param base The new base.
	 */
	public void setBase(int base) {

		this.base = base;
	} // end of setBase method

	@Override
	public void insertString(DocumentFilter.FilterBypass b, int offset,
			String str, AttributeSet s) throws BadLocationException {

		// not allowed
	} // end of insertString method

	@Override
	public void remove(DocumentFilter.FilterBypass b, int offset,
			int length) throws BadLocationException {

		// pass through unchanged
		super.remove(b,offset,length);
	} // end of insertString method

	@Override
	public void replace(DocumentFilter.FilterBypass b, int offset, int length,
			String str, AttributeSet s) throws BadLocationException {


		// if insertion
		if (length == 0) {

			// make sure is still numeric and not too big
			try {
				BigInteger val = new BigInteger(str,base);
				if (max != null) {
					String old = target.getText();
					String all = old.substring(0,offset)+str+old.substring(offset+length);
					BigInteger allBig = new BigInteger(all,base);
					if (allBig.compareTo(max) > 0)
						return;
				}

				// if so, do it
				super.replace(b,offset,length,str,s);
				return;
			}
			catch (NumberFormatException ex) {

				// if not numeric, ignore insertion
				return;
			}
		}

		// must be a replace
		String old = target.getText();
		String all = old.substring(0,offset)+str+old.substring(offset+length);

		// make sure new input is numeric and not too big
		try {
			BigInteger val = new BigInteger(all,base);
			if (max != null) {
				if (val.compareTo(max) > 0)
					return;
			}

			// if so, do replace
			String sval = val.toString(base);
			super.replace(b,0,old.length(),sval,s);
		}
		catch (NumberFormatException ex) {

			// if not numeric, ignore replace
			return;
		}
	} // end of replace method

} // end of TextFilter class