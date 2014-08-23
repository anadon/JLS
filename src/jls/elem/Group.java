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
		// determine width
		width = 0;
		for(Entry e : ranges) {
			width = Math.max(width, fm.stringWidth(e.toCircuitString()));
		}
		width = (width+2*s)/s*s;
	
		// determine height
		height = (puts+1)*s;

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
			if(value.equals("LEFT"))
			{
				orientation = JLSInfo.Orientation.LEFT;
			}
			else if(value.equals("RIGHT"))
			{
				orientation = JLSInfo.Orientation.RIGHT;
			}
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
	protected class GroupCreate extends JDialog implements ActionListener {
		
		// properties
		private JButton ok = new JButton("OK");
		private JButton cancel = new JButton("Cancel");
		private JTextField bitsField = new JTextField(defaultBits+"",10);
		private KeyPad bitsPad = new KeyPad(bitsField,10,defaultBits,this);
		private JRadioButton single = new JRadioButton("Single Bits");
		private JRadioButton group = new JRadioButton("Group Bits");
		private JRadioButton left = new JRadioButton("Left");
		private JRadioButton right = new JRadioButton("Right", true);
		private String type;
		
		/**
		 * Set up create dialog window.
		 * 
		 * @param x The x-coordinate of the position of the dialog.
		 * @param y The y-coordinate of the position of the dialog.
		 */
		protected GroupCreate(int x, int y, String type) {
			
			// set up window title
			super(JLSInfo.frame,"Create " + type,true);
			
			// save type
			this.type = type;
			
			// set not cancelled
			cancelled = false;
			
			// set up window
			Container window = getContentPane();
			window.setLayout(new BoxLayout(window,BoxLayout.Y_AXIS));
			
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
			JPanel orients = new JPanel(new GridLayout(1,3));
			orients.add(left);
			orients.add(new JLabel(""));
			orients.add(right);
			left.setHorizontalAlignment(SwingConstants.CENTER);
			right.setHorizontalAlignment(SwingConstants.CENTER);
			ButtonGroup gr = new ButtonGroup();
			gr.add(left);
			gr.add(right);
			window.add(orients);
			
			// set up ok and cancel buttons
			window.add(new JLabel(" "));
			JPanel okCancel = new JPanel(new GridLayout(1,2));
			ok.setBackground(Color.green);
			okCancel.add(ok);
			cancel.setBackground(Color.pink);
			okCancel.add(cancel);
			JButton help = new JButton("help");
			if (JLSInfo.hb == null) {
				Util.noHelp(help);
			}
			else {
				if (type.equals("Unbundler")) {
					JLSInfo.hb.enableHelpOnButton(help,"unbundle",null);
				}
				else {
					JLSInfo.hb.enableHelpOnButton(help,"bundle",null);
				}
			}
			okCancel.add(help);
			window.add(okCancel);
			getRootPane().setDefaultButton(ok);
			ok.addActionListener(this);
			bitsField.addActionListener(this);
			cancel.addActionListener(this);
			
			// set up window close listener to cancel gate
			setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			addWindowListener (
					new WindowAdapter() {
						public void windowClosing(WindowEvent e) {
							cancel();
						}
					}
			);
			
			// finish up GUI
			pack();
			Dimension d = getSize();
			setLocation(x-d.width/2,y-d.height/2);
			setVisible(true);
		} // end of constructor
		
		/**
		 * React to ok, reset and cancel buttons.
		 * 
		 * @param event The event object for this action.
		 */
		public void actionPerformed(ActionEvent event) {
			
			if (event.getSource() == ok || event.getSource() == bitsField) {
				try {
					bits = Integer.parseInt(bitsField.getText());
				}
				catch (NumberFormatException ex) {
					JOptionPane.showMessageDialog(this,
							"Value not numeric, try again", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				if (bits < 2) {
					JOptionPane.showMessageDialog(this,
							"Must be at least 2 bits", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				// set up ranges
				if (single.isSelected()) {
					for (int b=0; b<bits; b+=1) {
						ranges.add(new Entry(b, b));
					}
				}
				else {
					Point here = getLocation();
					dispose();
					new GetRanges(here,type);
				}
				if(left.isSelected())
				{
					orientation = JLSInfo.Orientation.LEFT;
				}
				else if(right.isSelected())
				{
					orientation = JLSInfo.Orientation.RIGHT;
				}
				dispose();
			}
			else if (event.getSource() == cancel) {
				cancel();
			}
			
		} // end of actionPerformed method
		
		/**
		 * Cancel this element.
		 */
		private void cancel() {
			
			cancelled = true;
			dispose();
		} // end of cancel method
		
	} // end of GroupCreate class
	
	/**
	 * Get bit group info.
	 */
	protected class GetRanges extends JDialog implements ActionListener {
		
		// properties
		private DefaultListModel pick = new DefaultListModel();
		private JList choose = new JList(pick); // LeftList?
		private DefaultListModel picked = new DefaultListModel();
		private JList chosen = new JList(picked);
		private JButton add = new JButton(">>");
		private JButton remove = new JButton("<<");
		private JButton upjumper = new JButton("Move bundle to top");
		private JButton upshifter = new JButton("Move bundle up");
		private JButton downshifter = new JButton("Move bundle down");
		private JButton downjumper = new JButton("Move bundle to bottom");
		private JButton ok = new JButton("OK");
		private JButton cancel = new JButton("Cancel");
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
		}
		
		/**
		 * Construct dialog.
		 * 
		 * @param where The location of the dialog.
		 * @param type Either "Bundler" or "Unbundler".
		 */
		public GetRanges(Point where, String type) {
			
			super(JLSInfo.frame,"Pick Bit Groups",true);
			
			this.type = type;
			
			// set up window
			Container window = getContentPane();
			window.setLayout(new BoxLayout(window,BoxLayout.Y_AXIS));
			
			// set up selections
			for (int b=0; b<bits; b+=1) {
				pick.addElement(new Entry(new int[]{b}));
			}
			choose.setSelectedIndex(0);
			
			// set up automatic disabling of already-bundled bits
			choose.setCellRenderer(new DefaultListCellRenderer() {
				@Override
				public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
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
			
			// set up ok and cancel buttons
			window.add(new JLabel(" "));
			JPanel okCancel = new JPanel(new GridLayout(1,2));
			ok.setBackground(Color.green);
			okCancel.add(ok);
			cancel.setBackground(Color.pink);
			okCancel.add(cancel);
			JButton help = new JButton("help");
			if (JLSInfo.hb == null)
				Util.noHelp(help);
			else {
				if (type.equals("unbundle")) {
					JLSInfo.hb.enableHelpOnButton(help,"unbundle",null);
				}
				else {
					JLSInfo.hb.enableHelpOnButton(help,"bundle",null);
				}
			}
			
			okCancel.add(help);
			window.add(okCancel);
			getRootPane().setDefaultButton(ok);
			
			// set up listeners
			add.addActionListener(this);
			remove.addActionListener(this);
			ok.addActionListener(this);
			cancel.addActionListener(this);
			upjumper.addActionListener(this);
			upshifter.addActionListener(this);
			downshifter.addActionListener(this);
			downjumper.addActionListener(this);
			
			// set up window close listener to cancel gate
			setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			addWindowListener (
					new WindowAdapter() {
						public void windowClosing(WindowEvent e) {
							cancel();
						}
					}
			);
			
			// finish up GUI
			pack();
			setLocation(where.x,where.y);
			setVisible(true);
		} // end of constructor
		
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
			else if (event.getSource() == ok) {
				
				// cancel if there are no ranges
				if (picked.size() == 0) {
					JOptionPane.showMessageDialog(this,
							"No groups chosen", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				// generate range entries
				for (int i=0; i<picked.size(); i+=1) {
					Entry e = (Entry)(picked.elementAt(i));
					ranges.add(new Entry(e.getValues()));
				}
				dispose();
			}
			else if (event.getSource() == cancel) {
				cancel();
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
		private void cancel() {
			
			cancelled = true;
			dispose();
		} // end of cancel method
		
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
