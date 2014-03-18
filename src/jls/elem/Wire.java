package jls.elem;

import jls.*;
import java.awt.*;
import java.awt.geom.*;
import java.io.*;
import javax.swing.*;
import java.util.*;

import javax.swing.JLabel;

/**
 * A wire segment (between two WireEnds), part of a WireNet.
 * 
 * @author David A. Poplawski
 */
public class Wire extends Element {
	
	// properties
	private WireNet net;		// the wire net it is a part of
	private WireEnd end1;		// the wire ends
	private WireEnd end2;
	private boolean marked;		// used to partition wire net
	private boolean touching;	// touching a wire end?
	private String probeName = null;
	
	/**
	 * Create a new wire with the given ends.
	 * 
	 * @param e1 One end of the wire.
	 * @param e2 The other end of the wire.
	 */
	public Wire(WireEnd e1, WireEnd e2) {
		
		super(null);
		end1 = e1;
		end2 = e2;
	} // end of constructor
	
	/**
	 * This form of init not used.
	 */
	public void init(Graphics g) {
		
		// do nothing
	} // end of init method
	
	/**
	 * A wire doesn't move on its own.
	 * It is drawn using the position of its ends.
	 * 
	 * @param dx Distance to move in the x direction.
	 * @param dy Distance to move in the y direction.
	 */
	public void move(int dx, int dy) {
		
	} // end of move method
	
	/**
	 * Get one end of this wire.
	 * 
	 * @return one end.
	 */
	public WireEnd getEnd() {
		
		return end1;
	} // end of getEnd method
	
	/**
	 * Get other end of wire from given end.
	 * 
	 * @param end One end of the wire.
	 * 
	 * @return the other end of the wire.
	 */
	public WireEnd getOtherEnd(WireEnd end) {
		
		if (end == end1)
			return end2;
		else
			return end1;
	} // end of getOtherEnd method
	
	/**
	 * Set wire ends.
	 * 
	 * @param e1 One new end.
	 * @param e2 The other new end.
	 */
	public void setEnds(WireEnd e1, WireEnd e2) {
		
		end1 = e1;
		end2 = e2;
	} // end of setEnds method
	
	/**
	 * Wires don't get saved.
	 */
	public void save(PrintWriter output) {
		
		// do nothing
	} // end of save method
	
	/**
	 * Draw this wire.
	 * 
	 * @param g Graphics object to draw with.
	 */
	public void draw(Graphics g) {
		
		if (touching) {
			g.setColor(JLSInfo.touchColor);
		}
		else if (highlight) {
			g.setColor(JLSInfo.highlightColor);
		}
		else if (getValue() == null) { // off
			g.setColor(Color.blue);
		}
		else if (!(getValue().isEmpty())) {
			g.setColor(JLSInfo.nonZeroColor);
		}
		else {
			g.setColor(Color.black);
		}
		int x1 = end1.getX();
		int y1 = end1.getY();
		int x2 = end2.getX();
		int y2 = end2.getY();
		g.drawLine(x1,y1,x2,y2);
		
		// draw probe name if there is one
		if (probeName == null)
			return;
		FontMetrics fm = g.getFontMetrics();
		int len = fm.stringWidth(probeName);
		g.setColor(Color.BLUE);
		
		// handle orientation cases
		if (y1 == y2) {
			
			// horizontal
			int mid = (x1+x2)/2;
			g.drawString(probeName,mid-len/2,y1-fm.getDescent());
		}
		else if (x1 == x2) {
			
			// vertical
			int mid = (y1+y2)/2;
			g.drawString(probeName,x1+1,mid);
		}
		else {
			
			// compute slope
			double slope = (double)(y2-y1)/(x2-x1);
			int midx = (x1+x2)/2;
			int midy = (y1+y2)/2;
			if (slope > 0) {
				
				// down to the right
				g.drawString(probeName,midx,midy-fm.getDescent());
			}
			else {
				
				// up to the right
				g.drawString(probeName,midx-len,midy-fm.getDescent());
			}
		}
	} // end of draw method
	
	/**
	 * Set bits in the wire net this wire is in.
	 * 
	 * @param bits The number of bits.
	 */
	public void setBits(int bits) {
		
		net.setBits(bits);
	} // end of setBits method

	/**
	 * Get the number of bits in this wire.
	 * 
	 * @return the number of bits.
	 */
	public int getBits() {
		
		return net.getBits();
	} // end of getBits method
	
	/**
	 * See if the given point is close to the wire.
	 * 
	 * @param x The x-coordinate of the given point.
	 * @param y The y-coordinate of the given point.
	 * 
	 * @return true if the point is within 1/2 spacing of the wire and not within 
	 * pointDiameter pixels of either end, false if not.
	 */
	public boolean contains(int x, int y) {
		
		int x1 = end1.getX();
		int y1 = end1.getY();
		int x2 = end2.getX();
		int y2 = end2.getY();
		int d = JLSInfo.pointDiameter;
		if (Line2D.ptSegDist(x1,y1,x2,y2,x,y) < JLSInfo.spacing/2 &&
				Point2D.distance(x1,y1,x,y) > d && Point2D.distance(x2,y2,x,y) > d) {
			return true;
		}
		else {
			return false;
		}
	} // end of contains method
	
	/**
	 * See if the wire (not counting end points) intersects a given rectangle.
	 * 
	 * @param rect The rectangle.
	 * 
	 * @return true if it it does, false if it does not.
	 */
	public boolean intersects(Rectangle rect) {
		
		// get end points of wire
		int x1 = end1.getX();
		int y1 = end1.getY();
		int x2 = end2.getX();
		int y2 = end2.getY();
		
		// if the rectangle intersects the wire...
		Rectangle2D r = (Rectangle2D)rect;
		if (r.intersectsLine(x1,y1,x2,y2)) {
			
			// then exclude end points (smaller rectangle a kludge 'cause contains
			// means inside, not including on the edge)
			Rectangle rs = new Rectangle(rect.x-1,rect.y-1,rect.width+2,rect.height+2);
			if (rs.contains(x1,y1) || rs.contains(x2,y2)) {
				return false;
			}
			return true;
		}
		return false;
	} // end of intersects method
	
	/**
	 * See if a given wire end touches the wire.
	 * The wire end must be within half a point diameter of the wire
	 * but not within a half point diameter of either end.
	 * 
	 * @param end The wire end.
	 * 
	 * @return true if the wire end touches the wire, false if not.
	 */
	public boolean touches(WireEnd end) {
		
		if (end == end1 || end == end2)
			return false;
		int x = end.getX();
		int y = end.getY();
		int x1 = end1.getX();
		int y1 = end1.getY();
		int x2 = end2.getX();
		int y2 = end2.getY();
		int d = JLSInfo.pointDiameter;
		if (Line2D.ptSegDist(x1,y1,x2,y2,x,y) < d/2) {
			if (Point.distance(x,y,x1,y1) < d/2) {
				return false;
			}
			if (Point.distance(x,y,x2,y2) < d/2) {
				return false;
			}
			return true;
		}
		else {
			return false;
		}
	} // end of touches method
	
	/**
	 * Display information about this wire.
	 * 
	 * @param info A JLabel to display with.
	 */
	public void showInfo(JLabel info) {
		
		String tri = "";
		if (net.isTriState())
			tri = "(tri-state) ";
		String inp = "";
		String value = "";
		if (!net.hasInput()) {
			inp = ", no input";
		}
		else {
			value = ", value = " + BitSetUtils.toDisplay(getValue(),net.getBits());
		}
		int bits = net.getBits();
		String probe = "";
		if (probeName != null)
			probe = " (probe: " + probeName + ")";
		if (bits <= 0) {
			info.setText(tri + "bits unknown" + probe);
		}
		else if (bits == 1) {
			info.setText(tri + "1 bit" + inp + probe + value);
		} 
		else {
			info.setText(tri + bits + " bits" + inp + probe + value);
		}
	} // end of showInfo method
	
	/**
	 * Remove this wire and both of its ends from the circuit.
	 * Ends are removed only if possible.
	 * 
	 * @param The circuit it is in.
	 */
	public void remove(Circuit circ) {
		
		// remove from circuit
		circ.remove(this);
		
		// remove from both ends
		end1.remove(this,circ);
		end2.remove(this,circ);
		
	} // end of remove method
	
	/**
	 * Set/reset marked flag (used to create new wire nets).
	 * 
	 * @param which True to mark, false to unmark.
	 */
	public void mark(boolean which) {
		
		marked = which;
	} // end of mark method
	
	/**
	 * See if this wire is marked.
	 * 
	 * @return true if marked, false if not.
	 */
	public boolean isMarked() {
		
		return marked;
	} // end of isMarked method
	
	/**
	 * Mark this wire and all wire ends and wires connected to it.
	 */
	public void traverse() {
		
		if (marked)
			return;
		marked = true;
		end1.traverse();
		end2.traverse();
	} // end of traverse method
	
	/**
	 * Put this wire in a new wire net.
	 * 
	 * @param the new wire net.
	 */
	public void setNet(WireNet net) {
		
		this.net = net;
	} // end of setNet method
	
	/**
	 * See if this wire is completely inside the given rectangle.
	 * 
	 * @return true if it is, false if not.
	 */
	public boolean isInside(Rectangle rect) {
		
		if (rect.contains(end1.getX(),end1.getY()) && rect.contains(end2.getX(),end2.getY())) {
			return true;
		}
		else {
			return false;
		}
	} // end of isInside method
	
	/**
	 * Set touching.
	 * 
	 * @param which True to say it is, false if not.
	 */
	public void setTouching(boolean which) {
		
		touching = which;
	} // end of setTouching method
	
	/**
	 * Get the length (in pixels) of this wire.
	 * 
	 * @return the length.
	 */
	public int length() {
		
		return (int)(Point.distance(end1.getX(),end1.getY(),end2.getX(),end2.getY()));
	} // end of length method

	/**
	 * Attach a probe to this wire.
	 * 
	 * @param name The name of the probe.
	 *         If null, then prompt user for a name.
	 */
	public void attachProbe(String name) {
		
		// see if a name is provided
		if (name != null) {
			probeName = name;
			return;
		}
		
		// get a name for this probe
		name = JOptionPane.showInputDialog("Name?");
		if (name == null)
			return;
		while (name.equals("")) {
			name = JOptionPane.showInputDialog("Invalid name, try again");
			if (name == null)
				return;
		}
		probeName = name;
		
	} // end of attachProbe method
	
	/**
	 * See if this wire has a probe attached.
	 * 
	 * @return true if it does, false if it does not.
	 */
	public boolean hasProbe() {
		
		return probeName != null;
	} // end of hasProbe method
	
	/**
	 * Get the probe name.
	 * 
	 * @return the name.
	 */
	public String getProbe() {
		
		return probeName;
	} // end of getProbe method
	
	/**
	 * Remove probe from this wire.
	 */
	public void removeProbe() {
		
		probeName = null;
	} // end of removeProbe
	
	/**
	 * Get the value on this wire (from the wire net).
	 * 
	 * @return the value.
	 */
	public BitSet getValue() {
		
		return net.getValue();
	} // end of getValue method

} // end of Wire class
