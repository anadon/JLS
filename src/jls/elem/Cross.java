package jls.elem;

import java.awt.*;

/**
 * Draw the cross where the horizontal and vertical lines of the
 * truth table meet.
 * 
 * @author David A. Poplawski
 */
public final class Cross extends Entry {

	/**
	 * Construct a new Cross.
	 * 
	 * @param ttelem A reference to the TruthTable object this is a part of.
	 */
	public Cross(TruthTable ttelem) {
		
		super(ttelem);
	} // end of constructor

	/**
	 * Draw a horizontal line centered vertically and a vertical line centered
	 * horizontally.
	 * 
	 * @param g The Graphics object to draw with.
	 */
	public void draw(Graphics g) {
		
		g.setColor(Color.black);
		g.drawLine(x,y+height/2,x+width,y+height/2);
		g.drawLine(x+width/2,y,x+width/2,y+height);
	} // end of draw method
	
} // end of Cross method
