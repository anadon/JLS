package jls.elem;

import jls.*;
import jls.sim.*;
import jls.util.Placement;

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
	
	// one constraint string, two surfaces: dialog and loader (issue #52);
	// non-positive times cause a zero-delay repost livelock at t=0
	static final String CYCLE_CONSTRAINT =
			"Cycle time must be a positive number of time units";
	static final String ONE_CONSTRAINT =
			"One time must be positive and less than the cycle time";

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
		new ClockCreate();
		
		// don't do anything if user cancelled gate
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
	 * @param g Unused.
	 */
	public void init(Graphics g) {

		// canonical geometry (RIGHT), transformed to the current
		// orientation (#24)
		int s = JLSInfo.spacing;
		width = 2*s;
		height = 2*s;
		GridTransform.Chain t = GridTransform.chain(2*s, 2*s);
		switch (orientation) {
		case RIGHT:
			break;
		case LEFT:
			t.mirrorX();
			break;
		case UP:
			t.rotateCCW();
			break;
		default: // DOWN
			t.rotateCW();
			break;
		}
		Point p = t.map(2*s, s);
		outputs.add(new Output("output",this,p.x,p.y,1));

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
		output.println("END");
	} // end of save method

	/**
	 * Get the cycle time (period) of this clock, in simulated time
	 * units (for consumers of the circuit model, e.g. the HDL exporter,
	 * issue #60).
	 *
	 * @return the cycle time.
	 */
	public int getCycleTime() {

		return cycleTime;
	} // end of getCycleTime method

	/**
	 * Get the number of time units per cycle this clock's output is 1.
	 *
	 * @return the one time.
	 */
	public int getOneTime() {

		return oneTime;
	} // end of getOneTime method

	// Declarative persistence (#23): one declaration drives save, load
	// dispatch, and copy for this element's own attributes.
	private static final java.util.List<Attribute> OWN_ATTRIBUTES =
			java.util.List.of(
		new Attribute.IntAttribute("cycle") {
			protected int get(Element el) { return ((Clock)el).cycleTime; }
			@Override
			protected void set(Element el, int v) {
				if (v < 1) {
					throw new IllegalArgumentException(CYCLE_CONSTRAINT);
				}
				((Clock)el).cycleTime = v;
			}
		},
		new Attribute.IntAttribute("one") {
			protected int get(Element el) { return ((Clock)el).oneTime; }
			@Override
			protected void set(Element el, int v) {
				if (v < 1) {
					throw new IllegalArgumentException(ONE_CONSTRAINT);
				}
				((Clock)el).oneTime = v;
			}
		},
		new Attribute.OrientationAttribute("orient") {
			protected JLSInfo.Orientation getOrientation(Element el) {
				return ((Clock)el).orientation;
			}
			protected void setOrientation(Element el, JLSInfo.Orientation o) {
				((Clock)el).orientation = o;
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
	 * Copy this element.
	 */
	public Element copy() {

		Clock it = new Clock(circuit);
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
			orientation = orientation.ccw();
		}
		else if(direction == JLSInfo.Orientation.RIGHT)
		{
			orientation = orientation.cw();
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
		orientation = orientation.flipped();
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
		new ClockCreate();
		
		if (!cancelled)
			circuit.markChanged();
		return false;
	
	} // end of change method
	
	/**
	 * Dialog box to set multi-input gate parameters (number of inputs, number of gates).
	 * Used by all simple gates (nand, and, nor, or, xor, not).
	 */
	@SuppressWarnings("serial")
	private class ClockCreate extends ElementDialog {

		// properties
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
		 */
		private ClockCreate() {
			
			// set up window title
			super("Create Clock","clock");

			// set not cancelled
			cancelled = false;

			// set up window
			Container window = getContentPane();

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

			confirmOnEnter(cycleTimeField);
			confirmOnEnter(oneTimeField);
			finishDialog();
		} // end of constructor

		/**
		 * Validate the form and set the clock parameters.
		 */
		protected void validateAndAccept() {

			// validate before mutating the element: a rejected dialog
			// must leave the clock unchanged (issue #52)
			int newCycleTime;
			int newOneTime;
			try {
				newCycleTime = Integer.parseInt(cycleTimeField.getText());
				newOneTime = Integer.parseInt(oneTimeField.getText());
			}
			catch (NumberFormatException ex) {
				reject("Value not numeric, try again");
				return;
			}
			if (newCycleTime < 1) {
				reject(CYCLE_CONSTRAINT);
				return;
			}
			if (newOneTime < 1 || newOneTime >= newCycleTime) {
				reject(ONE_CONSTRAINT);
				return;
			}
			cycleTime = newCycleTime;
			oneTime = newOneTime;
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
		} // end of validateAndAccept method

		/**
		 * Cancel this element.
		 */
		protected void cancelDialog() {

			cancelled = true;
			dispose();
		} // end of cancelDialog method

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
