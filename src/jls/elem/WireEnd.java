package jls.elem;

import jls.*;
import jls.sim.*;
import java.awt.*;
import java.io.PrintWriter;
import java.util.*;
import javax.swing.*;

/**
 * The end of a wire segment (displayed as a circle until connected).
 * 
 * @author David A. Poplawski
 */
public class WireEnd extends LogicElement {
	
	// properties
	private WireNet net;							// the net it is a part of
	private Set<Wire> wires = new HashSet<Wire>();	// the wires it is connected to
	private Put put = null;							// the put it is attached to
	private boolean touching = false;				// touching something (can connect)?
	private boolean marked;							// used to partition wire net
	private WireEnd myCopy;							// for cut/paste
	private int loadAttach;							// for loading circuit
	private String loadPut = null;					// for loading circuit
	private boolean loadTriState = false;			// for loading circuit
	private Set<Integer> loadWires = 				// for loading circuit
		new HashSet<Integer>();
	private Map<Integer,String> probeMap =			// for loading circuit
		new HashMap<Integer,String>();
	
	/**
	 * Get string version of properties (for debugging).
	 * 
	 * @return the string.
	 */
	public String toString() {
		
		return "WireEnd[" + net + "]";
	} // end of toString method
	
	/**
	 * Create a new wire end.
	 * 
	 * @param circuit The circuit the wire end is in.
	 */
	public WireEnd(Circuit circuit) {
		
		super(circuit);
	} // end of constructor
	
	/**
	 * This form of init not used.
	 * 
	 * @param The Graphics object needed by overridden methods.
	 */
	public void init(Graphics g) {
		
		// do nothing
	} // end of init method
	
	/**
	 * Initialize the wire end.
	 * 
	 * @param circ The circuit this wire end is in.
	 */
	public void init(Circuit circ) {
		
		width = JLSInfo.pointDiameter;
		height = JLSInfo.pointDiameter;
		
		fixPosition();
		
		// hook up wires
   next:for (int elid : loadWires) {
	   		// attach to put if needed
			if (loadPut != null) {
				Element elem = circ.getElementByID(loadAttach);
				Put p = elem.getPut(loadPut);
				p.setAttached(this);
				setPut(p);
			}
			
			// see if already hooked up
			WireEnd end = (WireEnd)(circ.getElementByID(elid));
			for (Wire wire : wires) {
				WireEnd otherEnd = wire.getOtherEnd(this);
				if (end == otherEnd) {
					continue next;
				}
			}
			
			// create a new wire between this and end
			Wire wire = new Wire(this,end);
			addWire(wire);
			end.addWire(wire);
			circ.addElement(wire);
			
			// put probe on wire if there is a probe
			String probeName = probeMap.get(elid);
			if (probeName == null)
				continue;
			wire.attachProbe(probeName);
		}
		
	} // end of init method
	
	/**
	 * Make a copy of this wire end.
	 */
	public WireEnd copy() {
		
		WireEnd it = new WireEnd(circuit);
		super.copy(it);
		myCopy = it;
		return it;
	} // end of copy method
	
	/**
	 * Get x-coordinate of this wire end.
	 * 
	 * @return The x-coordinate.
	 */
	public int getX() {
		
		return x;
	} // end of getX method
	
	/**
	 * Get y-coordinate of this wire end.
	 * 
	 * @return The y-coordinate.
	 */
	public int getY() {
		
		return y;
	} // end of getY method
	
	/**
	 * Add a wire to this end.
	 * 
	 * @param wire The wire to add.
	 */
	public void addWire(Wire wire) {
		
		wires.add(wire);
	} // end of addWire method
	
	/**
	 * See if the given point is inside the element's display area.
	 * 
	 * @param x The x-coordinate of the given point.
	 * @param y The y-coordinate of the given point.
	 * 
	 * @return true if the point is in the display area, false if not.
	 */
	public boolean contains(int x, int y) {
		
		return getRect().contains(x,y);
	} // end of contains method
	
	/**
	 * See if the element is completely inside a given rectangle.
	 * In this case only the center point needs to be inside the given rectangle.
	 * 
	 * @param rect The given rectangle.
	 * 
	 * @return true if the element is inside, false if not.
	 */
	public boolean isInside(Rectangle rect) {
		
		return rect.contains(x,y);
	} // end of isInside method
	
	/**
	 * Draw this element.
	 * 
	 * @param g The Graphics object to draw with.
	 */
	public void draw(Graphics g) {
		
		Color inside;
		if (touching) {
			inside = JLSInfo.touchColor;
		}
		else if (highlight) {
			inside = JLSInfo.highlightColor;
		}
		else if (put != null) {
			if (isAttached())
				return;
			inside = Color.black;
		}
		else if (wires.size() <= 1) {
			inside = Color.WHITE;
		}
		else {
			if (degree() == 2)
				return;
			inside = Color.black;
		}
		g.setColor(Color.BLACK);
		int pd = JLSInfo.pointDiameter;
		int pr = pd/2;
		g.fillOval(x-pr,y-pr,pd,pd);
		g.setColor(inside);
		g.fillOval(x-pr+1,y-pr+1,pd-2,pd-2);
	} // end of draw method
	
	/**
	 * This wire end will be removed, so remove wires it connects to.
	 * 
	 * @param circ The circuit this wire end is in.
	 */
	public void remove(Circuit circ) {
		
		circ.remove(this);
		for (Wire wire : wires) {
			circ.remove(wire);
			wire.getOtherEnd(this).remove(wire,circ);
		}
	} // end of remove method
	
	/**
	 * Remove wire from this wire end, and do nothing else.
	 */
	public void remove(Wire wire) {
		
		wires.remove(wire);
	} // end of remove method
	
	/**
	 * Remove a wire from this wire end.
	 * If last wire removed, remove this wire end too.
	 * 
	 * @param wire The wire to remove.
	 * @param circ The circuit the wire is in.
	 */
	public void remove(Wire wire, Circuit circ) {
		
		// remove the wire
		wires.remove(wire);
		circ.remove(wire);
		
		// if no other wires...
		if (wires.isEmpty()) {
			
			// disconnect from put
			if (isAttached()) {
				if (this.getPut() instanceof Output) {
					put.setAttached(null);
					put = null;
				}
				else if (getNet().isTriState() && put.getElement() instanceof TriProp) {
					TriProp el = (TriProp)put.getElement();
					put.setAttached(null);
					put = null;
					el.setTriState(false);
				}
				else {
					put.setAttached(null);
					put = null;
				}
			}
			
			// remove from circuit
			circ.remove(this);
		}
		
		// otherwise create a new wire net
		else {
			net = net.makeNet(this);
		}
	} // end of remove method
	
	/**
	 * Set the put this wire end is attached to.
	 * 
	 * @param put The put
	 */
	public void setPut(Put put) {
		
		this.put = put;
	} // end of setPut method
	
	/**
	 * Get the put this wire end is attached to, or null of not attached.
	 * 
	 * @return the put attached to.
	 */
	public Put getPut() {
		
		return put;
	} // end of getPut method
	
	/**
	 * See if this wire end is attached to a put.
	 * 
	 * @return true if it is, false if not.
	 */
	public boolean isAttached() {
		
		return put != null;
	} // end of isAttached method
	
	/**
	 * See if this wire end is touching a put.
	 * 
	 * @return true if it is, false if not.
	 */
	public boolean isTouching() {
		
		return touching;
	} // end of isTouching method
	
	/**
	 * Set whether this wire end is touching something.
	 * 
	 * @param setting True if it is, false if not.
	 */
	public void setTouching(boolean setting) {
		
		touching = setting;
	} // end of setTouching method
	
	/**
	 * Get the number of bits in the wire net this wire end is part of.
	 * 
	 * @return the number of bits.
	 */
	public int getBits() {
		
		return net.getBits();
	} // end of getBits method
	
	/**
	 * Set number of bits in the wire net this wire end is part of.
	 * 
	 * @param bits The number of bits.
	 */
	public void setBits(int bits) {
		
		net.setBits(bits);
	} // end of setBits method
	
	/**
	 * Make this wire end tri-state.
	 * 
	 * @param which True to make it tri-state, false otherwise.
	 */
	public void setTriState(boolean which) {
		
		loadTriState = which;
	} // end of setTriState method

	/**
	 * Find out if this wire end is tri-state.
	 * 
	 * @return true if it is, false if it is not.
	 */
	public boolean isTriState() {
		
		return net.isTriState();
	} // end of isTriState method
	
	/**
	 * Display information about this wire end.
	 * 
	 * @param info A JLabel to display with.
	 */
	public void showInfo(JLabel info) {
		
		String inp = "";
		if (!net.hasInput()) {
			inp = ", no input";
		}
		int bits = net.getBits();
		if (bits <= 0) {
			info.setText("not connected");
		}
		else if (bits == 1) {
			info.setText("1 bit" + inp);
		} 
		else {
			info.setText(bits + " bits" + inp);
		}
	} // end of showInfo method
	
	/**
	 * Set/reset marked flag (used to partition wire nets).
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
	 * Mark this wire end and all wires and wire ends connected to it.
	 */
	public void traverse() {
		
		if (marked)
			return;
		marked = true;
		for (Wire w : wires) {
			w.traverse();
		}
	} // end of traverse method
	
	/**
	 * Put this wire end in a new wire net.
	 * 
	 * @param the new wire net.
	 */
	public void setNet(WireNet net) {
		
		this.net = net;
	} // end of setNet method
	
	/**
	 * Get the wire net this wire end is in.
	 * 
	 * @return This wire end's wire net.
	 */
	public WireNet getNet() {
		
		return net;
	} // end of getNet method
	
	/**
	 * See if this wire end is dangling (i.e., has at most one wire and is not attached).
	 * 
	 * @return true if dangling, false if not.
	 */
	public boolean isDangling() {
		
		return wires.size() <= 1 && !isAttached();
	} // end of isDangling method
	
	/**
	 * Get all wires from this wire end.
	 * 
	 * @return the wires.
	 */
	public Set<Wire> getWires() {
		
		return wires;
	} // end of getWires method
	
	/**
	 * Get wire this wire end is connected to, or null if no connection.
	 * Assumes that there is only one (i.e., that this is a dangling end).
	 * 
	 * @return the wire, or null if there is none.
	 */
	public Wire getOnlyWire() {
		
		if (wires.size() == 0) {
			return null;
		}
		return (Wire)(wires.toArray()[0]);
	} // end of getOnlyWire method
	
	/**
	 * Get the rectangle bounding this element.
	 * 
	 * @return the bounding rectangle.
	 */
	public Rectangle getRect() {
		
		int d = JLSInfo.pointDiameter;
		int r = d/2;
		return new Rectangle(x-r,y-r,d,d);
	} // end of getRect method
	
	/**
	 * Get the number of wires coming out of this wire end.
	 * 
	 * @return the number of wires.
	 */
	public int degree() {
		
		return wires.size();
	} // end of degree method
	
	/**
	 * Get the copy of this wire end.
	 * 
	 * @return the copy.
	 */
	public WireEnd getCopy() {
		
		return myCopy;
	} // end of getCopy method
	
	/**
	 * Save this wire end in a file.
	 * 
	 * @param output A PrintWriter to write to.
	 */
	public void save(PrintWriter output) {
		
		output.println("ELEMENT WireEnd");
		super.save(output);
		if (getNet().isTriState()) {
			output.println(" int tristate 1");
		}
		if (isAttached()) {
			String putname = getPut().getName();
			output.println(" String put \"" + putname + "\"");
			int elid = getPut().getElement().getID();
			output.println(" ref attach " + elid);
			
		}
		for (Wire wire : wires) {
			int elid = wire.getOtherEnd(this).getID();
			output.println(" ref wire " + elid);
			if (wire.hasProbe()) {
				output.println(" probe " + elid + " \"" + wire.getProbe() + "\"");
			}
		}
		output.println("END");
	} // end of save method
	
	/**
	 * Set an instance variable value (during a load).
	 * 
	 * @param name The instance variable name.
	 * @param value The instance variable value.
	 */
	public void setValue(String name, int value) {
		
		if (name.equals("attach")) {
			loadAttach = value;
		} else if (name.equals("wire")) {
			loadWires.add(value);
		} else if (name.equals("tristate")) {
			if (value == 0)
				loadTriState = false;
			else
				loadTriState = true;
		} else {
			super.setValue(name,value);
		}
	} // end of setValue method
	
	/**
	 * See if this wire end is part of a tri-state net during a load.
	 * 
	 * @return true if it is, false if not.
	 */
	public boolean isLoadTriState() {
		
		return loadTriState;
	} // end of isLoadTriState method
	
	/**
	 * Set an instance variable value (during a load).
	 * 
	 * @param name The instance variable name.
	 * @param value The instance variable value.
	 */
	public void setValue(String name, String value) {
		
		if (name.equals("put")) {
			loadPut = value;
		} else {
			super.setValue(name,value);
		}
	} // end of setValue method
	
	/**
	 * Set probe on a wire.
	 * 
	 * @param id The id of the other end of the wire.
	 * @param name The probe name.
	 */
	public void setProbe(int id, String name) {
		
		probeMap.put(id,name);
	} // end of setProbe method

// -------------------------------------------------------------------------------
// Simulation
// -------------------------------------------------------------------------------
				
	/**
	 * Wire ends do nothing during simulation.
	 */
	public void initSim(Simulator sim) {
		
		// do nothing
	} // end of initSim method
	
} // end of WireEnd class
