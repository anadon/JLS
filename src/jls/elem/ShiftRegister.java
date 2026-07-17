package jls.elem;

import jls.*;
import jls.sim.*;
import jls.util.Placement;
import java.io.PrintWriter;
import java.util.BitSet;

/**
 * Combinational barrel shifter (issue #122). The bsiever fork lineage
 * shipped this element as "ShiftRegister" in its 4.6 release, and that
 * save tag is kept here so fork-authored circuits load upstream; note
 * that despite the name it holds no state - it is a Mux-like
 * combinational element that shifts its data input by the value on its
 * amount input.
 *
 * Semantics (derived from the fork source at bsiever/JLS@038a5b67, per
 * issue #122 section 10): output = input shifted by amount, where the
 * shift is logical left (zero fill), logical right (zero fill), or
 * arithmetic right (sign fill), fixed per instance. The amount input is
 * ceil(log2(bits)) wide. New value propagates after the instance's
 * delay, with the standard redundant-event suppression (section 6.2 of
 * docs/simulation-semantics.md).
 *
 * GUI classes are referenced fully qualified, not imported: new files
 * in jls.elem may not import java.awt/javax.swing (issue #77,
 * HeadlessCoreRatchetTest).
 *
 * @author David A. Poplawski
 */
public class ShiftRegister extends LogicElement {

	/** The shift-kind rule, shared by the dialog and the loader
	 *  (issue #52 pattern: one string, two surfaces). */
	static final String TYPE_CONSTRAINT =
			"Shift type must be LogicalLeft, LogicalRight or ArithmeticRight";

	/** The width rule, shared by the dialog and the loader. */
	static final String BITS_CONSTRAINT = "Must have at least 2 bits";

	/**
	 * The width rule: a 1-bit shifter has a 0-bit amount input.
	 *
	 * @param bits The proposed data width.
	 *
	 * @return the violated constraint message, or null if valid.
	 */
	static String checkBits(int bits) {

		return bits < 2 ? BITS_CONSTRAINT : null;
	} // end of checkBits method

	// shift kinds (save-file names are the enum names, fixed by the
	// fork's 4.6 file format)
	private enum Type {LogicalLeft, LogicalRight, ArithmeticRight};

	// default values
	private static final Type defaultType = Type.LogicalLeft;
	private static final int defaultBits = 8;
	private static final int defaultPropDelay = 25;

	// saved properties
	private Type type = defaultType;
	private int bits = defaultBits;
	private int propDelay = defaultPropDelay;
	private JLSInfo.Orientation outputOrientation = JLSInfo.Orientation.RIGHT;
	private JLSInfo.Orientation amountOrientation = JLSInfo.Orientation.DOWN;

	// running properties
	private boolean cancelled;

	/**
	 * Create a new shift register element.
	 *
	 * @param circuit The circuit this element is part of.
	 */
	public ShiftRegister(Circuit circuit) {

		super(circuit);
	} // end of constructor

	/**
	 * Display dialog to get characteristics.
	 *
	 * @param g The Graphics object to use to initialize sizes
	 * @param editWindow The editor window this element will be displayed in.
	 * @param x The x-coordinate of the last known mouse position.
	 * @param y The y-coordinate of the last known mouse position.
	 *
	 * @return false if cancelled, true otherwise.
	 */
	public boolean setup(java.awt.Graphics g, javax.swing.JPanel editWindow,
			int x, int y) {

		// show creation dialog
		new ShiftRegisterCreate();

		// don't do anything if user cancelled element
		if (cancelled)
			return false;

		// complete initialization
		init(g);

		// save position
		java.awt.Point p = Placement.dropPoint(editWindow,x,y,width,height);
		super.setXY(p.x,p.y);

		return true;
	} // end of setup method

	/**
	 * Initialize internal info for this element.
	 *
	 * @param g The Graphics object to use.
	 */
	public void init(java.awt.Graphics g) {

		// canonical geometry (output RIGHT), transformed to the current
		// output orientation (#24); the amount side is independent of
		// that transform, so its put is placed directly from its own
		// orientation - the same scheme as Mux, whose geometry this
		// element inherited in the fork
		int s = JLSInfo.spacing;
		GridTransform.Chain t = placement();
		java.awt.Dimension d = t.size();
		width = d.width;
		height = d.height;

		// determine number of amount bits
		int sbits = 32 - Integer.numberOfLeadingZeros(bits-1);

		// create amount input
		if(amountOrientation == JLSInfo.Orientation.DOWN)
		{
			inputs.add(new Input("amount",this,s,height,sbits));
		}
		else if(amountOrientation == JLSInfo.Orientation.UP)
		{
			inputs.add(new Input("amount",this,s,0,sbits));
		}
		else if(amountOrientation == JLSInfo.Orientation.LEFT)
		{
			inputs.add(new Input("amount",this,0,s,sbits));
		}
		else if(amountOrientation == JLSInfo.Orientation.RIGHT)
		{
			inputs.add(new Input("amount",this,width,s,sbits));
		}

		// create data input and output
		java.awt.Point in = t.map(0,s);
		inputs.add(new Input("input",this,in.x,in.y,bits));
		java.awt.Point out = t.map(2*s,s);
		outputs.add(new Output("output",this,out.x,out.y,bits));

	} // end of init method

	/**
	 * The transform from canonical geometry (output RIGHT) to the current
	 * output orientation.
	 */
	private GridTransform.Chain placement() {

		int s = JLSInfo.spacing;
		GridTransform.Chain t = GridTransform.chain(2*s,2*s);
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
	 * Draw this shift register.
	 *
	 * @param g The graphics object to draw with.
	 */
	public void draw(java.awt.Graphics g) {

		// draw context
		super.draw(g);

		// draw shape
		g.setColor(java.awt.Color.black);
		g.drawRect(x,y,width,height);

		// label the data input so it can't be confused with the
		// (narrower) amount input
		java.awt.FontMetrics fm = g.getFontMetrics();
		int ascent = fm.getAscent();
		int hi = ascent + fm.getDescent();
		int d2 = JLSInfo.pointDiameter/2;
		Input data = inputs.get(1);
		g.setColor(java.awt.Color.BLACK);
		if(outputOrientation == JLSInfo.Orientation.RIGHT)
		{
			g.drawString("in",x+d2,data.getY()-hi/2+ascent);
		}
		else if(outputOrientation == JLSInfo.Orientation.LEFT)
		{
			g.drawString("in",x+width-5*d2,data.getY()-hi/2+ascent);
		}
		else // UP or DOWN
		{
			g.drawString("in",data.getX()-4,y+5*d2);
		}

		// draw inputs and output
		for (Input input : inputs) {
			input.draw(g);
		}
		outputs.get(0).draw(g);

	} // end of draw method

	// Declarative persistence (#23): one declaration drives save, load
	// dispatch, and copy for this element's own attributes. The names
	// and save order are the fork's, so 4.6-era fork files load
	// unchanged (issue #122 H2).
	private static final java.util.List<Attribute> OWN_ATTRIBUTES =
			java.util.List.of(
		new Attribute.StringAttribute("type") {
			protected String get(Element el) {
				return ((ShiftRegister)el).type.name();
			}
			protected void set(Element el, String v) {
				// an unknown kind is rejected, not guessed (issue #122
				// section 9: fail with the element named)
				for (Type t : Type.values()) {
					if (t.name().equals(v)) {
						((ShiftRegister)el).type = t;
						return;
					}
				}
				throw new IllegalArgumentException(TYPE_CONSTRAINT);
			}
		},
		new Attribute.IntAttribute("bits") {
			protected int get(Element el) { return ((ShiftRegister)el).bits; }
			protected void set(Element el, int v) {
				String violated = checkBits(v);
				if (violated != null) {
					throw new IllegalArgumentException(violated);
				}
				((ShiftRegister)el).bits = v;
			}
		},
		new Attribute.IntAttribute("delay") {
			protected int get(Element el) { return ((ShiftRegister)el).propDelay; }
			protected void set(Element el, int v) { ((ShiftRegister)el).propDelay = v; }
		},
		new Attribute.OrientationAttribute("iOrient") {
			protected JLSInfo.Orientation getOrientation(Element el) {
				return ((ShiftRegister)el).outputOrientation;
			}
			protected void setOrientation(Element el, JLSInfo.Orientation o) {
				((ShiftRegister)el).outputOrientation = o;
			}
		},
		new Attribute.OrientationAttribute("sOrient") {
			protected JLSInfo.Orientation getOrientation(Element el) {
				return ((ShiftRegister)el).amountOrientation;
			}
			protected void setOrientation(Element el, JLSInfo.Orientation o) {
				((ShiftRegister)el).amountOrientation = o;
			}
		}
	);

	private static final java.util.List<Attribute> ALL_ATTRIBUTES =
			concatAttributes(OWN_ATTRIBUTES);

	/**
	 * Base attributes plus this element's own, in save order (#23).
	 */
	protected java.util.List<Attribute> savedAttributes() {

		return ALL_ATTRIBUTES;
	} // end of savedAttributes method

	/**
	 * Save this element.
	 *
	 * @param output The output writer.
	 */
	public void save(PrintWriter output) {

		output.println("ELEMENT ShiftRegister");
		super.save(output);
		output.println("END");
	} // end of save method

	/**
	 * Copy this element.
	 *
	 * @return a copy of this element.
	 */
	public Element copy() {

		ShiftRegister it = new ShiftRegister(circuit);
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
	public void showInfo(javax.swing.JLabel info) {

		String kind = "";
		switch (type) {
		case LogicalLeft: kind = "logical left"; break;
		case LogicalRight: kind = "logical right"; break;
		default: kind = "arithmetic right"; break;
		}
		info.setText(bits + " bit " + kind + " shift register");
	} // end of showInfo method

	/**
	 * Shift registers have timing info (propagation delay).
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
	 * @param temp The new delay amount.
	 */
	public void setDelay(int temp) {

		propDelay = temp;
	} // end of setDelay method

	/**
	 * Tells if a shift register is capable of flipping, can only flip
	 * when inputs or outputs have no attachments.
	 *
	 * @return False if any input or output has a wire attached, True otherwise
	 */
	public boolean canFlip() {

		for (Input i : inputs) {
			if (i.isAttached()) {
				return false;
			}
		}
		for (Output o : outputs) {
			if (o.isAttached()) {
				return false;
			}
		}
		return true;
	} // end of canFlip method

	/**
	 * This method will flip a shift register's amount input side.
	 *
	 * @param g The current graphics context to facilitate recalculation
	 *          of size when flipping.
	 */
	public void flip(java.awt.Graphics g) {

		amountOrientation = amountOrientation.flipped();
		inputs.clear();
		outputs.clear();
		width = 0;
		height = 0;
		init(g);
	} // end of flip method

	/**
	 * This method will rotate the shift register if it is rotateable.
	 *
	 * @param direction The direction to rotate.
	 * @param g The current graphics context for use in recalculating size.
	 */
	public void rotate(JLSInfo.Orientation direction, java.awt.Graphics g) {

		if (direction == JLSInfo.Orientation.LEFT) {
			amountOrientation = amountOrientation.ccw();
			outputOrientation = outputOrientation.ccw();
		}
		else if (direction == JLSInfo.Orientation.RIGHT) {
			amountOrientation = amountOrientation.cw();
			outputOrientation = outputOrientation.cw();
		}
		inputs.clear();
		outputs.clear();
		width = 0;
		height = 0;
		init(g);
	} // end of rotate method

	/**
	 * Tells if a shift register is capable of rotating, can only rotate
	 * when inputs or outputs have no attachments.
	 *
	 * @return False if any input or output has a wire attached, True otherwise
	 */
	public boolean canRotate() {

		for (Input i : inputs) {
			if (i.isAttached()) {
				return false;
			}
		}
		for (Output o : outputs) {
			if (o.isAttached()) {
				return false;
			}
		}
		return true;
	} // end of canRotate method

	/**
	 * Dialog box to set bits, shift kind and orientations.
	 */
	private class ShiftRegisterCreate extends ElementDialog
			implements java.awt.event.ActionListener {

		// properties
		private javax.swing.JTextField bitsField =
				new javax.swing.JTextField(defaultBits+"",5);
		private KeyPad bitsPad = new KeyPad(bitsField,10,defaultBits,this);
		private javax.swing.JRadioButton shiftLeft =
				new javax.swing.JRadioButton("Shift Left",true);
		private javax.swing.JRadioButton shiftRight =
				new javax.swing.JRadioButton("Shift Right");
		private javax.swing.JRadioButton shiftRightArith =
				new javax.swing.JRadioButton("Shift Right Arithmetic");
		private javax.swing.JRadioButton oLeft =
				new javax.swing.JRadioButton("Left");
		private javax.swing.JRadioButton oRight =
				new javax.swing.JRadioButton("Right", true);
		private javax.swing.JRadioButton oUp =
				new javax.swing.JRadioButton("Up");
		private javax.swing.JRadioButton oDown =
				new javax.swing.JRadioButton("Down");
		private javax.swing.JRadioButton sLeft =
				new javax.swing.JRadioButton("Left");
		private javax.swing.JRadioButton sRight =
				new javax.swing.JRadioButton("Right");
		private javax.swing.JRadioButton sUp =
				new javax.swing.JRadioButton("Up");
		private javax.swing.JRadioButton sDown =
				new javax.swing.JRadioButton("Down",true);
		private javax.swing.JLabel olbl2 =
				new javax.swing.JLabel("Amount Orientation");

		/**
		 * Set up dialog window.
		 */
		private ShiftRegisterCreate() {

			// set up window title
			super("Create Shift Register","shiftregister");

			// set not cancelled
			cancelled = false;

			// set up window
			java.awt.Container window = getContentPane();

			// set up input panel
			javax.swing.JPanel info =
					new javax.swing.JPanel(new java.awt.GridLayout(1,2));
			javax.swing.JLabel gates = new javax.swing.JLabel("Bits: ",
					javax.swing.SwingConstants.RIGHT);
			info.add(gates);
			javax.swing.JPanel ga =
					new javax.swing.JPanel(new java.awt.FlowLayout());
			ga.add(bitsField);
			ga.add(bitsPad);
			info.add(ga);
			window.add(info);

			// set up shift kind
			javax.swing.JLabel tlbl = new javax.swing.JLabel("Shift Type");
			tlbl.setAlignmentX(CENTER_ALIGNMENT);
			window.add(tlbl);
			javax.swing.JPanel types =
					new javax.swing.JPanel(new java.awt.GridLayout(3,1));
			javax.swing.ButtonGroup tgroup = new javax.swing.ButtonGroup();
			tgroup.add(shiftLeft);
			tgroup.add(shiftRight);
			tgroup.add(shiftRightArith);
			types.add(shiftLeft);
			types.add(shiftRight);
			types.add(shiftRightArith);
			javax.swing.JPanel containing = new javax.swing.JPanel();
			containing.add(types);
			containing.setAlignmentX(CENTER_ALIGNMENT);
			window.add(containing);

			// set up orientation radio buttons
			javax.swing.JPanel orient =
					new javax.swing.JPanel(new java.awt.GridLayout(3,3));
			javax.swing.JPanel orient2 =
					new javax.swing.JPanel(new java.awt.GridLayout(3,3));
			javax.swing.ButtonGroup gr = new javax.swing.ButtonGroup();
			javax.swing.ButtonGroup gr2 = new javax.swing.ButtonGroup();
			gr.add(oLeft);
			gr.add(oRight);
			gr.add(oDown);
			gr.add(oUp);
			gr2.add(sDown);
			gr2.add(sUp);
			gr2.add(sLeft);
			gr2.add(sRight);
			orient.add(new javax.swing.JLabel(""));
			orient.add(oUp);
			orient.add(new javax.swing.JLabel(""));
			orient.add(oLeft);
			orient.add(new javax.swing.JLabel(""));
			orient.add(oRight);
			orient.add(new javax.swing.JLabel(""));
			orient.add(oDown);
			orient.add(new javax.swing.JLabel(""));

			orient2.add(new javax.swing.JLabel(""));
			orient2.add(sUp);
			orient2.add(new javax.swing.JLabel(""));
			orient2.add(sLeft);
			orient2.add(new javax.swing.JLabel(""));
			orient2.add(sRight);
			orient2.add(new javax.swing.JLabel(""));
			orient2.add(sDown);
			orient2.add(new javax.swing.JLabel(""));

			javax.swing.JLabel olbl =
					new javax.swing.JLabel("Output Orientation");
			olbl.setAlignmentX(CENTER_ALIGNMENT);
			window.add(olbl);
			window.add(orient);

			olbl2.setAlignmentX(CENTER_ALIGNMENT);
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
		public void actionPerformed(java.awt.event.ActionEvent event) {

			if (event.getSource() == oLeft || event.getSource() == oRight) {
				olbl2.setVisible(true);
				sUp.setVisible(true);
				sDown.setVisible(true);
				sDown.setSelected(true);
				sLeft.setVisible(false);
				sRight.setVisible(false);
			}
			else if (event.getSource() == oUp || event.getSource() == oDown) {
				olbl2.setVisible(true);
				sLeft.setVisible(true);
				sLeft.setSelected(true);
				sRight.setVisible(true);
				sUp.setVisible(false);
				sDown.setVisible(false);
			}
		} // end of actionPerformed method

		/**
		 * Check the form against the element's parameter constraints
		 * (issue #52: the same rule strings the loader rejects with).
		 */
		protected java.util.List<Violation> validateInputs() {

			int newBits;
			try {
				newBits = Integer.parseInt(bitsField.getText());
			}
			catch (NumberFormatException ex) {
				return java.util.List.of(new Violation(
						"Value not numeric, try again", bitsField));
			}
			String violated = checkBits(newBits);
			if (violated != null) {
				return java.util.List.of(new Violation(violated, bitsField));
			}
			return java.util.List.of();
		} // end of validateInputs method

		/**
		 * Set the shift register parameters from the validated form.
		 */
		protected void validateAndAccept() {

			bits = Integer.parseInt(bitsField.getText());
			if (shiftRight.isSelected()) {
				type = ShiftRegister.Type.LogicalRight;
			}
			else if (shiftRightArith.isSelected()) {
				type = ShiftRegister.Type.ArithmeticRight;
			}
			else {
				type = ShiftRegister.Type.LogicalLeft;
			}
			if (oLeft.isSelected()) {
				outputOrientation = JLSInfo.Orientation.LEFT;
				if (sUp.isSelected()) {
					amountOrientation = JLSInfo.Orientation.UP;
				}
				else if (sDown.isSelected()) {
					amountOrientation = JLSInfo.Orientation.DOWN;
				}
			}
			else if (oRight.isSelected()) {
				outputOrientation = JLSInfo.Orientation.RIGHT;
				if (sUp.isSelected()) {
					amountOrientation = JLSInfo.Orientation.UP;
				}
				else if (sDown.isSelected()) {
					amountOrientation = JLSInfo.Orientation.DOWN;
				}
			}
			else if (oDown.isSelected()) {
				outputOrientation = JLSInfo.Orientation.DOWN;
				if (sLeft.isSelected()) {
					amountOrientation = JLSInfo.Orientation.LEFT;
				}
				else if (sRight.isSelected()) {
					amountOrientation = JLSInfo.Orientation.RIGHT;
				}
			}
			else if (oUp.isSelected()) {
				outputOrientation = JLSInfo.Orientation.UP;
				if (sLeft.isSelected()) {
					amountOrientation = JLSInfo.Orientation.LEFT;
				}
				else if (sRight.isSelected()) {
					amountOrientation = JLSInfo.Orientation.RIGHT;
				}
			}
			bitsPad.close();
			dispose();
		} // end of validateAndAccept method

		/**
		 * Cancel this shift register.
		 */
		protected void cancelDialog() {

			cancelled = true;
			bitsPad.close();
			dispose();
		} // end of cancelDialog method

	} // end of ShiftRegisterCreate class


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

			// get the amount to shift
			BitSet bw = inputs.get(0).getValue();
			if (bw == null)
				bw = new BitSet();
			int amount = BitSetUtils.ToInt(bw);

			// get the data input
			BitSet input = inputs.get(1).getValue();
			if (input == null)
				input = new BitSet();

			// shift
			BitSet newValue = new BitSet(bits);
			switch (type) {
			case LogicalLeft:
				for (int i = bits-1; i >= 0; i -= 1) {
					if (i-amount >= 0) {
						newValue.set(i,input.get(i-amount));
					}
				}
				break;
			case LogicalRight:
				for (int i = 0; i < bits; i += 1) {
					if (i+amount < bits) {
						newValue.set(i,input.get(i+amount));
					}
				}
				break;
			default: // ArithmeticRight
				for (int i = 0; i < bits; i += 1) {
					if (i+amount < bits) {
						newValue.set(i,input.get(i+amount));
					} else {
						// copy sign bit
						newValue.set(i,input.get(bits-1));
					}
				}
				break;
			}

			// if new value is different from the value propagating
			// through the shifter, then post an event
			if (!newValue.equals(toBeValue)) {
				toBeValue = (BitSet)newValue.clone();
				sim.post(new SimEvent(now+propDelay,this,newValue));
			}
		}
		else {

			// get the new output value
			BitSet value = (BitSet)todo;

			// send to output
			Output out = outputs.get(0);
			out.propagate(value,now,sim);
		}

	} // end of react method

} // end of ShiftRegister class
