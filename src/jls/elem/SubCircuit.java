package jls.elem;

import jls.*;
import jls.sim.*;
import jls.util.Placement;
import java.awt.*;

import javax.swing.*;

import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import java.util.*;

/**
 * Circuit element connecting this circuit to an imported subcircuit.
 * 
 * @author David A. Poplawski
 */
public final class SubCircuit extends LogicElement implements TriProp {
	
	// properties
	private Circuit subCircuit;		// the subcircuit
	private String name;			// the name in the circuit imported into
	private Map<Input,InputPin> inmap = new HashMap<Input,InputPin>();
	private Map<OutputPin,Output> outmap = new HashMap<OutputPin,Output>();
	private JLSInfo.Orientation orientation = JLSInfo.Orientation.RIGHT;
	
	// editing properties
	protected boolean cancelled;
	
	/**
	 * Create printable view of this element.
	 * 
	 * @return the name, orientation and input/output pin names.
	 */
	@Override
	public String toString() {
		
		String result = "SubCircuit[" + name + ",";
		switch (orientation) {
		case RIGHT: result += "right"; break;
		case LEFT: result += "left"; break;
		case UP: result += "up"; break;
		case DOWN: result += "down"; break;
		default: result += "unknown"; break;
		}
		result += ",inputs={";
		boolean firstTime = true;
		for (Input p : inmap.keySet()) {
			if (!firstTime)
				result += ",";
			firstTime = false;
			result += p.getName();
		}
		result += "},outputs={";
		firstTime = true;
		for (OutputPin p : outmap.keySet()) {
			if (!firstTime)
				result += ',';
			firstTime = false;
			result += p.getName();
		}
		result += "}]";
		return result;
	} // end of toString method
	
	/**
	 * Create a new subcircuit element.
	 * 
	 * @param circuit The circuit this element is part of.
	 * @see jls.edit.SaveAsNameCheckTest#importedCircuitsAreSkipped()
	 * @see jls.elem.HollowVsFilledCollisionTest#probe()
	 */
	public SubCircuit(Circuit circuit) {
		
		super(circuit);
	} // end of constructor
	
	/**
	 * Save the subcircuit that this element represents.
	 * 
	 * @param subCirc The subcircuit.
	 */
	public void setSubCircuit(Circuit subCirc) {
		
		subCircuit = subCirc;
	} // end of setSubCircuit method
	
	/**
	 * Get the subcircuit this element represents.
	 * 
	 * @return the subcircuit.
	 * @see jls.SimulationSemanticsRegressionTest#initInputsReachesInsideSubcircuits()
	 */
	public Circuit getSubCircuit() {
		
		return subCircuit;
	} // end of getSubCircuit method
	
	/**
	 * Get the (local) name of this subcircuit element (not the circuit it represents).
	 * 
	 * @return the name.
	 */
	@Override
	public String getName() {
		
		return name;
	} // end of getName method
	
	/**
	 * Set the (local) name of this subcircuit element.
	 * 
	 * @param name The name.
	 */
	public void setName(String name) {
		
		this.name = name;
	} // end of setName method
	
	/**
	 * Display dialog to get value and bits.
	 * 
	 * @param g The Graphics object to use to initialize sizes
	 * @param editWindow The editor window this subcircuit will be displayed in.
	 * @param x The x-coordinate of the last known mouse position.
	 * @param y The y-coordinate of the last known mouse position.
	 * 
	 * @return false if canceled, true otherwise.
	 */
	@Override
	public boolean setup(Graphics g, JPanel editWindow, int x, int y) {

		// show creation dialog
		new SubCreate();
		
		// don't do anything if user canceled gate
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
		
		// determine width if needed
		int s = JLSInfo.spacing;
		if (g != null) {
			if (width == 0 && height == 0) {
				FontMetrics fm = g.getFontMetrics();
				width = fm.stringWidth(" " + name + " ");
				for (Element el : subCircuit.getElements()) {
					if (el instanceof InputPin || el instanceof OutputPin) {
						Pin p = (Pin)el;
						String pinName = p.getName();
						width = Math.max(width,fm.stringWidth(pinName));
					}
				}
				width = (width+s-1)/s*s+s;
			}
			
		}
		
		// create sorted lists of input pins and output pins
		Comparator<Pin> cmp = new Comparator<Pin>() {
			/**
			 * Order two pins alphabetically by name.
			 *
			 * @param s1 The first pin.
			 * @param s2 The second pin.
			 *
			 * @return negative, zero or positive as s1's name orders before,
			 *         equal to or after s2's name.
			 */
			@Override
			public int compare(Pin s1, Pin s2) {
				return (s1.getName()).compareTo(s2.getName());
			}
		};
		SortedSet<InputPin> inputList = new TreeSet<InputPin>(cmp);
		SortedSet<OutputPin> outputList = new TreeSet<OutputPin>(cmp);
		for (Element el : subCircuit.getElements()) {
			if (el instanceof InputPin) {
				InputPin pin = (InputPin)el;
				inputList.add(pin);
			}
			else if (el instanceof OutputPin) {
				OutputPin pin = (OutputPin)el;
				outputList.add(pin);
			}
		}
		
		// create new list alternating elements from the two lists
		Vector<Pin> pins = new Vector<Pin>(inputList.size()+outputList.size());
		boolean fromInput = true;
		while (inputList.size()+ outputList.size() > 0) {
			if (fromInput) {
				fromInput = false;
				if (!inputList.isEmpty()) {
					InputPin pin = inputList.first();
					pins.add(pin);
					inputList.remove(pin);
				}
			}
			else {
				fromInput = true;
				if (!outputList.isEmpty()) {
					OutputPin pin = outputList.first();
					pins.add(pin);
					outputList.remove(pin);
				}
			}
		}
		
		// create inputs and outputs and determine height
		height = s;
		for (Pin el : pins) {
			if (el instanceof InputPin) {
				InputPin pin = (InputPin)el;
				Input in;
				if(orientation == JLSInfo.Orientation.RIGHT)
				{
					in = new Input(pin.getName(),this,0,height,pin.getBits());
				}
				else
				{
					in = new Input(pin.getName(),this,width,height,pin.getBits());
				}
				inputs.add(in);
				height += s;
				inmap.put(in,pin);
			}
			else if (el instanceof OutputPin) {
				OutputPin pin = (OutputPin)el;
				Output out;
				if(orientation == JLSInfo.Orientation.RIGHT)
				{
					out = new Output(pin.getName(),this,width,height,pin.getBits());
				}
				else
				{
					out = new Output(pin.getName(),this,0,height,pin.getBits());
				}
				outputs.add(out);
				height += s;
				outmap.put(pin,out);
				for (Input input : pin.inputs) {
					if (input.isAttached()) {
						out.setTriState(input.getWireEnd().isTriState());
					}
				}
				if (pin.isLoadTriState()) {
					out.loadSetTriState();
				}
			}
		}
		height += 2*s;
		
	} // end of init method
	
	/**
	 * Draw this element.
	 * 
	 * @param g The graphics object to draw with.
	 */
	@Override
	public void draw(Graphics g) {
		
		// draw context
		super.draw(g);
		
		// draw box
		int s = JLSInfo.spacing;
		g.setColor(Color.BLACK);
		g.drawRect(x,y,width,height);
		int yy = y+height-2*s;
		g.drawLine(x,yy,x+width,yy);
		
		// draw subcircuit name
		FontMetrics fm = g.getFontMetrics();
		int ascent = fm.getAscent();
		Rectangle2D rect = fm.getStringBounds(name,g);
		int dx = (int)Math.round((width-rect.getWidth())/2);
		int dy = (int)Math.round((2*s-rect.getHeight())/2);
		g.drawString(name,x+dx,yy+dy+ascent);
		
		// draw inputs and outputs
		for (Input input : inputs) {
			input.draw(g);
			String inputName = input.getName();
			rect = fm.getStringBounds(inputName,g);
			g.setColor(Color.BLACK);

			if(orientation == JLSInfo.Orientation.RIGHT)
			{
				dx = JLSInfo.pointDiameter/2+1;
				dy = ascent - (int)Math.round(rect.getHeight()/2);
			}
			else
			{
				dx = (int)(width - rect.getWidth() - (JLSInfo.pointDiameter/2+1));
				dy = ascent - (int)Math.round(rect.getHeight()/2);
			}
			g.drawString(inputName,x+dx,input.getY()+dy);
		}
		for (Output output : outputs) {
			output.draw(g);
			String outputName = output.getName();
			rect = fm.getStringBounds(outputName,g);
			g.setColor(Color.BLACK);
			if(orientation == JLSInfo.Orientation.LEFT)
			{
				dx = JLSInfo.pointDiameter/2+1;
				dy = ascent - (int)Math.round(rect.getHeight()/2);
			}
			else
			{
				dx = (int)(width - rect.getWidth() - (JLSInfo.pointDiameter/2+1));
				dy = ascent - (int)Math.round(rect.getHeight()/2);
			}
			g.drawString(outputName,x+dx,output.getY()+dy);
		}
		
	} // end of draw method
	
	/**
	 * Save this element in a file.
	 * 
	 * @param output The PrintWriter to write to.
	 */
	@Override
	public void save(PrintWriter output) {
		
		output.println("ELEMENT SubCircuit");
		output.println(" String orient \"" + orientation.toString() + "\"");
		super.save(output);
		subCircuit.save(output);
		output.println("END");
	} // end of save method

	/**
	 * A subcircuit block needs whatever format version its nested
	 * circuit needs - a file states its version once, at the top
	 * (issue #79), so the requirement propagates up through here.
	 *
	 * @return the minimum format version for the nested circuit.
	 */
	@Override
	public int saveFormatVersion() {

		return subCircuit.formatVersionNeeded();
	} // end of saveFormatVersion method
	
	/**
	 * Set a String instance variable value (during a load).
	 * 
	 * @param name The instance variable name.
	 * @param value The instance variable value.
	 */
	@Override
	public void setValue(String name, String value) {
		
		if (name.equals("orient")) {
			if(value.equals("LEFT"))
			{
				orientation = JLSInfo.Orientation.LEFT;
			}
			else if(value.equals("RIGHT"))
			{
				orientation = JLSInfo.Orientation.RIGHT;
			}
			
		} else {
			super.setValue(name,value);
		}
	} // end of setValue method
	
	/**
	 * Copy this element.
	 */
	@Override
	public Element copy() {
		
		// create infrastructure
		SubCircuit it = new SubCircuit(circuit);
		it.name = name;
		it.subCircuit = new Circuit(subCircuit.getName());
		it.subCircuit.setImported(it);
		it.orientation = orientation;
		
		// make a set of all elements except attached wire ends in subcircuit
		Set<Element> elements = new HashSet<Element>();
		for (Element el : subCircuit.getElements()) {
			if (el instanceof WireEnd) {
				WireEnd end = (WireEnd)el;
				if (end.isAttached())
					continue;
			}
			elements.add(el);
		}
		Util.copy(elements,it.subCircuit);
		Util.partition(it.subCircuit);
		
		for (Input input : inputs) {
			it.inputs.add(input.copy(it));
		}
		for (Output output : outputs) {
			it.outputs.add(output.copy(it));
		}
		
		// build maps
		for (Input input : it.inputs) {
			for (Element el : it.subCircuit.getElements()) {
				if (!(el instanceof InputPin))
					continue;
				InputPin pin = (InputPin)el;
				if (input.getName().equals(pin.getName())) {
					it.inmap.put(input,pin);
				}
			}
		}
		for (Output output : it.outputs) {
			for (Element el : it.subCircuit.getElements()) {
				if (!(el instanceof OutputPin))
					continue;
				OutputPin pin = (OutputPin)el;
				if (output.getName().equals(pin.getName())) {
					it.outmap.put(pin,output);
				}
			}
		}
		
		// finish up
		super.copy(it);
		return it;
	} // end of copy method

	/**
	 * Remove this element from the circuit.
	 * Unattaches from all wire nets.
	 * Forces wire nets it disconnects from to recheck their information.
	 * 
	 * @param circ The circuit it is being removed from.
	 */
	@Override
	public void remove(Circuit circ) {
		
		circ.removeName(name);
		super.remove(circ);
	} // end of remove method
	
	/**
	 * Display info about this element.
	 * 
	 * @param info The JLabel to display with.
	 */
	@Override
	public void showInfo(JLabel info) {
		
		info.setText(subCircuit.getName() + " (a subcircuit)");
	} // end of showInfo method
	
	/**
	 * Set whether elements in the subcircuit are watched.
	 * 
	 * @param which True to set all watchable elements to watched, false to make them
	 *        not watched.
	 */
	@Override
	public void setWatched(boolean which) {
		
		for (Element element : subCircuit.getElements()) {
			if (element instanceof LogicElement) {
				((LogicElement)element).setWatched(which);
			}
		}
	} // end of setWatched method
	
	/**
	 * Reset propagation delays of all elements in the subcircuit.
	 */
	@Override
	public void resetPropDelay() {
		
		subCircuit.resetAllDelays();
	} // end of resetPropDelay method

	/**
	 * Subcircuits can be changed.
	 * 
	 * @return true.
	 */
	@Override
	public boolean canChange() {
		
		return true;
	} // end of canChange method
	
	/**
	 * Remap all input and output pins to inputs and outputs.
	 * Used by undo/redo (finishDo).
	 * 
	 * @param circ The new circuit containing new versions of the input and output pins.
	 */
	public void remapPins(Circuit circ) {
		
		inmap.clear();
		outmap.clear();
		for (Element el : circ.getElements()) {
			if (el instanceof InputPin) {
				InputPin pin = (InputPin)el;
				for (Input in : inputs) {
					if (in.getName().equals(pin.getName())) {
						inmap.put(in,pin);
					}
				}
			}
			else if (el instanceof OutputPin) {
				OutputPin pin = (OutputPin)el;
				for (Output out : outputs) {
					if (out.getName().equals(pin.getName())) {
						outmap.put(pin,out);
					}
				}
			}
		}
	} // end of remapPins method
	
	// used by setTriState to avoid infinite recursion when an
	// input of this elements is connected to a tri-state propagating output
	private Set<InputPin> pinsChecked = new HashSet<InputPin>();
	
	/**
	 * Set all subcircuit input pins to tri-state or not.
	 * 
	 * @param which True to set to tri-state, false otherwise.
	 */
	@Override
	public void setTriState(boolean which) {
		
		for (Input input : inputs) {
			InputPin p = inmap.get(input);
			
			// if already checked, don't do it again
			if (pinsChecked.contains(p))
				continue;
			
			if (!input.isAttached()) {
				
				// unattached are not tri-state, so propagate false
				pinsChecked.add(p);
				p.setTriState(false);
				pinsChecked.remove(p);
			}
			else {
				
				// attached can be either, so find out which and propagate
				WireEnd w = input.getWireEnd();
				boolean is = w.isTriState();
				pinsChecked.add(p);
				p.setTriState(is);
				pinsChecked.remove(p);
			}
		}
	} // end of setTriState method
	
	/**
	 * See if this element can be flipped.
	 * It can be if none of its inputs or outputs are attached yet.
	 * 
	 * @return true if it can be, false if not.
	 */
	@Override
	public boolean canFlip() {
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
	} // end of canFlip method
	
	/**
	 * Flip this element in the display.
	 * 
	 * @param g The Graphics object used to draw this element.
	 */
	@Override
	public void flip(Graphics g) {
		if(orientation == JLSInfo.Orientation.LEFT)
		{
			orientation = JLSInfo.Orientation.RIGHT;
		}
		else if(orientation == JLSInfo.Orientation.RIGHT)
		{
			orientation = JLSInfo.Orientation.LEFT;
		}
		inputs.clear();
		outputs.clear();
		width = 0;
		height = 0;
		init(g);
	} // end of flip method

	/**
	 * Dialog box to give the subcircuit a name within this circuit.
	 */
	private class SubCreate extends ElementDialog {
			// properties
			private JTextField nameField = new JTextField("",12);
			private JRadioButton left = new JRadioButton("Left");
			private JRadioButton right = new JRadioButton("Right",true);
			
			/**
			 * Set up dialog window.
			 * 
			 */
			private SubCreate() {

				// set up window title
				super("Create Subcircuit","import");

				// set not cancelled
				cancelled = false;

				// set up window
				Container window = getContentPane();

				// set up input
				JPanel info = new JPanel(new BorderLayout());
				JLabel name = new JLabel("Name: ",SwingConstants.RIGHT);
				info.add(name,BorderLayout.WEST);
				info.add(nameField,BorderLayout.CENTER);
				window.add(info);
				
				//Setup orientation radio buttons
				JLabel olbl = new JLabel("Orientation");
				olbl.setAlignmentX(Component.CENTER_ALIGNMENT);
				window.add(olbl);
				JPanel orients = new JPanel(new GridLayout(3,3));
				orients.add(new JLabel(""));
				orients.add(new JLabel(""));
				orients.add(new JLabel(""));
				orients.add(left);
				orients.add(new JLabel(""));
				orients.add(right);
				orients.add(new JLabel(""));
				orients.add(new JLabel(""));
				orients.add(new JLabel(""));
				left.setHorizontalAlignment(SwingConstants.CENTER);
				right.setHorizontalAlignment(SwingConstants.CENTER);
				ButtonGroup gr = new ButtonGroup();
				gr.add(left);
				gr.add(right);
				window.add(orients);

				confirmOnEnter(nameField);
				finishDialog();
			} // end of constructor

			/**
			 * Validate the form and name the subcircuit.
			 */
			@Override
			protected void validateAndAccept() {

				String tname = nameField.getText().trim();
				if (tname.length() < 1 || !Util.isValidName(tname)) {
					reject("Invalid name");
					return;
				}
				if (!circuit.addName(tname)) {
					reject("Name already used");
					return;
				}
				if(left.isSelected())
				{
					orientation = JLSInfo.Orientation.LEFT;
				}
				else if(right.isSelected())
				{
					orientation = JLSInfo.Orientation.RIGHT;
				}
				name = tname;
				subCircuit.setName(name);
				dispose();
			} // end of validateAndAccept method

			/**
			 * Cancel this pin.
			 */
			@Override
			protected void cancelDialog() {

				cancelled = true;
				dispose();
			} // end of cancelDialog method

		} // end of SubCreate class
	
//	-------------------------------------------------------------------------------
//	Simulation
//	-------------------------------------------------------------------------------
	
	/**
	 * Initialize inputs of this element, and of all elements in the subcircuit,
	 * to 0.
	 */
	@Override
	public void initInputs() {
		
		// initialize this element's inputs
		super.initInputs();
		
		// initialize subcircuit's elements
		for (Element element : subCircuit.getElementsInStableOrder()) {
			if (element instanceof LogicElement) {
				LogicElement el = (LogicElement)element;
				el.initInputs();
			}
		}

	} // end of initInputs method
	
	/**
	 * Initialize this element by setting all output pins of this element to 0 or null
	 * and initializing all elements in the subcircuit.
	 * 
	 * @param sim The simulator for this circuit.
	 */
	@Override
	public void initSim(Simulator sim) {
		
		// set all output pins
		for (Output out : outputs) {
			if (out.isTriState())
				out.setValue(null);
			else
				out.setValue(new BitSet());
		}
		
		// canonical seed order at every nesting depth (#181): the inner
		// circuit's time-0 events must be posted in stable-id order for
		// the same reason as Simulator.initSimulation's top-level walk
		for (Element el : subCircuit.getElementsInStableOrder()) {
			if (el instanceof LogicElement) {
				LogicElement lel = (LogicElement)el;
				lel.initSim(sim);
			}
		}

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

		// send all input values to input pins of subcircuit
		for (Input in : inputs) {
			InputPin pin = inmap.get(in);
			BitSet copy = null;
			if (in.getValue() != null)
				copy = (BitSet)in.getValue().clone();
			sim.post(new SimEvent(now,pin,copy));
		}
		
	} // end of react method
	
	/**
	 * Send value to the output corresponding to given OutputPin.
	 * 
	 * @param pin The output pin.
	 * @param value The value.
	 * @param now The current time.
	 * @param sim The simulator.
	 */
	public void send(OutputPin pin, BitSet value, long now, Simulator sim) {
		
		Output out = outmap.get(pin);
		out.propagate(value,now,sim);
	} // end of send method
	
} // end of SubCircuit class