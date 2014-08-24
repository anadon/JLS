package jls.elem;

import jls.*;
import java.awt.*;
import java.util.*;

/**
 * Superclass for input and output points.
 * Contains common data and methods.
 * 
 * @author David A. Poplawski
 */
public abstract class Put {

	// properties
	protected String name;				// name
	protected LogicElement element;		// the element it is a part of
	protected int xr;					// x-coordinate of center relative to element
	protected int yr;					// y-coordinate of center relative to element
	private int savex;
	private int savey;
	protected int bits;					// number of bits
	private boolean touching = false;	// touching a WireEnd?
	private WireEnd wireEnd = null;		// the WireEnd this put attached to
	protected Put myCopy;				// to help cut/paste
	
	/**
	 * Create a new put.
	 * 
	 * @param name The name of this put (e.g., "input0").
	 * @param element The element this put is part of.
	 * @param xr The x-coordinate of the center of the put relative to the upper left
	 * 		corner of the element this put is in.
	 * @param yr The y-coordinate of the center of the put relative to the upper left
	 * 		corner of the element this put is in.
	 * @param bits The number of bits in this put.  0 implies arbitrary.
	 */
	public Put(String name, LogicElement element, int xr, int yr, int bits) {
		
		this.name = name;
		this.element = element;
		this.xr = xr;
		this.yr = yr;
		this.bits = bits;
	} // end of constructor
	
	/**
	 * Return a string version of the properties of this element.
	 * 
	 * @return the string.
	 */
	public String toString() {
		
		return name + ",bits=" + bits + ",x=" + xr + ",y=" + yr;
	} // end of toString method
	
	/**
	 * Get the put's name;
	 * 
	 * @return the put's name.
	 */
	public String getName() {
		
		return name;
	} // end of getName method
	
	/**
	 * Get put's x-coordinate.
	 * 
	 * @return The x-coordinate.
	 */
	public int getX() {
		
		return element.getX()+xr;
	} // end of getX method
	
	/**
	 * Get put's y-coordinate.
	 * 
	 * @return The y-coordinate.
	 */
	public int getY() {
		
		return element.getY()+yr;
	} // end of getY method
	
	/**
	 * Get number of bits in this put.
	 * 
	 * @return the number of bits.
	 */
	public int getBits() {
		
		return bits;
	} // end of getBits method
	
	/**
	 * Get the element this put is part of.
	 * 
	 * @return the element.
	 */
	public LogicElement getElement() {
		
		return element;
	} // end of getElement method
	
	/**
	 * Get wire end this put is attached to, or null if not attached.
	 */
	public WireEnd getWireEnd() {
		
		return wireEnd;
	} // end of getWireEnd method
	
	/**
	 * See if this put is attached to a WireEnd.
	 * 
	 * @return true if it is attached, false if not.
	 */
	public boolean isAttached() {
		
		return wireEnd != null;
	} // end of isAttached method
	
	/**
	 * Record that this put is attached to a WireEnd.
	 * Wire end can be null, indicating that the put is to become unattached.
	 *
	 * @param end The wire end it is to attach to, or null if detaching.
	 */
	public void setAttached(WireEnd end) {
		
		wireEnd = end;
	} // end of setAttached method
	
	/**
	 * Record that this put is touching or not touching a WireEnd.
	 * 
	 * @param is True if touching, false if not.
	 */
	public void setTouching(boolean is) {
		
		touching = is;
	} // end of setTouching method
	
	/** 
	 * See if this put is touching something.
	 * 
	 * @return True if touching, false if not.
	 */
	public boolean isTouching() {
		
		return touching;
	} // end of isTouching method
	
	/**
	 * Set new position.
	 * 
	 * @param x New x-coordinate.
	 * @param y New y-coordinate.
	 */
	public void setPosition(int x, int y) {
		
		this.xr = x;
		this.yr = y;
	} // end of setPosition method
	
	/**
	 * Save the position of this put.
	 */
	public void savePosition() {
		
		savex = xr;
		savey = yr;
	} // end of savePosition method
	
	/**
	 * Restore the position of this put.
	 */
	public void restorePosition() {
		
		xr = savex;
		yr = savey;
	} // end of restorePosition method
	
	/**
	 * Draw this put.
	 * 
	 * @param g The graphics object to draw with.
	 */
	public void draw(Graphics g) {
		
		Color inside;
		if (touching) {
			inside = JLSInfo.touchColor;
		}
		else if (isAttached()) {
			inside = Color.BLACK;
			return;
		}
		else {
			inside = Color.WHITE;
		}
		int d = JLSInfo.pointDiameter;
		int r = d/2;
		int x = element.getX()+xr;
		int y = element.getY()+yr;
		g.setColor(Color.BLACK);
		g.fillOval(x-r,y-r,d,d);
		g.setColor(inside);
		g.fillOval(x-r+1,y-r+1,d-2,d-2);
	} // end of draw method
	
	/**
	 * See if this put is at the same place as another put.
	 * 
	 * @param other The other put.
	 * 
	 * @return true if at the same place, false if not.
	 */
	public boolean intersects(Put other) {
		
		int thisx = element.getX() + xr;
		int thisy = element.getY() + yr;
		int otherx = other.element.getX() + other.xr;
		int othery = other.element.getY() + other.yr;
		if (this != other && thisx == otherx && thisy == othery) {
			return true;
		}
		return false;
	} // end of intersects method
	
	/**
	 * Get the copy of this put.
	 * 
	 * @return the copy.
	 */
	public Put getCopy() {
		
		return myCopy;
	} // end of getCopy method
	
//-------------------------------------------------------------------------------
// Simulation
//-------------------------------------------------------------------------------
		
	protected BitSet currentValue;
	
} // end of Put class
