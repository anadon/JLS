package jls.collab.net;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

import org.jspecify.annotations.Nullable;

/**
 * A seeded fault-injecting {@link Transport} decorator (issues
 * #163/#168, the G3/G4 chaos apparatus): wraps any transport and,
 * on the send side only, drops, duplicates, or reorders frames with
 * caller-chosen probabilities, and partitions/heals the outbound
 * direction on command. Every fate is drawn from one {@link Random}
 * seeded by the caller, so a failing schedule replays exactly from its
 * seed - print the seed in every assertion message. The decorator
 * never invents or corrupts a byte: every frame the peer receives is a
 * copy of a frame the caller sent, and reordering is a bounded
 * holdback (a held frame is released behind the next passing frame, or
 * at close), never a wall-clock sleep.
 */
final class ChaosTransport implements Transport {

	/**
	 * The most frames the reorder holdback may hold. Small and
	 * bounded: a held frame is released no later than the next
	 * passing send or the close, so chaos delays frames but never
	 * strands them.
	 */
	static final int HOLDBACK_CAP = 4;

	/** The real transport every surviving frame is sent through. */
	private final Transport delegate;

	/** The single seeded source of every chaos decision. */
	private final Random random;

	/** The probability a sent frame is silently dropped. */
	private final double dropProbability;

	/** The probability a surviving frame is delivered twice. */
	private final double duplicateProbability;

	/** The probability a surviving frame is held back and reordered. */
	private final double reorderProbability;

	/** Frames held back to be released behind a later frame. */
	private final Deque<byte[]> holdback = new ArrayDeque<>();

	/** Frames buffered while the outbound direction is partitioned. */
	private final Deque<byte[]> partitioned = new ArrayDeque<>();

	/** Whether the outbound direction is currently partitioned. */
	private boolean isPartitioned;

	/** How many frames the drop fate has consumed. */
	private int droppedCount;

	/** How many extra copies the duplicate fate has delivered. */
	private int duplicatedCount;

	/** How many frames the reorder fate has held back. */
	private int heldBackCount;

	/**
	 * Wrap a transport in seeded chaos.
	 *
	 * @param delegate The real transport to decorate.
	 * @param seed The seed every chaos decision derives from.
	 * @param dropProbability Chance in [0,1] a frame is dropped.
	 * @param duplicateProbability Chance in [0,1] a surviving frame is
	 *            delivered twice.
	 * @param reorderProbability Chance in [0,1] a surviving frame is
	 *            held back behind later frames.
	 */
	ChaosTransport(Transport delegate, long seed, double dropProbability,
			double duplicateProbability, double reorderProbability) {

		this.delegate = delegate;
		this.random = new Random(seed);
		this.dropProbability = dropProbability;
		this.duplicateProbability = duplicateProbability;
		this.reorderProbability = reorderProbability;
	} // end of constructor

	/**
	 * Send a frame through the chaos gauntlet: maybe dropped, maybe
	 * doubled, maybe held back behind later frames, and buffered
	 * while partitioned. The three fates are drawn in a fixed order
	 * from the one seeded source, so a schedule is a pure function of
	 * the seed and the call sequence.
	 *
	 * @param payload The opaque payload bytes.
	 *
	 * @throws IOException if the delegate rejects the write.
	 * @throws FrameRejected if the delegate rejects the frame.
	 */
	@Override
	public synchronized void send(byte[] payload)
			throws IOException, FrameRejected {

		if (random.nextDouble() < dropProbability) {
			droppedCount++;
			return;
		}
		int copies =
				random.nextDouble() < duplicateProbability ? 2 : 1;
		duplicatedCount += copies - 1;
		if (random.nextDouble() < reorderProbability
				&& holdback.size() + copies <= HOLDBACK_CAP) {
			for (int i = 0; i < copies; i++) {
				holdback.addLast(payload.clone());
			}
			heldBackCount++;
			return;
		}
		for (int i = 0; i < copies; i++) {
			deliver(payload.clone());
		}
		releaseHoldback();
	} // end of send method

	/**
	 * Deliver one owned frame: buffered if partitioned, sent
	 * otherwise.
	 *
	 * @param frame The frame, owned by this decorator.
	 *
	 * @throws IOException if the delegate rejects the write.
	 * @throws FrameRejected if the delegate rejects the frame.
	 */
	private void deliver(byte[] frame)
			throws IOException, FrameRejected {

		if (isPartitioned) {
			partitioned.addLast(frame);
		} else {
			delegate.send(frame);
		}
	} // end of deliver method

	/**
	 * Release every held-back frame, behind whatever was just
	 * delivered - that inversion is the reorder.
	 *
	 * @throws IOException if the delegate rejects a write.
	 * @throws FrameRejected if the delegate rejects a frame.
	 */
	private void releaseHoldback() throws IOException, FrameRejected {

		while (!holdback.isEmpty()) {
			deliver(holdback.removeFirst());
		}
	} // end of releaseHoldback method

	/**
	 * Cut the outbound direction: frames keep their chaos fates but
	 * pile up here instead of reaching the peer, until {@link
	 * #heal()}.
	 */
	synchronized void partition() {

		isPartitioned = true;
	} // end of partition method

	/**
	 * Reconnect the outbound direction and flush, in order, every
	 * frame buffered while partitioned - a partition delays frames,
	 * it never loses them.
	 *
	 * @throws IOException if the delegate rejects a write.
	 * @throws FrameRejected if the delegate rejects a frame.
	 */
	synchronized void heal() throws IOException, FrameRejected {

		isPartitioned = false;
		while (!partitioned.isEmpty()) {
			delegate.send(partitioned.removeFirst());
		}
	} // end of heal method

	/**
	 * Receive from the delegate, untouched: chaos is injected on this
	 * decorator's send side only.
	 *
	 * @return the next payload, or null if the channel ended cleanly.
	 *
	 * @throws IOException if the delegate's read fails.
	 * @throws FrameRejected if the delegate rejects a frame.
	 */
	@Override
	public byte @Nullable [] receive() throws IOException, FrameRejected {

		return delegate.receive();
	} // end of receive method

	/**
	 * Flush the reorder holdback so no frame is stranded, then close
	 * the delegate. Frames still partitioned are lost, as a real
	 * partition at close would lose them; heal first to keep them.
	 *
	 * @throws IOException if the flush or the close fails.
	 */
	@Override
	public synchronized void close() throws IOException {

		try {
			if (!isPartitioned) {
				releaseHoldback();
			}
		} catch (FrameRejected rejected) {
			throw new IOException(
					"the holdback flush was rejected at close", rejected);
		} finally {
			delegate.close();
		}
	} // end of close method

	/**
	 * How many frames the drop fate consumed.
	 *
	 * @return the dropped-frame count so far.
	 */
	synchronized int droppedCount() {

		return droppedCount;
	} // end of droppedCount method

	/**
	 * How many extra copies the duplicate fate delivered.
	 *
	 * @return the duplicated-frame count so far.
	 */
	synchronized int duplicatedCount() {

		return duplicatedCount;
	} // end of duplicatedCount method

	/**
	 * How many sends the reorder fate held back.
	 *
	 * @return the held-back count so far.
	 */
	synchronized int heldBackCount() {

		return heldBackCount;
	} // end of heldBackCount method

} // end of ChaosTransport class
