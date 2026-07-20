/**
 * Realizes a validated Yosys netlist as an editable JLS circuit (issue
 * #61, Stage 2 of the #59 HDL path). {@link jls.hdl.imp.NetlistImporter}
 * is the cell-to-element mapper and plain-text save-format emitter: it
 * consumes the {@link jls.hdl.yosys.YosysNetlist} model, checks it
 * through the {@link jls.hdl.yosys.CellValidator}, builds a placement
 * problem for the auto-layout seam ({@link jls.hdl.layout}), solves it
 * with the {@link jls.hdl.layout.HeuristicLayeredLayouter}, and writes
 * the placed result as {@code ELEMENT} blocks and {@code WireEnd}
 * chains that load through JLS's real loader.
 *
 * <p>This increment realizes the directly-mappable combinational core -
 * module ports, {@code $not}/{@code $and}/{@code $or}/{@code $xor}, the
 * 2:1 {@code $mux}, and constant drivers, wired word-for-word - and
 * reports (never silently mis-maps) every construct it does not yet
 * build: the Adder, Register, TriState, reductions, {@code $bmux},
 * hierarchy instances, bit-level slices/concatenations, and width
 * mismatches. The File-&gt;Import UI, the progress/cancel surface, and
 * the wider cell coverage are later increments of the same issue.</p>
 */
package jls.hdl.imp;
