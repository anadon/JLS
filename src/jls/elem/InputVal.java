package jls.elem;

import java.awt.Graphics;

/**
 * An input signal value in a truth table; drawing and sizing live in
 * ValEntry (#27 S4).
 * 
 * @author David A. Poplawski
 */
public final class InputVal extends ValEntry {

	/**
	 * Create a new entry.
	 * 
	 * @param ttelem A reference to the TruthTable object this is a part of.
	 * @param value The value of this signal in this place.
	 * @param g A Graphics object to size the value with.
	 */
	public InputVal(TruthTable ttelem, int value, Graphics g) {
		
		super(ttelem,value,g);
	} // end of constructor

	/**
	 * Change value when selected.
	 * A 0 or 1 becomes a don't care (if possible) and two rows are collapsed.
	 * A don't care expands into two rows.
	 * 
	 * @param row The row this entry is in.
	 * @param col The column this entry is in.
	 */
	@Override
	public void selected(int row, int col) {
		
		if (value == 2) {
			
			// undo don't care
			ttelem.undoDontCare(row-2,col);
		}
		else {
			
			// make a don't care
			ttelem.makeDontCare(row-2,col);
		}
	} // end of selected method

} // end of InputVal class
