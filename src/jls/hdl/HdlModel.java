package jls.hdl;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A language-neutral structural model of one circuit, produced by
 * {@link HdlExporter#buildModel} and consumed by an {@link HdlEmitter}
 * (issue #60). It knows nothing about Verilog or VHDL syntax: it holds
 * ports, internal nets, and per-element statements whose operands are
 * either net names or literal values. All names are already legalized
 * HDL identifiers; the {@link #renames} map records what legalization
 * changed so emitters can document it.
 *
 * <p>This class is deliberately dumb: all construction logic lives in
 * the exporter, all syntax in the emitters, so a VHDL emitter can be
 * added without touching either the model or the walker.</p>
 */
public final class HdlModel {

	/** Port direction. */
	public enum Direction { INPUT, OUTPUT }

	/** One module port. */
	public static final class Port {

		public final String name;
		public final Direction direction;
		public final int bits;
		/** Human context for the port (element kind, width), or null. */
		public final String comment;

		Port(String name, Direction direction, int bits, String comment) {
			this.name = name;
			this.direction = direction;
			this.bits = bits;
			this.comment = comment;
		}
	} // end of Port class

	/** One internal net (a wire that is not a port). */
	public static final class Net {

		public final String name;
		public final int bits;

		Net(String name, int bits) {
			this.name = name;
			this.bits = bits;
		}
	} // end of Net class

	/**
	 * A statement operand: either a reference to a whole net (or port)
	 * or a literal value of a known width. Unattached element inputs
	 * become zero literals, matching JLS's absent-inputs-read-as-0
	 * simulation rule (docs/simulation-semantics.md).
	 */
	public static final class Operand {

		private final String net;			// null for literals
		private final BigInteger literal;	// null for nets
		private final int bits;

		private Operand(String net, BigInteger literal, int bits) {
			this.net = net;
			this.literal = literal;
			this.bits = bits;
		}

		public static Operand net(String name, int bits) {
			return new Operand(name, null, bits);
		}

		public static Operand literal(BigInteger value, int bits) {
			return new Operand(null, value, bits);
		}

		public boolean isNet() {
			return net != null;
		}

		public String netName() {
			return net;
		}

		public BigInteger literalValue() {
			return literal;
		}

		public int bits() {
			return bits;
		}
	} // end of Operand class

	/** Base of all statements: one per exported element. */
	public abstract static class Statement {

		/** Identifies the source element ("AndGate at (240,120)"). */
		public final String comment;

		Statement(String comment) {
			this.comment = comment;
		}
	} // end of Statement class

	/** A bitwise gate (or plain buffer) driving one net. */
	public static final class GateStatement extends Statement {

		public enum Op { AND, OR, NAND, NOR, XOR, NOT, BUFFER }

		public final Op op;
		public final List<Operand> inputs;
		public final String output;

		GateStatement(String comment, Op op, List<Operand> inputs,
				String output) {
			super(comment);
			this.op = op;
			this.inputs = Collections.unmodifiableList(
					new ArrayList<Operand>(inputs));
			this.output = output;
		}
	} // end of GateStatement class

	/** Extend: every output bit copies the single 1-bit input. */
	public static final class ReplicateStatement extends Statement {

		public final Operand input;		// 1 bit
		public final String output;
		public final int bits;

		ReplicateStatement(String comment, Operand input, String output,
				int bits) {
			super(comment);
			this.input = input;
			this.output = output;
			this.bits = bits;
		}
	} // end of ReplicateStatement class

	/** A constant value driving one net. */
	public static final class ConstantStatement extends Statement {

		public final String output;
		public final int bits;
		public final BigInteger value;

		ConstantStatement(String comment, String output, int bits,
				BigInteger value) {
			super(comment);
			this.output = output;
			this.bits = bits;
			this.value = value;
		}
	} // end of ConstantStatement class

	/** Tri-state buffer: output = input when control is 1, else HiZ. */
	public static final class TriStateStatement extends Statement {

		public final Operand input;
		public final Operand control;	// 1 bit
		public final String output;
		public final int bits;

		TriStateStatement(String comment, Operand input, Operand control,
				String output, int bits) {
			super(comment);
			this.input = input;
			this.control = control;
			this.output = output;
			this.bits = bits;
		}
	} // end of TriStateStatement class

	/** Adder: {carryOut, sum} = a + b + carryIn. */
	public static final class AdderStatement extends Statement {

		public final Operand a;
		public final Operand b;
		public final Operand carryIn;	// 1 bit
		public final String sum;
		public final String carryOut;	// 1 bit
		public final int bits;

		AdderStatement(String comment, Operand a, Operand b, Operand carryIn,
				String sum, String carryOut, int bits) {
			super(comment);
			this.a = a;
			this.b = b;
			this.carryIn = carryIn;
			this.sum = sum;
			this.carryOut = carryOut;
			this.bits = bits;
		}
	} // end of AdderStatement class

	/**
	 * A register, in JLS's three flavors (docs/simulation-semantics.md
	 * §8.1). The state variable {@code regName} is distinct from the q
	 * and notQ nets it drives.
	 */
	public static final class RegisterStatement extends Statement {

		public enum Kind { LATCH, POS_EDGE, NEG_EDGE }

		public final Kind kind;
		public final String regName;
		public final Operand d;
		public final Operand clock;		// 1 bit; a literal clock never ticks
		public final String q;
		public final String notQ;
		public final int bits;
		public final BigInteger initial;

		RegisterStatement(String comment, Kind kind, String regName,
				Operand d, Operand clock, String q, String notQ, int bits,
				BigInteger initial) {
			super(comment);
			this.kind = kind;
			this.regName = regName;
			this.d = d;
			this.clock = clock;
			this.q = q;
			this.notQ = notQ;
			this.bits = bits;
			this.initial = initial;
		}
	} // end of RegisterStatement class

	/**
	 * A bit routing (Binder/Splitter): for every position i,
	 * target[targetIndex[i]] = source[sourceIndex[i]]. Emitters may
	 * coalesce contiguous runs into part-selects.
	 */
	public static final class BitMapStatement extends Statement {

		public final Operand source;
		private final int[] sourceIndex;
		public final String target;
		public final int targetBits;
		private final int[] targetIndex;

		BitMapStatement(String comment, Operand source, int[] sourceIndex,
				String target, int targetBits, int[] targetIndex) {
			super(comment);
			this.source = source;
			this.sourceIndex = sourceIndex.clone();
			this.target = target;
			this.targetBits = targetBits;
			this.targetIndex = targetIndex.clone();
		}

		public int[] sourceIndex() {
			return sourceIndex.clone();
		}

		public int[] targetIndex() {
			return targetIndex.clone();
		}
	} // end of BitMapStatement class

	// the model proper
	public final String moduleName;
	public final String sourceCircuitName;
	public final String jlsVersion;
	private final List<Port> ports = new ArrayList<Port>();
	private final List<Net> nets = new ArrayList<Net>();
	private final List<Statement> statements = new ArrayList<Statement>();
	private final Map<String, String> renames =
			new LinkedHashMap<String, String>();
	private final List<String> warnings = new ArrayList<String>();

	HdlModel(String moduleName, String sourceCircuitName, String jlsVersion) {
		this.moduleName = moduleName;
		this.sourceCircuitName = sourceCircuitName;
		this.jlsVersion = jlsVersion;
	}

	public List<Port> ports() {
		return Collections.unmodifiableList(ports);
	}

	public List<Net> nets() {
		return Collections.unmodifiableList(nets);
	}

	public List<Statement> statements() {
		return Collections.unmodifiableList(statements);
	}

	/** JLS name to legalized identifier, only where they differ. */
	public Map<String, String> renames() {
		return Collections.unmodifiableMap(renames);
	}

	/** Warnings collected during the walk (skipped elements etc.). */
	public List<String> warnings() {
		return Collections.unmodifiableList(warnings);
	}

	// package-private mutators for the exporter
	void addPort(Port port) {
		ports.add(port);
	}

	void addNet(String name, int bits) {
		nets.add(new Net(name, bits));
	}

	void addStatement(Statement statement) {
		statements.add(statement);
	}

	void addRename(String from, String to) {
		renames.put(from, to);
	}

	void addWarning(String warning) {
		warnings.add(warning);
	}

} // end of HdlModel class
