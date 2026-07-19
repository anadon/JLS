package jls.hdl;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import jls.Circuit;
import jls.JLSInfo;
import jls.elem.Adder;
import jls.elem.AndGate;
import jls.elem.Binder;
import jls.elem.Clock;
import jls.elem.Constant;
import jls.elem.Decoder;
import jls.elem.DelayGate;
import jls.elem.Display;
import jls.elem.Element;
import jls.elem.Extend;
import jls.elem.Gate;
import jls.elem.Input;
import jls.elem.InputPin;
import jls.elem.JumpEnd;
import jls.elem.JumpStart;
import jls.elem.LogicElement;
import jls.elem.Mux;
import jls.elem.NandGate;
import jls.elem.NorGate;
import jls.elem.NotGate;
import jls.elem.OrGate;
import jls.elem.Output;
import jls.elem.OutputPin;
import jls.elem.Pause;
import jls.elem.Put;
import jls.elem.Register;
import jls.elem.SigGen;
import jls.elem.Splitter;
import jls.elem.Stop;
import jls.elem.TestGen;
import jls.elem.Text;
import jls.elem.TriState;
import jls.elem.Wire;
import jls.elem.WireEnd;
import jls.elem.WireNet;
import jls.elem.XorGate;

/**
 * Schematic-to-HDL export, stage 1 of issue #60: walks a loaded
 * circuit's element graph into a language-neutral {@link HdlModel}
 * and renders it through an {@link HdlEmitter}. GUI-free by design
 * (see HeadlessCoreRatchetTest): usable from the CLI and, later, the
 * editor.
 *
 * <h2>Element policy (enumerated up front)</h2>
 * Every element class falls into exactly one bucket:
 * <ul>
 * <li><b>Exported</b>: InputPin, OutputPin (module ports); Constant;
 * the gate family (AND, OR, NAND, NOR, XOR, NOT, DELAY); Extend;
 * TriState; Adder; Register (all three types, with initial value);
 * Clock (an input port to drive from a testbench); Binder and
 * Splitter (bit routing); Mux and Decoder (selected assignments,
 * the #59-adjudicated case/select templates).</li>
 * <li><b>Net topology, not instances</b>: Wire, WireEnd, JumpStart,
 * JumpEnd. Same-named jumps alias nets, so the net walk folds them
 * into one HDL net.</li>
 * <li><b>Warn and skip</b> (simulation control / annotation, no HDL
 * meaning): Display, SigGen, Pause, Stop, Text, TestGen. Each skip
 * produces a warning the caller surfaces.</li>
 * <li><b>Reject</b> (inexpressible in this version, §9 escalation):
 * SubCircuit, Memory, StateMachine, TruthTable, and anything
 * unrecognized. Rejection lists <em>every</em> offender in one
 * message and nothing is written.</li>
 * </ul>
 *
 * <h2>Net walk</h2>
 * {@code Circuit.finishLoad} partitions wires into {@link WireNet}s;
 * this walker unions nets bridged by same-named JumpStart/JumpEnd
 * pairs (the same aliasing CircuitAssert's connectivity BFS honors)
 * so each electrical net becomes exactly one HDL net. Net names are
 * chosen deterministically, most-user-visible first: the input-pin or
 * clock port name when the net is a port, else the (smallest) jump
 * name, else {@code <register>_q}/{@code <register>_nq} for register
 * outputs, else a synthesized {@code net_<id>} from the driving
 * element's save id.
 */
public final class HdlExporter {

	/** Not instantiable; all entry points are static. */
	private HdlExporter() {
	} // not instantiable

	/** What the exporter produced: text plus non-fatal warnings. */
	public static final class Result {

		/** The rendered target-language text. */
		public final String text;
		/** Non-fatal messages produced during the walk, unmodifiable. */
		public final List<String> warnings;

		/**
		 * Captures one export's output.
		 * @param text The rendered target-language text.
		 * @param warnings Non-fatal messages; copied and made immutable.
		 */
		Result(String text, List<String> warnings) {
			this.text = text;
			this.warnings = Collections.unmodifiableList(
					new ArrayList<String>(warnings));
		}
	} // end of Result class

	/**
	 * Export a loaded circuit through the given emitter.
	 *
	 * @param circ The circuit (already through finishLoad).
	 * @param emitter The target-language emitter.
	 *
	 * @return the rendered text and any warnings.
	 *
	 * @throws HdlExportException if the circuit contains elements the
	 * exporter cannot express; the message names all of them.
	 *
	 * @jls.testedby jls.hdl.HdlPolicyTest#decoderWithUnattachedInputConstantlyDrivesBitZero()
	 * @jls.testedby jls.hdl.HdlPolicyTest#displayIsSkippedWithAWarning()
	 * @jls.testedby jls.hdl.HdlPolicyTest#memoryIsRejectedByName()
	 * @jls.testedby jls.hdl.HdlPolicyTest#muxWithUnattachedSelectPassesInputZeroThrough()
	 * @jls.testedby jls.hdl.HdlPolicyTest#rejectionListsEveryOffenderInOneMessage()
	 * @jls.testedby jls.hdl.HdlPolicyTest#simulationControlElementsAreAllSkipped()
	 * @jls.testedby jls.hdl.HdlPolicyTest#subCircuitIsRejectedCleanly()
	 * @jls.testedby jls.hdl.HdlPolicyTest#unconnectedOutputPinWarnsAndLeavesThePortUndriven()
	 * @jls.testedby jls.hdl.VerilogExportGoldenTest#assertGolden()
	 * @jls.testedby jls.hdl.VhdlEmitterPolicyTest#export()
	 * @jls.testedby jls.hdl.VhdlEmitterPolicyTest#verilogAndVhdlShareOneModelWalk()
	 * @jls.testedby jls.hdl.VhdlExportGoldenTest#assertGolden()
	 */
	public static Result export(Circuit circ, HdlEmitter emitter)
			throws HdlExportException {

		HdlModel model = buildModel(circ);
		return new Result(emitter.emit(model), model.warnings());
	} // end of export method

	/**
	 * Walk the circuit into the language-neutral model.
	 *
	 * @param circ The circuit (already through finishLoad).
	 *
	 * @return the model.
	 *
	 * @throws HdlExportException on inexpressible elements.
	 *
	 * @jls.testedby jls.hdl.HdlPolicyTest#twoNetsBridgedByJumpsBecomeOneVerilogNet()
	 */
	public static HdlModel buildModel(Circuit circ)
			throws HdlExportException {

		List<Element> ordered = orderedElements(circ);

		// ---- element policy, applied before anything is built ----
		List<String> offenders = new ArrayList<String>();
		List<String> skipped = new ArrayList<String>();
		List<Element> exported = new ArrayList<Element>();
		for (Element el : ordered) {
			if (isTopology(el)) {
				continue;
			}
			if (isSkipped(el)) {
				skipped.add(describe(el));
				continue;
			}
			if (isExported(el)) {
				exported.add(el);
				continue;
			}
			offenders.add(describe(el));
		}
		if (!offenders.isEmpty()) {
			throw new HdlExportException("circuit \"" + circ.getName()
					+ "\" contains elements HDL export does not support"
					+ " yet: " + String.join("; ", offenders));
		}

		HdlNames names = new HdlNames();
		String circuitName =
				circ.getName() == null ? "circuit" : circ.getName();
		String moduleName = names.reserve(circuitName);
		HdlModel model = new HdlModel(moduleName, circuitName,
				JLSInfo.versionString);
		for (String skip : skipped) {
			model.addWarning(skip + " has no HDL meaning; skipped");
		}

		// ---- net walk: wire nets unioned across same-named jumps ----
		UnionFind nets = new UnionFind();
		for (Element el : ordered) {
			if (el instanceof WireEnd) {
				nets.register(((WireEnd) el).getNet());
			}
		}
		Map<String, List<WireNet>> jumpNets =
				new TreeMap<String, List<WireNet>>();
		for (Element el : ordered) {
			String alias = jumpAlias(el);
			if (alias == null) {
				continue;
			}
			for (Put put : ((LogicElement) el).getAllPuts()) {
				if (put.isAttached()) {
					List<WireNet> list = jumpNets.get(alias);
					if (list == null) {
						list = new ArrayList<WireNet>();
						jumpNets.put(alias, list);
					}
					list.add(put.getWireEnd().getNet());
				}
			}
		}
		for (Map.Entry<String, List<WireNet>> entry : jumpNets.entrySet()) {
			List<WireNet> list = entry.getValue();
			for (int i = 1; i < list.size(); i += 1) {
				nets.union(list.get(0), list.get(i));
			}
		}

		Map<WireNet, Group> groups = new IdentityHashMap<WireNet, Group>();
		for (WireNet net : nets.all()) {
			WireNet root = nets.find(net);
			Group group = groups.get(root);
			if (group == null) {
				group = new Group();
				groups.put(root, group);
			}
			group.bits = Math.max(group.bits, net.getBits());
		}

		// ---- naming, most-user-visible name first ----

		// input pins and clocks become ports that ARE their nets
		List<InputPin> inputPins = byName(ordered, InputPin.class);
		List<OutputPin> outputPins = byName(ordered, OutputPin.class);
		Map<Element, String> portNames = new IdentityHashMap<Element, String>();
		for (InputPin pin : inputPins) {
			String port = names.reserve(pin.getName());
			portNames.put(pin, port);
			Group group = groupOf(pin.getOutputList().get(0), nets, groups);
			if (group != null) {
				group.name = port;
				group.isPort = true;
			}
			model.addPort(new HdlModel.Port(port, HdlModel.Direction.INPUT,
					pin.getBits(), "InputPin \"" + pin.getName() + "\""));
		}
		for (Element el : exported) {
			if (!(el instanceof Clock)) {
				continue;
			}
			Clock clock = (Clock) el;
			String port = names.synth("clk");
			portNames.put(clock, port);
			Group group = groupOf(clock.getOutputList().get(0), nets, groups);
			if (group != null) {
				group.name = port;
				group.isPort = true;
			}
			model.addPort(new HdlModel.Port(port, HdlModel.Direction.INPUT, 1,
					"JLS Clock at " + at(clock) + ", cycle "
							+ clock.getCycleTime() + " units (high "
							+ clock.getOneTime()
							+ "); drive this input from the testbench"));
		}
		for (OutputPin pin : outputPins) {
			String port = names.reserve(pin.getName());
			portNames.put(pin, port);
			model.addPort(new HdlModel.Port(port, HdlModel.Direction.OUTPUT,
					pin.getBits(), "OutputPin \"" + pin.getName() + "\""));
		}

		// jump names label their (fused) nets
		Map<Group, TreeSet<String>> aliases =
				new IdentityHashMap<Group, TreeSet<String>>();
		for (Map.Entry<String, List<WireNet>> entry : jumpNets.entrySet()) {
			for (WireNet net : entry.getValue()) {
				Group group = groups.get(nets.find(net));
				TreeSet<String> set = aliases.get(group);
				if (set == null) {
					set = new TreeSet<String>();
					aliases.put(group, set);
				}
				set.add(entry.getKey());
			}
		}
		List<Group> aliased = new ArrayList<Group>(aliases.keySet());
		aliased.sort(Comparator.comparing(g -> aliases.get(g).first()));
		for (Group group : aliased) {
			if (group.name == null) {
				group.name = names.reserve(aliases.get(group).first());
			}
		}

		// named registers label their q/notQ nets
		for (Element el : exported) {
			if (!(el instanceof Register)) {
				continue;
			}
			Register reg = (Register) el;
			String name = reg.getName();
			if (name == null || name.isEmpty()) {
				continue;
			}
			Group q = groupOf(reg.getOutputList().get(0), nets, groups);
			if (q != null && q.name == null) {
				q.name = names.synth(name + "_q");
			}
			Group nq = groupOf(reg.getOutputList().get(1), nets, groups);
			if (nq != null && nq.name == null) {
				nq.name = names.synth(name + "_nq");
			}
		}

		// everything else driven gets net_<id> from its driver
		for (Element el : exported) {
			LogicElement logic = (LogicElement) el;
			List<Output> outs = logic.getOutputList();
			for (int k = 0; k < outs.size(); k += 1) {
				Group group = groupOf(outs.get(k), nets, groups);
				if (group != null && group.name == null) {
					group.name = names.synth("net_" + el.getID()
							+ (outs.size() > 1 ? "_" + k : ""));
				}
			}
		}

		// undriven but read nets (inputs only) get net_u<id>
		List<Group> undriven = new ArrayList<Group>();
		Map<Group, Integer> undrivenId = new IdentityHashMap<Group, Integer>();
		Map<Group, String> undrivenKey = new IdentityHashMap<Group, String>();
		for (Element el : ordered) {
			if (!(el instanceof LogicElement) || isTopology(el)) {
				continue;
			}
			LogicElement logic = (LogicElement) el;
			for (Put put : logic.getAllPuts()) {
				Group group = groupOf(put, nets, groups);
				if (group == null || group.name != null
						|| undrivenKey.containsKey(group)) {
					continue;
				}
				undriven.add(group);
				undrivenId.put(group, el.getID());
				undrivenKey.put(group, String.format("%09d_%s",
						el.getID(), put.getName()));
			}
		}
		undriven.sort(Comparator.comparing(undrivenKey::get));
		for (Group group : undriven) {
			group.name = names.synth("net_u" + undrivenId.get(group));
		}

		// declare the internal (non-port) nets, sorted by name
		TreeMap<String, Integer> declared = new TreeMap<String, Integer>();
		for (Group group : groups.values()) {
			if (group.name != null && !group.isPort) {
				declared.put(group.name, Math.max(group.bits, 1));
			}
		}
		for (Map.Entry<String, Integer> decl : declared.entrySet()) {
			model.addNet(decl.getKey(), decl.getValue());
		}

		// ---- statements, in element save-id order ----
		for (Element el : exported) {
			buildStatement(model, el, portNames, nets, groups, names);
		}

		for (Map.Entry<String, String> rename : names.renames().entrySet()) {
			model.addRename(rename.getKey(), rename.getValue());
		}
		return model;
	} // end of buildModel method

	// ------------------------------------------------------------------
	// policy
	// ------------------------------------------------------------------

	/** Element classes with a direct HDL rendering. */
	private static final Set<Class<?>> EXPORTED = Set.of(
			InputPin.class, OutputPin.class, Constant.class,
			AndGate.class, OrGate.class, NandGate.class, NorGate.class,
			XorGate.class, NotGate.class, DelayGate.class, Extend.class,
			TriState.class, Adder.class, Register.class, Clock.class,
			Binder.class, Splitter.class, Mux.class, Decoder.class);

	/** Element classes with no HDL meaning, warn-and-skipped. */
	private static final Set<Class<?>> SKIPPED = Set.of(
			Display.class, SigGen.class, Pause.class, Stop.class,
			Text.class, TestGen.class);

	/** Element classes that are net topology, not instances. */
	private static final Set<Class<?>> TOPOLOGY = Set.of(
			Wire.class, WireEnd.class, JumpStart.class, JumpEnd.class);

	/**
	 * Tests the exported policy bucket.
	 * @param el The element to classify.
	 * @return true if the element has a direct HDL rendering.
	 */
	private static boolean isExported(Element el) {
		return EXPORTED.contains(el.getClass());
	}

	/**
	 * Tests the warn-and-skip policy bucket.
	 * @param el The element to classify.
	 * @return true if the element has no HDL meaning and is warn-and-skipped.
	 */
	private static boolean isSkipped(Element el) {
		return SKIPPED.contains(el.getClass());
	}

	/**
	 * Tests the net-topology policy bucket.
	 * @param el The element to classify.
	 * @return true if the element is net topology (wire/jump), not an instance.
	 */
	private static boolean isTopology(Element el) {
		return TOPOLOGY.contains(el.getClass());
	}

	// ------------------------------------------------------------------
	// per-element statement construction
	// ------------------------------------------------------------------

	/**
	 * Append the model statement(s) for one exported element, dispatching
	 * on its type to the matching HDL template.
	 *
	 * @param model The model being built; receives the statement(s).
	 * @param el The exported element.
	 * @param portNames Element-to-port-name map for pins and clocks.
	 * @param nets Union-find over the circuit's fused wire nets.
	 * @param groups Net-root-to-group map holding chosen net names.
	 * @param names Identifier allocator for any synthesized nets.
	 */
	private static void buildStatement(HdlModel model, Element el,
			Map<Element, String> portNames, UnionFind nets,
			Map<WireNet, Group> groups, HdlNames names) {

		LogicElement logic = (LogicElement) el;
		List<Input> ins = logic.getInputList();
		List<Output> outs = logic.getOutputList();

		if (el instanceof InputPin || el instanceof Clock) {
			return; // ports drive their nets directly
		}

		if (el instanceof OutputPin) {
			OutputPin pin = (OutputPin) el;
			if (!ins.get(0).isAttached()) {
				model.addWarning(describe(el)
						+ " is not connected; its port is left undriven");
				return;
			}
			model.addStatement(new HdlModel.GateStatement(
					"OutputPin \"" + pin.getName() + "\"",
					HdlModel.GateStatement.Op.BUFFER,
					List.of(operand(ins.get(0), nets, groups)),
					portNames.get(el)));
			return;
		}

		if (el instanceof Constant) {
			Constant con = (Constant) el;
			Group group = groupOf(outs.get(0), nets, groups);
			int bits = group == null ? 1 : Math.max(group.bits, 1);
			String target = target(model, el, 0, outs.get(0), bits, nets,
					groups, names);
			model.addStatement(new HdlModel.ConstantStatement(
					"Constant " + con.getValue() + " at " + at(el),
					target, bits, con.getValue()));
			return;
		}

		HdlModel.GateStatement.Op op = gateOp(el);
		if (op != null) {
			int bits = Math.max(outs.get(0).getBits(), 1);
			String target = target(model, el, 0, outs.get(0), bits, nets,
					groups, names);
			List<HdlModel.Operand> operands =
					new ArrayList<HdlModel.Operand>();
			for (Input in : ins) {
				operands.add(operand(in, nets, groups));
			}
			model.addStatement(new HdlModel.GateStatement(
					el.getClass().getSimpleName() + " at " + at(el), op,
					operands, target));
			return;
		}

		if (el instanceof Extend) {
			int bits = Math.max(outs.get(0).getBits(), 1);
			String target = target(model, el, 0, outs.get(0), bits, nets,
					groups, names);
			model.addStatement(new HdlModel.ReplicateStatement(
					"Extend (1 bit to " + bits + ") at " + at(el),
					operand(ins.get(0), nets, groups), target, bits));
			return;
		}

		if (el instanceof TriState) {
			int bits = Math.max(outs.get(0).getBits(), 1);
			String target = target(model, el, 0, outs.get(0), bits, nets,
					groups, names);
			model.addStatement(new HdlModel.TriStateStatement(
					"TriState at " + at(el),
					operand(ins.get(0), nets, groups),
					operand(ins.get(1), nets, groups), target, bits));
			return;
		}

		if (el instanceof Adder) {
			int bits = Math.max(outs.get(0).getBits(), 1);
			String sum = target(model, el, 0, outs.get(0), bits, nets,
					groups, names);
			String carry = target(model, el, 1, outs.get(1), 1, nets,
					groups, names);
			model.addStatement(new HdlModel.AdderStatement(
					"Adder at " + at(el),
					operand(ins.get(0), nets, groups),
					operand(ins.get(1), nets, groups),
					operand(ins.get(2), nets, groups), sum, carry, bits));
			return;
		}

		if (el instanceof Register) {
			Register reg = (Register) el;
			int bits = Math.max(reg.getBits(), 1);
			String q = target(model, el, 0, outs.get(0), bits, nets,
					groups, names);
			String notQ = target(model, el, 1, outs.get(1), bits, nets,
					groups, names);
			String regName = names.synth(reg.getName().isEmpty()
					? "reg_" + el.getID() : reg.getName());
			HdlModel.Operand clock = operand(ins.get(1), nets, groups);
			if (!clock.isNet()) {
				model.addWarning(describe(el) + " has no clock connected;"
						+ " it holds its initial value");
			}
			HdlModel.RegisterStatement.Kind kind;
			String kindText;
			switch (reg.getTypeName()) {
			case "pff":
				kind = HdlModel.RegisterStatement.Kind.POS_EDGE;
				kindText = "positive-edge flip-flop";
				break;
			case "nff":
				kind = HdlModel.RegisterStatement.Kind.NEG_EDGE;
				kindText = "negative-edge flip-flop";
				break;
			default:
				kind = HdlModel.RegisterStatement.Kind.LATCH;
				kindText = "transparent latch";
				break;
			}
			model.addStatement(new HdlModel.RegisterStatement(
					"Register \"" + reg.getName() + "\": " + bits + "-bit "
							+ kindText + ", initial value "
							+ reg.getInitialValue(),
					kind, regName, operand(ins.get(0), nets, groups), clock,
					q, notQ, bits, reg.getInitialValue()));
			return;
		}

		if (el instanceof Binder) {
			Binder binder = (Binder) el;
			List<int[]> ranges = binder.getRangeIndices();
			int outBits = Math.max(outs.get(0).getBits(), 1);
			String target = target(model, el, 0, outs.get(0), outBits, nets,
					groups, names);
			for (int k = 0; k < ranges.size(); k += 1) {
				int[] toBits = ranges.get(k);
				model.addStatement(new HdlModel.BitMapStatement(
						k == 0 ? "Binder at " + at(el) : null,
						operand(ins.get(k), nets, groups),
						ascending(toBits.length), target, outBits, toBits));
			}
			return;
		}

		if (el instanceof Splitter) {
			Splitter splitter = (Splitter) el;
			List<int[]> ranges = splitter.getRangeIndices();
			HdlModel.Operand source = operand(ins.get(0), nets, groups);
			for (int k = 0; k < ranges.size(); k += 1) {
				int[] fromBits = ranges.get(k);
				int outBits = Math.max(outs.get(k).getBits(), 1);
				String target = target(model, el, k, outs.get(k), outBits,
						nets, groups, names);
				model.addStatement(new HdlModel.BitMapStatement(
						k == 0 ? "Splitter at " + at(el) : null, source,
						fromBits, target, outBits,
						ascending(fromBits.length)));
			}
			return;
		}

		if (el instanceof Mux) {
			int bits = Math.max(outs.get(0).getBits(), 1);
			String target = target(model, el, 0, outs.get(0), bits, nets,
					groups, names);
			HdlModel.Operand select = operand(ins.get(0), nets, groups);
			List<HdlModel.Operand> values =
					new ArrayList<HdlModel.Operand>();
			for (int k = 1; k < ins.size(); k += 1) {
				values.add(operand(ins.get(k), nets, groups));
			}
			int ways = values.size();
			if (!select.isNet()) {
				// an unattached select reads 0 (JLS's absent-inputs rule),
				// so the mux degenerates to a buffer of input0
				model.addStatement(new HdlModel.GateStatement(
						"Mux (" + ways + " inputs) at " + at(el)
								+ "; select unattached, reads 0",
						HdlModel.GateStatement.Op.BUFFER,
						List.of(values.get(0)), target));
				return;
			}
			model.addStatement(new HdlModel.SelectStatement(
					"Mux (" + ways + " inputs) at " + at(el), select, values,
					HdlModel.Operand.literal(BigInteger.ZERO, bits), target,
					bits, names.synth("mux_" + el.getID())));
			return;
		}

		if (el instanceof Decoder) {
			int outBits = Math.max(outs.get(0).getBits(), 1);
			int inBits = Math.max(ins.get(0).getBits(), 1);
			String target = target(model, el, 0, outs.get(0), outBits, nets,
					groups, names);
			HdlModel.Operand select = operand(ins.get(0), nets, groups);
			String what = "Decoder (" + inBits + " to " + outBits + ") at "
					+ at(el);
			if (!select.isNet()) {
				// an unattached input reads 0 (JLS's absent-inputs rule),
				// so exactly output bit 0 is set, constantly
				model.addStatement(new HdlModel.ConstantStatement(
						what + "; input unattached, reads 0", target, outBits,
						BigInteger.ONE));
				return;
			}
			List<HdlModel.Operand> values =
					new ArrayList<HdlModel.Operand>();
			for (int k = 0; k < outBits; k += 1) {
				values.add(HdlModel.Operand.literal(
						BigInteger.ONE.shiftLeft(k), outBits));
			}
			model.addStatement(new HdlModel.SelectStatement(what, select,
					values, HdlModel.Operand.literal(BigInteger.ZERO, outBits),
					target, outBits, names.synth("dec_" + el.getID())));
			return;
		}

		// unreachable: the policy pass admits only the classes above
		throw new IllegalStateException(
				"no template for " + el.getClass().getName());
	} // end of buildStatement method

	/**
	 * Map a gate-family element to its HDL operation. The switch is
	 * exhaustive over the sealed {@link Gate} hierarchy (#95): a new gate
	 * kind fails compilation here instead of silently exporting nothing.
	 *
	 * @param el The element to map.
	 * @return the gate operation for a gate-family element, or null if the
	 * element is not a boolean gate ({@link Extend} is gate-family but has
	 * its own bit-extension template, handled before the gate branch).
	 */
	private static HdlModel.GateStatement.Op gateOp(Element el) {

		if (!(el instanceof Gate gate)) {
			return null;
		}
		return switch (gate) {
		case AndGate _ -> HdlModel.GateStatement.Op.AND;
		case OrGate _ -> HdlModel.GateStatement.Op.OR;
		case NandGate _ -> HdlModel.GateStatement.Op.NAND;
		case NorGate _ -> HdlModel.GateStatement.Op.NOR;
		case XorGate _ -> HdlModel.GateStatement.Op.XOR;
		case NotGate _ -> HdlModel.GateStatement.Op.NOT;
		case DelayGate _ -> HdlModel.GateStatement.Op.BUFFER;
		case Extend _ -> null;
		};
	} // end of gateOp method

	// ------------------------------------------------------------------
	// net-walk plumbing
	// ------------------------------------------------------------------

	/** One fused electrical net (wire nets joined by jump aliases). */
	private static final class Group {

		/** Chosen HDL net name; null until the naming pass assigns one. */
		String name = null;
		/** Width in bits: the widest member wire net seen so far. */
		int bits = 0;
		/** True when the net is a module port, not an internal net. */
		boolean isPort = false;

		/** Creates an unnamed, zero-width, non-port group. */
		Group() {
		} // end of Group constructor
	} // end of Group class

	/** Union-find over WireNet identity. */
	private static final class UnionFind {

		/** Each net's parent in its set's tree; roots map to themselves. */
		private final Map<WireNet, WireNet> parent =
				new IdentityHashMap<WireNet, WireNet>();
		/** All registered nets, in first-seen order. */
		private final List<WireNet> order = new ArrayList<WireNet>();

		/** Creates an empty forest with no registered nets. */
		UnionFind() {
		} // end of UnionFind constructor

		/**
		 * Add a net as its own singleton set, preserving first-seen order.
		 *
		 * @param net The net to track; null and duplicates are ignored.
		 */
		void register(WireNet net) {
			if (net != null && !parent.containsKey(net)) {
				parent.put(net, net);
				order.add(net);
			}
		}

		/**
		 * The canonical representative of the net's set, path-compressing
		 * along the way; registers the net first if unseen.
		 *
		 * @param net The net to look up.
		 * @return the set's root net.
		 */
		WireNet find(WireNet net) {
			register(net);
			WireNet root = net;
			while (parent.get(root) != root) {
				root = parent.get(root);
			}
			WireNet walk = net;
			while (parent.get(walk) != root) {
				WireNet next = parent.get(walk);
				parent.put(walk, root);
				walk = next;
			}
			return root;
		}

		/**
		 * Merge the two nets' sets into one.
		 *
		 * @param a One net.
		 * @param b The other net.
		 */
		void union(WireNet a, WireNet b) {
			parent.put(find(a), find(b));
		}

		/**
		 * Gives every net this forest has seen.
		 * @return all registered nets in first-seen order.
		 */
		List<WireNet> all() {
			return order;
		}
	} // end of UnionFind class

	/**
	 * The fused-net group a put connects to, creating the group on first
	 * touch.
	 *
	 * @param put The element put (input or output).
	 * @param nets Union-find over the circuit's wire nets.
	 * @param groups Net-root-to-group map.
	 * @return the group, or null if the put is unattached or has no net.
	 */
	private static Group groupOf(Put put, UnionFind nets,
			Map<WireNet, Group> groups) {

		if (put == null || !put.isAttached()) {
			return null;
		}
		WireNet net = put.getWireEnd().getNet();
		if (net == null) {
			return null;
		}
		WireNet root = nets.find(net);
		Group group = groups.get(root);
		if (group == null) {
			group = new Group();
			groups.put(root, group);
		}
		return group;
	} // end of groupOf method

	/**
	 * The operand an element input reads: its fused net if attached,
	 * else a zero literal of the put's width (JLS reads unattached
	 * inputs as 0).
	 *
	 * @param in The element input to resolve.
	 * @param nets Union-find over the circuit's wire nets.
	 * @param groups Net-root-to-group map holding chosen net names.
	 * @return the net-reference or zero-literal operand.
	 */
	private static HdlModel.Operand operand(Input in, UnionFind nets,
			Map<WireNet, Group> groups) {

		Group group = groupOf(in, nets, groups);
		if (group == null || group.name == null) {
			return HdlModel.Operand.literal(BigInteger.ZERO,
					Math.max(in.getBits(), 1));
		}
		return HdlModel.Operand.net(group.name, Math.max(group.bits, 1));
	} // end of operand method

	/**
	 * The net an element output drives: its fused net if attached,
	 * else a freshly declared dangling wire (unc_&lt;id&gt;_&lt;k&gt;)
	 * so every template stays uniform.
	 *
	 * @param model The model being built; receives any dangling net.
	 * @param el The driving element.
	 * @param k The output's index on the element.
	 * @param out The element output to resolve.
	 * @param bits Width of any dangling net to declare.
	 * @param nets Union-find over the circuit's wire nets.
	 * @param groups Net-root-to-group map holding chosen net names.
	 * @param names Identifier allocator for the dangling net.
	 * @return the name of the net the output drives.
	 */
	private static String target(HdlModel model, Element el, int k,
			Output out, int bits, UnionFind nets, Map<WireNet, Group> groups,
			HdlNames names) {

		Group group = groupOf(out, nets, groups);
		if (group != null && group.name != null) {
			return group.name;
		}
		String dangling = names.synth("unc_" + el.getID() + "_" + k);
		model.addNet(dangling, bits);
		return dangling;
	} // end of target method

	/**
	 * Gives the aliasing name a jump element contributes.
	 * @param el The element to inspect.
	 * @return the jump name if the element is a JumpStart or JumpEnd (nets
	 * sharing a name are fused), else null.
	 */
	private static String jumpAlias(Element el) {

		if (el instanceof JumpStart) {
			return ((JumpStart) el).getName();
		}
		if (el instanceof JumpEnd) {
			return ((JumpEnd) el).getName();
		}
		return null;
	} // end of jumpAlias method

	// ------------------------------------------------------------------
	// deterministic ordering and diagnostics
	// ------------------------------------------------------------------

	/**
	 * All circuit elements in a stable order: save id first (unique in
	 * every saved file), then class, name and position as tie-breaks
	 * for programmatically built circuits.
	 *
	 * @param circ The circuit whose elements to order.
	 * @return the elements, stably sorted.
	 */
	private static List<Element> orderedElements(Circuit circ) {

		List<Element> ordered = new ArrayList<Element>(circ.getElements());
		ordered.sort(Comparator
				.comparingInt(Element::getID)
				.thenComparing(el -> el.getClass().getName())
				.thenComparing(el -> el.getName() == null ? "" : el.getName())
				.thenComparingInt(Element::getX)
				.thenComparingInt(Element::getY));
		return ordered;
	} // end of orderedElements method

	/**
	 * All elements of the given type, sorted by name for deterministic port
	 * ordering.
	 *
	 * @param <T> The element subtype to keep.
	 * @param ordered The elements to filter.
	 * @param type The element subtype to keep.
	 * @return the matching elements, name-sorted.
	 */
	private static <T extends LogicElement> List<T> byName(
			List<Element> ordered, Class<T> type) {

		List<T> matches = new ArrayList<T>();
		for (Element el : ordered) {
			if (type.isInstance(el)) {
				matches.add(type.cast(el));
			}
		}
		matches.sort(Comparator.comparing(
				el -> el.getName() == null ? "" : el.getName()));
		return matches;
	} // end of byName method

	/**
	 * Builds the identity bit-index mapping.
	 * @param n The length.
	 * @return the identity index array {@code {0, 1, ..., n-1}}.
	 */
	private static int[] ascending(int n) {

		int[] a = new int[n];
		for (int i = 0; i < n; i += 1) {
			a[i] = i;
		}
		return a;
	} // end of ascending method

	/**
	 * Describes an element for diagnostics.
	 * @param el The element to describe.
	 * @return its type, name (when the element has one) and grid location.
	 */
	static String describe(Element el) {

		StringBuilder sb = new StringBuilder(el.getClass().getSimpleName());
		String name = el.getName();
		if (name != null && !name.isEmpty()) {
			sb.append(" \"").append(name).append('"');
		}
		sb.append(" at ").append(at(el));
		return sb.toString();
	} // end of describe method

	/**
	 * Formats an element's position for diagnostics.
	 * @param el The element.
	 * @return its grid location as {@code "(x,y)"} for diagnostics.
	 */
	private static String at(Element el) {

		return "(" + el.getX() + "," + el.getY() + ")";
	} // end of at method

} // end of HdlExporter class
