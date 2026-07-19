package jls.collab.crdt;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Causal delivery with exactly-once semantics (issue #171, collab
 * Stage 2): a replica feeds every arriving {@link OpEnvelope} through
 * {@link #offer}, and the buffer hands back the envelopes that are now
 * safe to apply, in causal order. An envelope is deliverable when the
 * replica has already delivered the origin's previous op and everything
 * the origin had observed when it emitted (the standard causal
 * broadcast condition); anything early is buffered until its
 * dependencies arrive, and anything already delivered — a flapping link
 * re-presenting an envelope, an anti-entropy overlap — is dropped, so
 * no op id is ever applied twice (research doc
 * {@code docs/collaborative-editing-research.md} &sect;5.4).
 *
 * Concurrent envelopes carry no order between them and are delivered in
 * arrival order; making concurrent deliveries converge across replicas
 * is the job of the per-kind CRDT merge rules layered above this
 * buffer, not of delivery.
 *
 * Two envelopes with the same op id but different contents are
 * equivocation; the buffer keeps the first and counts the second as a
 * duplicate. Attribution and the misbehavior policy for equivocating
 * peers are issue #170's scope.
 *
 * Not thread-safe: a replica owns one buffer and drives it from its
 * session thread.
 */
public final class CausalBuffer {

	/** Hostile-input cap on buffered envelopes (issue #38 discipline). */
	private static final int MAX_PENDING = 10_000;

	/** Everything this replica has delivered, as a vector clock. */
	private VectorClock clock = VectorClock.EMPTY;

	/** Envelopes whose causal dependencies have not all arrived. */
	private final List<OpEnvelope> pending =
			new ArrayList<OpEnvelope>();

	/** How many envelopes were dropped as already-seen. */
	private long duplicatesDropped;

	/**
	 * Create an empty buffer: nothing delivered yet, nothing pending.
	 */
	public CausalBuffer() {
	}

	/**
	 * Present one arriving envelope and collect everything that is now
	 * deliverable: possibly nothing (the envelope was early, or a
	 * duplicate), possibly the envelope alone, possibly a cascade of
	 * previously buffered envelopes it unblocked.
	 *
	 * @param envelope The arriving envelope.
	 *
	 * @return the envelopes to apply, in causal delivery order.
	 *
	 * @throws IllegalStateException if the envelope must be buffered but
	 *             the buffer is full (a peer flooding far-future
	 *             envelopes; issue #170 owns the policy response).
	 */
	public List<OpEnvelope> offer(OpEnvelope envelope) {

		if (envelope.seq() <= clock.counter(envelope.origin())
				|| isPending(envelope.id())) {
			duplicatesDropped += 1;
			return List.of();
		}
		if (pending.size() >= MAX_PENDING) {
			throw new IllegalStateException("the causal buffer holds "
					+ MAX_PENDING + " undeliverable envelopes; refusing '"
					+ envelope.id() + "'");
		}
		pending.add(envelope);
		List<OpEnvelope> delivered = new ArrayList<OpEnvelope>();
		boolean progressed = true;
		while (progressed) {
			progressed = false;
			Iterator<OpEnvelope> scan = pending.iterator();
			while (scan.hasNext()) {
				OpEnvelope candidate = scan.next();
				if (deliverable(candidate)) {
					scan.remove();
					clock = clock.merge(candidate.clock());
					delivered.add(candidate);
					progressed = true;
					break;
				}
			}
		}
		return delivered;
	} // end of offer method

	/**
	 * Whether an envelope's causal dependencies have all been delivered:
	 * it is the origin's next op, and every other peer's counter in its
	 * clock is already covered here.
	 *
	 * @param envelope The candidate.
	 *
	 * @return true exactly when the envelope can be delivered now.
	 */
	private boolean deliverable(OpEnvelope envelope) {

		String origin = envelope.origin();
		if (envelope.seq() != clock.counter(origin) + 1) {
			return false;
		}
		for (var entry : envelope.clock().entries().entrySet()) {
			if (!entry.getKey().equals(origin)
					&& entry.getValue() > clock.counter(entry.getKey())) {
				return false;
			}
		}
		return true;
	} // end of deliverable method

	/**
	 * Whether an op id is already waiting in the buffer.
	 *
	 * @param id The op id.
	 *
	 * @return true if a pending envelope carries that id.
	 */
	private boolean isPending(OpId id) {

		for (OpEnvelope envelope : pending) {
			if (envelope.id().equals(id)) {
				return true;
			}
		}
		return false;
	} // end of isPending method

	/**
	 * Everything this replica has delivered, as a vector clock — the
	 * clock anti-entropy exchanges on reconnect.
	 *
	 * @return the delivery clock.
	 */
	public VectorClock clock() {

		return clock;
	} // end of clock method

	/**
	 * How many envelopes are buffered awaiting dependencies.
	 *
	 * @return the pending count.
	 */
	public int pendingCount() {

		return pending.size();
	} // end of pendingCount method

	/**
	 * How many envelopes were dropped as duplicates of something
	 * already delivered or already buffered.
	 *
	 * @return the duplicate count.
	 */
	public long duplicatesDropped() {

		return duplicatesDropped;
	} // end of duplicatesDropped method

} // end of CausalBuffer class
