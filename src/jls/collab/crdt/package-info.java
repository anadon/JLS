/**
 * The replication substrate for simultaneous editing (issue #171,
 * collab Stage 2): operations from {@link jls.collab.op} travel between
 * peers as {@link jls.collab.crdt.OpEnvelope}s stamped with
 * {@link jls.collab.crdt.VectorClock}s, and each replica feeds arriving
 * envelopes through a {@link jls.collab.crdt.CausalBuffer} that
 * delivers them in causal order exactly once (op-id dedup, so flapping
 * links cannot double-apply — research doc
 * {@code docs/collaborative-editing-research.md} &sect;5.4).
 *
 * This package is deliberately headless and mechanism-only (issue #163
 * layering): it knows how to order and deduplicate envelopes, not what
 * the ops inside them mean. Per-kind merge semantics (add-wins element
 * set, per-attribute last-writer-wins, OR-set wires, RGA sequences),
 * anti-entropy resync, log compaction, and collaborative undo build on
 * top of this substrate and remain issue #171 work in progress.
 *
 * <p>Null-marked (issue #93): every reference in this package is
 * non-null unless annotated { }, and NullAway enforces
 * the contract at compile time on the default build.</p>
 */
@NullMarked
package jls.collab.crdt;

import org.jspecify.annotations.NullMarked;
