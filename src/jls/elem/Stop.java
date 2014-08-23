package jls.elem;

import jls.*;
import jls.sim.*;
import java.awt.*;
import java.io.*;
import javax.swing.*;
import java.util.*;

/**
 * Stop element.
 * Causes simulator to terminate when input is asserted.
 * 
 * @author David A. Poplawski
 */
public class Stop extends LogicElement {

	/**
	 * Create a new stop element.
	 * 
	 * @param circuit The circuit this element is part of.
	 */
	public Stop(Circuit circuit) {
		
		super(circuit);
	} // end of constructor
	

	/**
	 * Set up element.
	 * 
	 * @param g The Graphics object to use to initialize sizes
	 * @param editWindow The editor window this constant will be displayed in.
	 * @param x The x-coordinate of the last known mouse position.
	 * @param y The y-coordinate of the last known mouse position.
	 * 
	 * @return false if cancelled, true otherwise.
	 */
	public boolean setup(Graphics g, JPanel editWindow, int x, int y) {
		
		// complete initialization
		init(g);
		
		// save position
		Point p = MouseInfo.getPointerInfo().getLocation();
		Point win = editWindow.getLocationOnScreen();
		p.x -= win.x;
		p.y -= win.y;
		if (p != null) {
			super.setXY(p.x-width/2,p.y-height/2);
		}
		
		return true;
	} // end of setup method
	
	/**
	 * Initialize internal info for this element.
	 * 
	 * @param g Unused.
	 */
	public void init(Graphics g) {
		
		// set up size
		int s = JLSInfo.spacing;
		width = s * 2;
		height = width;
		
		// create inputs
		inputs.add(new Input("input0",this,0,s,1));
		inputs.add(new Input("input1",this,s,0,1));
		inputs.add(new Input("input2",this,s,2*s,1));
		inputs.add(new Input("input3",this,2*s,s,1));
	} // end of init method

	/**
	 * Draw this gate.
	 * 
	 * @param g The graphics object to draw with.
	 */
	public void draw(Graphics g) {
		
		int s = JLSInfo.spacing;
		
		// draw context
		super.draw(g);
		
		// draw only the attached inputs, or all four if none attached
		
		// get unattached inputs
		Vector<Input>detach = new Vector<Input>(4);
		for (Input input : inputs) {
			if (!input.isAttached())
				detach.add(input);
		}
		
		// if there are one, two or three unattached ones
		int count = detach.size();
		if (count > 0 && count < 4) {
			
			// remove unattached inputs
			inputs.removeAll(detach);
			
			// if no inputs left, put all four back
			if (inputs.size() == 0) {
				inputs.add(new Input("input0",this,0,s,1));
				inputs.add(new Input("input1",this,s,0,1));
				inputs.add(new Input("input2",this,s,2*s,1));
				inputs.add(new Input("input3",this,2*s,s,1));
			}
		}
		
		// draw stop sign
		Polygon sign = new Polygon();
		int h = s/2;
		int d = s*2;
		sign.addPoint(0,h);
		sign.addPoint(h,0);
		sign.addPoint(s+h,0);
		sign.addPoint(d,h);
		sign.addPoint(d,s+h);
		sign.addPoint(s+h,d);
		sign.addPoint(h,d);
		sign.addPoint(0,s+h);
		sign.translate(x,y);
		g.setColor(Color.black);
		g.drawPolygon(sign);
		
		// draw inputs
		for (Input input : inputs) {
			input.draw(g);
		}
		
		// draw "stop"
		Font f = g.getFont();
		Font nf = f.deriveFont((float)(f.getSize()*0.7));
		g.setFont(nf);
		FontMetrics fm = g.getFontMetrics();
		int w = fm.stringWidth("STOP");
		int ascent = fm.getAscent();
		int descent = fm.getDescent();
		int sh = ascent + descent;
		g.setColor(Color.black);
		g.drawString("STOP",x+(width-w)/2+1,y+s-sh/2+ascent);
		g.setFont(f);
		
	} // end of draw method
	

	/**
	 * Copy this element.
	 */
	public Element copy() {
		
		Stop it = new Stop(circuit);
		for (Input input : inputs) {
			it.inputs.add(input.copy(it));
		}
		super.copy(it);
		return it;
	} // end of copy method
	
	/**
	 * Save this element.
	 * 
	 * @param output The output writer.
	 */
	public void save(PrintWriter output) {
		
		output.println("ELEMENT Stop");
		super.save(output);
		output.println("END");
	} // end of save method
	
	/**
	 * Display info about this element.
	 * 
	 * @param info The JLabel to display with.
	 */
	public void showInfo(JLabel info) {
		
		info.setText("stop simulation");
	} // end of showInfo method
	

//	-------------------------------------------------------------------------------
//	Simulation
//	-------------------------------------------------------------------------------
	
	/**
	 * Initialize simulation.
	 * 
	 * @param The simulator.
	 */
	public void initSim(Simulator sim) {
		
		// do nothing
	} // end of initSim method
	
	/**
	 * React to an event.
	 * 
	 * @param now The current simulation time.
	 * @param sim The simulator to post events to.
	 * @param todo Should be null.
	 */
	public void react(long now, Simulator sim, Object todo) {
		
		// find the attached input
		for (Input input : inputs) {
			if (!input.isAttached())
				continue;
			
			// if value is a 1, stop
			BitSet in = input.getValue();
			if (in != null && in.cardinality() != 0) {
				sim.stop();
				return;
			}
		}
	} // end of react method
	
} // end of Stop class
