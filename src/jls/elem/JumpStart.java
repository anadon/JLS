package jls.elem;

import jls.*;
import jls.sim.*;
import jls.util.Placement;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.*;

import java.io.PrintWriter;
import java.util.*;

/**
 * Starting point of a named wire.
 * 
 * @author David A. Poplawski
 */
public final class JumpStart extends LogicElement
		implements TriProp, Watchable {
	
	// default value
	/** The default bit width for a new jump start. */
	private static final int defaultBits = 1;

	// saved properties
	/** The wire name (shared with the matching jump ends). */
	private String name;
	/** The bit width of the wire. */
	private int bits = defaultBits;
	/** True if this jump start's value is watched during simulation. */
	private boolean watched = false;

	// running properties
	/** True if the user cancelled the creation dialog. */
	private boolean cancelled;
	/** A reference to this jump start, for use inside the dialog inner class. */
	private JumpStart me;

	/** Which direction the element faces. */
	private JLSInfo.Orientation orientation = JLSInfo.Orientation.LEFT;
	
	/**
	 * Create a new adder element.
	 * 
	 * @param circuit The circuit this element is part of.
	 */
	public JumpStart(Circuit circuit) {
		
		super(circuit);
	} // end of constructor
	
	/**
	 * Display dialog to get characteristics.
	 * 
	 * @param g The Graphics object to use to initialize sizes
	 * @param editWindow The editor window this constant will be displayed in.
	 * @param x The x-coordinate of the last known mouse position.
	 * @param y The y-coordinate of the last known mouse position.
	 * 
	 * @return false if cancelled, true otherwise.
	 */
	@Override
	public boolean setup(Graphics g, JPanel editWindow, int x, int y) {
		
		// show creation dialog
		me = this;
		new StartCreate();
		
		// don't do anything if user cancelled
		if (cancelled)
			return false;
		
		// complete initialization
		init(g);
		
		// save position
		Point p = Placement.dropPoint(editWindow,x,y,width,height);
		super.setXY(p.x,p.y);
		
		return true;
	} // end of setup method
	
	/**
	 * Initialize internal info for this element.
	 * 
	 * @param g The Graphics object to use.
	 */
	@Override
	public void init(Graphics g) {
		
		if (g != null) {
			
			if (width == 0 && height == 0) {
				
				// set up size
				int s = JLSInfo.spacing;
				FontMetrics fm = g.getFontMetrics();
				int w = fm.stringWidth(" " + name + " ")+s;
				width = Math.max((w+s/2)/s*s,2*s);	// ceiling in spacings
				height = 0;	// not really, but bounding rectangle will be large enough
			}
			
		}
		
		// create input
		if(orientation == JLSInfo.Orientation.LEFT) {
			inputs.add(new Input("input",this,0,0,bits));
		}
		else if(orientation == JLSInfo.Orientation.RIGHT) {
			inputs.add(new Input("input",this,width,0,bits));
		}
		
		// save name in jumpstart list in this circuit
		circuit.addJumpStart(name,this);
		
	} // end of init method
	
	/**
	 * Draw this element.
	 * 
	 * @param g The graphics object to draw with.
	 */
	@Override
	public void draw(Graphics g) {
		
		// draw context
		super.draw(g);
		
		// highlight if corresponding end is selected
		for (Element el : circuit.getElements()) {
			if (!(el instanceof JumpEnd jend))
				continue;
			if (name.equals(jend.getName())) {
				if (el.highlight) {
					g.setColor(Color.orange);
					Graphics2D gg = (Graphics2D)g;
					gg.fill(getRect());
				}
			}
		}
		
		// set up corners
		int s = JLSInfo.spacing;
		int top = y-s/2;
		int bottom = y+s/2;
		
		// draw watched background
		if (watched) {
			g.setColor(JLSInfo.watchColor);
			g.fillRect(x, top, width-s, bottom-top);
		}
		
		// draw box
		if(orientation == JLSInfo.Orientation.LEFT) {
			g.setColor(Color.BLACK);
			g.drawLine(x,top,x,bottom);
			g.drawLine(x,top,x+width-s,top);
			g.drawLine(x,bottom,x+width-s,bottom);
			g.drawArc(x+width-3*s/2,top,s,s,-90,180);
			g.drawLine(x+width-s/2,y,x+width,y);
			g.drawLine(x+width,y,x+width-s/4,y-s/4);
			g.drawLine(x+width,y,x+width-s/4,y+s/4);
		}
		else if(orientation == JLSInfo.Orientation.RIGHT){
			g.setColor(Color.BLACK);
			g.drawLine(x+width,top,x+width,bottom);
			g.drawLine(x+s,top,x+width,top);
			g.drawLine(x+s,bottom,x+width,bottom);
			g.drawArc(x+s/2,top,s,s,-90,-180);
			g.drawLine(x,y,x+s/2,y);
			g.drawLine(x,y,x+s/4,y-s/4);
			g.drawLine(x,y,x+s/4,y+s/4);
		}
		
		// draw name
		FontMetrics fm = g.getFontMetrics();
		int ascent = fm.getAscent();
		int h = fm.getDescent() + ascent;
		int w = fm.stringWidth(name);
		int tx = 0;
		if(orientation == JLSInfo.Orientation.LEFT)
			tx = x+(width-s-w)/2+JLSInfo.pointDiameter/2;
		else 
			tx = x+(width+0-w)/2+JLSInfo.pointDiameter/2;
		g.drawString(name,tx,y-h/2+ascent);
		
		// draw input
		inputs.get(0).draw(g);
		
	} // end of draw method
	
	/**
	 * Get the rectangle bounding this element.
	 * 
	 * @return the rectangle bounding this element.
	 */
	@Override
	public Rectangle getRect() {
		
		return new Rectangle(x,y-JLSInfo.spacing/2,width,height+JLSInfo.spacing);
	} // end of getRect method
	
	// Declarative persistence (#23): one declaration drives save, load
	// dispatch, and copy for this element's own attributes.
	/** This element's own saved attributes, in save order. */
	private static final java.util.List<Attribute> OWN_ATTRIBUTES =
			java.util.List.of(
		new Attribute.StringAttribute("name") {
			/**
			 * Read the name of the given jump start for saving.
			 *
			 * @param el The jump start being saved.
			 * @return the jump start's name.
			 */
			@Override
			protected String get(Element el) { return ((JumpStart)el).name; }
			/**
			 * Store a loaded name into the given jump start and register
			 * it with the circuit.
			 *
			 * @param el The jump start being loaded.
			 * @param v The name read from the file.
			 */
			@Override
			protected void set(Element el, String v) {
				// loading a name registers it with the circuit
				((JumpStart)el).name = v;
				el.getCircuit().addName(v);
			}
			/**
			 * Copy the name field from one jump start to another without
			 * registering the name with the circuit.
			 *
			 * @param from The jump start to copy from.
			 * @param to The jump start to copy into.
			 */
			@Override
			public void copy(Element from, Element to) {
				// the handwritten copy assigned the field without
				// registering the name
				((JumpStart)to).name = ((JumpStart)from).name;
			}
		},
		new Attribute.IntAttribute("bits") {
			/**
			 * Read the bit width of the given jump start for saving.
			 *
			 * @param el The jump start being saved.
			 * @return the jump start's bit width.
			 */
			@Override
			protected int get(Element el) { return ((JumpStart)el).bits; }
			/**
			 * Store a loaded bit width into the given jump start.
			 *
			 * @param el The jump start being loaded.
			 * @param v The bit width read from the file.
			 */
			@Override
			protected void set(Element el, int v) { ((JumpStart)el).bits = v; }
		},
		new Attribute.IntAttribute("watch") {
			/**
			 * Read the watched flag of the given jump start as 1 or 0 for
			 * saving.
			 *
			 * @param el The jump start being saved.
			 * @return 1 if watched, 0 otherwise.
			 */
			@Override
			protected int get(Element el) { return ((JumpStart)el).watched ? 1 : 0; }
			/**
			 * Store a loaded watched flag into the given jump start.
			 *
			 * @param el The jump start being loaded.
			 * @param v Non-zero to mark it watched, zero otherwise.
			 */
			@Override
			protected void set(Element el, int v) { ((JumpStart)el).watched = v != 0; }
		},
		new Attribute.OrientationAttribute("orientation") {
			/**
			 * Read the orientation of the given jump start for saving.
			 *
			 * @param el The jump start being saved.
			 * @return the jump start's orientation.
			 */
			@Override
			protected JLSInfo.Orientation getOrientation(Element el) {
				return ((JumpStart)el).orientation;
			}
			/**
			 * Store a loaded orientation into the given jump start.
			 *
			 * @param el The jump start being loaded.
			 * @param o The orientation read from the file.
			 */
			@Override
			protected void setOrientation(Element el, JLSInfo.Orientation o) {
				((JumpStart)el).orientation = o;
			}
		}
	);

	/** Base attributes plus this element's own, in save order. */
	private static final java.util.List<Attribute> ALL_ATTRIBUTES =
			concatAttributes(OWN_ATTRIBUTES);

	/**
	 * Base attributes plus this element's own, in save order (#23).
	 */
	@Override
	protected java.util.List<Attribute> savedAttributes() {

		return ALL_ATTRIBUTES;
	} // end of savedAttributes method

	/**
	 * Save this element.
	 *
	 * @param output The output writer.
	 */
	@Override
	public void save(PrintWriter output) {

		output.println("ELEMENT JumpStart");
		super.save(output);
		output.println("END");
	} // end of save method

	/**
	 * Copy this element.
	 */
	@Override
	public Element copy() {

		JumpStart it = new JumpStart(circuit);
		it.inputs.add(inputs.get(0).copy(it));
		super.copy(it);
		return it;
	} // end of copy method
	
	/**
	 * Get the name of this jumpstart.
	 * 
	 * @return the name.
	 * @jls.testedby jls.ui.CircuitAssert#jumpAlias()
	 */
	@Override
	public String getName() {
		
		return name;
	} // end of getName method
	
	/**
	 * Get the direction of this jumpstart.
	 * 
	 * @return orientation
	 */
	public JLSInfo.Orientation getOrientation() {
		return orientation;
	}
	
	/**
	 * Get the number of bits in this element.
	 * 
	 * @return the number of bits.
	 */
	@Override
	public int getBits() {
		
		return bits;
	} // end of getBits method
	
	/**
	 * Display info about this element.
	 * 
	 * @param info The JLabel to display with.
	 */
	@Override
	public void showInfo(JLabel info) {
		
		info.setText(bits + " bit wire name, value = " +
				BitSetUtils.toDisplay(currentValue,bits));
	} // end of showInfo method
	
	/**
	 * Remove this element and all jump ends with the same name.
	 
	 * @param circ A reference back to the circuit the element is in.
	 */
	@Override
	public void remove(Circuit circ) {
		
		// remove from list of jump starts and list of names in circuit
		circuit.removeName(name);
		circ.removeJumpStart(name);
		
		// remove corresonding jump ends
		Set<Element> rems = new HashSet<Element>();
		for (Element el : circ.getElements()) {
			if (el instanceof JumpEnd end) {
				if (name.equals(end.getName()))
					rems.add(el);
			}
		}
		for (Element el : rems) {
			el.remove(circ);
		}
		
		// remove itself
		super.remove(circ);
	} // end of remove method
	
	/**
	 * Tells if an adder is capable of flipping, can only flip when inputs or outputs have no attachments.
	 * @return False if any input or output has a wire attached, True otherwise
	 */
	@Override
	public boolean canFlip()
	{
		return !(inputs.get(0).isAttached());
	}
	
	/**
	 * This method will flip an adder
	 * @param g The current graphics context to facilitate recalculation of size when flipping
	 */
	@Override
	public void flip(Graphics g)
	{
		if(orientation == JLSInfo.Orientation.LEFT)
		{
			orientation = JLSInfo.Orientation.RIGHT;
		}
		else if(orientation == JLSInfo.Orientation.RIGHT)
		{
			orientation = JLSInfo.Orientation.LEFT;
		}

		inputs.clear();
		
		if(orientation == JLSInfo.Orientation.LEFT) {
			inputs.add(new Input("input",this,0,0,bits));
		}
		else if(orientation == JLSInfo.Orientation.RIGHT) {
			inputs.add(new Input("input",this,width,0,bits));
		}
	}

	/**
	 * A jump start can be watched.
	 * 
	 * @return true.
	 */
	@Override
	public boolean canWatch() {
		
		return true;
	} // end of canWatch method
	
	/**
	 * See if this jumpstart is watched.
	 * 
	 * @return true if it is, false if it is not.
	 */
	@Override
	public boolean isWatched() {
		
		return watched;
	} // end of isWatched method
	
	/**
	 * Set whether this jumpstart is watched or not.
	 * 
	 * @param state True to make it watched, false to make it not watched.
	 */
	@Override
	public void setWatched(boolean state) {
		
		watched = state;
	} // end of setWatched method
	
	/**
	 * Dialog box to set input pin characteristics.
	 */
	private class StartCreate extends ElementDialog {

		// properties
		/** The text field for the wire name. */
		private JTextField nameField = new JTextField("",12);
		/** The text field for the bit width. */
		private JTextField bitsField = new JTextField("1",5);
		/** The key pad for entering the bit width. */
		private KeyPad bitsPad = new KeyPad(bitsField,10,1,this);
		/** Selects left orientation. */
		private JRadioButton left = new JRadioButton("left");
		/** Selects right orientation. */
		private JRadioButton right = new JRadioButton("right");
		
		/**
		 * Set up dialog window.
		 * 
		 */
		private StartCreate() {
			
			// set up window title
			super("Create Wire Start","start");

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
			
			// set up orientation panel
			window.add(new JLabel(" "));
			JLabel olbl = new JLabel("Orientation");
			olbl.setAlignmentX(Component.CENTER_ALIGNMENT);
			window.add(olbl);
			JPanel orients = new JPanel(new GridLayout(1,3));

			orients.add(left);
			orients.add(new JLabel(""));
			orients.add(right);
			left.setHorizontalAlignment(SwingConstants.CENTER);
			right.setHorizontalAlignment(SwingConstants.CENTER);
			ButtonGroup group = new ButtonGroup();
			group.add(left);
			group.add(right);
			left.setSelected(true);
			window.add(orients);

			confirmOnEnter(nameField);
			finishDialog();
		} // end of constructor

		/**
		 * Validate the form and create the jump start.
		 */
		@Override
		protected void validateAndAccept() {

			String tname = nameField.getText();
			if (!Util.isValidName(tname)) {
				reject("Invalid name");
				return;
			}
			try {
				bits = Integer.parseInt(bitsField.getText());
			}
			catch (NumberFormatException ex) {
				reject("Bits not numeric");
				return;
			}
			if (bits < 1) {
				reject("Must have at least 1 bit");
				return;
			}
			if (!circuit.addName(tname)) {
				reject("Name already used");
				return;
			}
			if (right.isSelected()) {
				orientation = JLSInfo.Orientation.RIGHT;
			}
			else {
				orientation = JLSInfo.Orientation.LEFT;
			}
			circuit.addJumpStart(tname,me);
			name = tname;
			bitsPad.close();
			dispose();
		} // end of validateAndAccept method

		/**
		 * Cancel this element.
		 */
		@Override
		protected void cancelDialog() {

			cancelled = true;
			dispose();
		} // end of cancelDialog method

	} // end of StartCreate class

	/**
	 * Set this element to tri-state or not.
	 * Propagate tri-state property to the other end(s) of the jump.
	 * 
	 * @param which True to set to tri-state, false otherwise.
	 */
	@Override
	public void setTriState(boolean which) {
		
		for (Element el : circuit.getElements()) {
			if (!(el instanceof JumpEnd jend))
				continue;
			if (getName().equals(jend.getName()))
				jend.setTriState(which);
		}
	} // end of setTriState method
	
//	-------------------------------------------------------------------------------
//	Simulation
//	-------------------------------------------------------------------------------
	
	/** The current value of the named wire during simulation. */
	private BitSet currentValue = new BitSet();
	/** The jump ends with a matching name, built at simulation start. */
	private Set<JumpEnd> jumpEnds = new HashSet<JumpEnd>();
	
	/**
	 * Get the current value.
	 * 
	 * @return the current value.
	 */
	@Override
	public BitSet getCurrentValue() {
		
		if (currentValue == null)
			return null;
		else
			return (BitSet)currentValue.clone();
	} // end of getCurrentValue method
	
	/**
	 * Initialize this element by creating a set of all matching jump ends
	 * and setting the current value to 0.
	 * 
	 * @param sim Unused.
	 */
	@Override
	public void initSim(Simulator sim) {
		
		// create set of matching jump ends
		jumpEnds.clear();
		for (Element el : circuit.getElements()) {
			if (!(el instanceof JumpEnd jend))
				continue;
			if (name.equals(jend.getName())) {
				jumpEnds.add(jend);
			}
		}
		
		// set current value to 0
		currentValue = new BitSet();
		
	} // end of initSim method
	
	/**
	 * React to an event.
	 * 
	 * @param now The current simulation time.
	 * @param sim The simulator to post events to.
	 * @param todo Unused.
	 */
	@Override
	public void react(long now, Simulator sim, Object todo) {
		
		
		// get the input value
		currentValue = inputs.get(0).getValue();
		if (currentValue != null)
			currentValue = (BitSet)currentValue.clone();
		
		// send to all matching jump ends
		for (JumpEnd jend : jumpEnds) {
			BitSet newValue = null;
			if (currentValue != null)
				newValue = (BitSet)currentValue.clone();
			sim.post(new SimEvent(now,jend,newValue));
		}
	} // end of react method
	
} // end of JumpStart class
