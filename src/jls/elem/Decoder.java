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
import java.awt.geom.Rectangle2D;
import java.io.PrintWriter;
import java.util.BitSet;

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
import jls.sim.SimEvent;
import jls.sim.Simulator;
import jls.util.Placement;

/**
 * n-input, 2^n-output decoder.
 * 
 * @author David A. Poplawski
 */
public final class Decoder extends LogicElement {
	
	// default values
	/** Default number of input bits. */
	private static final int defaultBits = 1;
	/** Default propagation delay. */
	private static final int defaultPropDelay = 15; 
	
	// saved properties
	/** Number of input bits (the decoder has 2^bits outputs). */
	private int bits = defaultBits;
	/** Propagation delay of this decoder. */
	private int propDelay = defaultPropDelay;
	//Orientation is based off of where the inputs are
	/** Which side the input is on. */
	private JLSInfo.Orientation orientation = JLSInfo.Orientation.LEFT;
	
	// running properties
	/** True if the user cancelled the creation dialog. */
	private boolean cancelled;
	/** The "decoder" label drawn on the element, abbreviated to "dec" if it doesn't fit. */
	private String dec;
	/** The input/output width label drawn on the element (e.g. "1-n"), oriented to match the element. */
	private String inout;
	
	/**
	 * Create a new decoder element.
	 * 
	 * @param circuit The circuit this element is part of.
	 */
	public Decoder(Circuit circuit) {
		
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
		new DecoderCreate();
		
		// don't do anything if user cancelled gate
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
		
		int s = JLSInfo.spacing;
		int outs = 1 << bits;
	
		if(orientation == JLSInfo.Orientation.LEFT)
		{
			inout = bits + "-" + outs;
		}
		else if(orientation == JLSInfo.Orientation.UP)
		{
			inout = bits + "\n | \n" + outs;
		}
		else if(orientation == JLSInfo.Orientation.DOWN)
		{
			inout = outs + "\n | \n" + bits;
		}
		else if(orientation == JLSInfo.Orientation.RIGHT)
		{
			inout = outs + "-" + bits;
		}
		dec = " decoder ";
		
		// set up size if there is a graphics object
		if (g != null) {
			
			// if element already has a size, use it
			if (width == 0 && height == 0) {
				
				if(orientation == JLSInfo.Orientation.LEFT || orientation == JLSInfo.Orientation.RIGHT)
				{
					width = 5*s;
					height = 2*s;
				}
				else
				{
					width = 2 * s;
					height = 5 * s;
				}
				FontMetrics fm = g.getFontMetrics();
				int bw = fm.stringWidth(inout);
				if (bw > width && orientation == JLSInfo.Orientation.LEFT) {
					inout = "1-n";
				}
				else if(bw > width && orientation == JLSInfo.Orientation.RIGHT)
				{
					inout = "n-1";
				}
				int dw = fm.stringWidth(dec);
				if (dw > width) {
					dec = "dec";
				}
			}
			
		}
		
		
		if(orientation == JLSInfo.Orientation.LEFT)
		{	
			// create input
			inputs.add(new Input("input",this,0,s,bits));
			// create output
			outputs.add(new Output("output",this,width,s,1<<bits));
		}
		else if(orientation == JLSInfo.Orientation.RIGHT)
		{
			// create input
			inputs.add(new Input("input",this,width,s,bits));
			// create output
			outputs.add(new Output("output",this,0,s,1<<bits));
		}
		else if(orientation == JLSInfo.Orientation.DOWN)
		{
			// create input
			inputs.add(new Input("input",this,s,height,bits));
			// create output
			outputs.add(new Output("output",this,s,0,1<<bits));
		}
		else if(orientation == JLSInfo.Orientation.UP)
		{
			// create input
			inputs.add(new Input("input",this,s,0,bits));
			// create output
			outputs.add(new Output("output",this,s,height,1<<bits));
		}
	} // end of init method
	
	/**
	 * Draw this gate.
	 * 
	 * @param g The graphics object to draw with.
	 */
	@Override
	public void draw(Graphics g) {
		
		// draw context
		super.draw(g);
		
		// draw box
		g.setColor(Color.BLACK);
		g.drawRect(x,y,width,height);
		
		// draw values inside box
		FontMetrics fm = g.getFontMetrics();
		if(orientation == JLSInfo.Orientation.LEFT || orientation == JLSInfo.Orientation.RIGHT)
		{
			Rectangle2D t = fm.getStringBounds(inout,g);
			g.drawString(inout,x+(int)(width-t.getWidth())/2,
					y+(int)(height/2-t.getHeight())/2+fm.getAscent());
			t = fm.getStringBounds(dec,g);
			g.drawString(dec,x+(int)(width-t.getWidth())/2,
					y+JLSInfo.spacing+(int)(height/2-t.getHeight())/2+fm.getAscent());
		}
		else if(orientation == JLSInfo.Orientation.UP)
		{
			Rectangle2D t = fm.getStringBounds("1", g);
			g.drawString("1", x+(width-(int)t.getWidth())/2, y+fm.getHeight()+5);
			g.drawString("|", x+(width-(int)t.getWidth())/2, y+fm.getHeight() + 20);
			g.drawString("n", x+(width-(int)t.getWidth())/2, y+fm.getHeight()+ 35);
		}
		else if(orientation == JLSInfo.Orientation.DOWN)
		{
			Rectangle2D t = fm.getStringBounds("1", g);
			g.drawString("n", x+(width-(int)t.getWidth())/2, y+fm.getHeight()+5);
			g.drawString("|", x+(width-(int)t.getWidth())/2, y+fm.getHeight() + 20);
			g.drawString("1", x+(width-(int)t.getWidth())/2, y+fm.getHeight()+ 35);
		}
		
		// draw input and output
		inputs.get(0).draw(g);
		outputs.get(0).draw(g);
		
	} // end of draw method
	
	// Declarative persistence (#23): one declaration drives save, load
	// dispatch, and copy for this element's own attributes.
	/** This element's own saved attributes: bits, delay and orientation. */
	private static final java.util.List<Attribute> OWN_ATTRIBUTES =
			java.util.List.of(
		new Attribute.IntAttribute("bits") {
			/**
			 * Read the input bit width from the given decoder.
			 *
			 * @param el The decoder to read from.
			 * @return the number of input bits.
			 */
			@Override
			protected int get(Element el) { return ((Decoder)el).bits; }
			/**
			 * Store the input bit width into the given decoder.
			 *
			 * @param el The decoder to write to.
			 * @param v The number of input bits.
			 */
			@Override
			protected void set(Element el, int v) { ((Decoder)el).bits = v; }
		},
		new Attribute.IntAttribute("delay") {
			/**
			 * Read the propagation delay from the given decoder.
			 *
			 * @param el The decoder to read from.
			 * @return the propagation delay.
			 */
			@Override
			protected int get(Element el) { return ((Decoder)el).propDelay; }
			/**
			 * Store the propagation delay into the given decoder.
			 *
			 * @param el The decoder to write to.
			 * @param v The new propagation delay.
			 */
			@Override
			protected void set(Element el, int v) { ((Decoder)el).propDelay = v; }
		},
		new Attribute.OrientationAttribute("orient") {
			/**
			 * Read the orientation from the given decoder.
			 *
			 * @param el The decoder to read from.
			 * @return the current orientation.
			 */
			@Override
			protected JLSInfo.Orientation getOrientation(Element el) {
				return ((Decoder)el).orientation;
			}
			/**
			 * Store the orientation into the given decoder.
			 *
			 * @param el The decoder to write to.
			 * @param o The new orientation.
			 */
			@Override
			protected void setOrientation(Element el, JLSInfo.Orientation o) {
				((Decoder)el).orientation = o;
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

		output.println("ELEMENT Decoder");
		super.save(output);
		output.println("END");
	} // end of save method

	/**
	 * Copy this element.
	 */
	@Override
	public Element copy() {

		Decoder it = new Decoder(circuit);
		it.inout = inout;
		it.dec = dec;
		it.inputs.add(inputs.get(0).copy(it));
		it.outputs.add(outputs.get(0).copy(it));
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
		
		info.setText(bits + " to " + (1<<bits) + " decoder");

	} // end of showInfo method

	/**
	 * Decoders have timing info (propagation delay).
	 * 
	 * @return true.
	 */
	@Override
	public boolean hasTiming() {
		
		return true;
	} // end of hasTiming method

	/**
	 * Reset propagation delay to default value.
	 */
	@Override
	public void resetPropDelay() {
		
		propDelay = defaultPropDelay;
	} // end of resetPropDelay method

	/**
	 * Get the propagation delay in this element.
	 * 
	 * @return the current delay.
	 */
	@Override
	public int getDelay() {
		
		return propDelay;
	} // end of getDelay method
	
	/**
	 * Set the propagation delay in this element.
	 * 
	 * @param temp The new delay amount.
	 */
	@Override
	public void setDelay(int temp) {
		
		propDelay = temp;
	} // end of setDelay method
	
	/**
	 * Tells if a decoder is capable of rotatating, can only rotate when inputs or outputs have no attachments.
	 * @return False if any input or output has a wire attached, True otherwise
	 */
	@Override
	public boolean canRotate()
	{
		return !(inputs.get(0).isAttached() || outputs.get(0).isAttached());
	}
	
	/**
	 *  This method will rotate the decoder if it is rotateable.
	 * @param direction The direction to rotate
	 * @param g The current graphics context for use in recalculating size
	 */
	@Override
	public void rotate(JLSInfo.Orientation direction, Graphics g)
	{
		if(direction == JLSInfo.Orientation.LEFT)
		{
			orientation = orientation.ccw();
			
		}
		else if(direction == JLSInfo.Orientation.RIGHT)
		{
			orientation = orientation.cw();
		}
		inputs.clear();
		outputs.clear();
		width = 0;
		height = 0;
		init(g);
	}
	
	/**
	 * This method will flip a decoder
	 * @param g The current graphics context to facilitate recalculation of size when flipping
	 */
	@Override
	public void flip(Graphics g)
	{
		orientation = orientation.flipped();
		inputs.clear();
		outputs.clear();
		width = 0;
		height = 0;
		init(g);
	}
	
	/**
	 * Tells if a decoder is capable of flipping, can only flip when inputs or outputs have no attachments.
	 * @return False if any input or output has a wire attached, True otherwise
	 */
	@Override
	public boolean canFlip()
	{
		return !(inputs.get(0).isAttached() || outputs.get(0).isAttached());
	}

	/**
	 * Dialog box to set bits.
	 */
	@SuppressWarnings("serial")
	private class DecoderCreate extends ElementDialog {

		// properties
		/** Text field for the number of input bits. */
		private JTextField bitsField = new JTextField(defaultBits+"",10);
		/** Pop-up numeric keypad attached to the bits field. */
		private KeyPad bitsPad = new KeyPad(bitsField,10,defaultBits,this);
		/** Selects input-on-the-left orientation (the default). */
		private JRadioButton left = new JRadioButton("Left", true);
		/** Selects input-on-the-right orientation. */
		private JRadioButton right = new JRadioButton("Right");
		/** Selects input-on-top orientation. */
		private JRadioButton up = new JRadioButton("Up");
		/** Selects input-on-the-bottom orientation. */
		private JRadioButton down = new JRadioButton("Down");
		
		/**
		 * Set up create dialog window.
		 * 
		 */
		private DecoderCreate() {
			
			// set up window title
			super("Create Decoder","decoder");

			// set not cancelled
			cancelled = false;

			// set up window
			Container window = getContentPane();

			// set up inputs
			JPanel info = new JPanel(new BorderLayout());
			JLabel bits = new JLabel("Input Bits: ",SwingConstants.RIGHT);
			info.add(bits,BorderLayout.WEST);
			info.add(bitsField,BorderLayout.CENTER);
			info.add(bitsPad,BorderLayout.EAST);
			JPanel orient = new JPanel(new GridLayout(3,3));
			ButtonGroup gr = new ButtonGroup();
			gr.add(left);
			gr.add(right);
			gr.add(down);
			gr.add(up);
			orient.add(new JLabel(""));
			orient.add(up);
			orient.add(new JLabel(""));
			orient.add(left);
			orient.add(new JLabel(""));
			orient.add(right);
			orient.add(new JLabel(""));
			orient.add(down);
			orient.add(new JLabel(""));
			window.add(info);
			JLabel olbl = new JLabel("Orientation");
			olbl.setAlignmentX(Component.CENTER_ALIGNMENT);
			window.add(olbl);
			window.add(orient);

			confirmOnEnter(bitsField);
			finishDialog();
		} // end of constructor

		/**
		 * Validate the form and create the decoder.
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
				reject("Must be at least 1 bit");
				return;
			}
			if (bits >= 32) {
				reject("Must be less than 32 bits");
				return;
			}
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
		 * Cancel this gate.
		 */
		@Override
		protected void cancelDialog() {

			cancelled = true;
			dispose();
		} // end of cancelDialog method

	} // end of DecoderCreate class
	
//	-------------------------------------------------------------------------------
//	Simulation
//	-------------------------------------------------------------------------------
	
	/** The output value this decoder will take on once its propagation delay elapses. */
	private BitSet toBeValue;
	
	/**
	 * Initialize this element by setting its output pin and to-be value to 0.
	 * 
	 * @param sim Unused.
	 */
	@Override
	public void initSim(Simulator sim) {
		
		// set output pin to 0
		Output out = outputs.get(0);
		BitSet zero = new BitSet(1);
		out.setValue(zero);
		
		// set post output change to 1
		BitSet one = new BitSet(1);
		one.flip(0);
		sim.post(new SimEvent(0,this,one));
		
		// set to-be value
		toBeValue = (BitSet)one.clone();
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
		
		// if the input has changed ...
		if (todo == null) {
			
			// get the input value
			BitSet value = inputs.get(0).getValue();
			if (value == null)
				value = new BitSet();
			
			// create new output value
			int inval = BitSetUtils.ToInt(value);
			BitSet newValue = new BitSet(inval+1);
			newValue.set(inval);

			// if new value is different from the value propagating through
			// the decoder, then post an event
			if (!newValue.equals(toBeValue)) {
				toBeValue = (BitSet)newValue.clone();
				sim.post(new SimEvent(now+propDelay,this,newValue));
			}
		}
		else {
			
			// get the new output value
			BitSet newValue = (BitSet)todo;
			
			// send to output
			Output out = outputs.get(0);
			out.propagate(newValue,now,sim);
		}
	} // end of react method
	
} // end of Decoder class
