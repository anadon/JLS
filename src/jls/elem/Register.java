package jls.elem;

import jls.*;
import jls.sim.*;
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
	public boolean setup(Graphics g, JPanel editWindow, int x, int y) {
		
		// show creation dialog
		Point pos = editWindow.getMousePosition();
		Point win = editWindow.getLocationOnScreen();
		if (pos == null) {
			new RegisterEdit(x+win.x,y+win.y,true);
		}
		else {
			new RegisterEdit(pos.x+win.x,pos.y+win.y,true);
		}
		
		// don't do anything if user cancelled gate
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
	 * Initialize internal info for this element.
	 * Figures out height and width using font info from graphics object.
	 * 
	 * @param g The Graphics object to use.
	 */
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
	
	/**
	 * Set an int instance variable value (during a load).
	 * 
	 * @param name The instance variable name.
	 * @param value The instance variable value.
	 */
	public void setValue(String name, int value) {
		
		if (name.equals("bits")) {
			bits = value;
		} else if (name.equals("delay")) {
			propDelay = value;
		} else if (name.equals("watch")) {
			if (value == 0)
				watched = false;
			else
				watched = true;
		} else {
			super.setValue(name,value);
		}
	} // end of setValue method

	/**
	 * Set a long instance variable value (during a load).
	 * This is here for compatibility with early version of JLS that saved the initial
	 * value as a long instead of a BigInteger.
	 * 
	 * @param name The instance variable name.
	 * @param value The instance variable value.
	 */
	public void setValue(String name, long value) {
		
		if (name.equals("init")) {
			initialValue = BigInteger.valueOf(value);
			currentValue = BitSetUtils.Create(value);
		} else {
			super.setValue(name,value);
		}
	} // end of setValue method
	
	/**
	 * Set a BigInteger instance variable value (during a load).
	 * 
	 * @param name The instance variable name.
	 * @param value The instance variable value.
	 */
	public void setValue(String name, BigInteger value) {
		
		if (name.equals("init")) {
			initialValue = value.add(BigInteger.ZERO);
			currentValue = BitSetUtils.Create(value);
		} else {
			super.setValue(name,value);
		}
	} // end of setValue method
	
	/**
	 * Set a String instance variable value (during a load).
	 * 
	 * @param name The instance variable name.
	 * @param value The instance variable value.
	 */
	public void setValue(String name, String value) {
		
		if (name.equals("name")) {
			this.name = value;
			circuit.addName(value);
		} else if (name.equals("type")) {
			if (value.equals("latch"))
				type = Type.Latch;
			else if (value.equals("pff"))
				type = Type.PosFF;
			else if (value.equals("nff"))
				type = Type.NegFF;
		}
		else if (name.equals("orient")) {
			if(value.equals("LEFT"))
			{
				orientation = JLSInfo.Orientation.LEFT;
			}
			else if(value.equals("RIGHT"))
			{
				orientation = JLSInfo.Orientation.RIGHT;
			}
			else if(value.equals("UP"))
			{
				orientation = JLSInfo.Orientation.UP;
			}
			else if(value.equals("DOWN"))
			{
				orientation = JLSInfo.Orientation.DOWN;
			} 
			} else {
			super.setValue(name,value);
		}
	} // end of setValue method
	
	/**
	 * Save this element.
	 * 
	 * @param output The output writer.
	 */
	public void save(PrintWriter output) {
		
		output.println("ELEMENT Register");
		super.save(output);
		output.println(" String name \"" + name + "\"");
		output.println(" int bits " + bits);
		output.println(" Int init " + initialValue.toString());
		output.println(" String orient \"" + orientation.toString() + "\"");
		output.println(" int delay " + propDelay);
		switch (type) {
		case Latch: output.println(" String type \"latch\""); break;
		case PosFF: output.println(" String type \"pff\""); break;
		case NegFF: output.println(" String type \"nff\""); break;
		}
		output.println(" int watch " + (watched ? 1 : 0));
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
	public Element copy() {
		
		Register it = new Register(circuit);
		it.name = new String(name);
		it.type = type;
		it.bits = bits;
		it.propDelay = propDelay;
		it.initialValue = initialValue.add(BigInteger.ZERO);
		it.currentValue = BitSetUtils.Create(initialValue);
		it.base = base;
		it.orientation = orientation;
		it.watched = watched;
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
	public String getName() {
		
		return name;
	} // end of getName method
	
	/**
	 * Get the number of bits in this register.
	 * 
	 * @return the number of bits.
	 */ 
	public int getBits() {
		
		return bits;
	} // end of getBits method

	/**
	 * Remove name from list of element names in this circuit.
	 * 
	 * @param circ A reference back to the circuit the element is in.
	 */
	public void remove(Circuit circ) {
		
		circ.removeName(name);
		super.remove(circ);
	} // end of remove method

	/**
	 * Registers have timing info (propagation delay).
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
	 * @param amount The new delay amount.
	 */
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
	 * Tells if a register is capable of rotatating, can only rotate when inputs or outputs have no attachments.
	 * @return False if any input or output has a wire attached, True otherwise
	 */
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
	public void rotate(JLSInfo.Orientation direction, Graphics g) {
		
		if(direction == JLSInfo.Orientation.LEFT) {
			if(orientation == JLSInfo.Orientation.LEFT) {
				orientation = JLSInfo.Orientation.DOWN;
			}
			else if(orientation == JLSInfo.Orientation.DOWN) {
				orientation = JLSInfo.Orientation.RIGHT;
			}
			else if(orientation == JLSInfo.Orientation.RIGHT) {
				orientation = JLSInfo.Orientation.UP;
			}
			else if(orientation == JLSInfo.Orientation.UP) {
				orientation = JLSInfo.Orientation.LEFT;
			}
		}
		else if(direction == JLSInfo.Orientation.RIGHT) {
			if(orientation == JLSInfo.Orientation.LEFT) {
				orientation = JLSInfo.Orientation.UP;
			}
			else if(orientation == JLSInfo.Orientation.DOWN) {
				orientation = JLSInfo.Orientation.LEFT;
			}
			else if(orientation == JLSInfo.Orientation.RIGHT) {
				orientation = JLSInfo.Orientation.DOWN;
			}
			else if(orientation == JLSInfo.Orientation.UP) {
				orientation = JLSInfo.Orientation.RIGHT;
			}
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
	public void flip(Graphics g) {
		
		if(orientation == JLSInfo.Orientation.LEFT) {
			orientation = JLSInfo.Orientation.RIGHT;
		}
		else if(orientation == JLSInfo.Orientation.RIGHT) {
			orientation = JLSInfo.Orientation.LEFT;
		}
		else if(orientation == JLSInfo.Orientation.UP) {
			orientation = JLSInfo.Orientation.DOWN;
		}
		else if(orientation == JLSInfo.Orientation.DOWN) {
			orientation = JLSInfo.Orientation.UP;
		}
		inputs.clear();
		outputs.clear();
		width = 0;
		height = 0;
		init(g);
	} // end of flip method

	/**
	 * Dialog box to create/modify register characteristics.
	 */
	private class RegisterEdit extends JDialog implements ActionListener {
		
		// properties
		private JButton ok = new JButton("OK");
		private JButton cancel = new JButton("Cancel");
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
		 * @param x The x-coordinate of the position of the dialog.
		 * @param y The y-coordinate of the position of the dialog.
		 * @param creating True if creating, false if modifying.
		 */
		private RegisterEdit(int x, int y, boolean creating) {
			
			// set up window title
			super(JLSInfo.frame,"Create Register",true);
			
			// set not cancelled
			cancelled = false;
			
			// save create/modify
			this.creating = creating;
			
			// set up window
			Container window = getContentPane();
			window.setLayout(new BoxLayout(window,BoxLayout.Y_AXIS));
			
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
				JLSInfo.hb.enableHelpOnButton(help,"register",null);
			okCancel.add(help);
			window.add(okCancel);
			getRootPane().setDefaultButton(ok);
			
			ok.addActionListener(this);
			nameField.addActionListener(this);
			bitsField.addActionListener(this);
			valueField.addActionListener(this);
			cancel.addActionListener(this);
			base2.addActionListener(this);
			base10.addActionListener(this);
			base16.addActionListener(this);
			
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
		 * React to buttons.
		 * 
		 * @param event The event object for this action.
		 */
		public void actionPerformed(ActionEvent event) {
			
			if (event.getSource() == ok || event.getSource() == nameField ||
					event.getSource() == bitsField || event.getSource() == valueField) {
				String tname = nameField.getText().trim();
				if (tname.equals("") || !Util.isValidName(tname)) {
					JOptionPane.showMessageDialog(this,
							"Missing or invalid name", "Error",
							JOptionPane.ERROR_MESSAGE);
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
					JOptionPane.showMessageDialog(this,
							"Value not numeric", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (tbits < 1) {
					JOptionPane.showMessageDialog(this,
							"Must have at least 1 bit", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (tinitialValue.bitLength() > tbits) {
					JOptionPane.showMessageDialog(this,
							"Value too large for number of bits", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (!creating) {
					circuit.removeName(name);
				}
				if (!circuit.addName(tname)) {
					JOptionPane.showMessageDialog(this,
							"Duplicate name", "Error",
							JOptionPane.ERROR_MESSAGE);
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
			}
			else if (event.getSource() == cancel) {
				cancel();
			}
			else if (event.getSource() == base2) {
				BigInteger val = BigInteger.ZERO;
				if (!valueField.getText().equals(""))
					val = new BigInteger(valueField.getText(),base);
				base = 2;
				valuePad.setBase(2);
				valueField.setText(val.toString(2));
			}
			else if (event.getSource() == base10) {
				BigInteger val = BigInteger.ZERO;
				if (!valueField.getText().equals(""))
					val = new BigInteger(valueField.getText(),base);
				base = 10;
				valuePad.setBase(10);
				valueField.setText(val.toString(10));
			}
			else if (event.getSource() == base16) {
				BigInteger val = BigInteger.ZERO;
				if (!valueField.getText().equals(""))
					val = new BigInteger(valueField.getText(),base);
				base = 16;
				valuePad.setBase(16);
				valueField.setText(val.toString(16));
			}
		} // end of actionPerformed method
		
		/**
		 * Cancel this gate.
		 */
		private void cancel() {
			
			cancelled = true;
			dispose();
		} // end of cancel method
		
	} // end of RegisterEdit class
	
	/**
	 * Registers can be modified.
	 * 
	 * @return true.
	 */ 
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
	public boolean change(Graphics g, JPanel editWindow, int x, int y) {
	
		// save g for valueFits method
		saveg = g;
		
		// display dialog
		Point pos = editWindow.getMousePosition();
		Point win = editWindow.getLocationOnScreen();
		if (pos == null) {
			new RegisterEdit(x+win.x,y+win.y,false);
		}
		else {
			new RegisterEdit(pos.x+win.x,pos.y+win.y,false);
		}
		
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
	public boolean canWatch() {
		
		return true;
	} // end of canWatch method
	
	/**
	 * See if this register is watched.
	 * 
	 * @return true if it is, false if it is not.
	 */
	public boolean isWatched() {
		
		return watched;
	} // end of isWatched method
	
	/**
	 * Set whether this register is watched or not.
	 * 
	 * @param state True to make it watched, false to make it not watched.
	 */
	public void setWatched(boolean state) {
		
		watched = state;
	} // end of setWatched method
	
	/**
	 * Print current value to stdout.
	 * 
	 * @param prefix qualified name.
	 */
	public void printValue(String prefix) {
		
		if (prefix.equals("")) {
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
	 */
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
	 */
	public void initSim(Simulator sim) {
		
		// create current values and to-be value
		BitSet currentValue = BitSetUtils.Create(initialValue);
		toBeValue = (BitSet)currentValue.clone();
		currentC = 0;
		
		// create values to output
		BitSet qOut = (BitSet)currentValue.clone();
		BitSet notQOut = (BitSet)currentValue.clone();
		notQOut.flip(0,bits);
		
		// set output pins to 0
		Output q = outputs.get(0);
		q.setValue(new BitSet(1));
		Output notq = outputs.get(1);
		notq.setValue(new BitSet(1));
		
		// post output event at time 0
		sim.post(new SimEvent(0,this,qOut.clone()));
		
	} // end of initSim method
	
	/**
	 * React to an event.
	 * 
	 * @param now The current simulation time.
	 * @param sim The simulator to post events to.
	 * @param todo If null, an input has changed, otherwise it is the value to output.
	 */
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
	public void showCurrentValue(Point where) {
		
		String hex = BitSetUtils.ToString(currentValue,16);
		String unsigned = BitSetUtils.ToString(currentValue,10);
		String signed = BitSetUtils.ToStringSigned(currentValue,bits);
		String value = "0x" +hex + " (" + unsigned + " unsigned, " + signed + " signed)";
		JOptionPane.showMessageDialog(JLSInfo.frame, value, "Information",
				JOptionPane.INFORMATION_MESSAGE);
	} // end of showCurrentValue method
	
} // end of Register class
