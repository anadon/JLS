package jls.elem;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;

/**
 * A truth-table value cell (0, 1, or don't-care) (#27 S4): InputVal and
 * OutputVal drew and sized identically and differ only in what a click
 * does, which stays in the subclasses' selected() methods.
 *
 * @author David A. Poplawski
 */
public abstract class ValEntry extends Entry {

	// properties
	/** The value displayed in this cell: 0 or 1, or 2 for don't-care. */
	int value = 0;  // 0 or 1, 2 = don't care

	/**
	 * Create a new entry.
	 *
	 * @param ttelem A reference to the TruthTable object this is a part of.
	 * @param value The value of this signal in this place.
	 * @param g A Graphics object to size the value with.
	 */
	public ValEntry(TruthTable ttelem, int value, Graphics g) {

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
	@Override
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

} // end of ValEntry class
