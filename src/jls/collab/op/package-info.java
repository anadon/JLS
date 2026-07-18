/**
 * The operation layer of the collaborative-editing program (issue #167,
 * collab Stage 0b): editor mutations reified as a closed vocabulary of
 * validated, invertible, serializable commands. The sealed
 * {@link jls.collab.op.CircuitOp} interface is the whole vocabulary -
 * data-only records addressing elements by stable id
 * ({@link jls.elem.ElementId}, issue #165) - and
 * {@link jls.collab.op.OpSink} is the single entry point mutations flow
 * through: the editor submits ops instead of mutating inline, and the
 * future replication layer (issue #163) observes the same entry point.
 * Every op validates before mutating (rejections throw
 * {@link jls.collab.op.OpRejected} leaving the circuit untouched),
 * computes an exact inverse against the pre-apply circuit (the
 * canonical-serialization oracle of issue #166 verifies restoration to
 * the byte), and round-trips through the strict
 * {@link jls.collab.op.CircuitOpReader}, which rejects rather than
 * repairs malformed input - this grammar is the future network surface
 * (issue #170). No class here may depend on Swing; the architecture
 * rules enforce that layering. The migration inventory and status live
 * in {@code docs/operation-layer.md}.
 */
package jls.collab.op;
