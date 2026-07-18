package jls.elem;

import jls.*;
import jls.sim.*;
import jls.util.Placement;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.io.PrintWriter;
import java.util.BitSet;
import javax.swing.*;

import java.math.*;

/**
 * n-bit register (level or edge triggered).
 * 
 * @author David A. Poplawski
 */
public class Register extends LogicElement {

	// register types
	/**
	 * The triggering modes a register can have: Latch (level triggered),
	 * PosFF (positive-edge triggered), and NegFF (negative-edge triggered).
	 */
	private enum Type {Latch, PosFF, NegFF};
	
	// default values
	private static final String defaultName = "";
	private static final Register.Type defaultType = Type.Latch;
	private static final int defaultBits = 1;
	private static final BigInteger defaultInitValue = BigInteger.ZERO;
	private static final int defaultBase = 10;
	private static final int defaultPropDelay = 50; 
	
	// saved properties
	private String name = defaultName;
	private Type type = defaultType;
	private int bits = defaultBits;
	private BigInteger initialValue = defaultInitValue;
	private int base = defaultBase;
	private int propDelay = defaultPropDelay;
	private boolean watched = false;
	private JLSInfo.Orientation orientation = JLSInfo.Orientation.RIGHT;
	
	// running properties
	private boolean cancelled;
	private boolean nameChanged;
	
	/**
	 * Create a new register element.
	 * 
	 * @param circuit The circuit this element is part of.
	 */
	public Register(Circuit circuit) {
		
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
		new RegisterEdit(true);
		
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
	 * Figures out height and width using font info from graphics object.
	 * 
	 * @param g The Graphics object to use.
	 */
	@Override
	public void init(Graphics g) {
		
		// set up size if there is a graphics object
		if (g != null) {
			
			if (width == 0 && height == 0) {
				int s = JLSInfo.spacing;
				FontMetrics fm = g.getFontMetrics();
				int w = fm.stringWidth(" " + name + " ");
				width = Math.max((w+s/2)/s*s,2*s)+2*s;
				if(width % (2*s) != 0)
				{
					width += s;
				}
				if(orientation == JLSInfo.Orientation.LEFT || orientation == JLSInfo.Orientation.RIGHT)
				{
					height = 5*s;
				}
				else
				{
					height = 6*s;
				}
			}
			
		}
		
		
		int s = JLSInfo.spacing;
		if(orientation == JLSInfo.Orientation.RIGHT)
		{
			// create inputs
			inputs.add(new Input("D",this,0,s,bits));
			inputs.add(new Input("C",this,0,4*s,1));
		
			// create outputs
			outputs.add(new Output("Q",this,width,s,bits));
			outputs.add(new Output("notQ",this,width,4*s,bits));
		}
		else if(orientation == JLSInfo.Orientation.LEFT)
		{
			// create inputs
			inputs.add(new Input("D",this,width,s,bits));
			inputs.add(new Input("C",this,width,4*s,1));
		
			// create outputs
			outputs.add(new Output("Q",this,0,s,bits));
			outputs.add(new Output("notQ",this,0,4*s,bits));
		}
		else if(orientation == JLSInfo.Orientation.UP)
		{
			// create inputs
			inputs.add(new Input("D",this,s,height,bits));
			inputs.add(new Input("C",this,width-s,height,1));
			
			// create outputs
			outputs.add(new Output("Q",this,s,0,bits));
			outputs.add(new Output("notQ",this,width-s,0,bits));
		}
		else if(orientation == JLSInfo.Orientation.DOWN)
		{
			// create inputs
			inputs.add(new Input("D",this,s,0,bits));
			inputs.add(new Input("C",this,width-s,0,1));
			
			// create outputs
			outputs.add(new Output("Q",this,s,height,bits));
			outputs.add(new Output("notQ",this,width-s,height,bits));
		}
	} // end of init method
	
	/**
	 * Draw this gate.
	 * 
	 * @param g The graphics object to draw with.
	 */
	@Override
	public void draw(Graphics g) {
		
		// draw watched background
		int s = JLSInfo.spacing;
		if (watched) {
			
			g.setColor(JLSInfo.watchColor);
			if(orientation == JLSInfo.Orientation.RIGHT)
			{
				g.fillRect(x+s,y,width-s,height);
			}
			else if(orientation == JLSInfo.Orientation.LEFT)
			{
				g.fillRect(x,y,width-s,height);
			}
			else if(orientation == JLSInfo.Orientation.UP)
			{
				g.fillRect(x,y,width,height-s);
			}
			else if(orientation == JLSInfo.Orientation.DOWN)
			{
				g.fillRect(x,y+s,width,height-s);
			}
		}
		
		// draw context
		super.draw(g);
		
		// draw box
		g.setColor(Color.BLACK);
		
		if(orientation == JLSInfo.Orientation.RIGHT)
		{
		
			g.drawRect(x+s,y,width-s,5*s);
			
			// draw name inside box
			FontMetrics fm = g.getFontMetrics();
			Rectangle2D t = fm.getStringBounds(name,g);
			double tw = t.getWidth();
			double th = t.getHeight();
			int dx = (int)((width-s-tw)/2)+s;
			int dy = (int)((5*s-th)/2+fm.getAscent());
			g.drawString(name,x+dx,y+dy);
			
			// draw D input, line to it and label
			Input one = inputs.get(0);
			int lx = one.getX();
			int ly = one.getY();
			g.setColor(Color.black);
			g.drawLine(lx,ly,lx+s,ly);
			int h = fm.getAscent()+fm.getDescent();
			g.drawString("D",lx+s+1,ly-h/2+fm.getAscent());
			one.draw(g);
		
			// draw C input, line to it, label and type
			Input two = inputs.get(1);
			lx = two.getX();
			ly = two.getY();
			int d = JLSInfo.pointDiameter;
			g.setColor(Color.black);
			switch (type) {
			case Latch:
				g.drawLine(lx,ly,lx+s,ly);
				g.drawString("C",lx+s+1,ly-h/2+fm.getAscent());
				break;
			case PosFF:
				g.drawLine(lx,ly,lx+s,ly);
				g.drawLine(lx+s,ly-d,lx+s+d,ly);
				g.drawLine(lx+s,ly+d,lx+s+d,ly);
				g.drawString("C",lx+s+d+1,ly-h/2+fm.getAscent());
				break;
			case NegFF:
				g.drawLine(lx,ly,lx+s-d,ly);
				g.drawOval(lx+s-d,ly-d/2,d,d);
				g.drawLine(lx+s,ly-d,lx+s+d,ly);
				g.drawLine(lx+s,ly+d,lx+s+d,ly);
				g.drawString("C",lx+s+d+1,ly-h/2+fm.getAscent());
				break;
			}
			two.draw(g);
		
			// draw Q output and label
			Output three = outputs.get(0);
			lx = three.getX();
			ly = three.getY();
			g.setColor(Color.black);
			g.drawString("Q",lx-fm.stringWidth("Q")-1,ly-h/2+fm.getAscent());
			three.draw(g);
		
			// draw notQ output and label
			Output four = outputs.get(1);
			lx = four.getX();
			ly = four.getY();
			g.setColor(Color.black);
			g.drawString("Q",lx-fm.stringWidth("Q")-1,ly-h/2+fm.getAscent());
			g.drawLine(lx-fm.stringWidth("Q")-1,ly-h/2,lx-2,ly-h/2);
			four.draw(g);
		}
		else if(orientation == JLSInfo.Orientation.LEFT)
		{
			g.drawRect(x,y,width-s,5*s);
			
			// draw name inside box
			FontMetrics fm = g.getFontMetrics();
			Rectangle2D t = fm.getStringBounds(name,g);
			double tw = t.getWidth();
			double th = t.getHeight();
			int dx = (int)((width-s-tw)/2);
			int dy = (int)((5*s-th)/2+fm.getAscent());
			g.drawString(name,x+dx,y+dy);
			
			// draw D input, line to it and label
			Input one = inputs.get(0);
			int lx = one.getX();
			int ly = one.getY();
			g.setColor(Color.black);
			g.drawLine(lx,ly,lx-s,ly);
			int h = fm.getAscent()+fm.getDescent();
			g.drawString("D",lx-s-10,ly-h/2+fm.getAscent());
			one.draw(g);
			
			// draw C input, line to it, label and type
			Input two = inputs.get(1);
			lx = two.getX();
			ly = two.getY();
			int d = JLSInfo.pointDiameter;
			g.setColor(Color.black);
			switch (type) {
			case Latch:
				g.drawLine(lx,ly,lx-s,ly);
				g.drawString("C",lx-s-10,ly-h/2+fm.getAscent());
				break;
			case PosFF:
				g.drawLine(lx,ly,lx-s,ly);
				g.drawLine(lx-s,ly-d,lx-s-d,ly);
				g.drawLine(lx-s,ly+d,lx-s-d,ly);
				g.drawString("C",lx-s-d-9,ly-h/2+fm.getAscent());
				break;
			case NegFF:
				g.drawLine(lx,ly,lx-s+d,ly);
				g.drawOval(lx-s,ly-d/2,d,d);
				g.drawLine(lx-s,ly-d,lx-s-d,ly);
				g.drawLine(lx-s,ly+d,lx-s-d,ly);
				g.drawString("C",lx-s-d-9,ly-h/2+fm.getAscent());
				break;
			}
			two.draw(g);
			
			// draw Q output and label
			Output three = outputs.get(0);
			lx = three.getX();
			ly = three.getY();
			g.setColor(Color.black);
			g.drawString("Q",lx+3,ly-h/2+fm.getAscent());
			three.draw(g);
			
			// draw notQ output and label
			Output four = outputs.get(1);
			lx = four.getX();
			ly = four.getY();
			g.setColor(Color.black);
			g.drawString("Q",lx+3,ly-h/2+fm.getAscent());
			g.drawLine(lx+fm.stringWidth("Q")+2,ly-h/2,lx+2,ly-h/2);
			four.draw(g);
		}
		else if(orientation == JLSInfo.Orientation.UP)
		{
			g.drawRect(x,y,width,height-s);
			
			// draw name inside box
			FontMetrics fm = g.getFontMetrics();
			Rectangle2D t = fm.getStringBounds(name,g);
			double tw = t.getWidth();
			double th = t.getHeight();
			int dx = (int)((width-tw)/2);
			int dy = (int)((5*s-th)/2+fm.getAscent());
			g.drawString(name,x+dx,y+dy);
			
			// draw D input, line to it and label
			Input one = inputs.get(0);
			int lx = one.getX();
			int ly = one.getY();
			g.setColor(Color.black);
			g.drawLine(lx,ly,lx,ly-s);
			int h = fm.getAscent()+fm.getDescent();
			g.drawString("D",lx-fm.stringWidth("D")/2,ly-s-h+fm.getAscent());
			one.draw(g);
			
			// draw C input, line to it, label and type
			Input two = inputs.get(1);
			lx = two.getX();
			ly = two.getY();
			int d = JLSInfo.pointDiameter;
			g.setColor(Color.black);
			switch (type) {
			case Latch:
				g.drawLine(lx,ly-s,lx,ly);
				g.drawString("C",lx-fm.stringWidth("C")/2,ly-s-h+fm.getAscent());
				break;
			case PosFF:
				g.drawLine(lx,ly-s,lx,ly);
				g.drawLine(lx-d,ly-s,lx,ly-s-d);
				g.drawLine(lx+d,ly-s,lx,ly-s-d);
				g.drawString("C",lx-fm.stringWidth("C")/2,ly-s-d-h+fm.getAscent());
				break;
			case NegFF:
				g.drawLine(lx,ly-s+d,lx,ly);
				g.drawOval(lx-d/2, ly-s, d, d);
				g.drawLine(lx-d,ly-s,lx,ly-s-d);
				g.drawLine(lx+d,ly-s,lx,ly-s-d);
				g.drawString("C",lx-fm.stringWidth("C")/2,ly-s-d-h+fm.getAscent());
				break;
			}
			two.draw(g);
			
			// draw Q output and label
			Output three = outputs.get(0);
			lx = three.getX();
			ly = three.getY();
			g.setColor(Color.black);
			g.drawString("Q",lx-fm.stringWidth("Q")/2,ly+d+fm.getAscent());
			three.draw(g);
			
			// draw notQ output and label
			Output four = outputs.get(1);
			lx = four.getX();
			ly = four.getY();
			g.setColor(Color.black);
			g.drawString("Q",lx-fm.stringWidth("Q")/2,ly+d+fm.getAscent());
			g.drawLine(lx-fm.stringWidth("Q")/2,ly+d-1,lx+fm.stringWidth("Q")/2,ly+d-1);
			four.draw(g);

		}
		else if(orientation == JLSInfo.Orientation.DOWN)
		{
			g.drawRect(x,y+s,width,height-s);
			
			// draw name inside box
			FontMetrics fm = g.getFontMetrics();
			Rectangle2D t = fm.getStringBounds(name,g);
			double tw = t.getWidth();
			double th = t.getHeight();
			int dx = (int)((width-tw)/2);
			int dy = (int)((5*s-th)/2+fm.getAscent());
			g.drawString(name,x+dx,y+s+dy);
			
			// draw D input, line to it and label
			Input one = inputs.get(0);
			int lx = one.getX();
			int ly = one.getY();
			g.setColor(Color.black);
			g.drawLine(lx,ly+s,lx,ly);
			int h = fm.getAscent()+fm.getDescent();
			g.drawString("D",lx-fm.stringWidth("D")/2,ly+s+1+fm.getAscent());
			one.draw(g);
			
			// draw C input, line to it, label and type
			Input two = inputs.get(1);
			lx = two.getX();
			ly = two.getY();
			int d = JLSInfo.pointDiameter;
			g.setColor(Color.black);
			switch (type) {
			case Latch:
				g.drawLine(lx,ly+s,lx,ly);
				g.drawString("C",lx-fm.stringWidth("C")/2,ly+s+1+fm.getAscent());
				break;
			case PosFF:
				g.drawLine(lx,ly+s,lx,ly);
				g.drawLine(lx-d,ly+s,lx,ly+s+d);
				g.drawLine(lx+d,ly+s,lx,ly+s+d);
				g.drawString("C",lx-fm.stringWidth("C")/2,ly+s+d+fm.getAscent());
				break;
			case NegFF:
				g.drawLine(lx,ly+s-d,lx,ly);
				g.drawOval(lx-d/2,ly+s-d,d,d);
				g.drawLine(lx-d,ly+s,lx,ly+s+d);
				g.drawLine(lx+d,ly+s,lx,ly+s+d);
				g.drawString("C",lx-fm.stringWidth("C")/2,ly+s+d+fm.getAscent());
				break;
			}
			two.draw(g);
			
			// draw Q output and label
			Output three = outputs.get(0);
			lx = three.getX();
			ly = three.getY();
			g.setColor(Color.black);
			g.drawString("Q",lx-fm.stringWidth("Q")/2,ly-h+fm.getAscent());
			three.draw(g);
			
			// draw notQ output and label
			Output four = outputs.get(1);
			lx = four.getX();
			ly = four.getY();
			g.setColor(Color.black);
			g.drawString("Q",lx-fm.stringWidth("Q")/2,ly-h+fm.getAscent());
			g.drawLine(lx-fm.stringWidth("Q")/2,ly-h,lx+fm.stringWidth("Q")/2,ly-h);
			four.draw(g);
			
		}
		
	} // end of draw method
	
	// Declarative persistence (#23): one declaration drives save, load
	// dispatch, and copy for this element's own attributes.
	private static final java.util.List<Attribute> OWN_ATTRIBUTES =
			java.util.List.of(
		new Attribute.StringAttribute("name") {
			/**
			 * Read the register's name for saving.
			 */
			@Override
			protected String get(Element el) { return ((Register)el).name; }
			/**
			 * Load the register's name and register it with the circuit.
			 */
			@Override
			protected void set(Element el, String v) {
				// loading a name registers it with the circuit
				((Register)el).name = v;
				el.getCircuit().addName(v);
			}
			/**
			 * Copy the register's name field without re-registering it.
			 */
			@Override
			public void copy(Element from, Element to) {
				// the handwritten copy assigned the field without
				// registering the name
				((Register)to).name = ((Register)from).name;
			}
		},
		new Attribute.IntAttribute("bits") {
			/**
			 * Read the register's bit width for saving.
			 */
			@Override
			protected int get(Element el) { return ((Register)el).bits; }
			/**
			 * Load the register's bit width.
			 */
			@Override
			protected void set(Element el, int v) { ((Register)el).bits = v; }
		},
		new Attribute.BigIntAttribute("init") {
			/**
			 * Read the register's initial value for saving.
			 */
			@Override
			protected BigInteger get(Element el) {
				return ((Register)el).initialValue;
			}
			/**
			 * Load the initial value and reset the displayed current value.
			 */
			@Override
			protected void set(Element el, BigInteger v) {
				// the handwritten loader (and copy) also reset the
				// displayed current value
				Register reg = (Register)el;
				reg.initialValue = v.add(BigInteger.ZERO);
				reg.currentValue = BitSetUtils.Create(v);
			}
		},
		new Attribute.OrientationAttribute("orient") {
			/**
			 * Read the register's orientation for saving.
			 */
			@Override
			protected JLSInfo.Orientation getOrientation(Element el) {
				return ((Register)el).orientation;
			}
			/**
			 * Load the register's orientation.
			 */
			@Override
			protected void setOrientation(Element el, JLSInfo.Orientation o) {
				((Register)el).orientation = o;
			}
		},
		new Attribute.IntAttribute("delay") {
			/**
			 * Read the register's propagation delay for saving.
			 */
			@Override
			protected int get(Element el) { return ((Register)el).propDelay; }
			/**
			 * Load the register's propagation delay.
			 */
			@Override
			protected void set(Element el, int v) { ((Register)el).propDelay = v; }
		},
		new Attribute.StringAttribute("type") {
			/**
			 * Read the register's type as its save-file name for saving.
			 */
			@Override
			protected String get(Element el) {
				switch (((Register)el).type) {
				case PosFF: return "pff";
				case NegFF: return "nff";
				default: return "latch";
				}
			}
			/**
			 * Load the register's type from its save-file name.
			 */
			@Override
			protected void set(Element el, String v) {
				// unknown strings leave the type unchanged, as the
				// handwritten loader did
				if (v.equals("latch"))
					((Register)el).type = Type.Latch;
				else if (v.equals("pff"))
					((Register)el).type = Type.PosFF;
				else if (v.equals("nff"))
					((Register)el).type = Type.NegFF;
			}
		},
		new Attribute.IntAttribute("watch") {
			/**
			 * Read the register's watched flag (1/0) for saving.
			 */
			@Override
			protected int get(Element el) { return ((Register)el).watched ? 1 : 0; }
			/**
			 * Load the register's watched flag.
			 */
			@Override
			protected void set(Element el, int v) { ((Register)el).watched = v != 0; }
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

		output.println("ELEMENT Register");
		super.save(output);
		output.println("END");
	} // end of save method

	/**
	 * Can't copy registers ('cause they have names).
	 *
	 * @return false;
	 */
	public boolean canCopy() {

		return false;
	} // end of canCopy method

	/**
	 * Copy this element.
	 */
	@Override
	public Element copy() {

		Register it = new Register(circuit);
		it.base = base;	// display radix is not a saved attribute
		it.inputs.add(inputs.get(0).copy(it));
		it.inputs.add(inputs.get(1).copy(it));
		it.outputs.add(outputs.get(0).copy(it));
		it.outputs.add(outputs.get(1).copy(it));
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
		
		String ty = "";
		switch (type) {
		case Latch: ty = "latch"; break;
		case PosFF: ty = "positive edge triggered flip-flop"; break;
		case NegFF: ty = "negative edge triggered flip-flop"; break;
		}
		info.setText(bits + " bit " + ty + ", value = " +
				BitSetUtils.toDisplay(currentValue,bits));
	} // end of showInfo method
	
	/**
	 * Get the name of this register.
	 * 
	 * @return the name.
	 */
	@Override
	public String getName() {
		
		return name;
	} // end of getName method
	
	/**
	 * Get the number of bits in this register.
	 * 
	 * @return the number of bits.
	 */ 
	@Override
	public int getBits() {
		
		return bits;
	} // end of getBits method

	/**
	 * Remove name from list of element names in this circuit.
	 * 
	 * @param circ A reference back to the circuit the element is in.
	 */
	@Override
	public void remove(Circuit circ) {
		
		circ.removeName(name);
		super.remove(circ);
	} // end of remove method

	/**
	 * Registers have timing info (propagation delay).
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
	 * Set the initial value of this register.
	 *
	 * @param value The initial value.
	 */
	public void setInitialValue(BigInteger value) {

		initialValue = value.add(BigInteger.ZERO);
	} // end of setInitialValue method

	/**
	 * Get the initial value of this register (for consumers of the
	 * circuit model, e.g. the HDL exporter, issue #60).
	 *
	 * @return the initial value.
	 */
	public BigInteger getInitialValue() {

		return initialValue;
	} // end of getInitialValue method

	/**
	 * The register type as its save-file name: "latch" (transparent
	 * latch), "pff" (positive-edge flip-flop) or "nff" (negative-edge
	 * flip-flop). The Type enum itself stays private; this mirrors the
	 * "type" attribute the file format already pins (issue #60).
	 *
	 * @return the type name.
	 */
	public String getTypeName() {

		switch (type) {
		case PosFF: return "pff";
		case NegFF: return "nff";
		default: return "latch";
		}
	} // end of getTypeName method
	
	/**
	 * Tells if a register is capable of rotatating, can only rotate when inputs or outputs have no attachments.
	 * @return False if any input or output has a wire attached, True otherwise
	 */
	@Override
	public boolean canRotate() {
		
		for(Input i : inputs) {
			if(i.isAttached()) {
				return false;
			}
		}
		for(Output o : outputs) {
			if(o.isAttached()) {
				return false;
			}
		}
		return true;
	} // end of canRotate method
	
	/**
	 * This method will rotate the register if it is rotateable.
	 * 
	 * @param direction The direction to rotate
	 * @param g The current graphics context for use in recalculating size
	 */
	@Override
	public void rotate(JLSInfo.Orientation direction, Graphics g) {
		
		if(direction == JLSInfo.Orientation.LEFT) {
			orientation = orientation.ccw();
		}
		else if(direction == JLSInfo.Orientation.RIGHT) {
			orientation = orientation.cw();
		}
		inputs.clear();
		outputs.clear();
		width = 0;
		height = 0;
		init(g);
	} // end of rotate method
	
	/**
	 * Tells if a register is capable of flipping, can only flip when inputs or outputs have no attachments.
	 * @return False if any input or output has a wire attached, True otherwise
	 */
	@Override
	public boolean canFlip() {
		
		for(Input i : inputs) {
			if(i.isAttached()) {
				return false;
			}
		}
		for(Output o : outputs) {
			if(o.isAttached()) {
				return false;
			}
		}
		return true;
	} // end of canFlip method
	
	/**
	 * This method will flip a register
	 * 
	 * @param g The current graphics context to facilitate recalculation of size when flipping
	 */
	@Override
	public void flip(Graphics g) {
		
		orientation = orientation.flipped();
		inputs.clear();
		outputs.clear();
		width = 0;
		height = 0;
		init(g);
	} // end of flip method

	/**
	 * Dialog box to create/modify register characteristics.
	 */
	private class RegisterEdit extends ElementDialog implements ActionListener {

		// properties
		private JTextField nameField = new JTextField(name);
		private JTextField bitsField = new JTextField(bits+"");
		private JTextField valueField = new JTextField("");
		private KeyPad bitsPad = new KeyPad(bitsField,10,bits,this);
		private KeyPad valuePad = new KeyPad(valueField,16,initialValue.longValue(),this);
		private JRadioButton base2 = new JRadioButton("2");
		private JRadioButton base10 = new JRadioButton("10");
		private JRadioButton base16 = new JRadioButton("16");
		private JRadioButton latch = new JRadioButton("Level-Trig");
		private JRadioButton posFF = new JRadioButton("Pos-Trig");
		private JRadioButton negFF = new JRadioButton("Neg-Trig");
		private JRadioButton left = new JRadioButton("Left");
		private JRadioButton right = new JRadioButton("Right");
		private JRadioButton up = new JRadioButton("Up");
		private JRadioButton down = new JRadioButton("Down");
		private boolean creating;
		
		/**
		 * Set up dialog window.
		 * 
		 * @param creating True if creating, false if modifying.
		 */
		private RegisterEdit(boolean creating) {
			
			// set up window title
			super("Create Register","register");

			// set not cancelled
			cancelled = false;

			// save create/modify
			this.creating = creating;

			// set up window
			Container window = getContentPane();

			// set up types
			JLabel tlbl = new JLabel("Trigger");
			tlbl.setAlignmentX(Component.CENTER_ALIGNMENT);
			window.add(tlbl);
			JPanel types = new JPanel(new GridLayout(1,3));
			types.add(latch);
			latch.setToolTipText("Level-triggered");
			types.add(posFF);
			posFF.setToolTipText("{positive,leading,rising}-edge-triggered");
			types.add(negFF);
			negFF.setToolTipText("{negative,trailing,falling}-edge-triggered");
			switch (type) {
			case Latch:
				latch.setSelected(true);
				break;
			case PosFF:
				posFF.setSelected(true);
				break;
			case NegFF:
				negFF.setSelected(true);
				break;
			}
			latch.setHorizontalAlignment(SwingConstants.CENTER);
			posFF.setHorizontalAlignment(SwingConstants.CENTER);
			negFF.setHorizontalAlignment(SwingConstants.CENTER);
			ButtonGroup tgroup = new ButtonGroup();
			tgroup.add(latch);
			tgroup.add(posFF);
			tgroup.add(negFF);
			window.add(types);
			
			// set up inputs
			int rows = 2;
			if (creating) {
				rows = 3;
			}
			window.add(new JLabel(" "));
			JPanel info = new JPanel(new BorderLayout());
			JPanel labels = new JPanel(new GridLayout(rows,1,1,5));
			JLabel name = new JLabel("Name: ",SwingConstants.RIGHT);
			labels.add(name);
			if (creating) {
				JLabel bits = new JLabel("Bits: ",SwingConstants.RIGHT);
				labels.add(bits);
			}
			JLabel init = new JLabel("Initial value: ",SwingConstants.RIGHT);
			labels.add(init);
			info.add(labels,BorderLayout.WEST);
			
			JPanel fields = new JPanel(new GridLayout(rows,1,1,5));
			fields.add(nameField);
			if (creating) {
				JPanel b = new JPanel(new BorderLayout());
				b.add(bitsField,BorderLayout.CENTER);
				b.add(bitsPad,BorderLayout.EAST);
				fields.add(b);
			}
			JPanel i = new JPanel(new BorderLayout());
			valueField.setText(Util.convert(initialValue,base,false));
			i.add(valueField,BorderLayout.CENTER);
			i.add(valuePad,BorderLayout.EAST);
			valuePad.setBase(base);
			fields.add(i);
			info.add(fields,BorderLayout.CENTER);
			window.add(info);
			
			// set up radix selection
			window.add(new JLabel(" "));
			JLabel rlbl = new JLabel("Radix");
			rlbl.setAlignmentX(Component.CENTER_ALIGNMENT);
			window.add(rlbl);
			JPanel bases = new JPanel(new GridLayout(1,3));
			bases.add(base2);
			bases.add(base10);
			bases.add(base16);
			base2.setHorizontalAlignment(SwingConstants.CENTER);
			base10.setHorizontalAlignment(SwingConstants.CENTER);
			base16.setHorizontalAlignment(SwingConstants.CENTER);
			ButtonGroup group = new ButtonGroup();
			group.add(base2);
			group.add(base10);
			group.add(base16);
			switch (base) {
			case 2:
				base2.setSelected(true);
				break;
			case 10:
				base10.setSelected(true);
				break;
			case 16:
				base16.setSelected(true);
				break;
			}
			window.add(bases);
			
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
			switch(orientation) {
			case LEFT:
				left.setSelected(true);
				break;
			case RIGHT:
				right.setSelected(true);
				break;
			case UP:
				up.setSelected(true);
				break;
			case DOWN:
				down.setSelected(true);
				break;
			}
			window.add(orients);

			base2.addActionListener(this);
			base10.addActionListener(this);
			base16.addActionListener(this);

			confirmOnEnter(nameField);
			confirmOnEnter(bitsField);
			confirmOnEnter(valueField);
			finishDialog();
		} // end of constructor

		/**
		 * Validate the form and apply it to the register.
		 */
		@Override
		protected void validateAndAccept() {

			String tname = nameField.getText().trim();
			if (tname.isEmpty() || !Util.isValidName(tname)) {
				reject("Missing or invalid name");
				return;
			}
			int tbits = bits;
			BigInteger tinitialValue = BigInteger.ZERO;
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
			try {
				if (creating) {
					tbits = Integer.parseInt(bitsField.getText());
				}
				tinitialValue = new BigInteger(valueField.getText(),base);
			}
			catch (NumberFormatException ex) {
				reject("Value not numeric");
				return;
			}
			if (tbits < 1) {
				reject("Must have at least 1 bit");
				return;
			}
			if (tinitialValue.bitLength() > tbits) {
				reject("Value too large for number of bits");
				return;
			}
			if (!creating) {
				circuit.removeName(name);
			}
			if (!circuit.addName(tname)) {
				reject("Duplicate name");
				return;
			}
			name = tname;
			bits = tbits;
			initialValue = tinitialValue.add(BigInteger.ZERO);
			currentValue = BitSetUtils.Create(initialValue);
			if (latch.isSelected())
				type = Register.Type.Latch;
			else if (negFF.isSelected())
				type = Register.Type.NegFF;
			else
				type = Register.Type.PosFF;
			bitsPad.close();
			valuePad.close();
			circuit.markChanged();
			if (!creating && !valueFits(tname)) {
				nameChanged = true;
			}
			else {
				nameChanged = false;
			}
			dispose();
		} // end of validateAndAccept method

		/**
		 * React to radix buttons.
		 *
		 * @param event The event object for this action.
		 */
		@Override
		public void actionPerformed(ActionEvent event) {

			if (event.getSource() == base2) {
				BigInteger val = BigInteger.ZERO;
				if (!valueField.getText().isEmpty())
					val = new BigInteger(valueField.getText(),base);
				base = 2;
				valuePad.setBase(2);
				valueField.setText(val.toString(2));
			}
			else if (event.getSource() == base10) {
				BigInteger val = BigInteger.ZERO;
				if (!valueField.getText().isEmpty())
					val = new BigInteger(valueField.getText(),base);
				base = 10;
				valuePad.setBase(10);
				valueField.setText(val.toString(10));
			}
			else if (event.getSource() == base16) {
				BigInteger val = BigInteger.ZERO;
				if (!valueField.getText().isEmpty())
					val = new BigInteger(valueField.getText(),base);
				base = 16;
				valuePad.setBase(16);
				valueField.setText(val.toString(16));
			}
		} // end of actionPerformed method

		/**
		 * Cancel this gate.
		 */
		@Override
		protected void cancelDialog() {

			cancelled = true;
			dispose();
		} // end of cancelDialog method

	} // end of RegisterEdit class
	
	/**
	 * Registers can be modified.
	 * 
	 * @return true.
	 */ 
	@Override
	public boolean canChange() {
		
		return true;
	} // end of canChange method
	
	private Graphics saveg;

	/**
	 * Show change dialog.
	 * 
	 * @param g The Graphics object to use to determine size.
	 * @param editWindow The editor window this element is in.
	 * @param x The current x-coordinate of the mouse.
	 * @param y The current y-coordinate of the mouse.
	 * 
	 * @return true if the name has grown, false if not.
	 */
	@Override
	public boolean change(Graphics g, JPanel editWindow, int x, int y) {
	
		// save g for valueFits method
		saveg = g;
		
		// display dialog
		new RegisterEdit(false);
		
		// if element got bigger, detach and make user re-position
		if (nameChanged) {
			detach();
			width = 0;
			height = 0;
			init(g);
			return true;
		}
		
		return false;
	
	} // end of change method
	
	/**
	 * See if the given value fits in the box on the screen.
	 * 
	 * @param value The value to check.
	 * 
	 * @return true if it fits, false if not.
	 */
	public boolean valueFits(String value) {
		
		int s = JLSInfo.spacing;
		FontMetrics fm = saveg.getFontMetrics();
		int w = fm.stringWidth(value)+s;
		int rw = Math.max((w+s/2)/s*s,2*s);
		return rw <= (width-s);
		
	} // end of valueFits method
	
	/**
	 * A register can be watched.
	 * 
	 * @return true.
	 */
	@Override
	public boolean canWatch() {
		
		return true;
	} // end of canWatch method
	
	/**
	 * See if this register is watched.
	 * 
	 * @return true if it is, false if it is not.
	 */
	@Override
	public boolean isWatched() {
		
		return watched;
	} // end of isWatched method
	
	/**
	 * Set whether this register is watched or not.
	 * 
	 * @param state True to make it watched, false to make it not watched.
	 */
	@Override
	public void setWatched(boolean state) {
		
		watched = state;
	} // end of setWatched method
	
	/**
	 * Print current value to stdout.
	 * 
	 * @param prefix qualified name.
	 */
	public void printValue(String prefix) {
		
		if (prefix.isEmpty()) {
			System.out.printf("Register %s: %s\n", name, BitSetUtils.toDisplay(currentValue,bits));
		}
		else {
			System.out.printf("Register %s.%s: %s\n", prefix, name, BitSetUtils.toDisplay(currentValue,bits));
		}
		
	} // end of printValue method
	
//	-------------------------------------------------------------------------------
//	Simulation
//	-------------------------------------------------------------------------------
	
	private BitSet toBeValue;
	private BitSet currentValue = new BitSet();
	private int currentC;
	
	/**
	 * Get the current value stored in this register.
	 * 
	 * @return the current value.
	 * @see jls.SimulationSemanticsRegressionTest#registerInitSimResetsTheWatchedCurrentValue()
	 */
	@Override
	public BitSet getCurrentValue() {
		
		if (currentValue == null)
			return null;
		else
			return (BitSet)currentValue.clone();
	} // end of getCurrentValue method
	
	/**
	 * Initialize this element by setting its output pin and to-be value to 0.
	 * 
	 * @param sim Unused.
	 * @see jls.SimulationSemanticsRegressionTest#registerInitSimResetsTheWatchedCurrentValue()
	 */
	@Override
	public void initSim(Simulator sim) {

		// set current value and to-be value to the initial value
		// (assign the field, not a shadowing local - issue #98, S2)
		currentValue = BitSetUtils.Create(initialValue);
		toBeValue = (BitSet)currentValue.clone();
		currentC = 0;

		// set output pins to 0
		Output q = outputs.get(0);
		q.setValue(new BitSet(1));
		Output notq = outputs.get(1);
		notq.setValue(new BitSet(1));

		// post output event at time 0 to drive the initial value
		sim.post(new SimEvent(0,this,currentValue.clone()));

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
		
		// if an input has changed ...
		if (todo == null) {
			
			BitSet inVal = inputs.get(1).getValue();
			if (inVal == null)
				inVal = new BitSet();
			int c = (int)(BitSetUtils.ToLong(inVal));
			BitSet d = inputs.get(0).getValue();
			if (d == null)
				d = new BitSet();
			switch (type) {
			case Latch:
				if (c == 0)
					break;
				if (d.equals(toBeValue))
					break;
				toBeValue = (BitSet)d.clone();
				sim.post(new SimEvent(now+propDelay,this,d.clone()));
				break;
			case PosFF:
				if (currentC == 1)
					break;
				if (c == 0)
					break;
				if (d.equals(toBeValue))
					break;
				toBeValue = (BitSet)d.clone();
				sim.post(new SimEvent(now+propDelay,this,d.clone()));
				break;
			case NegFF:
				if (currentC == 0)
					break;
				if (c == 1)
					break;
				if (d.equals(toBeValue))
					break;
				toBeValue = (BitSet)d.clone();
				sim.post(new SimEvent(now+propDelay,this,d.clone()));
				break;
			}
			currentC = c;
		}
		else {
			
			// get the new output value
			BitSet newQ = (BitSet)todo;
			
			// save for watch
			currentValue = (BitSet)newQ.clone();
			
			// make copy and complement
			BitSet qOut = (BitSet)newQ.clone();
			BitSet notQOut = (BitSet)newQ.clone();
			notQOut.flip(0,bits);
			
			// send to outputs
			Output q = outputs.get(0);
			q.propagate(qOut,now,sim);
			Output notq = outputs.get(1);
			notq.propagate(notQOut,now,sim);
		}
		
	} // end of react method
	
	/**
	 * Display current value.
	 * 
	 * @param where Unused.
	 */
	@Override
	public void showCurrentValue(Point where) {
		
		String hex = BitSetUtils.ToString(currentValue,16);
		String unsigned = BitSetUtils.ToString(currentValue,10);
		String signed = BitSetUtils.ToStringSigned(currentValue,bits);
		String value = "0x" +hex + " (" + unsigned + " unsigned, " + signed + " signed)";
		TellUser.info(JLSInfo.frame, value, "Information");
	} // end of showCurrentValue method
	
} // end of Register class
