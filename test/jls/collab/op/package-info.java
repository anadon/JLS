/**
 * Test suite for {@code jls.collab.op}, the operation layer of the
 * collaborative-editing program (issue #167). Pins the vocabulary's
 * whole contract against the canonical-serialization oracle (issue
 * #166): op-vs-inline byte parity, apply-then-inverse restoring the
 * exact prior bytes on freshly loaded and save/load-restored circuits
 * alike, save/read/save byte-identical serialization round-trips with
 * strict rejection of malformed and oversized input, and atomicity -
 * a rejected op leaves the circuit byte-identical, including when
 * validation fails partway through a multi-element op. Fixtures are
 * the wire-heavy fork shift-register file (shared with the
 * determinism suite) and a minimal unwired-element circuit for the
 * rotate/flip ops, whose validity requires detached puts.
 */
package jls.collab.op;
