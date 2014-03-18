package jls.elem;

import jls.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import javax.swing.*;

/**
 * Superclass of all simple gates (AND, OR, etc).
 * Contains common info and method.
 * 
 * @author David A. Poplawski
 */
public abstract class Gate extends LogicElement {
	
	// named constants
	protected enum Orientation {up, down, left, right};
	protected static final int defaultInputs = 2;
	protected static final int defaultBits = 1;
	protected static final Orientation defaultOrientation = Orientation.right;
	
	// saved properties
	protected int numInputs = defaultInputs;
	protected int bits = defaultBits;
	protected Orientation orientation = defaultOrientation;
	protected int propDelay;
	
	// running properties
	protected GeneralPath gateShape;
	protected boolean cancelled;
	
	/**
	 * Create a new Gate object.
	 * Subclass constructors do most of the work.
	 * 
	 * @param circuit
	 */
	public Gate(Circuit circuit) {
		
		super(circuit);
	} // end of constructor
	
	/**
	 * Initialize this element in a GUI context.
	 * 
	 * @param g The graphics object to use.
	 * @param editWindow The editor window this circuit is displayed in.
	 * @param x The x-coordinate of the last known mouse position.
	 * @param y The y-coordinate of the last known mouse position.
	 * @param type The type of gate (e.g., "AND").
	 * 
	 * @return false if canceled, true otherwise.
	 */
	public boolean setup(Graphics g, JPanel editWindow, int x, int y, String type) {
		
		// show creation dialog
		Point pos = editWindow.getMousePosition();
		Point win = editWindow.getLocationOnScreen();
		GateCreate gate = null;
		if (pos == null) {
			gate = new GateCreate(x+win.x,y+win.y,type);
		}
		else {
			gate = new GateCreate(pos.x+win.x,pos.y+win.y,type);
		}
		
		// don't do anything if user canceled gate
		if (cancelled)
			return false;
		
		// get info
		if (type.equals("NOT") || type.equals("DELAY") || type.equals("Extend")) {
			numInputs = 1;
		}
		else {
			numInputs = gate.getInputs();
		}
		bits = gate.getGates();
		orientation = gate.getOrientation();
		propDelay = getDefaultDelay();
		
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
	 * Sets up size, inputs and outputs.
	 * 
	 * @param g Unused.
	 */
	public void init(Graphics g) {
		
		// set up size
		if (orientation == Orientation.left || orientation == Orientation.right) {
			width = JLSInfo.spacing*4;
			height = JLSInfo.spacing*(Math.max(numInputs,3)-1);
		}
		else { // up or down
			width = JLSInfo.spacing*(Math.max(numInputs,3)-1);
			height = JLSInfo.spacing*4;
		}
		
		Output out;
		
		if (orientation == Orientation.left || orientation == Orientation.right) {
			
			int inx = 0;
			int outx = JLSInfo.spacing*4;
			if (orientation == Orientation.left) {
				inx = JLSInfo.spacing*4;
				outx = 0;
			}
			
			// set up output
			int dist = (Math.max(numInputs,4)-3)/2*JLSInfo.spacing;
			out = new Output("output",this,outx,dist+JLSInfo.spacing,bits);
			outputs.add(out);
			
			// set up inputs
			if (numInputs == 1) { // not or delay gate
				inputs.add(new Input("input0",this,inx,JLSInfo.spacing,bits));
			}
			else {
				int yc = 0;
				for (int i=0; i<numInputs; i+=1) {
					inputs.add(new Input("input"+i,this,inx,yc,bits));
					if (numInputs == 2)
						yc += 2*JLSInfo.spacing;
					else
						yc += JLSInfo.spacing;
				}
			}
			
		}
		else { // up or down
			int iny = 0;
			int outy = JLSInfo.spacing*4;
			if (orientation == Orientation.up) {
				iny = JLSInfo.spacing*4;
				outy = 0;
			}
			
			// set up output
			int dist = (Math.max(numInputs,4)-3)/2*JLSInfo.spacing;
			out = new Output("output",this,dist+JLSInfo.spacing,outy,bits);
			outputs.add(out);
			
			// set up inputs
			if (numInputs == 1) { // not or delay gate
				inputs.add(new Input("input0",this,JLSInfo.spacing,iny,bits));
			}
			else {
				int xc = 0;
				for (int i=0; i<numInputs; i+=1) {
					inputs.add(new Input("input"+i,this,xc,iny,bits));
					if (numInputs == 2)
						xc += 2*JLSInfo.spacing;
					else
						xc += JLSInfo.spacing;
				}
			}
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
		
		// draw the gate
		int s = JLSInfo.spacing;
		int dist = (Math.max(numInputs,4)-3)/2*s;
		int ox = 0;
		int oy = 0;
		AffineTransform trans = new AffineTransform();
		if (orientation == Orientation.right) {
			trans.translate(x+s,y+dist);
			ox = -s;
		}
		else if (orientation == Orientation.left) {
			trans.translate(x+s,y+dist);
			trans.rotate(Math.toRadians(180),s,s);
			ox = s;
		}
		else if (orientation == Orientation.up) {
			trans.translate(x+dist,y+s);
			trans.rotate(Math.toRadians(-90),s,s);
			oy = s;
		}
		else if (orientation == Orientation.down) {
			trans.translate(x+dist,y+s);
			trans.rotate(Math.toRadians(90),s,s);
			oy = -s;
		}
		GeneralPath temp = (GeneralPath)(gateShape.clone());
		temp.transform(trans);
		g.setColor(Color.black);
		Graphics2D gg = (Graphics2D)g;
		gg.draw(temp);
		
		// draw input/output points and line to them
		Output out = outputs.get(0);
		double inc = 2.0*s/(2*numInputs);
		if (orientation == Orientation.left || orientation == Orientation.right) {
			
			// output
			int lx = out.getX();
			g.drawLine(lx,y+dist+s,lx+ox,y+dist+s);
			out.draw(g);
			
			// inputs
			double ye = y + dist + inc;
			for (int p=0; p<numInputs; p+=1) {
				lx = inputs.get(p).getX();
				int ly = inputs.get(p).getY();
				g.setColor(Color.black);
				g.drawLine(lx,ly,lx-ox,(int)(ye+0.5));
				ye += inc*2;
				inputs.get(p).draw(g);
			}
		}
		else { // up or down
			
			// output
			int ly = out.getY();
			g.drawLine(x+dist+s,ly,x+dist+s,ly+oy);
			out.draw(g);
			
			// inputs
			double xe = x + dist + inc;
			for (int p=0; p<numInputs; p+=1) {;
			ly = inputs.get(p).getY();
			int lx = inputs.get(p).getX();
			g.setColor(Color.black);
			g.drawLine(lx,ly,(int)(xe+0.5),ly-oy);
			xe += inc*2;
			inputs.get(p).draw(g);
			}
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
		} else if (name.equals("numInputs")) {
			numInputs = value;
		} else if (name.equals("delay")) {
			propDelay = value;
		} else {
			super.setValue(name,value);
		}
	} // end of setValue method
	
	/**
	 * Set a String instance variable value (during a load);
	 * 
	 * @param name The instance variable name.
	 * @param value The instance variable value.
	 */
	public void setValue(String name, String value) {
		
		if (name.equals("orientation")) {
			if (value.equals("left")) {
				orientation = Orientation.left;
			}
			else if (value.equals("up")) {
				orientation = Orientation.up;
			}
			else if (value.equals("down")) {
				orientation = Orientation.down;
			}
			else if (value.equals("right")) {
				orientation = Orientation.right;
			}
		} else {
			super.setValue(name,value);
		}
	} // end of setValue method
	
	/**
	 * Copy this gate.
	 * 
	 * @return A copy of this gate.
	 */
	public abstract Element copy();
	
	/**
	 * Copy info in this element to another element.
	 * 
	 * @param it The element to copy to.
	 */
	public void copy(Gate it) {
		
		it.numInputs = numInputs;
		it.bits = bits;
		it.orientation = orientation;
		it.propDelay = propDelay;
		it.outputs.add(outputs.get(0).copy(it));
		for (Input in : inputs) {
			it.inputs.add(in.copy(it));
		}
		super.copy(it);
		return;
	} // end of copy method
	
	/**
	 * Save this element in a file.
	 * 
	 * @param output The output writer.
	 * @param type The type of gate (e.g., "AND"), all capitals.
	 * @param triState If true, save that this element has a tri-state output.
	 */
	public void save(PrintWriter output, String type, boolean triState) {
		
		output.println("ELEMENT " + type);
		super.save(output);
		output.println(" int bits " + bits);
		output.println(" int numInputs " + numInputs);
		output.println(" String orientation \"" + orientation + "\"");
		output.println(" int delay " + propDelay);
		if (triState) {
			output.println(" int tristate 1");
		}
		output.println("END");
	} // end of save method
	
	/**
	 * Display info about this and gate.
	 * 
	 * @param info The JLabel to display with.
	 * @param type The type of element (e.g., "AND"), all capitals.
	 */
	public void showInfo(JLabel info, String type) {
		
		if (bits == 1)
			info.setText(numInputs + "-input " + type + " gate");
		else
			info.setText(bits + " " + numInputs + "-input " + type + " gate");
	} // end of showInfo method
	
	/**
	 * Get the rectangle bounding this element.
	 * For gates this will be 1/2 space higher and lower (for left/right gates)
	 * or 1/2 space wider on each side (for up/down gates).
	 * 
	 * @return a rectangle that bounds the element on the screen.
	 */
	public Rectangle getRect() {
		
		if (orientation == Orientation.left || orientation == Orientation.right) {
			return new Rectangle(x,y-JLSInfo.spacing/2,width,height+JLSInfo.spacing);
		}
		else {
			return new Rectangle(x-JLSInfo.spacing/2,y,width+JLSInfo.spacing,height);
		}
	} // end of getRect method
	
	/**
	 * Overridden.
	 */
	public void setToPrevious() {}
	
	/**
	 * Overridden.
	 */
	public int getDefaultDelay() {
		
		return 0;
	} // end of getDefaultDelay method
	
	/**
	 * Reset propagation delay to default value.
	 */
	public void resetPropDelay() {
		
		propDelay = getDefaultDelay();
	} // end of resetPropDelay method
	
	/**
	 * Gates have timing info (propagation delay).
	 * 
	 * @return true.
	 */
	public boolean hasTiming() {
		
		return true;
	} // end of hasTiming method

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
	 *  This method will rotate the gate if it is rotate-able.
	 * @param direction The direction to rotate
	 * @param g The current graphics context for use in recalculating size
	 */
	public void rotate(JLSInfo.Orientation direction, Graphics g)
	{
		if(orientation == Orientation.left)
		{
			if(direction == JLSInfo.Orientation.LEFT)
			{
				orientation = Orientation.down;
			}
			else
			{
				orientation = Orientation.up;
			}
		}
		else if(orientation == Orientation.right)
		{
			if(direction == JLSInfo.Orientation.LEFT)
			{
				orientation = Orientation.up;
			}
			else
			{
				orientation = Orientation.down;
			}
		}
		else if(orientation == Orientation.up)
		{
			if(direction == JLSInfo.Orientation.LEFT)
			{
				orientation = Orientation.left;
			}
			else
			{
				orientation = Orientation.right;
			}
		}
		else if(orientation == Orientation.down)
		{
			if(direction == JLSInfo.Orientation.LEFT)
			{
				orientation = Orientation.right;
			}
			else
			{
				orientation = Orientation.left;
			}
		}
		width = 0;
		height = 0;
		inputs.clear();
		outputs.clear();
		init(g);
	}
	
	/**
	 * Tells if a gate is capable of rotating, can only rotate when inputs or outputs have no attachments.
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
	 * Tells if a gate is capable of flipping, can only flip when inputs or outputs have no attachments.
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
	 * This method will flip a gate
	 * @param g The current graphics context to facilitate recalculation of size when flipping
	 */
	public void flip(Graphics g)
	{
		if(orientation == Orientation.left)
		{
			orientation = Orientation.right;
		}
		else if(orientation == Orientation.right)
		{
			orientation = Orientation.left;
		}
		else if(orientation == Orientation.up)
		{
			orientation = Orientation.down;
		}
		else if(orientation == Orientation.down)
		{
			orientation = Orientation.up;
		}
		inputs.clear();
		outputs.clear();
		width = 0;
		height = 0;
		init(g);
	}
	
	/**
	 * Dialog box to set multi-input gate parameters (number of inputs, number of gates).
	 * Used by all simple gates (nand, and, nor, or, xor).
	 */
	protected class GateCreate extends JDialog implements ActionListener {
		
		// properties
		private JButton ok = new JButton("OK");
		private JButton repeat;
		private JButton cancel = new JButton("Cancel");
		private JTextField inputsField = new JTextField(defaultInputs+"",5);
		private JTextField gatesField = new JTextField(defaultBits+"",5);
		private KeyPad inputsPad = new KeyPad(inputsField,10,defaultInputs,this);
		private KeyPad gatesPad = new KeyPad(gatesField,10,defaultBits,this);
		private JRadioButton left = new JRadioButton("left");
		private JRadioButton up = new JRadioButton("up");
		private JRadioButton down = new JRadioButton("down");
		private JRadioButton right = new JRadioButton("right");
		private String type;
		
		/**
		 * Set up dialog window.
		 * 
		 * @param x The x-coordinate of the position of the dialog.
		 * @param y The y-coordinate of the position of the dialog.
		 * @param type The type of gate (e.g. "AND").
		 */
		protected GateCreate(int x, int y, String type) {
			
			// set up window title
			super(JLSInfo.frame,"Create " + type + " Gate",true);
			
			// set not canceled
			cancelled = false;
			
			// set up window
			Container window = getContentPane();
			window.setLayout(new BoxLayout(window,BoxLayout.Y_AXIS));
			
			// set up input panel
			this.type = type;
			JPanel info = null;
			if (type.equals("NOT") || type.equals("XOR") || type == "Extend") {
				info = new JPanel(new GridLayout(1,2));
			}
			else {
				info = new JPanel(new GridLayout(2,2));
				JLabel inputs = new JLabel("Inputs: ",SwingConstants.RIGHT);
				info.add(inputs);
				JPanel in = new JPanel(new FlowLayout());
				in.add(inputsField);
				in.add(inputsPad);
				info.add(in);
			}
			JLabel gates;
			if (type.equals("Extend")) {
				gates = new JLabel("Outputs: ",SwingConstants.RIGHT);
				gatesField.setText("2");
			}
			else {
				gates = new JLabel("Gates (bits): ",SwingConstants.RIGHT);
			}
		
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
			group.add(up);
			group.add(down);
			group.add(right);
			right.setSelected(true);
			window.add(orients);
			
			// set up repeat button
			if (!type.equals("Extend")) {
				window.add(new JLabel(" "));
				JPanel rep = new JPanel();
				repeat = new JButton("Repeat Previous " + type + " Gate");
				repeat.setBackground(Color.yellow);
				rep.add(repeat);
				window.add(rep);
				repeat.addActionListener(this);
			}
			
			
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
				JLSInfo.hb.enableHelpOnButton(help,type,null);
			okCancel.add(help);
			window.add(okCancel);
			getRootPane().setDefaultButton(ok);
			
			ok.addActionListener(this);
			cancel.addActionListener(this);
			inputsField.addActionListener(this);
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
			if (event.getSource() == ok || event.getSource() == inputsField || event.getSource() == gatesField) {
				try {
					numInputs = Integer.parseInt(inputsField.getText());
					bits = Integer.parseInt(gatesField.getText());
				}
				catch (NumberFormatException ex) {
					JOptionPane.showMessageDialog(this,
							"Value not numeric", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (numInputs < 2) {
					JOptionPane.showMessageDialog(this,
							"Must have at least 2 inputs", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (bits < 1 && type != "Extend") {
					JOptionPane.showMessageDialog(this,
							"Must have at least 1 gate (bit)", "Error",
							JOptionPane.ERROR_MESSAGE);
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
				inputsPad.close();
				gatesPad.close();
				dispose();
			}
			
			// if repeat previous button, set previous values
			else if (event.getSource() == repeat) {
				setToPrevious();
				inputsField.setText(""+numInputs);
				gatesField.setText(""+bits);
				left.setSelected(false);
				right.setSelected(false);
				up.setSelected(false);
				down.setSelected(false);
				if (orientation == Orientation.left)
					left.setSelected(true);
				else if (orientation == Orientation.right)
					right.setSelected(true);
				else if (orientation == Orientation.up)
					up.setSelected(true);
				else 
					down.setSelected(true);
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
			inputsPad.close();
			gatesPad.close();
			dispose();
		} // end of cancel method
		
		/**
		 * Get number of gates.
		 * 
		 * @return The the number of gates selected.
		 */
		public int getGates() {
			
			return bits;
		} // end of getGates method
		
		/**
		 * Get number of inputs (bits).
		 * 
		 * @return The the number of inputs selected.
		 */
		public int getInputs() {
			
			return numInputs;
		} // end of getInputs method
		
		/**
		 * Get the orientation of the gate.
		 * 
		 * @return the orientation.
		 */
		public Orientation getOrientation() {
			
			return orientation;
		} // end of get Orientation method
		
	} // end of GateCreate class
	
} // end of Gate class
