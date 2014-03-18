package jls.elem;

import jls.*;
import jls.elem.Group.Entry;
import jls.sim.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.PrintWriter;
import java.util.BitSet;

import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Split an n-bit input signal into multiple single or multiple bit outputs.
 * 
 * @author David A. Poplawski
 */
public class Splitter extends Group implements TriProp {
	
	/**
	 * Create a new splitter element.
	 * 
	 * @param circuit The circuit this element is part of.
	 */
	public Splitter(Circuit circuit) {
		
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
			new GroupCreate(x+win.x,y+win.y,"Unbundler");
		}
		else {
			new GroupCreate(pos.x+win.x,pos.y+win.y,"Unbundler");
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
		
		int s = JLSInfo.spacing;
		
		if(orientation == JLSInfo.Orientation.RIGHT)
		{
			// set up input
			inputs.add(new Input("input",this,0,((ranges.size()-1)/2+1)*s,bits));
			
			// set up outputs
			int ypos = s;
			for (Entry e : ranges) {
				Output out = new Output(e.toCircuitString(),this,width,ypos,e.getSize());
				outputs.add(out);
				if (loadTriState) {
					out.loadSetTriState();
				}
				ypos += s;
			}
		}
		else if(orientation == JLSInfo.Orientation.LEFT)
		{
			// set up input
			inputs.add(new Input("input",this,width,((ranges.size()-1)/2+1)*s,bits));
		
			// set up outputs
			int ypos = s;
			for (Entry e : ranges) {
				Output out = new Output(e.toCircuitString(),this,0,ypos,e.getSize());
				outputs.add(out);
				if (loadTriState) {
					out.loadSetTriState();
				}
				ypos += s;
			}
		}
		else if(orientation == JLSInfo.Orientation.UP)
		{
			int xpos = s;
			for (Entry e : ranges) {
				Output out = new Output(e.toCircuitString(),this,xpos,0,e.getSize());
				outputs.add(out);
				if (loadTriState) {
					out.loadSetTriState();
				}
				xpos += s;
			}
			// set up input
			inputs.add(new Input("input",this,((ranges.size()-1)/2+1)*s,height,bits));
		}
		else if(orientation == JLSInfo.Orientation.DOWN)
		{
			int xpos = s;
			for (Entry e : ranges) {
				Output out = new Output(e.toCircuitString(),this,xpos,height,e.getSize());
				outputs.add(out);
				if (loadTriState) {
					out.loadSetTriState();
				}
				xpos += s;
			}
			// set up input
			inputs.add(new Input("input",this,((ranges.size()-1)/2+1)*s,0,bits));
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
		
		if(orientation == JLSInfo.Orientation.RIGHT)
		{
			// draw input and line from it
			Input input = inputs.get(0);
			g.setColor(Color.black);
			int ypos = input.getY();
			g.drawLine(x,ypos,x+s/2,ypos);
			input.draw(g);
		
			// draw split line
			g.setColor(Color.BLACK);
			g.drawLine(x+s/2,y+s,x+s/2,y+height-s);
		
			// draw outputs and lines to them
			FontMetrics fm = g.getFontMetrics();
			for (Output output : outputs) {
				output.draw(g);
				ypos = output.getY();
				Rectangle2D t = fm.getStringBounds(output.getName(),g);
				g.setColor(Color.BLACK);
				int edge = (int)(x+width-t.getWidth()-d2);
				g.drawString(output.getName(),edge,(int)(ypos-t.getHeight()/2+fm.getAscent()));
				g.drawLine(x+s/2,ypos,edge-d2,ypos);
			}
		}
		else if(orientation == JLSInfo.Orientation.LEFT)
		{
			// draw input and line from it
			Input input = inputs.get(0);
			g.setColor(Color.black);
			int ypos = input.getY();
			g.drawLine(x+width,ypos,x+width-s/2,ypos);
			input.draw(g);
			
			// draw split line
			g.setColor(Color.BLACK);
			g.drawLine(x+width-s/2,y+s,x+width-s/2,y+height-s);
			
			// draw outputs and lines to them
			FontMetrics fm = g.getFontMetrics();
			for (Output output : outputs) {
				output.draw(g);
				ypos = output.getY();
				Rectangle2D t = fm.getStringBounds(output.getName(),g);
				g.setColor(Color.BLACK);
				int edge = (int)(x+JLSInfo.pointDiameter/2);
				g.drawString(output.getName(),edge, (int)(ypos-t.getHeight()/2+fm.getAscent()));
				g.drawLine(x+width-s/2,ypos,(int)(edge+t.getWidth()+d2),ypos);
			}
		}
		else if(orientation == JLSInfo.Orientation.DOWN)
		{
			int inum = 0;
			FontMetrics fm = g.getFontMetrics();
			for (Output output : outputs) {
				output.draw(g);
				int xpos = output.getX();
				Rectangle2D t = fm.getStringBounds(output.getName(),g);
				g.setColor(Color.BLACK);
				int edge = (int)(y+height-JLSInfo.pointDiameter/2);
				if(inum%2 == 0)
				{
					g.drawString(output.getName(),xpos-(int)t.getWidth()/2, (int)(edge-t.getHeight()/2+6));
				}
				g.drawLine(xpos,y+s,xpos,(int)(edge-t.getHeight()+d2));
				inum++;
			}
			
			// draw split line
			g.setColor(Color.BLACK);
			g.drawLine(x+s,y+s,x+width-s,y+s);
			
			// draw output and line to it
			Input input = inputs.get(0);
			g.setColor(Color.black);
			int xpos = input.getX();
			g.drawLine(xpos,y+s,xpos,y);
			input.draw(g);
		}
		else if(orientation == JLSInfo.Orientation.UP)
		{
			int inum = 0;
			FontMetrics fm = g.getFontMetrics();
			for (Output output : outputs) {
				output.draw(g);
				int xpos = output.getX();
				Rectangle2D t = fm.getStringBounds(output.getName(),g);
				g.setColor(Color.BLACK);
				int edge = (int)(y+JLSInfo.pointDiameter/2);
				if(inum%2 == 0)
				{
					g.drawString(output.getName(),xpos-(int)t.getWidth()/2, (int)(edge+t.getHeight()/2+6));
				}
				g.drawLine(xpos,y+height-s,xpos,(int)(edge+t.getHeight()+d2));
				inum++;
			}
			
			// draw split line
			g.setColor(Color.BLACK);
			g.drawLine(x+s,y+height-s,x+width-s,y+height-s);
			
			// draw output and line to it
			Input input = inputs.get(0);
			g.setColor(Color.black);
			int xpos = input.getX();
			g.drawLine(xpos,y+height-s,xpos,y+height);
			input.draw(g);
		}
		
	} // end of draw method
	
	/**
	 * Copy this element.
	 */
	public Element copy() {
		
		Splitter it = new Splitter(circuit);
		super.copy(it);
		return it;
	} // end of copy method
	
	/**
	 * Save this element.
	 * 
	 * @param output The output writer.
	 */
	public void save(PrintWriter output) {
		
		output.println("ELEMENT Splitter");
		super.save(output);
	} // end of save method
	
	/**
	 * Display info about this element.
	 * 
	 * @param info The JLabel to display with.
	 */
	public void showInfo(JLabel info) {
		
		info.setText("unbundle " + bits + " bits");
	} // end of showInfo method
	
	/**
	 * Set this element to tri-state or not.
	 * 
	 * @param which True to set to tri-state, false otherwise.
	 */
	public void setTriState(boolean which) {
		
		triState = which;
		for (Output out : outputs) {
			out.setTriState(which);
		}
	} // end of setTriState method
	
	/**
	 *  This method will rotate the splitter if it is rotateable.
	 * @param direction The direction to rotate
	 * @param g The current graphics context for use in recalculating size
	 */
	public void rotate(JLSInfo.Orientation direction, Graphics g)
	{
		super.rotate(direction, g);
		init(g);
	}
	
	/**
	 * This method will flip a splitter
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
	 * Initialize this element by setting its outputs to 0 or null.
	 * 
	 * @param sim Unused.
	 */
	public void initSim(Simulator sim) {

		Output out = outputs.get(0);
		BitSet value = null;
		if (!out.isTriState()) {
			value = new BitSet();
		}
		for (Output output : outputs)
			output.setValue(value);
	} // end of initSim method
	
	/**
	 * React to an event.
	 * 
	 * @param now The current simulation time.
	 * @param sim The simulator to post events to.
	 * @param todo Unused.
	 */
	public void react(long now, Simulator sim, Object todo) {
		
		// get the input value
		BitSet value = inputs.get(0).getValue();
		
		// if null, send null to all outputs
		if (value == null) {
			for (Output output : outputs) {
				output.propagate(null,now,sim);
			}
			return;
		}
		
		// pick out bit range and send to corresponding output
		int outNum = 0;
		for(Entry e : ranges) {
			BitSet newValue = new BitSet(e.getSize());
			int vpos = 0;
			for (int i : e.getValues()) {
				boolean val = value.get(i);
				newValue.set(vpos,val);
				vpos += 1;
			}
			outputs.get(outNum).propagate(newValue,now,sim);
			outNum += 1;
		}
		
	} // end of react method
	
} // end of Splitter class
