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
 * Delay gate.
 * Logically neutral, simply delays a signal change by a given amount.
 * 
 * @author David A. Poplawski
 */
public class DelayGate extends Gate {
	
	// default values
	private static final int defaultPropDelay = 1;
	
	/**
	 * Create DELAY gate.
	 * 
	 * @param circuit The circuit this DELAY gate is in.
	 */
	public DelayGate(Circuit circuit) {
		
		super(circuit);
		
		// create image for draw
		int s = JLSInfo.spacing;
		Line2D top = new Line2D.Double(0,0,2*s,s);
		Line2D bottom = new Line2D.Double(2*s,s,0,2*s);
		Line2D side = new Line2D.Double(0,2*s,0,0);
		gateShape = new GeneralPath(top);
		gateShape.append(bottom,true);
		gateShape.append(side,true);
		gateShape.closePath();
	} // end of constructor
	
	/**
	 * Initialize this element in a GUI context.
	 * 
	 * @param g The graphics object to use.
	 * @param editWindow The editor window this circuit is displayed in.
	 * @param x The x-coordinate of the last known mouse position.
	 * @param y The y-coordinate of the last known mouse position.
	 * 
	 * @return false if canceled, true otherwise.
	 */
	public boolean setup(Graphics g, JPanel editWindow, int x, int y) {

		// show creation dialog
		Point pos = editWindow.getMousePosition();
		Point win = editWindow.getLocationOnScreen();
		if (pos == null) {
			new DelayCreate(x+win.x,y+win.y);
		}
		else {
			new DelayCreate(pos.x+win.x,pos.y+win.y);
		}
		
		// don't do anything if user canceled gate
		if (cancelled)
			return false;

		// set up inputs and outputs
		numInputs = 1;
		init(g);
		
		// save position
		Point p = editWindow.getMousePosition();
		if (p != null)
			super.setXY(p.x-width/2,p.y-height/2);
		
		return true;
		
	} // end of setup method
	
	/**
	 * Copy this element.
	 * 
	 * @return A copy of this DELAY gate.
	 */
	public Element copy() {
		
		DelayGate it = new DelayGate(circuit);
		super.copy(it);
		return it;
	} // end of copy method
	
	/**
	 * Save this element in a file.
	 * 
	 * @param output The output writer.
	 */
	public void save(PrintWriter output) {
		
		super.save(output,"DelayGate",false);
	} // end of save method
	
	/**
	 * Display info about this DELAY gate.
	 * 
	 * @param info The JLabel to display with.
	 */
	public void showInfo(JLabel info) {
		
		String pd = ", delay = " + propDelay;
		if (bits == 1)
			info.setText("DELAY gate" + pd);
		else
			info.setText(bits + " DELAY gates" + pd);
	} // end of showInfo method
	
	/**
	 * Cannot reset propagation delay.
	 */
	public void resetPropDelay() {
		
		// do nothing
	} // end of resetPropDelay method

	/**
	 * Dialog box to set delay gate parameters.
	 */
	private class DelayCreate extends JDialog implements ActionListener {
			
			// properties
			private JButton ok = new JButton("OK");
			private JButton cancel = new JButton("Cancel");
			private JTextField delayField = new JTextField(defaultPropDelay+"",5);
			private JTextField gatesField = new JTextField(defaultBits+"",5);
			private KeyPad delayPad = new KeyPad(delayField,10,defaultPropDelay,this);
			private KeyPad gatesPad = new KeyPad(gatesField,10,defaultBits,this);
			private JRadioButton left = new JRadioButton("left");
			private JRadioButton up = new JRadioButton("up");
			private JRadioButton down = new JRadioButton("down");
			private JRadioButton right = new JRadioButton("right");

			/**
			 * Set up dialog window.
			 * 
			 * @param x The x-coordinate of the position of the dialog.
			 * @param y The y-coordinate of the position of the dialog.
			 */
			public DelayCreate(int x, int y) {

				// set up window title
				super(JLSInfo.frame,"Create DELAY Gate",true);
				
				// set not cancelled
				cancelled = false;
				
				// set up window
				Container window = getContentPane();
				window.setLayout(new BoxLayout(window,BoxLayout.Y_AXIS));
				
				// set up input panel
				JPanel info = new JPanel(new GridLayout(2,2));
				JLabel inputs = new JLabel("Propagation Delay: ",SwingConstants.RIGHT);
				info.add(inputs);
				JPanel in = new JPanel(new FlowLayout());
				in.add(delayField);
				in.add(delayPad);
				info.add(in);
				JLabel gates = new JLabel("Gates (bits): ",SwingConstants.RIGHT);
				info.add(gates);
				JPanel ga = new JPanel(new FlowLayout());
				ga.add(gatesField);
				ga.add(gatesPad);
				info.add(ga);
				window.add(info);
				
				// set up orientation panel
				window.add(new JLabel(" "));
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
				ButtonGroup group = new ButtonGroup();
				group.add(left);
				group.add(right);
				group.add(up);
				group.add(down);
				right.setSelected(true);
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
					JLSInfo.hb.enableHelpOnButton(help,"DELAY",null);
				okCancel.add(help);
				window.add(okCancel);
				getRootPane().setDefaultButton(ok);
				
				ok.addActionListener(this);
				cancel.addActionListener(this);
				delayField.addActionListener(this);
				gatesField.addActionListener(this);
				
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
				
				// if ok button, check values for validity
				if (event.getSource() == ok || event.getSource() == delayField || event.getSource() == gatesField) {
					try {
						propDelay = Integer.parseInt(delayField.getText());
						bits = Integer.parseInt(gatesField.getText());
					}
					catch (NumberFormatException ex) {
						JOptionPane.showMessageDialog(this,
								 "Value not numeric, try again", "Error",
								 JOptionPane.ERROR_MESSAGE);
						propDelay = 0;
						return;
					}
					if (bits < 1) {
						JOptionPane.showMessageDialog(this,
								 "Must have at least 1 gate", "Error",
								 JOptionPane.ERROR_MESSAGE);
						propDelay = 0;
						return;
					}
					if (left.isSelected()) {
						orientation = Orientation.left;
					}
					else if (right.isSelected()) {
						orientation = Orientation.right;
					}
					else if (up.isSelected()) {
						orientation = Orientation.up;
					}
					else {
						orientation = Orientation.down;
					}
					delayPad.close();
					gatesPad.close();
					dispose();
				}
				
				// if cancel button, cancel gate creation
				else if (event.getSource() == cancel) {
					cancel();
				}
			} // end of actionPerformed method
			
			/**
			 * Cancel this gate.
			 */
			private void cancel() {
				
				cancelled = true;
				delayPad.close();
				gatesPad.close();
				dispose();
			} // end of cancel method
			
		} // end of DelayCreate class

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
		
		// set output pin
		Output out = (Output)(outputs.toArray()[0]);
		BitSet bitval = new BitSet(1);
		out.setValue(bitval);
		
		// set to-be value
		toBeValue = new BitSet(1);
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
			
			// get the input bits
			BitSet value = ((Input)(inputs.toArray()[0])).getValue();
			if (value == null) {
				value = new BitSet();
			}
			else {
				value = (BitSet)value.clone();
			}
			
			// if new value is different from the value propagating through
			// this gate, then post an event
			if (!value.equals(toBeValue)) {
				toBeValue = (BitSet)value.clone();
				sim.post(new SimEvent(now+propDelay,this,value));
			}
		}
		else {
			
			// get the new output value
			BitSet newValue = (BitSet)todo;
			
			// send to output
			Output out = (Output)(outputs.toArray()[0]);
			out.propagate(newValue,now,sim);
		}
		
	} // end of react method
	
} // end of DelayGate class
