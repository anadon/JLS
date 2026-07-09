package jls.elem;

import jls.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.util.BitSet;

import javax.swing.*;

/**
 * Superclass of input and output pins.
 * Contains common data and methods.
 * 
 * @author David A. Poplawski
 */
public abstract class Pin extends LogicElement {
	
	// saved properties
	protected String name;
	protected int bits;
	protected boolean watched = false;
	protected JLSInfo.Orientation orientation = JLSInfo.Orientation.RIGHT;
	
	// editting properties
	protected boolean cancelled;
	
	/**
	 * Create a new input pin.
	 * 
	 * @param circ The circuit this pin will be in.
	 */
	public Pin(Circuit circ) {
		
		super(circ);
	} // end of constructor
	
	/**
	 * Return a string version of this element.
	 * 
	 * @return the string.
	 */
	public String toString() {
		
		return name + ",bits=" + bits + ",watched=" + watched + ",hashCode=" + hashCode();
	} // end of toString method
	
	/**
	 * Get the name of this pin.
	 * 
	 * @return the name.
	 */
	public String getName() {
		
		return name;
	} // end of getName method
	
	/**
	 * Get the number of bits in this pin.
	 * 
	 * @return the number of bits.
	 */
	public int getBits() {
		
		return bits;
	} // end of getBits method
	
	/**
	 * Display dialog to get pin name and bits.
	 * 
	 * @param g The Graphics object to use to determine the name's size.
	 * @param editWindow The editor window this pin is displayed in.
	 * @param x The x-coordinate of the last known mouse position.
	 * @param y The y-coordinate of the last known mouse position.
	 * @param type The type of pin ("Input" or "Output").
	 * 
	 * @return false if cancelled, true otherwise.
	 */
	public boolean setup(Graphics g, JPanel editWindow, int x, int y, String type) {
		
		// show creation dialog
		Point pos = editWindow.getMousePosition();
		Point win = editWindow.getLocationOnScreen();
		if (pos == null) {
			new PinCreate(x+win.x,y+win.y,type);
		}
		else {
			new PinCreate(pos.x+win.x,pos.y+win.y,type);
		}
		
		// don't do anything if user cancelled gate
		if (cancelled)
			return false;
		
		// finish up
		init(g);
		
		// save position
		Point p = MouseInfo.getPointerInfo().getLocation();
		p.x -= win.x;
		p.y -= win.y;
		if (p != null) {
			super.setXY(p.x-width/2,p.y-height/2);
		}
		
		return true;
	} // end of setup method
	
	/**
	 * Initialize internal info for this element.
	 * Computes the size of the pin as a function of the name.
	 * 
	 * @param g Graphics object used to compute the size of the name.
	 */
	public void init(Graphics g) {
		
		// set up size if needed
		if (g != null) {
			
			if (width == 0 && height == 0) {
				int s = JLSInfo.spacing;
				FontMetrics fm = g.getFontMetrics();
				int w = fm.stringWidth(" " + name + " ");
				if(orientation == JLSInfo.Orientation.LEFT || orientation == JLSInfo.Orientation.RIGHT)
				{
					width = Math.max((w+s/2)/s*s,2*s)+s;	// ceiling in spacings
					height = 2*s;
				}
				else
				{
					width = Math.max((w+s/2)/s*s,2*s);
					if(width % (2*s) != 0)
					{
						width += s;
					}
					height = 3*s;
				}
			}
		}
	} // end of init method
	
	/**
	 * Set an int instance variable value (during a load).
	 * 
	 * @param name The instance variable name.
	 * @param value The instance variable value.
	 */
	// Declarative persistence (#23): one declaration drives save, load
	// dispatch, and copy for the attributes shared by both pins.
	private static final java.util.List<Attribute> OWN_ATTRIBUTES =
			java.util.List.of(
		new Attribute.StringAttribute("name") {
			protected String get(Element el) { return ((Pin)el).name; }
			protected void set(Element el, String v) {
				// loading a name registers it with the circuit
				((Pin)el).name = v;
				el.getCircuit().addName(v);
			}
			public void copy(Element from, Element to) {
				// the handwritten pin copies assigned the field
				// without registering the name
				((Pin)to).name = ((Pin)from).name;
			}
		},
		new Attribute.IntAttribute("bits") {
			protected int get(Element el) { return ((Pin)el).bits; }
			protected void set(Element el, int v) { ((Pin)el).bits = v; }
		},
		new Attribute.IntAttribute("watch") {
			protected int get(Element el) { return ((Pin)el).watched ? 1 : 0; }
			protected void set(Element el, int v) { ((Pin)el).watched = v != 0; }
		},
		new Attribute.OrientationAttribute("orient") {
			protected JLSInfo.Orientation getOrientation(Element el) {
				return ((Pin)el).orientation;
			}
			protected void setOrientation(Element el, JLSInfo.Orientation o) {
				((Pin)el).orientation = o;
			}
		}
	);

	private static final java.util.List<Attribute> ALL_ATTRIBUTES =
			concatAttributes(OWN_ATTRIBUTES);

	/**
	 * Base attributes plus the shared pin attributes, in save order
	 * (#23).
	 */
	protected java.util.List<Attribute> savedAttributes() {

		return ALL_ATTRIBUTES;
	} // end of savedAttributes method
	
	/**
	 * Pins cannot be copied (copy/paste).
	 * 
	 * @return false.
	 */
	public boolean canCopy() {
		
		return false;
	} // end of canCopy method
	
	/**
	 * Save this element in a file.
	 *
	 * @param output The output writer.
	 */
	public void save(PrintWriter output) {

		super.save(output);
		output.println("END");
	} // end of save method

	/**
	 * The pin kind for user-facing messages ("Input" or "Output").
	 */
	protected abstract String pinKind();

	/**
	 * Load-time tri-state marker (" int tristate 1" in saved files).
	 */
	protected boolean loadTriState = false;

	/**
	 * Set an int instance variable value (during a load).
	 *
	 * @param name The instance variable name.
	 * @param value The instance variable value.
	 */
	public void setValue(String name, int value) {

		if (name.equals("tristate")) {
			loadTriState = true;
		} else {
			super.setValue(name, value);
		}
	} // end of setValue method

	/**
	 * Draw this pin: the orientation-pointed pentagon, watched
	 * background, centered name, and its single put (#27 S4 - the two
	 * pin classes drew byte-identical shapes).
	 *
	 * @param g The graphics object to draw with.
	 */
	public void draw(Graphics g) {

		// set up shape
		int s = JLSInfo.spacing;
		Polygon p = new Polygon();
		if (orientation == JLSInfo.Orientation.RIGHT) {
			p.addPoint(x, y);
			p.addPoint(x + width - s, y);
			p.addPoint(x + width, y + height / 2);
			p.addPoint(x + width - s, y + height);
			p.addPoint(x, y + height);
		} else if (orientation == JLSInfo.Orientation.LEFT) {
			p.addPoint(x + s, y);
			p.addPoint(x, y + height / 2);
			p.addPoint(x + s, y + height);
			p.addPoint(x + width, y + height);
			p.addPoint(x + width, y);
		} else if (orientation == JLSInfo.Orientation.UP) {
			p.addPoint(x + width / 2, y);
			p.addPoint(x + width, y + s);
			p.addPoint(x + width, y + height);
			p.addPoint(x, y + height);
			p.addPoint(x, y + s);
		} else if (orientation == JLSInfo.Orientation.DOWN) {
			p.addPoint(x, y);
			p.addPoint(x + width, y);
			p.addPoint(x + width, y + height - s);
			p.addPoint(x + width / 2, y + height);
			p.addPoint(x, y + height - s);
		}
		// draw watched background
		if (watched) {
			g.setColor(JLSInfo.watchColor);
			g.fillPolygon(p);
		}

		// draw context
		super.draw(g);

		// draw box
		g.setColor(Color.BLACK);
		g.drawPolygon(p);

		// draw name inside box
		FontMetrics fm = g.getFontMetrics();
		Rectangle2D t = fm.getStringBounds(name, g);
		double tw = t.getWidth();
		double th = t.getHeight();
		int bx = x;
		int by = y;
		int bwidth = width;
		int bheight = height;
		if (orientation == JLSInfo.Orientation.LEFT) {
			bx = x + s;
			bwidth = width - s;
		}
		else if (orientation == JLSInfo.Orientation.RIGHT) {
			bwidth = width - s;
		}
		else if (orientation == JLSInfo.Orientation.UP) {
			by = y + s;
			bheight = height - s;
		}
		else { // DOWN
			bheight = height - s;
		}
		int dx = (int) ((bwidth - tw) / 2);
		int dy = (int) ((bheight - th) / 2 + fm.getAscent());

		g.drawString(name, bx + dx, by + dy);

		// draw the put
		for (Input in : inputs) {
			in.draw(g);
		}
		for (Output out : outputs) {
			out.draw(g);
		}
	} // end of draw method

	/**
	 * Print current value to stdout.
	 *
	 * @param qual Qualified subcircuit name.
	 */
	public void printValue(String qual) {

		if (qual.equals("")) {
			System.out.printf("%s Pin %s: %s\n", pinKind(), name,
					BitSetUtils.toDisplay(currentValue, bits));
		}
		else {
			System.out.printf("%s Pin %s.%s: %s\n", pinKind(), qual, name,
					BitSetUtils.toDisplay(currentValue, bits));
		}
	} // end of printValue method

	/**
	 * Remove this element, but only if it is not part of a subcircuit.
	 *
	 * @param circ The circuit this element is in.
	 */
	public void remove(Circuit circ) {

		if (circ.isImported()) {
			TellUser.error(JLSInfo.frame,
					"Can't remove " + pinKind().toLowerCase() + " pin "
					+ name + " from a subcircuit", "Error");
			return;
		}
		circ.removeName(name);
		super.remove(circ);
	} // end of remove method

	/**
	 * A pin can rotate or flip only when its put has no attachment.
	 *
	 * @return false if the put has a wire attached, true otherwise.
	 */
	public boolean canRotate() {

		for (Input in : inputs) {
			if (in.isAttached()) {
				return false;
			}
		}
		for (Output out : outputs) {
			if (out.isAttached()) {
				return false;
			}
		}
		return true;
	} // end of canRotate method

	/**
	 * A pin can rotate or flip only when its put has no attachment.
	 *
	 * @return false if the put has a wire attached, true otherwise.
	 */
	public boolean canFlip() {

		return canRotate();
	} // end of canFlip method

	/**
	 * Rotate this pin, rebuilding its geometry and put.
	 *
	 * @param direction The direction to rotate.
	 * @param g The current graphics context for size recalculation.
	 */
	public void rotate(JLSInfo.Orientation direction, Graphics g) {

		if (direction == JLSInfo.Orientation.LEFT) {
			orientation = orientation.ccw();
		}
		else if (direction == JLSInfo.Orientation.RIGHT) {
			orientation = orientation.cw();
		}
		inputs.clear();
		outputs.clear();
		width = 0;
		height = 0;
		init(g);
	} // end of rotate method

	/**
	 * Flip this pin, rebuilding its geometry and put.
	 *
	 * @param g The current graphics context for size recalculation.
	 */
	public void flip(Graphics g) {

		orientation = orientation.flipped();
		inputs.clear();
		outputs.clear();
		width = 0;
		height = 0;
		init(g);
	} // end of flip method

	// -----------------------------------------------------------------
	// Simulation state shared by both pins
	// -----------------------------------------------------------------

	protected BitSet currentValue = new BitSet();

	/**
	 * Get the current value.
	 *
	 * @return the current value.
	 */
	public BitSet getCurrentValue() {

		if (currentValue == null)
			return null;
		else
			return (BitSet) currentValue.clone();
	} // end of getCurrentValue method
	
	/**
	 * Display info about this element.
	 * 
	 * @param info The JLabel to display with.
	 */
	public void showInfo(JLabel info) {
		
		info.setText(bits + " bit input pin");
	} // end of showInfo method
	
	/**
	 * Dialog box to set input pin characteristics.
	 */
	private class PinCreate extends ElementDialog {
		
		// properties
		private JTextField nameField = new JTextField("",12);
		private JTextField bitsField = new JTextField("1",5);
		private KeyPad bitsPad = new KeyPad(bitsField,10,1,this);
		private JRadioButton left = new JRadioButton("Left");
		private JRadioButton right = new JRadioButton("Right", true);
		private JRadioButton up = new JRadioButton("Up");
		private JRadioButton down = new JRadioButton("Down");
		
		/**
		 * Set up dialog window.
		 * 
		 * @param x The x-coordinate of the position of the dialog.
		 * @param y The y-coordinate of the position of the dialog.
		 * @param type The type of pin ("Input" or "Output").
		 */
		private PinCreate(int x, int y, String type) {
			
			// set up window title
			super("Create " + type + " Pin",type);
			
			// set not cancelled
			cancelled = false;
			
			// set up window
			Container window = getContentPane();
			
			// set up inputs
			JPanel info = new JPanel(new BorderLayout());
			JPanel labels = new JPanel(new GridLayout(2,1,1,5));
			JLabel name = new JLabel("Name: ",SwingConstants.RIGHT);
			labels.add(name);
			JLabel bits = new JLabel("Bits: ",SwingConstants.RIGHT);
			labels.add(bits);
			info.add(labels,BorderLayout.WEST);
			
			JPanel data = new JPanel(new GridLayout(2,1,1,5));
			data.add(nameField);
			JPanel bitsPanel = new JPanel(new BorderLayout());
			bitsPanel.add(bitsField,BorderLayout.CENTER);
			bitsPanel.add(bitsPad,BorderLayout.EAST);
			data.add(bitsPanel);
			info.add(data,BorderLayout.CENTER);
			window.add(info);
			
			//Setup orientation radio buttons
			JLabel olbl = new JLabel("Orientation");
			olbl.setAlignmentX(Component.CENTER_ALIGNMENT);
			window.add(olbl);
			JPanel orients = new JPanel(new GridLayout(3,3));
			orients.add(new JLabel(""));
			orients.add(up);
			orients.add(new JLabel(""));
			orients.add(left);
			orients.add(new JLabel(""));
			orients.add(right);
			orients.add(new JLabel(""));
			orients.add(down);
			orients.add(new JLabel(""));
			left.setHorizontalAlignment(SwingConstants.CENTER);
			right.setHorizontalAlignment(SwingConstants.CENTER);
			up.setHorizontalAlignment(SwingConstants.CENTER);
			down.setHorizontalAlignment(SwingConstants.CENTER);
			ButtonGroup gr = new ButtonGroup();
			gr.add(left);
			gr.add(right);
			gr.add(down);
			gr.add(up);
			window.add(orients);
			
			confirmOnEnter(nameField);
			finishDialog(x,y);
		} // end of constructor
		
		/**
		 * Validate the form and create the pin.
		 */
		protected void validateAndAccept() {
			
			try {
				bits = Integer.parseInt(bitsField.getText());
			}
			catch (NumberFormatException ex) {
				reject("Bits not numeric, try again");
				return;
			}
			if (bits < 1) {
				reject("Must have at least 1 bit");
				return;
			}
			String tname = nameField.getText().trim();
			if (tname.length() < 1 || !Util.isValidName(tname)) {
				reject("Missing or invalid name, try again");
				return;
			}
			if (!circuit.addName(tname)) {
				reject("Name already used, try again");
				return;
			}
			name = tname;
			if(left.isSelected())
			{
				orientation = JLSInfo.Orientation.LEFT;
			}
			else if(right.isSelected())
			{
				orientation = JLSInfo.Orientation.RIGHT;
			}
			else if(up.isSelected())
			{
				orientation = JLSInfo.Orientation.UP;
			}
			else if(down.isSelected())
			{
				orientation = JLSInfo.Orientation.DOWN;
			}
			dispose();
		} // end of validateAndAccept method
		
		/**
		 * Cancel this pin.
		 */
		protected void cancelDialog() {
			
			cancelled = true;
			dispose();
		} // end of cancelDialog method
		
	} // end of PinCreate class
	
	/**
	 * A pin be watched.
	 * 
	 * @return true.
	 */
	public boolean canWatch() {
		
		return true;
	} // end of canWatch method
	
	/**
	 * See if this pin is watched.
	 * 
	 * @return true if it is, false if it is not.
	 */
	public boolean isWatched() {
		
		return watched;
	} // end of isWatched method
	
	/**
	 * Set whether this pin is watched or not.
	 * 
	 * @param state True to make it watched, false to make it not watched.
	 */
	public void setWatched(boolean state) {
		
		watched = state;
	} // end of setWatched method
	
} // end of Pin class
