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
	public enum Direction {
		/** The port feeds values into the module. */
		INPUT,
		/** The port carries values out of the module. */
		OUTPUT
	} // end of Direction enum

	/** One module port. */
	public static final class Port {

		/** Legalized HDL identifier for the port. */
		public final String name;
		/** Whether the port is an input or an output. */
		public final Direction direction;
		/** Width of the port in bits. */
		public final int bits;
		/** Human context for the port (element kind, width), or null. */
		public final String comment;

		/**
		 * Builds an immutable port descriptor.
		 * @param name legalized HDL identifier for the port
		 * @param direction whether the port is an input or an output
		 * @param bits width of the port in bits
		 * @param comment human context (element kind, width), or null
		 */
		Port(String name, Direction direction, int bits, String comment) {
			this.name = name;
			this.direction = direction;
			this.bits = bits;
			this.comment = comment;
		}
	} // end of Port class

	/** One internal net (a wire that is not a port). */
	public static final class Net {

		/** Legalized HDL identifier for the net. */
		public final String name;
		/** Width of the net in bits. */
		public final int bits;

		/**
		 * Builds an immutable internal-net descriptor.
		 * @param name legalized HDL identifier for the net
		 * @param bits width of the net in bits
		 */
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

		/** Referenced net name; null for literals. */
		private final String net;
		/** Literal value; null for net references. */
		private final BigInteger literal;
		/** Operand width in bits. */
		private final int bits;

		/**
		 * Private canonical constructor; use {@link #net} or
		 * {@link #literal}. Exactly one of net/literal is non-null.
		 * @param net referenced net name, or null for a literal
		 * @param literal literal value, or null for a net reference
		 * @param bits operand width in bits
		 */
		private Operand(String net, BigInteger literal, int bits) {
			this.net = net;
			this.literal = literal;
			this.bits = bits;
		}

		/**
		 * Creates an operand referencing a whole net or port.
		 * @param name the net/port name
		 * @param bits operand width in bits
		 * @return a net-reference operand
		 */
		public static Operand net(String name, int bits) {
			return new Operand(name, null, bits);
		}

		/**
		 * Creates a literal-valued operand of a known width.
		 * @param value the literal value
		 * @param bits operand width in bits
		 * @return a literal operand
		 */
		public static Operand literal(BigInteger value, int bits) {
			return new Operand(null, value, bits);
		}

		/**
		 * Distinguishes net references from literals.
		 * @return true if this operand references a net, false if literal
		 */
		public boolean isNet() {
			return net != null;
		}

		/**
		 * Gives the net this operand references, if any.
		 * @return the referenced net name, or null for a literal
		 */
		public String netName() {
			return net;
		}

		/**
		 * Gives the constant this operand carries, if any.
		 * @return the literal value, or null for a net reference
		 */
		public BigInteger literalValue() {
			return literal;
		}

		/**
		 * Gives the operand's width.
		 * @return operand width in bits
		 */
		public int bits() {
			return bits;
		}
	} // end of Operand class

	/** Base of all statements: one per exported element. */
	public abstract static class Statement {

		/** Identifies the source element ("AndGate at (240,120)"). */
		public final String comment;

		/**
		 * Records the provenance shared by all statement kinds.
		 * @param comment identifies the source element
		 */
		Statement(String comment) {
			this.comment = comment;
		}

		/**
		 * Double-dispatch to the emitter's per-statement template.
		 * @param visitor the emitter template to dispatch to
		 */
		public abstract void accept(StatementVisitor visitor);
	} // end of Statement class

	/**
	 * One visit method per statement kind; emitters implement this so
	 * dispatch is by polymorphism, not instanceof chains, and a new
	 * statement kind fails to compile until every emitter handles it.
	 */
	public interface StatementVisitor {
		/**
		 * Emit a bitwise gate statement.
		 * @param statement the statement to emit
		 */
		void visit(GateStatement statement);
		/**
		 * Emit a bit-replication (extend) statement.
		 * @param statement the statement to emit
		 */
		void visit(ReplicateStatement statement);
		/**
		 * Emit a constant-driver statement.
		 * @param statement the statement to emit
		 */
		void visit(ConstantStatement statement);
		/**
		 * Emit a tri-state buffer statement.
		 * @param statement the statement to emit
		 */
		void visit(TriStateStatement statement);
		/**
		 * Emit an adder statement.
		 * @param statement the statement to emit
		 */
		void visit(AdderStatement statement);
		/**
		 * Emit a register statement.
		 * @param statement the statement to emit
		 */
		void visit(RegisterStatement statement);
		/**
		 * Emit a bit-routing (Binder/Splitter) statement.
		 * @param statement the statement to emit
		 */
		void visit(BitMapStatement statement);
		/** Emit a selected-assignment (Mux/Decoder) statement. */
		void visit(SelectStatement statement);
	} // end of StatementVisitor interface

	/** A bitwise gate (or plain buffer) driving one net. */
	public static final class GateStatement extends Statement {

		/** The bitwise operation the gate applies; BUFFER is a plain pass-through. */
		public enum Op {
			/** Bitwise AND of the inputs. */
			AND,
			/** Bitwise OR of the inputs. */
			OR,
			/** Bitwise NAND of the inputs. */
			NAND,
			/** Bitwise NOR of the inputs. */
			NOR,
			/** Bitwise XOR of the inputs. */
			XOR,
			/** Bitwise complement of the single input. */
			NOT,
			/** Plain pass-through of the single input. */
			BUFFER
		} // end of Op enum

		/** The bitwise operation (or BUFFER). */
		public final Op op;
		/** The gate inputs, unmodifiable. */
		public final List<Operand> inputs;
		/** Net driven by the gate. */
		public final String output;

		/**
		 * Builds an immutable gate statement.
		 * @param comment identifies the source element
		 * @param op the bitwise operation (or BUFFER)
		 * @param inputs the gate inputs (defensively copied)
		 * @param output net driven by the gate
		 */
		GateStatement(String comment, Op op, List<Operand> inputs,
				String output) {
			super(comment);
			this.op = op;
			this.inputs = Collections.unmodifiableList(
					new ArrayList<Operand>(inputs));
			this.output = output;
		}

		/** Double-dispatch to the emitter's matching visit method. */
		@Override
		public void accept(StatementVisitor visitor) {
			visitor.visit(this);
		}
	} // end of GateStatement class

	/** Extend: every output bit copies the single 1-bit input. */
	public static final class ReplicateStatement extends Statement {

		/** The single 1-bit input to replicate. */
		public final Operand input;
		/** Net driven by the replicated bits. */
		public final String output;
		/** Number of output bits. */
		public final int bits;

		/**
		 * Builds an immutable replicate statement.
		 * @param comment identifies the source element
		 * @param input the single 1-bit input to replicate
		 * @param output net driven by the replicated bits
		 * @param bits number of output bits
		 */
		ReplicateStatement(String comment, Operand input, String output,
				int bits) {
			super(comment);
			this.input = input;
			this.output = output;
			this.bits = bits;
		}

		/** Double-dispatch to the emitter's matching visit method. */
		@Override
		public void accept(StatementVisitor visitor) {
			visitor.visit(this);
		}
	} // end of ReplicateStatement class

	/** A constant value driving one net. */
	public static final class ConstantStatement extends Statement {

		/** Net driven by the constant. */
		public final String output;
		/** Width of the constant in bits. */
		public final int bits;
		/** The constant value. */
		public final BigInteger value;

		/**
		 * Builds an immutable constant statement.
		 * @param comment identifies the source element
		 * @param output net driven by the constant
		 * @param bits width of the constant in bits
		 * @param value the constant value
		 */
		ConstantStatement(String comment, String output, int bits,
				BigInteger value) {
			super(comment);
			this.output = output;
			this.bits = bits;
			this.value = value;
		}

		/** Double-dispatch to the emitter's matching visit method. */
		@Override
		public void accept(StatementVisitor visitor) {
			visitor.visit(this);
		}
	} // end of ConstantStatement class

	/** Tri-state buffer: output = input when control is 1, else HiZ. */
	public static final class TriStateStatement extends Statement {

		/** The driven value. */
		public final Operand input;
		/** 1-bit enable; output is HiZ when 0. */
		public final Operand control;
		/** Net driven by the buffer. */
		public final String output;
		/** Width of the data path in bits. */
		public final int bits;

		/**
		 * Builds an immutable tri-state statement.
		 * @param comment identifies the source element
		 * @param input the driven value
		 * @param control 1-bit enable; output is HiZ when 0
		 * @param output net driven by the buffer
		 * @param bits width of the data path in bits
		 */
		TriStateStatement(String comment, Operand input, Operand control,
				String output, int bits) {
			super(comment);
			this.input = input;
			this.control = control;
			this.output = output;
			this.bits = bits;
		}

		/** Double-dispatch to the emitter's matching visit method. */
		@Override
		public void accept(StatementVisitor visitor) {
			visitor.visit(this);
		}
	} // end of TriStateStatement class

	/** Adder: {carryOut, sum} = a + b + carryIn. */
	public static final class AdderStatement extends Statement {

		/** First addend. */
		public final Operand a;
		/** Second addend. */
		public final Operand b;
		/** 1-bit carry input. */
		public final Operand carryIn;
		/** Net driven by the sum. */
		public final String sum;
		/** 1-bit carry output net. */
		public final String carryOut;
		/** Width of the addends and sum in bits. */
		public final int bits;

		/**
		 * Builds an immutable adder statement.
		 * @param comment identifies the source element
		 * @param a first addend
		 * @param b second addend
		 * @param carryIn 1-bit carry input
		 * @param sum net driven by the sum
		 * @param carryOut 1-bit carry output net
		 * @param bits width of the addends and sum in bits
		 */
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

		/** Double-dispatch to the emitter's matching visit method. */
		@Override
		public void accept(StatementVisitor visitor) {
			visitor.visit(this);
		}
	} // end of AdderStatement class

	/**
	 * A register, in JLS's three flavors (docs/simulation-semantics.md
	 * §8.1). The state variable {@code regName} is distinct from the q
	 * and notQ nets it drives.
	 */
	public static final class RegisterStatement extends Statement {

		/** The register's clocking behavior: level-sensitive latch or edge-triggered flip-flop. */
		public enum Kind {
			/** Level-sensitive latch: transparent while the clock is 1. */
			LATCH,
			/** Flip-flop clocked on the rising edge. */
			POS_EDGE,
			/** Flip-flop clocked on the falling edge. */
			NEG_EDGE
		} // end of Kind enum

		/** Latch, positive-edge, or negative-edge. */
		public final Kind kind;
		/** The state variable name. */
		public final String regName;
		/** Data input. */
		public final Operand d;
		/** 1-bit clock; a literal clock never ticks. */
		public final Operand clock;
		/** Net driven by the register state. */
		public final String q;
		/** Net driven by the inverted state. */
		public final String notQ;
		/** Width of the register in bits. */
		public final int bits;
		/** Reset/initial value. */
		public final BigInteger initial;

		/**
		 * Builds an immutable register statement.
		 * @param comment identifies the source element
		 * @param kind latch, positive-edge, or negative-edge
		 * @param regName the state variable name
		 * @param d data input
		 * @param clock 1-bit clock; a literal clock never ticks
		 * @param q net driven by the register state
		 * @param notQ net driven by the inverted state
		 * @param bits width of the register in bits
		 * @param initial reset/initial value
		 */
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

		/** Double-dispatch to the emitter's matching visit method. */
		@Override
		public void accept(StatementVisitor visitor) {
			visitor.visit(this);
		}
	} // end of RegisterStatement class

	/**
	 * A bit routing (Binder/Splitter): for every position i,
	 * target[targetIndex[i]] = source[sourceIndex[i]]. Emitters may
	 * coalesce contiguous runs into part-selects.
	 */
	public static final class BitMapStatement extends Statement {

		/** The source operand being routed. */
		public final Operand source;
		/** Source bit positions, one per routed bit. */
		private final int[] sourceIndex;
		/** Net receiving the routed bits. */
		public final String target;
		/** Width of the target in bits. */
		public final int targetBits;
		/** Target bit positions, one per routed bit. */
		private final int[] targetIndex;

		/**
		 * Builds an immutable bit-routing statement.
		 * @param comment identifies the source element
		 * @param source the source operand being routed
		 * @param sourceIndex source bit positions (defensively copied)
		 * @param target net receiving the routed bits
		 * @param targetBits width of the target in bits
		 * @param targetIndex target bit positions (defensively copied)
		 */
		BitMapStatement(String comment, Operand source, int[] sourceIndex,
				String target, int targetBits, int[] targetIndex) {
			super(comment);
			this.source = source;
			this.sourceIndex = sourceIndex.clone();
			this.target = target;
			this.targetBits = targetBits;
			this.targetIndex = targetIndex.clone();
		}

		/**
		 * Gives the source bit positions.
		 * @return a copy of the source bit-position array
		 */
		public int[] sourceIndex() {
			return sourceIndex.clone();
		}

		/**
		 * Gives the target bit positions.
		 * @return a copy of the target bit-position array
		 */
		public int[] targetIndex() {
			return targetIndex.clone();
		}

		/** Double-dispatch to the emitter's matching visit method. */
		@Override
		public void accept(StatementVisitor visitor) {
			visitor.visit(this);
		}
	} // end of BitMapStatement class

	/**
	 * A selected assignment (Mux and Decoder, issue #59's "combinational
	 * three"): the target takes {@code values.get(i)} when the selector
	 * equals {@code i}, and {@code defaultValue} for any other selector
	 * value. A JLS Mux reads 0 past its last input, so its default is a
	 * zero literal; a Decoder enumerates its selector's full range, so
	 * its default is unreachable in two-state simulation and exists only
	 * to keep the emitted case/select complete. The selector is always a
	 * net reference: the exporter folds a literal (unattached) selector
	 * into the chosen value before building a statement, so emitters
	 * never see a constant case expression.
	 */
	public static final class SelectStatement extends Statement {

		public final Operand selector;
		public final List<Operand> values;
		public final Operand defaultValue;
		public final String target;
		public final int bits;
		/**
		 * State-variable name for emitters whose selected assignment
		 * needs a helper (the Verilog {@code reg} driven by the
		 * {@code always}/{@code case} block); unused by emitters with a
		 * concurrent selected assignment (VHDL {@code with}/
		 * {@code select}).
		 */
		public final String helperName;

		/**
		 * @param comment identifies the source element
		 * @param selector the (always net) selector value
		 * @param values value taken per selector index (defensively copied)
		 * @param defaultValue value taken for any other selector value
		 * @param target net driven by the selection
		 * @param bits width of the target and every value in bits
		 * @param helperName pre-claimed state-variable name for emitters
		 * that need one
		 */
		SelectStatement(String comment, Operand selector,
				List<Operand> values, Operand defaultValue, String target,
				int bits, String helperName) {
			super(comment);
			this.selector = selector;
			this.values = Collections.unmodifiableList(
					new ArrayList<Operand>(values));
			this.defaultValue = defaultValue;
			this.target = target;
			this.bits = bits;
			this.helperName = helperName;
		}

		/** Double-dispatch to the emitter's matching visit method. */
		@Override
		public void accept(StatementVisitor visitor) {
			visitor.visit(this);
		}
	} // end of SelectStatement class

	// the model proper
	/** Legalized HDL module name. */
	public final String moduleName;
	/** Original JLS circuit name. */
	public final String sourceCircuitName;
	/** JLS version that produced the model. */
	public final String jlsVersion;
	/** The module ports, in declaration order. */
	private final List<Port> ports = new ArrayList<Port>();
	/** The internal nets, in declaration order. */
	private final List<Net> nets = new ArrayList<Net>();
	/** The per-element statements, in walk order. */
	private final List<Statement> statements = new ArrayList<Statement>();
	/** JLS name to legalized identifier, only where they differ. */
	private final Map<String, String> renames =
			new LinkedHashMap<String, String>();
	/** Warnings collected during the walk (skipped elements etc.). */
	private final List<String> warnings = new ArrayList<String>();

	/**
	 * Creates an empty model; the exporter fills it via the package-private
	 * mutators.
	 * @param moduleName legalized HDL module name
	 * @param sourceCircuitName original JLS circuit name
	 * @param jlsVersion JLS version that produced the model
	 */
	HdlModel(String moduleName, String sourceCircuitName, String jlsVersion) {
		this.moduleName = moduleName;
		this.sourceCircuitName = sourceCircuitName;
		this.jlsVersion = jlsVersion;
	}

	/**
	 * Gives the module ports.
	 * @return the module ports, unmodifiable
	 */
	public List<Port> ports() {
		return Collections.unmodifiableList(ports);
	}

	/**
	 * Gives the internal nets.
	 * @return the internal nets, unmodifiable
	 * @see jls.hdl.HdlPolicyTest#twoNetsBridgedByJumpsBecomeOneVerilogNet()
	 */
	public List<Net> nets() {
		return Collections.unmodifiableList(nets);
	}

	/**
	 * Gives the per-element statements.
	 * @return the per-element statements, unmodifiable
	 */
	public List<Statement> statements() {
		return Collections.unmodifiableList(statements);
	}

	/**
	 * JLS name to legalized identifier, only where they differ.
	 * @return the rename map, unmodifiable
	 */
	public Map<String, String> renames() {
		return Collections.unmodifiableMap(renames);
	}

	/**
	 * Warnings collected during the walk (skipped elements etc.).
	 * @return the warning messages, unmodifiable
	 */
	public List<String> warnings() {
		return Collections.unmodifiableList(warnings);
	}

	// package-private mutators for the exporter
	/**
	 * Appends a port (exporter use only).
	 * @param port the port to add
	 */
	void addPort(Port port) {
		ports.add(port);
	}

	/**
	 * Appends an internal net (exporter use only).
	 * @param name legalized net name
	 * @param bits width of the net in bits
	 */
	void addNet(String name, int bits) {
		nets.add(new Net(name, bits));
	}

	/**
	 * Appends a statement (exporter use only).
	 * @param statement the statement to add
	 */
	void addStatement(Statement statement) {
		statements.add(statement);
	}

	/**
	 * Records a legalization rename (exporter use only).
	 * @param from original JLS name
	 * @param to legalized HDL identifier
	 */
	void addRename(String from, String to) {
		renames.put(from, to);
	}

	/**
	 * Records a walk-time warning (exporter use only).
	 * @param warning the warning message
	 */
	void addWarning(String warning) {
		warnings.add(warning);
	}

} // end of HdlModel class
