package jls.elem;

import java.awt.Graphics;

/**
 * An output signal value in a truth table; drawing and sizing live in
 * ValEntry (#27 S4).
 * 
 * @author David A. Poplawski
 */
public final class OutputVal extends ValEntry {

	/**
	 * Create a new entry.
	 * 
	 * @param ttelem A reference to the TruthTable object this is a part of.
	 * @param value The value of this signal in this place.
	 * @param g A Graphics object to size the value with.
	 */
	public OutputVal(TruthTable ttelem, int value, Graphics g) {
		
		super(ttelem,value,g);
	} // end of constructor

	/**
	 * Change value when selected.
	 * 
	 * @param row The row this entry is in.
	 * @param col The column this entry is in.
	 */
	public void selected(int row, int col) {
		
		ttelem.toggleOutput(row-2,col-1);
	} // end of selected method

} // end of OutputVal class
