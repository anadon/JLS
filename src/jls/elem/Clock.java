package jls.elem;

import jls.*;
import jls.sim.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.BitSet;
import javax.swing.*;

/**
 * Clock element.
 * 
 * @author David A. Poplawski
 */
public class Clock extends LogicElement {
	
	// default values
	private static int defaultCycleTime = 2;
	private static int defaultOneTime = defaultCycleTime/2;
	
	// properties
	private int cycleTime = defaultCycleTime;
	private int oneTime = defaultOneTime;
	private JLSInfo.Orientation orientation = JLSInfo.Orientation.RIGHT;
	private boolean cancelled;
	
	/**
	 * Create a new clock element.
	 * 
	 * @param circuit The circuit this element is part of.
	 */
	public Clock(Circuit circuit) {
		
		super(circuit);
	} // end of constructor
	/**
	 * Display dialog to get value and bits.
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
			new ClockCreate(x+win.x,y+win.y);
		}
		else {
			new ClockCreate(pos.x+win.x,pos.y+win.y);
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
	 * @param g Unused.
	 */
	public void init(Graphics g) {
		
		int s = JLSInfo.spacing;
		width = 2*s;
		height = 2*s;
		if(orientation == JLSInfo.Orientation.RIGHT)
		{
			outputs.add(new Output("output",this,width,height/2,1));
		}
		else if(orientation == JLSInfo.Orientation.LEFT)
		{
			outputs.add(new Output("output",this,0,height/2,1));
		}
		else if(orientation == JLSInfo.Orientation.UP)
		{
			outputs.add(new Output("output",this,width/2,0,1));
		}
		else if(orientation == JLSInfo.Orientation.DOWN)
		{
			outputs.add(new Output("output",this,s,height,1));
		}

	} // end of init method
	
	/**
	 * Draw this element.
	 * 
	 * @param g The graphics object to draw with.
	 */
	public void draw(Graphics g) {
		
		// draw context
		super.draw(g);
		
		// draw rounded rectangle
		int s = JLSInfo.spacing;
		int s2 = s/2;
		int s4 = s/4;
		int d = s*2;
		g.setColor(Color.BLACK);
		g.drawRoundRect(x,y,d,d,s2,s2);
		
		// draw waveform
		
		int bottom = y+s+s2;
		int top = y+s2;
		int left = x+s4;
		g.drawLine(left,bottom,left+s4,bottom);
		left += s4;
		g.drawLine(left,bottom,left,top);
		g.drawLine(left,top,left+s2,top);
		left += s2;
		g.drawLine(left,top,left,bottom);
		g.drawLine(left,bottom,left+s2,bottom);
		left += s2;
		g.drawLine(left,bottom,left,top);
		g.drawLine(left,top,left+s4,top);
		
		// draw output
		outputs.get(0).draw(g);
	} // end of draw method
	
	/**
	 * Save thiselement.
	 * 
	 * @param output The output writer.
	 */
	public void save(PrintWriter output) {
		
		output.println("ELEMENT Clock");
		super.save(output);
		output.println(" int cycle " + cycleTime);
		output.println(" int one " + oneTime);
		output.println(" String orient \"" + orientation.toString() + "\"");
		output.println("END");
	} // end of save method
	
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
	 * Set an int instance variable value (during a load).
	 * 
	 * @param name The instance variable name.
	 * @param value The instance variable value.
	 */
	public void setValue(String name, int value) {
		
		if (name.equals("cycle")) {
			cycleTime = value;
		} else if (name.equals("one")) {
			oneTime = value;
		} else {
			super.setValue(name,value);
		}
	} // end of setValue method
	
	/**
	 * Copy this element.
	 */
	public Element copy() {
		
		Clock it = new Clock(circuit);
		it.cycleTime = cycleTime;
		it.oneTime = oneTime;
		it.orientation = orientation;
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
		
		info.setText("clock, cycle time = " + cycleTime +
				" (zero for " + (cycleTime-oneTime) + ", one for " + oneTime + ")");
	} // end of showInfo method

	/**
	 * Clock values can be changed.
	 * 
	 * @return true.
	 */
	public boolean canChange() {
		
		return true;
	} // end of canChange method
	
	/**
	 * Tells if a clock is capable of rotatating, can only rotate when output has no attachment.
	 * @return False if output has a wire attached, True otherwise
	 */
	public boolean canRotate()
	{
		return !outputs.get(0).isAttached();
	}
	
	/**
	 *  This method will rotate the clock if it is rotateable.
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
		outputs.remove(0);
		init(g);
	}
	
	/**
	 * Tells if a clock is capable of flipping, can only flip when output has no attachment.
	 * @return False if output has a wire attached, True otherwise
	 */
	public boolean canFlip()
	{
		return !outputs.get(0).isAttached();
	}
	
	/**
	 * This method will flip a clock
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
		width = 0;
		height = 0;
		init(g);
	}
	
	/**
	 * Show change dialog.
	 * 
	 * @param g The Graphics object to use to determine size.
	 * @param editWindow The editor window this element is in.
	 * @param x The current x-coordinate of the mouse.
	 * @param y The current y-coordinate of the mouse.
	 * 
	 * @return false.
	 */
	public boolean change(Graphics g, JPanel editWindow, int x, int y) {
		
		// display dialog
		Point pos = editWindow.getMousePosition();
		Point win = editWindow.getLocationOnScreen();
		if (pos == null) {
			new ClockCreate(x+win.x,y+win.y);
		}
		else {
			new ClockCreate(pos.x+win.x,pos.y+win.y);
		}
		
		if (!cancelled)
			circuit.markChanged();
		return false;
	
	} // end of change method
	
	/**
	 * Dialog box to set multi-input gate parameters (number of inputs, number of gates).
	 * Used by all simple gates (nand, and, nor, or, xor, not).
	 */
	private class ClockCreate extends JDialog implements ActionListener {
		
		// properties
		private JButton ok = new JButton("OK");
		private JButton cancel = new JButton("Cancel");
		private JTextField cycleTimeField = new JTextField(cycleTime+"",10);
		private JTextField oneTimeField = new JTextField(oneTime+"",10);
		private KeyPad cycleTimePad = new KeyPad(cycleTimeField,10,defaultCycleTime,this);
		private KeyPad oneTimePad = new KeyPad(oneTimeField,10,defaultOneTime,this);
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
		private ClockCreate(int x, int y) {
			
			// set up window title
			super(JLSInfo.frame,"Create Clock",true);
			
			// set not cancelled
			cancelled = false;
			
			// set up window
			Container window = getContentPane();
			window.setLayout(new BoxLayout(window,BoxLayout.Y_AXIS));
			
			// set up inputs
			JPanel info = new JPanel(new BorderLayout());
			
			JPanel labels = new JPanel(new GridLayout(3,1,1,5));
			JLabel ctime = new JLabel("Cycle Time: ",SwingConstants.RIGHT);
			labels.add(ctime);
			JLabel otime = new JLabel("One Time: ",SwingConstants.RIGHT);
			labels.add(otime);
			info.add(labels,BorderLayout.WEST);
			
			JPanel fields = new JPanel(new GridLayout(2,1,1,5));
			JPanel ct = new JPanel(new BorderLayout());
			ct.add(cycleTimeField,BorderLayout.CENTER);
			ct.add(cycleTimePad,BorderLayout.EAST);
			fields.add(ct);
			JPanel ot = new JPanel(new BorderLayout());
			ot.add(oneTimeField,BorderLayout.CENTER);
			ot.add(oneTimePad,BorderLayout.EAST);
			fields.add(ot);
			info.add(fields,BorderLayout.CENTER);
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
			JPanel okCancel = new JPanel(new GridLayout(1,2));
			ok.setBackground(Color.green);
			okCancel.add(ok);
			cancel.setBackground(Color.pink);
			okCancel.add(cancel);
			JButton help = new JButton("Help");
			if (JLSInfo.hb == null)
				Util.noHelp(help);
			else
				JLSInfo.hb.enableHelpOnButton(help,"clock",null);
			okCancel.add(help);
			window.add(okCancel);
			getRootPane().setDefaultButton(ok);
			
			ok.addActionListener(this);
			cycleTimeField.addActionListener(this);
			oneTimeField.addActionListener(this);
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
			
			if (event.getSource() == ok || event.getSource() == cycleTimeField || event.getSource() == oneTimeField) {
				try {
					int newCycleTime = Integer.parseInt(cycleTimeField.getText());
					int newOneTime = Integer.parseInt(oneTimeField.getText());
					
					cycleTime = newCycleTime;
					oneTime = newOneTime;
				}
				catch (NumberFormatException ex) {
					JOptionPane.showMessageDialog(this,
							"Value not numeric, try again", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (oneTime >= cycleTime) {
					JOptionPane.showMessageDialog(this,
							"One time must be less than cycle time", "Error",
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
		
	} // end of ClockCreate class
	
//	-------------------------------------------------------------------------------
//	Simulation
//	-------------------------------------------------------------------------------
	
	/**
	 * Initialize this element by setting its output pin and to-be value to 0.
	 * 
	 * @param sim Unused.
	 */
	public void initSim(Simulator sim) {
		
		// set output pin
		Output out = outputs.get(0);
		BitSet zero = new BitSet(1);
		out.setValue(zero);
		BitSet one = new BitSet();
		one.flip(0);
		sim.post(new SimEvent(cycleTime-oneTime,this,one));
		
	} // end of initSim method
	
	/**
	 * React to an event.
	 * 
	 * @param now The current simulation time.
	 * @param sim The simulator to post events to.
	 * @param todo If null, an input has changed, otherwise it is the value to output.
	 */
	public void react(long now, Simulator sim, Object todo) {
		
		// send new value
		BitSet send = (BitSet)todo;
		Output out = outputs.get(0);
		out.propagate(send,now,sim);
		
		// post next event
		BitSet next = (BitSet)send.clone();
		next.flip(0);
		int when = oneTime;
		if (send.cardinality() == 0) {
			when = cycleTime - oneTime;
		}
		sim.post(new SimEvent(now+when,this,next));
		
	} // end of react method
	
} // end of Clock class
