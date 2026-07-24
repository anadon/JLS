package jls.elem;

import java.io.PrintWriter;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;

import jls.core.Geometry;
import jls.Circuit;
import jls.TellUser;
import jls.sim.SimEvent;
import jls.sim.Simulator;

/**
 * Logic specified via a truth table.
 * Simulation and table-model code; the GUI (drawing, print, and the
 * create/edit dialog) lives in the {@code jls.edit} package (issue #77).
 *
 * @author David A. Poplawski
 */
public final class TruthTable extends LogicElement
		implements Timed {

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
	public static final String SIGNALS_CONSTRAINT =
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
	/** The current edit dialog window (parent for error popups), GUI-side;
	 *  null when no dialog is open (e.g. headless model tests). */
	private java.awt.Component edit;
	/** GUI hook: re-lays out and repaints the edit-dialog display after a
	 *  model change. Null when no display exists (headless), so the table
	 *  mutators run identically with or without a dialog. */
	private Runnable displayRefresher;
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
	 * Initialize element.
	 *
	 * @param g The graphics object to use.
	 */
	@Override
	public void init(java.awt.Graphics g) {

		// determine width if needed
		int s = Geometry.SPACING;
		if (g != null) {
			if (width == 0 && height == 0) {
				java.awt.FontMetrics fm = g.getFontMetrics();
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
	public void showInfo(javax.swing.JLabel info) {

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
	 * Get the name of this truth table.
	 * 
	 * @return the name.
	 */
	@Override
	public String getName() {
		
		return name;
	} // end of getName method
	
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
	 * Snapshot the current signals and table before an edit begins, so a
	 * cancelled edit can be rolled back (issue #77: the model half of the
	 * former change() prologue; the GUI-side dialog lives in
	 * {@code jls.edit.TruthTableDialog}).
	 */
	public void beginChange() {

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
	} // end of beginChange method

	/**
	 * Reconcile the element after an accepted edit: mark the circuit
	 * changed, and if the name, inputs or outputs changed, detach from all
	 * wires and re-initialize so the element must be re-placed (issue #77:
	 * the model half of the former change() epilogue).
	 *
	 * @param g The Graphics object to use to determine size.
	 *
	 * @return true if element must be re-placed in the circuit, false if not.
	 */
	public boolean finishChange(java.awt.Graphics g) {

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

	} // end of finishChange method

	/**
	 * Apply an accepted element name from the edit dialog (issue #77: the
	 * model half of the former dialog's validateAndAccept).
	 *
	 * @param tname The new (already validated) element name.
	 */
	public void acceptName(String tname) {

		if (tname.equals(name))
			nameChanged = false;
		else {
			circuit.addName(tname);
			nameChanged = true;
			anyChanges = true;
		}

		name = tname;
	} // end of acceptName method

	/**
	 * Roll the signals and table back to the pre-edit snapshot and record
	 * the cancellation (issue #77: the model half of the former dialog's
	 * cancelDialog).
	 */
	public void restoreFromCancel() {

		// restore info
		inputNames = iNCopy;
		outputNames = oNCopy;
		table = tcopy;

		// tell caller what happened
		cancelled = true;
	} // end of restoreFromCancel method

	/**
	 * Whether the last edit dialog was cancelled.
	 *
	 * @return true if the edit was cancelled.
	 */
	public boolean wasCancelled() {

		return cancelled;
	} // end of wasCancelled method

	/**
	 * Set (or clear) the cancelled flag; the edit dialog clears it before
	 * showing.
	 *
	 * @param cancelled The new cancelled state.
	 */
	public void setCancelled(boolean cancelled) {

		this.cancelled = cancelled;
	} // end of setCancelled method

	/**
	 * Get the input signal names, in column order.
	 *
	 * @return the input signal names.
	 */
	public Vector<String> getInputNames() {

		return inputNames;
	} // end of getInputNames method

	/**
	 * Get the output signal names, in column order.
	 *
	 * @return the output signal names.
	 */
	public Vector<String> getOutputNames() {

		return outputNames;
	} // end of getOutputNames method

	/**
	 * Get the table entries, indexed by row then column.
	 *
	 * @return the value table.
	 */
	public int[][] getTable() {

		return table;
	} // end of getTable method

	/**
	 * Set the GUI dialog window used as the parent for error popups; null
	 * when no dialog is open.
	 *
	 * @param edit The dialog window, or null.
	 */
	public void setEditParent(java.awt.Component edit) {

		this.edit = edit;
	} // end of setEditParent method

	/**
	 * Get the GUI dialog window used as the parent for error popups.
	 *
	 * @return the dialog window, or null.
	 */
	public java.awt.Component getEditParent() {

		return edit;
	} // end of getEditParent method

	/**
	 * Register the GUI hook that re-lays out and repaints the edit-dialog
	 * display after a model change; null (the default) makes
	 * refreshDisplay a no-op for headless use.
	 *
	 * @param displayRefresher The refresh hook, or null.
	 */
	public void setDisplayRefresher(Runnable displayRefresher) {

		this.displayRefresher = displayRefresher;
	} // end of setDisplayRefresher method

	/**
	 * Relayout and repaint the table display, if one exists.
	 * The display panel is created by the edit dialog (or lazily by the
	 * print path); the table-model mutators below run identically with
	 * or without one, which is what lets the headless model tests (issue
	 * #159) exercise them without a dialog.
	 */
	private void refreshDisplay() {

		if (displayRefresher == null)
			return;
		displayRefresher.run();
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
