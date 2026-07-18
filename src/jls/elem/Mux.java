package jls.elem;

import jls.*;
import jls.sim.*;
import jls.util.Placement;
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
public final class Mux extends LogicElement {

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
	@Override
	public boolean setup(Graphics g, JPanel editWindow, int x, int y) {
		
		// show creation dialog
		new MuxCreate();
		
		// don't do anything if user cancelled element
		if (cancelled)
			return false;
		
		// complete initialization
		init(g);
		
		// save position
		Point p = Placement.dropPoint(editWindow,x,y,width,height);
		super.setXY(p.x,p.y);
		
		return true;
	} // end of setup method
	
	/**
	 * Initialize internal info for this element.
	 * 
	 * @param g The Graphics object to use.
	 */
	@Override
	public void init(Graphics g) {

		// canonical geometry (output RIGHT), transformed to the current
		// output orientation (#24); the selector side is independent of
		// that transform (input order never mirrors with the selector),
		// so its put is placed directly from its own orientation
		int s = JLSInfo.spacing;
		GridTransform.Chain t = placement();
		Dimension d = t.size();
		width = d.width;
		height = d.height;

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

		// create inputs and output
		for (int i=0; i<numInputs; i+=1) {
			Point p = t.map(0,(i+1)*s);
			inputs.add(new Input("input"+i,this,p.x,p.y,bits));
		}
		Point p = t.map(2*s,(numInputs+1)/2*s);
		outputs.add(new Output("output",this,p.x,p.y,bits));

	} // end of init method

	/**
	 * The transform from canonical geometry (output RIGHT) to the current
	 * output orientation.
	 */
	private GridTransform.Chain placement() {

		int s = JLSInfo.spacing;
		GridTransform.Chain t = GridTransform.chain(2*s,(numInputs+1)*s);
		switch (outputOrientation) {
		case RIGHT:
			break;
		case LEFT:
			t.mirrorX();
			break;
		case UP:
			t.rotateCCW();
			break;
		default: // DOWN
			t.rotateCCW().mirrorY();
			break;
		}
		return t;
	} // end of placement method
	
	/**
	 * Draw this mux.
	 * 
	 * @param g The graphics object to draw with.
	 */
	@Override
	public void draw(Graphics g) {
		
		// draw context
		super.draw(g);
		
		// draw shape: the conventional trapezoid symbol (#123), wide side
		// at the inputs, narrow side at the output. Canonical segments
		// (output RIGHT) are mapped through the orientation transform
		// (#24). The slant is half a grid unit, so the narrow side still
		// spans every put on it and the selector's put circle (radius
		// pointDiameter/2) still touches the slanted edge at its put.
		int s = JLSInfo.spacing;
		g.setColor(Color.black);
		GridTransform.Chain t = placement();
		int slant = s/2;
		int ch = (numInputs+1)*s;					// canonical height
		t.drawLine(g,x,y,0,0,0,ch);					// input (wide) side
		t.drawLine(g,x,y,2*s,slant,2*s,ch-slant);	// output (narrow) side
		t.drawLine(g,x,y,0,0,2*s,slant);			// top slant
		t.drawLine(g,x,y,0,ch,2*s,ch-slant);		// bottom slant
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

	// Declarative persistence (#23): one declaration drives save, load
	// dispatch, and copy for this element's own attributes.
	private static final java.util.List<Attribute> OWN_ATTRIBUTES =
			java.util.List.of(
		new Attribute.IntAttribute("inputs") {
			/**
			 * Read the number of data inputs from the given mux.
			 *
			 * @param el The element (a Mux) to read from.
			 * @return the number of inputs.
			 */
			@Override
			protected int get(Element el) { return ((Mux)el).numInputs; }
			/**
			 * Set the number of data inputs on the given mux.
			 *
			 * @param el The element (a Mux) to modify.
			 * @param v The new number of inputs.
			 */
			@Override
			protected void set(Element el, int v) { ((Mux)el).numInputs = v; }
		},
		new Attribute.IntAttribute("bits") {
			/**
			 * Read the bit width from the given mux.
			 *
			 * @param el The element (a Mux) to read from.
			 * @return the number of bits.
			 */
			@Override
			protected int get(Element el) { return ((Mux)el).bits; }
			/**
			 * Set the bit width on the given mux.
			 *
			 * @param el The element (a Mux) to modify.
			 * @param v The new bit width.
			 */
			@Override
			protected void set(Element el, int v) { ((Mux)el).bits = v; }
		},
		new Attribute.IntAttribute("delay") {
			/**
			 * Read the propagation delay from the given mux.
			 *
			 * @param el The element (a Mux) to read from.
			 * @return the propagation delay.
			 */
			@Override
			protected int get(Element el) { return ((Mux)el).propDelay; }
			/**
			 * Set the propagation delay on the given mux.
			 *
			 * @param el The element (a Mux) to modify.
			 * @param v The new propagation delay.
			 */
			@Override
			protected void set(Element el, int v) { ((Mux)el).propDelay = v; }
		},
		new Attribute.OrientationAttribute("iOrient") {
			/**
			 * Read the output orientation from the given mux.
			 *
			 * @param el The element (a Mux) to read from.
			 * @return the output orientation.
			 */
			@Override
			protected JLSInfo.Orientation getOrientation(Element el) {
				return ((Mux)el).outputOrientation;
			}
			/**
			 * Set the output orientation on the given mux.
			 *
			 * @param el The element (a Mux) to modify.
			 * @param o The new output orientation.
			 */
			@Override
			protected void setOrientation(Element el, JLSInfo.Orientation o) {
				((Mux)el).outputOrientation = o;
			}
		},
		new Attribute.OrientationAttribute("sOrient") {
			/**
			 * Read the selector orientation from the given mux.
			 *
			 * @param el The element (a Mux) to read from.
			 * @return the selector orientation.
			 */
			@Override
			protected JLSInfo.Orientation getOrientation(Element el) {
				return ((Mux)el).selectorOrientation;
			}
			/**
			 * Set the selector orientation on the given mux.
			 *
			 * @param el The element (a Mux) to modify.
			 * @param o The new selector orientation.
			 */
			@Override
			protected void setOrientation(Element el, JLSInfo.Orientation o) {
				((Mux)el).selectorOrientation = o;
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
	 * Save this element.
	 *
	 * @param output The output writer.
	 */
	@Override
	public void save(PrintWriter output) {

		output.println("ELEMENT Mux");
		super.save(output);
		output.println("END");
	} // end of save method

	/**
	 * Copy this element.
	 *
	 * @return a copy of this element.
	 */
	@Override
	public Element copy() {

		Mux it = new Mux(circuit);
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
	@Override
	public void showInfo(JLabel info) {
		
		info.setText(numInputs + " input, " + bits + " bit multiplexor");
	} // end of showInfo method

	/**
	 * Multiplexors have timing info (propagation delay).
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
	 * Tells if a mux is capable of flipping, can only flip when inputs or outputs have no attachments.
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
	 * This method will flip a mux's selector
	 * @param g The current graphics context to facilitate recalculation of size when flipping
	 */
	@Override
	public void flip(Graphics g)
	{
		selectorOrientation = selectorOrientation.flipped();
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
	@Override
	public void rotate(JLSInfo.Orientation direction, Graphics g)
	{
		if(direction == JLSInfo.Orientation.LEFT)
		{
			selectorOrientation = selectorOrientation.ccw();
			outputOrientation = outputOrientation.ccw();
		}
		else if(direction == JLSInfo.Orientation.RIGHT)
		{
			selectorOrientation = selectorOrientation.cw();
			outputOrientation = outputOrientation.cw();
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
	 * Dialog box to set inputs and bits.
	 */
	protected class MuxCreate extends ElementDialog implements ActionListener {

		// properties
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
		 */
		protected MuxCreate() {
			
			// set up window title
			super("Create Multiplexor","mux");

			// set not cancelled
			cancelled = false;

			// set up window
			Container window = getContentPane();

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

			oLeft.addActionListener(this);
			oRight.addActionListener(this);
			oUp.addActionListener(this);
			oDown.addActionListener(this);

			confirmOnEnter(inputsField);
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
		 * Validate the form and create the mux.
		 */
		@Override
		protected void validateAndAccept() {

			try {
				numInputs = Integer.parseInt(inputsField.getText());
				bits = Integer.parseInt(bitsField.getText());
			}
			catch (NumberFormatException ex) {
				reject("Value not numeric, try again");
				numInputs = -1;
				return;
			}
			if (numInputs < 2) {
				reject("Must have at least 2 inputs");
				numInputs = -1;
				return;
			}
			if (bits < 1) {
				reject("Must be at least one bit");
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
		} // end of validateAndAccept method

		/**
		 * Cancel this mux.
		 */
		@Override
		protected void cancelDialog() {

			cancelled = true;
			inputsPad.close();
			bitsPad.close();
			dispose();
		} // end of cancelDialog method

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
	@Override
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
	@Override
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
