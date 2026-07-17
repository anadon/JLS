package jls.elem;

import java.nio.charset.StandardCharsets;

import jls.*;
import jls.sim.*;
import jls.util.Placement;
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
	
	// one constraint string, two surfaces: dialog and loader (issue #52)
	static final String CAPACITY_CONSTRAINT = "Capacity must be at least 1";
	static final String BITS_CONSTRAINT = "Must have at least 1 bit";

	/**
	 * The capacity rule, shared by the dialog and the loader: an invalid
	 * capacity crashes DenseWordStore at simulation start (M12).
	 *
	 * @param capacity The proposed capacity in words.
	 *
	 * @return the violated constraint message, or null if valid.
	 */
	static String checkCapacity(int capacity) {

		return capacity < 1 ? CAPACITY_CONSTRAINT : null;
	} // end of checkCapacity method

	/**
	 * The word-size rule, shared by the dialog and the loader.
	 *
	 * @param bits The proposed bits per word.
	 *
	 * @return the violated constraint message, or null if valid.
	 */
	static String checkBits(int bits) {

		return bits < 1 ? BITS_CONSTRAINT : null;
	} // end of checkBits method

	// absolute cap on RLE-decoded initial-memory words (issue #38)
	static final long MAX_INIT_WORDS = 1L << 24;

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
	 * @param circ The circuit this element is part of.
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
	@Override
	public boolean setup(Graphics g, JPanel editWindow, int x, int y) {
		
		// show creation dialog
		new MemoryEdit(true);
		
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
	 * Initialize internal info for this element.
	 * Figures out height and width using font info from graphics object.
	 * Note input positions: RAM-> 0 - addr, 1 - input, 2 - WE, 3 - OE, 4 - CS
	 *                       ROM-> 0 - addr, 1 - OE, 2 - CS
	 * 
	 * @param g The Graphics object to use.
	 */
	@Override
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
	@Override
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
	@Override
	public void setValue(String name, int value) {

		if (name.equals("bits")) {
			String violatedBits = checkBits(value);
			if (violatedBits != null) {
				throw new IllegalArgumentException(violatedBits);
			}
			bits = value;
		} else if (name.equals("cap")) {
			// an invalid capacity crashes DenseWordStore at simulation
			// start; reject it at load like the dialog does (issue #52)
			String violated = checkCapacity(value);
			if (violated != null) {
				throw new IllegalArgumentException(violated);
			}
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
	@Override
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
		} else if (name.equals("initrle")) {
			// the writer emits cap before initrle, so the declared
			// capacity bounds the expansion here (issue #38)
			initialValue = decodeInitRLE(value,
					Math.min(capacity, MAX_INIT_WORDS));
		} else {
			super.setValue(name,value);
		}
	} // end of setValue method
	
	/**
	 * Save this element.
	 * 
	 * @param output The output writer.
	 */
	@Override
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

		// large memory images are usually plain "addr value" dumps with
		// long runs of identical words; save those run-length encoded
		// (#21). Text with comments or hand formatting keeps the raw
		// encoding so it round-trips exactly. The loader accepts both.
		String rle = encodeInitRLE(initialValue);
		if (rle != null && rle.length() < initialValue.length()) {
			output.println(" String initrle \"" + rle + "\"");
		} else {
			String str = initialValue.replace("\\","\\\\");
			str = str.replace("\"","\\\"");
			str = str.replace("\n","\\n");
			output.println(" String init \"" + str + "\"");
		}
		output.println("END");
	} // end of save method

	/**
	 * Encode initial-memory text as run-length-encoded tokens, or return
	 * null when the text is not a canonical dump (comments, extra tokens,
	 * unusual formatting, duplicate addresses) and must be saved raw.
	 *
	 * Canonical dump: one "addr value" pair per line, lowercase hex, no
	 * leading zeros, ascending addresses. Encoding: space-separated
	 * "addr:value" tokens, with ":count" appended for a run of count
	 * consecutive addresses holding the same value.
	 *
	 * @param text The initial-memory text.
	 *
	 * @return the encoded form, or null to use the raw encoding.
	 */
	static String encodeInitRLE(String text) {

		if (text.isEmpty()) {
			return null;
		}

		// parse strictly: exactly "addr value" per line
		java.util.List<Integer> addrs = new java.util.ArrayList<Integer>();
		java.util.List<BigInteger> values = new java.util.ArrayList<BigInteger>();
		for (String line : text.split("\n", -1)) {
			if (line.isEmpty()) {
				continue;
			}
			String[] tokens = line.split(" ");
			if (tokens.length != 2) {
				return null;
			}
			int addr;
			BigInteger value;
			try {
				addr = Integer.parseInt(tokens[0], 16);
				value = new BigInteger(tokens[1], 16);
			} catch (NumberFormatException ex) {
				return null;
			}
			if (addr < 0) {
				return null;
			}
			addrs.add(addr);
			values.add(value);
		}
		if (addrs.isEmpty()) {
			return null;
		}

		// must reproduce the text exactly (modulo a missing final newline)
		StringBuilder canonical = new StringBuilder();
		for (int i = 0; i < addrs.size(); i += 1) {
			canonical.append(Integer.toHexString(addrs.get(i))).append(' ')
					.append(values.get(i).toString(16)).append('\n');
		}
		String canon = canonical.toString();
		if (!text.equals(canon) && !canon.equals(text + "\n")) {
			return null;
		}

		// ascending addresses, no duplicates (otherwise decode would
		// reorder relative to the original last-value-wins semantics)
		for (int i = 1; i < addrs.size(); i += 1) {
			if (addrs.get(i) <= addrs.get(i - 1)) {
				return null;
			}
		}

		// run-length encode consecutive addresses with identical values
		StringBuilder out = new StringBuilder();
		int i = 0;
		while (i < addrs.size()) {
			int run = 1;
			while (i + run < addrs.size()
					&& addrs.get(i + run) == addrs.get(i) + run
					&& values.get(i + run).equals(values.get(i))) {
				run += 1;
			}
			if (out.length() > 0) {
				out.append(' ');
			}
			out.append(Integer.toHexString(addrs.get(i))).append(':')
					.append(values.get(i).toString(16));
			if (run > 1) {
				out.append(':').append(Integer.toHexString(run));
			}
			i += run;
		}
		return out.toString();
	} // end of encodeInitRLE method

	/**
	 * Decode a run-length-encoded initial-memory attribute back to the
	 * canonical "addr value" text the dialog and simulator use.
	 *
	 * @param rle The encoded form.
	 *
	 * @return the canonical text.
	 *
	 * @throws IllegalArgumentException if the encoding is malformed (the
	 *             loader reports a load error).
	 */
	static String decodeInitRLE(String rle) {

		return decodeInitRLE(rle, MAX_INIT_WORDS);
	} // end of decodeInitRLE method

	/**
	 * Decode with an explicit address bound. A hostile run length used to
	 * expand ~2^31 lines into a StringBuilder (issue #38); every decoded
	 * address must be below the declared memory capacity.
	 *
	 * @param rle The encoded form.
	 * @param maxWords Upper bound on decoded addresses (exclusive).
	 *
	 * @return the canonical text.
	 */
	static String decodeInitRLE(String rle, long maxWords) {

		StringBuilder text = new StringBuilder();
		for (String token : rle.trim().split(" ")) {
			if (token.isEmpty()) {
				continue;
			}
			String[] parts = token.split(":");
			if (parts.length != 2 && parts.length != 3) {
				throw new IllegalArgumentException("bad initrle token: " + token);
			}
			int addr;
			BigInteger value;
			int run;
			try {
				addr = Integer.parseInt(parts[0], 16);
				value = new BigInteger(parts[1], 16);
				run = parts.length == 3 ? Integer.parseInt(parts[2], 16) : 1;
			} catch (NumberFormatException ex) {
				throw new IllegalArgumentException("bad initrle token: " + token);
			}
			if (addr < 0 || run < 1 || (long) addr + run > maxWords) {
				throw new IllegalArgumentException("bad initrle token: " + token
						+ " (addresses must stay below the memory capacity)");
			}
			String valueHex = value.toString(16);
			for (int i = 0; i < run; i += 1) {
				text.append(Integer.toHexString(addr + i)).append(' ')
						.append(valueHex).append('\n');
			}
		}
		return text.toString();
	} // end of decodeInitRLE method

	/**
	 * Copy this element.
	 */
	@Override
	public Element copy() {
		
		Memory it = new Memory(circuit);
		it.name = name;
		it.type = type;
		it.bits = bits;
		it.accessTime = accessTime;
		it.initialValue = initialValue;
		it.capacity = capacity;
		it.fileName = fileName;
		it.specs = specs;
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
	@Override
	public void showInfo(JLabel info) {
		
		if (fileName.isEmpty()) {
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
	@Override
	public String getName() {
		
		return name;
	} // end of getName method
	
	/**
	 * Get the number of bits in this memory.
	 * 
	 * @return the number of bits.
	 */ 
	@Override
	public int getBits() {
		
		return bits;
	} // end of getBits method
	
	/**
	 * A memory can be watched.
	 * 
	 * @return true.
	 */
	@Override
	public boolean canWatch() {
		
		return true;
	} // end of canWatch method
	
	/**
	 * See if this memory is watched.
	 * 
	 * @return true if it is, false if it is not.
	 */
	@Override
	public boolean isWatched() {
		
		return watched;
	} // end of isWatched method
	
	/**
	 * Set whether this memory is watched or not.
	 * 
	 * @param state True to make it watched, false to make it not watched.
	 */
	@Override
	public void setWatched(boolean state) {
		
		watched = state;
	} // end of setWatched method

	/**
	 * Memories have timing info (access time).
	 * 
	 * @return true.
	 */
	@Override
	public boolean hasTiming() {
		
		return true;
	} // end of hasTiming method

	/**
	 * Reset access time to default value.
	 */
	@Override
	public void resetPropDelay() {
		
		accessTime = defaultAccessTime;
	} // end of resetPropDelay method

	/**
	 * Get the access time in this element.
	 * 
	 * @return the current delay.
	 */
	@Override
	public int getDelay() {
		
		return accessTime;
	} // end of getDelay method
	
	/**
	 * Set the access time in this element.
	 * 
	 * @param temp The new delay amount.
	 */
	@Override
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
	@Override
	public void remove(Circuit circ) {
		
		circ.removeName(name);
		super.remove(circ);
	} // end of remove method
	
	/**
	 * Dialog box to create/modify register characteristics.
	 */
	private class MemoryEdit extends ElementDialog implements ActionListener {
		
		// properties
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
		 * @param create True if creating a new memory element, false if changing one.
		 */
		private MemoryEdit(boolean create) {
			
			// set up window title
			super(create ? "Create Memory" : "Change Memory","memory");
			
			// save create
			this.create = create;
			
			// set not cancelled
			cancelled = false;
			
			// set up window
			Container window = getContentPane();
			
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
			
			// set up listeners
			confirmOnEnter(nameField);
			confirmOnEnter(bitsField);
			confirmOnEnter(capacityField);
			fromFile.addActionListener(this);
			builtIn.addActionListener(this);
			
			finishDialog();
		} // end of constructor
		
		/**
		 * Check the form against the shared memory constraints (issue
		 * #52): a rejected dialog must leave the memory unchanged.
		 */
		@Override
		protected java.util.List<Violation> validateInputs() {

			java.util.List<Violation> violations =
					new java.util.ArrayList<Violation>();
			String tname = nameField.getText().trim();
			if (tname.isEmpty() || !Util.isValidName(tname)) {
				violations.add(new Violation("Missing or invalid name",
						nameField));
			}
			else if (!tname.equals(name) && circuit.hasName(tname)) {
				violations.add(new Violation("Duplicate name", nameField));
			}
			int tbits = bits;
			int tcapacity = capacity;
			if (create) {
				try {
					tbits = Integer.parseInt(bitsField.getText());
					String violated = checkBits(tbits);
					if (violated != null) {
						violations.add(new Violation(violated, bitsField));
					}
				}
				catch (NumberFormatException ex) {
					violations.add(new Violation("Value not numeric",
							bitsField));
				}
				try {
					tcapacity = Integer.parseInt(capacityField.getText(),10);
					String violated = checkCapacity(tcapacity);
					if (violated != null) {
						violations.add(new Violation(violated, capacityField));
					}
				}
				catch (NumberFormatException ex) {
					violations.add(new Violation("Value not numeric",
							capacityField));
				}
				if (!ram.isSelected() && !rom.isSelected()) {
					violations.add(new Violation("Pick RAM or ROM", ram));
				}
			}
			if (violations.isEmpty()) {
				String msg = initOK(tempInit,tcapacity,tbits,false);
				if (msg != null) {
					violations.add(new Violation(
							msg + " in built in initialization", null));
				}
			}
			return violations;
		} // end of validateInputs method

		/**
		 * Apply the validated form to the memory element.
		 */
		@Override
		protected void validateAndAccept() {

			String tname = nameField.getText().trim();
			int tbits = bits;
			int tcapacity = capacity;
			Memory.Type ttype = null;
			if (create) {
				try {
					tbits = Integer.parseInt(bitsField.getText());
				}
				catch (NumberFormatException ex) {
					reject("Value not numeric", bitsField);
					return;
				}
				try {
					tcapacity = Integer.parseInt(capacityField.getText(),10);
				}
				catch (NumberFormatException ex) {
					reject("Value not numeric", capacityField);
					return;
				}
				ttype = ram.isSelected() ? Memory.Type.RAM : Memory.Type.ROM;
			}

			// everything is ok, so make it permanent
			if (!name.isEmpty())
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
		} // end of validateAndAccept method
		
		/**
		 * React to the initial-contents buttons.
		 * 
		 * @param event The event object for this action.
		 */
		@Override
		public void actionPerformed(ActionEvent event) {
			
			if (event.getSource() == fromFile) {
				
				// get file name
				String file  = TellUser.prompt(this,
						"File name?", fileName);
				if (file == null)
					return;
				file = file.trim();
				while (!file.isEmpty() && !Util.isValidName(file)) {
					TellUser.error(this,
							"Missing or invalid file name, try again", "Error");
					file = TellUser.prompt(this,
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
					@Override
					public void actionPerformed(ActionEvent event) {
						tempInit = area.getText();
						String msg = initOK(tempInit,Integer.MAX_VALUE,Integer.MAX_VALUE,false);
						if (msg != null) {
							TellUser.error(getParent(),
									msg, "Error");
							return;
						}
						fileName = "";
						init.dispose();
					}
				};
				Action cancelAction = new AbstractAction("cancel") {
					@Override
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
		@Override
		protected void cancelDialog() {
			
			cancelled = true;
			dispose();
		} // end of cancelDialog method
		
		/**
		 * See if the given name fits in the box on the screen.
		 * 
		 * @param name The value to check.
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
	@Override
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
	@Override
	public boolean change(Graphics g, JPanel editWindow, int x, int y) {
		
		// save g for valueFits method
		saveg = g;
		
		// display dialog
		new MemoryEdit(false);
		
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
			if (!temp.isEmpty() && temp.charAt(0) != '#') {

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
		for (int addr : mem.addresses()) {
			
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
				if (qual.isEmpty()) {
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
			if (qual.isEmpty()) {
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
	
	private WordStore mem;
	private WordStore initMem;
	private BitSet currentValue;
	private class WriteRecord {
		BitSet what;
		int where;
		long when;
	};

	// The write history exists to feed the activity dialog; letting it
	// grow with every write made long simulations leak (#20), so it is
	// bounded to the newest records.
	private static final int ACTIVITY_LIMIT = 10_000;
	private LinkedList<WriteRecord> activity =
		new LinkedList<WriteRecord>();

	/**
	 * Storage for simulated memory words (#20). Only addresses that have
	 * been explicitly initialized or written count as present; get()
	 * returns null for the rest, exactly like the Map storage this
	 * replaces (the contents dialog and printChangedValues list only
	 * present addresses).
	 */
	private interface WordStore {

		/** The stored word, or null if this address was never set. */
		BitSet get(int addr);

		/** Store a word at an address. */
		void put(int addr, BitSet value);

		/** Addresses that have been set, in ascending order. */
		SortedSet<Integer> addresses();

		/** An independent copy (the running memory starts as a copy of the initial image). */
		WordStore copy();
	}

	/**
	 * Dense storage: one long per word plus one presence bit, about 8
	 * bytes per word. The Map storage it replaces cost ~100 bytes per
	 * word (entry + boxed Integer + BitSet with internal long[]).
	 */
	private static final class DenseWordStore implements WordStore {

		private final long[] words;
		private final BitSet present;

		DenseWordStore(int capacity) {
			words = new long[capacity];
			present = new BitSet(capacity);
		}

		private DenseWordStore(DenseWordStore from) {
			words = from.words.clone();
			present = (BitSet)from.present.clone();
		}

		@Override
		public BitSet get(int addr) {
			if (!present.get(addr))
				return null;
			return BitSet.valueOf(new long[] { words[addr] });
		}

		@Override
		public void put(int addr, BitSet value) {
			long[] asLongs = value.toLongArray();
			words[addr] = asLongs.length == 0 ? 0 : asLongs[0];
			present.set(addr);
		}

		@Override
		public SortedSet<Integer> addresses() {
			SortedSet<Integer> addrs = new TreeSet<Integer>();
			for (int a = present.nextSetBit(0); a >= 0; a = present.nextSetBit(a+1)) {
				addrs.add(a);
			}
			return addrs;
		}

		@Override
		public WordStore copy() {
			return new DenseWordStore(this);
		}
	}

	/**
	 * Map fallback for words wider than 64 bits and for huge memories,
	 * which are typically sparse (dense storage would eagerly allocate
	 * the full capacity).
	 */
	private static final class SparseWordStore implements WordStore {

		private final Map<Integer,BitSet> map;

		SparseWordStore() {
			map = new HashMap<Integer,BitSet>();
		}

		private SparseWordStore(SparseWordStore from) {
			map = new HashMap<Integer,BitSet>(from.map);
		}

		@Override
		public BitSet get(int addr) {
			return map.get(addr);
		}

		@Override
		public void put(int addr, BitSet value) {
			map.put(addr, value);
		}

		@Override
		public SortedSet<Integer> addresses() {
			return new TreeSet<Integer>(map.keySet());
		}

		@Override
		public WordStore copy() {
			return new SparseWordStore(this);
		}
	}

	// dense storage allocates the full capacity eagerly; past this many
	// words (32 MB of longs) assume sparse use and fall back to the map
	private static final int DENSE_CAPACITY_LIMIT = 1 << 22;

	private WordStore newWordStore() {

		if (bits <= 64 && capacity <= DENSE_CAPACITY_LIMIT)
			return new DenseWordStore(capacity);
		return new SparseWordStore();
	}
	
	/**
	 * Initialize this element.
	 * 
	 * @param sim Unused.
	 */
	@Override
	public void initSim(Simulator sim) {
		
		// create initial memory array
		initMem = newWordStore();
		
		// if there is an initialization file specified
		if (!fileName.isEmpty()) {
			
			// read and parse file
			try {
				File file = new File(fileName);
				int length = (int)file.length();
				FileInputStream in = new FileInputStream(file);
				InputStreamReader input = new InputStreamReader(in, StandardCharsets.UTF_8);
				char [] info = new char[length];
				input.read(info,0,length);
				input.close();
				String msg = initOK(new String(info),capacity,bits,true);
				if (msg != null) {
					if (JLSInfo.batch && JLSInfo.frame == null) {
						System.out.println(msg + " in memory file " +
								name + ", all zeros assumed");
					}
					else {
						TellUser.error(JLSInfo.frame,
								msg + "in memory file " +
								name + ", all zeros assumed", "Error");
					}
				}
				
			}
			catch (IOException ex) {
				
				if (JLSInfo.batch && JLSInfo.frame == null) {
					System.out.println("Initialization file for memory " +
							name + " cannot be read, all zeros assumed");
				}
				else {
					TellUser.error(JLSInfo.frame,
							"Initialization file for memory " +
							name + " cannot be read, all zeros assumed", "Error");
				}
			}
			
		}
		else {
			
			// parse built-in values
			String msg = initOK(initialValue,capacity,bits,true);
			if (msg != null) {
				if (JLSInfo.batch && JLSInfo.frame == null) {
					System.out.println(msg + " in memory file " +
							name + ", all zeros assumed");
				}
				else {
					TellUser.error(JLSInfo.frame,
							msg + " in memory file " +
							name + ", all zeros assumed", "Error");
				}
			}
		}
		
		// copy initial memory values to running memory
		mem = initMem.copy();
		
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
	@Override
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
				
				// save in activity history (newest first, bounded)
				WriteRecord rec = new WriteRecord();
				rec.what = (BitSet)(act.data.clone());
				rec.where = act.addr;
				rec.when = now;
				activity.addFirst(rec);
				if (activity.size() > ACTIVITY_LIMIT)
					activity.removeLast();
				
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
	@Override
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
	@Override
	public void showCurrentValue(Point where) {
		
		// set up
		final JDialog contents = new JDialog(JLSInfo.frame,true);
		Container window = contents.getContentPane();
		window.setLayout(new BorderLayout());
		
		// if simulator not run yet, make a dummy mem array
		if (mem == null) {
			mem = newWordStore();
		}

		// create addr/data display
		SortedSet<Integer> addrs = mem.addresses();
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
			@Override
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
