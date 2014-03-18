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
		
		int s = JLSInfo.spacing;
		height = 4*s;
		width = 4*s;
		
		if(orientation == JLSInfo.Orientation.RIGHT)
		{
			// create inputs
			inputs.add(new Input("A",this,0,s,bits));
			inputs.add(new Input("B",this,0,3*s,bits));
			inputs.add(new Input("Cin",this,width/2,0,1));
		
			// create output
			outputs.add(new Output("S",this,width,2*s,bits));
			outputs.add(new Output("Cout",this,width/2,height,1));
		}
		else if(orientation == JLSInfo.Orientation.LEFT)
		{
			// create inputs
			inputs.add(new Input("A",this,width,s,bits));
			inputs.add(new Input("B",this,width,3*s,bits));
			inputs.add(new Input("Cin",this,width/2,0,1));
		
			// create output
			outputs.add(new Output("S",this,0,2*s,bits));
			outputs.add(new Output("Cout",this,width/2,height,1));
		}
		else if(orientation == JLSInfo.Orientation.DOWN)
		{
			// create inputs
			inputs.add(new Input("A",this,s,0,bits));
			inputs.add(new Input("B",this,3*s,0,bits));
			inputs.add(new Input("Cin",this,0,height/2,1));
		
			// create output
			outputs.add(new Output("S",this,2*s,height,bits));
			outputs.add(new Output("Cout",this,width,height/2,1));
		}
		else if(orientation == JLSInfo.Orientation.UP)
		{
			// create inputs
			inputs.add(new Input("A",this,s,height,bits));
			inputs.add(new Input("B",this,3*s,height,bits));
			inputs.add(new Input("Cin",this,0,height/2,1));
		
			// create output
			outputs.add(new Output("S",this,2*s,0,bits));
			outputs.add(new Output("Cout",this,width,height/2,1));
		}
	} // end of init method
	
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
			int descent = fm.getDescent();
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
			descent = fm.getDescent();
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
	public void setValue(String name, int value) {
		
		if (name.equals("bits")) {
			bits = value;
		} else if (name.equals("delay")) {
			propDelay = value;
		} else {
			super.setValue(name,value);
		}
	} // end of setValue method
	
	/**
	 * Set a String instance variable value (during a load).
	 * 
	 * @param name The instance variable name.
	 * @param value The instance variable value.
	 */
	public void setValue(String name, String value) {
		
		if (name.equals("orient")) {
			if(value.equals("LEFT"))
			{
				orientation = JLSInfo.Orientation.LEFT;
			}
			else if(value.equals("RIGHT"))
			{
				orientation = JLSInfo.Orientation.RIGHT;
			}
			else if(value.equals("UP"))
			{
				orientation = JLSInfo.Orientation.UP;
			}
			else if(value.equals("DOWN"))
			{
				orientation = JLSInfo.Orientation.DOWN;
			}
			
		} else {
			super.setValue(name,value);
		}
	} // end of setValue method
	
	/**
	 * Save this element.
	 * 
	 * @param output The output writer.
	 */
	public void save(PrintWriter output) {
		
		output.println("ELEMENT Adder");
		super.save(output);
		output.println(" int bits " + bits);
		output.println(" int delay " + propDelay);
		output.println(" String orient \"" + orientation.toString() + "\"");
		output.println("END");
	} // end of save method
	
	/**
	 * Copy this element.
	 */
	public Element copy() {
		
		Adder it = new Adder(circuit);
		it.bits = bits;
		it.propDelay = propDelay;
		it.orientation = orientation;
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
			if(orientation == JLSInfo.Orientation.LEFT)
			{
				orientation = JLSInfo.Orientation.DOWN;
			}
			else if(orientation == JLSInfo.Orientation.DOWN)
			{
				orientation = JLSInfo.Orientation.RIGHT;
			}
			else if(orientation == JLSInfo.Orientation.RIGHT)
			{
				orientation = JLSInfo.Orientation.UP;
			}
			else if(orientation == JLSInfo.Orientation.UP)
			{
				orientation = JLSInfo.Orientation.LEFT;
			}
			
		}
		else if(direction == JLSInfo.Orientation.RIGHT)
		{
			if(orientation == JLSInfo.Orientation.LEFT)
			{
				orientation = JLSInfo.Orientation.UP;
			}
			else if(orientation == JLSInfo.Orientation.DOWN)
			{
				orientation = JLSInfo.Orientation.LEFT;
			}
			else if(orientation == JLSInfo.Orientation.RIGHT)
			{
				orientation = JLSInfo.Orientation.DOWN;
			}
			else if(orientation == JLSInfo.Orientation.UP)
			{
				orientation = JLSInfo.Orientation.RIGHT;
			}
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
		if(orientation == JLSInfo.Orientation.LEFT)
		{
			orientation = JLSInfo.Orientation.RIGHT;
		}
		else if(orientation == JLSInfo.Orientation.RIGHT)
		{
			orientation = JLSInfo.Orientation.LEFT;
		}
		else if(orientation == JLSInfo.Orientation.UP)
		{
			orientation = JLSInfo.Orientation.DOWN;
		}
		else if(orientation == JLSInfo.Orientation.DOWN)
		{
			orientation = JLSInfo.Orientation.UP;
		}
		outputs.clear();
		inputs.clear();
		width = 0;
		height = 0;
		init(g);
	}

	/**
	 * Dialog box to set bits.
	 */
	private class AdderCreate extends JDialog implements ActionListener {
		
		// properties
		private JButton ok = new JButton("OK");
		private JButton cancel = new JButton("Cancel");
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
			super(JLSInfo.frame,"Create Adder",true);
			
			// set not cancelled
			cancelled = false;
			
			// set up window
			Container window = getContentPane();
			window.setLayout(new BoxLayout(window,BoxLayout.Y_AXIS));
			
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
			
			// set up ok and cancel buttons
			window.add(new JLabel(" "));
			JPanel okCancel = new JPanel(new GridLayout(1,3));
			ok.setBackground(Color.green);
			okCancel.add(ok);
			cancel.setBackground(Color.pink);
			okCancel.add(cancel);
			JButton help = new JButton("Help");
			if (JLSInfo.hb == null)
				Util.noHelp(help);
			else
				JLSInfo.hb.enableHelpOnButton(help,"adder",null);
			okCancel.add(help);
			window.add(okCancel);
			getRootPane().setDefaultButton(ok);
			
			ok.addActionListener(this);
			bitsField.addActionListener(this);
			cancel.addActionListener(this);
			
			// set up window close listener to cancel gate
			setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			addWindowListener (
					new WindowAdapter() {
						public void windowClosing(WindowEvent e) {
							cancel();
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
			
			if (event.getSource() == ok || event.getSource() == bitsField) {
				try {
					bits = Integer.parseInt(bitsField.getText());
				}
				catch (NumberFormatException ex) {
					JOptionPane.showMessageDialog(this,
							"Value not numeric, try again", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (bits < 1) {
					JOptionPane.showMessageDialog(this,
							"Must be at least 1 bit", "Error",
							JOptionPane.ERROR_MESSAGE);
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
			}
			else if (event.getSource() == cancel) {
				cancel();
			}
			
			
		} // end of actionPerformed method
		
		/**
		 * Cancel this element.
		 */
		private void cancel() {
			
			cancelled = true;
			dispose();
		} // end of cancel method
		
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
