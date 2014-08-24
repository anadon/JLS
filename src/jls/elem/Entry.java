package jls.elem;

import java.awt.*;

/**
 * Superclass of all truth table display entries.
 * 
 * @author David A. Poplawski
 */
public abstract class Entry {
	
	// named constants
	private final int defaultWidth = 10;
	private final int defaultHeight = 10;
	
	// properties
	protected TruthTable ttelem;
	protected int x;
	protected int y;
	protected int minWidth = defaultWidth;
	protected int minHeight = defaultHeight;
	protected int width = defaultWidth;
	protected int height = defaultHeight;
	
	/**
	 * Construct a new Entry.
	 * 
	 * @param ttelem A reference to the TruthTable object this is a part of.
	 */
	public Entry(TruthTable ttelem) {
		
		this.ttelem = ttelem;
	} // end of constructor
	
	/**
	 * Set the x,y position of this entry.
	 * 
	 * @param x The x-coordinate.
	 * @param y The y-coordinate.
	 */
	public void setPosition(int x, int y) {
		
		this.x = x;
		this.y = y;
	} // end of setPosition method
	
	/**
	 * Set the size of this entry.
	 * 
	 * @param width The new width.
	 * @param height The new height.
	 */
	public void setSize(int width, int height) {
		
		this.width = width;
		this.height = height;
	} // end of setSize method
	
	/**
	 * Get the minimum width of this element.
	 * 
	 * @return the minimum width.
	 */
	public int getMinWidth() {
		
		return minWidth;
	} // end of getMinWidth method
	
	/**
	 * Get the minimum height of this element.
	 * 
	 * @return the minmum height.
	 */
	public int getMinHeight() {
		
		return minHeight;
	} // end of getMinHeight method
	
	/**
	 * Draw this entry.
	 * 
	 * @param g The Graphics object to draw with.
	 */
	public abstract void draw(Graphics g);
	
	/**
	 * See if a given point is inside this entry.
	 * 
	 * @param x The x-coordinate of the point.
	 * @param y The y-coordinate of the point.
	 * 
	 * @return true if the point is inside the entry, false if it is not.
	 */
	public boolean contains(int x, int y) {
		
		if (x <= this.x || y <= this.y || x >= this.x+width || y >= this.y+height)
			return false;
		return true;
	} // end of contains method
	
	/**
	 * Do something when the mouse is clicked on this entry.
	 * 
	 * @param row The row this entry is in.
	 * @param col The column this entry is in.
	 */
	public void selected(int row, int col) {
	
		// the default is to do nothing
	} // end of selected method

} // end of Entry class
