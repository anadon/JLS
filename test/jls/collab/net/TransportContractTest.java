package jls.collab.net;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

/**
 * The {@link Transport} contract, run against both implementations
 * (issues #163/#168): the in-memory {@link LoopbackTransport} pair and
 * a real {@link SocketSession} pair - handshake and all - over a
 * loopback TCP socket, the same rig {@link SocketSessionTest} drives.
 * What the loopback pair exists to promise is exactly what the socket
 * delivers: in-order opaque frames both ways, empty payloads as legal
 * frames, a peer's clean close as a null that repeats forever, and the
 * same over-cap rejection. Each contract case runs once per
 * implementation, so a drift between the test double and the real
 * transport fails here by name.
 */
class TransportContractTest {

	/** The joiner (initiator) identity for the socket rig. */
	private static final IdentityKey ALICE =
			IdentityKey.generate("alice");

	/** The starter (responder) identity for the socket rig. */
	private static final IdentityKey BOB = IdentityKey.generate("bob");

	/**
	 * A connected pair of transports under contract, closed as one.
	 *
	 * @param left One endpoint.
	 * @param right The other endpoint of the same channel.
	 */
	private record Rig(Transport left, Transport right)
			implements AutoCloseable {

		@Override
		public void close() throws Exception {

			try {
				left.close();
			} finally {
				right.close();
			}
		} // end of close method

	} // end of Rig record

	/**
	 * Build the in-memory rig.
	 *
	 * @return a connected loopback pair.
	 */
	private static Rig loopbackRig() {

		LoopbackTransport.Pair pair = LoopbackTransport.pair();
		return new Rig(pair.left(), pair.right());
	} // end of loopbackRig method

	/**
	 * Build the real rig: a handshaken {@link SocketSession} pair over
	 * a loopback TCP socket, the {@link SocketSessionTest} shape.
	 *
	 * @return a connected socket-session pair.
	 *
	 * @throws Exception if the bind, connect, or handshake fails.
	 */
	private static Rig socketRig() throws Exception {

		ExecutorService pool = Executors.newSingleThreadExecutor();
		try (SessionListener listener = SessionListener.bindLoopback(0)) {
			Future<SocketSession> starter =
					pool.submit(() -> listener.accept(BOB));
			SocketSession joiner = SocketSession.join(
					InetAddress.getLoopbackAddress(), listener.port(),
					ALICE, 5_000);
			return new Rig(joiner, starter.get(10, TimeUnit.SECONDS));
		} finally {
			pool.shutdownNow();
		}
	} // end of socketRig method

	@Test
	void loopbackCarriesFramesInOrderBothWays() throws Exception {
		try (Rig rig = loopbackRig()) {
			assertInOrderBothWays(rig);
		}
	}

	@Test
	void socketCarriesFramesInOrderBothWays() throws Exception {
		try (Rig rig = socketRig()) {
			assertInOrderBothWays(rig);
		}
	}

	@Test
	void loopbackTreatsAnEmptyPayloadAsAFrame() throws Exception {
		try (Rig rig = loopbackRig()) {
			assertEmptyPayloadIsAFrame(rig);
		}
	}

	@Test
	void socketTreatsAnEmptyPayloadAsAFrame() throws Exception {
		try (Rig rig = socketRig()) {
			assertEmptyPayloadIsAFrame(rig);
		}
	}

	@Test
	void loopbackReadsAPeerCloseAsNullForever() throws Exception {
		try (Rig rig = loopbackRig()) {
			assertPeerCloseReadsAsNull(rig);
		}
	}

	@Test
	void socketReadsAPeerCloseAsNullForever() throws Exception {
		try (Rig rig = socketRig()) {
			assertPeerCloseReadsAsNull(rig);
		}
	}

	@Test
	void loopbackRejectsAnOverCapPayload() throws Exception {
		try (Rig rig = loopbackRig()) {
			assertOverCapPayloadIsRejected(rig);
		}
	}

	@Test
	void socketRejectsAnOverCapPayload() throws Exception {
		try (Rig rig = socketRig()) {
			assertOverCapPayloadIsRejected(rig);
		}
	}

	/**
	 * Contract: frames arrive in send order, independently in each
	 * direction.
	 *
	 * @param rig The connected pair under test.
	 *
	 * @throws Exception if a send or receive fails.
	 */
	private static void assertInOrderBothWays(Rig rig) throws Exception {

		for (int i = 0; i < 5; i++) {
			rig.left().send(frame("left " + i));
			rig.right().send(frame("right " + i));
		}
		for (int i = 0; i < 5; i++) {
			assertArrayEquals(frame("left " + i), rig.right().receive(),
					"left-to-right frames arrive in send order");
			assertArrayEquals(frame("right " + i), rig.left().receive(),
					"right-to-left frames arrive in send order");
		}
	} // end of assertInOrderBothWays method

	/**
	 * Contract: an empty payload is a legal frame, distinct from the
	 * end of the stream.
	 *
	 * @param rig The connected pair under test.
	 *
	 * @throws Exception if a send or receive fails.
	 */
	private static void assertEmptyPayloadIsAFrame(Rig rig)
			throws Exception {

		rig.left().send(new byte[0]);
		rig.left().send(frame("after"));
		assertArrayEquals(new byte[0], rig.right().receive(),
				"an empty payload is a frame, not an end of stream");
		assertArrayEquals(frame("after"), rig.right().receive());
	} // end of assertEmptyPayloadIsAFrame method

	/**
	 * Contract: a peer closing on a frame boundary reads as null,
	 * after pending frames drain, and forever after.
	 *
	 * @param rig The connected pair under test.
	 *
	 * @throws Exception if a send, receive, or close fails.
	 */
	private static void assertPeerCloseReadsAsNull(Rig rig)
			throws Exception {

		rig.left().send(frame("last words"));
		rig.left().close();
		assertArrayEquals(frame("last words"), rig.right().receive(),
				"frames sent before the close still arrive");
		assertNull(rig.right().receive(),
				"a clean close on a frame boundary reads as null");
		assertNull(rig.right().receive(),
				"the stream stays at end of file forever");
	} // end of assertPeerCloseReadsAsNull method

	/**
	 * Contract: a payload over {@link SecureLink#MAX_PAYLOAD_BYTES}
	 * is a typed rejection on send, before anything reaches the peer.
	 * What happens to the channel afterward is implementation-defined
	 * - a rejected seal poisons a {@link SecureLink} for good, while
	 * the loopback pair has no crypto state to poison - so the shared
	 * contract asserts the rejection only.
	 *
	 * @param rig The connected pair under test.
	 */
	private static void assertOverCapPayloadIsRejected(Rig rig) {

		byte[] overCap = new byte[SecureLink.MAX_PAYLOAD_BYTES + 1];
		assertThrows(FrameRejected.class,
				() -> rig.left().send(overCap),
				"one byte over the shared cap is a typed rejection");
	} // end of assertOverCapPayloadIsRejected method

	/**
	 * Encode a label as a frame payload.
	 *
	 * @param label The label to encode.
	 *
	 * @return the label's UTF-8 bytes.
	 */
	private static byte[] frame(String label) {

		return label.getBytes(StandardCharsets.UTF_8);
	} // end of frame method

} // end of TransportContractTest class
