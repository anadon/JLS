package jls.hdl;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

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

	/**
	 * One module port (a record, issue #94).
	 *
	 * @param name Legalized HDL identifier for the port.
	 * @param direction Whether the port is an input or an output.
	 * @param bits Width of the port in bits.
	 * @param comment Human context for the port (element kind, width).
	 */
	public record Port(String name, Direction direction, int bits,
			String comment) {
	} // end of Port record

	/**
	 * One internal net (a wire that is not a port) — a record, issue #94.
	 *
	 * @param name Legalized HDL identifier for the net.
	 * @param bits Width of the net in bits.
	 */
	public record Net(String name, int bits) {
	} // end of Net record

	/**
	 * A statement operand: either a reference to a whole net (or port)
	 * or a literal value of a known width. Unattached element inputs
	 * become zero literals, matching JLS's absent-inputs-read-as-0
	 * simulation rule (docs/simulation-semantics.md). A record (issue
	 * #94); exactly one of {@code netName}/{@code literalValue} is
	 * non-null, so build instances through {@link #net} or
	 * {@link #literal}.
	 *
	 * @param netName Referenced net name; null for literals.
	 * @param literalValue Literal value; null for net references.
	 * @param bits Operand width in bits.
	 */
	public record Operand(@Nullable String netName,
			@Nullable BigInteger literalValue, int bits) {

		/**
		 * Rejects the component mixes the factories never produce
		 * (both null, or both non-null).
		 */
		public Operand {
			if ((netName == null) == (literalValue == null)) {
				throw new IllegalArgumentException(
						"exactly one of netName/literalValue must be non-null");
			}
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
			return netName != null;
		}
	} // end of Operand record

	/** Base of all statements: one per exported element. */
	public abstract static class Statement {

		/**
		 * Identifies the source element ("AndGate at (240,120)"), or null
		 * for a continuation statement that shares the prior line's element
		 * (e.g. the 2nd..nth range of a Binder/Splitter).
		 */
		public final @Nullable String comment;

		/**
		 * Records the provenance shared by all statement kinds.
		 * @param comment identifies the source element, or null for a
		 * continuation statement that carries no fresh provenance
		 */
		Statement(@Nullable String comment) {
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
		/**
		 * Emit a selected-assignment (Mux/Decoder) statement.
		 * @param statement the statement to emit
		 */
		void visit(SelectStatement statement);
		/**
		 * Emit a priority truth-table (TruthTable) statement.
		 * @param statement the statement to emit
		 */
		void visit(PriorityCaseStatement statement);
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
		 * @param comment identifies the source element, or null for the
		 * 2nd..nth range of a multi-range Binder/Splitter (only the first
		 * range carries the element's provenance comment)
		 * @param source the source operand being routed
		 * @param sourceIndex source bit positions (defensively copied)
		 * @param target net receiving the routed bits
		 * @param targetBits width of the target in bits
		 * @param targetIndex target bit positions (defensively copied)
		 */
		BitMapStatement(@Nullable String comment, Operand source,
				int[] sourceIndex, String target, int targetBits,
				int[] targetIndex) {
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

		/** The (always net) selector value. */
		public final Operand selector;
		/** Value taken per selector index. */
		public final List<Operand> values;
		/** Value taken for any other selector value. */
		public final Operand defaultValue;
		/** Net driven by the selection. */
		public final String target;
		/** Width of the target and every value in bits. */
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
		 * Create a selected-assignment statement.
		 *
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

	/**
	 * A priority truth table (TruthTable, issue #59): every row pairs an
	 * input pattern over the 1-bit inputs (0, 1, or 2 for don't-care)
	 * with the 0/1 value each target takes when that row is the FIRST
	 * match in table order. An input vector matching no row leaves every
	 * target holding its prior value - TruthTable.react's issue-#52 rule
	 * - so emitters must render a priority match WITHOUT a default
	 * branch (Verilog {@code casez} with no {@code default} arm, VHDL
	 * if/elsif {@code std_match} with no {@code else}). Output
	 * don't-cares were already lowered to 0 by the exporter, matching
	 * the simulator. Two documented divergences. Startup: JLS drives
	 * row 0's output values at startup, while HDL outputs stay
	 * undefined until an input pattern first matches. High-impedance
	 * inputs (a tri-state-driven or floating net): JLS reads a null
	 * input as 0 and matches rows accordingly, but in Verilog a z bit
	 * in the {@code casez} <em>expression</em> is itself a wildcard -
	 * the first row whose other bits match wins, silently shifting row
	 * priority - while the VHDL {@code std_match} chain matches no row
	 * and holds the prior value. Synthesizable HDL cannot branch on z
	 * (z-comparison needs the simulation-only {@code ===}), so this
	 * remains a documented simulation-boundary divergence, the same
	 * treatment as the gate templates' x-degradation; in synthesized
	 * hardware the input resolves to a real level and all three agree.
	 */
	public static final class PriorityCaseStatement extends Statement {

		/** One table row: an input pattern and its output values. */
		public static final class Row {

			/** Per input column: 0, 1, or 2 (don't care). */
			private final int[] pattern;
			/** Per output column: 0 or 1 (don't care already lowered). */
			private final int[] outputs;

			/**
			 * Builds an immutable row.
			 * @param pattern per-input 0/1/2 entries (defensively copied)
			 * @param outputs per-output 0/1 values (defensively copied)
			 */
			Row(int[] pattern, int[] outputs) {
				this.pattern = pattern.clone();
				this.outputs = outputs.clone();
			}

			/**
			 * Gives the input pattern.
			 * @return a copy of the per-input 0/1/2 entries
			 */
			public int[] pattern() {
				return pattern.clone();
			}

			/**
			 * Gives the output values.
			 * @return a copy of the per-output 0/1 values
			 */
			public int[] outputs() {
				return outputs.clone();
			}
		} // end of Row class

		/** The 1-bit inputs, in table column order, unmodifiable. */
		public final List<Operand> inputs;
		/** The rows, in table (priority) order, unmodifiable. */
		public final List<Row> rows;
		/** The 1-bit nets driven, one per output column, unmodifiable. */
		public final List<String> targets;
		/**
		 * State-variable names, one per target, for emitters whose
		 * priority match needs helpers (the Verilog {@code reg}s driven
		 * by the {@code always}/{@code casez} block); emitters that
		 * assign the targets directly from a process (VHDL) ignore them.
		 */
		public final List<String> helperNames;

		/**
		 * Builds an immutable priority-case statement.
		 * @param comment identifies the source element
		 * @param inputs the 1-bit inputs, in table column order
		 * (defensively copied)
		 * @param rows the rows, in table (priority) order (defensively
		 * copied)
		 * @param targets the driven 1-bit nets, one per output column
		 * (defensively copied)
		 * @param helperNames pre-claimed state-variable names, one per
		 * target, for emitters that need them (defensively copied)
		 */
		PriorityCaseStatement(String comment, List<Operand> inputs,
				List<Row> rows, List<String> targets,
				List<String> helperNames) {
			super(comment);
			this.inputs = Collections.unmodifiableList(
					new ArrayList<Operand>(inputs));
			this.rows = Collections.unmodifiableList(
					new ArrayList<Row>(rows));
			this.targets = Collections.unmodifiableList(
					new ArrayList<String>(targets));
			this.helperNames = Collections.unmodifiableList(
					new ArrayList<String>(helperNames));
		}

		/** Double-dispatch to the emitter's matching visit method. */
		@Override
		public void accept(StatementVisitor visitor) {
			visitor.visit(this);
		}
	} // end of PriorityCaseStatement class

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
	 * @jls.testedby jls.hdl.HdlPolicyTest#twoNetsBridgedByJumpsBecomeOneVerilogNet()
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
