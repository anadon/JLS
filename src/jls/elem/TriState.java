package jls.elem;

import jls.*;
import jls.sim.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.PrintWriter;
import java.util.BitSet;

import javax.swing.*;

/**
 * Tri-state buffer(s).
 * 
 * @author David A. Poplawski
 */
public class TriState extends LogicElement {
	
	// defaults
	private static final int defaultBits = 1;
	private static final int defaultPropDelay = 5;
	
	// saved properties
	private int bits = defaultBits;
	private int propDelay = defaultPropDelay;
	private JLSInfo.Orientation gateOrientation = JLSInfo.Orientation.RIGHT;
	private JLSInfo.Orientation controlOrientation = JLSInfo.Orientation.DOWN;
	// running properties
	private boolean cancelled;
	
	/**
	 * Create a new Gate object.
	 * Subclass constructors do most of the work.
	 * 
	 * @param circuit
	 */
	public TriState(Circuit circuit) {
		
		super(circuit);
	} // end of constructor
	
	/**
	 * Initialize this element in a GUI context.
	 * 
	 * @param g The graphics object to use.
	 * @param editWindow The editor window this circuit is displayed in.
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
			new TriStateCreate(x+win.x,y+win.y);
		}
		else {
			new TriStateCreate(pos.x+win.x,pos.y+win.y);
		}
		
		// don't do anything if user cancelled gate
		if (cancelled)
			return false;
		
		// set up inputs and outputs
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
	 * Sets up size, inputs and output.
	 * 
	 * @param g Unused.
	 */
	public void init(Graphics g) {
		
		// set up size
		int s = JLSInfo.spacing;
	
		// set up inputs and outputs
		Output out = null;
		if(gateOrientation == JLSInfo.Orientation.RIGHT) {
			width = 4*s;
			height = 3*s;
			if(controlOrientation == JLSInfo.Orientation.DOWN) {
				inputs.add(new Input("input",this,0,s,bits));
				inputs.add(new Input("control",this,2*s,3*s,1));
				out = new Output("output",this,4*s,s,bits);
			}
			else if(controlOrientation == JLSInfo.Orientation.UP) {
				inputs.add(new Input("input",this,0,2*s,bits));
				inputs.add(new Input("control",this,2*s,0,1));
				out = new Output("output",this,4*s,2*s,bits);
			}
		}
		else if(gateOrientation == JLSInfo.Orientation.LEFT) {
			width = 4*s;
			height = 3*s;
			if(controlOrientation == JLSInfo.Orientation.DOWN) {
				inputs.add(new Input("input",this,4*s,s,bits));
				inputs.add(new Input("control",this,2*s,3*s,1));
				out = new Output("output",this,0,s,bits);
			}
			else if(controlOrientation == JLSInfo.Orientation.UP) {
				inputs.add(new Input("input",this,4*s,2*s,bits));
				inputs.add(new Input("control",this,2*s,0,1));
				out = new Output("output",this,0,2*s,bits);
			}
		}
		else if(gateOrientation == JLSInfo.Orientation.UP) {
			width = 3*s;
			height = 4*s;
			if(controlOrientation == JLSInfo.Orientation.LEFT) {
				inputs.add(new Input("input",this,2*s,4*s,bits));
				inputs.add(new Input("control",this,0,2*s,1));
				out = new Output("output",this,2*s,0,bits);
			}
			else if(controlOrientation == JLSInfo.Orientation.RIGHT) {
				inputs.add(new Input("input",this,s,4*s,bits));
				inputs.add(new Input("control",this,3*s,2*s,1));
				out = new Output("output",this,s,0,bits);
			}
		}
		else if(gateOrientation == JLSInfo.Orientation.DOWN) {
			width = 3*s;
			height = 4*s;
			if(controlOrientation == JLSInfo.Orientation.LEFT) {
				inputs.add(new Input("input",this,2*s,0,bits));
				inputs.add(new Input("control",this,0,2*s,1));
				out  = new Output("output",this,2*s,4*s,bits);
			}
			else if(controlOrientation == JLSInfo.Orientation.RIGHT) {
				inputs.add(new Input("input",this,s,0,bits));
				inputs.add(new Input("control",this,3*s,2*s,1));
				out  = new Output("output",this,s,4*s,bits);
			}
		}
		outputs.add(out);
		out.setTriState(true);
	} // end of init method
	
	/**
	 * Draw this gate.
	 * 
	 * @param g The graphics object to draw with.
	 */
	public void draw(Graphics g) {
		
		// draw context
		super.draw(g);
		
		// draw gate
		int s = JLSInfo.spacing;
		g.setColor(Color.black);
		if(gateOrientation == JLSInfo.Orientation.RIGHT) {
			int offset = 0;
			if(controlOrientation == JLSInfo.Orientation.UP) {
				offset = s;
			}
			g.drawLine(x,y+s+offset,x+s,y+s+offset);	// input wire
			g.drawLine(x+s,y+offset,x+s,y+2*s+offset);	// back
			g.drawLine(x+s,y+offset,x+3*s,y+s+offset);	// top
			g.drawLine(x+s,y+2*s+offset,x+3*s,y+s+offset);	// bottom
			g.drawLine(x+3*s,y+s+offset,x+4*s,y+s+offset);	// output wire
			if(controlOrientation == JLSInfo.Orientation.DOWN) {
				g.drawLine(x+2*s,(int)(y+2*s-.5*s),x+2*s,y+3*s);  // control wire
			}
			else {
				g.drawLine(x+2*s,(int)(y+s+.5*s),x+2*s,y);	// control wire
			}
		}
		else if(gateOrientation == JLSInfo.Orientation.LEFT) {
			int offset = 0;
			if(controlOrientation == JLSInfo.Orientation.UP) {
				offset = s;
			}
			g.drawLine(x,y+s+offset,x+s,y+s+offset);	// output wire
			g.drawLine(x+3*s,y+offset,x+3*s,y+2*s+offset);	// back
			g.drawLine(x+3*s,y+offset,x+s,y+s+offset);	// top
			g.drawLine(x+3*s,y+2*s+offset,x+s,y+s+offset);	// bottom
			g.drawLine(x+3*s,y+s+offset,x+4*s,y+s+offset);	// input wire
			if(controlOrientation == JLSInfo.Orientation.DOWN) {
				g.drawLine(x+2*s,(int)(y+2*s-.5*s),x+2*s,y+3*s);  // control wire
			}
			else {
				g.drawLine(x+2*s,(int)(y+s+.5*s),x+2*s,y);	// control wire
			}
		}
		else if(gateOrientation == JLSInfo.Orientation.UP) {
			int offset = 0;
			if(controlOrientation == JLSInfo.Orientation.LEFT) {
				offset = s;
			}
			g.drawLine(x+s+offset,y+3*s,x+s+offset,y+4*s);	// output wire
			g.drawLine(x+offset,y+3*s,x+2*s+offset,y+3*s);	// back
			g.drawLine(x+s+offset,y+s,x+2*s+offset,y+3*s);	// top
			g.drawLine(x+s+offset,y+s,x+offset,y+3*s);	// bottom
			g.drawLine(x+s+offset,y+s,x+s+offset,y);	// input wire
			if(controlOrientation == JLSInfo.Orientation.LEFT) {
				g.drawLine(x,y+2*s,(int)(x+s+.5*s),y+2*s);	// control wire
			}
			else if(controlOrientation == JLSInfo.Orientation.RIGHT) {
				g.drawLine((int)(x+s+.5*s),y+2*s,x+3*s,y+2*s);	// control wire
			}
		}
		else if(gateOrientation == JLSInfo.Orientation.DOWN) {
			int offset = 0;
			if(controlOrientation == JLSInfo.Orientation.LEFT) {
				offset = s;
			}
			g.drawLine(x+s+offset,y+3*s,x+s+offset,y+4*s);	// input wire
			g.drawLine(x+offset,y+s,x+2*s+offset,y+s);	// back
			g.drawLine(x+offset,y+s,x+s+offset,y+3*s);	// top
			g.drawLine(x+s+offset,y+3*s,x+2*s+offset,y+s);	// bottom
			g.drawLine(x+s+offset,y+s,x+s+offset,y);	// output wire
			if(controlOrientation == JLSInfo.Orientation.LEFT) {
				g.drawLine(x,y+2*s,(int)(x+s+.5*s),y+2*s);	// control wire
			}
			else if(controlOrientation == JLSInfo.Orientation.RIGHT) {
				g.drawLine((int)(x+s+.5*s),y+2*s,x+3*s,y+2*s);	// control wire
			}
		}
		// draw inputs and outputs
		inputs.get(0).draw(g);
		inputs.get(1).draw(g);
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
	 * Save this element in a file.
	 * 
	 * @param output The output writer.
	 */
	public void save(PrintWriter output) {
		
		output.println("ELEMENT TriState");
		super.save(output);
		output.println(" int bits " + bits);
		output.println(" int delay " + propDelay);
		output.println(" String Gorient \"" + gateOrientation.toString() + "\"");
		output.println(" String Corient \"" + controlOrientation.toString() + "\"");
		output.println("END");
	} // end of save method
	
	/**
	 * Set an int instance variable value (during a load).
	 * 
	 * @param name The instance variable name.
	 * @param value The instance variable value.
	 */
	public void setValue(String name, String value) {
		
		if (name.equals("Gorient")) {
			if(value.equals("LEFT"))
			{
				gateOrientation = JLSInfo.Orientation.LEFT;
			}
			else if(value.equals("RIGHT"))
			{
				gateOrientation = JLSInfo.Orientation.RIGHT;
			}
			else if(value.equals("UP"))
			{
				gateOrientation = JLSInfo.Orientation.UP;
			}
			else if(value.equals("DOWN"))
			{
				gateOrientation = JLSInfo.Orientation.DOWN;
			}
		} 
		else if(name.equals("Corient"))
		{
			if(value.equals("LEFT"))
			{
				controlOrientation = JLSInfo.Orientation.LEFT;
			}
			else if(value.equals("RIGHT"))
			{
				controlOrientation = JLSInfo.Orientation.RIGHT;
			}
			else if(value.equals("UP"))
			{
				controlOrientation = JLSInfo.Orientation.UP;
			}
			else if(value.equals("DOWN"))
			{
				controlOrientation = JLSInfo.Orientation.DOWN;
			}
		}
		else 
		{
			super.setValue(name,value);
		}
	} // end of setValue method
	
	/**
	 * Copy this element.
	 * 
	 * @return a copy of this tri-state.
	 */
	public Element copy() {
		
		TriState it = new TriState(circuit);
		it.bits = bits;
		it.gateOrientation = gateOrientation;
		it.controlOrientation = controlOrientation;
		it.propDelay = propDelay;
		it.inputs.add(inputs.get(0).copy(it));
		it.inputs.add(inputs.get(1).copy(it));
		it.outputs.add(outputs.get(0).copy(it));
		it.outputs.get(0).setTriState(true);
		super.copy(it);
		return it;
	} // end of copy method
	
	/**
	 * Display info about this and gate.
	 * 
	 * @param info The JLabel to display with.
	 */
	public void showInfo(JLabel info) {
		
		if (bits == 1)
			info.setText("tri-state gate");
		else
			info.setText(bits + " tri-state gates");
	} // end of showInfo method

	/**
	 * Tri-states have timing info (propagation delay).
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
	 * Tells if a tristate is capable of flipping, can only flip when inputs or outputs have no attachments.
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
	 * This method will flip a tristate's control input
	 * @param g The current graphics context to facilitate recalculation of size when flipping
	 */
	public void flip(Graphics g)
	{
		if(controlOrientation == JLSInfo.Orientation.LEFT)
		{
			controlOrientation = JLSInfo.Orientation.RIGHT;
		}
		else if(controlOrientation == JLSInfo.Orientation.RIGHT)
		{
			controlOrientation = JLSInfo.Orientation.LEFT;
		}
		else if(controlOrientation == JLSInfo.Orientation.UP)
		{
			controlOrientation = JLSInfo.Orientation.DOWN;
		}
		else if(controlOrientation == JLSInfo.Orientation.DOWN)
		{
			controlOrientation = JLSInfo.Orientation.UP;
		}
		inputs.clear();
		outputs.clear();
		width = 0;
		height = 0;
		init(g);
	}
	
	/**
	 * Tells if a tristate is capable of rotatating, can only rotate when inputs or outputs have no attachments.
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
	 *  This method will rotate the tristate if it is rotateable.
	 * @param direction The direction to rotate
	 * @param g The current graphics context for use in recalculating size
	 */
	public void rotate(JLSInfo.Orientation direction, Graphics g)
	{
		if(direction == JLSInfo.Orientation.LEFT)
		{
			if(controlOrientation == JLSInfo.Orientation.LEFT)
			{
				controlOrientation = JLSInfo.Orientation.DOWN;
			}
			else if(controlOrientation == JLSInfo.Orientation.DOWN)
			{
				controlOrientation = JLSInfo.Orientation.RIGHT;
			}
			else if(controlOrientation == JLSInfo.Orientation.RIGHT)
			{
				controlOrientation = JLSInfo.Orientation.UP;
			}
			else if(controlOrientation == JLSInfo.Orientation.UP)
			{
				controlOrientation = JLSInfo.Orientation.LEFT;
			}
			
			if(gateOrientation == JLSInfo.Orientation.LEFT)
			{
				gateOrientation = JLSInfo.Orientation.DOWN;
			}
			else if(gateOrientation == JLSInfo.Orientation.DOWN)
			{
				gateOrientation = JLSInfo.Orientation.RIGHT;
			}
			else if(gateOrientation == JLSInfo.Orientation.RIGHT)
			{
				gateOrientation = JLSInfo.Orientation.UP;
			}
			else if(gateOrientation == JLSInfo.Orientation.UP)
			{
				gateOrientation = JLSInfo.Orientation.LEFT;
			}
			
		}
		else if(direction == JLSInfo.Orientation.RIGHT)
		{
			if(controlOrientation == JLSInfo.Orientation.LEFT)
			{
				controlOrientation = JLSInfo.Orientation.UP;
			}
			else if(controlOrientation == JLSInfo.Orientation.DOWN)
			{
				controlOrientation = JLSInfo.Orientation.LEFT;
			}
			else if(controlOrientation == JLSInfo.Orientation.RIGHT)
			{
				controlOrientation = JLSInfo.Orientation.DOWN;
			}
			else if(controlOrientation == JLSInfo.Orientation.UP)
			{
				controlOrientation = JLSInfo.Orientation.RIGHT;
			}
			
			if(gateOrientation == JLSInfo.Orientation.LEFT)
			{
				gateOrientation = JLSInfo.Orientation.UP;
			}
			else if(gateOrientation == JLSInfo.Orientation.DOWN)
			{
				gateOrientation = JLSInfo.Orientation.LEFT;
			}
			else if(gateOrientation == JLSInfo.Orientation.RIGHT)
			{
				gateOrientation = JLSInfo.Orientation.DOWN;
			}
			else if(gateOrientation == JLSInfo.Orientation.UP)
			{
				gateOrientation = JLSInfo.Orientation.RIGHT;
			}
		}
		inputs.clear();
		outputs.clear();
		width = 0;
		height = 0;
		init(g);
	}

	/**
	 * Dialog box to set bits.
	 */
	private class TriStateCreate extends JDialog implements ActionListener {
		
		// properties
		private JButton ok = new JButton("OK");
		private JButton cancel = new JButton("Cancel");
		private JTextField bitsField = new JTextField(defaultBits+"",10);
		private KeyPad bitsPad = new KeyPad(bitsField,10,defaultBits,this);
		private JRadioButton oLeft = new JRadioButton("Left");
		private JRadioButton oRight = new JRadioButton("Right", true);
		private JRadioButton oUp = new JRadioButton("Up");
		private JRadioButton oDown = new JRadioButton("Down");
		private JRadioButton sLeft = new JRadioButton("Left");
		private JRadioButton sRight = new JRadioButton("Right");
		private JRadioButton sUp = new JRadioButton("Up");
		private JRadioButton sDown = new JRadioButton("Down",true);
		private JLabel olbl2 = new JLabel("Control Orientation");
		
		/**
		 * Set up create dialog window.
		 * 
		 * @param x The x-coordinate of the position of the dialog.
		 * @param y The y-coordinate of the position of the dialog.
		 */
		private TriStateCreate(int x, int y) {
			
			// set up window title
			super(JLSInfo.frame,"Create TriState",true);
			
			// set not cancelled
			cancelled = false;
			
			// set up window
			Container window = getContentPane();
			window.setLayout(new BoxLayout(window,BoxLayout.Y_AXIS));
			
			// set up inputs
			JPanel info = new JPanel(new BorderLayout());
			JLabel bits = new JLabel("Gates (bits): ",SwingConstants.RIGHT);
			info.add(bits,BorderLayout.WEST);
			info.add(bitsField,BorderLayout.CENTER);
			info.add(bitsPad,BorderLayout.EAST);
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
				JLSInfo.hb.enableHelpOnButton(help,"TRISTATE",null);
			okCancel.add(help);
			window.add(okCancel);
			
			ok.addActionListener(this);
			bitsField.addActionListener(this);
			cancel.addActionListener(this);
			oLeft.addActionListener(this);
			oRight.addActionListener(this);
			oUp.addActionListener(this);
			oDown.addActionListener(this);
			
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
				if(this.oLeft.isSelected())
				{
					gateOrientation = JLSInfo.Orientation.LEFT;
					if(this.sUp.isSelected())
					{
						controlOrientation = JLSInfo.Orientation.UP;
					}
					else if(this.sDown.isSelected())
					{
						controlOrientation = JLSInfo.Orientation.DOWN;
					}
				}
				else if(this.oRight.isSelected())
				{
					gateOrientation = JLSInfo.Orientation.RIGHT;
					if(this.sUp.isSelected())
					{
						controlOrientation = JLSInfo.Orientation.UP;
					}
					else if(this.sDown.isSelected())
					{
						controlOrientation = JLSInfo.Orientation.DOWN;
					}
				}
				else if(this.oDown.isSelected())
				{
					gateOrientation = JLSInfo.Orientation.DOWN;
					if(this.sLeft.isSelected())
					{
						controlOrientation = JLSInfo.Orientation.LEFT;
					}
					else if(this.sRight.isSelected())
					{
						controlOrientation = JLSInfo.Orientation.RIGHT;
					}
				}
				else if(this.oUp.isSelected())
				{
					gateOrientation = JLSInfo.Orientation.UP;
					if(this.sLeft.isSelected())
					{
						controlOrientation = JLSInfo.Orientation.LEFT;
					}
					else if(this.sRight.isSelected())
					{
						controlOrientation = JLSInfo.Orientation.RIGHT;
					}
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
		
	} // end of TriStateCreate class
	
//	-------------------------------------------------------------------------------
//	Simulation
//	-------------------------------------------------------------------------------
	
	/**
	 * Initialize this element by setting its output pin to off (null).
	 * 
	 * @param sim Unused.
	 */
	public void initSim(Simulator sim) {
		
		// set output pin
		Output out = outputs.get(0);
		out.setValue(null);
		
	} // end of initSim method
	
	/**
	 * React to an event.
	 * 
	 * @param now The current simulation time.
	 * @param sim The simulator to post events to.
	 * @param todo If null, an input has changed, otherwise it is the value to output.
	 */
	public void react(long now, Simulator sim, Object todo) {
		
		// if the input has changed ...
		if (todo == null) {
			
			// get control input
			BitSet control = inputs.get(1).getValue();
			if (control ==  null)
				control = new BitSet();
			
			// if it is zero, turn off output
			if (!control.get(0)) {
				sim.post(new SimEvent(now+propDelay,this,"off"));
			}
			else {

				// get the data input and send it to the output
				BitSet value = inputs.get(0).getValue();
				if (value == null)
					value = new BitSet();
				else
					value = (BitSet)value.clone();
				sim.post(new SimEvent(now+propDelay,this,value));
			}
			
		}
		
		// if gate is turning off, propagate null
		else if (todo instanceof String) {
			
			Output out = outputs.get(0);
			out.propagate(null,now,sim);
		}
		else {
			
			// get the new output value
			BitSet newValue = (BitSet)todo;
		
			// propagate value
			Output out = outputs.get(0);
			out.propagate(newValue,now,sim);
		}
		
	} // end of react method
	
} // end of TriState method
