package jls.elem;

import java.io.PrintWriter;
import java.util.BitSet;

import org.jspecify.annotations.Nullable;

import jls.BitSetUtils;
import jls.Circuit;
import jls.core.Geometry;
import jls.sim.SimEvent;
import jls.sim.SimEvent.MemoryRead;
import jls.sim.SimEvent.MemoryWrite;
import jls.sim.SimEvent.NewValue;
import jls.sim.SimEvent.PinChanged;
import jls.sim.SimEvent.StateChanged;
import jls.sim.SimEvent.TableOutput;
import jls.sim.SimEvent.TriStateOff;
import jls.sim.Simulator;

/**
 * Multi-port register file (issue #201): a bank of {@code count} words,
 * each {@code bits} wide, with a configurable number of asynchronous read
 * ports and clock-synchronous write ports, and an optional "register 0
 * reads as zero" convention. This collapses the ~95 elements (31
 * Registers, hold/load Muxes, AND gates, a Decoder and two read Muxes) a
 * RISC datapath's register file otherwise needs into a single first-class
 * element.
 *
 * <p>Each read port has an address input ({@code RAn}) and a data output
 * ({@code RDn}); each write port has an address ({@code WAn}), data
 * ({@code WDn}) and write-enable ({@code WEn}) input. A single clock
 * input ({@code C}) commits every enabled write on its rising edge.
 * Reads are combinational. Orientation is fixed, mirroring
 * {@link Memory}.
 *
 * @author Josh Marshall
 */
public final class RegisterFile extends LogicElement
		implements Timed, Watchable, Editable {

	// default values
	/** Default element name (empty until the user supplies one). */
	private static final String defaultName = "";
	/** Default number of bits per word. */
	private static final int defaultBits = 1;
	/** Default number of words. */
	private static final int defaultCount = 2;
	/** Default number of read ports. */
	private static final int defaultReadPorts = 2;
	/** Default number of write ports. */
	private static final int defaultWritePorts = 1;
	/** Default "register 0 reads as zero" flag. */
	private static final boolean defaultReg0Zero = true;
	/** Default propagation delay. */
	private static final int defaultPropDelay = 50;

	// saved properties
	/** The name of this register file. */
	private String name = defaultName;
	/** The number of bits per word. */
	private int bits = defaultBits;
	/** The number of words (registers). */
	private int count = defaultCount;
	/** The number of read ports. */
	private int readPorts = defaultReadPorts;
	/** The number of write ports. */
	private int writePorts = defaultWritePorts;
	/** True if register 0 always reads as zero. */
	private boolean reg0Zero = defaultReg0Zero;
	/** The propagation delay of this element. */
	private int propDelay = defaultPropDelay;
	/** True if this element's value is watched during simulation. */
	private boolean watched = false;

	// running properties
	/**
	 * The "count x bits" description drawn inside the element, or null
	 * before {@link #init} has computed it.
	 */
	private @Nullable String specs;

	/**
	 * Create a new register-file element.
	 *
	 * @param circuit The circuit this element is part of.
	 */
	public RegisterFile(Circuit circuit) {

		super(circuit);
	} // end of constructor

	/**
	 * The number of bits an address input needs to index every word.
	 *
	 * @return the address width in bits (at least 1).
	 */
	private int addressBits() {

		return Math.max(1,
				32 - Integer.numberOfLeadingZeros(Math.max(1, count - 1)));
	} // end of addressBits method

	/**
	 * Initialize internal info for this element. Read ports occupy the
	 * left edge (address) and right edge (data); write ports occupy the
	 * left edge (address, data, write-enable); the shared clock sits on
	 * the bottom edge.
	 *
	 * @param g The Graphics object to use.
	 */
	@Override
	public void init(jls.core.@org.jspecify.annotations.Nullable TextMetrics g) {

		int s = Geometry.SPACING;
		String specs = " " + count + "x" + bits + " REGS ";
		this.specs = specs;

		int leftCount = readPorts + writePorts * 3;
		int rows = Math.max(leftCount, readPorts);

		// set up size if there is a graphics object
		if (g != null) {

			// set up size if it doesn't already have one
			if (width == 0 && height == 0) {
				jls.core.TextMetrics fm = g;
				int w1 = (fm.stringWidth(" " + name + " ") + s/2)/s*s;
				int w2 = (fm.stringWidth(specs) + s/2)/s*s;
				width = Math.max(Math.max(w1, w2), 6*s);
				height = (rows + 1) * s;
			}
		}

		int addrBits = addressBits();

		// left-edge inputs: read addresses, then per write port the
		// address, data and write-enable
		int y = s;
		for (int r = 0; r < readPorts; r += 1) {
			inputs.add(new Input("RA" + r, this, 0, y, addrBits));
			y += s;
		}
		for (int w = 0; w < writePorts; w += 1) {
			inputs.add(new Input("WA" + w, this, 0, y, addrBits));
			y += s;
			inputs.add(new Input("WD" + w, this, 0, y, bits));
			y += s;
			inputs.add(new Input("WE" + w, this, 0, y, 1));
			y += s;
		}

		// shared write clock on the bottom edge
		inputs.add(new Input("C", this, s, height, 1));

		// right-edge outputs: one read-data per read port
		int yo = s;
		for (int r = 0; r < readPorts; r += 1) {
			outputs.add(new Output("RD" + r, this, width, yo, bits));
			yo += s;
		}
	} // end of init method

	/**
	 * The "count x bits" description drawn inside the box (read by the
	 * GUI-side renderer).
	 *
	 * @return the spec string, as computed by {@link #init}.
	 */
	public String getSpecs() {

		String specs = this.specs;
		if (specs == null) {
			throw new IllegalStateException("getSpecs before init");
		}
		return specs;
	} // end of getSpecs method

	// Declarative persistence (#23): one declaration drives save, load
	// dispatch, and copy for this element's own attributes.
	/** This element's own saved attributes, in save order. */
	private static final java.util.List<Attribute> OWN_ATTRIBUTES =
			java.util.List.of(
		new Attribute.StringAttribute("name") {
			/**
			 * Read the register file's name for saving.
			 */
			@Override
			protected String get(Element el) { return ((RegisterFile)el).name; }
			/**
			 * Load the name and register it with the circuit.
			 */
			@Override
			protected void set(Element el, String v) {
				((RegisterFile)el).name = v;
				el.getCircuit().addName(v);
			}
			/**
			 * Copy the name field without re-registering it.
			 */
			@Override
			public void copy(Element from, Element to) {
				((RegisterFile)to).name = ((RegisterFile)from).name;
			}
		},
		new Attribute.IntAttribute("bits") {
			/**
			 * Read the word width for saving.
			 */
			@Override
			protected int get(Element el) { return ((RegisterFile)el).bits; }
			/**
			 * Load the word width.
			 */
			@Override
			protected void set(Element el, int v) { ((RegisterFile)el).bits = v; }
		},
		new Attribute.IntAttribute("count") {
			/**
			 * Read the number of words for saving.
			 */
			@Override
			protected int get(Element el) { return ((RegisterFile)el).count; }
			/**
			 * Load the number of words.
			 */
			@Override
			protected void set(Element el, int v) { ((RegisterFile)el).count = v; }
		},
		new Attribute.IntAttribute("read") {
			/**
			 * Read the number of read ports for saving.
			 */
			@Override
			protected int get(Element el) { return ((RegisterFile)el).readPorts; }
			/**
			 * Load the number of read ports.
			 */
			@Override
			protected void set(Element el, int v) { ((RegisterFile)el).readPorts = v; }
		},
		new Attribute.IntAttribute("write") {
			/**
			 * Read the number of write ports for saving.
			 */
			@Override
			protected int get(Element el) { return ((RegisterFile)el).writePorts; }
			/**
			 * Load the number of write ports.
			 */
			@Override
			protected void set(Element el, int v) { ((RegisterFile)el).writePorts = v; }
		},
		new Attribute.IntAttribute("reg0zero") {
			/**
			 * Read the register-0-reads-zero flag (1/0) for saving.
			 */
			@Override
			protected int get(Element el) {
				return ((RegisterFile)el).reg0Zero ? 1 : 0;
			}
			/**
			 * Load the register-0-reads-zero flag.
			 */
			@Override
			protected void set(Element el, int v) {
				((RegisterFile)el).reg0Zero = v != 0;
			}
		},
		new Attribute.IntAttribute("delay") {
			/**
			 * Read the propagation delay for saving.
			 */
			@Override
			protected int get(Element el) { return ((RegisterFile)el).propDelay; }
			/**
			 * Load the propagation delay.
			 */
			@Override
			protected void set(Element el, int v) { ((RegisterFile)el).propDelay = v; }
		},
		new Attribute.IntAttribute("watch") {
			/**
			 * Read the watched flag (1/0) for saving.
			 */
			@Override
			protected int get(Element el) {
				return ((RegisterFile)el).watched ? 1 : 0;
			}
			/**
			 * Load the watched flag.
			 */
			@Override
			protected void set(Element el, int v) {
				((RegisterFile)el).watched = v != 0;
			}
		}
	);

	/** Base attributes plus this element's own, in save order. */
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
	 * Save this element.
	 *
	 * @param output The output writer.
	 */
	@Override
	public void save(PrintWriter output) {

		output.println("ELEMENT RegisterFile");
		super.save(output);
		output.println("END");
	} // end of save method

	/**
	 * Copy this element.
	 */
	@Override
	public Element copy() {

		RegisterFile it = new RegisterFile(getCircuit());
		it.specs = specs;
		for (Input input : inputs) {
			it.inputs.add(input.copy(it));
		}
		for (Output output : outputs) {
			it.outputs.add(output.copy(it));
		}
		super.copy(it);
		return it;
	} // end of copy method

	/**
	 * Display info about this element.
	 *
	 * @return the text describing this element, or an empty string.
	 */
	@Override
	public String infoText() {

		return count + "x" + bits + " register file, " + readPorts
				+ " read / " + writePorts + " write"
				+ (reg0Zero ? ", reg 0 reads zero" : "");
	} // end of showInfo method

	/**
	 * Get the name of this register file.
	 *
	 * @return the name.
	 */
	@Override
	public String getName() {

		return name;
	} // end of getName method

	/**
	 * Get the number of bits per word in this register file.
	 *
	 * @return the number of bits per word.
	 */
	@Override
	public int getBits() {

		return bits;
	} // end of getBits method

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
	 * See if this register file is watched.
	 *
	 * @return true if it is, false if it is not.
	 */
	@Override
	public boolean isWatched() {

		return watched;
	} // end of isWatched method

	/**
	 * Set whether this register file is watched or not.
	 *
	 * @param state True to make it watched, false to make it not watched.
	 */
	@Override
	public void setWatched(boolean state) {

		watched = state;
	} // end of setWatched method

	/**
	 * Reset propagation delay to default value.
	 */
	@Override
	public void resetPropDelay() {

		propDelay = defaultPropDelay;
	} // end of resetPropDelay method

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

//	-------------------------------------------------------------------------------
//	Simulation
//	-------------------------------------------------------------------------------

	/** The words held during simulation, or null before initSim. */
	private BitSet @Nullable [] words;
	/** The most recent value seen on the shared clock input. */
	private int currentC;

	/**
	 * Initialize this element: clear every word and drive each read-data
	 * output to zero.
	 *
	 * @param sim Unused.
	 */
	@Override
	public void initSim(Simulator sim) {

		BitSet[] words = new BitSet[count];
		for (int i = 0; i < count; i += 1) {
			words[i] = new BitSet(bits);
		}
		this.words = words;
		currentC = 0;

		for (int r = 0; r < readPorts; r += 1) {
			getOutput("RD" + r).setValue(new BitSet(bits));
		}
	} // end of initSim method

	/**
	 * The value read from a word address: zero when the address selects
	 * register 0 and register 0 reads as zero, or when the address is out
	 * of range; otherwise the stored word.
	 *
	 * @param words The word store.
	 * @param addr The word address.
	 *
	 * @return the read value.
	 */
	private BitSet readWord(BitSet[] words, int addr) {

		if (addr < 0 || addr >= count) {
			return new BitSet(bits);
		}
		if (reg0Zero && addr == 0) {
			return new BitSet(bits);
		}
		return (BitSet) words[addr].clone();
	} // end of readWord method

	/**
	 * React to an event. On a rising clock edge every enabled write
	 * commits; every read port is then re-driven combinationally.
	 *
	 * @param now The current simulation time.
	 * @param sim The simulator to post events to.
	 * @param todo PinChanged when an input changes.
	 */
	@Override
	public void react(long now, Simulator sim, SimEvent.Payload todo) {

		switch (todo) {

		case PinChanged _ -> {

			BitSet[] words = this.words;
			if (words == null) {
				throw new IllegalStateException("react before initSim");
			}

			// detect a rising edge on the shared clock
			BitSet clockVal = getInput("C").getValue();
			if (clockVal == null)
				clockVal = new BitSet();
			int c = clockVal.get(0) ? 1 : 0;
			boolean risingEdge = currentC == 0 && c == 1;
			currentC = c;

			// commit enabled writes on the rising edge
			if (risingEdge) {
				for (int w = 0; w < writePorts; w += 1) {
					BitSet weVal = getInput("WE" + w).getValue();
					if (weVal == null || !weVal.get(0)) {
						continue;
					}
					BitSet addrVal = getInput("WA" + w).getValue();
					if (addrVal == null)
						addrVal = new BitSet();
					int addr = BitSetUtils.ToInt(addrVal);
					if (addr < 0 || addr >= count) {
						continue;
					}
					if (reg0Zero && addr == 0) {
						continue;
					}
					BitSet data = getInput("WD" + w).getValue();
					if (data == null)
						data = new BitSet();
					BitSet stored = new BitSet(bits);
					for (int i = 0; i < bits; i += 1) {
						if (data.get(i)) {
							stored.set(i);
						}
					}
					words[addr] = stored;
				}
			}

			// re-drive every read port combinationally
			for (int r = 0; r < readPorts; r += 1) {
				BitSet addrVal = getInput("RA" + r).getValue();
				if (addrVal == null)
					addrVal = new BitSet();
				int addr = BitSetUtils.ToInt(addrVal);
				getOutput("RD" + r).propagate(readWord(words, addr), now, sim);
			}
		}

		case NewValue _, TriStateOff _, StateChanged _, MemoryRead _,
				MemoryWrite _, TableOutput _ ->
			throw new IllegalStateException("unexpected payload: " + todo);
		}
	} // end of react method

} // end of RegisterFile class
