package jls.elem;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;

/**
 * Draw an input signal value and react to value being clicked on in the truth table.
 * 
 * @author David A. Poplawski
 */
public final class InputVal extends Entry {
	
	// properties
	int value = 0;  // 0 or 1, 2 = don't care

	/**
	 * Create a new entry.
	 * 
	 * @param ttelem A reference to the TruthTable object this is a part of.
	 * @param value The value of this signal in this place.
	 * @param g A Graphics object to size the value with.
	 */
	public InputVal(TruthTable ttelem, int value, Graphics g) {
		
		super(ttelem);
		this.value = value;
		FontMetrics fm = g.getFontMetrics();
		minHeight = fm.getAscent()+fm.getDescent();
		minWidth = fm.stringWidth(" " + value + " ");
	} // end of constructor
	
	/**
	 * Get the value of this entry.
	 * 
	 * @return the value.
	 */
	public int getValue() {
		
		return value;
	} // end of getValue method

	/**
	 * Draw this entry.
	 * 
	 * @param g The Graphics object to draw with.
	 */
	public void draw(Graphics g) {

		FontMetrics fm = g.getFontMetrics();
		int ascent = fm.getAscent();
		int height = ascent + fm.getDescent();
		String str = "" + value;
		if (value == 2)
			str = "x";
		int width = fm.stringWidth(str);
		g.setColor(Color.BLACK);
		g.drawString(str,x+(this.width-width)/2,y+(this.height-height)/2+ascent);
	} // end of draw method

	/**
	 * Change value when selected.
	 * A 0 or 1 becomes a don't care (if possible) and two rows are collapsed.
	 * A don't care expands into two rows.
	 * 
	 * @param row The row this entry is in.
	 * @param col The column this entry is in.
	 */
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
