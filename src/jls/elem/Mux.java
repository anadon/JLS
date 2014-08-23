package jls.elem;

import jls.*;
import jls.sim.*;
import java.awt.*;
import java.awt.event.*;
import java.io.PrintWriter;
import java.util.BitSet;
import javax.swing.*;

/**
 * Multiplexor.
 * 
 * @author David A. Poplawski
 */
public class Mux extends LogicElement {

	// default values
	private static final int defaultInputs = 2;
	private static final int defaultBits = 1;
	private static final int defaultPropDelay = 25; 
	
	// saved properties
	private int numInputs = defaultInputs;
	private int bits = defaultBits;
	private int propDelay = defaultPropDelay;
	private JLSInfo.Orientation outputOrientation = JLSInfo.Orientation.RIGHT;
	private JLSInfo.Orientation selectorOrientation = JLSInfo.Orientation.DOWN;
	
	// running properties
	private boolean cancelled;

	/**
	 * Create a new multiplexor element.
	 * 
	 * @param circuit The circuit this element is part of.
	 */
	public Mux(Circuit circuit) {
		
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
			new MuxCreate(x+win.x,y+win.y);
		}
		else {
			new MuxCreate(pos.x+win.x,pos.y+win.y);
		}
		
		// don't do anything if user cancelled element
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
		
		// set up size
		int s = JLSInfo.spacing;
		
		if(outputOrientation == JLSInfo.Orientation.LEFT || outputOrientation == JLSInfo.Orientation.RIGHT)
		{
			height = (numInputs+1)*s;
			width = 2*s;
		}
		else
		{
			width = (numInputs+1)*s;
			height = 2*s;
		}
		
		// determine number of select bits
		int sbits = 32 - Integer.numberOfLeadingZeros(numInputs-1);
		
		// create select input
		if(selectorOrientation == JLSInfo.Orientation.DOWN)
		{
			inputs.add(new Input("select",this,s,height,sbits));
		}
		else if(selectorOrientation == JLSInfo.Orientation.UP)
		{
			inputs.add(new Input("select",this,s,0,sbits));
		}
		else if(selectorOrientation == JLSInfo.Orientation.LEFT)
		{
			inputs.add(new Input("select",this,0,s,sbits));
		}
		else if(selectorOrientation == JLSInfo.Orientation.RIGHT)
		{
			inputs.add(new Input("select",this,width,s,sbits));
		}
		
		
		int ypos = s;
		if(outputOrientation == JLSInfo.Orientation.RIGHT)
		{
			// create inputs
			for (int i=0; i<numInputs; i+=1) {
				inputs.add(new Input("input"+i,this,0,ypos,bits));
				ypos += s;
			}
			// create output
			outputs.add(new Output("output",this,width,(numInputs+1)/2*s,bits));
		}
		else if(outputOrientation == JLSInfo.Orientation.LEFT)
		{
			// create inputs
			for (int i=0; i<numInputs; i+=1) {
				inputs.add(new Input("input"+i,this,width,ypos,bits));
				ypos += s;
			}
			// create output
			outputs.add(new Output("output",this,0,(numInputs+1)/2*s,bits));
		}
		else if(outputOrientation == JLSInfo.Orientation.DOWN)
		{
			// create inputs
			for (int i=0; i<numInputs; i+=1) {
				inputs.add(new Input("input"+i,this,ypos,0,bits));
				ypos += s;
			}
			// create output
			outputs.add(new Output("output",this,(numInputs+1)/2*s,height,bits));
		}
		else if(outputOrientation == JLSInfo.Orientation.UP)
		{
			// create inputs
			for (int i=0; i<numInputs; i+=1) {
				inputs.add(new Input("input"+i,this,ypos,height,bits));
				ypos += s;
			}
			// create output
			outputs.add(new Output("output",this,(numInputs+1)/2*s,0,bits));
		}
		
	} // end of init method
	
	/**
	 * Draw this mux.
	 * 
	 * @param g The graphics object to draw with.
	 */
	public void draw(Graphics g) {
		
		// draw context
		super.draw(g);
		
		// draw shape
		int s = JLSInfo.spacing;
		g.setColor(Color.black);
		if(outputOrientation == JLSInfo.Orientation.LEFT || outputOrientation == JLSInfo.Orientation.RIGHT)
		{
			g.drawArc(x,y,2*s,2*s,0,180);
			g.drawLine(x,y+s,x,y+height-s);
			g.drawLine(x+2*s,y+s,x+2*s,y+height-s);
			g.drawArc(x,y+height-2*s,2*s,2*s,180,180);
		}
		else
		{
			g.drawArc(x, y, 2*s, 2*s, -90, -180);
			g.drawLine(x+s,y,x+width-s,y);
			g.drawLine(x+s,y+2*s,x+width-s,y+2*s);
			g.drawArc(x+width-2*s,y,2*s,2*s,-90,180);
		}
		// draw inputs and labels
		FontMetrics fm = g.getFontMetrics();
		int ascent = fm.getAscent();
		int hi = ascent + fm.getDescent();
		int d2 = JLSInfo.pointDiameter/2;
		int inum = -1; // first input is selector, so start one too small
		if(outputOrientation == JLSInfo.Orientation.LEFT || outputOrientation == JLSInfo.Orientation.RIGHT)
		{
			for (Input input : inputs) {
				input.draw(g);
				if (inum >= 0) {
					g.setColor(Color.BLACK);
					if(outputOrientation == JLSInfo.Orientation.RIGHT)
					{
						g.drawString(inum+"",x+d2,input.getY()-hi/2+ascent);			
					}
					else if(outputOrientation == JLSInfo.Orientation.LEFT)
					{
						g.drawString(inum+"",x+width-5*d2,input.getY()-hi/2+ascent);
					}
					else if(outputOrientation == JLSInfo.Orientation.UP || outputOrientation == JLSInfo.Orientation.DOWN)
					{
						g.drawString(inum+"",input.getX()-4,y+5*d2);
					}
				}
				inum += 1;
			}
		}
		if(outputOrientation == JLSInfo.Orientation.UP || outputOrientation == JLSInfo.Orientation.DOWN)
		{
			if(inputs.size() == 3)
			{
				
				inputs.get(0).draw(g);
				inputs.get(1).draw(g);
				inputs.get(2).draw(g);
				g.setColor(Color.BLACK);
				g.drawString("0",inputs.get(1).getX()-4,y+5*d2);
				g.drawString("1",inputs.get(2).getX()-4,y+5*d2);
			}
			else
			{
				for (Input input : inputs) {
					input.draw(g);
					if (inum >= 0 && inum%2 == 0) {
						g.setColor(Color.BLACK);
						if(outputOrientation == JLSInfo.Orientation.RIGHT)
						{
						g.drawString(inum+"",x+d2,input.getY()-hi/2+ascent);			
						}
						else if(outputOrientation == JLSInfo.Orientation.LEFT)
						{
							g.drawString(inum+"",x+width-5*d2,input.getY()-hi/2+ascent);
						}
						else if(outputOrientation == JLSInfo.Orientation.UP || outputOrientation == JLSInfo.Orientation.DOWN)
						{
							g.drawString(inum+"",input.getX()-4,y+5*d2);
						}
					}
					inum += 1;
				}
			}
		}
		
		// draw output
		outputs.get(0).draw(g);
		
	} // end of draw method

	/**
	 * Set an int instance variable value (during a load).
	 * 
	 * @param name The instance variable name.
	 * @param value The instance variable value.
	 */
	public void setValue(String name, int value) {
		
		if (name.equals("inputs")) {
			numInputs = value;
		} else if (name.equals("bits")) {
			bits = value;
		} else if (name.equals("delay")) {
			propDelay = value;
		} else {
			super.setValue(name,value);
		}
	} // end of setValue method
	
	/**
	 * Set an int instance variable value (during a load).
	 * 
	 * @param name The instance variable name.
	 * @param value The instance variable value.
	 */
	public void setValue(String name, String value) {
		
		if (name.equals("iOrient")) {
			if(value.equals("LEFT"))
			{
				outputOrientation = JLSInfo.Orientation.LEFT;
			}
			else if(value.equals("RIGHT"))
			{
				outputOrientation = JLSInfo.Orientation.RIGHT;
			}
			else if(value.equals("UP"))
			{
				outputOrientation = JLSInfo.Orientation.UP;
			}
			else if(value.equals("DOWN"))
			{
				outputOrientation = JLSInfo.Orientation.DOWN;
			}
		} 
		else if(name.equals("sOrient"))
		{
			if(value.equals("LEFT"))
			{
				selectorOrientation = JLSInfo.Orientation.LEFT;
			}
			else if(value.equals("RIGHT"))
			{
				selectorOrientation = JLSInfo.Orientation.RIGHT;
			}
			else if(value.equals("UP"))
			{
				selectorOrientation = JLSInfo.Orientation.UP;
			}
			else if(value.equals("DOWN"))
			{
				selectorOrientation = JLSInfo.Orientation.DOWN;
			}
		}
		else 
		{
			super.setValue(name,value);
		}
	} // end of setValue method
	
	/**
	 * Save this element.
	 * 
	 * @param output The output writer.
	 */
	public void save(PrintWriter output) {
		
		output.println("ELEMENT Mux");
		super.save(output);
		output.println(" int inputs " + numInputs);
		output.println(" int bits " + bits);
		output.println(" int delay " + propDelay);
		output.println(" String iOrient \"" + outputOrientation.toString() + "\"");
		output.println(" String sOrient \"" + selectorOrientation.toString() + "\"");
		output.println("END");
	} // end of save method

	/**
	 * Copy this element.
	 * 
	 * @return a copy of this element.
	 */
	public Element copy() {
		
		Mux it = new Mux(circuit);
		it.numInputs = numInputs;
		it.bits = bits;
		it.propDelay = propDelay;
		it.outputOrientation = outputOrientation;
		it.selectorOrientation = selectorOrientation;
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
		
		info.setText(numInputs + " input, " + bits + " bit multiplexor");
	} // end of showInfo method

	/**
	 * Multiplexors have timing info (propagation delay).
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
	 * Tells if a mux is capable of flipping, can only flip when inputs or outputs have no attachments.
	 * @return False if any input or output has a wire attached, True otherwise
	 */
	public boolean canFlip()
	{
		boolean success = true;
		for(Input i : inputs)
		{
			if(i.isAttached())
			{
				success = false;
				break;
			}
		}
		for(Output o : outputs)
		{
			if(o.isAttached())
			{
				success = false;
				break;
			}
		}
		return success;
	}
	
	/**
	 * This method will flip a mux's selector
	 * @param g The current graphics context to facilitate recalculation of size when flipping
	 */
	public void flip(Graphics g)
	{
		if(selectorOrientation == JLSInfo.Orientation.LEFT)
		{
			selectorOrientation = JLSInfo.Orientation.RIGHT;
		}
		else if(selectorOrientation == JLSInfo.Orientation.RIGHT)
		{
			selectorOrientation = JLSInfo.Orientation.LEFT;
		}
		else if(selectorOrientation == JLSInfo.Orientation.UP)
		{
			selectorOrientation = JLSInfo.Orientation.DOWN;
		}
		else if(selectorOrientation == JLSInfo.Orientation.DOWN)
		{
			selectorOrientation = JLSInfo.Orientation.UP;
		}
		inputs.clear();
		outputs.clear();
		width = 0;
		height = 0;
		init(g);
	}
	
	/**
	 *  This method will rotate the mux if it is rotateable.
	 * @param direction The direction to rotate
	 * @param g The current graphics context for use in recalculating size
	 */
	public void rotate(JLSInfo.Orientation direction, Graphics g)
	{
		if(direction == JLSInfo.Orientation.LEFT)
		{
			if(selectorOrientation == JLSInfo.Orientation.LEFT)
			{
				selectorOrientation = JLSInfo.Orientation.DOWN;
			}
			else if(selectorOrientation == JLSInfo.Orientation.DOWN)
			{
				selectorOrientation = JLSInfo.Orientation.RIGHT;
			}
			else if(selectorOrientation == JLSInfo.Orientation.RIGHT)
			{
				selectorOrientation = JLSInfo.Orientation.UP;
			}
			else if(selectorOrientation == JLSInfo.Orientation.UP)
			{
				selectorOrientation = JLSInfo.Orientation.LEFT;
			}
			
			if(outputOrientation == JLSInfo.Orientation.LEFT)
			{
				outputOrientation = JLSInfo.Orientation.DOWN;
			}
			else if(outputOrientation == JLSInfo.Orientation.DOWN)
			{
				outputOrientation = JLSInfo.Orientation.RIGHT;
			}
			else if(outputOrientation == JLSInfo.Orientation.RIGHT)
			{
				outputOrientation = JLSInfo.Orientation.UP;
			}
			else if(outputOrientation == JLSInfo.Orientation.UP)
			{
				outputOrientation = JLSInfo.Orientation.LEFT;
			}
			
		}
		else if(direction == JLSInfo.Orientation.RIGHT)
		{
			if(selectorOrientation == JLSInfo.Orientation.LEFT)
			{
				selectorOrientation = JLSInfo.Orientation.UP;
			}
			else if(selectorOrientation == JLSInfo.Orientation.DOWN)
			{
				selectorOrientation = JLSInfo.Orientation.LEFT;
			}
			else if(selectorOrientation == JLSInfo.Orientation.RIGHT)
			{
				selectorOrientation = JLSInfo.Orientation.DOWN;
			}
			else if(selectorOrientation == JLSInfo.Orientation.UP)
			{
				selectorOrientation = JLSInfo.Orientation.RIGHT;
			}
			
			if(outputOrientation == JLSInfo.Orientation.LEFT)
			{
				outputOrientation = JLSInfo.Orientation.UP;
			}
			else if(outputOrientation == JLSInfo.Orientation.DOWN)
			{
				outputOrientation = JLSInfo.Orientation.LEFT;
			}
			else if(outputOrientation == JLSInfo.Orientation.RIGHT)
			{
				outputOrientation = JLSInfo.Orientation.DOWN;
			}
			else if(outputOrientation == JLSInfo.Orientation.UP)
			{
				outputOrientation = JLSInfo.Orientation.RIGHT;
			}
		}
		inputs.clear();
		outputs.clear();
		width = 0;
		height = 0;
		init(g);
	}
	
	/**
	 * Tells if a mux is capable of rotatating, can only rotate when inputs or outputs have no attachments.
	 * @return False if any input or output has a wire attached, True otherwise
	 */
	public boolean canRotate()
	{
		boolean success = true;
		for(Input i : inputs)
		{
			if(i.isAttached())
			{
				success = false;
				break;
			}
		}
		for(Output o : outputs)
		{
			if(o.isAttached())
			{
				success = false;
				break;
			}
		}
		return success;
	}

	/**
	 * Dialog box to set inputs and bits.
	 */
	protected class MuxCreate extends JDialog implements ActionListener {
		
		// properties
		private JButton ok = new JButton("OK");
		private JButton cancel = new JButton("Cancel");
		private JTextField inputsField = new JTextField(defaultInputs+"",5);
		private JTextField bitsField = new JTextField(defaultBits+"",5);
		private KeyPad inputsPad = new KeyPad(inputsField,10,defaultInputs,this);
		private KeyPad bitsPad = new KeyPad(bitsField,10,defaultBits,this);
		private JRadioButton oLeft = new JRadioButton("Left");
		private JRadioButton oRight = new JRadioButton("Right", true);
		private JRadioButton oUp = new JRadioButton("Up");
		private JRadioButton oDown = new JRadioButton("Down");
		private JRadioButton sLeft = new JRadioButton("Left");
		private JRadioButton sRight = new JRadioButton("Right");
		private JRadioButton sUp = new JRadioButton("Up");
		private JRadioButton sDown = new JRadioButton("Down",true);
		private JLabel olbl2 = new JLabel("Selector Orientation");
		
		/**
		 * Set up dialog window.
		 * 
		 * @param x The x-coordinate of the position of the dialog.
		 * @param y The y-coordinate of the position of the dialog.
		 */
		protected MuxCreate(int x, int y) {
			
			// set up window title
			super(JLSInfo.frame,"Create Multiplexor",true);
			
			// set not cancelled
			cancelled = false;
			
			// set up window
			Container window = getContentPane();
			window.setLayout(new BoxLayout(window,BoxLayout.Y_AXIS));
			
			// set up input panel
			JPanel info = null;
			info = new JPanel(new GridLayout(2,2));
			JLabel inputs = new JLabel("Inputs: ",SwingConstants.RIGHT);
			info.add(inputs);
			JPanel in = new JPanel(new FlowLayout());
			in.add(inputsField);
			in.add(inputsPad);
			info.add(in);
			
			JLabel gates;
			gates = new JLabel("Bits: ",SwingConstants.RIGHT);
			info.add(gates);
			JPanel ga = new JPanel(new FlowLayout());
			ga.add(bitsField);
			ga.add(bitsPad);
			info.add(ga);
			window.add(info);
			
			JPanel orient = new JPanel(new GridLayout(3,3));
			JPanel orient2 = new JPanel(new GridLayout(3,3));
			ButtonGroup gr = new ButtonGroup();
			ButtonGroup gr2 = new ButtonGroup();
			gr.add(this.oLeft);
			gr.add(this.oRight);
			gr.add(this.oDown);
			gr.add(this.oUp);
			gr2.add(this.sDown);
			gr2.add(this.sUp);
			gr2.add(this.sLeft);
			gr2.add(this.sRight);
			orient.add(new JLabel(""));
			orient.add(this.oUp);
			orient.add(new JLabel(""));
			orient.add(this.oLeft);
			orient.add(new JLabel(""));
			orient.add(this.oRight);
			orient.add(new JLabel(""));
			orient.add(this.oDown);
			orient.add(new JLabel(""));
			
			
			orient2.add(new JLabel(""));
			orient2.add(this.sUp);
			orient2.add(new JLabel(""));
			orient2.add(this.sLeft);
			orient2.add(new JLabel(""));
			orient2.add(this.sRight);
			orient2.add(new JLabel(""));
			orient2.add(this.sDown);
			orient2.add(new JLabel(""));
			
			JLabel olbl = new JLabel("Output Orientation");
			olbl.setAlignmentX(Component.CENTER_ALIGNMENT);
			window.add(olbl);
			window.add(orient);

			olbl2.setAlignmentX(Component.CENTER_ALIGNMENT);
			window.add(olbl2);
			window.add(orient2);
			
			sLeft.setVisible(false);
			sRight.setVisible(false);
			
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
				JLSInfo.hb.enableHelpOnButton(help,"mux",null);
			okCancel.add(help);
			window.add(okCancel);
			getRootPane().setDefaultButton(ok);
			
			ok.addActionListener(this);
			inputsField.addActionListener(this);
			bitsField.addActionListener(this);
			cancel.addActionListener(this);
			oLeft.addActionListener(this);
			oRight.addActionListener(this);
			oUp.addActionListener(this);
			oDown.addActionListener(this);
			
			// set up window close listener to cancel mux
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
		 * React to ok and cancel buttons.
		 * 
		 * @param event The event object for this action.
		 */
		public void actionPerformed(ActionEvent event) {
			
			if(event.getSource() == oLeft || event.getSource() == oRight)
			{
				olbl2.setVisible(true);
				sUp.setVisible(true);
				sDown.setVisible(true);
				sDown.setSelected(true);
				sLeft.setVisible(false);
				sRight.setVisible(false);
				return;
			}
			else if(event.getSource() == oUp || event.getSource() == oDown)
			{
				olbl2.setVisible(true);
				sLeft.setVisible(true);
				sLeft.setSelected(true);
				sRight.setVisible(true);
				sUp.setVisible(false);
				sDown.setVisible(false);
				return;
			}
			
			// if ok button, check values for validity
			if (event.getSource() == ok || event.getSource() == inputsField || event.getSource() == bitsField) {
				try {
					numInputs = Integer.parseInt(inputsField.getText());
					bits = Integer.parseInt(bitsField.getText());
				}
				catch (NumberFormatException ex) {
					JOptionPane.showMessageDialog(this,
							"Value not numeric, try again", "Error",
							JOptionPane.ERROR_MESSAGE);
					numInputs = -1;
					return;
				}
				if (numInputs < 2) {
					JOptionPane.showMessageDialog(this,
							"Must have at least 2 inputs", "Error",
							JOptionPane.ERROR_MESSAGE);
					numInputs = -1;
					return;
				}
				if (bits < 1) {
					JOptionPane.showMessageDialog(this,
							"Must be at least one bit", "Error",
							JOptionPane.ERROR_MESSAGE);
					numInputs = -1;
					return;
				}
				if(this.oLeft.isSelected())
				{
					outputOrientation = JLSInfo.Orientation.LEFT;
					if(this.sUp.isSelected())
					{
						selectorOrientation = JLSInfo.Orientation.UP;
					}
					else if(this.sDown.isSelected())
					{
						selectorOrientation = JLSInfo.Orientation.DOWN;
					}
				}
				else if(this.oRight.isSelected())
				{
					outputOrientation = JLSInfo.Orientation.RIGHT;
					if(this.sUp.isSelected())
					{
						selectorOrientation = JLSInfo.Orientation.UP;
					}
					else if(this.sDown.isSelected())
					{
						selectorOrientation = JLSInfo.Orientation.DOWN;
					}
				}
				else if(this.oDown.isSelected())
				{
					outputOrientation = JLSInfo.Orientation.DOWN;
					if(this.sLeft.isSelected())
					{
						selectorOrientation = JLSInfo.Orientation.LEFT;
					}
					else if(this.sRight.isSelected())
					{
						selectorOrientation = JLSInfo.Orientation.RIGHT;
					}
				}
				else if(this.oUp.isSelected())
				{
					outputOrientation = JLSInfo.Orientation.UP;
					if(this.sLeft.isSelected())
					{
						selectorOrientation = JLSInfo.Orientation.LEFT;
					}
					else if(this.sRight.isSelected())
					{
						selectorOrientation = JLSInfo.Orientation.RIGHT;
					}
				}
				inputsPad.close();
				bitsPad.close();
				dispose();
			}
			
			// if cancel button, cancel mux creation
			else if (event.getSource() == cancel) {
				cancel();
			}
			
		} // end of actionPerformed method
		
		/**
		 * Cancel this mux.
		 */
		private void cancel() {
			
			cancelled = true;
			inputsPad.close();
			bitsPad.close();
			dispose();
		} // end of cancel method
		
	} // end of MuxCreate class
	

//	-------------------------------------------------------------------------------
//	Simulation
//	-------------------------------------------------------------------------------
	
	private BitSet toBeValue;
	
	/**
	 * Initialize this element by setting its output and to-be value to 0.
	 * 
	 * @param sim Unused.
	 */
	public void initSim(Simulator sim) {
		
		// set outputs to 0
		BitSet zero = new BitSet(1);
		outputs.get(0).setValue(zero);
		
		// set to-be value
		toBeValue = (BitSet)zero.clone();
	} // end of initSim method
	
	/**
	 * React to an event.
	 * 
	 * @param now The current simulation time.
	 * @param sim The simulator to post events to.
	 * @param todo Null if an input change, the new output value otherwise.
	 */
	public void react(long now, Simulator sim, Object todo) {
		
		// if an input has changed ...
		if (todo == null) {
			
			// get the selector input
			BitSet bw = inputs.get(0).getValue();
			if (bw == null)
				bw = new BitSet();
			int which = BitSetUtils.ToInt(bw);
			
			// get the selected input
			BitSet newValue;
			if (which >= numInputs) {
				newValue = new BitSet(1);
			}
			else {
				newValue = inputs.get(which+1).getValue();
				if (newValue == null)
					newValue = new BitSet();
			}
	
			// if new value is different from the value propagating through
			// the mux, then post an event
			if (!newValue.equals(toBeValue)) {
				toBeValue = (BitSet)newValue.clone();
				sim.post(new SimEvent(now+propDelay,this,newValue));
			}
		}
		else {
			
			// get the new output value
			BitSet value = (BitSet)todo;
			
			// send to output
			Output sumOut = outputs.get(0);
			sumOut.propagate(value,now,sim);
		}
		
	} // end of react method

} // end of Mux class
