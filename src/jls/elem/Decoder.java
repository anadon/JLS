package jls.elem;

import jls.*;
import jls.elem.Gate.Orientation;
import jls.sim.*;
import java.awt.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

import java.awt.event.*;
import java.awt.geom.*;

/**
 * n-input, 2^n-output decoder.
 * 
 * @author David A. Poplawski
 */
public class Decoder extends LogicElement {
	
	// default values
	private static final int defaultBits = 1;
	private static final int defaultPropDelay = 15; 
	
	// saved properties
	private int bits = defaultBits;
	private int propDelay = defaultPropDelay;
	//Orientation is based off of where the inputs are
	private JLSInfo.Orientation orientation = JLSInfo.Orientation.LEFT;
	
	// running properties
	private boolean cancelled;
	private String dec;
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
	public boolean setup(Graphics g, JPanel editWindow, int x, int y) {
		
		// show creation dialog
		Point pos = editWindow.getMousePosition();
		Point win = editWindow.getLocationOnScreen();
		if (pos == null) {
			new DecoderCreate(x+win.x,y+win.y);
		}
		else {
			new DecoderCreate(pos.x+win.x,pos.y+win.y);
		}
		
		// don't do anything if user cancelled gate
		if (cancelled)
			return false;
		
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
			inout = outs + "/n | /n" + bits;
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
		
		output.println("ELEMENT Decoder");
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
		
		Decoder it = new Decoder(circuit);
		it.bits = bits;
		it.propDelay = propDelay;
		it.inout = new String(inout);
		it.dec = new String(dec);
		it.orientation = orientation;
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
	public void showInfo(JLabel info) {
		
		info.setText(bits + " to " + (1<<bits) + " decoder");

	} // end of showInfo method

	/**
	 * Decoders have timing info (propagation delay).
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
		
		propDelay = defaultPropDelay;
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
	 * Tells if a decoder is capable of rotatating, can only rotate when inputs or outputs have no attachments.
	 * @return False if any input or output has a wire attached, True otherwise
	 */
	public boolean canRotate()
	{
		return !(inputs.get(0).isAttached() || outputs.get(0).isAttached());
	}
	
	/**
	 *  This method will rotate the decoder if it is rotateable.
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
		width = 0;
		height = 0;
		init(g);
	}
	
	/**
	 * This method will flip a decoder
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
	public boolean canFlip()
	{
		return !(inputs.get(0).isAttached() || outputs.get(0).isAttached());
	}

	/**
	 * Dialog box to set bits.
	 */
	private class DecoderCreate extends JDialog implements ActionListener {
		
		// properties
		private JButton ok = new JButton("OK");
		private JButton cancel = new JButton("Cancel");
		private JTextField bitsField = new JTextField(defaultBits+"",10);
		private KeyPad bitsPad = new KeyPad(bitsField,10,defaultBits,this);
		private JRadioButton left = new JRadioButton("Left", true);
		private JRadioButton right = new JRadioButton("Right");
		private JRadioButton up = new JRadioButton("Up");
		private JRadioButton down = new JRadioButton("Down");
		
		/**
		 * Set up create dialog window.
		 * 
		 * @param x The x-coordinate of the position of the dialog.
		 * @param y The y-coordinate of the position of the dialog.
		 */
		private DecoderCreate(int x, int y) {
			
			// set up window title
			super(JLSInfo.frame,"Create Decoder",true);
			
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
			
			// set up ok and cancel buttons
			window.add(new JLabel(" "));
			JPanel okCancel = new JPanel(new GridLayout(1,2));
			ok.setBackground(Color.green);
			okCancel.add(ok);
			cancel.setBackground(Color.pink);
			okCancel.add(cancel);
			JButton help = new JButton("Help");
			if (JLSInfo.hb == null)
				Util.noHelp(help);
			else
				JLSInfo.hb.enableHelpOnButton(help,"decoder",null);
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
				if (bits >= 32) {
					JOptionPane.showMessageDialog(this,
							"Must be less than 32 bits", "Error",
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
		 * Cancel this gate.
		 */
		private void cancel() {
			
			cancelled = true;
			dispose();
		} // end of cancel method
		
	} // end of DecoderCreate class
	
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
