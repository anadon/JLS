package jls.elem;

import java.nio.charset.StandardCharsets;

import jls.core.Geometry;
import jls.*;
import jls.sim.*;
import java.io.*;
import java.math.*;
import java.util.*;

/**
 * Memory element, initialized internally or from a file.
 * Implements either ROM or SRAM (no DRAM).
 * 
 * @author David A. Poplawski
 */
public final class Memory extends LogicElement
		implements Timed, Watchable {
	
	// types
	/**
	 * The two kinds of memory this element can be configured as: RAM
	 * (readable and writable) or ROM (read-only).
	 */
	private static enum Type {
		/** Readable and writable memory. */
		RAM,
		/** Read-only memory. */
		ROM};
	
	// default values
	/** The default element name (empty until the user supplies one). */
	private static final String defaultName = "";
	/** The default memory type. */
	private static final Type defaultType = Type.RAM;
	/** The default number of bits per word. */
	private static final int defaultBits = 1;
	/** The default capacity, in words. */
	private static final int defaultCapacity = 2;
	/** The default initialization file name (empty means no file). */
	private static final String defaultFileName = "";
	/** The default access time, in simulation time units. */
	private static final int defaultAccessTime = 100; 
	/** The default built-in initial value text (empty means all zeros). */
	private static final String defaultInitialValue = "";
	
	// one constraint string, two surfaces: dialog and loader (issue #52)
	/** Message reported when a proposed capacity is less than 1 word. */
	static final String CAPACITY_CONSTRAINT = "Capacity must be at least 1";
	/** Message reported when a proposed word size is less than 1 bit. */
	static final String BITS_CONSTRAINT = "Must have at least 1 bit";

	/**
	 * The capacity rule, shared by the dialog and the loader: an invalid
	 * capacity crashes DenseWordStore at simulation start (M12).
	 *
	 * @param capacity The proposed capacity in words.
	 *
	 * @return the violated constraint message, or null if valid.
	 *
	 * @jls.testedby jls.elem.DialogValidationTest#memoryCapacityRuleIsOneStringOnTwoSurfaces()
	 */
	public static String checkCapacity(int capacity) {

		return capacity < 1 ? CAPACITY_CONSTRAINT : null;
	} // end of checkCapacity method

	/**
	 * The word-size rule, shared by the dialog and the loader.
	 *
	 * @param bits The proposed bits per word.
	 *
	 * @return the violated constraint message, or null if valid.
	 *
	 * @jls.testedby jls.elem.DialogValidationTest#memoryBitsRuleIsOneStringOnTwoSurfaces()
	 */
	public static String checkBits(int bits) {

		return bits < 1 ? BITS_CONSTRAINT : null;
	} // end of checkBits method

	// absolute cap on RLE-decoded initial-memory words (issue #38)
	/** The most words an RLE-encoded initialization is allowed to expand to. */
	static final long MAX_INIT_WORDS = 1L << 24;

	// saved properties
	/** The name of this memory element. */
	private String name = defaultName;
	/** Whether this memory is RAM or ROM. */
	private Type type = defaultType;
	/** The number of bits per word. */
	private int bits = defaultBits;
	/** The capacity of this memory, in words. */
	private int capacity = defaultCapacity;
	/** The initialization file name, or empty if initialized internally. */
	private String fileName = defaultFileName;
	/** The access time (read/write delay), in simulation time units. */
	private int accessTime = defaultAccessTime;
	/** The built-in initial value text, or empty if none. */
	private String initialValue = defaultInitialValue;
	/** True if this memory is being watched during simulation. */
	private boolean watched = false;

	// running properties
	/** The "capacity x bits" type description drawn inside the element. */
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
	 * Initialize internal info for this element.
	 * Figures out height and width using font info from graphics object.
	 * Note input positions: RAM-> 0 - addr, 1 - input, 2 - WE, 3 - OE, 4 - CS
	 *                       ROM-> 0 - addr, 1 - OE, 2 - CS
	 *
	 * @param g The Graphics object to use.
	 */
	@Override
	public void init(jls.core.TextMetrics g) {

		// set up
		if (type == Type.RAM) {
			specs = " " + capacity + "x" + bits + " RAM ";
		}
		else {
			specs = " " + capacity + "x" + bits + " ROM ";
		}

		// set up size if there is a graphics object
		int s = Geometry.SPACING;
		if (g != null) {

			// set up size if it doesn't already have one
			if (width == 0 && height == 0) {
				jls.core.TextMetrics fm = g;
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
	 * The "capacity x bits" type description drawn inside the box (issue
	 * #77: read by the GUI-side renderer).
	 *
	 * @return the spec string, as computed by {@link #init}.
	 */
	public String getSpecs() {

		return specs;
	} // end of getSpecs method

	/**
	 * Whether this memory is RAM (issue #77: read by the GUI-side renderer
	 * to lay out the data/WE ports).
	 *
	 * @return true if RAM, false if ROM.
	 */
	public boolean isRAM() {

		return type == Type.RAM;
	} // end of isRAM method

	/**
	 * The built-in initial value text (issue #77: read by the GUI-side
	 * dialog).
	 *
	 * @return the initial value text, or empty if none.
	 */
	public String getInitialValue() {

		return initialValue;
	} // end of getInitialValue method

	/**
	 * The initialization file name (issue #77: read by the GUI-side
	 * dialog).
	 *
	 * @return the file name, or empty if initialized internally.
	 */
	public String getFileName() {

		return fileName;
	} // end of getFileName method

	/**
	 * Set the memory name (issue #77: applied by the GUI-side dialog; the
	 * dialog owns the circuit name-table bookkeeping).
	 *
	 * @param name The new name.
	 */
	public void setName(String name) {

		this.name = name;
	} // end of setName method

	/**
	 * Set the built-in initial value text (issue #77: applied by the
	 * GUI-side dialog).
	 *
	 * @param initialValue The new initial value text.
	 */
	public void setInitialValue(String initialValue) {

		this.initialValue = initialValue;
	} // end of setInitialValue method

	/**
	 * Set the initialization file name (issue #77: applied by the GUI-side
	 * dialog).
	 *
	 * @param fileName The new file name.
	 */
	public void setFileName(String fileName) {

		this.fileName = fileName;
	} // end of setFileName method

	/**
	 * Set the memory type (issue #77: applied by the GUI-side dialog).
	 *
	 * @param ram True for RAM, false for ROM.
	 */
	public void setType(boolean ram) {

		this.type = ram ? Type.RAM : Type.ROM;
	} // end of setType method

	/**
	 * Set the number of bits per word (issue #77: applied by the GUI-side
	 * dialog).
	 *
	 * @param bits The new number of bits.
	 */
	public void setBits(int bits) {

		this.bits = bits;
	} // end of setBits method

	/**
	 * Set the capacity, in words (issue #77: applied by the GUI-side
	 * dialog).
	 *
	 * @param capacity The new capacity.
	 */
	public void setCapacity(int capacity) {

		this.capacity = capacity;
	} // end of setCapacity method

	/**
	 * Detach and re-size after a change that grew the name (issue #77:
	 * driven by the GUI-side dialog, replacing the tail of the former
	 * {@code change}).
	 *
	 * @param g The Graphics object to use to recompute size.
	 */
	public void reinit(jls.core.TextMetrics g) {

		detach();
		width = 0;
		height = 0;
		init(g);
	} // end of reinit method

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
		String rle = encodeInitRLE(initialValue,
				Math.min(capacity, MAX_INIT_WORDS));
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
	 *
	 * @jls.testedby jls.elem.MemoryInitEncodingTest#bigValuesSurvive()
	 * @jls.testedby jls.elem.MemoryInitEncodingTest#encodesRunsCompactly()
	 * @jls.testedby jls.elem.MemoryInitEncodingTest#refusesNonCanonicalText()
	 * @jls.testedby jls.elem.MemoryInitEncodingTest#toleratesMissingFinalNewline()
	 */
	static String encodeInitRLE(String text) {

		return encodeInitRLE(text, MAX_INIT_WORDS);
	} // end of encodeInitRLE method

	/**
	 * Encode with an explicit address bound. Addresses at or past the
	 * bound keep the raw encoding: the raw form is loaded leniently
	 * (out-of-range init falls back to zeros with a warning at
	 * simulation start), but the RLE decoder rejects such addresses at
	 * load, so encoding them would save a file the loader refuses to
	 * read back - a save/load fixed-point violation found by the
	 * generative fuzzer (issue #160).
	 *
	 * @param text The initial-memory text.
	 * @param maxWords Upper bound on encoded addresses (exclusive).
	 *
	 * @return the encoded form, or null to use the raw encoding.
	 *
	 * @jls.testedby jls.elem.MemoryInitEncodingTest#outOfCapacityAddressesStayRaw()
	 */
	static String encodeInitRLE(String text, long maxWords) {

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
			if (addr < 0 || addr >= maxWords) {
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
	 *
	 * @jls.testedby jls.elem.MemoryInitEncodingTest#bigValuesSurvive()
	 * @jls.testedby jls.elem.MemoryInitEncodingTest#decodeRejectsGarbage()
	 * @jls.testedby jls.elem.MemoryInitEncodingTest#decodeRejectsHostileRuns()
	 * @jls.testedby jls.elem.MemoryInitEncodingTest#encodesRunsCompactly()
	 * @jls.testedby jls.elem.MemoryInitEncodingTest#toleratesMissingFinalNewline()
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
	 *
	 * @jls.testedby jls.elem.MemoryInitEncodingTest#decodeRejectsHostileRuns()
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
	public String infoText() {
		
		if (fileName.isEmpty()) {
			return specs + ", built-in initializaion";
		}
		else {
			return specs + ", initialization file = \"" + fileName + "\"";
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
	 * Memory's timing value is an access time, not a propagation delay
	 * (issue #78): the timing dialog labels it accordingly.
	 *
	 * @return {@code true} - memory is the one element that uses access
	 *         time.
	 */
	@Override
	public boolean usesAccessTime() {

		return true;
	} // end of usesAccessTime method

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
	 * Memories can be modified.
	 * 
	 * @return true.
	 */ 
	@Override
	public boolean canChange() {
		
		return true;
	} // end of canChange method
	
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
	
	/** The running memory contents during simulation. */
	private WordStore mem;
	/** The initial memory image, copied to the running memory at simulation start. */
	private WordStore initMem;
	/** The value currently driven on the output, or null if none. */
	private BitSet currentValue;
	/**
	 * One entry in the write history: the value written (what), the address
	 * written to (where), and the simulation time of the write (when). Used
	 * to populate the activity dialog.
	 */
	private static class WriteRecord {
		/** The value written. */
		BitSet what;
		/** The address written to. */
		int where;
		/** The simulation time of the write. */
		long when;

		/**
		 * Create an empty record; the caller fills in the fields.
		 */
		private WriteRecord() {
		}
	};

	// The write history exists to feed the activity dialog; letting it
	// grow with every write made long simulations leak (#20), so it is
	// bounded to the newest records.
	/** The maximum number of write records kept in the history. */
	private static final int ACTIVITY_LIMIT = 10_000;
	/** The most recent writes, newest first, bounded by ACTIVITY_LIMIT. */
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

		/**
		 * The stored word, or null if this address was never set.
		 *
		 * @param addr The word address.
		 *
		 * @return the word, or null if absent.
		 */
		BitSet get(int addr);

		/**
		 * Store a word at an address.
		 *
		 * @param addr The word address.
		 * @param value The word to store.
		 */
		void put(int addr, BitSet value);

		/**
		 * Addresses that have been set, in ascending order.
		 *
		 * @return the set addresses.
		 */
		SortedSet<Integer> addresses();

		/**
		 * An independent copy (the running memory starts as a copy of the initial image).
		 *
		 * @return the copy.
		 */
		WordStore copy();
	}

	/**
	 * Dense storage: one long per word plus one presence bit, about 8
	 * bytes per word. The Map storage it replaces cost ~100 bytes per
	 * word (entry + boxed Integer + BitSet with internal long[]).
	 */
	private static final class DenseWordStore implements WordStore {

		/** The stored words, one long per address. */
		private final long[] words;
		/** One bit per address: set if that address holds a word. */
		private final BitSet present;

		/**
		 * Allocate dense storage for the full capacity, all words absent.
		 *
		 * @param capacity The number of words to reserve.
		 */
		DenseWordStore(int capacity) {
			words = new long[capacity];
			present = new BitSet(capacity);
		}

		/**
		 * Copy constructor: an independent snapshot of another store.
		 *
		 * @param from The store to copy.
		 */
		private DenseWordStore(DenseWordStore from) {
			words = from.words.clone();
			present = (BitSet)from.present.clone();
		}

		/**
		 * The stored word, or null if this address was never set.
		 *
		 * @param addr The word address.
		 *
		 * @return the word, or null if absent.
		 */
		@Override
		public BitSet get(int addr) {
			if (!present.get(addr))
				return null;
			return BitSet.valueOf(new long[] { words[addr] });
		}

		/**
		 * Store a word at an address and mark it present.
		 *
		 * @param addr The word address.
		 * @param value The word to store.
		 */
		@Override
		public void put(int addr, BitSet value) {
			long[] asLongs = value.toLongArray();
			words[addr] = asLongs.length == 0 ? 0 : asLongs[0];
			present.set(addr);
		}

		/**
		 * Addresses that have been set, in ascending order.
		 *
		 * @return the present addresses.
		 */
		@Override
		public SortedSet<Integer> addresses() {
			SortedSet<Integer> addrs = new TreeSet<Integer>();
			for (int a = present.nextSetBit(0); a >= 0; a = present.nextSetBit(a+1)) {
				addrs.add(a);
			}
			return addrs;
		}

		/**
		 * An independent copy of this store.
		 *
		 * @return the copy.
		 */
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

		/** The stored words, keyed by address. */
		private final Map<Integer,BitSet> map;

		/**
		 * Create an empty sparse store.
		 */
		SparseWordStore() {
			map = new HashMap<Integer,BitSet>();
		}

		/**
		 * Copy constructor: an independent snapshot of another store.
		 *
		 * @param from The store to copy.
		 */
		private SparseWordStore(SparseWordStore from) {
			map = new HashMap<Integer,BitSet>(from.map);
		}

		/**
		 * The stored word, or null if this address was never set.
		 *
		 * @param addr The word address.
		 *
		 * @return the word, or null if absent.
		 */
		@Override
		public BitSet get(int addr) {
			return map.get(addr);
		}

		/**
		 * Store a word at an address.
		 *
		 * @param addr The word address.
		 * @param value The word to store.
		 */
		@Override
		public void put(int addr, BitSet value) {
			map.put(addr, value);
		}

		/**
		 * Addresses that have been set, in ascending order.
		 *
		 * @return the present addresses.
		 */
		@Override
		public SortedSet<Integer> addresses() {
			return new TreeSet<Integer>(map.keySet());
		}

		/**
		 * An independent copy of this store.
		 *
		 * @return the copy.
		 */
		@Override
		public WordStore copy() {
			return new SparseWordStore(this);
		}
	}

	// dense storage allocates the full capacity eagerly; past this many
	// words (32 MB of longs) assume sparse use and fall back to the map
	/** The largest capacity, in words, given dense storage. */
	private static final int DENSE_CAPACITY_LIMIT = 1 << 22;

	/**
	 * Pick a word store sized for this memory: dense for narrow words and
	 * modest capacities, sparse otherwise.
	 *
	 * @return a new, empty word store.
	 */
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
					if (JLSInfo.noWindow()) {
						System.out.println(msg + " in memory file " +
								name + ", all zeros assumed");
					}
					else {
						TellUser.error(null,
								msg + "in memory file " +
								name + ", all zeros assumed", "Error");
					}
				}
				
			}
			catch (IOException ex) {
				
				if (JLSInfo.noWindow()) {
					System.out.println("Initialization file for memory " +
							name + " cannot be read, all zeros assumed");
				}
				else {
					TellUser.error(null,
							"Initialization file for memory " +
							name + " cannot be read, all zeros assumed", "Error");
				}
			}
			
		}
		else {
			
			// parse built-in values
			String msg = initOK(initialValue,capacity,bits,true);
			if (msg != null) {
				if (JLSInfo.noWindow()) {
					System.out.println(msg + " in memory file " +
							name + ", all zeros assumed");
				}
				else {
					TellUser.error(null,
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
	
	/**
	 * The memory operation implied by the current input signals: WRITE a
	 * word, READ a word, or OFF when the chip is not selected.
	 */
	private static enum MemoryAction {
		/** Write a word to memory. */
		WRITE,
		/** Read a word from memory. */
		READ,
		/** The chip is not selected; drive no value. */
		OFF};
	
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
		/**
		 * A pending memory operation scheduled as a future event: the
		 * action to perform, the target address, and the data involved.
		 */
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
	 *
	 * @jls.testedby jls.BatchSimulationGoldenTest#ramWriteStoresTheWord()
	 */
	public BitSet getCurrentValue(int loc){

	   return mem.get(loc);
	   } // end of getCurrentValue method

	/**
	 * The addresses of the currently-stored (non-zero) memory words,
	 * creating an empty store if the simulator has not run yet (issue #77:
	 * exposed so the GUI-side memory-contents dialog can list them without
	 * the model holding any Swing).
	 *
	 * @return the set of stored addresses, ascending.
	 */
	public SortedSet<Integer> storedAddresses() {

		if (mem == null) {
			mem = newWordStore();
		}
		return mem.addresses();
	} // end of storedAddresses method

} // end of Memory class
