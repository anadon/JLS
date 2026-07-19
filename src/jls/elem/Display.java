package jls.elem;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.BitSet;
import java.util.Vector;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import jls.BitSetUtils;
import jls.Circuit;
import jls.Help;
import jls.JLSInfo;
import jls.KeyPad;
import jls.Util;
import jls.sim.Simulator;
import jls.util.Placement;

/**
 * Display an input value on the circuit editor screen.
 * 
 * @author David A. Poplawski
 */
public final class Display extends LogicElement {
	
	// constants
	/** The input width a new display starts with. */
	private final int defaultBits = 1;
	
	// properties
	/** The width of the displayed input, in bits. */
	private int bits = defaultBits;
	/** The radix (2, 10 or 16) the value is displayed in. */
	private int base = 10;
	
	// running properties
	/** True if the user cancelled the creation dialog. */
	private boolean cancelled = false;
	
	// legacy
	/** Legacy single-input orientation marker; -1 means "new save format". */
	int orient = -1;
	
	/**
	 * Create new element.
	 * 
	 * @param circuit The circuit this element is in.
	 */
	public Display(Circuit circuit) {
		
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
		new DispCreate();
		
		// don't do anything if user cancelled element
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
	 * Initialize this element.
	 * 
	 * @param g Unused.
	 */
	@Override
	public void init(Graphics g) {
		
		// set up size
		int s = JLSInfo.spacing;
		if (g != null) {
			
			// use existing size if it has one
			if (width == 0 && height == 0) {
				height = 2*s;
				FontMetrics fm = g.getFontMetrics();
				BigInteger maxVal = new BigInteger("1").shiftLeft(bits).subtract(new BigInteger("1"));
				
				String longest = maxVal.toString(base).replaceAll(".","0");
				switch (base) {
				case 2:
					longest = longest + "B";
					break;
				case 16:
					longest = "0x" + longest;
				}
				int strLen = fm.stringWidth(" " + longest + " ");
				width = (strLen+2*s-1)/(2*s)*(2*s);
			}
		}
		
		// create input
		if(orient < 0) { // New save format
			inputs.add(new Input("input0",this,0,s,bits)); // Left
			inputs.add(new Input("input1",this,width/2,0,bits)); // Top
			inputs.add(new Input("input2",this,width/2,height,bits)); // Bottom
			inputs.add(new Input("input3",this,width,s,bits)); // Right
		} else { // Old save format
			switch(orient) {
			case 0:
				inputs.add(new Input("input",this,width/2,0,bits)); // Top
				break;
			case 1:
				inputs.add(new Input("input",this,width/2,height,bits)); // Bottom
				break;
			case 2:
				inputs.add(new Input("input",this,0,s,bits)); // Left
				break;
			case 3:
				inputs.add(new Input("input",this,width,s,bits)); // Right
				break;
			}
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
		
		// draw shape
		int s = JLSInfo.spacing;
		g.setColor(Color.black);
		g.drawRect(x,y,width,height);
		g.drawRoundRect(x,y,width,height,s,s);
		
		// draw value inside shape
		FontMetrics fm = g.getFontMetrics();
		int ascent = fm.getAscent();
		int hi = ascent + fm.getDescent();
		String value = " HiZ ";
		if (currentValue != null) {
			value = BitSetUtils.ToString(currentValue,base);
			switch (base) {
			case 2:
				while (value.length() < bits)
					value = "0" + value;
				value = value + "B";
				break;
			case 16:
				while (value.length()*4 < bits)
					value = "0" + value;
				value = "0x" + value;
			}
			value = " " + value + " ";
		}
		int sw = fm.stringWidth(value);
		g.drawString(value,x+(width-sw)/2,y+(height-hi)/2+ascent);
		
		// draw attached input (draw all four if nothing is attached)
		// get unattached inputs
		Vector<Input>detach = new Vector<Input>(4);
		for (Input input : inputs) {
			if (!input.isAttached())
				detach.add(input);
		}
		
		// if there are one, two or three unattached ones
		int count = detach.size();
		if (count > 0 && count < 4) {
			
			// remove unattached inputs
			inputs.removeAll(detach);
			
			// if one input remains, fix its bit number
			
			// if no inputs left, put all four back
			if (inputs.size() == 0) {
				inputs.add(new Input("input0",this,0,s,bits)); // Left
				inputs.add(new Input("input1",this,width/2,0,bits)); // Top
				inputs.add(new Input("input2",this,width/2,height,bits)); // Bottom
				inputs.add(new Input("input3",this,width,s,bits)); // Right
			}
		}
		for (Input input : inputs) {
			input.draw(g);
		}
	} // end of draw method

	/**
	 * Save this element.
	 * 
	 * @param output The output writer.
	 */
	@Override
	public void save(PrintWriter output) {

		output.println("ELEMENT Display");
		super.save(output);
		output.println("END");
	} // end of save method

	// Declarative persistence (#23): one declaration drives save, load
	// dispatch, and copy for this element's own attributes.
	/** This element's own persisted attributes, in save order. */
	private static final java.util.List<Attribute> OWN_ATTRIBUTES =
			java.util.List.of(
		new Attribute.IntAttribute("bits") {
			/**
			 * Read the bit count from the given display element.
			 */
			@Override
			protected int get(Element el) { return ((Display)el).bits; }
			/**
			 * Store the bit count into the given display element.
			 */
			@Override
			protected void set(Element el, int v) { ((Display)el).bits = v; }
		},
		new Attribute.IntAttribute("base") {
			/**
			 * Read the display radix from the given display element.
			 */
			@Override
			protected int get(Element el) { return ((Display)el).base; }
			/**
			 * Store the display radix into the given display element.
			 */
			@Override
			protected void set(Element el, int v) { ((Display)el).base = v; }
		},
		new Attribute.IntAttribute("orient") {
			// legacy single-input save format marker; only saved when
			// present in the loaded file
			/**
			 * Read the legacy orientation marker from the given display element.
			 */
			@Override
			protected int get(Element el) { return ((Display)el).orient; }
			/**
			 * Store the legacy orientation marker into the given display element.
			 */
			@Override
			protected void set(Element el, int v) { ((Display)el).orient = v; }
			/**
			 * Suppress saving the orientation marker unless a legacy value was loaded.
			 */
			@Override
			protected boolean omitted(Element el) {
				// -1 is the "no marker" sentinel; 0 (Top) is a valid
				// legacy value and must be re-saved (issue #57)
				return ((Display)el).orient < 0;
			}
			/**
			 * Deliberately skip copying the legacy orientation marker.
			 */
			@Override
			public void copy(Element from, Element to) {
				// the handwritten copy never carried the legacy marker
				// to the copy
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
	 * Copy this element.
	 *
	 * @return a copy of this element.
	 */
	@Override
	public Element copy() {

		Display it = new Display(circuit);
		for (Input input : inputs) {
			it.inputs.add(input.copy(it));
		}
		super.copy(it);
		return it;
	} // end of copy method

	/**
	 * Display info about this element.
	 * 
	 * @param info The JLabel to display with.
	 */
	@Override
	public void showInfo(JLabel info) {
		
		info.setText(bits + " bit display, value = " + BitSetUtils.toDisplay(currentValue,bits));
	} // end of showInfo method
	
	/**
	 *  This method will rotate the display if it is rotateable.
	 * @param direction The direction to rotate
	 * @param g The current graphics context for use in recalculating size
	 */
	@Override
	public void rotate(JLSInfo.Orientation direction, Graphics g)
	{
		// No-op
	}
	
	/**
	 * Tells if a display is capable of rotatating, can only rotate when input has no attachment.
	 * @return False if input has a wire attached, True otherwise
	 */
	@Override
	public boolean canRotate()
	{
		// Displays cannot be rotated ever since they implement the Stop behavior for inputs.
		return false;
	}
	
	/**
	 * Tells if a display is capable of flippinging, can only flip when input has no attachment.
	 * @return False if input has a wire attached, True otherwise
	 */
	@Override
	public boolean canFlip()
	{
		// Displays cannot be flipped ever since they implement the Stop behavior for inputs.
		return false;
	}
	
	/**
	 * This method will flip a display
	 * @param g The current graphics context to facilitate recalculation of size when flipping
	 */
	@Override
	public void flip(Graphics g)
	{
		// No-op
	}


	/**
	 * Dialog box to set inputs and bits.
	 */
	@SuppressWarnings("serial")
	protected class DispCreate extends ElementDialog {

		// properties
		/** Text field for entering the number of input bits. */
		private JTextField bitsField = new JTextField(defaultBits+"",5);
		/** Pop-up keypad for the bits field. */
		private KeyPad bitsPad = new KeyPad(bitsField,10,defaultBits,this);
		/** Radio button selecting base 2 (binary) display. */
		private JRadioButton b2 = new JRadioButton("2");
		/** Radio button selecting base 10 (decimal) display. */
		private JRadioButton b10 = new JRadioButton("10");
		/** Radio button selecting base 16 (hexadecimal) display. */
		private JRadioButton b16 = new JRadioButton("16");
		
		/**
		 * Set up dialog window.
		 * 
		 */
		protected DispCreate() {
			
			// set up window title
			super("Create Display","display");

			// set not cancelled
			cancelled = false;

			// set up window
			Container window = getContentPane();

			// set up bits panel
			JPanel info = new JPanel(new BorderLayout());
			JLabel bits = new JLabel("Bits: ",SwingConstants.RIGHT);
			info.add(bits,BorderLayout.WEST);
			info.add(bitsField,BorderLayout.CENTER);
			info.add(bitsPad,BorderLayout.EAST);
			window.add(info);
			
			// set up radix panel
			window.add(new JLabel(" "));
			JLabel dlbl = new JLabel("Display Radix");
			dlbl.setAlignmentX(Component.CENTER_ALIGNMENT);
			window.add(dlbl);
			JPanel radix = new JPanel(new GridLayout(1,3));
			radix.add(b2);
			radix.add(b10);
			radix.add(b16);
			ButtonGroup rgroup = new ButtonGroup();
			rgroup.add(b2);
			rgroup.add(b10);
			rgroup.add(b16);
			b10.setSelected(true);
			window.add(radix);

			confirmOnEnter(bitsField);
			finishDialog();
		} // end of constructor

		/**
		 * Validate the form and create the display.
		 */
		@Override
		protected void validateAndAccept() {

			try {
				bits = Integer.parseInt(bitsField.getText());
			}
			catch (NumberFormatException ex) {
				reject("Value not numeric, try again");
				return;
			}
			if (bits < 1) {
				reject("Must be at least one bit");
				return;
			}

			if (b2.isSelected())
				base = 2;
			else if (b10.isSelected())
				base = 10;
			else
				base = 16;
			bitsPad.close();
			dispose();
		} // end of validateAndAccept method

		/**
		 * Cancel this mux.
		 */
		@Override
		protected void cancelDialog() {

			cancelled = true;
			bitsPad.close();
			dispose();
		} // end of cancelDialog method

	} // end of DispCreate class

//	-------------------------------------------------------------------------------
//	Simulation
//	-------------------------------------------------------------------------------
	
	/** The value being displayed; null shows as HiZ. */
	private BitSet currentValue = new BitSet();
	
	/**
	 * Set the current display value to 0.
	 * 
	 * @param sim Unused.
	 */
	@Override
	public void initSim(Simulator sim) {
		
		Input in = inputs.get(0);
		if (in.isAttached() && in.getWireEnd().getNet().isTriState()) {
			currentValue = null;
		}
		else {
			currentValue = new BitSet();
		}
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
		
		currentValue = inputs.get(0).getValue();
	} // end of react method

} // end of Display class
