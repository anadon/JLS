package jls.collab.net;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jspecify.annotations.Nullable;

/**
 * An in-memory {@link Transport} pair (issues #163/#168, collab Stage
 * 1a): two connected endpoints over two bounded in-process queues, so
 * the replication stack's tests exercise the exact {@link Transport}
 * contract - in-order opaque frames, blocking receive, null on clean
 * close, the {@value SecureLink#MAX_PAYLOAD_BYTES}-byte payload cap -
 * without binding a socket, running a handshake, or touching a wall
 * clock. No socket is constructed here, so the socket-confinement
 * ratchet's census is untouched.
 *
 * <p>Fidelity choices, each mirroring the real {@link SocketSession}:
 * frames are defensively copied on send, so a caller reusing its
 * buffer cannot corrupt a frame in flight; an over-cap payload is a
 * {@link FrameRejected} with the same cap the secure link enforces;
 * closing either endpoint ends both directions, and the peer drains
 * any frames already sent before reading the clean end as null - the
 * "close on a frame boundary" shape. One deliberate difference: where
 * TCP applies backpressure by blocking, this pair holds no thread, so
 * a sender that runs more than {@value #MAX_PENDING_FRAMES} frames
 * ahead of its peer fails loudly with an {@link IOException} instead
 * of blocking - a test that overruns the cap is a test with a missing
 * receiver, and a deterministic failure beats a silent hang.</p>
 */
public final class LoopbackTransport implements Transport {

	/**
	 * The most frames one direction may hold undelivered. A pair
	 * exists for tests, where a sender running unboundedly ahead of
	 * its receiver is a bug; the cap turns that bug into an immediate
	 * {@link IOException} instead of unbounded memory growth or a
	 * hang.
	 */
	static final int MAX_PENDING_FRAMES = 64;

	/**
	 * The clean-end sentinel a closing endpoint enqueues, compared by
	 * identity. An empty payload is a legal frame, but every payload
	 * is defensively copied on send, so no legal frame can ever be
	 * this exact array.
	 */
	private static final byte[] EOF = new byte[0];

	/**
	 * The queue this endpoint sends into (the peer's inbound). Sized
	 * two over the pending cap so the two possible end-of-stream
	 * sentinels - one from each endpoint's close - always fit even
	 * when the cap's worth of frames is pending.
	 */
	private final BlockingQueue<byte[]> outbound;

	/** The queue this endpoint receives from (the peer sends into). */
	private final BlockingQueue<byte[]> inbound;

	/**
	 * Whether this endpoint has been closed. Close is idempotent; the
	 * flip from false to true happens exactly once and enqueues the
	 * end-of-stream sentinels.
	 */
	private final AtomicBoolean closed = new AtomicBoolean();

	/**
	 * Whether this endpoint has consumed an end-of-stream sentinel.
	 * Once drained, every receive returns null without touching the
	 * queue, matching a socket stream that stays at end-of-file.
	 */
	private boolean drained;

	/** Serializes senders, so the cap check and enqueue are atomic. */
	private final Object sendLock = new Object();

	/** Serializes receivers, the "one I/O thread" shape made safe. */
	private final Object receiveLock = new Object();

	/**
	 * Bind one endpoint to its two queues. Only {@link #pair()}
	 * constructs endpoints, always in connected pairs.
	 *
	 * @param outbound The queue this endpoint sends into.
	 * @param inbound The queue this endpoint receives from.
	 */
	private LoopbackTransport(BlockingQueue<byte[]> outbound,
			BlockingQueue<byte[]> inbound) {

		this.outbound = outbound;
		this.inbound = inbound;
	} // end of constructor

	/**
	 * Create a connected pair: what one endpoint sends, the other
	 * receives, in order, in both directions.
	 *
	 * @return the two connected endpoints.
	 */
	public static Pair pair() {

		BlockingQueue<byte[]> leftToRight =
				new ArrayBlockingQueue<>(MAX_PENDING_FRAMES + 2);
		BlockingQueue<byte[]> rightToLeft =
				new ArrayBlockingQueue<>(MAX_PENDING_FRAMES + 2);
		return new Pair(new LoopbackTransport(leftToRight, rightToLeft),
				new LoopbackTransport(rightToLeft, leftToRight));
	} // end of pair method

	/**
	 * Copy a payload and hand the copy to the peer. The copy is what
	 * makes frames opaque values rather than shared buffers: mutating
	 * the argument after this call cannot alter what the peer reads.
	 *
	 * @param payload The opaque payload bytes (may be empty).
	 *
	 * @throws IOException if this endpoint is closed or the peer's
	 *             pending-frame cap is full.
	 * @throws FrameRejected if the payload is over the {@value
	 *             SecureLink#MAX_PAYLOAD_BYTES}-byte cap.
	 */
	@Override
	public void send(byte[] payload) throws IOException, FrameRejected {

		if (payload.length > SecureLink.MAX_PAYLOAD_BYTES) {
			throw new FrameRejected("a payload of " + payload.length
					+ " bytes exceeds the cap of "
					+ SecureLink.MAX_PAYLOAD_BYTES);
		}
		synchronized (sendLock) {
			if (closed.get()) {
				throw new IOException(
						"this loopback endpoint is closed");
			}
			if (pendingFrames() >= MAX_PENDING_FRAMES
					|| !outbound.offer(payload.clone())) {
				throw new IOException("the peer has "
						+ MAX_PENDING_FRAMES + " frames pending and is"
						+ " not receiving; a loopback pair fails fast"
						+ " where a socket would block");
			}
		}
	} // end of send method

	/**
	 * Count the real frames pending toward the peer, excluding any
	 * end-of-stream sentinel the peer's own close may have enqueued.
	 *
	 * @return how many undelivered payload frames the peer holds.
	 */
	private int pendingFrames() {

		int pending = 0;
		for (byte[] frame : outbound) {
			if (frame != EOF) {
				pending++;
			}
		}
		return pending;
	} // end of pendingFrames method

	/**
	 * Block until the peer's next frame arrives, then return it. The
	 * frame was copied when sent and is removed from the queue here,
	 * so the caller owns the returned array outright.
	 *
	 * @return the next payload, or null once either endpoint has
	 *         closed and every earlier frame has been drained.
	 *
	 * @throws IOException if the wait is interrupted; the thread's
	 *             interrupt status is restored first.
	 */
	@Override
	public byte @Nullable [] receive() throws IOException {

		synchronized (receiveLock) {
			if (drained) {
				return null;
			}
			byte[] frame;
			try {
				frame = inbound.take();
			} catch (InterruptedException interrupted) {
				Thread.currentThread().interrupt();
				InterruptedIOException failed = new InterruptedIOException(
						"interrupted while waiting for a loopback frame");
				failed.initCause(interrupted);
				throw failed;
			}
			if (frame == EOF) {
				drained = true;
				return null;
			}
			return frame;
		}
	} // end of receive method

	/**
	 * Close this endpoint, ending both directions. The peer drains any
	 * frames already sent, then reads null; a receive blocked on this
	 * endpoint wakes and returns null too. Idempotent and safe to call
	 * from a finally block.
	 */
	@Override
	public void close() {

		if (closed.compareAndSet(false, true)) {
			// wake the peer's blocked receive with a clean end, and
			// this endpoint's own; the queues reserve room for both
			// sentinels, so these offers cannot fail while the send
			// cap holds
			boolean peerWoken = outbound.offer(EOF);
			boolean selfWoken = inbound.offer(EOF);
			if (!peerWoken || !selfWoken) {
				throw new IllegalStateException(
						"loopback close failed to enqueue EOF sentinel");
			}
		}
	} // end of close method

	/**
	 * A connected loopback pair: frames sent on {@link #left()} arrive
	 * on {@link #right()} and vice versa.
	 *
	 * @param left One endpoint of the channel.
	 * @param right The other endpoint of the same channel.
	 */
	public record Pair(LoopbackTransport left, LoopbackTransport right) {

	} // end of Pair record

} // end of LoopbackTransport class
