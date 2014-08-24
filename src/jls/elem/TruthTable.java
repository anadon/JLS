package jls.elem;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.MouseInfo;
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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import jls.Circuit;
import jls.JLSInfo;
import jls.Util;
import jls.sim.SimEvent;
import jls.sim.Simulator;

/**
 * Logic specified via a truth table.
 * Editor and simulation code.
 * 
 * @author David A. Poplawski
 */
public final class TruthTable extends LogicElement implements Printable {

	// default values
	private static final int defaultDelay = 30; 
	private static final int dialogWidth = 300;
	private static final int dialogHeight = 500;

	// properties
	private String name = "";
	private int propDelay = defaultDelay;
	private Vector<String>inputNames = new Vector<String>();
	private Vector<String>outputNames = new Vector<String>();
	private int[][] table = new int[0][0];

	// running properties
	private boolean cancelled;
	private boolean nameChanged;
	private boolean anyChanges;
	TTEditor edit;
	DisplayBool disp;
	private int rows;
	private int cols;
	private int irow = 0;	// for reading from a file
	private int icol = 0;
	private Vector<String>iNCopy = new Vector<String>();
	private Vector<String>oNCopy = new Vector<String>();
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
	public boolean setup(Graphics g, JPanel editWindow, int x, int y) {

		// show creation dialog
		edit = new TTEditor(this);

		// don't do anything if user cancelled gate
		if (cancelled)
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
	 * Initialize element.
	 * 
	 * @param g The graphics object to use.
	 */
	public void init(Graphics g) {

		// determine width if needed
		int s = JLSInfo.spacing;
		if (g != null) {
			if (width == 0 && height == 0) {
				FontMetrics fm = g.getFontMetrics();
				String dname = name;
				if (name.equals("")) 
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
		if (name.equals("")) {
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
	public void save(PrintWriter output) {

		output.println("ELEMENT TruthTable");
		super.save(output);
		output.println(" String name \"" + name + "\"");
		output.println(" int delay " + propDelay);
		output.println(" int rows " + rows);
		output.println(" int cols " + cols);
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
		}
		else if (name.equals("input")) {
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
	 * Set an int instance variable value (during a load).
	 * 
	 * @param name The name of the variable.
	 * @param value The value of the variable.
	 */
	public void setValue(String name, int value) {

		if (name.equals("rows")) {
			rows = value;
		} else if (name.equals("cols")) {
			cols = value;
			table = new int[rows][cols];
		} else {
			super.setValue(name,value);
		}
	} // end of setValue method

	/**
	 * Set a pair of int instance variable values (during a load).
	 * 
	 * @param v1 The first value.
	 * @param v1 The second value.
	 */
	public void setPair(int v1, int v2) {

		if (v1 != irow) {
			irow = v1;
			icol = 0;
		}
		table[v1][icol] = v2;
		icol += 1;
	} // end of setPair method

	/**
	 * Copy this element.
	 */
	public Element copy() {

		// create new element
		TruthTable it = new TruthTable(circuit);

		// set basic info
		it.name = new String(name);

		// copy input and output names
		it.inputNames = new Vector<String>(inputNames);
		it.outputNames = new Vector<String>(outputNames);

		// copy table
		it.rows = table.length;
		it.cols = table[0].length;
		it.table = new int[rows][cols];
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

		info.setText("circuit determined by truth table");

	} // end of showInfo method

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
	 * Print the truth table.
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
	private class TTEditor extends JDialog implements ActionListener {

		// properties
		private JTextField inputField = new JTextField(10);
		private JTextField outputField = new JTextField(10);
		private JTextField nameField = new JTextField(10);
		private JButton ok = new JButton("ok");
		private JButton cancel = new JButton("cancel");

		/**
		 * Initialize and show dialog.
		 */
		public TTEditor(TruthTable ttelem) {

			// set up window title
			super(JLSInfo.frame,"Edit Truth Table",true);

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
			JPanel okCancel = new JPanel(new GridLayout(1,3));
			ok.setBackground(Color.green);
			okCancel.add(ok);
			cancel.setBackground(Color.pink);
			okCancel.add(cancel);
			JButton help = new JButton("Help");
			if (JLSInfo.hb == null)
				Util.noHelp(help);
			else
				JLSInfo.hb.enableHelpOnButton(help,"truth",null);
			okCancel.add(help);
			other.add(okCancel,BorderLayout.SOUTH);
			window.add(other,BorderLayout.SOUTH);
			getRootPane().setDefaultButton(ok);

			// add listeners
			ok.addActionListener(this);
			cancel.addActionListener(this);
			inputField.addActionListener(this);
			outputField.addActionListener(this);
			nameField.addActionListener(this);

			// set up window close listener to cancel element
			addWindowListener (
					new WindowAdapter() {
						public void windowClosing(WindowEvent event) {
							cancel();
						}
						public void windowOpened(WindowEvent event) {
							disp.doLayout(inputNames,outputNames,table,null);
							disp.repaint();
						}
					}
			);

			// finish up
			setSize(dialogWidth,dialogHeight);
			setLocation(100,100);
			setVisible(true);
		} // end of constructor

		/**
		 * Listen for button events.
		 * 
		 * @param event The event object.
		 */
		public void actionPerformed(ActionEvent event) {

			if (event.getSource() == ok || event.getSource() == nameField) {
				String tname = nameField.getText().trim();
				if (tname.equals("") || !Util.isValidName(tname)) {
					JOptionPane.showMessageDialog(this,
							"Missing or invalid element name", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (inputNames.size() == 0 || outputNames.size() == 0) {
					JOptionPane.showMessageDialog(this,
							"Must have at least one input signal and one output signal",
							"Error", JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (tname.equals(name))
					nameChanged = false;
				else {
					if (!circuit.addName(tname)) {
						JOptionPane.showMessageDialog(this,
								"Duplicate element name", "Error",
								JOptionPane.ERROR_MESSAGE);
						return;
					}
					nameChanged = true;
					anyChanges = true;
				}

				name = tname;
				dispose();
			}
			else if (event.getSource() == cancel) {
				cancel();
			}
			else if (event.getSource() == inputField) {
				addInput(inputField.getText().trim());
				inputField.setText("");
			}
			else if (event.getSource() == outputField) {
				addOutput(outputField.getText().trim());
				outputField.setText("");
			}
		} // end of actionPerformed method

		/**
		 * Cancel the edit.
		 */
		private void cancel() {

			// restore info
			inputNames = iNCopy;
			outputNames = oNCopy;
			table = tcopy;

			// tell caller what happened
			cancelled = true;
			dispose();
		} // end of cancel method

	} // end of TTEditor class

	/**
	 * Add a new input signal to the truth table.
	 * 
	 * @param signal The new input signal name.
	 */
	public void addInput(String signal) {

		// ignore empty input
		if (signal.equals("")) 
			return;

		// don't allow duplicate names
		for (String name : inputNames) {
			if (signal.equals(name)) {
				JOptionPane.showMessageDialog(edit,"duplicate signal name", "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
		}
		for (String name : outputNames) {
			if (signal.equals(name)) {
				JOptionPane.showMessageDialog(edit,"duplicate signal name", "Error",
						JOptionPane.ERROR_MESSAGE);
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
			disp.doLayout(inputNames,outputNames,table,null);
			disp.repaint();
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
		disp.doLayout(inputNames,outputNames,table,null);
		disp.repaint();
		anyChanges = true;
	} // end of addInput method

	/**
	 * Add a new output signal to the truth table.
	 * 
	 * @param signal The new output signal name.
	 */
	public void addOutput(String signal) {

		// ignore empty input
		if (signal.equals("")) 
			return;

		// don't allow duplicate names
		for (String name : inputNames) {
			if (signal.equals(name)) {
				JOptionPane.showMessageDialog(edit,"duplicate signal name", "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
		}
		for (String name : outputNames) {
			if (signal.equals(name)) {
				JOptionPane.showMessageDialog(edit,"duplicate signal name", "Error",
						JOptionPane.ERROR_MESSAGE);
				return;
			}
		}

		// can't add an output until there is at least one input
		if (inputNames.size() == 0) {
			JOptionPane.showMessageDialog(edit,"add at least one input first", "Error",
					JOptionPane.ERROR_MESSAGE);
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
		disp.doLayout(inputNames,outputNames,table,null);
		disp.repaint();
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
				JOptionPane.showMessageDialog(edit,"cannot remove: output conflict",
						"Error", JOptionPane.ERROR_MESSAGE);
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
		disp.doLayout(inputNames,outputNames,table,null);
		disp.repaint();
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
		disp.doLayout(inputNames,outputNames,table,null);
		disp.repaint();
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
		disp.doLayout(inputNames,outputNames,table,null);
		disp.repaint();
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
			JOptionPane.showMessageDialog(edit,"not possible", "Error",
					JOptionPane.ERROR_MESSAGE);
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
		disp.doLayout(inputNames,outputNames,table,null);
		disp.repaint();
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
				disp.doLayout(inputNames,outputNames,table,null);
				disp.repaint();
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
		disp.doLayout(inputNames,outputNames,table,null);
		disp.repaint();
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

		String newSignal = getNewName(signal);
		if (newSignal == null)
			return;

		int pos = inputNames.indexOf(signal);
		inputNames.set(pos,newSignal);
		disp.doLayout(inputNames,outputNames,table,null);
		disp.repaint();
		anyChanges = true;
	} // end of renameInput method

	/**
	 * Rename output signal.
	 * 
	 * @param signal Current output signal name.
	 */
	public void renameOutput(String signal) {

		String newSignal = getNewName(signal);
		if (newSignal == null)
			return;

		int pos = outputNames.indexOf(signal);
		outputNames.set(pos,newSignal);
		disp.doLayout(inputNames,outputNames,table,null);
		disp.repaint();
		anyChanges = true;
	} // end of renameOutput method

	/**
	 * Get new signal name.
	 * Check for invalid name and duplicate names.
	 * 
	 * @param signal Current signal name.
	 * 
	 * @return new signal name, or null if invalid or duplicate or canceled.
	 */
	private String getNewName(String signal) {

		// get name
		String newSignal =
			JOptionPane.showInputDialog(edit,"Enter new output signal name");

		if (newSignal == null)
			return null;

		// trim off junk
		newSignal = newSignal.trim();

		// don't allow null
		if (newSignal.equals("")) {
			JOptionPane.showMessageDialog(edit,"invalid name", "Error",
					JOptionPane.ERROR_MESSAGE);
			return null;
		}

		// don't allow duplicate names
		for (String name : inputNames) {
			if (newSignal.equals(name)) {
				JOptionPane.showMessageDialog(edit,"duplicate signal name");
				return null;
			}
		}
		for (String name : outputNames) {
			if (newSignal.equals(name)) {
				JOptionPane.showMessageDialog(edit,"duplicate signal name");
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
		disp.doLayout(inputNames,outputNames,table,null);
		disp.repaint();
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
		disp.doLayout(inputNames,outputNames,table,null);
		disp.repaint();
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
		disp.doLayout(inputNames,outputNames,table,null);
		disp.repaint();
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
		disp.doLayout(inputNames,outputNames,table,null);
		disp.repaint();
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
	 */
	public int getDefaultDelay() {

		return defaultDelay;
	} // end of getDefaultDelay method

	/**
	 * Reset propagation delay to default value.
	 */
	public void resetPropDelay() {

		propDelay = getDefaultDelay();
	} // end of resetPropDelay method

	/**
	 * Combinational logic has timing info (propagation delay).
	 * 
	 * @return true.
	 */
	public boolean hasTiming() {

		return true;
	} // end of hasTiming method

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

	//-------------------------------------------------------------------------------
	// Simulation
	//-------------------------------------------------------------------------------

	private int[] toBeValue;
	class Out {
		int position;
		BitSet value;
	}

	/**
	 * Initialize this element by setting its output pins and to-be values to 0.
	 * 
	 * @param sim Unused.
	 */
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

			//System.out.println("matched row " + matchingRow);

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
