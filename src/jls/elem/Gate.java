package jls.elem;

import jls.*;
import jls.sim.*;
import jls.util.Placement;

import java.util.BitSet;

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
	 * Identity and shared "remember previous settings" state for one gate
	 * kind (#22). Each subclass holds a single static Kind, replacing the
	 * per-class copies of the display name, save name, default delay, and
	 * previous-settings statics. What remains in a gate subclass is its
	 * genuine content: the outline shape and computeOutput().
	 */
	protected static final class Kind {
		
		private final String displayName;	// e.g. "AND"
		private final String saveName;		// e.g. "AndGate" (must match the
											// class name Circuit.load resolves)
		private final int fixedInputs;		// forced input count, or -1 if the
											// user chooses
		private final int defaultDelay;
		private int previousInputs = defaultInputs;
		private int previousBits = defaultBits;
		private Orientation previousOrientation = defaultOrientation;
		
		protected Kind(String displayName, String saveName, int fixedInputs,
				int defaultDelay) {
			
			this.displayName = displayName;
			this.saveName = saveName;
			this.fixedInputs = fixedInputs;
			this.defaultDelay = defaultDelay;
		} // end of constructor
		
	} // end of Kind class
	
	/**
	 * The kind descriptor for this gate.
	 * 
	 * @return the (static, per-class) kind.
	 */
	protected abstract Kind kind();
	
	/**
	 * Initialize this element in a GUI context: show the creation dialog
	 * for this kind and remember the accepted settings for the next gate
	 * of the same kind.
	 * 
	 * @param g The graphics object to use.
	 * @param editWindow The editor window this circuit is displayed in.
	 * @param x The x-coordinate of the last known mouse position.
	 * @param y The y-coordinate of the last known mouse position.
	 * 
	 * @return false if cancelled, true otherwise.
	 */
	@Override
	public boolean setup(Graphics g, JPanel editWindow, int x, int y) {
		
		Kind k = kind();
		boolean ok = setup(g,editWindow,x,y,k.displayName);
		if (ok) {
			if (k.fixedInputs < 0) {
				k.previousInputs = numInputs;
			}
			k.previousBits = bits;
			k.previousOrientation = orientation;
		}
		return ok;
	} // end of setup method
	
	/**
	 * Save this element in a file, under its kind's save name.
	 * 
	 * @param output The output writer.
	 */
	@Override
	public void save(PrintWriter output) {
		
		save(output,kind().saveName,false);
	} // end of save method
	
	/**
	 * Display info about this gate.
	 * 
	 * @param info The JLabel to display with.
	 */
	@Override
	public void showInfo(JLabel info) {
		
		showInfo(info,kind().displayName);
	} // end of showInfo method
	
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
		GateCreate gate = new GateCreate(type);
		
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
		Point p = Placement.dropPoint(editWindow,x,y,width,height);
		super.setXY(p.x,p.y);
		
		return true;
		
	} // end of setup method
	
	/**
	 * Initialize internal info for this element.
	 * Sets up size, inputs and outputs.
	 * 
	 * @param g Unused.
	 */
	@Override
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
	@Override
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
	// Declarative persistence (#23): one declaration drives save, load
	// dispatch, and copy for the attributes shared by every gate kind.
	private static final java.util.List<Attribute> OWN_ATTRIBUTES =
			java.util.List.of(
		new Attribute.IntAttribute("bits") {
			@Override
			protected int get(Element el) { return ((Gate)el).bits; }
			@Override
			protected void set(Element el, int v) { ((Gate)el).bits = v; }
		},
		new Attribute.IntAttribute("numInputs") {
			@Override
			protected int get(Element el) { return ((Gate)el).numInputs; }
			@Override
			protected void set(Element el, int v) { ((Gate)el).numInputs = v; }
		},
		new Attribute.StringAttribute("orientation") {
			@Override
			protected String get(Element el) {
				return ((Gate)el).orientation.toString();
			}
			@Override
			protected void set(Element el, String v) {
				// gates use their own lowercase orientation enum;
				// unknown strings leave the orientation unchanged
				for (Orientation o : Orientation.values()) {
					if (o.toString().equals(v)) {
						((Gate)el).orientation = o;
					}
				}
			}
		},
		new Attribute.IntAttribute("delay") {
			@Override
			protected int get(Element el) { return ((Gate)el).propDelay; }
			@Override
			protected void set(Element el, int v) { ((Gate)el).propDelay = v; }
		}
	);

	private static final java.util.List<Attribute> ALL_ATTRIBUTES =
			concatAttributes(OWN_ATTRIBUTES);

	/**
	 * Base attributes plus the shared gate attributes, in save order
	 * (#23).
	 */
	@Override
	protected java.util.List<Attribute> savedAttributes() {

		return ALL_ATTRIBUTES;
	} // end of savedAttributes method
	
	/**
	 * Copy this gate.
	 * 
	 * @return A copy of this gate.
	 */
	@Override
	public Element copy() {
		
		try {
			Gate it = getClass().getConstructor(Circuit.class).newInstance(circuit);
			copy(it);
			return it;
		} catch (ReflectiveOperationException ex) {
			throw new IllegalStateException("gate copy failed", ex);
		}
	} // end of copy method
	
	/**
	 * Copy info in this element to another element.
	 * 
	 * @param el The element to copy to.
	 */
	@Override
	public void copy(Element el) {

		Gate it = (Gate)el;
		it.outputs.add(outputs.get(0).copy(it));
		for (Input in : inputs) {
			it.inputs.add(in.copy(it));
		}
		super.copy(el);
		return;
	} // end of copy method
	
	/**
	 * Gate sizes are a pure function of input count and orientation,
	 * recomputed by init() on every load, so they are not saved (#21).
	 */
	@Override
	protected boolean sizeIsRecomputedOnLoad() {

		return true;
	} // end of sizeIsRecomputedOnLoad method

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
	@Override
	public Rectangle getRect() {
		
		if (orientation == Orientation.left || orientation == Orientation.right) {
			return new Rectangle(x,y-JLSInfo.spacing/2,width,height+JLSInfo.spacing);
		}
		else {
			return new Rectangle(x-JLSInfo.spacing/2,y,width+JLSInfo.spacing,height);
		}
	} // end of getRect method
	
	/**
	 * Set characteristics to the values of the previously created gate of
	 * this kind. Gates with a fixed input count keep their current count,
	 * matching the historical per-gate behavior.
	 */
	public void setToPrevious() {
		
		Kind k = kind();
		if (k.fixedInputs < 0) {
			numInputs = k.previousInputs;
		}
		bits = k.previousBits;
		orientation = k.previousOrientation;
	} // end of setToPrevious method
	
	/**
	 * Get default propagation delay for this gate kind.
	 */
	public int getDefaultDelay() {
		
		return kind().defaultDelay;
	} // end of getDefaultDelay method
	
	/**
	 * Reset propagation delay to default value.
	 */
	@Override
	public void resetPropDelay() {
		
		propDelay = getDefaultDelay();
	} // end of resetPropDelay method
	
	/**
	 * Gates have timing info (propagation delay).
	 * 
	 * @return true.
	 */
	@Override
	public boolean hasTiming() {
		
		return true;
	} // end of hasTiming method

	/**
	 * Get the propagation delay in this element.
	 * 
	 * @return the current delay.
	 */
	@Override
	public int getDelay() {
		
		return propDelay;
	} // end of getDelay method
	
	/**
	 * Set the propagation delay in this element.
	 * 
	 * @param temp The new delay amount.
	 */
	@Override
	public void setDelay(int temp) {
		
		propDelay = temp;
	} // end of setDelay method
	
	/**
	 *  This method will rotate the gate if it is rotate-able.
	 * @param direction The direction to rotate
	 * @param g The current graphics context for use in recalculating size
	 */
	@Override
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
	@Override
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
	@Override
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
	@Override
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
	@SuppressWarnings("serial")
	protected class GateCreate extends ElementDialog implements ActionListener {
		
		// properties
		private JButton repeat;
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
		 * @param type The type of gate (e.g. "AND").
		 */
		protected GateCreate(String type) {
			
			// set up window title
			super("Create " + type + " Gate",type);
			
			// set not canceled
			cancelled = false;
			
			// set up window
			Container window = getContentPane();
			
			// set up input panel
			this.type = type;
			JPanel info = null;
			if (type.equals("NOT") || type.equals("XOR") || type.equals("Extend")) {
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
			
			confirmOnEnter(inputsField);
			confirmOnEnter(gatesField);
			finishDialog();
		} // end of constructor
		
		/**
		 * Validate the form and create the gate.
		 */
		@Override
		protected void validateAndAccept() {
			
			try {
				numInputs = Integer.parseInt(inputsField.getText());
				bits = Integer.parseInt(gatesField.getText());
			}
			catch (NumberFormatException ex) {
				reject("Value not numeric");
				return;
			}
			if (numInputs < 2) {
				reject("Must have at least 2 inputs");
				return;
			}
			if (bits < 1 && !"Extend".equals(type)) {
				reject("Must have at least 1 gate (bit)");
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
		} // end of validateAndAccept method
		
		/**
		 * React to the repeat previous button.
		 * 
		 * @param event The event object for this action.
		 */
		@Override
		public void actionPerformed(ActionEvent event) {
			
			// if repeat previous button, set previous values
			if (event.getSource() == repeat) {
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
		} // end of actionPerformed method
		
		/**
		 * Cancel this gate.
		 */
		@Override
		protected void cancelDialog() {
			
			cancelled = true;
			inputsPad.close();
			gatesPad.close();
			dispose();
		} // end of cancelDialog method
		
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
	

//-------------------------------------------------------------------------------
// Simulation
//-------------------------------------------------------------------------------

	private BitSet toBeValue;

	/**
	 * Compute this gate's output value from its current input values,
	 * with absent (null) values counting as zero. This is the only
	 * simulation behavior that differs between the gate kinds (#22);
	 * the event handling below is shared.
	 *
	 * @return the output value.
	 */
	protected abstract BitSet computeOutput();

	/**
	 * Initialize this element: the output pin starts at 0, and a gate
	 * whose output for all-zero inputs is not 0 (NAND, NOR, NOT) posts
	 * an event to drive that value at time 0.
	 *
	 * @param sim The simulator to post events to.
	 */
	@Override
	public void initSim(Simulator sim) {

		// set output pin
		Output out = outputs.get(0);
		out.setValue(new BitSet(1));

		// drive the all-zero-inputs output value
		BitSet initial = computeOutput();
		if (!initial.isEmpty()) {
			sim.post(new SimEvent(0,this,initial));
		}
		toBeValue = (BitSet)initial.clone();
	} // end of initSim method

	/**
	 * React to an event.
	 *
	 * @param now The current simulation time.
	 * @param sim The simulator to post events to.
	 * @param todo If null, an input has changed, otherwise it is the value to output.
	 */
	@Override
	public void react(long now, Simulator sim, Object todo) {

		// if the input has changed ...
		if (todo == null) {

			BitSet value = computeOutput();

			// if new value is different from the value propagating through
			// this gate, then post an event
			if (!value.equals(toBeValue)) {
				toBeValue = (BitSet)value.clone();
				sim.post(new SimEvent(now+propDelay,this,value));
			}
		}
		else {

			// send the new output value to the output
			BitSet newValue = (BitSet)todo;
			outputs.get(0).propagate(newValue,now,sim);
		}
	} // end of react method

} // end of Gate class
