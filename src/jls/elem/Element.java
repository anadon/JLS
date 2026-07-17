package jls.elem;

import jls.*;
import jls.edit.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.swing.*;

import java.util.*;
import java.math.*;

/**
 * Super class for all logic elements (including non-active ones).
 * Contains common display info and methods.
 * 
 * @author David A. Poplawski
 */
public class Element {

	// saved properties
	private int id; 						// unique for every element when written to file
	protected int x; 						// upper left corner of element
	protected int y;						//   (snap-to position for most elements)
	protected int width = 0; 				// size of element
	protected int height = 0;
	private boolean uneditable = false;		// to keep others from editing this element
	private int tracePosition = -1;			// position in signal trace (-1 if none)

	// running properties
	protected boolean highlight = false;	// whether the elements should be drawn highlighted
	private int savex;						// so it can be put back after an aborted move
	private int savey;
	protected Circuit circuit;				// the circuit this element is part of

	/**
	 * Create a new Element object.
	 * 
	 * @param circuit The circuit this element is part of.
	 */
	public Element(Circuit circuit) {

		this.circuit = circuit;
	} // end of constructor

	/**
	 * Set up this element (overridden by most elements).
	 */
	public boolean setup(Graphics g, JPanel editWindow, int x, int y) {

		return false;
	} // end of init method

	/**
	 * Set coordinates of this element.
	 * 
	 * @param x The x-coordinate of the upper left corner of this element.
	 * @param y The y-coordinate of the upper left corner of this element.
	 */
	public void setXY(int x, int y) {

		this.x = x;
		this.y = y;
	} // end of setXY method

	/**
	 * Change the circuit this element is in.
	 * 
	 * @param circuit The new circuit.
	 */
	public void setCircuit(Circuit circuit) {

		this.circuit = circuit;
	} // end of setCircuit method

	/**
	 * Get the circuit this element is part of.
	 * 
	 * @return the circuit.
	 */
	public Circuit getCircuit() {

		return circuit;
	} // end of getCircuit method

	/**
	 * Get the element's id.
	 * 
	 * @return the id.
	 */
	public int getID() {

		return id;
	} // end of getID method

	/**
	 * Get x-coordinate of this element.
	 * 
	 * @return the x-coordinate.
	 */
	public int getX() {

		return x;
	} // end of getX method

	/**
	 * Get y-coordinate of this element.
	 * 
	 * @return the y-coordinate.
	 */
	public int getY() {

		return y;
	} // end of getY method
	
	/**
	 * Get the trace position of this element.
	 * 
	 * @return the trace position (-1 if not traced)
	 */
	public int getTracePosition() {
		
		return tracePosition;
	} // end of getTracePosition method
	
	/**
	 * Set the trace position of this element.
	 * 
	 * @param position The position.
	 */
	public void setTracePosition(int position) {
		
		tracePosition = position;
	} // end of setTracePosition method

	// The attributes every element saves, in their historical save order
	// (#23). One declaration drives save, copy, and load dispatch.
	private static final java.util.List<Attribute> BASE_ATTRIBUTES =
			java.util.List.of(
		new Attribute.IntAttribute("id") {
			@Override
			protected int get(Element el) { return el.id; }
			@Override
			protected void set(Element el, int value) { el.id = value; }
			@Override
			public void copy(Element from, Element to) {
				// ids are assigned at save time, never copied
			}
		},
		new Attribute.IntAttribute("x") {
			@Override
			protected int get(Element el) { return el.x; }
			@Override
			protected void set(Element el, int value) { el.x = value; }
		},
		new Attribute.IntAttribute("y") {
			@Override
			protected int get(Element el) { return el.y; }
			@Override
			protected void set(Element el, int value) { el.y = value; }
		},
		new Attribute.IntAttribute("width") {
			@Override
			protected int get(Element el) { return el.width; }
			@Override
			protected void set(Element el, int value) { el.width = value; }
			@Override
			protected boolean omitted(Element el) {
				// recomputed by init() on load for some elements (#21)
				return el.sizeIsRecomputedOnLoad();
			}
		},
		new Attribute.IntAttribute("height") {
			@Override
			protected int get(Element el) { return el.height; }
			@Override
			protected void set(Element el, int value) { el.height = value; }
			@Override
			protected boolean omitted(Element el) {
				return el.sizeIsRecomputedOnLoad();
			}
		},
		new Attribute.IntAttribute("fixed") {
			@Override
			protected int get(Element el) { return el.uneditable ? 1 : 0; }
			@Override
			protected void set(Element el, int value) {
				// any saved value means uneditable, as the loader always did
				el.uneditable = true;
			}
			@Override
			protected boolean omitted(Element el) { return !el.uneditable; }
			@Override
			public void copy(Element from, Element to) {
				to.uneditable = from.uneditable;
			}
		},
		new Attribute.IntAttribute("trpos") {
			@Override
			protected int get(Element el) { return el.tracePosition; }
			@Override
			protected void set(Element el, int value) { el.tracePosition = value; }
			@Override
			protected boolean omitted(Element el) { return el.tracePosition == -1; }
		}
	);

	/**
	 * The attributes this element saves, in save order (#23). Subclasses
	 * that declare their own attributes return the base list plus their
	 * own; unconverted subclasses keep their handwritten save/copy/
	 * setValue methods and only inherit the base list.
	 *
	 * @return the attribute list.
	 */
	protected java.util.List<Attribute> savedAttributes() {

		return BASE_ATTRIBUTES;
	} // end of savedAttributes method

	/**
	 * Build a full attribute list for a subclass: the base attributes
	 * followed by the subclass's own, preserving save order (#23).
	 *
	 * @param own The subclass's own attributes.
	 *
	 * @return an immutable combined list.
	 */
	protected static java.util.List<Attribute> concatAttributes(
			java.util.List<Attribute> own) {

		java.util.List<Attribute> all =
				new java.util.ArrayList<Attribute>(BASE_ATTRIBUTES);
		all.addAll(own);
		return java.util.Collections.unmodifiableList(all);
	} // end of concatAttributes method

	/**
	 * Set an int instance variable value (during a load).
	 * 
	 * @param name The name of the variable.
	 * @param value The value of the variable.
	 */
	public void setValue(String name, int value) {

		for (Attribute attr : savedAttributes()) {
			if (attr.setInt(this, name, value)) {
				return;
			}
		}
	} // end of setValue method

	/**
	 * Set a long instance variable value (during a load).
	 * 
	 * @param name The name of the variable.
	 * @param value The value of the variable.
	 */
	public void setValue(String name, long value) {

		for (Attribute attr : savedAttributes()) {
			if (attr.setLong(this, name, value)) {
				return;
			}
		}
	} // end of setValue method

	/**
	 * Set a BigInteger instance variable value (during a load).
	 * 
	 * @param name The name of the variable.
	 * @param value The value of the variable.
	 */
	public void setValue(String name, BigInteger value) {

		for (Attribute attr : savedAttributes()) {
			if (attr.setBigInt(this, name, value)) {
				return;
			}
		}
	} // end of setValue method

	/**
	 * Set a String instance variable value (during a load).
	 * 
	 * @param name The name of the variable.
	 * @param value The value of the variable.
	 */
	public void setValue(String name, String value) {

		for (Attribute attr : savedAttributes()) {
			if (attr.setString(this, name, value)) {
				return;
			}
		}
	} // end of setValue method

	/**
	 * Set a pair of int instance variable values (during a load).
	 * 
	 * @param v1 The first value.
	 * @param v1 The second value.
	 */
	public void setPair(int v1, int v2) {

	} // end of setPair method

	/**
	 * Initialize internal information for this element.
	 * 
	 * @param g The graphics object to use.
	 */
	public void init(Graphics g) throws Exception{
		throw new Exception("ERROR: using undefined function from " + this.getName());
	}

	/**
	 * Placeholder for element copies.
	 */
	public Element copy() {

		return null;
	} // end of copy method

	/**
	 * Make a copy of this element in the parameter object.
	 * 
	 * @param it The element to copy info to.
	 */
	public void copy(Element it) {

		for (Attribute attr : savedAttributes()) {
			attr.copy(this, it);
		}
	} // end of copy method

	/**
	 * Save current coordinates in case move doesn't work.
	 */
	public void savePosition() {

		savex = x;
		savey = y;
	} // end of savePosition method

	/**
	 * Restore saved coordinates when move doesn't work.
	 */
	public void restorePosition() {

		x = savex;
		y = savey;
	} // end of restorePosition method

	/**
	 * Move element.
	 * 
	 * @param dx Distance to move in the x-direction.
	 * @param dy Distance to move in the y-direction.
	 */
	public void move(int dx, int dy) {

		x += dx;
		y += dy;
	} // end of move method

	/**
	 * Fix position of this element (overridden).
	 */
	public void fixPosition() {

	} // end of fixPosition method

	/**
	 * See if the given point is inside the element's display area.
	 * 
	 * @param x The x-coordinate of the given point.
	 * @param y The y-coordinate of the given point.
	 * 
	 * @return true if the point is in the display area, false if not.
	 */
	public boolean contains(int x, int y) {

		Rectangle thisRect = getRect();
		return thisRect.contains(x,y);
	} // end of contains method

	/**
	 * See if this element intersects another.
	 *
	 * @param other The other element.
	 *
	 * @return true if this element intersects the other, false if not.
	 */
	public boolean intersects(Element other) {

		// special case for intersecting with a wire
		if (other instanceof Wire) {
			Wire wire = (Wire)other;
			WireEnd end1 = wire.getEnd();
			WireEnd end2 = wire.getOtherEnd(end1);

			// no problem if this element is one end of the wire
			if (this instanceof WireEnd && (this == end1 || this == end2)) {
				return false;
			}

			// otherwise check for wire intersection with this element's bounding rectangle
			return wire.intersects(getRect());
		}

		// simply see if the elements' bounding rectangles intersect
		Rectangle thisRect = getRect();
		Rectangle otherRect = other.getRect();
		return thisRect.intersects(otherRect);
	} // end of intersects method

	/**
	 * See if this element is completely inside a given rectangle.
	 * 
	 * @param rect The given rectangle.
	 * 
	 * @return true if the element is inside, false if not.
	 */
	public boolean isInside(Rectangle rect) {

		Rectangle me = getRect();
		return rect.contains(me);
	} // end of isInside method

	/**
	 * Set/reset highlight.
	 * 
	 * @param light True if item should be highlighted, false otherwise.
	 */
	public void setHighlight(boolean light) {

		highlight = light;
		if (circuit != null) {
			circuit.noteHighlight(this, light);
		}
	} // end of setHightlight method

	/**
	 * Set id of this element (for file save).
	 * 
	 * @param id The id.
	 */
	public void setID(int id) {

		this.id = id;
	} // end of setID method

	/**
	 * Save all information about this element in a file.
	 * 
	 * @param output The print writer to use.
	 */
	public void save(PrintWriter output) {

		for (Attribute attr : savedAttributes()) {
			attr.save(this, output);
		}
	} // end of save method

	/**
	 * Whether this element's init() recomputes width/height
	 * unconditionally on load. If so, saving them is redundant and they
	 * are omitted to shrink saved files (#21); the loader has always
	 * tolerated absent attributes, so older JLS versions still read the
	 * files.
	 *
	 * @return false unless a subclass overrides.
	 */
	protected boolean sizeIsRecomputedOnLoad() {

		return false;
	} // end of sizeIsRecomputedOnLoad method

	/**
	 * Highlight this element on the screen.
	 * Subclasses draw the element itself.
	 * 
	 * @param g The Graphics object to draw with.
	 */
	public void draw(Graphics g) {

		// highlight if necessary
		if (highlight) {
			g.setColor(Color.pink);
			Graphics2D gg = (Graphics2D)g;
			gg.fill(getRect());
		}
	} // end of draw method

	/**
	 * This element will be removed, so do whatever is needed.
	 * 
	 * @param circ A reference back to the circuit the element is in.
	 */
	public void remove(Circuit circ) {

		circ.remove(this);
	} // end of remove method

	/**
	 * Get put near given x,y position (if one) - overridden.
	 */
	public Put getPut(int x, int y) {

		return null;
	} // end of getPut method

	/**
	 * Display infomation about element (overridden).
	 * 
	 * @param info A JLabel to display with.
	 */
	public void showInfo(JLabel info) {

	} // end of showInfo method

	/**
	 * See if this element is touching (for possible connections).
	 * This is overridden in wire ends.
	 */
	public boolean isTouching() {

		return false;
	} // end of isTouching method

	/**
	 * Untouch all inputs and outputs.
	 */
	public void untouchPuts() {}

	/**
	 * Get all inputs and outputs.
	 * Generally overridden.
	 * 
	 * @return a set of all inputs and outputs.
	 */
	public Set<Put> getAllPuts() { return new HashSet<Put>(); }

	/**
	 * Get the rectangle bounding this element.
	 *  
	 * @return the bounding rectangle.
	 */
	public Rectangle getRect() {

		return new Rectangle(x,y,width,height);
	} // end of getRect method

	/**
	 * The bounds this element occupies in the spatial index (#3, #17).
	 * Must contain every point for which contains() can be true and every
	 * rectangle this element can intersect. The default is the bounding
	 * rectangle; wires override it since their extent comes from their
	 * ends, not from x/y/width/height.
	 *
	 * @return the index bounds.
	 */
	public Rectangle getIndexBounds() {

		return getRect();
	} // end of getIndexBounds method

	/**
	 * Set/reset touching flag(s) for this element.
	 * Overridden by wire ends and logic elements.
	 * 
	 * @param setting True to set, false to reset.
	 */
	public void setTouching(boolean setting) {}

	/**
	 * Get put by name.
	 * 
	 * @param name Name of the put.
	 * 
	 * @return The put.
	 */
	public Put getPut(String name) {

		return null;
	} // end of getPut method

	/**
	 * Check whether the element can be changed after it is created and placed.
	 * Default is not.
	 * Overriden by elements that can.
	 * 
	 * @return true if element can be change, false otherwise.
	 */
	public boolean canChange() {

		return false;
	} // end of canChange method

	/**
	 * Change element characteristics.
	 * Overridden in elements that can change.
	 * 
	 * @param g A Graphics object to use for sizing.
	 * @param editWindow The editor window.
	 * @param x The current x-coordinate of the cursor.
	 * @param y The current y-coordinate of the cursor.
	 * 
	 * @return true if the element did change, false if not.
	 */
	public boolean change(Graphics g, JPanel editWindow, int x, int y) {

		return false;
	} // end of change method

	/**
	 * Check whether element has a quick change (shortcut) option.
	 * Overridden by elements than can.
	 * 
	 * @return true if is does, false if not.
	 */
	public boolean quickChange() {

		return false;
	} // end of quicChange method

	/**
	 * Set up menu item for quick changes.
	 * Overridden by elements that can do it.
	 * Should never be called.
	 * 
	 * @return a menu item (can be a menu with submenu items)
	 * 
	 * @throws UnsupportedOperationException if called and not overridden.
	 */
	public JMenuItem setupQuickMenu(SimpleEditor sed) {

		throw new UnsupportedOperationException("setupQuickMenu");
	} // end of setupQuickMenu method

	/**
	 * Check whether the element has timing info, i.e., propagation delay or access time.
	 * Default is do not.
	 * Overridden by elements tht do.
	 * 
	 * @return true if the element has timing info, false if not.
	 */
	public boolean hasTiming() {

		return false;
	} // end of hasTiming method

	/**
	 * Show timing change dialog.
	 * 
	 * @param editWindow The editor window this element is in.
	 * @param x The current x-coordinate of the mouse.
	 */
	public void changeTiming(JPanel editWindow, int x) {

		// display dialog
		new DelayChange(this instanceof Memory);

	} // end of changeTiming method

	/**
	 * The save-format version this element's current state requires
	 * (issue #79 evolution policy: a writer emits the highest version
	 * whose features the file uses). Almost everything is expressible
	 * in version 1; an element whose state older readers would
	 * silently mis-load overrides this (issue #124: vertical groups).
	 *
	 * @return the minimum format version that can carry this element.
	 */
	public int saveFormatVersion()
	{
		return 1;
	}

	/**
	 * Tells if an element is capable of rotatating, defaults to false.
	 * This method may be overridden if an element supports rotation
	 * @return True if an element supports rotation otherwise false
	 */
	public boolean canRotate()
	{
		return false;
	}

	/**
	 *  This method will rotate the element if it is supported.
	 *  This must be overridden by supporting classes.
	 * @param direction The direction to rotate
	 * @param g The current graphics context for use in recalculating size
	 */
	public void rotate(JLSInfo.Orientation direction, Graphics g)
	{
		throw new UnsupportedOperationException("Rotate");
	}

	/**
	 * Tells if an element is capable of flipping, defaults to false.
	 * This method may be overridden if an element supports flipping
	 * @return True if an element supports flipping otherwise false
	 */
	public boolean canFlip()
	{
		return false;
	}

	/**
	 * This method will flip an element, it must be overridden by classes that support it
	 * @param g The current graphics context to facilitate recalculation of size when flipping
	 */
	public void flip(Graphics g)
	{
		throw new UnsupportedOperationException("Flip");
	}

	/**
	 * Display dialog letting user change the propagation delay or access time.
	 */
	@SuppressWarnings("serial")
	private class DelayChange extends ElementDialog {

		// properties
		private JTextField delayField = new JTextField(10);
		private KeyPad delayPad = new KeyPad(delayField,10,0,this);

		/**
		 * Set up create dialog window.
		 * 
		 * @param isMemory True if this is a memory element, false if not.
		 */
		private DelayChange(boolean isMemory) {

			// set up window title
			super("Change Timing",null);

			// set up window
			Container window = getContentPane();

			// set up input
			JPanel info = new JPanel(new BorderLayout());
			JLabel delay;
			if (isMemory) {
				delay = new JLabel("Memory access time: ",SwingConstants.RIGHT);
			}
			else {
				delay = new JLabel("Propagation delay: ",SwingConstants.RIGHT);
			}
			info.add(delay,BorderLayout.WEST);
			info.add(delayField,BorderLayout.CENTER);
			delayField.setText(getDelay()+"");
			info.add(delayPad,BorderLayout.EAST);
			window.add(info);

			confirmOnEnter(delayField);
			finishDialog();
		} // end of constructor

		/**
		 * Validate and apply the new delay.
		 */
		@Override
		protected void validateAndAccept() {

			int temp = 0;
			try {
				temp = Integer.parseInt(delayField.getText());
			}
			catch (NumberFormatException ex) {
				reject("Value not numeric, try again");
				return;
			}
			if (temp <= 0) {
				reject("Propagation delay must be greater than 0");
				return;
			}
			setDelay(temp);
			dispose();
		} // end of validateAndAccept method

	} // end of DelayChange class

	/**
	 * Get the propagation delay or access time in this element.
	 * Overridden by elements with timing info.
	 * 
	 * @return the current delay.
	 */
	public int getDelay() {

		return 0;
	} // end of getDelay method

	/**
	 * Set the propagation delay or access time in this element.
	 * Overridden by elements with timing info.
	 * 
	 * @param temp The new delay amount.
	 *        Must be Integer, don't change to int!
	 */
	public void setDelay(int temp) {

		// do nothing
	} // end of setDelay method

	/**
	 * Check wether the element can be watched.
	 * Default is not.
	 * Overridden by elements that can be.
	 * 
	 * @return false;
	 */
	public boolean canWatch() {

		return false;
	} // end of canWatch method

	/**
	 * See if element is currently watched.
	 * Default is not.
	 * Overridden by elements that can be watched.
	 * 
	 * @return false.
	 */
	public boolean isWatched() {

		return false;
	} // end of isWatched method

	/**
	 * Set whether this element is watched or not.
	 * Overridden by elements that can be watched.
	 * 
	 * @param state Unused.
	 */
	public void setWatched(boolean state) {

	} // end of setWatched method

	/**
	 * Display the current value of this element.
	 * Only works for watchable elements, and is overridden there.
	 * 
	 * @param where The point on the screen to display at.
	 */
	public void showCurrentValue(Point where) {

	} // end of showCurrent value method

	/**
	 * Get the name of this element.
	 * Overridden by elements that actually have names.
	 * 
	 * @return null;
	 */
	public String getName() {

		return null;
	} // end of getName method

	/**
	 * See if this element is uneditable.
	 * 
	 * @return true if it is uneditable, false if not.
	 */
	public boolean isUneditable() {

		return uneditable;
	} // end of isUneditable method

	/**
	 * Make this element uneditable.
	 * This cannot be undone without editting the saved file.
	 */
	public void makeUneditable() {

		uneditable = true;
	} // end of makeUneditable method

} // end of Element class
