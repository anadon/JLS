package jls.elem;

import jls.*;
import jls.sim.*;
import jls.util.Placement;
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
public final class TriState extends LogicElement {
	
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
	 * @see jls.SimulationSemanticsRegressionTest#triStateDoesNotRepostUnchangedOutputEvents()
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
	@Override
	public boolean setup(Graphics g, JPanel editWindow, int x, int y) {
		
		// show creation dialog
		new TriStateCreate();
		
		// don't do anything if user cancelled gate
		if (cancelled)
			return false;
		
		// set up inputs and outputs
		init(g);
		
		// save position
		Point p = Placement.dropPoint(editWindow,x,y,width,height);
		super.setXY(p.x,p.y);
		
		return true;
		
	} // end of setup method
	
	/**
	 * Initialize internal info for this element.
	 * Sets up size, inputs and output.
	 * 
	 * @param g Unused.
	 * @see jls.SimulationSemanticsRegressionTest#triStateDoesNotRepostUnchangedOutputEvents()
	 */
	@Override
	public void init(Graphics g) {

		// canonical geometry (gate RIGHT, control DOWN), transformed to
		// the current orientation pair (#24)
		int s = JLSInfo.spacing;
		GridTransform.Chain t = placement();
		Dimension d = t.size();
		width = d.width;
		height = d.height;
		Point in = t.map(0, s);
		Point ctl = t.map(2*s, 3*s);
		Point outAt = t.map(4*s, s);
		inputs.add(new Input("input",this,in.x,in.y,bits));
		inputs.add(new Input("control",this,ctl.x,ctl.y,1));
		Output out = new Output("output",this,outAt.x,outAt.y,bits);
		outputs.add(out);
		out.setTriState(true);
	} // end of init method

	/**
	 * The transform from canonical geometry (gate RIGHT, control DOWN)
	 * to the current orientation pair.
	 */
	private GridTransform.Chain placement() {

		// the control must be perpendicular to the gate; loads with an
		// invalid pair have always failed, so keep rejecting them
		boolean gateHorizontal =
				gateOrientation == JLSInfo.Orientation.LEFT
				|| gateOrientation == JLSInfo.Orientation.RIGHT;
		boolean controlHorizontal =
				controlOrientation == JLSInfo.Orientation.LEFT
				|| controlOrientation == JLSInfo.Orientation.RIGHT;
		if (gateHorizontal == controlHorizontal) {
			throw new IllegalStateException(
					"invalid TriState orientation combination: gate "
					+ gateOrientation + ", control " + controlOrientation);
		}
		int s = JLSInfo.spacing;
		GridTransform.Chain t = GridTransform.chain(4*s, 3*s);
		switch (gateOrientation) {
		case RIGHT:
			if (controlOrientation == JLSInfo.Orientation.UP) {
				t.mirrorY();
			}
			break;
		case LEFT:
			if (controlOrientation == JLSInfo.Orientation.UP) {
				t.rotate180();
			}
			else {
				t.mirrorX();
			}
			break;
		case UP:
			t.rotateCCW();
			if (controlOrientation == JLSInfo.Orientation.LEFT) {
				t.mirrorX();
			}
			break;
		default: // DOWN
			if (controlOrientation == JLSInfo.Orientation.LEFT) {
				t.rotateCW();
			}
			else {
				t.rotateCCW().mirrorY();
			}
			break;
		}
		return t;
	} // end of placement method
	
	/**
	 * Draw this gate.
	 * 
	 * @param g The graphics object to draw with.
	 */
	@Override
	public void draw(Graphics g) {
		
		// draw context
		super.draw(g);
		
		// draw gate: canonical segments (gate RIGHT, control DOWN)
		// mapped through the orientation transform (#24)
		int s = JLSInfo.spacing;
		g.setColor(Color.black);
		GridTransform.Chain t = placement();
		t.drawLine(g,x,y,0,s,s,s);				// input wire
		t.drawLine(g,x,y,s,0,s,2*s);			// back
		t.drawLine(g,x,y,s,0,3*s,s);			// top
		t.drawLine(g,x,y,s,2*s,3*s,s);			// bottom
		t.drawLine(g,x,y,3*s,s,4*s,s);			// output wire
		t.drawLine(g,x,y,2*s,3*s/2,2*s,3*s);	// control wire
		// draw inputs and outputs
		inputs.get(0).draw(g);
		inputs.get(1).draw(g);
		outputs.get(0).draw(g);
		
	} // end of draw method
	
	// Declarative persistence (#23): one declaration drives save, load
	// dispatch, and copy for this element's own attributes.
	private static final java.util.List<Attribute> OWN_ATTRIBUTES =
			java.util.List.of(
		new Attribute.IntAttribute("bits") {
			/**
			 * Read the bit-width from a tri-state element.
			 *
			 * @param el The tri-state element to read from.
			 * @return the number of bits.
			 */
			@Override
			protected int get(Element el) { return ((TriState)el).bits; }
			/**
			 * Set the bit-width on a tri-state element.
			 *
			 * @param el The tri-state element to modify.
			 * @param v The new bit-width.
			 */
			@Override
			protected void set(Element el, int v) { ((TriState)el).bits = v; }
		},
		new Attribute.IntAttribute("delay") {
			/**
			 * Read the propagation delay from a tri-state element.
			 *
			 * @param el The tri-state element to read from.
			 * @return the propagation delay.
			 */
			@Override
			protected int get(Element el) { return ((TriState)el).propDelay; }
			/**
			 * Set the propagation delay on a tri-state element.
			 *
			 * @param el The tri-state element to modify.
			 * @param v The new propagation delay.
			 */
			@Override
			protected void set(Element el, int v) { ((TriState)el).propDelay = v; }
		},
		new Attribute.OrientationAttribute("Gorient") {
			/**
			 * Read the gate orientation from a tri-state element.
			 *
			 * @param el The tri-state element to read from.
			 * @return the gate orientation.
			 */
			@Override
			protected JLSInfo.Orientation getOrientation(Element el) {
				return ((TriState)el).gateOrientation;
			}
			/**
			 * Set the gate orientation on a tri-state element.
			 *
			 * @param el The tri-state element to modify.
			 * @param o The new gate orientation.
			 */
			@Override
			protected void setOrientation(Element el, JLSInfo.Orientation o) {
				((TriState)el).gateOrientation = o;
			}
		},
		new Attribute.OrientationAttribute("Corient") {
			/**
			 * Read the control orientation from a tri-state element.
			 *
			 * @param el The tri-state element to read from.
			 * @return the control orientation.
			 */
			@Override
			protected JLSInfo.Orientation getOrientation(Element el) {
				return ((TriState)el).controlOrientation;
			}
			/**
			 * Set the control orientation on a tri-state element.
			 *
			 * @param el The tri-state element to modify.
			 * @param o The new control orientation.
			 */
			@Override
			protected void setOrientation(Element el, JLSInfo.Orientation o) {
				((TriState)el).controlOrientation = o;
			}
		}
	);

	private static final java.util.List<Attribute> ALL_ATTRIBUTES =
			concatAttributes(OWN_ATTRIBUTES);

	/**
	 * Base attributes plus this element's own, in save order (#23).
	 */
	@Override
	protected java.util.List<Attribute> savedAttributes() {

		return ALL_ATTRIBUTES;
	} // end of savedAttributes method

	/**
	 * Save this element in a file.
	 *
	 * @param output The output writer.
	 */
	@Override
	public void save(PrintWriter output) {

		output.println("ELEMENT TriState");
		super.save(output);
		output.println("END");
	} // end of save method

	/**
	 * Copy this element.
	 *
	 * @return a copy of this tri-state.
	 */
	@Override
	public Element copy() {

		TriState it = new TriState(circuit);
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
	@Override
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
	@Override
	public boolean hasTiming() {
		
		return true;
	} // end of hasTiming method

	/**
	 * Reset propagation delay to default value.
	 */
	@Override
	public void resetPropDelay() {
		
		propDelay = defaultPropDelay;
	} // end of resetPropDelay method

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
	 * Tells if a tristate is capable of flipping, can only flip when inputs or outputs have no attachments.
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
	 * This method will flip a tristate's control input
	 * @param g The current graphics context to facilitate recalculation of size when flipping
	 */
	@Override
	public void flip(Graphics g)
	{
		controlOrientation = controlOrientation.flipped();
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
	 *  This method will rotate the tristate if it is rotateable.
	 * @param direction The direction to rotate
	 * @param g The current graphics context for use in recalculating size
	 */
	@Override
	public void rotate(JLSInfo.Orientation direction, Graphics g)
	{
		if(direction == JLSInfo.Orientation.LEFT)
		{
			controlOrientation = controlOrientation.ccw();
			gateOrientation = gateOrientation.ccw();
		}
		else if(direction == JLSInfo.Orientation.RIGHT)
		{
			controlOrientation = controlOrientation.cw();
			gateOrientation = gateOrientation.cw();
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
	private class TriStateCreate extends ElementDialog implements ActionListener {

		// properties
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
		 */
		private TriStateCreate() {
			
			// set up window title
			super("Create TriState","TRISTATE");

			// set not cancelled
			cancelled = false;

			// set up window
			Container window = getContentPane();

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

			oLeft.addActionListener(this);
			oRight.addActionListener(this);
			oUp.addActionListener(this);
			oDown.addActionListener(this);

			confirmOnEnter(bitsField);
			finishDialog();
		} // end of constructor

		/**
		 * React to output orientation buttons.
		 *
		 * @param event The event object for this action.
		 */
		@Override
		public void actionPerformed(ActionEvent event) {

			if(event.getSource() == oLeft || event.getSource() == oRight)
			{
				olbl2.setVisible(true);
				sUp.setVisible(true);
				sDown.setVisible(true);
				sDown.setSelected(true);
				sLeft.setVisible(false);
				sRight.setVisible(false);
			}
			else if(event.getSource() == oUp || event.getSource() == oDown)
			{
				olbl2.setVisible(true);
				sLeft.setVisible(true);
				sLeft.setSelected(true);
				sRight.setVisible(true);
				sUp.setVisible(false);
				sDown.setVisible(false);
			}
		} // end of actionPerformed method

		/**
		 * Validate the form and create the tri-state gate.
		 */
		@Override
		protected void validateAndAccept() {

			try {
				bits = Integer.parseInt(bitsField.getText());
			}
			catch (NumberFormatException ex) {
				reject("Value not numeric, try again");
				return;
			}
			if (bits < 1) {
				reject("Must be at least 1 bit");
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
		} // end of validateAndAccept method

		/**
		 * Cancel this gate.
		 */
		@Override
		protected void cancelDialog() {

			cancelled = true;
			dispose();
		} // end of cancelDialog method

	} // end of TriStateCreate class
	
//	-------------------------------------------------------------------------------
//	Simulation
//	-------------------------------------------------------------------------------

	// the value scheduled to reach the output, null meaning off (HiZ);
	// used to suppress redundant output events (issue #98, S6)
	private BitSet toBeValue;

	/**
	 * Initialize this element by setting its output pin to off (null).
	 *
	 * @param sim Unused.
	 * @see jls.SimulationSemanticsRegressionTest#triStateDoesNotRepostUnchangedOutputEvents()
	 */
	@Override
	public void initSim(Simulator sim) {

		// set output pin
		Output out = outputs.get(0);
		out.setValue(null);
		toBeValue = null;

	} // end of initSim method
	
	/**
	 * React to an event.
	 * 
	 * @param now The current simulation time.
	 * @param sim The simulator to post events to.
	 * @param todo If null, an input has changed, otherwise it is the value to output.
	 * @see jls.SimulationSemanticsRegressionTest#triStateDoesNotRepostUnchangedOutputEvents()
	 */
	@Override
	public void react(long now, Simulator sim, Object todo) {
		
		// if the input has changed ...
		if (todo == null) {
			
			// get control input
			BitSet control = inputs.get(1).getValue();
			if (control ==  null)
				control = new BitSet();

			// if it is zero, turn off output
			// (but not if it is already off or turning off - #98, S6)
			if (!control.get(0)) {
				if (toBeValue == null)
					return;
				toBeValue = null;
				sim.post(new SimEvent(now+propDelay,this,"off"));
			}
			else {

				// get the data input and send it to the output
				// (but not if that value is already on the way - #98, S6)
				BitSet value = inputs.get(0).getValue();
				if (value == null)
					value = new BitSet();
				else
					value = (BitSet)value.clone();
				if (value.equals(toBeValue))
					return;
				toBeValue = (BitSet)value.clone();
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
