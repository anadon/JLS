package jls.collab.net;

import java.io.Closeable;
import java.io.IOException;

import org.jspecify.annotations.Nullable;

/**
 * One bidirectional, in-order channel of opaque frames between two
 * collaboration peers (issues #163/#168, collab Stage 1a): the seam
 * the replication stack talks through, so everything above the wire
 * can run against an in-memory {@link LoopbackTransport} pair in tests
 * and against a live {@link SocketSession} in production without
 * changing a line. The interface is exactly the frame surface
 * {@code SocketSession} already exposes; extracting it adds no
 * behavior.
 *
 * <p>The contract, shared by every implementation:</p>
 *
 * <ul>
 * <li><b>Frames are opaque.</b> A payload's bytes mean nothing here -
 * no circuit semantics, no operation vocabulary, nothing above the
 * wire ({@code jls.ArchitectureRulesTest#transportKnowsNothingOfCircuits}
 * pins that for the whole package).</li>
 * <li><b>In-order per direction.</b> Frames sent from one endpoint
 * arrive at the peer in send order, without loss or duplication, until
 * the channel ends. The two directions are independent.</li>
 * <li><b>No threads of its own.</b> {@link #receive()} blocks the
 * calling thread until a frame arrives or the channel ends; the layer
 * above owns the session thread (the "one I/O thread per session"
 * shape issue #168 asks for).</li>
 * <li><b>Orderly end is null.</b> A peer that closes on a frame
 * boundary ends the stream cleanly: {@link #receive()} returns null,
 * then and forever. A torn or hostile end is an exception, never
 * null.</li>
 * </ul>
 */
public interface Transport extends Closeable {

	/**
	 * Send one frame to the peer. The payload's meaning is opaque
	 * here; implementations enforce the {@value
	 * SecureLink#MAX_PAYLOAD_BYTES}-byte payload cap (the #38
	 * hostile-input discipline) and may reject a caller that runs
	 * unboundedly ahead of its peer.
	 *
	 * @param payload The opaque payload bytes (may be empty).
	 *
	 * @throws IOException if the channel is closed or the write fails.
	 * @throws FrameRejected if the payload is over the cap or the
	 *             channel has been poisoned by an earlier failure.
	 */
	void send(byte[] payload) throws IOException, FrameRejected;

	/**
	 * Block until the peer's next frame arrives, then return its
	 * payload. A clean channel end - the peer closing on a frame
	 * boundary - returns null, and every later call returns null too.
	 *
	 * @return the next payload, or null if the channel ended cleanly.
	 *
	 * @throws IOException if the read fails or the wait is
	 *             interrupted.
	 * @throws FrameRejected if a frame is over-cap, truncated, or
	 *             fails authentication.
	 */
	byte @Nullable [] receive() throws IOException, FrameRejected;

	/**
	 * Close this endpoint, ending the channel. Idempotent and safe to
	 * call from a finally block; the peer observes a clean end (a null
	 * from {@link #receive()}) once the frames already in flight are
	 * drained.
	 *
	 * @throws IOException if closing the underlying resource fails.
	 */
	@Override
	void close() throws IOException;

} // end of Transport interface
