package jls.elem;

import jls.*;
import jls.sim.*;
import jls.util.Placement;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.util.*;

import javax.swing.*;

/**
 * Receiving end of a named wire.
 *  
 * @author David A. Poplawski
 */
public final class JumpEnd extends LogicElement {
	
	// default value
	/** Default number of bits in the named wire. */
	private static final int defaultBits = 1;

	/**
	 * Message shown when the END gesture is invoked with no named wires
	 * to connect to (#131).
	 */
	public static final String NO_NAMED_WIRES =
			"No named wires exist. Name a wire with START first.";
	
	// saved properties
	/** Number of bits in the named wire. */
	private int bits = defaultBits;
	/** Name of the named wire this end connects to. */
	private String name;

	// running properties
	/** True if the user cancelled the creation dialog. */
	private boolean cancelled;
	/** True if the saved file marked the output tri-state. */
	private boolean loadTriState = false;

	/** Which way the element points. */
	private JLSInfo.Orientation orientation = JLSInfo.Orientation.RIGHT;
	
	/**
	 * Create a new wire jump end.
	 * 
	 * @param circuit The circuit this element is part of.
	 *
	 * @jls.testedby jls.elem.JumpEndNoNamedWiresTest#endGestureFailsFastWhenNoNamedWiresExist()
	 * @jls.testedby jls.elem.JumpEndNoNamedWiresTest#endGestureStillReachesDialogWithANamedWire()
	 * @jls.testedby jls.elem.JumpEndNoNamedWiresTest#matchGesturePresetNameBypassesGuardAndDialog()
	 */
	public JumpEnd(Circuit circuit) {
		
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
	 *
	 * @jls.testedby jls.elem.JumpEndNoNamedWiresTest#endGestureFailsFastWhenNoNamedWiresExist()
	 * @jls.testedby jls.elem.JumpEndNoNamedWiresTest#endGestureStillReachesDialogWithANamedWire()
	 * @jls.testedby jls.elem.JumpEndNoNamedWiresTest#matchGesturePresetNameBypassesGuardAndDialog()
	 */
	@Override
	public boolean setup(Graphics g, JPanel editWindow, int x, int y) {
		
		// show creation dialog
		
		if(name == null) {

			// fail fast when there is nothing to connect to: the selection
			// dialog would show an empty list and could never be completed
			// (#131; prior art bsiever/JLS@26053a00)
			if (circuit.getJumpStartNames().isEmpty()) {
				TellUser.error(editWindow,NO_NAMED_WIRES,"Error");
				return false;
			}
			new EndCreate();
		}
		else {
			bits = circuit.getJumpStart(name).getBits();
			if(circuit.getJumpStart(name).getOrientation() == JLSInfo.Orientation.LEFT)
				orientation = JLSInfo.Orientation.RIGHT;
			else
				orientation = JLSInfo.Orientation.LEFT;
			cancelled = false;
		}
		// don't do anything if user cancelled
		if (cancelled)
			return false;
		
		// complete initialization
		init(g);
		
		// set tri-state status
		JumpStart start = circuit.getJumpStart(name);
		Input in = start.getInput("input");
		if (in.isAttached()) {
			if (in.getWireEnd().getNet().isTriState()) {
				for (Output out : outputs) {
					out.setTriState(true);
				}
			}
		}
		
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
		
		// create output
		Output out;
		if(orientation == JLSInfo.Orientation.RIGHT)
			out = new Output("output",this,width,0,bits);
		else
			out = new Output("output",this,0,0,bits);
		outputs.add(out);
		if (loadTriState) {
			out.loadSetTriState();
		}
		
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
		
		// highlight if corresponding start is selected
		for (Element el : circuit.getElements()) {
			if (!(el instanceof JumpStart))
				continue;
			JumpStart jstart = (JumpStart)el;
			if (name.equals(jstart.getName())) {
				if (el.highlight) {
					g.setColor(Color.orange);
					Graphics2D gg = (Graphics2D)g;
					gg.fill(getRect());
				}
			}
		}
		
		// draw box
		int s = JLSInfo.spacing;
		int top = y-s/2;
		int bottom = y+s/2;
		
		if(orientation == JLSInfo.Orientation.LEFT) {
			g.setColor(Color.BLACK);
			g.drawLine(x+width-s/2,top,x+width-s/2,bottom);
			g.drawLine(x+s/2,top,x+width-s/2,top);
			g.drawLine(x+s/2,bottom,x+width-s/2,bottom);
			g.drawArc(x,top,s,s,-90,-180);
			g.drawLine(x+width-s/2,y,x+width,y);
			g.drawLine(x+width-s/2,y,x+width-s/4,y-s/4);
			g.drawLine(x+width-s/2,y,x+width-s/4,y+s/4);
		}
		else if(orientation == JLSInfo.Orientation.RIGHT) {
			g.setColor(Color.BLACK);
			g.drawLine(x+s/2,top,x+s/2,bottom);
			g.drawLine(x+s/2,top,x+width-s/2,top);
			g.drawLine(x+s/2,bottom,x+width-s/2,bottom);
			g.drawArc(x+width-s,top,s,s,-90,180);
			g.drawLine(x,y,x+s/2,y);
			g.drawLine(x+s/2,y,x+s/4,y-s/4);
			g.drawLine(x+s/2,y,x+s/4,y+s/4);
		}
		
		// draw name
		FontMetrics fm = g.getFontMetrics();
		int ascent = fm.getAscent();
		int h = fm.getDescent() + ascent;
		int w = fm.stringWidth(name);
		int tx = 0;
		if(orientation == JLSInfo.Orientation.RIGHT)
			tx = x+s/2+(width-s-w)/2+JLSInfo.pointDiameter/2;
		else if(orientation == JLSInfo.Orientation.LEFT)
			tx = x+s/2+(width-2*s-w)/2+JLSInfo.pointDiameter/2;
		g.drawString(name,tx,y-h/2+ascent);
		
		// draw output
		outputs.get(0).draw(g);
		
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
	// dispatch, and copy for this element's own attributes. The
	// " int tristate 1" line reflects derived output state, not a plain
	// field, so it stays hand-printed in save() and hand-loaded in
	// setValue below.
	/** This element's own saved attributes, in save order (#23). */
	private static final java.util.List<Attribute> OWN_ATTRIBUTES =
			java.util.List.of(
		new Attribute.StringAttribute("name") {
			/**
			 * Read the wire name to be written out for this element.
			 *
			 * @param el The JumpEnd being saved.
			 * @return the wire name.
			 */
			@Override
			protected String get(Element el) { return ((JumpEnd)el).name; }
			/**
			 * Restore the wire name during a load, registering it with the
			 * circuit so later lookups resolve.
			 *
			 * @param el The JumpEnd being loaded.
			 * @param v The wire name read from the file.
			 */
			@Override
			protected void set(Element el, String v) {
				// loading a name registers it with the circuit
				((JumpEnd)el).name = v;
				el.getCircuit().addName(v);
			}
			/**
			 * Copy the wire name from one element to another without
			 * re-registering it with the circuit.
			 *
			 * @param from The source JumpEnd.
			 * @param to The destination JumpEnd.
			 */
			@Override
			public void copy(Element from, Element to) {
				// the handwritten copy assigned the field without
				// registering the name
				((JumpEnd)to).name = ((JumpEnd)from).name;
			}
		},
		new Attribute.IntAttribute("bits") {
			/**
			 * Read the bit width to be written out for this element.
			 *
			 * @param el The JumpEnd being saved.
			 * @return the number of bits.
			 */
			@Override
			protected int get(Element el) { return ((JumpEnd)el).bits; }
			/**
			 * Restore the bit width during a load.
			 *
			 * @param el The JumpEnd being loaded.
			 * @param v The number of bits read from the file.
			 */
			@Override
			protected void set(Element el, int v) { ((JumpEnd)el).bits = v; }
		},
		new Attribute.OrientationAttribute("orientation") {
			/**
			 * Read the orientation to be written out for this element.
			 *
			 * @param el The JumpEnd being saved.
			 * @return the element's orientation.
			 */
			@Override
			protected JLSInfo.Orientation getOrientation(Element el) {
				return ((JumpEnd)el).orientation;
			}
			/**
			 * Restore the orientation during a load.
			 *
			 * @param el The JumpEnd being loaded.
			 * @param o The orientation read from the file.
			 */
			@Override
			protected void setOrientation(Element el, JLSInfo.Orientation o) {
				((JumpEnd)el).orientation = o;
			}
		}
	);

	/** Base attributes followed by this element's own, in save order (#23). */
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
	 * Set an int instance variable value (during a load).
	 *
	 * @param name The instance variable name.
	 * @param value The instance variable value.
	 */
	@Override
	public void setValue(String name, int value) {

		if (name.equals("tristate")) {
			loadTriState = true;
		} else {
			super.setValue(name,value);
		}
	} // end of setValue method

	/**
	 * Save this element.
	 *
	 * @param output The output writer.
	 */
	@Override
	public void save(PrintWriter output) {

		output.println("ELEMENT JumpEnd");
		super.save(output);
		if (outputs.get(0).isTriState())
			output.println(" int tristate 1");
		output.println("END");
	} // end of save method

	/**
	 * Copy this element.
	 */
	@Override
	public Element copy() {

		JumpEnd it = new JumpEnd(circuit);
		it.outputs.add(outputs.get(0).copy(it));
		super.copy(it);
		return it;
	} // end of copy method
	
	/**
	 * Get the name of this jump end.
	 * 
	 * @return the name.
	 *
	 * @jls.testedby jls.ui.CircuitAssert#jumpAlias()
	 */
	@Override
	public String getName() {
		
		return name;
	} // end of getName method
	
	/**
	 * Display info about this element.
	 * 
	 * @param info The JLabel to display with.
	 */
	@Override
	public void showInfo(JLabel info) {
		
		info.setText(bits + " bit wire connection, value = " +
				BitSetUtils.toDisplay(currentValue,bits));
	} // end of showInfo method

	/**
	 * Set this element to tri-state or not.
	 * 
	 * @param which True to set to tri-state, false otherwise.
	 */
	public void setTriState(boolean which) {
		
		for (Output out : outputs) {
			out.setTriState(which);
		}
	} // end of setTriState method
	
	/**
	 * Tells if an adder is capable of flipping, can only flip when inputs or outputs have no attachments.
	 * @return False if any input or output has a wire attached, True otherwise
	 */
	@Override
	public boolean canFlip()
	{
		return !(outputs.get(0).isAttached());
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

		outputs.clear();
		
		// create output
		Output out;
		if(orientation == JLSInfo.Orientation.RIGHT)
			out = new Output("output",this,width,0,bits);
		else
			out = new Output("output",this,0,0,bits);
		outputs.add(out);
		if (loadTriState) {
			out.loadSetTriState();
		}
	}
	
	/**
	 * Dialog box to set jump end characteristics.
	 */
	@SuppressWarnings("serial")
	private class EndCreate extends ElementDialog {

		// properties
		/** List of named wire (jump start) names to pick from. */
		private JList starts;
		/** Selects leftward orientation. */
		private JRadioButton left = new JRadioButton("left");
		/** Selects rightward orientation. */
		private JRadioButton right = new JRadioButton("right");
		
		/**
		 * Set up dialog window.
		 * 
		 */
		private EndCreate() {
			
			// set up window title
			super("Create Wire End","end");

			// set not cancelled
			cancelled = false;

			// set up window
			Container window = getContentPane();

			// set up jumpstart name list
			JLabel heading = new JLabel("Select Wire Name",SwingConstants.CENTER);
			heading.setAlignmentX((float)0.5);
			window.add(heading);

			starts = new JList<String>(circuit.getJumpStartNames().toArray(new String[0]));
			starts.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			starts.setVisibleRowCount(Math.min(circuit.getJumpStartNames().size(),10));
			JScrollPane pane = new JScrollPane(starts);
			window.add(pane);
			
			// highlight name if there is only one
			if (circuit.getJumpStartNames().size() == 1) {
				starts.setSelectedIndex(0);
			}
			
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
			right.setSelected(true);
			window.add(orients);

			finishDialog();
		} // end of constructor

		/**
		 * Validate the form and create the jump end.
		 */
		@Override
		protected void validateAndAccept() {

			if (starts.getSelectedIndex() < 0) {
				reject("Nothing selected");
				return;
			}
			name = (String)starts.getSelectedValue();
			bits = circuit.getJumpStart(name).getBits();
			if (right.isSelected()) {
				orientation = JLSInfo.Orientation.RIGHT;
			}
			else {
				orientation = JLSInfo.Orientation.LEFT;
			}
			dispose();
		} // end of validateAndAccept method

		/**
		 * Cancel this jump end.
		 */
		@Override
		protected void cancelDialog() {

			cancelled = true;
			dispose();
		} // end of cancelDialog method

	} // end of EndCreate class
	
//	-------------------------------------------------------------------------------
//	Simulation
//	-------------------------------------------------------------------------------
	
	/** The value currently on the output, or null when tri-stated off. */
	private BitSet currentValue = new BitSet();
	
	/**
	 * Initialize this element by setting its output to 0.
	 * 
	 * @param sim Unused.
	 */
	@Override
	public void initSim(Simulator sim) {
		
		// set output to 0 or off
		Output out = outputs.get(0);
		if (out.isTriState()) {
			currentValue = null;
			out.setValue(null);
		}
		else {
			currentValue = new BitSet();
			BitSet bitval = new BitSet(1);
			out.setValue(bitval);
		}
		
	} // end of initSim method
	
	/**
	 * React to an event.
	 * 
	 * @param now The current simulation time.
	 * @param sim The simulator to post events to.
	 * @param todo The value to send along.
	 */
	@Override
	public void react(long now, Simulator sim, Object todo) {
		
		// get the input value
		BitSet value = (BitSet)todo;
		currentValue = null;
		if (value != null)
			currentValue = (BitSet)value.clone();
		
		// send to output
		Output out = outputs.get(0);
		if (value == null)
			out.propagate(null,now,sim);
		else
			out.propagate((BitSet)value.clone(),now,sim);
	
	} // end of react method

	/**
	 * Set the name of the named wire this end connects to, bypassing the
	 * selection dialog (used by the editor's match gesture).
	 *
	 * @param newName The wire name to attach to.
	 *
	 * @jls.testedby jls.elem.JumpEndNoNamedWiresTest#matchGesturePresetNameBypassesGuardAndDialog()
	 */
	public void setName(String newName) {
		name = newName;
	}

} // end of JumpEnd method
