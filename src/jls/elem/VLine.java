package jls.elem;

import java.awt.*;

/**
 * Draw part of the vertical line separating the inputs from the outputs
 * of the truth table.
 * 
 * @author David A. Poplawski
 */
public final class VLine extends Entry {

	/**
	 * Construct a new VLine.
	 * 
	 * @param ttelem A reference to the TruthTable object this is a part of.
	 */
	public VLine(TruthTable ttelem) {
		
		super(ttelem);
	} // end of constructor
	
	/**
	 * Draw a vertical line all the way across the entry,
	 * centered horizontally.
	 * 
	 * @param g The Graphics object to draw with.
	 */
	public void draw(Graphics g) {
		
		g.setColor(Color.black);
		g.drawLine(x+width/2,y,x+width/2,y+height);
	} // end of draw method
	
} // end of VLine class
