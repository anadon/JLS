package jls.elem;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.io.PrintWriter;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import jls.Circuit;
import jls.Help;
import jls.JLSInfo;
import jls.TellUser;
import jls.Util;
import jls.util.Placement;
import jls.sim.SimEvent;
import jls.sim.Simulator;

/**
 * Logic specified via a truth table.
 * Editor and simulation code.
 * 
 * @author David A. Poplawski
 */
public final class TruthTable extends LogicElement
		implements Printable, Timed {

	// default values
	/** Default propagation delay (simulation time units). */
	private static final int defaultDelay = 30;
	/** Initial width of the edit dialog, in pixels. */
	private static final int dialogWidth = 300;
	/** Initial height of the edit dialog, in pixels. */
	private static final int dialogHeight = 500;

	// dialog-side constraint (issue #52): a table with no signals cannot
	// compute anything
	/** Error message shown when the table has no input or no output signal. */
	static final String SIGNALS_CONSTRAINT =
			"Must have at least one input signal and one output signal";

	/**
	 * The wording of the table-bounds rule the loader rejects with (issue
	 * #52). The dialog enforces the same rule structurally - it only ever
	 * builds in-range entries - so the string lives here, once.
	 *
	 * @param row The offending row index.
	 * @param col The offending column index.
	 *
	 * @return the constraint message for that entry.
	 *
	 * @jls.testedby jls.elem.DialogValidationTest#truthTableEntryRuleHasOneWording()
	 */
	static String entryConstraint(int row, int col) {

		return "truth table entry (" + row + "," + col
				+ ") is outside the declared table size";
	} // end of entryConstraint method

	// properties
	/** The name of this truth table element. */
	private String name = "";
	/** Propagation delay from an input change to the outputs (simulation time units). */
	private int propDelay = defaultDelay;
	/** Names of the input signals, in column order. */
	private Vector<String>inputNames = new Vector<String>();
	/** Names of the output signals, in column order. */
	private Vector<String>outputNames = new Vector<String>();
	/** Table entries, indexed by row then column (inputs first, then outputs); 0, 1 or 2 (don't care). */
	private int[][] table = new int[0][0];

	// running properties
	/** True if the user cancelled the edit dialog. */
	private boolean cancelled;
	/** True if the edit dialog changed the element's name. */
	private boolean nameChanged;
	/** True if the edit dialog changed the signals or table entries. */
	private boolean anyChanges;
	/** The current edit dialog (parent for error popups). */
	TTEditor edit;
	/** The panel that draws the truth table in the edit dialog and when printing. */
	DisplayBool disp;
	/** Number of rows in the table. */
	private int rows;
	/** Number of columns in the table (inputs plus outputs). */
	private int cols;
	/** Row of the next table entry read from a file. */
	private int irow = 0;	// for reading from a file
	/** Column of the next table entry read from a file. */
	private int icol = 0;
	/** Copy of the input names saved before an edit, restored on cancel. */
	private Vector<String>iNCopy = new Vector<String>();
	/** Copy of the output names saved before an edit, restored on cancel. */
	private Vector<String>oNCopy = new Vector<String>();
	/** Copy of the table entries saved before an edit, restored on cancel. */
	private int[][] tcopy = new int[0][0];

	/**
	 * Create a new truth table element.
	 * 
	 * @param circ The circuit this element is in.
	 */
	public TruthTable(Circuit circ) {

		super(circ);
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
		edit = new TTEditor(this);

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
	 * Initialize element.
	 * 
	 * @param g The graphics object to use.
	 */
	@Override
	public void init(Graphics g) {

		// determine width if needed
		int s = JLSInfo.spacing;
		if (g != null) {
			if (width == 0 && height == 0) {
				FontMetrics fm = g.getFontMetrics();
				String dname = name;
				if (name.isEmpty()) 
					dname = "Logic";
				width = fm.stringWidth(" " + dname + " ");
				for (String input : inputNames) {
					width = Math.max(width,fm.stringWidth(input));
				}
				for (String output : outputNames) {
					width = Math.max(width,fm.stringWidth(output));
				}
				width = (width+s-1)/s*s+s;
			}
		}

		// create new list alternating elements from the two lists
		Set<String>	saveInputs = new HashSet<String>(inputNames);
		Vector<String> pins = new Vector<String>(inputNames.size()+outputNames.size());
		Vector<String> ins = new Vector<String>(inputNames);
		Vector<String> outs = new Vector<String>(outputNames);
		boolean takeFromInput = true;
		while (ins.size()+ outs.size() > 0) {
			if (takeFromInput) {
				takeFromInput = false;
				if (!ins.isEmpty()) {
					String pin = ins.get(0);
					pins.add(pin);
					ins.remove(0);
				}
			}
			else {
				takeFromInput = true;
				if (!outs.isEmpty()) {
					String pin = outs.get(0);
					pins.add(pin);
					outs.remove(0);
				}
			}
		}

		// create input and output signals and determine height
		height = s;
		for (String signal : pins) {
			if (saveInputs.contains(signal)) {
				Input in = new Input(signal,this,0,height,1);
				inputs.add(in);
				height += s;
			}
			else {
				Output out = new Output(signal,this,width,height,1);
				outputs.add(out);
				height += s;
			}
		}
		height += 2*s;

	} // end of init method

	/**
	 * Draw this truth table element.
	 * 
	 * @param g The graphics object to draw with.
	 */
	@Override
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
		g.drawRect(x,y,width,height);
		g.drawLine(x,y+height-2*s,x+width,y+height-2*s);

		// draw name
		String dname = name;
		if (name.isEmpty()) {
			dname = "Logic";
		}
		int w = fm.stringWidth(dname);
		g.drawString(dname,x+(width-w)/2,y+height-s-fontHeight/2+ascent);

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
	 * Save this element in a file.
	 * 
	 * @param output The PrintWriter to write to.
	 */
	@Override
	public void save(PrintWriter output) {

		output.println("ELEMENT TruthTable");
		super.save(output);
		for (String in : inputNames) {
			output.println(" String input \"" + in + "\"");
		}
		for (String out : outputNames) {
			output.println(" String output \"" + out + "\"");
		}
		for (int r=0; r<rows; r+=1) {
			for (int c=0; c<cols; c+=1) {
				output.println(" pair " + r + " " + table[r][c]);
			}
		}
		output.println("END");
	} // end of save method

	// Declarative persistence (#23): one declaration drives save, load
	// dispatch, and copy for this element's simple attributes. The
	// repeated " String input"/" String output" and " pair" lines are
	// list-valued and stay handwritten in save(), setValue and setPair.
	/** Declarations of this element's own saved attributes, in save order. */
	private static final java.util.List<Attribute> OWN_ATTRIBUTES =
			java.util.List.of(
		new Attribute.StringAttribute("name") {
			/** Reads the name field of the given truth table. */
			@Override
			protected String get(Element el) { return ((TruthTable)el).name; }
			/** Sets the name during a load and registers it with the circuit. */
			@Override
			protected void set(Element el, String v) {
				// loading a name registers it with the circuit
				((TruthTable)el).name = v;
				el.getCircuit().addName(v);
			}
			/** Copies the name field without registering it with the circuit. */
			@Override
			public void copy(Element from, Element to) {
				// the handwritten copy assigned the field without
				// registering the name
				((TruthTable)to).name = ((TruthTable)from).name;
			}
		},
		new Attribute.IntAttribute("delay") {
			/** Reads the propagation delay of the given truth table. */
			@Override
			protected int get(Element el) { return ((TruthTable)el).propDelay; }
			/** Sets the propagation delay during a load. */
			@Override
			protected void set(Element el, int v) { ((TruthTable)el).propDelay = v; }
		},
		new Attribute.IntAttribute("rows") {
			/** Reads the row count of the given truth table. */
			@Override
			protected int get(Element el) { return ((TruthTable)el).rows; }
			/** Sets the row count during a load. */
			@Override
			protected void set(Element el, int v) { ((TruthTable)el).rows = v; }
		},
		new Attribute.IntAttribute("cols") {
			/** Reads the column count of the given truth table. */
			@Override
			protected int get(Element el) { return ((TruthTable)el).cols; }
			/** Sets the column count during a load and allocates the table. */
			@Override
			protected void set(Element el, int v) {
				// setting cols allocates the table (rows is loaded and
				// copied first, in save order)
				TruthTable tt = (TruthTable)el;
				tt.cols = v;
				tt.table = new int[tt.rows][tt.cols];
			}
		}
	);

	/** Base attributes followed by this element's own, in save order. */
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
	 * Set a String instance variable value (during a load).
	 *
	 * @param name The instance variable name.
	 * @param value The instance variable value.
	 */
	@Override
	public void setValue(String name, String value) {

		if (name.equals("input")) {
			inputNames.add(value);
		}
		else if (name.equals("output")) {
			outputNames.add(value);
		}
		else {
			super.setValue(name,value);
		}
	} // end of setValue method

	/**
	 * Set a pair of int instance variable values (during a load).
	 * 
	 * @param v1 The first value.
	 * @param v2 The second value.
	 */
	@Override
	public void setPair(int v1, int v2) {

		if (v1 != irow) {
			irow = v1;
			icol = 0;
		}
		// a malformed file used to die on the raw array access with a
		// message-free AIOOBE; reject with the real constraint (issue #52)
		if (v1 < 0 || v1 >= table.length
				|| icol >= table[v1].length) {
			throw new IllegalArgumentException(entryConstraint(v1, icol));
		}
		table[v1][icol] = v2;
		icol += 1;
	} // end of setPair method

	/**
	 * Copy this element.
	 */
	@Override
	public Element copy() {

		// create new element; the attribute registry copies name, delay,
		// rows and cols (allocating the copy's table)
		TruthTable it = new TruthTable(circuit);
		super.copy(it);

		// copy input and output names
		it.inputNames = new Vector<String>(inputNames);
		it.outputNames = new Vector<String>(outputNames);

		// copy table contents
		for (int r=0; r<rows; r+=1) {
			for (int c=0; c<cols; c+=1) {
				it.table[r][c] = table[r][c];
			}
		}

		// copy inputs and outputs
		for (Input input : inputs) {
			it.inputs.add(input.copy(it));
		}
		for (Output output : outputs) {
			it.outputs.add(output.copy(it));
		}
		return it;
	} // end of copy method

	/**
	 * Display info about this element.
	 * 
	 * @param info The JLabel to display with.
	 */
	@Override
	public void showInfo(JLabel info) {

		info.setText("circuit determined by truth table");

	} // end of showInfo method

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
	 * Print the truth table.
	 */
	@Override
	public int print(Graphics g, PageFormat format, int pagenum) {

		// use better graphics
		Graphics2D gg = (Graphics2D)g;

		// the display panel is otherwise created only when the edit
		// dialog opens, so printing a freshly loaded circuit crashed
		// here with an NPE (found by PrintPathSmokeTest, issue #91)
		if (disp == null) {
			disp = new DisplayBool(this);
		}

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
		disp.doLayout(inputNames,outputNames,table,gg);
		Dimension bounds = disp.getPreferredSize();

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

		// print
		disp.print(gg);

		return Printable.PAGE_EXISTS;
	} // end of print method

	/**
	 * Get the name of this truth table.
	 * 
	 * @return the name.
	 */
	@Override
	public String getName() {
		
		return name;
	} // end of getName method
	
	/**
	 * Get the display.
	 * 
	 * @return the display object.
	 */
	public DisplayBool getDisplay() {

		return disp;
	} // end of getDisplay method

	/**
	 * Truth tables can be modified.
	 * 
	 * @return true.
	 */ 
	@Override
	public boolean canChange() {

		return true;
	} // end of canChange method
	
	/**
	 * Truth tables cannot be copied.
	 * 
	 * @return false.
	 */
	public boolean canCopy() {
		
		return false;
	} // end of canCopy method

	/**
	 * Show edit dialog.
	 * When done, make sure inputs and outputs are still the same, and that the name
	 * is still the same.
	 * If not, detatch existing element from all wires and go into "moving" editor
	 * state.
	 * 
	 * @param g The Graphics object to use to determine size.
	 * @param editWindow The editor window this element is in.
	 * @param x The current x-coordinate of the mouse.
	 * @param y The current y-coordinate of the mouse.
	 * 
	 * @return true if element must be re-placed in the circuit, false if not.
	 */
	@Override
	public boolean change(Graphics g, JPanel editWindow, int x, int y) {

		// save current truth table info
		iNCopy = new Vector<String>(inputNames);
		oNCopy = new Vector<String>(outputNames);
		int rows = table.length;
		int cols = table[0].length;
		tcopy = new int[rows][cols];
		for (int r=0; r<rows; r+=1) {
			for (int c=0; c<cols; c+=1) {
				tcopy[r][c] = table[r][c];
			}
		}
		anyChanges = false;

		// display dialog
		new TTEditor(this);

		// quit if cancelled
		if (cancelled) {
			return false;
		}

		// mark circuit changed if there were any changes in truth table
		if (anyChanges) {
			circuit.markChanged();
		}

		// if name has changed, detach
		if (nameChanged || anyChanges) {
			detach();
			width = 0;
			height = 0;
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
		newNames.addAll(inputNames);

		// if not the same, detach
		if (!oldNames.equals(newNames)) {
			detach();
			width = 0;
			height = 0;
			init(g);
			return true;
		}

		// make a set of all old output names
		oldNames.clear();
		for (Output output : outputs) {
			oldNames.add(output.getName());
		}

		// make a set of all new input names
		newNames.clear();
		newNames.addAll(outputNames);

		// if not the same, detach
		if (!oldNames.equals(newNames)) {
			detach();
			width = 0;
			height = 0;
			init(g);
			return true;
		}

		return false;

	} // end of change method

	/**
	 * Dialog to create/edit a truth table.
	 */
	@SuppressWarnings("serial")
	private class TTEditor extends ElementDialog implements ActionListener {

		// properties
		/** Text field for typing the name of a new input signal. */
		private JTextField inputField = new JTextField(10);
		/** Text field for typing the name of a new output signal. */
		private JTextField outputField = new JTextField(10);
		/** Text field for editing the element's name. */
		private JTextField nameField = new JTextField(10);

		/**
		 * Initialize and show dialog.
		 *
		 * @param ttelem The truth table element being created or edited.
		 */
		public TTEditor(TruthTable ttelem) {

			// set up window title
			super("Edit Truth Table",null);

			// set up display
			disp = new DisplayBool(ttelem);

			// set not cancelled
			cancelled = false;

			// set up window
			Container window = getContentPane();
			window.setLayout(new BorderLayout());

			// add components
			JScrollPane pane = new JScrollPane(disp);
			window.add(pane,BorderLayout.CENTER);
			JPanel other = new JPanel(new BorderLayout());
			JPanel info = new JPanel(new BorderLayout());
			JPanel labels = new JPanel(new GridLayout(3,1));
			labels.add(new JLabel("new input: ",SwingConstants.RIGHT));
			labels.add(new JLabel("new output: ",SwingConstants.RIGHT));
			labels.add(new JLabel("name: ",SwingConstants.RIGHT));
			info.add(labels,BorderLayout.WEST);
			JPanel inputs = new JPanel(new GridLayout(3,1));
			inputs.add(inputField);
			inputs.add(outputField);
			inputs.add(nameField);
			nameField.setText(name);
			info.add(inputs,BorderLayout.CENTER);
			other.add(info,BorderLayout.NORTH);
			other.add(getErrorLabel(),BorderLayout.CENTER);
			JPanel okCancel = new JPanel(new GridLayout(1,3));
			okCancel.add(ok);
			okCancel.add(cancel);
			JButton help = new JButton("Help");
			Help.enableHelpOnButton(help, "truth");
			okCancel.add(help);
			other.add(okCancel,BorderLayout.SOUTH);
			window.add(other,BorderLayout.SOUTH);

			// add listeners (OK, Cancel, Escape and the close box are
			// wired by the shared dialog base, issue #26)
			inputField.addActionListener(this);
			outputField.addActionListener(this);
			confirmOnEnter(nameField);
			installDialogBehavior();

			// lay out the table once the window exists
			addWindowListener (
					new WindowAdapter() {
						/** Lays out the table display once the dialog window is open. */
						@Override
						public void windowOpened(WindowEvent event) {
							disp.doLayout(inputNames,outputNames,table,null);
							disp.repaint();
						}
					}
			);

			// finish up: place relative to the owner window (#104)
			setSize(dialogWidth,dialogHeight);
			setLocationRelativeTo(getOwner());
			setVisible(true);
		} // end of constructor

		/**
		 * Listen for the new-signal field events.
		 *
		 * @param event The event object.
		 */
		@Override
		public void actionPerformed(ActionEvent event) {

			if (event.getSource() == inputField) {
				addInput(inputField.getText().trim());
				inputField.setText("");
			}
			else if (event.getSource() == outputField) {
				addOutput(outputField.getText().trim());
				outputField.setText("");
			}
		} // end of actionPerformed method

		/**
		 * Check the form against the truth table constraints (issue #52).
		 */
		@Override
		protected java.util.List<Violation> validateInputs() {

			java.util.List<Violation> violations =
					new java.util.ArrayList<Violation>();
			String tname = nameField.getText().trim();
			if (tname.isEmpty() || !Util.isValidName(tname)) {
				violations.add(new Violation("Missing or invalid element name",
						nameField));
			}
			else if (!tname.equals(name) && circuit.hasName(tname)) {
				violations.add(new Violation("Duplicate element name",
						nameField));
			}
			if (inputNames.size() == 0 || outputNames.size() == 0) {
				violations.add(new Violation(SIGNALS_CONSTRAINT, inputField));
			}
			return violations;
		} // end of validateInputs method

		/**
		 * Apply the validated form to the truth table.
		 */
		@Override
		protected void validateAndAccept() {

			String tname = nameField.getText().trim();
			if (tname.equals(name))
				nameChanged = false;
			else {
				circuit.addName(tname);
				nameChanged = true;
				anyChanges = true;
			}

			name = tname;
			dispose();
		} // end of validateAndAccept method

		/**
		 * Cancel the edit.
		 */
		@Override
		protected void cancelDialog() {

			// restore info
			inputNames = iNCopy;
			outputNames = oNCopy;
			table = tcopy;

			// tell caller what happened
			cancelled = true;
			dispose();
		} // end of cancelDialog method

	} // end of TTEditor class

	/**
	 * Relayout and repaint the table display, if one exists.
	 * The display panel is created by the edit dialog (or lazily by the
	 * print path); the table-model mutators below run identically with
	 * or without one, which is what lets the headless model tests (issue
	 * #159) exercise them without a dialog.
	 */
	private void refreshDisplay() {

		if (disp == null)
			return;
		disp.doLayout(inputNames,outputNames,table,null);
		disp.repaint();
	} // end of refreshDisplay method

	/**
	 * Add a new input signal to the truth table.
	 *
	 * @param signal The new input signal name.
	 */
	public void addInput(String signal) {

		// ignore empty input
		if (signal.isEmpty()) 
			return;

		// don't allow duplicate names
		for (String name : inputNames) {
			if (signal.equals(name)) {
				TellUser.error(edit,"duplicate signal name", "Error");
				return;
			}
		}
		for (String name : outputNames) {
			if (signal.equals(name)) {
				TellUser.error(edit,"duplicate signal name", "Error");
				return;
			}
		}

		// take care of initial case of an empty truth table
		if (inputNames.size() == 0) {

			// add to input name list
			inputNames.add(signal);

			// create new table
			table = new int[2][1];

			// add new input signal column
			table[0][0] = 0;
			table[1][0] = 1;

			// finish up
			rows = 2;
			cols = 1;
			refreshDisplay();
			anyChanges = true;
			return;
		}

		// add to input name list
		int ins = inputNames.size();
		inputNames.add(signal);

		// create larger array
		int newRows = rows*2;
		int newCols = cols+1;
		int [][] newTable = new int[newRows][newCols];

		// copy inputs
		for (int c=0; c<ins; c+=1) {
			int nr = 0;
			for (int r=0; r<rows; r+=1) {
				newTable[nr][c] = table[r][c];
				newTable[nr+1][c] = table[r][c];
				nr += 2;
			}
		}

		// put in new signal column
		int nr = 0;
		for (int r=0; r<rows; r+=1) {
			newTable[nr][ins] = 0;
			newTable[nr+1][ins] = 1;
			nr += 2;
		}

		// copy output columns
		for (int c=ins; c<cols; c+=1) {
			nr = 0;
			for (int r=0; r<rows; r+=1) {
				newTable[nr][c+1] = table[r][c];
				newTable[nr+1][c+1] = table[r][c];
				nr += 2;
			}
		}

		// finish up
		table = newTable;
		cols = newCols;
		rows = newRows;
		refreshDisplay();
		anyChanges = true;
	} // end of addInput method

	/**
	 * Add a new output signal to the truth table.
	 * 
	 * @param signal The new output signal name.
	 */
	public void addOutput(String signal) {

		// ignore empty input
		if (signal.isEmpty()) 
			return;

		// don't allow duplicate names
		for (String name : inputNames) {
			if (signal.equals(name)) {
				TellUser.error(edit,"duplicate signal name", "Error");
				return;
			}
		}
		for (String name : outputNames) {
			if (signal.equals(name)) {
				TellUser.error(edit,"duplicate signal name", "Error");
				return;
			}
		}

		// can't add an output until there is at least one input
		if (inputNames.size() == 0) {
			TellUser.error(edit,"add at least one input first", "Error");
			return;
		}

		// create larger array
		int [][] newTable = new int[rows][cols+1];

		// copy everything in the table so far
		for (int r=0; r<rows; r+=1) {
			for (int c=0; c<cols; c+=1) {
				newTable[r][c] = table[r][c];
			}
		}

		// put in new signal column
		outputNames.add(signal);
		for (int r=0; r<rows; r+=1) {
			newTable[r][cols] = 2;
		}

		// finish up
		table = newTable;
		cols += 1;
		refreshDisplay();
		anyChanges = true;
	} // end of addOutput method

	/**
	 * Remove the given input from the truth table.
	 * Can only be done if all outputs match.
	 * For example, 
	 *   a b | f
	 *   0 0 | 0
	 *   0 1 | 1
	 *   1 0 | 0
	 *   1 1 | 1
	 * b cannot be removed because for a=0, f=0 when b=0, f=1 when b=0, so
	 * what should f be when b is removed?  On the other hand, a can be removed.
	 * 
	 * @param signal The name of the input signal to remove.
	 */
	public void removeInput(String signal) {

		// find column
		int col = inputNames.indexOf(signal);

		// see if everything in this column could be a don't care
		SortedSet<Integer> dups = new TreeSet<Integer>();
		for (int r=0; r<rows; r+=1) {
			if (dups.contains(r))
				continue;
			if (table[r][col] == 2) {
				continue;
			}
			int matchingRow = findMatchingRow(r,col);
			if (matchingRow == -1) {
				TellUser.error(edit,"cannot remove: output conflict",
						"Error");
				return;
			}
			dups.add(matchingRow);
		}
		int newRows = rows - dups.size();

		// delete the column and duplicate rows
		inputNames.remove(col);
		int[][] newTable = new int[newRows][cols-1];
		int nr = 0;
		for (int r=0; r<rows; r+=1) {
			int nc = 0;
			if (dups.contains(r))
				continue;
			for (int c=0; c<cols; c+=1) {
				if (c == col)
					continue;
				newTable[nr][nc] = table[r][c];
				nc += 1;
			}
			nr += 1;
		}

		// finish up
		table = newTable;
		rows = newRows;
		cols -= 1;
		refreshDisplay();
		anyChanges = true;
	} // end of removeInput method

	/**
	 * Remove the given output from the truth table.
	 * 
	 * @param which The position of the name to remove.
	 */
	public void removeOutput(String which) {

		// remove from output names
		int pos = outputNames.indexOf(which);
		int col = inputNames.size()+pos;
		outputNames.remove(which);

		// make a new table
		int [][] newTable = new int[rows][cols-1];

		// copy everything before the given column
		for (int r=0; r<rows; r+=1) {
			for (int c=0; c<col; c+=1) {
				newTable[r][c] = table[r][c];
			}
		}

		// move everything after the given column to the left
		for (int r=0; r<rows; r+=1) {
			for (int c=col; c<cols-1; c+=1) {
				newTable[r][c] = table[r][c+1];
			}
		}

		// finish up
		table = newTable;
		cols -= 1;
		refreshDisplay();
		anyChanges = true;
	} // end of removeOutput method

	/**
	 * Change the output value in a given place from 0->1, 1->x, x->0.
	 * 
	 * @param row The display row.
	 * @param col The display column.
	 */
	public void toggleOutput(int row, int col) {

		table[row][col] = (table[row][col] + 1) % 3;
		refreshDisplay();
		anyChanges = true;
	} // end of toggleOutput method

	/**
	 * Make a don't care at a given position, if possible.
	 * Two rows are collapsed into one, with the lowest index row remaining
	 * and the other one removed.
	 * 
	 * @param row The row getting the don't care.
	 * @param col The column getting the don't care.
	 */
	public void makeDontCare(int row, int col) {

		// find matching row, if there is one
		int matchingRow = findMatchingRow(row,col);
		if (matchingRow == -1) {
			TellUser.error(edit,"not possible", "Error");
			return;
		}

		// find the lowest and highest numbered rows
		int minRow = Math.min(row,matchingRow);
		int maxRow = Math.max(row,matchingRow);

		// put a don't care in the lowest numbered row
		table[minRow][col] = 2;

		// if don't cares in minimum row output columns,
		// make them be whatever the maximum row output columns are
		for (int c=inputNames.size(); c<cols; c+=1) {
			if (table[minRow][c] == 2)
				table[minRow][c] = table[maxRow][c];
		}

		// remove the redundant row
		removeRow(maxRow);

		// finish up
		refreshDisplay();
		anyChanges = true;
	} // end of makeDontCare method

	/**
	 * Find row that matches a given row.
	 * 
	 * @param row The row number to try to match.
	 * @param ignore A column to ignore when looking for a match, or -1
	 *        if no column should be ignored.
	 * 
	 * @return the matching row number, if one.
	 *         Otherwise return -1.
	 */
	public int findMatchingRow(int row, int ignore) {

		for (int r=0; r<rows; r+=1) {

			// don't try to match a row with itself
			if (r == row)
				continue;

			// check all columns
			boolean match = true;
			for (int c=0; c<cols; c+=1) {

				// ignore the specified column
				if (c == ignore)
					continue;


				if (c < inputNames.size()) {

					// input columns must match exactly
					if (table[r][c] != table[row][c]) {
						match = false;
						break;
					}
				}
				else {

					// output rows can match with don't cares
					if (table[r][c] != 2 && table[row][c] != 2 &&
							table[r][c] != table[row][c]) {
						match = false;
						break;
					}
				}
			}
			if (match)
				return r;
		}
		return -1;
	} // end of findMatchingRow method

	/**
	 * Remove a row of the table.
	 * 
	 * @param row The row to remove.
	 */
	public void removeRow(int row) {

		// make new table
		int[][] newTable = new int[rows-1][cols];

		// copy rows above the removed one
		for (int r=0; r<row; r+=1) {
			newTable[r] = table[r];
		}

		// copy rows below the removed one
		for (int r=row; r<rows-1; r+=1) {
			newTable[r] = table[r+1];
		}

		// finish up
		table = newTable;
		rows -= 1;
		anyChanges = true;
	} // end of removeRow method

	/**
	 * Remove a don't care by changing the x in this row to a 0 and
	 * adding (in the correct place) a new row that has a 1 where the x is.
	 * 
	 * @param row The row with the don't care being undone.
	 * @param col The column with the don't care being undone.
	 */

	public void undoDontCare(int row, int col) {

		// change guaranteed
		anyChanges = true;

		// temporarily set don't care value to 1
		table[row][col] = 1;

		// now see where this row belongs in the table
		int newCode = makeRowCode(row);
		for (int ir=row+1; ir<rows; ir+=1) {
			int thisCode = makeRowCode(ir);
			if (newCode < thisCode) {

				// it belongs before row ir, so...

				// copy the table up to the insertion row
				int [][] newTable = new int[rows+1][cols];
				for (int r=0; r<ir; r+=1) {
					for (int c=0; c<cols; c+=1) {
						newTable[r][c] = table[r][c];
					}
				}

				// make copy of original row at row ir
				for (int c=0; c<cols; c+=1) {
					newTable[ir][c] = table[row][c];
				}

				// copy the rest of the table
				for (int r=rows-1; r>=ir; r-=1) {
					for (int c=0; c<cols; c+=1) {
						newTable[r+1][c] = table[r][c];
					}
				}

				// put 0 into the original don't care place
				newTable[row][col] = 0;

				// finish up
				table = newTable;
				rows += 1;
				refreshDisplay();
				return;
			}
		}

		// it belongs at the end, so...

		// copy entire table
		int [][] newTable = new int[rows+1][cols];
		for (int r=0; r<rows; r+=1) {
			for (int c=0; c<cols; c+=1) {
				newTable[r][c] = table[r][c];
			}
		}

		// make copy of original row at the end
		for (int c=0; c<cols; c+=1) {
			newTable[rows][c] = table[row][c];
		}

		// put 0 into the original don't care place
		newTable[row][col] = 0;

		// finish up
		table = newTable;
		rows += 1;
		refreshDisplay();
	} // end of undoDontCare method

	/**
	 * Create an integer that is equal to the binary value in a given row.
	 * Don't care's are assumed to be 0.
	 * 
	 * @param row The row.
	 * 
	 * @return the corresponding integer.
	 */
	public int makeRowCode(int row) {

		int val = 0;
		int pos = 0;
		for (int c=inputNames.size()-1; c>=0; c-=1) {
			if (table[row][c] == 1)
				val += 1 << pos;
			pos += 1;
		}
		return val;
	} // end of makeRowCode method

	/**
	 * Rename input signal.
	 * 
	 * @param signal Current input signal name.
	 */
	public void renameInput(String signal) {

		String newSignal = getNewName();
		if (newSignal == null)
			return;

		int pos = inputNames.indexOf(signal);
		inputNames.set(pos,newSignal);
		refreshDisplay();
		anyChanges = true;
	} // end of renameInput method

	/**
	 * Rename output signal.
	 * 
	 * @param signal Current output signal name.
	 */
	public void renameOutput(String signal) {

		String newSignal = getNewName();
		if (newSignal == null)
			return;

		int pos = outputNames.indexOf(signal);
		outputNames.set(pos,newSignal);
		refreshDisplay();
		anyChanges = true;
	} // end of renameOutput method

	/**
	 * Get new signal name.
	 * Check for invalid name and duplicate names.
	 *
	 * @return new signal name, or null if invalid or duplicate or canceled.
	 */
	private String getNewName() {

		// get name
		String newSignal =
			TellUser.prompt(edit,"Enter new output signal name");

		if (newSignal == null)
			return null;

		// trim off junk
		newSignal = newSignal.trim();

		// don't allow null
		if (newSignal.isEmpty()) {
			TellUser.error(edit,"invalid name", "Error");
			return null;
		}

		// don't allow duplicate names
		for (String name : inputNames) {
			if (newSignal.equals(name)) {
				TellUser.error(edit,"duplicate signal name", "Error");
				return null;
			}
		}
		for (String name : outputNames) {
			if (newSignal.equals(name)) {
				TellUser.error(edit,"duplicate signal name", "Error");
				return null;
			}
		}

		// otherwise ok
		return newSignal;
	} // end of getNewName method

	/**
	 * Move output signal column left one position.
	 * If already the farthest left, do nothing.
	 * 
	 * @param signal The signal name of the column to move.
	 */
	public void moveOutputLeft(String signal) {

		// get position, do nothing if already farthest left
		int pos = outputNames.indexOf(signal);
		if (pos == 0)
			return;

		// move signal name
		outputNames.remove(pos);
		outputNames.add(pos-1,signal);

		// swap column data
		int col = inputNames.size()+pos;
		for (int r=0; r<rows; r+=1) {
			int temp = table[r][col];
			table[r][col] = table[r][col-1];
			table[r][col-1] = temp;
		}

		// finish up
		refreshDisplay();
		anyChanges = true;
	} // end of moveOutputLeft method

	/**
	 * Move output signal column right one position.
	 * If already the farthest left, do nothing.
	 * 
	 * @param signal The signal name of the column to move.
	 */
	public void moveOutputRight(String signal) {

		// get position, do nothing if already farthest left
		int pos = outputNames.indexOf(signal);
		if (pos == outputNames.size()-1)
			return;

		// move signal name
		outputNames.remove(pos);
		outputNames.add(pos+1,signal);

		// swap column data
		int col = inputNames.size()+pos;
		for (int r=0; r<rows; r+=1) {
			int temp = table[r][col];
			table[r][col] = table[r][col+1];
			table[r][col+1] = temp;
		}

		// finish up
		refreshDisplay();
		anyChanges = true;
	} // end of moveOutputRight method

	/**
	 * Move input signal column left one position, if not already farthest left.
	 * Reorder bit assignments accordingly.
	 * 
	 * @param signal The signal name of the column to move.
	 */
	public void moveInputLeft(String signal) {

		// get position, do nothing if farthest left
		int pos = inputNames.indexOf(signal);
		if (pos == 0)
			return;

		// move signal name
		inputNames.remove(pos);
		inputNames.add(pos-1,signal);

		// swap column data
		for (int r=0; r<rows; r+=1) {
			int temp = table[r][pos];
			table[r][pos] = table[r][pos-1];
			table[r][pos-1] = temp;
		}

		// reorder rows
		reorderRows();

		// finish up
		refreshDisplay();
		anyChanges = true;
	} // end of moveInputLeft method

	/**
	 * Move input signal column right one position, if not already farthest right.
	 * Reorder bit assignments accordingly.
	 * 
	 * @param signal The signal name of the column to move.
	 */
	public void moveInputRight(String signal) {

		// get position, do nothing if farthest right
		int pos = inputNames.indexOf(signal);
		if (pos == inputNames.size()-1)
			return;

		// move signal name
		inputNames.remove(pos);
		inputNames.add(pos+1,signal);

		// swap column data
		for (int r=0; r<rows; r+=1) {
			int temp = table[r][pos];
			table[r][pos] = table[r][pos+1];
			table[r][pos+1] = temp;
		}

		// reorder rows
		reorderRows();

		// finish up
		refreshDisplay();
		anyChanges = true;
	} // end of moveInputRight method

	/**
	 * Reorder rows based on bit assignments.
	 * Don't cares are treated like 0's.
	 */
	private void reorderRows() {

		// create map between old and new rows
		SortedMap<Integer,Integer>map = new TreeMap<Integer,Integer>();
		for (int r=0; r<rows; r+=1) {
			int newRow = this.makeRowCode(r);
			map.put(newRow,r);
		}

		// put rows into new table in proper order
		int[][] newTable = new int[rows][cols];
		int row = 0;
		for (int i : map.keySet()) {
			int oldRow = map.get(i);
			for (int c=0; c<cols; c+=1) {
				newTable[row][c] = table[oldRow][c];
			}
			row += 1;
		}

		// finish up
		table = newTable;
		anyChanges = true;
	} // end of reorderRows method

	/**
	 * Get default propagation delay.
	 *
	 * @return the default propagation delay.
	 */
	public int getDefaultDelay() {

		return defaultDelay;
	} // end of getDefaultDelay method

	/**
	 * Reset propagation delay to default value.
	 */
	@Override
	public void resetPropDelay() {

		propDelay = getDefaultDelay();
	} // end of resetPropDelay method

	/**
	 * Combinational logic has timing info (propagation delay).
	 * 
	 * @return true.
	 */
	@Override
	public boolean hasTiming() {

		return true;
	} // end of hasTiming method

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

	//-------------------------------------------------------------------------------
	// Simulation
	//-------------------------------------------------------------------------------

	/** The value (0 or 1) each output will have once its pending event fires, indexed by output position. */
	private int[] toBeValue;
	/**
	 * A pending output change carried through the simulator: an output pin's
	 * position (index into the outputs list) paired with the BitSet value it
	 * should take on when the scheduled event fires.
	 */
	static class Out {
		/** Index of the output pin in the outputs list. */
		int position;
		/** The value the output pin should take on. */
		BitSet value;

		/**
		 * Create a pending output change.
		 */
		Out() {
		}
	}

	/**
	 * Initialize this element by setting its output pins and to-be values to 0.
	 * 
	 * @param sim Unused.
	 */
	@Override
	public void initSim(Simulator sim) {

		// create toBeValue array
		toBeValue = new int[outputNames.size()];

		// set output pins and to be values
		int pos = 0;
		int offset = inputNames.size();
		for (Output output : outputs) {

			// set output to 0
			output.setValue(new BitSet());

			// if it should become nonzero then post an event
			int outValue = table[0][pos+offset];
			if (outValue == 1) {
				toBeValue[pos] = 1;
				BitSet val = new BitSet(1);
				val.set(0);
				Out out = new Out();
				out.position = pos;
				out.value = val;
				sim.post(new SimEvent(propDelay,this,out));
			}
			pos += 1;
		}

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

			// find a matching row of the truth table
			int matchingRow = -1;
			int cols = inputNames.size();
			for (int row=0; row<rows; row+=1) {
				boolean match = true;
				for (int col=0; col<cols; col+=1) {
					if (table[row][col] == 2)
						continue;
					BitSet inb = inputs.get(col).getValue();
					if (inb == null)
						inb = new BitSet();
					int inputValue = inb.get(0) ? 1 : 0;
					if (inputValue != table[row][col]) {
						match = false;
						break;
					}
				}
				if (match) {
					matchingRow = row;
					break;
				}
			}

			// no matching row: leave the outputs unchanged instead of
			// killing the simulation thread with table[-1] (issue #52)
			if (matchingRow < 0) {
				return;
			}

			// for each output value...
			int offset = inputNames.size();
			int pos = 0;
			for (int i = 0; i < outputs.size(); i++) {
				// if it is different than the value propagating through
				// this circuit, then post an event
				int outValue = table[matchingRow][pos+offset];

				// don't care becomes false
				if (outValue == 2)
					outValue = 0;

				if (outValue != toBeValue[pos]) {
					toBeValue[pos] = outValue;
					BitSet val = new BitSet(1);
					if (outValue == 1)
						val.set(0);
					Out out = new Out();
					out.position = pos;
					out.value = val;
					sim.post(new SimEvent(now+propDelay,this,out));
				}
				pos += 1;
			}
		}
		else {

			// get the new output
			Out newOut = (Out)todo;

			// send to output
			Output out = outputs.get(newOut.position);
			BitSet val = newOut.value;
			BitSet newVal = (BitSet)val.clone();
			out.propagate(newVal,now,sim);
		}

	} // end of react method

	/**
	 * Print table (for debugging)
	 */
	public void printTable() {

		for (int i=0; i<table.length; i+=1) {
			for (int j=0; j<table[0].length; j+=1) {
				System.out.print(table[i][j] + " ");
			}
			System.out.println();
		}
	} // end of printTable method

} // end of TruthTable class
