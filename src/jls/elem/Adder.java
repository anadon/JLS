package jls.elem;

import jls.*;
import jls.sim.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

import javax.swing.*;

import java.io.*;
import java.util.*;

/**
 * An n-bit adder with carry in and carry out (n chosen by user).
 * Propagation delay is proportional to the number of bits.
 * 
 * @author David A. Poplawski
 */
public class Adder extends LogicElement {
	
	// default values
	private static final int defaultBits = 1;
	private static final int defaultPropDelay = 30; 
	
	// saved properties
	private int bits = defaultBits;
	private int propDelay = defaultPropDelay;
	private JLSInfo.Orientation orientation = JLSInfo.Orientation.RIGHT;
	
	// running properties
	private boolean cancelled;

	/**
	 * Create a new adder element.
	 * 
	 * @param circuit The circuit this element is part of.
	 */
	public Adder(Circuit circuit) {
		
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
	public boolean setup(Graphics g, JPanel editWindow, int x, int y) {
		
		// show creation dialog
		Point pos = editWindow.getMousePosition();
		Point win = editWindow.getLocationOnScreen();
		if (pos == null) {
			new AdderCreate(x+win.x,y+win.y);
		}
		else {
			new AdderCreate(pos.x+win.x,pos.y+win.y);
		}
		
		// don't do anything if user cancelled gate
		if (cancelled)
			return false;
		
		// set propagation delay based on number of bits
		propDelay = defaultPropDelay * bits;
		
		// complete initialization
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
	 * 
	 * @param g The Graphics object to use.
	 */
	public void init(Graphics g) {

		// canonical geometry (RIGHT), transformed to the current
		// orientation (#24)
		int s = JLSInfo.spacing;
		height = 4*s;
		width = 4*s;
		GridTransform.Chain t = placement();
		Point a = t.map(0,s);
		Point b = t.map(0,3*s);
		Point cin = t.map(2*s,0);
		Point sum = t.map(4*s,2*s);
		Point cout = t.map(2*s,4*s);
		inputs.add(new Input("A",this,a.x,a.y,bits));
		inputs.add(new Input("B",this,b.x,b.y,bits));
		inputs.add(new Input("Cin",this,cin.x,cin.y,1));
		outputs.add(new Output("S",this,sum.x,sum.y,bits));
		outputs.add(new Output("Cout",this,cout.x,cout.y,1));
	} // end of init method

	/**
	 * The transform from canonical geometry (RIGHT) to the current
	 * orientation.
	 */
	private GridTransform.Chain placement() {

		int s = JLSInfo.spacing;
		GridTransform.Chain t = GridTransform.chain(4*s, 4*s);
		switch (orientation) {
		case RIGHT:
			break;
		case LEFT:
			t.mirrorX();
			break;
		case DOWN:
			t.rotateCW().mirrorX();
			break;
		default: // UP
			t.rotateCW().mirrorX().mirrorY();
			break;
		}
		return t;
	} // end of placement method
	
	/**
	 * Draw this gate.
	 * 
	 * @param g The graphics object to draw with.
	 */
	public void draw(Graphics g) {
		
		// draw context
		super.draw(g);
		
		// draw box
		g.setColor(Color.BLACK);
		g.drawRect(x,y,width,height);
		
		// draw plus sign
		int s = JLSInfo.spacing;
		if(orientation == JLSInfo.Orientation.UP || orientation == JLSInfo.Orientation.DOWN)
		{
			g.drawLine(x+2*s,y+s,x+2*s,y+2*s);
			g.drawLine(x+3*s/2,y+3*s/2,x+5*s/2,y+3*s/2);
		}
		else if(orientation == JLSInfo.Orientation.LEFT || orientation == JLSInfo.Orientation.RIGHT)
		{
			g.drawLine(x+2*s,y+3*s/2,x+2*s,y+5*s/2);
			g.drawLine(x+3*s/2,y+2*s,x+5*s/2,y+2*s);
		}
		
		// draw input and output labels
		int d = JLSInfo.pointDiameter;
		FontMetrics fm = g.getFontMetrics();
		if(orientation == JLSInfo.Orientation.RIGHT)
		{
			int ascent = fm.getAscent();
			Rectangle2D t = fm.getStringBounds("A",g);
			g.drawString("A", x+d/2, (int)(y+s-t.getHeight()/2)+ascent);
			t = fm.getStringBounds("B",g);
			g.drawString("B", x+d/2, (int)(y+3*s-t.getHeight()/2)+ascent);
			t = fm.getStringBounds("S",g);
			g.drawString("S", (int)(x+width-t.getWidth()-d/2),
				(int)(y+2*s-t.getHeight()/2)+ascent);
		
			Font f = g.getFont();
			float fs = f.getSize2D();
			Font nf = f.deriveFont((float)(fs*0.75));
			g.setFont(nf);
			fm = g.getFontMetrics();
			ascent = fm.getAscent();
			int descent = fm.getDescent();
			t = fm.getStringBounds("Cin",g);
			g.drawString("Cin", x+(int)(width-t.getWidth())/2, y+ascent);
			t = fm.getStringBounds("Cout",g);
			g.drawString("Cout", x+(int)(width-t.getWidth())/2, y+height-descent);
			g.setFont(f);
		}
		else if(orientation == JLSInfo.Orientation.LEFT)
		{
			int ascent = fm.getAscent();
			Rectangle2D t = fm.getStringBounds("A",g);
			g.drawString("A", (int)(x+width-t.getWidth()-d/2), (int)(y+s-t.getHeight()/2)+ascent);
			t = fm.getStringBounds("B",g);
			g.drawString("B", (int)(x+width-t.getWidth()-d/2), (int)(y+3*s-t.getHeight()/2)+ascent);
			t = fm.getStringBounds("S",g);
			g.drawString("S", (int)(x+d/2),
				(int)(y+2*s-t.getHeight()/2)+ascent);
		
			Font f = g.getFont();
			float fs = f.getSize2D();
			Font nf = f.deriveFont((float)(fs*0.75));
			g.setFont(nf);
			fm = g.getFontMetrics();
			ascent = fm.getAscent();
			int descent = fm.getDescent();
			t = fm.getStringBounds("Cin",g);
			g.drawString("Cin", x+(int)(width-t.getWidth())/2, y+ascent);
			t = fm.getStringBounds("Cout",g);
			g.drawString("Cout", x+(int)(width-t.getWidth())/2, y+height-descent);
			g.setFont(f);
		}
		else if(orientation == JLSInfo.Orientation.DOWN)
		{
			int ascent = fm.getAscent();
			int descent = fm.getDescent();
			Rectangle2D t = fm.getStringBounds("A",g);
			g.drawString("A", (int)(x+s-t.getWidth()/2), (int)(y+t.getHeight()/4)+ascent);
			t = fm.getStringBounds("B",g);
			g.drawString("B", (int)(x+width-t.getWidth()-d/2), (int)(y+t.getHeight()/4)+ascent);
			t = fm.getStringBounds("S",g);
			g.drawString("S", x+(int)(width-t.getWidth())/2, y+height-descent);
		
			Font f = g.getFont();
			float fs = f.getSize2D();
			Font nf = f.deriveFont((float)(fs*0.70));
			g.setFont(nf);
			fm = g.getFontMetrics();
			ascent = fm.getAscent();
			descent = fm.getDescent();
			t = fm.getStringBounds("Cin",g);
			g.drawString("Cin", x+5, y+height/2+ascent);
			t = fm.getStringBounds("Cout",g);
			g.drawString("Cout", x+(int)(width-t.getWidth()-5), y+height/2+ascent);
			g.setFont(f);	
		}
		else if(orientation == JLSInfo.Orientation.UP)
		{
			int ascent = fm.getAscent();
			Rectangle2D t = fm.getStringBounds("A",g);
			g.drawString("A", (int)(x+s-t.getWidth()/2), (int)(y+height-t.getHeight())+ascent);
			t = fm.getStringBounds("B",g);
			g.drawString("B", (int)(x+width-t.getWidth()-d/2), (int)(y+height-t.getHeight())+ascent);
			t = fm.getStringBounds("S",g);
			g.drawString("S", x+(int)(width-t.getWidth())/2, y+(int)(t.getHeight()));
		
			Font f = g.getFont();
			float fs = f.getSize2D();
			Font nf = f.deriveFont((float)(fs*0.70));
			g.setFont(nf);
			fm = g.getFontMetrics();
			ascent = fm.getAscent();
			t = fm.getStringBounds("Cin",g);
			g.drawString("Cin", x+5, y+height/2+ascent);
			t = fm.getStringBounds("Cout",g);
			g.drawString("Cout", x+(int)(width-t.getWidth()-5), y+height/2+ascent);
			g.setFont(f);
		}
		// draw inputs and outputs
		for (Input input : inputs) {
			input.draw(g);
		}
		for (Output output : outputs) {
			output.draw(g);
		}
		
	} // end of draw method
	
	/**
	 * Set an int instance variable value (during a load).
	 * 
	 * @param name The instance variable name.
	 * @param value The instance variable value.
	 */
	// Declarative persistence (#23): one declaration drives save, load
	// dispatch, and copy for this element's own attributes.
	private static final java.util.List<Attribute> OWN_ATTRIBUTES =
			java.util.List.of(
		new Attribute.IntAttribute("bits") {
			protected int get(Element el) { return ((Adder)el).bits; }
			protected void set(Element el, int v) { ((Adder)el).bits = v; }
		},
		new Attribute.IntAttribute("delay") {
			protected int get(Element el) { return ((Adder)el).propDelay; }
			protected void set(Element el, int v) { ((Adder)el).propDelay = v; }
		},
		new Attribute.OrientationAttribute("orient") {
			protected JLSInfo.Orientation getOrientation(Element el) {
				return ((Adder)el).orientation;
			}
			protected void setOrientation(Element el, JLSInfo.Orientation o) {
				((Adder)el).orientation = o;
			}
		}
	);

	private static final java.util.List<Attribute> ALL_ATTRIBUTES =
			concatAttributes(OWN_ATTRIBUTES);

	/**
	 * Base attributes plus this element's own, in save order (#23).
	 */
	protected java.util.List<Attribute> savedAttributes() {

		return ALL_ATTRIBUTES;
	} // end of savedAttributes method

	/**
	 * Save this element.
	 *
	 * @param output The output writer.
	 */
	public void save(PrintWriter output) {
		
		output.println("ELEMENT Adder");
		super.save(output);
		output.println("END");
	} // end of save method
	
	/**
	 * Copy this element.
	 */
	public Element copy() {
		
		Adder it = new Adder(circuit);
		for (Input input : inputs) {
			it.inputs.add(input.copy(it));
		}
		for (Output output : outputs) {
			it.outputs.add(output.copy(it));
		}
		super.copy(it);
		return it;
	} // end of copy method
	
	/**
	 * Display info about this element.
	 * 
	 * @param info The JLabel to display with.
	 */
	public void showInfo(JLabel info) {
		
		info.setText(bits + " bit adder");
	} // end of showInfo method

	/**
	 * Adders have timing info (propagation delay).
	 * 
	 * @return true.
	 */
	public boolean hasTiming() {
		
		return true;
	} // end of hasTiming method

	/**
	 * Reset propagation delay to default value.
	 */
	public void resetPropDelay() {
		
		propDelay = bits * defaultPropDelay;
	} // end of resetPropDelay method

	/**
	 * Get the propagation delay in this element.
	 * 
	 * @return the current delay.
	 */
	public int getDelay() {
		
		return propDelay;
	} // end of getDelay method
	
	/**
	 * Set the propagation delay in this element.
	 * 
	 * @param amount The new delay amount.
	 */
	public void setDelay(int temp) {
		
		propDelay = temp;
	} // end of setDelay method
	
	/**
	 * Tells if an adder is capable of rotatating, can only rotate when inputs or outputs have no attachments.
	 * @return False if any input or output has a wire attached, True otherwise
	 */
	public boolean canRotate()
	{
		return !(inputs.get(0).isAttached() || inputs.get(1).isAttached() 
				|| inputs.get(2).isAttached() || outputs.get(0).isAttached() 
				|| outputs.get(1).isAttached());
	}
	
	/**
	 *  This method will rotate the adder if it is rotateable.
	 * @param direction The direction to rotate
	 * @param g The current graphics context for use in recalculating size
	 */
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
		init(g);
	}
	
	/**
	 * Tells if an adder is capable of flipping, can only flip when inputs or outputs have no attachments.
	 * @return False if any input or output has a wire attached, True otherwise
	 */
	public boolean canFlip()
	{
		return !(inputs.get(0).isAttached() || inputs.get(1).isAttached() 
				|| inputs.get(2).isAttached() || outputs.get(0).isAttached() 
				|| outputs.get(1).isAttached());
	}
	
	/**
	 * This method will flip an adder
	 * @param g The current graphics context to facilitate recalculation of size when flipping
	 */
	public void flip(Graphics g)
	{
		orientation = orientation.flipped();
		outputs.clear();
		inputs.clear();
		width = 0;
		height = 0;
		init(g);
	}

	/**
	 * Dialog box to set bits.
	 */
	@SuppressWarnings("serial")
	private class AdderCreate extends ElementDialog {
		
		// properties
		private JTextField bitsField = new JTextField(defaultBits+"",10);
		private KeyPad bitsPad = new KeyPad(bitsField,10,defaultBits,this);
		private JRadioButton left = new JRadioButton("Left");
		private JRadioButton right = new JRadioButton("Right", true);
		private JRadioButton up = new JRadioButton("Up");
		private JRadioButton down = new JRadioButton("Down");
		
		/**
		 * Set up create dialog window.
		 * 
		 * @param x The x-coordinate of the position of the dialog.
		 * @param y The y-coordinate of the position of the dialog.
		 */
		private AdderCreate(int x, int y) {
			
			// set up window title
			super("Create Adder","adder");
			
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
			
			confirmOnEnter(bitsField);
			finishDialog(x,y);
		} // end of constructor
		
		/**
		 * Validate the form and create the adder.
		 */
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
		 * Cancel this element.
		 */
		protected void cancelDialog() {
			
			cancelled = true;
			dispose();
		} // end of cancelDialog method
		
	} // end of AdderCreate class
	
//	-------------------------------------------------------------------------------
//	Simulation
//	-------------------------------------------------------------------------------
	
	private BitSet toBeValue;
	
	/**
	 * Initialize this element by setting its output pin and to-be value to 0.
	 * 
	 * @param sim Unused.
	 */
	public void initSim(Simulator sim) {
		
		// set output pins to 0
		BitSet zero = new BitSet(1);
		for (Output output : outputs) {
			output.setValue((BitSet)zero.clone());
		}
		
		// set to-be values
		toBeValue = (BitSet)zero.clone();
	} // end of initSim method
	
	/**
	 * React to an event.
	 * 
	 * @param now The current simulation time.
	 * @param sim The simulator to post events to.
	 * @param todo Unused.
	 */
	public void react(long now, Simulator sim, Object todo) {
		
		// if the input has changed ...
		if (todo == null) {
			
			// get the input values
			BitSet a = inputs.get(0).getValue();
			if (a == null)
				a = new BitSet();
			BitSet b = inputs.get(1).getValue();
			if (b == null)
				b = new BitSet();
			BitSet cin = inputs.get(2).getValue();
			if (cin == null)
				cin = new BitSet();
			boolean c = true;
			if (cin.cardinality() == 0) {
				c = false;
			}
			
			// create new output values
			BitSet allsum = BitSetUtils.SumCarry(c,a,b);
		
			// if new value is different from the value propagating through
			// the adder, then post an event
			if (!allsum.equals(toBeValue)) {
				toBeValue = (BitSet)allsum.clone();
				sim.post(new SimEvent(now+propDelay,this,allsum));
			}
		}
		else {
			
			// get the new output value
			BitSet allsum = (BitSet)todo;
			
			// break into sum and carry
			BitSet sum = (BitSet)allsum.clone();
			BitSet carry = new BitSet(1);
			carry.set(0,sum.get(bits));
			sum.clear(bits);
			
			// send to outputs
			Output sumOut = outputs.get(0);
			sumOut.propagate(sum,now,sim);
			Output carryOut = outputs.get(1);
			carryOut.propagate(carry,now,sim);
		}
	} // end of react method

} // end of Adder class
