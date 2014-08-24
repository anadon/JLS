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

	/**
	 * Set an int instance variable value (during a load).
	 * 
	 * @param name The name of the variable.
	 * @param value The value of the variable.
	 */
	public void setValue(String name, int value) {

		if (name.equals("id")) {
			id = value;
		} else if (name.equals("x")) {
			x = value;
		} else if (name.equals("y")) {
			y = value;
		} else if (name.equals("width")) {
			width = value;
		} else if (name.equals("height")) {
			height = value;
		} else if (name.equals("fixed")) {
			uneditable = true;
		} else if (name.equals("trpos")) {
			tracePosition = value;
		}
	} // end of setValue method

	/**
	 * Set a long instance variable value (during a load).
	 * 
	 * @param name The name of the variable.
	 * @param value The value of the variable.
	 */
	public void setValue(String name, long value) {

	} // end of setValue method

	/**
	 * Set a BigInteger instance variable value (during a load).
	 * 
	 * @param name The name of the variable.
	 * @param value The value of the variable.
	 */
	public void setValue(String name, BigInteger value) {

	} // end of setValue method

	/**
	 * Set a String instance variable value (during a load).
	 * 
	 * @param name The name of the variable.
	 * @param value The value of the variable.
	 */
	public void setValue(String name, String value) {

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

		it.x = x;
		it.y = y;
		it.width = width;
		it.height = height;
		it.uneditable = uneditable;
		it.tracePosition = tracePosition;
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
<<<<<<< HEAD
	 * TODO major point of optimization
	 * 
	 * @param other
	 *            The other element.
=======
	 * @param other The other element.
>>>>>>> 6fff4f8d5651621bfd72b14010a8a3fdd3ba837a
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

		output.println(" int id " + id);
		output.println(" int x " + x);
		output.println(" int y " + y);
		output.println(" int width " + width);
		output.println(" int height " + height);
		if (uneditable) {
			output.println(" int fixed 1");
		}
		if (tracePosition != -1) {
			output.println(" int trpos " + tracePosition);
		}
	} // end of save method

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
	 * @param g The Graphics object to use to determine size.
	 * @param editWindow The editor window this element is in.
	 * @param x The current x-coordinate of the mouse.
	 * @param y The current y-coordinate of the mouse.
	 */
	public void changeTiming(JPanel editWindow, int x, int y) {

		// display dialog
		Point pos = editWindow.getMousePosition();
		Point win = editWindow.getLocationOnScreen();
		if (pos == null) {
			new DelayChange(x+win.x,y+win.y,this instanceof Memory);
		}
		else {
			new DelayChange(pos.x+win.x,pos.y+win.y,this instanceof Memory);
		}

	} // end of changeTiming method

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
	private class DelayChange extends JDialog implements ActionListener {

		// properties
		private JButton ok = new JButton("OK");
		private JButton cancel = new JButton("Cancel");
		private JTextField delayField = new JTextField(10);
		private KeyPad delayPad = new KeyPad(delayField,10,0,this);

		/**
		 * Set up create dialog window.
		 * 
		 * @param x The x-coordinate of the position of the dialog.
		 * @param y The y-coordinate of the position of the dialog.
		 * @param isMemory True if this is a memory element, false if not.
		 */
		private DelayChange(int x, int y, boolean isMemory) {

			// set up window title
			super(JLSInfo.frame,"Change Timing",true);

			// set up window
			Container window = getContentPane();
			window.setLayout(new BoxLayout(window,BoxLayout.Y_AXIS));

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

			// set up ok and cancel buttons
			window.add(new JLabel(" "));
			JPanel okCancel = new JPanel(new GridLayout(1,2));
			ok.setBackground(Color.green);
			okCancel.add(ok);
			cancel.setBackground(Color.pink);
			okCancel.add(cancel);
			window.add(okCancel);
			getRootPane().setDefaultButton(ok);

			ok.addActionListener(this);
			cancel.addActionListener(this);
			delayField.addActionListener(this);

			// set up window close listener to cancel gate
			setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			addWindowListener (
					new WindowAdapter() {
						public void windowClosing(WindowEvent e) {
							dispose();
						}
					}
			);

			// finish up GUI
			pack();
			Dimension d = getSize();
			setLocation(x-d.width/2,y-d.height/2);
			setVisible(true);
		} // end of constructor

		/**
		 * React to ok, reset and cancel buttons.
		 * 
		 * @param event The event object for this action.
		 */
		public void actionPerformed(ActionEvent event) {

			if (event.getSource() == ok || event.getSource() == delayField) {
				int temp = 0;
				try {
					temp = Integer.parseInt(delayField.getText());
				}
				catch (NumberFormatException ex) {
					JOptionPane.showMessageDialog(this,
							"Value not numeric, try again", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (temp <= 0) {
					JOptionPane.showMessageDialog(this,
							"Propagation delay must be greater than 0", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				setDelay(temp);
				dispose();
			}
			else if (event.getSource() == cancel) {
				dispose();
			}

		} // end of actionPerformed method

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
	 * @param amount The new delay amount.
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
