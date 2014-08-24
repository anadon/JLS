package jls.elem;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.io.PrintWriter;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import jls.BitSetUtils;
import jls.Circuit;
import jls.JLSInfo;
import jls.sim.SimEvent;
import jls.sim.Simulator;

/**
 * The state machine editor and simulation code.
 * 
 * @author David A. Poplawski
 */
public final class StateMachine extends LogicElement implements Printable {

	// default values
	private static final int defaultPropDelay = 30; 
	private static int defaultTrigger = 1;	// 1=rising, -1=falling
	
	// saved properties
	private int propDelay = defaultPropDelay;
	private Set<State> states = new HashSet<State>();
	private int trigger = defaultTrigger;
	private String name = "";
	
	// for loading from file
	private enum LoadState {machine, newState, newOutput, newTransition};
	private LoadState loadState = LoadState.machine;
	private State buildState;
	
	// running properties
	private boolean canceled;
	private JDialog currentDialog;
	private Rectangle bounds;
	private StateMachine original;

	/**
	 * Create a new adder element.
	 * 
	 * @param circuit The circuit this element is part of.
	 */
	public StateMachine(Circuit circuit) {
		
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
	 * @return false if canceled, true otherwise.
	 */
	public boolean setup(Graphics g, JPanel editWindow, int x, int y) {
		
		// show creation dialog
		new StateEditor(this,true);
		
		// don't do anything if user canceled
		if (canceled)
			return false;
		
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
	 * @param g The Graphics object to use.
	 */
	public void init(Graphics g) {

		// determine width if needed
		int s = JLSInfo.spacing;
		if (g != null) {
			if (width == 0 && height == 0) {
				FontMetrics fm = g.getFontMetrics();
				String dname = name;
				if (name.equals("")) 
					dname = "State Machine";
				width = fm.stringWidth(" " + dname + " ");
				for (State state : states) {
					width = Math.max(width,state.getWidthInfo(fm));
				}
				width = Math.max(width,fm.stringWidth("clock"));
				width = (width+s-1)/s*s+s;
			}
		}

		// create sorted lists of input and output signals
		SortedSet<String> inputList = new TreeSet<String>();
		SortedSet<String> outputList = new TreeSet<String>();
		for (State state : states) {
			inputList.addAll(state.getInputs());
			outputList.addAll(state.getOutputs());
		}
		
		// create new list alternating elements from the two lists
		Set<String>	saveInputs = new HashSet<String>(inputList);
		Vector<String> pins = new Vector<String>(inputList.size()+outputList.size());
		boolean takeFromInput = true;
		while (inputList.size()+ outputList.size() > 0) {
			if (takeFromInput) {
				takeFromInput = false;
				if (!inputList.isEmpty()) {
					String pin = inputList.first();
					pins.add(pin);
					inputList.remove(pin);
				}
			}
			else {
				takeFromInput = true;
				if (!outputList.isEmpty()) {
					String pin = outputList.first();
					pins.add(pin);
					outputList.remove(pin);
				}
			}
		}
		
		// create input and output signals and determine height
		height = s;
		for (String signal : pins) {
			if (saveInputs.contains(signal)) {
				Input in = new Input(signal,this,0,height,inputBits(signal));
				inputs.add(in);
				height += s;
			}
			else {
				Output out = new Output(signal,this,width,height,outputBits(signal));
				outputs.add(out);
				height += s;
			}
		}
		height += 4*s;
		
		inputs.add(new Input("clock",this,0,height-s,1));
		
	} // end of init method
	
	/**
	 * Determine number of bits in a given input.
	 * 
	 * @param signal The input signal name.
	 * 
	 * @return The number of bits in that signal.
	 */
	private int inputBits(String signal) {
		
		for (State state : states) {
			int bits = state.inputBits(signal);
			if (bits > 0)
				return bits;
		}
		return 0; // should never do this
	} // end of inputBits method

	/**
	 * Determine number of bits in a given output.
	 * 
	 * @param signal The output signal name.
	 * 
	 * @return The number of bits in that signal.
	 */
	private int outputBits(String signal) {
		
		for (State state : states) {
			int bits = state.outputBits(signal);
			if (bits > 0)
				return bits;
		}
		return 0; // should never do this
	} // end of inputBits method
	
	/**
	 * Draw this statemachine element.
	 * 
	 * @param g The graphics object to draw with.
	 */
	public void draw(Graphics g) {
		
		// set up
		int s = JLSInfo.spacing;
		int d2 = JLSInfo.pointDiameter/2;
		FontMetrics fm = g.getFontMetrics();
		int ascent = fm.getAscent();
		int descent = fm.getDescent();
		int fontHeight = ascent + descent;
		
		// draw context
		super.draw(g);
		
		// draw box
		g.setColor(Color.BLACK);
		g.drawRoundRect(x,y,width,height,6,6);
		g.drawLine(x,y+height-4*s,x+width,y+height-4*s);
		g.drawLine(x,y+height-2*s,x+width,y+height-2*s);
		
		// draw name
		String dname = name;
		if (name.equals("")) {
			dname = "State Machine";
		}
		int w = fm.stringWidth(dname);
		g.drawString(dname,x+(width-w)/2,y+height-3*s-fontHeight/2+ascent);
		
		// draw inputs and outputs and their names
		for (Input input : inputs) {
			int dy = input.getY();
			g.setColor(Color.black);
			g.drawString(input.getName(),x+d2,dy-fontHeight/2+ascent);
			input.draw(g);
		}
		for (Output output : outputs) {
			int dy = output.getY();
			int ow = fm.stringWidth(output.getName());
			g.setColor(Color.black);
			g.drawString(output.getName(),x+width-ow-d2,dy-fontHeight/2+ascent);
			output.draw(g);
		}
		
	} // end of draw method

	/**
	 * Print the state machine.
	 */
	public int print(Graphics g, PageFormat format, int pagenum) {
		
		// use better graphics
		Graphics2D gg = (Graphics2D)g;

		// construct name
		Circuit c = circuit;
		String nm = name + " in " + circuit.getName();
		while (c.isImported()) {
			c = c.getSubElement().getCircuit();
			nm += " in " + c.getName();
		}
		
		// set up
		FontMetrics fm = g.getFontMetrics();
		int ascent = fm.getAscent();
		int descent = fm.getDescent();
		int fontHeight = ascent + descent;
		
		// get bounds
		bounds = null;
		for (State state : states) {
			if (bounds == null)
				bounds = state.getBounds(g);
			else
				bounds.add(state.getBounds(g));
		}
		if (bounds == null) {
			bounds = new Rectangle(0,0,1,1);
		}
		
		// scale and adjust as needed
		double width = format.getImageableWidth();
		double height = format.getImageableHeight();
		double scale = 1.0;
		if (bounds.width > width) {
			scale = 1.0*width/bounds.width;
		}
		if (bounds.height > height) {
			scale = Math.min(scale,1.0*height/bounds.height);
		}
		gg.translate(format.getImageableX(),format.getImageableY());
		gg.drawString(nm,0,ascent);
		gg.translate(0,2*fontHeight);
		gg.scale(scale,scale);
		gg.translate(-bounds.x,-bounds.y);
		
		// print
		for (State state : states) {
			state.draw(gg);
		}
		return Printable.PAGE_EXISTS;
	} // end of print method

	/**
	 * Save this element in a file.
	 * 
	 * @param output The PrintWriter to write to.
	 */
	public void save(PrintWriter output) {
		
		output.println("ELEMENT StateMachine");
		super.save(output);
		output.println(" String name \"" + name + "\"");
		output.println(" int delay " + propDelay);
		output.println(" int trig " + trigger);
		for (State state : states) {
			state.save(output);
		}
		output.println("END");
	} // end of save method

	/**
	 * Set a String instance variable value (during a load).
	 * 
	 * @param name The instance variable name.
	 * @param value The instance variable value.
	 */
	public void setValue(String name, String value) {
		
		switch (loadState) {
		case machine:
			if (name.equals("name")) {
				this.name = value;
			} else if (name.equals("state")) {
				loadState = LoadState.newState;
				buildState = new State(this,value,null);
				states.add(buildState);
				buildState.fixTrans();
			} else {
				super.setValue(name,value);
			}
			break;
		case newState:
			if (name.equals("state")) {
				loadState = LoadState.newState;
				buildState = new State(this,value,null);
				states.add(buildState);
				buildState.fixTrans();
			}
			else if (name.equals("output")) {
				loadState = LoadState.newOutput;
				buildState.setOutputValue(name,value);
			}
			else if (name.equals("trans")) {
				loadState = LoadState.newTransition;
				buildState.setTransValue(name,value);
			}
			break;
		case newOutput:
			if (name.equals("state")) {
				loadState = LoadState.newState;
				buildState = new State(this,value,null);
				states.add(buildState);
				buildState.fixTrans();
			}
			else if (name.equals("output")) {
				loadState = LoadState.newOutput;
				buildState.setOutputValue(name,value);
			}
			else if (name.equals("trans")) {
				loadState = LoadState.newTransition;
				buildState.setTransValue(name,value);
			}
			break;
		case newTransition:
			if (name.equals("state")) {
				loadState = LoadState.newState;
				buildState = new State(this,value,null);
				states.add(buildState);
				buildState.fixTrans();
			}
			else if (name.equals("output")) {
				loadState = LoadState.newOutput;
				buildState.setOutputValue(name,value);
			}
			else if (name.equals("trans") || name.equals("next")) {
				loadState = LoadState.newTransition;
				buildState.setTransValue(name,value);
			}
			break;
		}
	} // end of setValue (string) method

	/**
	 * Set an int instance variable value (during a load).
	 * 
	 * @param name The instance variable name.
	 * @param value The instance variable value.
	 */
	public void setValue(String name, int value) {
		
		switch (loadState) {
		case machine:
			if (name.equals("trig")) {
				trigger = value;
			} else if (name.equals("delay")) {
				propDelay = value;
			} else {
				super.setValue(name,value);
			}
			break;
		case newState:
			buildState.setValue(name,value);
			break;
		case newOutput:
			buildState.setOutputValue(name,value);
			break;
		case newTransition:
			buildState.setTransValue(name,value);
			break;
		}
		
	} // end of setValue (int) method

	/**
	 * Set a long instance variable value (during a load).
	 * 
	 * @param name The instance variable name.
	 * @param value The instance variable value.
	 */
	public void setValue(String name, long value) {
		
		switch (loadState) {
		case newOutput:
			buildState.setOutputValue(name,value);
			break;
		}
		
	} // end of setValue (long) method

	/**
	 * Set a pair of int instance variable values (during a load).
	 * 
	 * @param v1 The first value.
	 * @param v1 The second value.
	 */
	public void setPair(int v1, int v2) {
		
		buildState.setTransPair(v1,v2);
	} // end of setPair method

	/**
	 * Can't copy state machines.
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
		
		// create new element
		StateMachine it = new StateMachine(circuit);
		
		// set basic info
		it.name = new String(name);
		it.propDelay = propDelay;
		it.trigger = trigger;
		
		// copy all the states
		Map<String,State> stateMap = new HashMap<String,State>();
		for (State state : states) {
			State st = state.copy(it);
			it.states.add(st);
			stateMap.put(state.getName(),st);
		}
		
		// link transitions to states
		for (State istate : it.states) {
			istate.linkTrans(it,stateMap);
		}
		
		// copy inputs and outputs
		for (Input input : inputs) {
			it.inputs.add(input.copy(it));
		}
		for (Output output : outputs) {
			it.outputs.add(output.copy(it));
		}
		
		// finish up
		super.copy(it);
		return it;
	} // end of copy method
	
	/**
	 * Display info about this element.
	 * 
	 * @param info The JLabel to display with.
	 */
	public void showInfo(JLabel info) {
		
		String trig = "falling";
		if (trigger == 1) {
			trig = "rising";
		}
		String msg = "state machine (" + trig + " edge triggered), ";
		if (currentState == null) {
			info.setText(msg + "no current state");
		}
		else {
			info.setText(msg + "current state = " + currentState.getName());
		}
	} // end of showInfo method
	
	/**
	 * Get the name of this state machine.
	 * 
	 * @return the name.
	 */
	public String getName() {
		
		return name;
	} // end of getName method
	
	/**
	 * Get the set of states in this state machine.
	 * 
	 * @return the set of states.
	 */
	Set<State> getStates() {
		
		return states;
	} // end of getStates method

	/**
	 * Get all inputs from this state machine.
	 * 
	 * @return all outputs.
	 */
	Vector<Input> getInputs() {
		
		return inputs;
	} // end of getInputs method
	
	/**
	 * Get all outputs from this state machine.
	 * 
	 * @return all outputs.
	 */
	Vector<Output> getOutputs() {
		
		return outputs;
	} // end of getOutputs method

	/**
	 * State machines have timing info (propagation delay).
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
	 * State machines can be modified.
	 * 
	 * @return true.
	 */ 
	public boolean canChange() {
		
		return true;
	} // end of canChange method
	
	/**
	 * Show edit dialog.
	 * When done, make sure inputs and outputs are still the same, and that the name
	 * is still the same.
	 * If not, detach existing element from all wires and go into "moving" editor
	 * state.
	 * 
	 * @param g The Graphics object to use to determine size.
	 * @param editWindow The editor window this element is in.
	 * @param x The current x-coordinate of the mouse.
	 * @param y The current y-coordinate of the mouse.
	 * 
	 * @return true if element must be re-placed in the circuit, false if not.
	 */
	public boolean change(Graphics g, JPanel editWindow, int x, int y) {
	
		// save current state machine
		original = (StateMachine)copy();
		
		// display dialog
		StateEditor ed = new StateEditor(this,false);
		
		// if canceled, restore original state machine
		if (canceled) {
			states = original.states;
		}
		
		// if name has changed, detach
		if (ed.nameChanged()) {
			detach();
			width = 0;
			height =  0;
			init(g);
			return true;
		}
		
		// make a set of all old input names
		Set<String> oldNames = new HashSet<String>();
		for (Input input : inputs) {
			oldNames.add(input.getName());
		}
		
		// make a set of all new input names
		Set<String> newNames = new HashSet<String>();
		newNames.add("clock");
		for (State state : states) {
			newNames.addAll(state.getInputs());
		}
		
		// if not the same, detach
		if (!oldNames.equals(newNames)) {
			detach();
			width = 0;
			height =  0;
			init(g);
			return true;
		}
		
		// detach if any input signal's #bits changed
		for (Input input : inputs) {
			int bits = input.getBits();
			for (State state : states) {
				for (String inp : state.getInputs()) {
					if (input.getName().equals(inp) && state.inputBits(inp) != bits) {
						detach();
						width = 0;
						height =  0;
						init(g);
						return true;
					}
				}
			}
		}

		// make a set of all old output names
		oldNames.clear();
		for (Output output : outputs) {
			oldNames.add(output.getName());
		}
		
		// make a set of all new output names
		newNames.clear();
		for (State state : states) {
			newNames.addAll(state.getOutputs());
		}

		// if not the same, detach
		if (!oldNames.equals(newNames)) {
			detach();
			width = 0;
			height =  0;
			init(g);
			return true;
		}

		// detach if any out signal's #bits changed
		for (Output output : outputs) {
			int bits = output.getBits();
			for (State state : states) {
				for (String out : state.getOutputs()) {
					if (output.getName().equals(out) && state.outputBits(out) != bits) {
						detach();
						width = 0;
						height =  0;
						init(g);
						return true;
					}
				}
			}
		}
		
		return false;
	
	} // end of change method
	
	
	// drawing states
	private enum DrawState {idle, created, placing, selecting, selected, moving,
		newTrans, pointMoving};
	
	/**
	 * Create dialog.
	 */
	private class StateEditor extends JDialog
		implements ActionListener, MouseListener, MouseMotionListener {
		
		// properties
		private JPanel editArea;
		private JLabel msgLabel = new JLabel("");
		private JLabel stateLabel = new JLabel(" ");
		private DrawState drawState = DrawState.idle;
		private int x;
		private int y;
		private Rectangle selrect = null;
		private Set<State> selected = new HashSet<State>();
		private State on;
		private JRadioButton rising = new JRadioButton("rising edge");
		private JRadioButton falling = new JRadioButton("falling edge");
		private JButton enlarge = new JButton("+");
		private JTextField nameField = new JTextField(30);
		private JButton ok = new JButton("ok");
		private JButton cancel = new JButton("cancel");
		private JMenuItem changeName = new JMenuItem("change name");
		private JMenuItem makeInit = new JMenuItem("make initial state");
		private JMenuItem addTrans = new JMenuItem("add transition");
		private JMenuItem deleteAllTrans = new JMenuItem("delete all transitions");
		private JMenuItem editOutputs = new JMenuItem("edit outputs");
		private JMenuItem showOutputs = new JMenuItem("view outputs");
		private JMenuItem delete = new JMenuItem("delete state(s)");
		private JMenuItem alignHor = new JMenuItem("align states horizontally");
		private JMenuItem alignVer = new JMenuItem("align states vertically");
		private Vector<Point> points = new Vector<Point>();
		private Point movingPoint;
		private boolean nameChange;
		private StateMachine machine;

		/**
		 * Set up create dialog window.
		 * 
		 * @param machine The state machine the new state will be in.
		 * @param creating True if creating a new element, false if editing an existing one.
		 */
		private StateEditor(StateMachine machine, boolean creating) {
			
			// set up window title
			super(JLSInfo.frame,"Edit State Machine",true);

			// save reference to me
			currentDialog = this;
			this.machine = machine;
			
			// set not canceled
			canceled = false;
			
			// set up window
			Container window = getContentPane();
			window.setLayout(new BorderLayout());
			
			// set up message area
			JPanel messages = new JPanel(new BorderLayout());
			messages.setBackground(Color.cyan);
			messages.add(msgLabel,BorderLayout.CENTER);
			messages.add(stateLabel,BorderLayout.EAST);
			window.add(messages,BorderLayout.NORTH);
			
			// set up editing area
			editArea = new JPanel() {
				public void paintComponent(Graphics g) {
					super.paintComponent(g);
					if (selrect != null) {
						if (drawState == DrawState.selecting) {
							g.setColor(Color.GRAY);
							g.drawRect(selrect.x,selrect.y,selrect.width,selrect.height);
						}
						else {
							g.setColor(Color.lightGray);
							g.fillRect(selrect.x,selrect.y,selrect.width,selrect.height);
						}
					}
					if (drawState == DrawState.newTrans) {
						drawTrans(on,points,g);
					}
					for (State state : states) {
						state.draw(g);
					}
				}
			};
			if (creating) {
				Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
				editArea.setPreferredSize(new Dimension(screen.width-100,screen.height-100));
			}
			else {
				Rectangle es = null;
				for (State state : states) {
					if (es == null)
						es = state.getBounds(null);
					else
						es.add(state.getBounds(null));
				}
				if (es != null) {
					int w = (int)((es.x+es.width)*1.1);
					int h = (int)((es.y+es.height)*1.1);
					editArea.setPreferredSize(new Dimension(w,h));
				}
				else {
					Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
					editArea.setPreferredSize(new Dimension(screen.width-100,screen.height-100));
				}
			}
			editArea.setBackground(Color.white);
			editArea.addMouseListener(this);
			editArea.addMouseMotionListener(this);
			JScrollPane pane = new JScrollPane(editArea);
			window.add(pane,BorderLayout.CENTER);
			
			// set up bottom stuff
			JPanel bottom = new JPanel(new GridLayout(3,1));
			
			JPanel stuff = new JPanel(new BorderLayout());
			
			JPanel trig = new JPanel(new FlowLayout(FlowLayout.CENTER));
			trig.add(new JLabel("Trigger: "));
			trig.add(rising);
			trig.add(falling);
			ButtonGroup group = new ButtonGroup();
			group.add(rising);
			group.add(falling);
			if (trigger == 1)
				rising.setSelected(true);
			else
				falling.setSelected(true);
			stuff.add(trig,BorderLayout.CENTER);
			stuff.add(enlarge,BorderLayout.EAST);
			bottom.add(stuff);
			enlarge.setToolTipText("expand drawing area");
			
			JPanel namePanel = new JPanel(new FlowLayout());
			namePanel.add(new JLabel("Name: "));
			namePanel.add(nameField);
			nameField.setText(name);
			bottom.add(namePanel);
			
			JPanel okCancel = new JPanel(new GridLayout(1,3));
			ok.setBackground(Color.green);
			okCancel.add(ok);
			cancel.setBackground(Color.pink);
			okCancel.add(cancel);
			JButton help = new JButton("Help");
			if (JLSInfo.hb == null)
				jls.Util.noHelp(help);
			else
				JLSInfo.hb.enableHelpOnButton(help,"stmach",null);
			okCancel.add(help);
			bottom.add(okCancel);
			getRootPane().setDefaultButton(ok);
			
			window.add(bottom,BorderLayout.SOUTH);
			
			// set up listeners
			enlarge.addActionListener(this);
			ok.addActionListener(this);
			cancel.addActionListener(this);
			changeName.addActionListener(this);
			makeInit.addActionListener(this);
			addTrans.addActionListener(this);
			deleteAllTrans.addActionListener(this);
			editOutputs.addActionListener(this);
			showOutputs.addActionListener(this);
			delete.addActionListener(this);
			alignHor.addActionListener(this);
			alignVer.addActionListener(this);

			// set up new state key binding
			Action newState = new AbstractAction() {
				public void actionPerformed(ActionEvent event) {
					
					createNewState();
				}
			};
			editArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_N,InputEvent.CTRL_MASK),"new state");
			editArea.getActionMap().put("new state", newState);

			// set up window close listener to cancel element
			addWindowListener (
					new WindowAdapter() {
						public void windowClosing(WindowEvent e) {
							cancel();
						}
					}
			);
			
			// finish up GUI
			Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
			Dimension dialog = getPreferredSize();
			int w = Math.min(dialog.width,screen.width-100);
			int h = Math.min(dialog.height+100,screen.height-100);
			setSize(w,h);
			setLocation(50,50);
			setVisible(true);
			
		} // end of constructor
		
		/**
		 * Set and display current drawing state.
		 * 
		 * @param state The new drawing state.
		 */
		public void setDrawState(DrawState state) {
			
			drawState = state;
			switch(state) {
			case idle:
				stateLabel.setText(" ");
				break;
			case created:
				stateLabel.setText("created new state");
				break;
			case placing:
				stateLabel.setText("left click to place, right click to cancel");
				break;
			case selecting:
				stateLabel.setText("selecting states");
				break;
			case selected:
				stateLabel.setText("selected states");
				break;
			case moving:
				stateLabel.setText("moving state(s)");
				break;
			case newTrans:
				stateLabel.setText("creating new transition");
				break;
			case pointMoving:
				stateLabel.setText("moving a transition corner");
			}
		} // end of setDrawState method
		
		/**
		 * Draw a partial transition.
		 * Does not draw arrow, used during transition creation.
		 * 
		 * @param from Starting state.
		 * @param points Midpoints, last entry is current end point.
		 * @param g The Graphics object to use.
		 */
		public void drawTrans(State from, Vector<Point> points, Graphics g) {
			
			Point last = from.getLocation();
			Point prev = null;
			for (Point p : points) {
				g.setColor(Color.black);
				g.drawLine(last.x,last.y,p.x,p.y);
				prev = last;
				last = p;
			}
			SMUtil.drawArrow(last.x,last.y,SMUtil.getAngle(last.x-prev.x,prev.y-last.y),g);
		} // end of drawTrans method
		
		/**
		 * React to events.
		 * 
		 * @param event The event.
		 */
		public void actionPerformed(ActionEvent event) {
			
			if (event.getSource() == ok) {
				if (rising.isSelected()) {
					trigger = 1;
				}
				else {
					trigger = -1;
				}
				String oldName =  name;
				name = nameField.getText().trim();
				if (oldName.equals(name)) {
					nameChange = false;
				}
				else {
					nameChange = true;
				}
				dispose();
			}
			else if (event.getSource() == cancel) {
				cancel();
			}
			else if (event.getSource() == changeName) {
				Point win = editArea.getLocationOnScreen();
				CreateState cs = new CreateState(x+win.x,y+win.y,"Change",on.getName());
				if (!cs.wasCancelled()) {
					boolean ok = on.changeName(cs.getName(),editArea.getGraphics());
					if (!ok) {
						JOptionPane.showMessageDialog(this,
								"Name won't fit", "Error",
								JOptionPane.ERROR_MESSAGE);
						return;
					}
					editArea.repaint();
				}
			}
			else if (event.getSource() == makeInit) {
				
				// turn off initial state anywhere else
				for (State state : states) {
					state.setInitial(false);
				}
				
				// make current state initial
				on.setInitial(true);
				setDrawState(DrawState.idle);
				editArea.repaint();
			}
			else if (event.getSource() == addTrans) {
				points.clear();
				points.add(new Point(x,y));
				setDrawState(DrawState.newTrans);
			}
			else if (event.getSource() == deleteAllTrans) {
				on.deleteAllTrans();
				setDrawState(DrawState.idle);
				editArea.repaint();
			}
			else if (event.getSource() == editOutputs) {
				on.editOuts(currentDialog);
			}
			else if (event.getSource() == showOutputs) {
				on.showOuts(getMousePosition(),currentDialog);
			}
			else if (event.getSource() == delete) {
				if (on != null) {
					
					// remove all transitions to this state
					for (State state : states) {
						state.removeTrans(on);
					}
					
					// remove this state and its transitions
					states.remove(on);
					
					// finish up
					selrect = null;
					setDrawState(DrawState.idle);
					editArea.repaint();
				}
				else {
					
					// remove every selected state
					for (State state : selected) {
						
						// remove transitions to each state first
						for (State st : states) {
							st.removeTrans(state);
						}
						
						// then remove the each state
						states.remove(state);
					}
					
					// finish up
					selrect = null;
					setDrawState(DrawState.idle);
					editArea.repaint();
				}
			}
			else if (event.getSource() == alignHor) {

				// find average x-value
				int sum = 0;
				for (State state : selected) {
					sum += state.getX();
				}
				int newx = sum/selected.size();
				
				// move states
				for (State state : selected) {
					state.savePosition();
					state.setX(newx);
				}
				
				// check for overlaps
				if (stateOverlap()) {
					JOptionPane.showMessageDialog(this,"Can't do - states will overlap");
					for (State state : selected) {
						state.restorePosition();
					}
				}

				// finish up
				selected.clear();
				selrect = null;
				setDrawState(DrawState.idle);
				editArea.repaint();
			}
			else if (event.getSource() == alignVer) {
				
				// find average y-value
				int sum = 0;
				for (State state : selected) {
					sum += state.getY();
				}
				int newy = sum/selected.size();
				
				// move states
				for (State state : selected) {
					state.savePosition();
					state.setY(newy);
				}

				// check for overlaps
				if (stateOverlap()) {
					JOptionPane.showMessageDialog(this,"Can't do - states overlap");
					for (State state : selected) {
						state.restorePosition();
					}
				}

				// finish up
				selected.clear();
				selrect = null;
				setDrawState(DrawState.idle);
				editArea.repaint();
			}
			else if (event.getSource() == enlarge) {
				Dimension cs = editArea.getPreferredSize();
				int width = (int)(cs.width*1.1);
				int height = (int)(cs.height*1.1);
				editArea.setPreferredSize(new Dimension(width,height));
				editArea.revalidate();
			}
			
		} // end of actionPerformed method
		
		/**
		 * See if any states in the selected set overlap any state.
		 * 
		 * @return true if there is overlap, false if not
		 */
		private boolean stateOverlap() {
			
			for (State sel : selected) {
				for (State state : states) {
					if (sel == state)
						continue;
					if (sel.overlaps(state)) {
						return true;
					}
				}
			}
			return false;
		} // end of stateOverlap method

		/**
		 * Cancel this dialog.
		 */
		private void cancel() {
			
			canceled = true;
			dispose();
		} // end of cancel method
		
		/**
		 * See if the name of this element has changed.
		 * 
		 * @return true if the name has changed, false if it hasn't.
		 */
		public boolean nameChanged() {
			
			return nameChange;
		} // end of nameChanged method
		
		/**
		 * React to mouse pressed events.
		 * 
		 * @param event The mouse event.
		 */
		public void mousePressed(MouseEvent event) {
			
			// get info
			x = event.getX();
			y = event.getY();
			boolean leftButton = event.getButton() == MouseEvent.BUTTON1;
			boolean rightButton = event.getButton() == MouseEvent.BUTTON3;
			
			if (drawState == DrawState.idle) {
				
				// if left button
				if (leftButton) {
					
					// see if a transition point is at x,y
					for (State state : states) {
						Point p = state.highlightTransPoints(x,y);
						if (p != null) {
							movingPoint = p;
							setDrawState(DrawState.pointMoving);
							return;
						}
					}
					
					// see if a state is at x,y
					selected.clear();
					for (State state : states) {
						if (state.contains(x,y)) {
							state.setHighlight(true);
							selected.add(state);
							state.savePosition();
						}
					}
					
					// if no state there, then start selection rectangle
					if (selected.isEmpty()) {
						selrect = new Rectangle(x,y,0,0);
						setDrawState(DrawState.selecting);
						editArea.repaint();
						return;
					}
					
					// otherwise start moving stuff
					else {
						setDrawState(DrawState.moving);
						editArea.repaint();
						return;
					}
				}
				
				// if right button...
				else if (rightButton){
					
					// see if over a transition
					for (State state : states) {
						if (state.highlightTrans(x,y)) {
							int result = JOptionPane.showConfirmDialog(null,
						            "delete transition?", "option",
						            JOptionPane.YES_NO_OPTION);
							if (result == 0) {
								state.deleteLastHighlighted();
								editArea.repaint();
							}
							return;
						}
					}
					
					// see if there is a single state at this position
					on = null;
					for (State state : states) {
						if (state.contains(x,y)) {
							on = state;
							break;
						}
					}
					
					// if so, construct and show state option menu
					if (on != null) {
						JPopupMenu menu = new JPopupMenu();
						menu.add(addTrans);
						menu.add(showOutputs);
						menu.add(editOutputs);
						menu.add(changeName);
						menu.add(makeInit);
						menu.add(deleteAllTrans);
						menu.add(delete);
						menu.show(editArea,x,y);
					}
					
					// otherwise create a new state
					else {
						createNewState();
						return;
					}
				}
			}
			
			// if just created
			if (drawState == DrawState.created) {
				Point p = editArea.getMousePosition();
				if (p != null) {
					for (State state : selected) {
						state.moveTo(p.x,p.y);
					}
				}
				repaint();
				return;
			}
			
			// if placing...
			if (drawState == DrawState.placing) {
				
				// right click cancels
				if (rightButton) {
					setDrawState(DrawState.idle);
					for (State state : selected) {
						states.remove(state);
					}
					selected.clear();
					editArea.repaint();
					return;
				}
			
				// check for overlap
				boolean overlaps = false;
				for (State sel : selected) {
					for (State st : states) {
						if (sel != st && sel.overlaps(st)) {
							overlaps = true;
						}
					}
				}
				if (overlaps) {
					return;
				}
				
				// finish up
				setDrawState(DrawState.idle);
				for (State state : selected) {
					state.setHighlight(false);
				}
				selected.clear();
				editArea.repaint();
				mouseMoved(event);
				return;
			}
			
			// if selected...
			if (drawState == DrawState.selected) {
				
				if (leftButton) {
					if (selrect.contains(x,y)) {
						setDrawState(DrawState.moving);
						selrect = null;
						repaint();
					}
					else {
						setDrawState(DrawState.idle);
						selrect = null;
						mousePressed(event);
					}
				}
				else { // right button

					// see if it is in the selected area
					if (selrect != null && selrect.contains(x,y)) {
						JPopupMenu menu = new JPopupMenu();
						menu.add(delete);
						if (selected.size() > 1) {
							menu.add(alignHor);
							menu.add(alignVer);
						}
						menu.show(editArea,x,y);
					}
				}
			}
			
			// if adding a new transition
			if (drawState == DrawState.newTrans) {
				
				// right button cancels
				if (rightButton) {
					points.clear();
					for (State state : states) {
						state.setHighlight(false);
					}
					setDrawState(DrawState.idle);
					editArea.repaint();
					return;
				}
				
				// if not on a state, add point
				State target = null;
				for (State state : states) {
					if (state.contains(x,y)) {
						target = state;
						break;
					}
				}
				if (target == null) {
					points.add(new Point(x,y));
					editArea.repaint();
					return;
				}
				
				// otherwise create new transition
				on.newTransition(target,points,currentDialog);
				points.clear();
				on.setHighlight(false);
				setDrawState(DrawState.idle);
				editArea.repaint();
				return;
			}
			
		} // end of mousePressed method
		
		/**
		 * Create a new state.
		 */
		private void createNewState() {
			
			Point win = editArea.getLocationOnScreen();
			CreateState cs = new CreateState(x+win.x,y+win.y,"Create","");
			if (cs.wasCancelled()) {
				return;
			}
			State state = new State(machine,cs.getName(),editArea.getGraphics());
			
			// display it at the current mouse position (if possible)
			Point p = editArea.getMousePosition();
			if (p == null) {
				int d = JLSInfo.stateDiameter;
				state.moveTo(-d,-d);	// anywhere off screen
			}
			else {
				state.moveTo(p.x,p.y);
			}
			state.setHighlight(true);
			if (states.isEmpty()) {
				state.setInitial(true);
			}
			states.add(state);
			selected.clear();
			selected.add(state);
			setDrawState(DrawState.created);
			editArea.repaint();
			return;
		} // end of createNewState method

		/**
		 * React to mouse released events.
		 * 
		 * @param event The mouse event.
		 */
		public void mouseReleased(MouseEvent event) {
			
			if (drawState == DrawState.moving || drawState == DrawState.placing) {
				
				// check for overlap
				boolean overlaps = false;
				for (State sel : selected) {
					for (State st : states) {
						if (sel != st && sel.overlaps(st)) {
							overlaps = true;
						}
					}
				}
				if (overlaps) {
					if (drawState == DrawState.placing)
						return;
					for (State sel : selected) {
						sel.restorePosition();
					}
				}
				
				// finish up
				selected.clear();
				setDrawState(DrawState.idle);
				editArea.repaint();
				return;
			}
			
			// if selecting...
			if (drawState == DrawState.selecting) {
				
				// if nothing selected, return to idle
				if (selected.isEmpty()) {
					selrect = null;
					setDrawState(DrawState.idle);
					editArea.repaint();
					return;
				}
				
				// shrink selected rectangle
				selrect = null;
				for (State state : selected) {
					if (selrect == null) {
						selrect = state.getRect();
					}
					else {
						selrect.add(state.getRect());
					}
				}
				setDrawState(DrawState.selected);
				repaint();
				return;
			}
			
			// if moving a point
			if (drawState == DrawState.pointMoving) {
				setDrawState(DrawState.idle);
			}
		} // end of mouseReleased method

		/**
		 * React to mouse moved events.
		 * 
		 * @param event The mouse event.
		 */
		public void mouseMoved(MouseEvent event) {
			
			// get info
			int oldx = x;
			int oldy = y;
			x = event.getX();
			y = event.getY();
			
			// if just moving cursor around...
			if (drawState == DrawState.idle) {
				
				// highlight state cursor is over, unhighlight the rest
				for (State state : states) {
					
					if (state.contains(x,y)) {
						state.setHighlight(true);
					}
					else {
						state.setHighlight(false);
					}
					
					// highlight any transition points from this state too
					state.highlightTransPoints(x,y);
					
					// highlight any transitions from this state too
					state.highlightTrans(x,y);
				}
				editArea.repaint();
				return;
			}
			
			// if just created
			if (drawState == DrawState.created ) {
				Point p = editArea.getMousePosition();
				if (p == null) {
					p = new Point(x,y);
				}
				oldx = p.x;
				oldy = p.y;
				for (State state : selected) {
					state.moveTo(p.x,p.y);
				}
				setDrawState(DrawState.placing);
			}
			
			// if placing or moving
			if (drawState == DrawState.placing || drawState == DrawState.moving) {
				for (State state : selected) {
					state.move(x-oldx,y-oldy,selected);
				}
				editArea.repaint();
			}
			
			// if making a transition
			if (drawState == DrawState.newTrans) {
				
				// update position of last point in points list
				Point p = points.lastElement();
				p.x = x;
				p.y = y;
				
				// highlight state if under cursor
				for (State state : states) {
					if (state == on)
						continue;
					state.setHighlight(false);
					if (state.contains(x,y)) {
						state.setHighlight(true);
						break;
					}
				}
				repaint();
			}
		} // end of mouseMoved method

		/**
		 * React to mouse dragged events.
		 * 
		 * @param event The mouse event.
		 */
		public void mouseDragged(MouseEvent event) {
			
			// if moving
			if (drawState == DrawState.moving) {

				// get info
				int oldx = x;
				int oldy = y;
				x = event.getX();
				y = event.getY();
				
				// move all states that are selected
				for (State state : selected) {
					state.move(x-oldx,y-oldy,selected);
				}
				editArea.repaint();
				return;
			}
			
			// if selecting
			if (drawState == DrawState.selecting) {
				
				// get info
				int nx = event.getX();
				int ny = event.getY();
				
				// modify selection rectangle
				int rx = Math.min(nx,x);
				int ry = Math.min(ny,y);
				int rw = Math.abs(nx-x);
				int rh = Math.abs(ny-y);
				selrect.setBounds(rx,ry,rw,rh);
				
				// highlight selected elements
				selected.clear();
				for (State state : states) {
					state.setHighlight(false);
					if (state.isInside(selrect)) {
						selected.add(state);
						state.setHighlight(true);
						state.savePosition();
					}
				}
				
				// finish up
				editArea.repaint();
				return;
			}
			
			// if moving a transition point
			if (drawState == DrawState.pointMoving) {

				// get info
				x = event.getX();
				y = event.getY();
				
				// if there is a state at the point, don't move there
				for (State state : states) {
					if (state.contains(x,y))
						return;
				}
				
				// move point
				movingPoint.x = x;
				movingPoint.y = y;
				editArea.repaint();
				return;
			}
		} // end of mouseDragged method

		// unused
		public void mouseClicked(MouseEvent event) {}
		public void mouseEntered(MouseEvent event) {}
		public void mouseExited(MouseEvent event) {}
		
	} // end of Create class
	
	/**
	 * Create a new state with a name.
	 */
	private class CreateState extends JDialog implements ActionListener {
		
		private String name;
		private JTextField nameField = new JTextField(10);
		private JButton ok = new JButton("ok");
		private JButton cancel = new JButton("cancel");
		private boolean stateCancelled;
		
		/**
		 * Create a new state.
		 * 
		 * @param xp The x-coordinate of where to show this dialog.
		 * @param yp The y-coordinate of where to show this dialog.
		 * @param title The partial title of this dialog (e.g., "Create" or "Change");
		 */
		public CreateState(int xp, int yp, String title, String currentName) {

			// set up window title
			super(JLSInfo.frame,title + " State",true);
			
			// set not cancelled
			stateCancelled = false;
			
			// set up window
			Container window = getContentPane();
			window.setLayout(new BorderLayout());
			
			// set up name field
			JPanel name = new JPanel(new BorderLayout());
			name.add(new JLabel("Name: ",SwingConstants.RIGHT),BorderLayout.WEST);
			name.add(nameField,BorderLayout.CENTER);
			nameField.setText(currentName);
			window.add(name,BorderLayout.NORTH);
			
			// add listeners
			ok.addActionListener(this);
			cancel.addActionListener(this);
			nameField.addActionListener(this);

			// set up ok and cancel buttons
			JPanel okCancel = new JPanel(new GridLayout(1,2));
			ok.setBackground(Color.green);
			okCancel.add(ok);
			cancel.setBackground(Color.pink);
			okCancel.add(cancel);
			window.add(okCancel,BorderLayout.SOUTH);
			getRootPane().setDefaultButton(ok);

			// set up window close listener to cancel mux
			setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			addWindowListener (
					new WindowAdapter() {
						public void windowClosing(WindowEvent e) {
							stateCancelled = true;
							dispose();
						}
					}
			);
			
			// finish up
			pack();
			setLocation(xp,yp);
			setVisible(true);
			
		} // end of constructor
		
		public void actionPerformed(ActionEvent event) {
			
			if (event.getSource() == ok || event.getSource() == nameField) {
				name = nameField.getText().trim();
				if (name.equals("")) {
					JOptionPane.showMessageDialog(this,
							"Missing name", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				for (State state : states) {
					if (state.getName().equals(name)) {
						JOptionPane.showMessageDialog(this,
								"Duplicate name", "Error",
								JOptionPane.ERROR_MESSAGE);
						return;
					}
				}
				dispose();
			}
			else if (event.getSource() == cancel) {
				stateCancelled = true;
				dispose();
			}
			
		} // end of actionPerformed
		
		/**
		 * Get the name given to the state.
		 * 
		 * @return the name.
		 */
		public String getName() {
			
			return name;
		} // end of getName method
		
		/**
		 * See if new state dialog was cancelled.
		 * 
		 * @return true if it was cancelled, false if not.
		 */
		public boolean wasCancelled() {
			
			return stateCancelled;
		} // end of wasCancelled method
		
	} // end of CreateState class
	
	
	/**
	 * Make a state output summary object for printing.
	 * 
	 * @return the object that will do the printing when asked, or null if no outputs
	 *         in any of the states.
	 */
	public Printable makeOutSum() {
		
		// return null if no outputs
		int outs = 0;
		for (State state : states) {
			outs += state.numOuts();
		}
		if (outs == 0)
			return null;
		
		return new Printable() {
			
			public int print(Graphics g, PageFormat format, int pagenum) {
				
				// set up
				int pageWidth = (int)format.getImageableWidth();
				g.translate((int)(format.getImageableX()),(int)(format.getImageableY()));
				int x = 0;
				int y = 0;
				
				// sort states by name
				SortedMap<String,State> sorted = new TreeMap<String,State>();
				for (State state : states) {
					sorted.put(state.getName(),state);
				}
				
				// print outputs for each state
				int maxHeight = 0;
				SortedSet<Integer> borderX = new TreeSet<Integer>();
				for (String sname : sorted.keySet()) {
					State state = sorted.get(sname);
					
					// determine maximum width and height
					int stateWidth = state.maxWidth(g);
					maxHeight = Math.max(maxHeight,state.maxHeight(g));
					
					// print next state to right unless no more room
					if (x+stateWidth > pageWidth) {
						g.drawLine(0,y+maxHeight+5,x-10,y+maxHeight+5);
						int lines = borderX.size()-1;
						for (int i=0; i<lines; i+=1) {
							int bx = borderX.first();
							g.drawLine(bx,y,bx,y+maxHeight);
							borderX.remove(bx);
						}
						x = 0;
						y += maxHeight+10;
						maxHeight = 0;
						borderX.clear();
					}
					
					// print state name and outputs
					borderX.add(x+stateWidth+5);
					state.print(g,x,y);
					x += stateWidth + 10;
					
				}

				// print vertical borders on last line
				if (borderX.size() > 1) {
					int lines = borderX.size()-1;
					for (int i=0; i<lines; i+=1) {
						int bx = borderX.first();
						g.drawLine(bx,y,bx,y+maxHeight);
						borderX.remove(bx);
					}
				}
				
				return Printable.PAGE_EXISTS;
			}
		};
		
	} // end of makeOutSum method

//	-------------------------------------------------------------------------------
//	Simulation
//	-------------------------------------------------------------------------------
	
	private int oldClock;
	private boolean busy = false;	// true between edge and outputs value
	private State currentState;
	
	/**
	 * Initialize this element by setting its outputs to 0,
	 * busy to false, and currentState to the initial state.
	 * 
	 * @param sim Unused.
	 */
	public void initSim(Simulator sim) {
		
		// set all outputs to 0
		for (Output output : outputs) {
			output.setValue(new BitSet());
		}
		
		// set latest clock input value to 0
		oldClock = 0;
		
		// find the initial state
		for (State state : states) {
			if (state.isInitial()) {
				currentState = state;
				break;
			}
		}
		
		// send all initial state output values
		currentState.sendOutputs(this,0,sim);
		
		// make not busy
		busy = false;
		
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
			
			// get clock input value
			BitSet cval = getInput("clock").getValue();
			if (cval == null)
				cval = new BitSet();
			int newClock = (int)(BitSetUtils.ToLong(getInput("clock").getValue()));
			
			// if still doing a transition, don't start another
			if (busy) {
				oldClock = newClock;
				return;
			}
			
			// check for proper clock change
			if (trigger == 1) {	// rising edge
				if (!(oldClock == 0 && newClock == 1)) {
					oldClock = newClock;
					return;
				}
			}
			else {	// falling edge
				if (!(oldClock == 1 && newClock == 0)) {
					oldClock = newClock;
					return;
				}
			}
			
			// do a transition, so figure out next state
			State newState = currentState.getNextState();
			
			// if no next state, then stay busy forever
			if (newState == null) {
				busy =  true;
				return;
			}
			
			// save clock value and make state machine busy
			oldClock = newClock;
			busy = true;
			
			// post event
			sim.post(new SimEvent(now+propDelay,this,newState));
		}
		else {
			
			// get the new state
			currentState = (State)todo;
			
			// send values to outputs
			currentState.sendOutputs(this,now,sim);
			
			// no longer busy
			busy = false;
		}
		
	} // end of react method
	
} // end of StateMachine class
