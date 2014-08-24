package jls.elem;

import jls.*;
import jls.elem.Group.Entry;
import jls.sim.*;
import java.awt.*;
import java.awt.geom.*;
import java.io.PrintWriter;
import java.util.BitSet;
import javax.swing.*;

/**
 * Bind multiple input wires (or bundles) into a single bundle.
 * 
 * @author David A. Poplawski
 */
public class Binder extends Group implements TriProp {
	
	
	/**
	 * Create a new binder element.
	 * 
	 * @param circuit The circuit this element is part of.
	 */
	public Binder(Circuit circuit) {
		
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
			new GroupCreate(x+win.x,y+win.y,"Bundler");
		}
		else {
			new GroupCreate(pos.x+win.x,pos.y+win.y,"Bundler");
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
		if (p != null)
			setXY(p.x-width/2,p.y-height/2);
		
		return true;
	} // end of setup method
	
	/**
	 * Initialize internal info for this element.
	 * 
	 * @param g The Graphics object to use.
	 */
	public void init(Graphics g) {
		
		// set up height and width
		super.init(g);
		
		// set up inputs
		int s = JLSInfo.spacing;
		if(orientation == JLSInfo.Orientation.RIGHT)
		{
			int ypos = s;
			for(Entry e : ranges) {
				inputs.add(new Input(e.toCircuitString(), this, 0, ypos, e.getSize()));
				ypos += s;
			}
		
			// set up output
			outputs.add(new Output("output",this,width,((ranges.size()-1)/2+1)*s,bits));
			if (loadTriState) {
				outputs.get(0).loadSetTriState();
			}
		}
		else if(orientation == JLSInfo.Orientation.LEFT)
		{
			int ypos = s;
			for(Entry e : ranges) {
				inputs.add(new Input(e.toCircuitString(), this, width, ypos, e.getSize()));
				ypos += s;
			}
			
			// set up output
			outputs.add(new Output("output",this,0,((ranges.size()-1)/2+1)*s,bits));
			if (loadTriState) {
				outputs.get(0).loadSetTriState();
			}
		}
		else if(orientation == JLSInfo.Orientation.DOWN)
		{
			int xpos = s;
			for(Entry e : ranges) {
				inputs.add(new Input(e.toCircuitString(), this, xpos, 0, e.getSize()));
				xpos += s;
			}
			// set up output
			outputs.add(new Output("output",this,((ranges.size()-1)/2+1)*s,height,bits));

			if (loadTriState) {
				outputs.get(0).loadSetTriState();
			}
		}
		else if(orientation == JLSInfo.Orientation.UP)
		{
			int xpos = s;
			for(Entry e : ranges) {
				inputs.add(new Input(e.toCircuitString(), this, xpos, height, e.getSize()));
				xpos += s;
			}
			// set up output
			outputs.add(new Output("output",this,((ranges.size()-1)/2+1)*s,0,bits));

			if (loadTriState) {
				outputs.get(0).loadSetTriState();
			}
		}
	} // end of init method
	
	/**
	 * Draw this element.
	 * 
	 * @param g The graphics object to draw with.
	 */
	public void draw(Graphics g) {
		
		// draw context
		super.draw(g);
		
		// set up
		int d2 = JLSInfo.pointDiameter/2;
		int s = JLSInfo.spacing;
		
		// draw inputs
		FontMetrics fm = g.getFontMetrics();
		
		if(orientation == JLSInfo.Orientation.RIGHT)
		{
			for (Input input : inputs) {
				input.draw(g);
				int ypos = input.getY();
				Rectangle2D t = fm.getStringBounds(input.getName(),g);
				g.setColor(Color.BLACK);
				int edge = (int)(x+JLSInfo.pointDiameter/2);
				g.drawString(input.getName(),edge, (int)(ypos-t.getHeight()/2+fm.getAscent()));
				g.drawLine(x+width-s/2,ypos,(int)(edge+t.getWidth()+d2),ypos);
			}
		
			// draw split line
			g.setColor(Color.BLACK);
			g.drawLine(x+width-s/2,y+s,x+width-s/2,y+height-s);
		
			// draw output and line to it
			Output output = outputs.get(0);
			g.setColor(Color.black);
			int ypos = output.getY();
			g.drawLine(x+width,ypos,x+width-s/2,ypos);
			output.draw(g);
		}
		else if(orientation == JLSInfo.Orientation.LEFT)
		{
			for (Input input : inputs) {
				input.draw(g);
				int ypos = input.getY();
				Rectangle2D t = fm.getStringBounds(input.getName(),g);
				g.setColor(Color.BLACK);
				int edge = (int)(x+width-JLSInfo.pointDiameter/2-t.getWidth());
				g.drawString(input.getName(),edge, (int)(ypos-t.getHeight()/2+fm.getAscent()));
				g.drawLine(x+s/2,ypos,(int)(edge-d2),ypos);
			}
			
			// draw split line
			g.setColor(Color.BLACK);
			g.drawLine(x+s/2,y+s,x+s/2,y+height-s);
			
			// draw output and line to it
			Output output = outputs.get(0);
			g.setColor(Color.black);
			int ypos = output.getY();
			g.drawLine(x,ypos,x+s/2,ypos);
			output.draw(g);
		}
		else if(orientation == JLSInfo.Orientation.DOWN)
		{
			int inum = 0;
			for (Input input : inputs) {
				input.draw(g);
				int xpos = input.getX();
				Rectangle2D t = fm.getStringBounds(input.getName(),g);
				g.setColor(Color.BLACK);
				int edge = (int)(y+JLSInfo.pointDiameter/2);
				if(inum%2 == 0)
				{
					g.drawString(input.getName(),xpos-(int)t.getWidth()/2, (int)(edge+t.getHeight()/2+6));
				}
				g.drawLine(xpos,y+height-s,xpos,(int)(edge+t.getHeight()+d2));
				inum++;
			}
			
			// draw split line
			g.setColor(Color.BLACK);
			g.drawLine(x+s,y+height-s,x+width-s,y+height-s);
			
			// draw output and line to it
			Output output = outputs.get(0);
			g.setColor(Color.black);
			int xpos = output.getX();
			g.drawLine(xpos,y+height-s,xpos,y+height);
			output.draw(g);
		}
		else if(orientation == JLSInfo.Orientation.UP)
		{
			int inum = 0;
			for (Input input : inputs) {
				input.draw(g);
				int xpos = input.getX();
				Rectangle2D t = fm.getStringBounds(input.getName(),g);
				g.setColor(Color.BLACK);
				int edge = (int)(y+height-JLSInfo.pointDiameter/2);
				if(inum%2 == 0)
				{
					g.drawString(input.getName(),xpos-(int)t.getWidth()/2, (int)(edge-t.getHeight()/2+6));
				}
				g.drawLine(xpos,y+s,xpos,(int)(edge-t.getHeight()+d2));
				inum++;
			}
			
			// draw split line
			g.setColor(Color.BLACK);
			g.drawLine(x+s,y+s,x+width-s,y+s);
			
			// draw output and line to it
			Output output = outputs.get(0);
			g.setColor(Color.black);
			int xpos = output.getX();
			g.drawLine(xpos,y+s,xpos,y);
			output.draw(g);
		}
	} // end of draw method
	
	/**
	 * Copy this element.
	 */
	public Element copy() {
		
		Binder it = new Binder(circuit);
		super.copy(it);
		return it;
	} // end of copy method
	
	/**
	 * Save this element.
	 * 
	 * @param output The output writer.
	 */
	public void save(PrintWriter output) {
		
		output.println("ELEMENT Binder");
		super.save(output);
	} // end of save method
	
	/**
	 * Display info about this element.
	 * 
	 * @param info The JLabel to display with.
	 */
	public void showInfo(JLabel info) {
		
		info.setText("bundle " + bits + " bits");
	} // end of showInfo method
	
	/**
	 * Set this element to tri-state or not.
	 * Output will be tri-state if and only if all attached inputs are.
	 * Don't propagate if no change.
	 * 
	 * @param which True to set to tri-state, false otherwise.
	 */
	public void setTriState(boolean which) {
		
		// save current state
		boolean saveState = this.triState;
		
		// see if all attached inputs are tri-state
		int tri = 0;
		for (Input input : inputs) {
			if (input.isAttached() && input.getWireEnd().isTriState()) {
				tri += 1;
			}
		}
		
		// set tri-state if all inputs are tri-state
		int numInputs = inputs.size();
		if (numInputs > 0 && numInputs == tri) {
			triState = true;
		}
		else {
			triState = false;
		}
		
		// don't propagate if no change
		if (triState == saveState)
			return;
		
		// propagate to outputs
		for (Output out : outputs) {
			out.setTriState(triState);
		}
	} // end of setTriState method
	
	
	/**
	 *  This method will rotate the binder if it is rotateable.
	 * @param direction The direction to rotate
	 * @param g The current graphics context for use in recalculating size
	 */
	public void rotate(JLSInfo.Orientation direction, Graphics g)
	{
		super.rotate(direction, g);
		init(g);
	}
	
	/**
	 * This method will flip a binder
	 * @param g The current graphics context to facilitate recalculation of size when flipping
	 */
	public void flip(Graphics g)
	{
		super.flip(g);
		init(g);
	}
	
//	-------------------------------------------------------------------------------
//	Simulation
//	-------------------------------------------------------------------------------
	
	/**
	 * Initialize this element by setting its output to 0 or null.
	 * 
	 * @param sim Unused.
	 */
	public void initSim(Simulator sim) {
		
		Output out = outputs.get(0);
		BitSet value = null;
		if (!out.isTriState()) {
			value = new BitSet();
		}
		outputs.get(0).setValue(value);

	} // end of initSim method
	
	/**
	 * React to an event.
	 * 
	 * @param now The current simulation time.
	 * @param sim The simulator to post events to.
	 * @param todo Unused.
	 */
	public void react(long now, Simulator sim, Object todo) {
		
		// create output value
		BitSet newValue = new BitSet(bits);
		
		// get the input values and set bits in the output value
		int inNum = 0;
		boolean allOff = true;
		for (Entry e : ranges) {
			BitSet value = inputs.get(inNum).getValue();
			
			// make a tristate off be a 0
			if (value == null) {
				value = new BitSet();
			}
			else {
				allOff = false;
			}
			int inp = 0;
			for(int i : e.getValues()) {
				boolean val = value.get(inp);
				newValue.set(i, val);
				inp += 1;
			}
			inNum += 1;
		}
		
		// send value to output
		if (allOff) {
			outputs.get(0).propagate(null,now,sim);
		}
		else {
			outputs.get(0).propagate(newValue,now,sim);
		}
		
	} // end of react method
	
} // end of Binder class
