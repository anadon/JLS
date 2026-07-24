package jls.elem;

import jls.core.Geometry;
import jls.core.Orientation;
import jls.*;
import jls.elem.Group.Entry;
import jls.sim.*;
import java.io.PrintWriter;
import java.util.BitSet;

/**
 * Split an n-bit input signal into multiple single or multiple bit outputs.
 * 
 * @author David A. Poplawski
 */
public final class Splitter extends Group implements TriProp {
	
	/**
	 * Create a new splitter element.
	 * 
	 * @param circuit The circuit this element is part of.
	 */
	public Splitter(Circuit circuit) {
		
		super(circuit);
	} // end of constructor

	/**
	 * Initialize internal info for this element.
	 *
	 * @param g The Graphics object to use.
	 */
	@Override
	public void init(jls.core.TextMetrics g) {
		
		// set up height and width
		super.init(g);
		
		int s = Geometry.SPACING;
		
		if(orientation == Orientation.RIGHT)
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
		else if(orientation == Orientation.LEFT)
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
		else if(orientation == Orientation.UP)
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
		else if(orientation == Orientation.DOWN)
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
	 * Copy this element.
	 */
	@Override
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
	@Override
	public void save(PrintWriter output) {
		
		output.println("ELEMENT Splitter");
		super.save(output);
	} // end of save method
	
	/**
	 * Display info about this element.
	 * 
	 * @param info The JLabel to display with.
	 */
	@Override
	public String infoText() {

		return "unbundle " + bits + " bits";
	} // end of showInfo method
	
	/**
	 * Set this element to tri-state or not.
	 * 
	 * @param which True to set to tri-state, false otherwise.
	 */
	@Override
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
	@Override
	public void rotate(Orientation direction, jls.core.TextMetrics g)
	{
		super.rotate(direction, g);
		init(g);
	}
	
	/**
	 * This method will flip a splitter
	 * @param g The current graphics context to facilitate recalculation of size when flipping
	 */
	@Override
	public void flip(jls.core.TextMetrics g)
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
	@Override
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
	@Override
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
