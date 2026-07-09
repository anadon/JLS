package jls.elem;

import jls.*;
import jls.sim.*;
import jls.util.Placement;

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
	
	// identity (#22); previous-settings unused: DELAY has its own dialog
	private static final Kind KIND =
			new Kind("DELAY","DelayGate",1,0);
	
	/**
	 * The kind descriptor for this gate.
	 */
	protected Kind kind() {
		
		return KIND;
	} // end of kind method
	
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
		new DelayCreate();
		
		// don't do anything if user canceled gate
		if (cancelled)
			return false;

		// set up inputs and outputs
		numInputs = 1;
		init(g);
		
		// save position
		Point p = Placement.dropPoint(editWindow,x,y,width,height);
		super.setXY(p.x,p.y);
		
		return true;
		
	} // end of setup method
	
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
	@SuppressWarnings("serial")
	private class DelayCreate extends ElementDialog {

			// properties
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
			 */
			public DelayCreate() {

				// set up window title
				super("Create DELAY Gate","DELAY");

				// set not cancelled
				cancelled = false;

				// set up window
				Container window = getContentPane();

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

				confirmOnEnter(delayField);
				confirmOnEnter(gatesField);
				finishDialog();
			} // end of constructor

			/**
			 * Validate the form and create the delay gate.
			 */
			protected void validateAndAccept() {

				try {
					propDelay = Integer.parseInt(delayField.getText());
					bits = Integer.parseInt(gatesField.getText());
				}
				catch (NumberFormatException ex) {
					reject("Value not numeric, try again");
					propDelay = 0;
					return;
				}
				if (bits < 1) {
					reject("Must have at least 1 gate");
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
			} // end of validateAndAccept method

			/**
			 * Cancel this gate.
			 */
			protected void cancelDialog() {

				cancelled = true;
				delayPad.close();
				gatesPad.close();
				dispose();
			} // end of cancelDialog method

		} // end of DelayCreate class

//-------------------------------------------------------------------------------
// Simulation
//-------------------------------------------------------------------------------

	/**
	 * A delay gate outputs its input unchanged (after the propagation delay).
	 */
	protected BitSet computeOutput() {

		BitSet value = inputs.get(0).getValue();
		if (value == null)
			return new BitSet();
		return (BitSet)value.clone();
	} // end of computeOutput method


} // end of DelayGate class
