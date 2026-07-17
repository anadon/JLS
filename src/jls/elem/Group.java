package jls.elem;

import jls.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import javax.swing.*;

import java.util.*;

/**
 * Superclass of binder/splitter.
 * Contains common info and method.
 * 
 * @author David A. Poplawski
 */
public abstract class Group extends LogicElement {
	
	// default values
	private static final int defaultBits = 2;

	// one constraint string, two surfaces: dialog and loader (issue #52)
	static final String BITS_CONSTRAINT = "Must be at least 2 bits";
	static final String INDEX_CONSTRAINT =
			"group wire indices must be non-negative";

	/**
	 * The bundle-width rule, shared by the dialog and the loader.
	 *
	 * @param bits The proposed number of bundled bits.
	 *
	 * @return the violated constraint message, or null if valid.
	 */
	static String checkBits(int bits) {

		return bits < 2 ? BITS_CONSTRAINT : null;
	} // end of checkBits method

	// saved properties
	protected int bits = defaultBits;
	//protected SortedSet<GetRanges.Entry> ranges;
	protected ArrayList<Entry> ranges;
	protected boolean triState = false;
	protected JLSInfo.Orientation orientation = JLSInfo.Orientation.RIGHT;
	
	// running properties
	protected boolean cancelled;
	protected boolean loadTriState = false;
	protected boolean noncontig = false; // False only for legacy saves
	
	/**
	 * Create a new splitter/binder element.
	 * 
	 * @param circuit The circuit this element is part of.
	 */
	public Group(Circuit circuit) {
		super(circuit);
		ranges = new ArrayList<Entry>();
	} // end of constructor
	
	/**
	 * Initialize internal info for this element.
	 * 
	 * @param g The Graphics object to use.
	 */
	public void init(Graphics g) {
		
		// no need to do anything if no graphics object
		if (g == null)
			return;
		
		// do nothing if element already has a size
		if (width != 0 || height != 0)
			return;
		
		// set up
		FontMetrics fm = g.getFontMetrics();
		int s = JLSInfo.spacing;
		int puts = ranges.size();

		// the size decomposes into an across axis (from the narrow puts
		// to the bundle side, sized by the widest range label and
		// snapped to the grid) and an along axis (one grid step per
		// put); orientation decides which axis is which (issue #124,
		// re-derived from AmityWilder's vertical-groups branch)
		int across = 0;
		for(Entry e : ranges) {
			across = Math.max(across, fm.stringWidth(e.toCircuitString()));
		}
		across = (across+2*s)/s*s;
		int along = (puts+1)*s;

		if (orientation == JLSInfo.Orientation.LEFT
				|| orientation == JLSInfo.Orientation.RIGHT) {
			width = across;
			height = along;
		}
		else {
			width = along;
			height = across;
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
		
		// draw the box if some input or output is unattached
		boolean doit = false;
		for (Input input : inputs) {
			if (!input.isAttached())
				doit = true;
		}
		for (Output output : outputs) {
			if (!output.isAttached()) {
				doit = true;
			}
		}
		if (doit) {
			g.setColor(Color.gray);
			g.drawRect(x,y,width,height);
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
			String violated = checkBits(value);
			if (violated != null) {
				throw new IllegalArgumentException(violated);
			}
			bits = value;
		} else if (name.equals("tristate")) {
			if (value == 1)
				loadTriState = true;
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
		
		if (name.equals("orient")) {
			// unknown strings leave the orientation unchanged, matching
			// the historical loaders (issue #124: all four orientations)
			orientation = JLSInfo.Orientation.parse(value, orientation);
		} else if(name.equals("noncontig")) {
			if(value.equals("true")) noncontig = true;
			else noncontig = false;
		} else {
			super.setValue(name,value);
		}
	} // end of setValue method
	
	/**
	 * This method will flip a group
	 * @param g The current graphics context to facilitate recalculation of size when flipping
	 */
	public void flip(Graphics g)
	{
		orientation = orientation.flipped();
		inputs.clear();
		outputs.clear();
		width = 0;
		height = 0;
	}

	/**
	 * This method will rotate the group a quarter turn (issue #124).
	 * Subclasses re-run init to rebuild the puts on the new axes.
	 * @param direction The direction to rotate
	 * @param g The current graphics context for use in recalculating size
	 */
	public void rotate(JLSInfo.Orientation direction, Graphics g)
	{
		if(direction == JLSInfo.Orientation.LEFT)
		{
			orientation = orientation.ccw();
		}
		else if(direction == JLSInfo.Orientation.RIGHT)
		{
			orientation = orientation.cw();
		}
		inputs.clear();
		outputs.clear();
		width = 0;
		height = 0;
	}

	/**
	 * Tells if a Group is capable of rotating: the same no-attachments
	 * constraint as flipping, since both rebuild every put.
	 * @return False if any input or output has a wire attached, True otherwise
	 */
	public boolean canRotate()
	{
		return canFlip();
	}

	/**
	 * Tells if a Group is capable of flippiing, can only flip when inputs or outputs have no attachments.
	 * @return False if any input or output has a wire attached, True otherwise
	 */
	public boolean canFlip()
	{
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
	}
	
	/**
	 * Set a pair of int instance variable values (during a load).
	 * 
	 * @param v1 The first value.
	 * @param v1 The second value.
	 */
	public void setPair(int v1, int v2) {
		// a malformed file used to NPE/AIOOBE here; reject with a real
		// message and bound the sparse-list growth (issue #52)
		if (v1 < 0 || v2 < 0) {
			throw new IllegalArgumentException(
					INDEX_CONSTRAINT + " (" + v1 + "," + v2 + ")");
		}
		if (noncontig && v1 > 4096) {
			throw new IllegalArgumentException(
					"group wire index " + v1 + " is implausibly large");
		}
		if(noncontig) {
			// Newer save file: more complex, but more features
			Entry e;
			if(ranges.size() <= v1 || ranges.get(v1) == null) {
				e = new Entry(new int[]{});
			}
			else {
				e = ranges.get(v1);
			}
			int[] s1 = e.getValues().clone();
			int[] s2 = new int[s1.length + 1];
			System.arraycopy(s1, 0, s2, 0, s1.length);
			s2[s2.length - 1] = v2;
			e.setValues(s2);
			while(ranges.size() <= v1) {
				ranges.add(null);
			}
			ranges.set(v1, e);
		} else {
			// Legacy save file: sorted, contiguous ranges
			ranges.add(new Entry(v1, v2));
		}
	} // end of setPair method
	
	/**
	 * The bit routing of this binder/splitter, one entry per bundled
	 * put, in put order: entry k holds the bundle-side bit indices the
	 * k-th narrow put's bits map to, in narrow-put bit order (bit 0
	 * first). For a Binder the narrow puts are its inputs; for a
	 * Splitter, its outputs. Exposed for consumers of the circuit
	 * model, e.g. the HDL exporter (issue #60); the Entry class itself
	 * stays internal.
	 *
	 * @return a fresh list of fresh index arrays.
	 */
	public ArrayList<int[]> getRangeIndices() {

		ArrayList<int[]> indices = new ArrayList<int[]>(ranges.size());
		for (Entry e : ranges) {
			indices.add(e.getValues());
		}
		return indices;
	} // end of getRangeIndices method

	/**
	 * Save this element.
	 *
	 * @param output The output writer.
	 */
	public void save(PrintWriter output) {

		super.save(output);
		output.println(" int bits " + bits);
		output.println(" String orient \"" + orientation.toString() + "\"");
		// noncontig is always true for newer files. Without this, JLS assumes false (legacy save)
		output.println(" String noncontig \"true\"");
		if (triState) {
			output.println(" int tristate 1");
		}
		else {
			output.println(" int tristate 0");
		}
		
		int r = 0;
		for(Entry e : ranges) {
			for(int i : e.getValues()) {
				output.println(" pair " + r + " " + i);
			}
			r += 1;
		}
		output.println("END");
	} // end of save method

	/**
	 * A vertically-oriented group is a format-version-2 feature: a
	 * version-1 reader ignores the UP/DOWN orient value and silently
	 * loads the group horizontal, so files that use it must declare
	 * version 2 to be refused cleanly by older readers instead
	 * (issue #124 per the docs/file-format.md section 9 policy).
	 *
	 * @return 2 if this group is vertical, 1 otherwise.
	 */
	public int saveFormatVersion() {

		if (orientation == JLSInfo.Orientation.UP
				|| orientation == JLSInfo.Orientation.DOWN) {
			return 2;
		}
		return 1;
	} // end of saveFormatVersion method
	
	/**
	 * Copy values to new object.
	 * 
	 * @param it The new object.
	 */
	public void copy(Group it) {
		
		it.bits = bits;
		it.ranges = new ArrayList<Entry>(ranges);
		it.triState = triState;
		it.orientation = orientation;
		for (Input input : inputs) {
			it.inputs.add(input.copy(it));
		}
		for (Output output : outputs) {
			it.outputs.add(output.copy(it));
		}
		super.copy(it);
		return;
	} // end of copy method
	
	/**
	 * Dialog box to set bits.
	 */
	@SuppressWarnings("serial")
	protected class GroupCreate extends ElementDialog {

		// properties
		private JTextField bitsField = new JTextField(defaultBits+"",10);
		private KeyPad bitsPad = new KeyPad(bitsField,10,defaultBits,this);
		private JRadioButton single = new JRadioButton("Single Bits");
		private JRadioButton group = new JRadioButton("Group Bits");
		private JRadioButton left = new JRadioButton("Left");
		private JRadioButton right = new JRadioButton("Right", true);
		private JRadioButton up = new JRadioButton("Up");
		private JRadioButton down = new JRadioButton("Down");
		private String type;
		
		/**
		 * Set up create dialog window.
		 * 
		 */
		protected GroupCreate(String type) {

			// set up window title
			super("Create " + type,
					type.equals("Unbundler") ? "unbundle" : "bundle");

			// save type
			this.type = type;

			// set not cancelled
			cancelled = false;

			// set up window
			Container window = getContentPane();

			// set up inputs
			JPanel info = new JPanel(new BorderLayout());
			JLabel bits;
			if (type.equals("Unbundler")) {
				bits = new JLabel("Input Bits: ",SwingConstants.RIGHT);
			}
			else {
				bits = new JLabel("Output Bits: ",SwingConstants.RIGHT);
			}
			info.add(bits,BorderLayout.WEST);
			info.add(bitsField,BorderLayout.CENTER);
			info.add(bitsPad,BorderLayout.EAST);
			window.add(info);
			
			// set up single bits or grouped choice
			window.add(new JLabel(" "));
			JPanel choice = new JPanel(new GridLayout(1,2));
			single.setSelected(true);
			choice.add(single);
			choice.add(group);
			window.add(choice);
			ButtonGroup choices = new ButtonGroup();
			choices.add(single);
			choices.add(group);
			
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
			gr.add(up);
			gr.add(down);
			window.add(orients);

			confirmOnEnter(bitsField);
			finishDialog();
		} // end of constructor

		/**
		 * Check the bit count against the shared group constraint (issue
		 * #52): a rejected dialog must leave the element unchanged.
		 */
		protected java.util.List<Violation> validateInputs() {

			int newBits;
			try {
				newBits = Integer.parseInt(bitsField.getText());
			}
			catch (NumberFormatException ex) {
				return java.util.List.of(new Violation(
						"Value not numeric, try again", bitsField));
			}
			String violated = checkBits(newBits);
			if (violated != null) {
				return java.util.List.of(new Violation(violated, bitsField));
			}
			return java.util.List.of();
		} // end of validateInputs method

		/**
		 * Create the bundler/unbundler from the validated form.
		 */
		protected void validateAndAccept() {

			bits = Integer.parseInt(bitsField.getText());

			// set up ranges
			if (single.isSelected()) {
				for (int b=0; b<bits; b+=1) {
					ranges.add(new Entry(b, b));
				}
			}
			else {
				dispose();
				new GetRanges(type);
			}
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
			dispose();
		} // end of validateAndAccept method

		/**
		 * Cancel this element.
		 */
		protected void cancelDialog() {

			cancelled = true;
			dispose();
		} // end of cancelDialog method

	} // end of GroupCreate class
	
	/**
	 * Get bit group info.
	 */
	@SuppressWarnings("serial")
	protected class GetRanges extends ElementDialog implements ActionListener {

		// properties
		private DefaultListModel<Entry> pick = new DefaultListModel<Entry>();
		private JList<Entry> choose = new JList<Entry>(pick); // LeftList?
		private DefaultListModel<Entry> picked = new DefaultListModel<Entry>();
		private JList<Entry> chosen = new JList<Entry>(picked);
		private JButton add = new JButton(">>");
		private JButton remove = new JButton("<<");
		private JButton upjumper = new JButton("Move bundle to top");
		private JButton upshifter = new JButton("Move bundle up");
		private JButton downshifter = new JButton("Move bundle down");
		private JButton downjumper = new JButton("Move bundle to bottom");
		private String type;
		
		/**
		 * Dummy constructor, for internal use only.
		 * Permits use of a kludgy fix that permits instantiation
		 * of an Entry from within a GroupCreate object. Since Entry
		 * is a subclass of GetRanges, which is in turn a sibling of
		 * GroupCreate, Java will not allow GroupCreate to instantiate
		 * an Entry unless it has a reference to an instance of
		 * GetRanges. This protected constructor allows us to do
		 * exactly that, without creating the entire Swing dialog.
		 */
		protected GetRanges() {
			// No-op. This object will get garbage collected.
			super("Pick Bit Groups",null);
		}
		
		/**
		 * Construct dialog.
		 * 
		 * @param type Either "Bundler" or "Unbundler".
		 */
		public GetRanges(String type) {

			super("Pick Bit Groups",
					type.equals("unbundle") ? "unbundle" : "bundle");

			this.type = type;

			// set up window
			Container window = getContentPane();

			// set up selections
			for (int b=0; b<bits; b+=1) {
				pick.addElement(new Entry(new int[]{b}));
			}
			choose.setSelectedIndex(0);
			
			// set up automatic disabling of already-bundled bits
			choose.setCellRenderer(new DefaultListCellRenderer() {
				@Override
				public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
					boolean allow = true;
					Entry e = (Entry) pick.get(index);
					if(e.isPicked()) {
						allow = false;
					}
					super.getListCellRendererComponent(list, value, index, isSelected && allow, cellHasFocus && allow);
					return this;
			    }
			});
			
			// set up list display
			JPanel lists = new JPanel(new FlowLayout());
			
			JPanel leftList = new JPanel(new BorderLayout());
			JLabel left = new JLabel("choose from",SwingConstants.CENTER);
			leftList.add(left,BorderLayout.NORTH);
			JScrollPane chpane = new JScrollPane(choose);
			leftList.add(chpane,BorderLayout.CENTER);
			lists.add(leftList);
			
			JPanel buttons = new JPanel(new GridLayout(2,1));
			buttons.add(add);
			buttons.add(remove);
			lists.add(buttons);
			
			JPanel rightList = new JPanel(new BorderLayout());
			JLabel right = new JLabel("chosen",SwingConstants.CENTER);
			rightList.add(right,BorderLayout.NORTH);
			chosen.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			JScrollPane rpane = new JScrollPane(chosen);
			rightList.add(rpane,BorderLayout.CENTER);
			lists.add(rightList);
			
			JPanel shifters = new JPanel(new GridLayout(4,1));
			shifters.add(upjumper);
			shifters.add(upshifter);
			shifters.add(downshifter);
			shifters.add(downjumper);
			lists.add(shifters);
			
			window.add(lists);

			// set up listeners
			add.addActionListener(this);
			remove.addActionListener(this);
			upjumper.addActionListener(this);
			upshifter.addActionListener(this);
			downshifter.addActionListener(this);
			downjumper.addActionListener(this);

			finishDialog();
		} // end of constructor

		/**
		 * There must be at least one chosen group.
		 */
		protected java.util.List<Violation> validateInputs() {

			if (picked.size() == 0) {
				return java.util.List.of(new Violation("No groups chosen",
						chosen));
			}
			return java.util.List.of();
		} // end of validateInputs method

		/**
		 * Create the range entries from the validated choices.
		 */
		protected void validateAndAccept() {

			// generate range entries
			for (int i=0; i<picked.size(); i+=1) {
				Entry e = (Entry)(picked.elementAt(i));
				ranges.add(new Entry(e.getValues()));
			}
			dispose();
		} // end of validateAndAccept method
		
		/**
		 * React to ok, reset and cancel buttons.
		 * 
		 * @param event The event object for this action.
		 */
		public void actionPerformed(ActionEvent event) {
			if (event.getSource() == add) {
				
				// make sure no bit is already picked
				// (only can happen for a Bundler)
				
				int[] selected = choose.getSelectedIndices();
				for(int s : selected) {
					Entry ent = (Entry)(pick.elementAt(s));
					if (ent.isPicked()) {
						choose.removeSelectionInterval(s, s);
					}
				}
				
				selected = choose.getSelectedIndices();
				
				// Don't go indexing things that don't exist
				if(selected.length == 0) return;
				
				// add entry to the end of the list (user can resort it themselves)
				picked.addElement(new Entry(selected));
				
				// mark chosen bits as picked if a bundler
				if (type.equals("Bundler")) {
					for (int p : selected) {
						Entry ent = (Entry)(pick.elementAt(p));
						ent.setPicked(true);
						pick.setElementAt(ent,p);
					}
				}
				
				// move selection to next unchosen bit
				int first = -1;
				int where = (selected[selected.length - 1]+1)%pick.size();
				for (int i=0; i<pick.size(); i+=1) {
					Entry ent = (Entry)(pick.elementAt(where));
					if (!ent.isPicked()) {
						first = where;
						break;
					}
					where = (where+1)%pick.size();
				}
				if (first != -1) {
					choose.setSelectedIndex(first);
				}
				else {
					choose.clearSelection();
				}
			}
			
			else if (event.getSource() == remove) {
				
				// remove from chosen list (if something to remove)
				int where = chosen.getSelectedIndex();
				if (where < 0)
					return;
				Entry ent = (Entry)picked.elementAt(where);
				picked.removeElement(ent);
				
				for(int i : ent.getValues()) {
					Entry e = (Entry) pick.elementAt(i);
					e.setPicked(false);
					pick.setElementAt(e, i);
				}
			}
			else if (event.getSource() == upjumper) {
				int selected = chosen.getSelectedIndex();
				Entry a = (Entry) picked.getElementAt(selected);
				while(selected > 0) {
					Entry b = (Entry) picked.getElementAt(selected - 1);
					picked.setElementAt(b, selected);
					picked.setElementAt(a, selected - 1);
					selected -= 1;
				}
				chosen.setSelectedIndex(0);
			}
			else if (event.getSource() == upshifter) {
				int selected = chosen.getSelectedIndex();
				if(selected > 0) {
					Entry a = (Entry) picked.getElementAt(selected);
					Entry b = (Entry) picked.getElementAt(selected - 1);
					picked.setElementAt(b, selected);
					picked.setElementAt(a, selected - 1);
					chosen.setSelectedIndex(selected - 1);
				}
			}
			else if (event.getSource() == downshifter) {
				int selected = chosen.getSelectedIndex();
				if(selected >= 0 && selected < picked.getSize() - 1) {
					Entry a = (Entry) picked.getElementAt(selected);
					Entry b = (Entry) picked.getElementAt(selected + 1);
					picked.setElementAt(b, selected);
					picked.setElementAt(a, selected + 1);
					chosen.setSelectedIndex(selected + 1);
				}
			}
			else if (event.getSource() == downjumper) {
				int selected = chosen.getSelectedIndex();
				Entry a = (Entry) picked.getElementAt(selected);
				while (selected >= 0 && selected < picked.getSize() - 1) {
					Entry b = (Entry) picked.getElementAt(selected + 1);
					picked.setElementAt(b, selected);
					picked.setElementAt(a, selected + 1);
					selected += 1;
				}
				chosen.setSelectedIndex(picked.getSize() - 1);
			}
		} // end of actionPerformed method

		/**
		 * Cancel this gate.
		 */
		protected void cancelDialog() {

			cancelled = true;
			dispose();
		} // end of cancelDialog method

	} // end of GetRanges class
	
	/**
	 * A bit range entry.
	 */
	protected class Entry {
		
		// properties
		//private int from;
		//private int to;
		private int[] values;
		private boolean picked;
		
		/**
		 * Create an entry storing the given values
		 * @param values what values to save
		 */
		public Entry(int[] values) {
			this.values = values.clone();
		}
		
		/**
		 * Create an entry storing every value between min and max (inclusive)
		 * @param min lowest value to store
		 * @param max highest value to store
		 */
		public Entry(int min, int max) {
			this.values = new int[1 + max - min];
			for(int i = min; i <= max; i += 1) {
				values[i - min] = i;
			}
		}
		
		/**
		 * Find minimum value.
		 * 
		 * @return lowest index in values, or -1 if values is empty
		 */
		public int getMin() {
			if(values != null && values.length > 0)
				return values[0];
			else
				return -1;
		}
		
		/**
		 * Find maximum value.
		 * 
		 * @return highest index in values, or -1 if values is empty
		 */
		public int getMax() {
			if(values != null && values.length > 0)
				return values[values.length - 1];
			else
				return -1;
		}
		
		/**
		 * Return a copy of the saved set of indices
		 * 
		 * @return int[] of indices
		 */
		public int[] getValues() {
			return values.clone();
		}
		
		/**
		 * Save a new set of values
		 * 
		 * @param values New set of values to use
		 */
		public void setValues(int[] values) {
			this.values = values.clone();
		}
		
		/**
		 * Return number of stored elements
		 */
		public int getSize() {
			return values.length;
		}
		
		/**
		 * Set/reset picked flag.
		 * 
		 * @param which True to set, false to reset.
		 */
		public void setPicked(boolean which) {
			
			picked = which;
		} // end of setPicked method
		
		/**
		 * See if picked.
		 * 
		 * @return true if picked, false if not.
		 */
		public boolean isPicked() {
			
			return picked;
		} // end of isPicked method
		
		/**
		 * Convert to a string, either a single number if from and to are equal,
		 * or a range if not (e.g., "9 - 5"). Note if range is non-continuous
		 * 
		 * @return The string.
		 */
		public String toString() {
			
			String p = "";
			if (picked) {
				p = " (bundled)";
			}
			return toCircuitString() + p;
		} // end of toString method
		
		public String toCircuitString() {
			if(getMin() == getMax()) {
				return String.valueOf(getMin());	
			}
			else if (getMax() - getMin() + 1 == values.length) {
				return getMax() + "-" + getMin();
			}
			else {
				String s = "";
				ArrayList<Integer> l = new ArrayList<Integer>();
				int i;
				
				// Create a reversed version for consistency with dialog
				int[] v = new int[values.length];
				for(i = 0; i < values.length; i += 1) {
					v[values.length - i - 1] = values[i];
				}
				
				i = 0;
				while(i < v.length) {
					if(l.isEmpty())
						l.add(v[i]);
					else {
						if(v[i] == v[i-1] - 1)
							l.add(v[i]);
						else {
							if(!s.isEmpty())
								s += ", ";
							if(l.size() > 1)
								s += l.get(0) + "-" + l.get(l.size() - 1);
							else if(!l.isEmpty())
								s += l.get(0);
							l.clear();
							l.add(v[i]);
						}
					}
					i += 1;
				}
				if(!s.isEmpty() && !l.isEmpty())
					s += ", ";
				if(l.size() > 1)
					s += l.get(0) + "-" + l.get(l.size() - 1);
				else if(!l.isEmpty())
					s += l.get(0);
				l.clear();
				return s;
			}
		} // end of toCircuitString method
		
	} // end of Entry class
	
} // end of Group class
