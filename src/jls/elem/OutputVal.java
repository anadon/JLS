package jls.elem;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;

/**
 * Draw an output signal value and react to value being clicked on in the truth table.
 * 
 * @author David A. Poplawski
 */
public final class OutputVal extends Entry {
	
	// properties
	int value = 0;  // 0 or 1, 2 = don't care

	/**
	 * Create a new entry.
	 * 
	 * @param ttelem A reference to the TruthTable object this is a part of.
	 * @param value The value of this signal in this place.
	 * @param g A Graphics object to size the value with.
	 */
	public OutputVal(TruthTable ttelem, int value, Graphics g) {
		
		super(ttelem);
		this.value = value;
		FontMetrics fm = g.getFontMetrics();
		minHeight = fm.getAscent()+fm.getDescent();
		minWidth = fm.stringWidth(" " + value + " ");
	} // end of constructor

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
	 * 
	 * @param row The row this entry is in.
	 * @param col The column this entry is in.
	 */
	public void selected(int row, int col) {
		
		ttelem.toggleOutput(row-2,col-1);
	} // end of selected method

} // end of InputVal class

