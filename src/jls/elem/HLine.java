package jls.elem;

import java.awt.*;

/**
 * Draw part of the horizontal line below the signal names.
 * 
 * @author David A. Poplawski
 */
public final class HLine extends Entry {
	/**
	 * Construct a new HLine.
	 * 
	 * @param ttelem A reference to the TruthTable object this is a part of.
	 */
	public HLine(TruthTable ttelem) {
		
		super(ttelem);
	} // end of constructor
	
	/**
	 * Draw a horizontal line all the way across the entry,
	 * centered vertically.
	 * 
	 * @param g The Graphics object to draw with.
	 */
	public void draw(Graphics g) {
		
		g.setColor(Color.black);
		g.drawLine(x,y+height/2,x+width,y+height/2);
	} // end of draw method
	
} // end of HLine method
