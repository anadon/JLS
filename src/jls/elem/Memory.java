package jls.elem;

import jls.*;
import jls.sim.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.*;
import javax.swing.*;
import java.math.*;
import java.util.*;

/**
 * Memory element, initialized internally or from a file.
 * Implements either ROM or SRAM (no DRAM).
 * 
 * @author David A. Poplawski
 */
public class Memory extends LogicElement {
	
	// types
	private static enum Type {RAM,ROM};
	
	// default values
	private static final String defaultName = "";
	private static final Type defaultType = Type.RAM;
	private static final int defaultBits = 1;
	private static final int defaultCapacity = 2;
	private static final String defaultFileName = "";
	private static final int defaultAccessTime = 100; 
	private static final String defaultInitialValue = "";
	
	// saved properties
	private String name = defaultName;
	private Type type = defaultType;
	private int bits = defaultBits;
	private int capacity = defaultCapacity;
	private String fileName = defaultFileName;
	private int accessTime = defaultAccessTime;
	private String initialValue = defaultInitialValue;
	private boolean watched = false;
	
	// running properties
	private boolean cancelled;
	private boolean changed;
	private String specs;
	
	/**
	 * Create a new memory element.
	 * 
	 * @param circuit The circuit this element is part of.
	 */
	public Memory(Circuit circ) {
		
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
		Point pos = editWindow.getMousePosition();
		Point win = editWindow.getLocationOnScreen();
		if (pos == null) {
			new MemoryEdit(x+win.x,y+win.y,true);
		}
		else {
			new MemoryEdit(pos.x+win.x,pos.y+win.y,true);
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
		if (p != null) {
			super.setXY(p.x-width/2,p.y-height/2);
		}
		
		return true;
	} // end of setup method
	
	/**
	 * Initialize internal info for this element.
	 * Figures out height and width using font info from graphics object.
	 * Note input positions: RAM-> 0 - addr, 1 - input, 2 - WE, 3 - OE, 4 - CS
	 *                       ROM-> 0 - addr, 1 - OE, 2 - CS
	 * 
	 * @param g The Graphics object to use.
	 */
	public void init(Graphics g) {
		
		// set up
		if (type == Type.RAM) {
			specs = " " + capacity + "x" + bits + " RAM ";
		}
		else {
			specs = " " + capacity + "x" + bits + " ROM ";
		}
		
		// set up size if there is a graphics object
		int s = JLSInfo.spacing;
		if (g != null) {
			
			// set up size if it doesn't already have one
			if (width == 0 && height == 0) {
				FontMetrics fm = g.getFontMetrics();
				int w1 = (fm.stringWidth(" " + name + " ")+s/2)/s*s;
				int minW = 0;
				if (type == Type.RAM) {
					height = 8*s;
					minW = 6*s;
				}
				else {
					height = 7*s;
					minW = 4*s;
				}
				int w2 = (fm.stringWidth(specs)+s/2)/s*s;
				int w = Math.max(w1,w2);
				width = Math.max(w,minW);
			}
		}
		
		// create inputs
		int abits = 32 - Integer.numberOfLeadingZeros(capacity-1);
		inputs.add(new Input("address",this,0,4*s,abits));
		int over = s;
		if (type == Type.RAM) {
			inputs.add(new Input("input",this,0,5*s,bits));
			inputs.add(new Input("WE",this,s,height,1));
			over += 2*s;
		}
		inputs.add(new Input("OE",this,over,height,1));
		inputs.add(new Input("CS",this,over+2*s,height,1));
		
		// create output
		Output out = new Output("output",this,width,4*s,bits);
		outputs.add(out);
		out.setTriState(true);
	} // end of init method
	
	/**
	 * Draw this element.
	 * 
	 * @param g The graphics object to draw with.
	 */
	public void draw(Graphics g) {
		
		// draw watched background
		int s = JLSInfo.spacing;
		int d2 = JLSInfo.pointDiameter/2;
		if (watched) {
			g.setColor(JLSInfo.watchColor);
			g.fillRect(x,y,width,height);
		}
		
		// draw context
		super.draw(g);
		
		// draw box
		g.setColor(Color.BLACK);
		g.drawRect(x,y,width,height);
		
		// draw specs inside box
		FontMetrics fm = g.getFontMetrics();
		int ascent = fm.getAscent();
		Rectangle2D t = fm.getStringBounds(specs,g);
		double tw = t.getWidth();
		double th = t.getHeight();
		int dx = (int)((width-tw)/2);
		int dy = (int)(s-th/2+ascent);
		g.drawString(specs,x+dx,y+dy);
		
		// draw name inside box
		t = fm.getStringBounds(name,g);
		tw = t.getWidth();
		th = t.getHeight();
		dx = (int)((width-tw)/2);
		dy = (int)(3*s);
		g.drawString(name,x+dx,y+dy);
		
		// draw address input and label
		Input addr = inputs.get(0);
		int lx = addr.getX();
		int ly = addr.getY();
		int h = fm.getAscent()+fm.getDescent();
		g.setColor(Color.black);
		g.drawString("addr",lx+d2,ly-h/2+ascent);
		addr.draw(g);
		
		// draw data input and label
		if (type == Type.RAM) {
			Input in = inputs.get(1);
			lx = in.getX();
			ly = in.getY();
			h = fm.getAscent()+fm.getDescent();
			g.setColor(Color.black);
			g.drawString("data",lx+d2,ly-h/2+ascent);
			in.draw(g);
		}
		
		// draw data output and label
		Output out = outputs.get(0);
		lx = out.getX();
		ly = out.getY();
		g.setColor(Color.black);
		g.drawString("out",lx-fm.stringWidth("out")-d2,ly-h/2+ascent);
		out.draw(g);
		
		// draw WE input and label
		int inum = 1;
		if (type == Type.RAM) {
			Input we = inputs.get(2);
			lx = we.getX();
			ly = we.getY();
			int w = fm.stringWidth("WE");
			g.setColor(Color.black);
			g.drawString("WE",lx-w/2,ly-d2-fm.getDescent());
			g.drawLine(lx-w/2,ly-h-1,lx+w/2,ly-h-1);
			we.draw(g);
			inum = 3;
		}
		
		// draw OE input and label
		Input oe = inputs.get(inum);
		lx = oe.getX();
		ly = oe.getY();
		int w = fm.stringWidth("OE");
		g.setColor(Color.black);
		g.drawString("OE",lx-w/2,ly-d2-fm.getDescent());
		g.drawLine(lx-w/2,ly-h-1,lx+w/2,ly-h-1);
		oe.draw(g);
		
		// draw CS input and label
		Input cs = inputs.get(inum+1);
		lx = cs.getX();
		ly = cs.getY();
		w = fm.stringWidth("CS");
		g.setColor(Color.black);
		g.drawString("CS",lx-w/2,ly-d2-fm.getDescent());
		g.drawLine(lx-w/2,ly-h-1,lx+w/2,ly-h-1);
		cs.draw(g);
		
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
		} else if (name.equals("cap")) {
			capacity = value;
		} else if (name.equals("time")) {
			accessTime = value;
		} else if (name.equals("watch")) {
			if (value == 0)
				watched = false;
			else
				watched = true;
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
		
		if (name.equals("name")) {
			this.name = value;
			circuit.addName(value);
		} else if (name.equals("type")) {
			if (value.equals("RAM")) {
				type = Type.RAM;
			}
			else if (value.equals("ROM")) {
				type = Type.ROM;
			}
		} else if (name.equals("file")) {
			fileName = value;
		} else if (name.equals("init")) {
			initialValue = value;
		} else {
			super.setValue(name,value);
		}
	} // end of setValue method
	
	/**
	 * Save this element.
	 * 
	 * @param output The output writer.
	 */
	public void save(PrintWriter output) {
		
		output.println("ELEMENT Memory");
		super.save(output);
		output.println(" String name \"" + name + "\"");
		output.println(" String type \"" + type + "\"");
		output.println(" int bits " + bits);
		output.println(" int cap " + capacity);
		output.println(" int time " + accessTime);
		output.println(" int watch " + (watched ? 1 : 0));
		output.println(" String file \"" + fileName + "\"");
		String str = initialValue.replace("\\","\\\\");
		str = str.replace("\"","\\\"");
		str = str.replace("\n","\\n");
		output.println(" String init \"" + str + "\"");
		output.println("END");
	} // end of save method

	/**
	 * Copy this element.
	 */
	public Element copy() {
		
		Memory it = new Memory(circuit);
		it.name = new String(name);
		it.type = type;
		it.bits = bits;
		it.accessTime = accessTime;
		it.initialValue = new String(initialValue);
		it.capacity = capacity;
		it.fileName = new String(fileName);
		it.specs = new String(specs);
		it.watched = watched;
		for (Input input : inputs) {
			it.inputs.add(input.copy(it));
		}
		it.outputs.add(outputs.get(0).copy(it));
		super.copy(it);
		return it;
	} // end of copy method
	
	/**
	 * Display info about this element.
	 * 
	 * @param info The JLabel to display with.
	 */
	public void showInfo(JLabel info) {
		
		if (fileName.equals("")) {
			info.setText(specs + ", built-in initializaion");
		}
		else {
			info.setText(specs + ", initialization file = \"" + fileName + "\"");
		}
	} // end of showInfo method
	
	/**
	 * Get the name of this memory.
	 * 
	 * @return the name.
	 */
	public String getName() {
		
		return name;
	} // end of getName method
	
	/**
	 * Get the number of bits in this memory.
	 * 
	 * @return the number of bits.
	 */ 
	public int getBits() {
		
		return bits;
	} // end of getBits method
	
	/**
	 * A memory can be watched.
	 * 
	 * @return true.
	 */
	public boolean canWatch() {
		
		return true;
	} // end of canWatch method
	
	/**
	 * See if this memory is watched.
	 * 
	 * @return true if it is, false if it is not.
	 */
	public boolean isWatched() {
		
		return watched;
	} // end of isWatched method
	
	/**
	 * Set whether this memory is watched or not.
	 * 
	 * @param state True to make it watched, false to make it not watched.
	 */
	public void setWatched(boolean state) {
		
		watched = state;
	} // end of setWatched method

	/**
	 * Memories have timing info (access time).
	 * 
	 * @return true.
	 */
	public boolean hasTiming() {
		
		return true;
	} // end of hasTiming method

	/**
	 * Reset access time to default value.
	 */
	public void resetPropDelay() {
		
		accessTime = defaultAccessTime;
	} // end of resetPropDelay method

	/**
	 * Get the access time in this element.
	 * 
	 * @return the current delay.
	 */
	public int getDelay() {
		
		return accessTime;
	} // end of getDelay method
	
	/**
	 * Set the access time in this element.
	 * 
	 * @param amount The new delay amount.
	 */
	public void setDelay(int temp) {
		
		accessTime = temp;
	} // end of setDelay method
	
	/**
	 * Set the name of the memory file.
	 * 
	 * @param memFile The memory file name.
	 */
	public void setMemFile(String memFile) {
		
		fileName = memFile;
		initialValue = "";
	} // end of setMemFile method

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
	 * Dialog box to create/modify register characteristics.
	 */
	private class MemoryEdit extends JDialog implements ActionListener {
		
		// properties
		private JButton ok = new JButton("OK");
		private JButton cancel = new JButton("Cancel");
		private JTextField nameField = new JTextField(name);
		private JTextField bitsField = new JTextField(bits+"",10);
		private JTextField capacityField = new JTextField(defaultCapacity+"",10);
		private KeyPad bitsPad = new KeyPad(bitsField,10,bits,this);
		private KeyPad capacityPad = new KeyPad(capacityField,10,defaultCapacity,this);
		private JRadioButton ram = new JRadioButton("RAM");
		private JRadioButton rom = new JRadioButton("ROM");
		private JButton fromFile = new JButton("from File");
		private JButton builtIn = new JButton("Built In");
		private boolean create;
		String tempInit = initialValue;
		
		/**
		 * Set up dialog window.
		 * 
		 * @param x The x-coordinate of the position of the dialog.
		 * @param y The y-coordinate of the position of the dialog.
		 * @param create True if creating a new memory element, false if changing one.
		 */
		private MemoryEdit(int x, int y, boolean create) {
			
			// set up window title
			super(JLSInfo.frame,create ? "Create Memory" : "Change Memory",true);
			
			// save create
			this.create = create;
			
			// set not cancelled
			cancelled = false;
			
			// set up window
			Container window = getContentPane();
			window.setLayout(new BoxLayout(window,BoxLayout.Y_AXIS));
			
			// set up types
			if (create) {
				JLabel tlbl = new JLabel("Type");
				tlbl.setAlignmentX(Component.CENTER_ALIGNMENT);
				window.add(tlbl);
				JPanel types = new JPanel(new GridLayout(1,2));
				types.add(ram);
				ram.setToolTipText("Random Access Memory");
				ram.setHorizontalAlignment(SwingConstants.CENTER);
				types.add(rom);
				rom.setToolTipText("Read Only Memory");
				rom.setHorizontalAlignment(SwingConstants.CENTER);
				ButtonGroup group = new ButtonGroup();
				group.add(ram);
				group.add(rom);
				window.add(types);
			}
			
			// set up inputs
			if (create) {
				window.add(new JLabel(" "));
				JPanel info = new JPanel(new BorderLayout());
				JPanel labels = new JPanel(new GridLayout(3,1,1,5));
				JLabel name = new JLabel("Name: ",SwingConstants.RIGHT);
				labels.add(name);
				JLabel bits = new JLabel("Bits/Word: ",SwingConstants.RIGHT);
				labels.add(bits);
				JLabel words = new JLabel("Capacity (words): ",SwingConstants.RIGHT);
				labels.add(words);
				info.add(labels,BorderLayout.WEST);
				
				JPanel fields = new JPanel(new GridLayout(3,1,1,5));
				fields.add(nameField);
				JPanel b = new JPanel(new BorderLayout());
				b.add(bitsField,BorderLayout.CENTER);
				b.add(bitsPad,BorderLayout.EAST);
				fields.add(b);
				
				JPanel i = new JPanel(new BorderLayout());
				i.add(capacityField,BorderLayout.CENTER);
				i.add(capacityPad,BorderLayout.EAST);
				capacityPad.setBase(10);
				fields.add(i);
				info.add(fields,BorderLayout.CENTER);
				window.add(info);
			}
			else {
				JPanel info = new JPanel(new BorderLayout());
				JLabel name = new JLabel("Name: ",SwingConstants.RIGHT);
				info.add(name,BorderLayout.WEST);
				info.add(nameField,BorderLayout.CENTER);
				window.add(info);
			}
			
			
			// set up initial values buttons
			window.add(new JLabel(" "));
			JLabel ilbl = new JLabel("Initial Memory Contents:");
			ilbl.setAlignmentX(Component.CENTER_ALIGNMENT);
			window.add(ilbl);
			JPanel inits = new JPanel(new GridLayout(1,2));
			inits.add(builtIn);
			builtIn.setToolTipText("open dialog to set initial values");
			inits.add(fromFile);
			fromFile.setToolTipText("open dialog to set file name");
			window.add(inits);
			
			// set up ok and cancel buttons
			window.add(new JLabel(" "));
			JPanel okCancel = new JPanel(new GridLayout(1,2));
			ok.setBackground(Color.green);
			okCancel.add(ok);
			cancel.setBackground(Color.pink);
			okCancel.add(cancel);
			JButton help = new JButton("Help");
			if (JLSInfo.hb == null)
				Util.noHelp(help);
			else
				JLSInfo.hb.enableHelpOnButton(help,"memory",null);
			okCancel.add(help);
			window.add(okCancel);
			getRootPane().setDefaultButton(ok);
			
			// set up listeners
			ok.addActionListener(this);
			nameField.addActionListener(this);
			bitsField.addActionListener(this);
			capacityField.addActionListener(this);
			cancel.addActionListener(this);
			ram.addActionListener(this);
			rom.addActionListener(this);
			fromFile.addActionListener(this);
			builtIn.addActionListener(this);
			
			// set up window close listener to cancel memory
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
		 * React to buttons.
		 * 
		 * @param event The event object for this action.
		 */
		public void actionPerformed(ActionEvent event) {
			
			if (event.getSource() == ok || event.getSource() == nameField ||
					event.getSource() == bitsField || event.getSource() == capacityField) {
				String tname = nameField.getText().trim();
				if (tname.equals("") || !Util.isValidName(tname)) {
					JOptionPane.showMessageDialog(this,
							"Missing or invalid name", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				int tbits = bits;
				int tcapacity = capacity;
				Memory.Type ttype = null;
				if (create) {
					try {
						tbits = Integer.parseInt(bitsField.getText());
						tcapacity = Integer.parseInt(capacityField.getText(),10);
					}
					catch (NumberFormatException ex) {
						JOptionPane.showMessageDialog(this,
								"Value not numeric", "Error",
								JOptionPane.ERROR_MESSAGE);
						return;
					}
					if (tbits < 1) {
						JOptionPane.showMessageDialog(this,
								"Must have at least 1 bit", "Error",
								JOptionPane.ERROR_MESSAGE);
						return;
					}
					if (ram.isSelected()) {
						ttype = Memory.Type.RAM;
					}
					else if (rom.isSelected()) {
						ttype = Memory.Type.ROM;
					}
					else {
						JOptionPane.showMessageDialog(this,
								"Pick RAM or ROM", "Error",
								JOptionPane.ERROR_MESSAGE);
						return;
					}
				}
				if (!tname.equals(name) && circuit.hasName(tname)) {
					JOptionPane.showMessageDialog(this,
							"Duplicate name", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}
				String msg = initOK(tempInit,tcapacity,tbits,false);
				if (msg != null) {
					JOptionPane.showMessageDialog(getParent(),
							msg + " in built in initialization",
							"Error", JOptionPane.ERROR_MESSAGE);
					return;
				}

				// everything is ok, so make it permanent
				if (!name.equals(""))
					circuit.removeName(name);
				circuit.addName(tname);
				name = tname;
				initialValue = tempInit;
				if (create) {
					type = ttype;
					bits = tbits;
					capacity = tcapacity;
					bitsPad.close();
					capacityPad.close();
				}
				circuit.markChanged();
				if (!create && !nameFits(tname)) {
					changed = true;
				}
				else {
					changed = false;
				}
				dispose();
			}
			else if (event.getSource() == cancel) {
				cancel();
			}
			else if (event.getSource() == fromFile) {
				
				// get file name
				String file  = JOptionPane.showInputDialog(this,
						"File name?", fileName);
				if (file == null)
					return;
				file = file.trim();
				while (!file.equals("") && !Util.isValidName(file)) {
					JOptionPane.showMessageDialog(this,
							"Missing or invalid file name, try again", "Error",
							JOptionPane.ERROR_MESSAGE);
					file = JOptionPane.showInputDialog(this,
							"File name?", fileName);
					if (file == null)
						return;
					file = file.trim();
				}
				fileName = file;
				initialValue = "";
			}
			else if (event.getSource() == builtIn) {
				
				// construct input dialog
				final JDialog init = new JDialog(this,true);
				Container window = init.getContentPane();
				window.setLayout(new BoxLayout(window,BoxLayout.Y_AXIS));
				final JTextArea area = new JTextArea(tempInit,10,12);
				JScrollPane pane = new JScrollPane(area);
				window.add(pane);
				
				Action okAction = new AbstractAction("ok") {
					public void actionPerformed(ActionEvent event) {
						tempInit = area.getText();
						String msg = initOK(tempInit,Integer.MAX_VALUE,Integer.MAX_VALUE,false);
						if (msg != null) {
							JOptionPane.showMessageDialog(getParent(),
									msg, "Error", JOptionPane.ERROR_MESSAGE);
							return;
						}
						fileName = "";
						init.dispose();
					}
				};
				Action cancelAction = new AbstractAction("cancel") {
					public void actionPerformed(ActionEvent event) {
						init.dispose();
					}
				};
				window.add(new JLabel(" "));
				JPanel okCancel = new JPanel(new GridLayout(1,2));
				okCancel.add(new JButton(okAction));
				okCancel.add(new JButton(cancelAction));
				window.add(okCancel);
				
				init.pack();
				Point loc = getLocation();
				init.setLocation(loc);
				init.setVisible(true);
			}
		} // end of actionPerformed method
		
		/**
		 * Cancel this element.
		 */
		private void cancel() {
			
			cancelled = true;
			dispose();
		} // end of cancel method
		
		/**
		 * See if the given name fits in the box on the screen.
		 * 
		 * @param value The value to check.
		 * 
		 * @return true if it fits, false if not.
		 */
		public boolean nameFits(String name) {
			
			FontMetrics fm = saveg.getFontMetrics();
			int w = fm.stringWidth(" " + name + " ");
			return w <= width;
			
		} // end of nameFits method
		
	} // end of MemoryCreate class
	
	/**
	 * Memories can be modified.
	 * 
	 * @return true.
	 */ 
	public boolean canChange() {
		
		return true;
	} // end of canChange method
	
	private Graphics saveg;
	
	/**
	 * Show change dialog.
	 * 
	 * @param g The Graphics object to use to determine size.
	 * @param editWindow The editor window this element is in.
	 * @param x The current x-coordinate of the mouse.
	 * @param y The current y-coordinate of the mouse.
	 * 
	 * @return true if the name is bigger, false if not.
	 */
	public boolean change(Graphics g, JPanel editWindow, int x, int y) {
		
		// save g for valueFits method
		saveg = g;
		
		// display dialog
		Point pos = editWindow.getMousePosition();
		Point win = editWindow.getLocationOnScreen();
		if (pos == null) {
			new MemoryEdit(x+win.x,y+win.y,false);
		}
		else {
			new MemoryEdit(pos.x+win.x,pos.y+win.y,false);
		}
		
		// if name too big, resize and detach
		if (changed) {
			detach();
			width = 0;
			height = 0;
			init(g);
			return true;
		}
		return false;
		
	} // end of change method
	
	/**
	 * Parse initial value text.
	 * 
	 * @param str The initial value text.
	 * @param maxAddr The maximum memory address.
	 * @param bitsPerWord The number of bits per word.
	 * @param storing True if values are to be stored, false if just checking.
	 * 
	 * @return null if syntax is ok, an error message if not.
	 */
	public String initOK(String str, int maxAddr, int bitsPerWord, boolean storing) {
		
		// set up scanner
		Scanner scan = new Scanner(str);
		boolean scanning = true;
		int lineNumber = 0;
		String nextLine = "";
		
		// read a line
		try {
			nextLine = scan.nextLine();
			lineNumber += 1;
		}
		catch (NoSuchElementException ex) {
			scanning = false;
		}
		
		// as long as there are lines...
		while (scanning) {
			
			// iognore blank  or comment lines
			String temp = nextLine.trim();
			if (!temp.equals("") && temp.charAt(0) != '#') {

				// check this line
				Scanner lscan = new Scanner (nextLine);
				lscan.useRadix(16);
				
				// get address
				int addr = 0;
				if (lscan.hasNextInt()) {
					addr = lscan.nextInt();
				}
				else {
					return "line " + lineNumber + ": missing or invalid address";
				}

				// check address
				if (addr < 0 || addr >= maxAddr) {
					return "line " + lineNumber + ": invalid address";
				}
				
				// get data value
				BigInteger data = BigInteger.ZERO;
				if (lscan.hasNextBigInteger()) {
					data = lscan.nextBigInteger();
				}
				else {
					return "line " + lineNumber + ": missing or invalid value";
				}
				
				// check value
				BitSet bval = BitSetUtils.Create(data);
				if (bval.length() > bitsPerWord) {
					return "line " + lineNumber + ": value has more bits than word size";
				}
				
				// put data in memory if storing
				if (storing) {
					initMem.put(addr,bval);
				}
			}
			
			// read next line
			try {
				nextLine = scan.nextLine();
				lineNumber += 1;
			}
			catch (NoSuchElementException ex) {
				scanning = false;
			}
		}
		return null;
	} // end of initOK method
	
	/**
	 * Print all memory locations that changed during simulation.
	 * 
	 * @param qual Qualified subcircuit name.
	 */
	public void printChangedValues(String qual) {
		
		// nothing changes in ROM's
		if (type == Type.ROM)
			return;
		
		// check all locations
		boolean firstTime = true;
		SortedSet<Integer> addrs = new TreeSet<Integer>();
		Set<Integer> temp = new HashSet<Integer>(mem.keySet());
		for (int addr : temp) {
			addrs.add(addr);
		}
		for (int addr : addrs) {
			
			// get initial value
			BitSet initial;
			if (initMem.get(addr) == null) {
				initial = new BitSet(1);
			}
			else {
				initial = initMem.get(addr);
			}
			
			// if no change, do nothing
			if (mem.get(addr).equals(initial))
				continue;
			
			// if first time printing, print a heading
			if (firstTime) {
				firstTime = false;
				if (qual.equals("")) {
					System.out.println("Changed locations in memory " + name);
				}
				else {
					System.out.println("Changed locations in memory " + qual + "." + name);
				}
				
			}
			
			// print address, old value and new value
			System.out.printf(" 0x%x: ", addr);
			String oldValue = BitSetUtils.toDisplay(initial,bits);
			String newValue = BitSetUtils.toDisplay(mem.get(addr),bits);
			System.out.println(oldValue + " -> " + newValue);
		}
		
		// if nothing changed, print message to that effect
		if (firstTime) {
			if (qual.equals("")) {
				System.out.println("No changes in memory " + name);
			}
			else {
				System.out.println("No changes in memory " + qual + "." + name);
			}
			
		}
	} // end of printChangedValues method
	
	/**
	 * Get the capacity of this memory element.
	 * 
	 * @return the capacity, in words.
	 */
	public int getCapacity() {
		
		return capacity;
	} // end of getCapacity method
	
//	-------------------------------------------------------------------------------
//	Simulation
//	-------------------------------------------------------------------------------
	
	private Map<Integer,BitSet> mem;
	private Map<Integer,BitSet> initMem;
	private BitSet currentValue;
	private class WriteRecord {
		BitSet what;
		int where;
		long when;
	};
	private java.util.List<WriteRecord> activity =
		new LinkedList<WriteRecord>();
	
	/**
	 * Initialize this element.
	 * 
	 * @param sim Unused.
	 */
	public void initSim(Simulator sim) {
		
		// create initial memory array
		initMem = new HashMap<Integer,BitSet>();
		
		// if there is an initialization file specified
		if (!fileName.equals("")) {
			
			// read and parse file
			try {
				File file = new File(fileName);
				int length = (int)file.length();
				FileInputStream in = new FileInputStream(file);
				InputStreamReader input = new InputStreamReader(in);
				char [] info = new char[length];
				input.read(info,0,length);
				input.close();
				String msg = initOK(new String(info),capacity,bits,true);
				if (msg != null) {
					if (JLSInfo.batch && JLSInfo.frame == null && !JLSInfo.isApplet) {
						System.out.println(msg + " in memory file " +
								name + ", all zeros assumed");
					}
					else {
						JOptionPane.showMessageDialog(JLSInfo.frame,
								msg + "in memory file " +
								name + ", all zeros assumed", "Error",
								JOptionPane.ERROR_MESSAGE);
					}
				}
				
			}
			catch (IOException ex) {
				
				if (JLSInfo.batch && JLSInfo.frame == null && !JLSInfo.isApplet) {
					System.out.println("Initialization file for memory " +
							name + " cannot be read, all zeros assumed");
				}
				else {
					JOptionPane.showMessageDialog(JLSInfo.frame,
							"Initialization file for memory " +
							name + " cannot be read, all zeros assumed", "Error",
							JOptionPane.ERROR_MESSAGE);
				}
			}
			
		}
		else {
			
			// parse built-in values
			String msg = initOK(new String(initialValue),capacity,bits,true);
			if (msg != null) {
				if (JLSInfo.batch && JLSInfo.frame == null && !JLSInfo.isApplet) {
					System.out.println(msg + " in memory file " +
							name + ", all zeros assumed");
				}
				else {
					JOptionPane.showMessageDialog(JLSInfo.frame,
							msg + " in memory file " +
							name + ", all zeros assumed", "Error",
							JOptionPane.ERROR_MESSAGE);
				}
			}
		}
		
		// copy initial memory values to running memory
		mem = new HashMap<Integer,BitSet>(initMem);
		
		// set output value to null
		getOutput("output").setValue(null);
		
		// set current value to null
		currentValue = null;
		
		// clear activity history
		activity.clear();
		
	} // end of initSim method
	
	private static enum MemoryAction {WRITE,READ,OFF};
	
	/**
	 * React to an event.
	 * 
	 * @param now The current simulation time.
	 * @param sim The simulator to post events to.
	 * @param todo If null, an input has changed, otherwise it is the value to output.
	 */
	public void react(long now, Simulator sim, Object todo) {
		
		// todo
		class MemAction {
			MemoryAction action;
			int addr;
			BitSet data;
		};
		
		// if an input has changed ...
		if (todo == null) {
			
			// get inputs
			BitSet csb = getInput("CS").getValue();
			if (csb == null)
				csb = new BitSet();
			boolean cs = csb.get(0);
			BitSet oeb = getInput("OE").getValue();
			if (oeb == null)
				oeb = new BitSet();
			boolean oe = oeb.get(0);
			boolean we = true;
			if (type == Type.RAM) {
				BitSet web = getInput("WE").getValue();
				if (web == null)
					web = new BitSet();
				we = web.get(0);
				
			}
			BitSet addr = getInput("address").getValue();
			if (addr == null)
				addr = new BitSet();
			
			// if RAM, chip select and write enable...
			if (type == Type.RAM && !cs && !we) {
				
				// do a write
				MemAction act = new MemAction();
				act.action = MemoryAction.WRITE;
				act.addr = BitSetUtils.ToInt(addr);
				BitSet data = (BitSet)(getInput("input").getValue());
				if (data == null)
					data = new BitSet();
				act.data = (BitSet)(data.clone());
				sim.post(new SimEvent(now+accessTime,this,act));
			}
			
			// if chip select and output enable ...
			if (!cs && !oe) {
				
				// do a read
				MemAction act = new MemAction();
				act.action = MemoryAction.READ;
				act.addr = BitSetUtils.ToInt(addr);
				sim.post(new SimEvent(now+accessTime,this,act));
			}
			else {
				
				// turn off tristate output
				MemAction act = new MemAction();
				act.action = MemoryAction.OFF;
				sim.post(new SimEvent(now+accessTime,this,act));
			}
		}
		else {
			
			// finish read or write
			MemAction act = (MemAction)todo;
			
			// if a write...
			if (act.action == MemoryAction.WRITE) {
				
				// get the value to write
				BitSet value = (BitSet)act.data.clone();
				
				// if address is not legal, don't write anything
				if (act.addr >= capacity)
					return;
				
				// save in activity history
				WriteRecord rec = new WriteRecord();
				rec.what = (BitSet)(act.data.clone());
				rec.where = act.addr;
				rec.when = now;
				activity.add(0,rec);
				
				// store in memory
				mem.put(act.addr, value);
			}
			
			// else if its a read...
			else if (act.action == MemoryAction.READ){
				
				// if invalid address, turn off tristate output
				if (act.addr >= capacity) {
					currentValue = null;
					getOutput("output").propagate(null,now,sim);
					return;
				}
				
				// get value from memory
				BitSet value;
				if (mem.get(act.addr) == null) {
					value =  new BitSet();
				}
				else {
					value = (BitSet)mem.get(act.addr).clone();
				}
				
				// send to output
				getOutput("output").propagate(value,now,sim);
				
				// set current value
				currentValue = (BitSet)value.clone();
			}
			
			// else it is to turn off the tristate output
			else {
				currentValue = null;
				getOutput("output").propagate(null,now,sim);
			}
		}
		
	} // end of react method
	
	/**
	 * Get a string representing the activity history of this element.
	 * 
	 * @return the history.
	 */
	public String getActivityTrace() {
		
		String result = "";
		for (WriteRecord rec : activity) {
			result += "0x" + BitSetUtils.ToString(rec.what,16) +
			" written to location 0x" +
			Integer.toString(rec.where,16) +
			" at time " + rec.when + "\n";
		}
		return result;
	} // end of getActivityTrace method
	
	/**
	 * The current value is the last value output.
	 * 
	 * @return the last value output.
	 */
	public BitSet getCurrentValue() {
		
		return currentValue;
	} // end of getCurrentValue method
	
	/**
	 * Get the current value of a given memory location.
	 * 
	 * @param loc The location.
	 * 
	 * @return the value at that location.
	 */
	public BitSet getCurrentValue(int loc){
		
	   return mem.get(loc);
	   } // end of getCurrentValue method
	
	/**
	 * Show memory contents.
	 * 
	 * @param where Where on the screen to display.
	 */
	public void showCurrentValue(Point where) {
		
		// set up
		final JDialog contents = new JDialog(JLSInfo.frame,true);
		Container window = contents.getContentPane();
		window.setLayout(new BorderLayout());
		
		// if simulator not run yet, make a dummy mem array
		if (mem == null) {
			mem = new HashMap<Integer,BitSet>();
		}
		
		// create addr/data display
		Set<Integer> unsorted = mem.keySet();
		SortedSet<Integer> addrs = new TreeSet<Integer>(unsorted);
		JPanel display = new JPanel(new GridLayout(addrs.size(),2,10,3));
		for (int addr : addrs) {
			
			// address info
			String hexAddr = Integer.toHexString(addr);
			String allAddr = "0x" + hexAddr + " (" + addr + "):";
			JLabel addrLabel = new JLabel(allAddr,SwingConstants.RIGHT);
			addrLabel.setOpaque(true);
			addrLabel.setBackground(Color.WHITE);
			display.add(addrLabel);
			
			// data info
			BitSet temp = new BitSet(1);
			if (mem.get(addr) != null) {
				temp = mem.get(addr);
			}
			String hex = BitSetUtils.ToString(temp,16);
			String unsigned = BitSetUtils.ToString(temp,10);
			String signed = BitSetUtils.ToStringSigned(temp,bits);
			String value = "0x" + hex + " (" + unsigned + " unsigned, " + signed + " signed)";
			JLabel dataLabel = new JLabel(value);
			dataLabel.setOpaque(true);
			dataLabel.setBackground(Color.WHITE);
			display.add(dataLabel);
		}
		JScrollPane pane = new JScrollPane(display);
		int width = (int)(display.getPreferredSize().getWidth());
		int height = (int)(display.getPreferredSize().getHeight());
		pane.setPreferredSize(new Dimension(width+50,Math.min(height,400)));
		window.add(pane,BorderLayout.CENTER);
		JButton ok = new JButton("ok");
		ok.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				contents.dispose();
			}
		});
		window.add(ok,BorderLayout.SOUTH);
		window.add(new JLabel("Non-Zero Words:",SwingConstants.CENTER),BorderLayout.NORTH);
		
		contents.pack();
		contents.setLocation(10,10);
		contents.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		contents.setVisible(true);
		
	} // end of showCurrentValue method
	
} // end of Memory class
