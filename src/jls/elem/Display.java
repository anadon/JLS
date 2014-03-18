package jls.elem;

import java.awt.*;
import java.awt.event.*;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.BitSet;
import java.util.Vector;

import javax.swing.*;
import jls.*;
import jls.elem.Gate.Orientation;
import jls.sim.*;

/**
 * Display an input value on the circuit editor screen.
 * 
 * @author David A. Poplawski
 */
public class Display extends LogicElement {
	
	// constants
	private final int defaultBits = 1;
	
	// properties
	private int bits = defaultBits;
	private int base = 10;
	
	// running properties
	private boolean cancelled = false;
	
	// legacy
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
	public boolean setup(Graphics g, JPanel editWindow, int x, int y) {
		
		// show creation dialog
		Point pos = editWindow.getMousePosition();
		Point win = editWindow.getLocationOnScreen();
		if (pos == null) {
			new DispCreate(x+win.x,y+win.y);
		}
		else {
			new DispCreate(pos.x+win.x,pos.y+win.y);
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
	 * Initialize this element.
	 * 
	 * @param g Unused.
	 */
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
	public void save(PrintWriter output) {
		
		output.println("ELEMENT Display");
		super.save(output);
		output.println(" int bits " + bits);
		output.println(" int base " + base);
		if(orient > 0) output.println(" int orient " + orient);
		output.println("END");
	} // end of save method

	/**
	 * Set an int instance variable value (during a load).
	 * 
	 * @param name The instance variable name.
	 * @param value The instance variable value.
	 */
	public void setValue(String name, int value) {
		
		if (name.equals("bits")) {
			bits = value;
		} else if (name.equals("base")) {
			base = value;
		} else if (name.equals("orient")) {
			orient = value;
		} else {
			super.setValue(name,value);
		}
	} // end of setValue method
	
	/**
	 * Copy this element.
	 * 
	 * @return a copy of this element.
	 */
	public Element copy() {
		
		Display it = new Display(circuit);
		it.bits = bits;
		it.base = base;
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
	public void showInfo(JLabel info) {
		
		info.setText(bits + " bit display, value = " + BitSetUtils.toDisplay(currentValue,bits));
	} // end of showInfo method
	
	/**
	 *  This method will rotate the display if it is rotateable.
	 * @param direction The direction to rotate
	 * @param g The current graphics context for use in recalculating size
	 */
	public void rotate(JLSInfo.Orientation direction, Graphics g)
	{
		// No-op
	}
	
	/**
	 * Tells if a display is capable of rotatating, can only rotate when input has no attachment.
	 * @return False if input has a wire attached, True otherwise
	 */
	public boolean canRotate()
	{
		// Displays cannot be rotated ever since they implement the Stop behavior for inputs.
		return false;
	}
	
	/**
	 * Tells if a display is capable of flippinging, can only flip when input has no attachment.
	 * @return False if input has a wire attached, True otherwise
	 */
	public boolean canFlip()
	{
		// Displays cannot be flipped ever since they implement the Stop behavior for inputs.
		return false;
	}
	
	/**
	 * This method will flip a display
	 * @param g The current graphics context to facilitate recalculation of size when flipping
	 */
	public void flip(Graphics g)
	{
		// No-op
	}


	/**
	 * Dialog box to set inputs and bits.
	 */
	protected class DispCreate extends JDialog implements ActionListener {
		
		// properties
		private JButton ok = new JButton("OK");
		private JButton cancel = new JButton("Cancel");
		private JTextField bitsField = new JTextField(defaultBits+"",5);
		private KeyPad bitsPad = new KeyPad(bitsField,10,defaultBits,this);
		private JRadioButton b2 = new JRadioButton("2");
		private JRadioButton b10 = new JRadioButton("10");
		private JRadioButton b16 = new JRadioButton("16");
		
		/**
		 * Set up dialog window.
		 * 
		 * @param x The x-coordinate of the position of the dialog.
		 * @param y The y-coordinate of the position of the dialog.
		 */
		protected DispCreate(int x, int y) {
			
			// set up window title
			super(JLSInfo.frame,"Create Display",true);
			
			// set not cancelled
			cancelled = false;
			
			// set up window
			Container window = getContentPane();
			window.setLayout(new BoxLayout(window,BoxLayout.Y_AXIS));
			
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
				JLSInfo.hb.enableHelpOnButton(help,"display",null);
			okCancel.add(help);
			window.add(okCancel);
			getRootPane().setDefaultButton(ok);
			
			ok.addActionListener(this);
			bitsField.addActionListener(this);
			cancel.addActionListener(this);
			
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
			
			// if ok button, check values for validity
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
							"Must be at least one bit", "Error",
							JOptionPane.ERROR_MESSAGE);
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
			bitsPad.close();
			dispose();
		} // end of cancel method
		
	} // end of DispCreate class

//	-------------------------------------------------------------------------------
//	Simulation
//	-------------------------------------------------------------------------------
	
	private BitSet currentValue = new BitSet();
	
	/**
	 * Set the current display value to 0.
	 * 
	 * @param sim Unused.
	 */
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
	public void react(long now, Simulator sim, Object todo) {
		
		currentValue = inputs.get(0).getValue();
	} // end of react method

} // end of Display class
