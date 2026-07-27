package jls.collab.op;

import java.awt.Graphics;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import jls.Circuit;
import jls.core.Geometry;
import jls.elem.Element;
import jls.elem.ElementId;
import jls.elem.Input;
import jls.elem.Output;
import jls.elem.Put;
import jls.elem.TriProp;
import jls.elem.Wire;
import jls.elem.WireEnd;
import jls.elem.WireNet;

/**
 * Add one whole wire net to a circuit from its serialized wire-end
 * blocks (issue #167): the ends, the segments between them, the put
 * attachments, and the probes, reconstructed by replaying the file
 * loader's exact algorithm so an added net is indistinguishable from a
 * loaded one ({@link NetBlocks}). Inverse: {@link RemoveWire} addressed
 * at any of the net's ends.
 *
 * Cross-references inside the blocks use the op's local numbering; the
 * elements the net attaches to travel as stable ids (issue #165) in
 * {@link #attach}, index-aligned with local ids {@code 0..k-1}, so the
 * op means the same thing on a restored or replicated circuit.
 *
 * Validation is atomic and strict: the blocks must be exactly what a
 * save writes, form exactly one connected net whose ends' stable ids
 * are fresh, list every wire (and its probe) from both of its ends, and
 * attach only to existing, unattached puts of the anchors the op
 * carries. Nothing is added unless every check passes.
 *
 * @param attach The stable ids of the attachment anchors, in stable-id
 *            order; index {@code i} is local id {@code i} inside the
 *            blocks. Empty for a dangling net.
 * @param blocks The serialized wire-end blocks, in stable-id order;
 *            block {@code i} declares local id {@code attach.size() +
 *            i}.
 */
public record AddWire(List<ElementId> attach, List<String> blocks)
		implements CircuitOp {

	/**
	 * Defensive copy: the record's lists are immutable whatever the
	 * caller passed (issue #94 discipline).
	 */
	public AddWire {

		attach = List.copyOf(attach);
		blocks = List.copyOf(blocks);
	} // end of compact constructor

	/**
	 * Serialize one surviving connected component of a clipped net
	 * (issue #167): the delete gesture removes a partially selected net
	 * with {@link RemoveWire} of the whole net and re-adds each
	 * surviving connected component through this factory. The blocks
	 * are {@code WireEnd.save}'s exact format filtered to the surviving
	 * subgraph: only wires in the kept set (and their probes) are
	 * referenced, attachments to elements in the removed set are
	 * dropped (those elements leave in the gesture's
	 * {@link RemoveElements}), and the tri-state flag is recomputed for
	 * the component alone, mirroring {@code WireNet.makeNet}.
	 *
	 * @param ends The component's wire ends, in any order; each must
	 *            keep at least one wire.
	 * @param keptWires The surviving wires; every kept wire of the
	 *            given ends must connect two of them.
	 * @param removedAnchors The elements leaving in the same gesture;
	 *            attachments to them are not serialized.
	 *
	 * @return the op that adds the surviving component.
	 *
	 * @throws IllegalArgumentException if the arguments do not describe
	 *             one nonempty surviving component - a planner bug, not
	 *             hostile input, hence unchecked.
	 */
	public static AddWire survivors(List<WireEnd> ends,
			Set<Wire> keptWires, Set<Element> removedAnchors) {

		return NetBlocks.toSurvivorAddWire(ends, keptWires,
				removedAnchors);
	} // end of survivors method

	/**
	 * The vetted form of the op: the resolved anchors and parsed end
	 * blocks validation produced.
	 *
	 * @param anchors The resolved anchor elements, in attach order.
	 * @param specs The parsed end blocks, in block order.
	 */
	private record Plan(List<Element> anchors,
			List<NetBlocks.EndSpec> specs) {
	} // end of Plan record

	@Override
	public void apply(Circuit circuit, Graphics g) throws OpRejected {

		Plan plan = validate(circuit);
		int base = attach.size();

		// construct the ends the way the loader does: coordinates and
		// stable id from the block, the fixed wire-end size, and a
		// fixPosition to sync the non-snap position (WireEnd.init)
		List<WireEnd> ends =
				new ArrayList<WireEnd>(plan.specs().size());
		for (NetBlocks.EndSpec spec : plan.specs()) {
			WireEnd end = new WireEnd(circuit);
			end.setValue("x", spec.x());
			end.setValue("y", spec.y());
			end.setValue("width", Geometry.POINT_DIAMETER);
			end.setValue("height", Geometry.POINT_DIAMETER);
			end.setValue("sid", spec.sid().toString());
			end.fixPosition();
			ends.add(end);
		}

		// attach puts (validation proved each exists and is free)
		for (int i = 0; i < ends.size(); i += 1) {
			NetBlocks.EndSpec spec = plan.specs().get(i);
			if (spec.attach() < 0) {
				continue;
			}
			Put p = Objects.requireNonNull(plan.anchors()
					.get(spec.attach())
					.getPut(Objects.requireNonNull(spec.putName())));
			p.setAttached(ends.get(i));
			ends.get(i).setPut(p);
		}

		// create wires in block order, each end's refs in save order -
		// the exact construction order WireEnd.init and finishLoad use,
		// so the rebuilt wires-per-end ordering (and with it every
		// future save's ref lines) matches a loaded circuit's
		for (int i = 0; i < ends.size(); i += 1) {
			WireEnd end = ends.get(i);
			NetBlocks.EndSpec spec = plan.specs().get(i);
			for (int ref : spec.wireRefs()) {
				WireEnd other = ends.get(ref - base);
				if (wireBetween(end, other) != null) {
					continue;
				}
				Wire wire = new Wire(end, other);
				end.addWire(wire);
				other.addWire(wire);
				circuit.addElement(wire);
				String probe = spec.probes().get(ref);
				if (probe != null) {
					wire.attachProbe(probe);
				}
			}
			circuit.addElement(end);
		}

		// build the wire net, mirroring Circuit.finishLoad's partition
		Map<WireEnd, NetBlocks.EndSpec> specOf =
				new HashMap<WireEnd, NetBlocks.EndSpec>();
		for (int i = 0; i < ends.size(); i += 1) {
			specOf.put(ends.get(i), plan.specs().get(i));
		}
		LinkedList<WireEnd> visit = new LinkedList<WireEnd>();
		visit.add(ends.get(0));
		WireNet net = new WireNet();
		Set<WireEnd> visited = new HashSet<WireEnd>();
		while (!visit.isEmpty()) {
			WireEnd vend = visit.remove();
			visited.add(vend);
			net.add(vend);
			vend.setNet(net);
			if (Objects.requireNonNull(specOf.get(vend)).triState()) {
				net.loadTriState();
			}
			Put vendPut = vend.getPut();
			if (vendPut != null) {
				net.setBits(vendPut.getBits());
				if (vendPut instanceof Output) {
					net.setInput();
				}
			}
			for (Wire wire : vend.getWires()) {
				WireEnd otherEnd = wire.getOtherEnd(vend);
				if (!visited.contains(otherEnd)) {
					visit.add(otherEnd);
					net.add(wire);
					wire.setNet(net);
				}
			}
		}

		// a tri-state net re-arms the tri-state property of the input
		// pins it drives, mirroring the editor's connect() - and exactly
		// undoing what removing the net cleared
		if (net.isTriState()) {
			for (WireEnd end : ends) {
				Put p = end.getPut();
				if (p instanceof Input
						&& p.getElement() instanceof TriProp pin) {
					pin.setTriState(true);
				}
			}
		}
	} // end of apply method

	@Override
	public CircuitOp invert(Circuit before) throws OpRejected {

		Plan plan = validate(before);
		return new RemoveWire(plan.specs().get(0).sid());
	} // end of invert method

	/**
	 * Vet the whole op against the target circuit without touching it.
	 *
	 * @param target The circuit the op would mutate.
	 *
	 * @return the resolved anchors and parsed blocks.
	 *
	 * @throws OpRejected if any check fails; the target is untouched
	 *             either way.
	 */
	private Plan validate(Circuit target) throws OpRejected {

		if (blocks.isEmpty()) {
			throw new OpRejected(
					"a wire add needs at least one wire-end block");
		}

		// anchors: existing put-bearing elements, each listed once, in
		// stable-id order (the canonical order the inverse writes)
		List<Element> anchors = new ArrayList<Element>(attach.size());
		for (int i = 0; i < attach.size(); i += 1) {
			if (i > 0 && attach.get(i - 1)
					.compareTo(attach.get(i)) >= 0) {
				throw new OpRejected("attachment anchors must be listed "
						+ "once each, in stable-id order");
			}
			Element el = Ops.resolve(target, attach.get(i));
			if (el instanceof Wire || el instanceof WireEnd) {
				throw new OpRejected("an attachment anchor must be a "
						+ "put-bearing element, not a wire");
			}
			anchors.add(el);
		}

		int base = attach.size();
		List<NetBlocks.EndSpec> specs =
				new ArrayList<NetBlocks.EndSpec>(blocks.size());
		for (int i = 0; i < blocks.size(); i += 1) {
			specs.add(NetBlocks.parseEnd(blocks.get(i), base,
					blocks.size(), i));
		}

		// end stable ids: canonical order (which also rules out
		// duplicates) and fresh in the target circuit
		for (int i = 1; i < specs.size(); i += 1) {
			if (specs.get(i - 1).sid()
					.compareTo(specs.get(i).sid()) >= 0) {
				throw new OpRejected("wire-end blocks must arrive in "
						+ "stable-id order");
			}
		}
		for (NetBlocks.EndSpec spec : specs) {
			for (Element existing : target.getElements()) {
				if (existing.getStableId().equals(spec.sid())) {
					throw new OpRejected("the circuit already has an "
							+ "element with stable id '" + spec.sid()
							+ "'");
				}
			}
		}

		// every wire (and its probe) must be listed from both ends
		for (int i = 0; i < specs.size(); i += 1) {
			int self = base + i;
			NetBlocks.EndSpec spec = specs.get(i);
			for (int ref : spec.wireRefs()) {
				NetBlocks.EndSpec other = specs.get(ref - base);
				if (!other.wireRefs().contains(self)) {
					throw new OpRejected("a wire must be listed from "
							+ "both of its ends");
				}
				if (!Objects.equals(spec.probes().get(ref),
						other.probes().get(self))) {
					throw new OpRejected("a probed wire must carry the "
							+ "same probe on both of its ends");
				}
			}
		}

		// exactly one connected net (a multi-net add is several ops),
		// which also makes any end a valid address for the inverse
		Set<Integer> reached = new HashSet<Integer>();
		LinkedList<Integer> frontier = new LinkedList<Integer>();
		frontier.add(0);
		reached.add(0);
		while (!frontier.isEmpty()) {
			int i = frontier.remove();
			for (int ref : specs.get(i).wireRefs()) {
				if (reached.add(ref - base)) {
					frontier.add(ref - base);
				}
			}
		}
		if (reached.size() != specs.size()) {
			throw new OpRejected(
					"the blocks must form one connected net");
		}

		// the net's tri-state flag is per net, so every end must agree
		for (NetBlocks.EndSpec spec : specs) {
			if (spec.triState() != specs.get(0).triState()) {
				throw new OpRejected("every end of a net must agree on "
						+ "the net's tri-state flag");
			}
		}

		// attachments: the named put must exist, be free, and be
		// claimed by at most one arriving end; every anchor carried
		// must actually anchor something
		boolean[] anchorUsed = new boolean[base];
		Set<Put> claimed = new HashSet<Put>();
		for (NetBlocks.EndSpec spec : specs) {
			if (spec.attach() < 0) {
				continue;
			}
			anchorUsed[spec.attach()] = true;
			Element anchor = anchors.get(spec.attach());
			Put p = anchor
					.getPut(Objects.requireNonNull(spec.putName()));
			if (p == null) {
				throw new OpRejected("element '"
						+ anchor.getStableId() + "' has no put named '"
						+ spec.putName() + "'");
			}
			if (p.isAttached()) {
				throw new OpRejected("put '" + spec.putName()
						+ "' of element '" + anchor.getStableId()
						+ "' already has a wire attached");
			}
			if (!claimed.add(p)) {
				throw new OpRejected("two ends attach to put '"
						+ spec.putName() + "' of element '"
						+ anchor.getStableId() + "'");
			}
		}
		for (int i = 0; i < base; i += 1) {
			if (!anchorUsed[i]) {
				throw new OpRejected("anchor '" + attach.get(i)
						+ "' is carried but never attached to");
			}
		}

		return new Plan(anchors, specs);
	} // end of validate method

	/**
	 * The wire between two ends, if one exists already.
	 *
	 * @param a One end.
	 * @param b The other end.
	 *
	 * @return the wire, or null if the ends are not directly wired.
	 */
	private static @Nullable Wire wireBetween(WireEnd a, WireEnd b) {

		for (Wire w : a.getWires()) {
			if (w.getOtherEnd(a) == b) {
				return w;
			}
		}
		return null;
	} // end of wireBetween method

	@Override
	public void save(PrintWriter output) {

		output.println("OP AddWire");
		for (ElementId id : attach) {
			Ops.saveString(output, "id", id.toString());
		}
		for (String block : blocks) {
			Ops.saveString(output, "block", block);
		}
		output.println("END");
	} // end of save method

} // end of AddWire record
