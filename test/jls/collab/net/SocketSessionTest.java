package jls.collab.net;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.DataOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

/**
 * The socket transport's contract (issue #168, collab Stage 1a): two
 * instances on loopback complete the handshake over a real TCP socket,
 * derive the same SAS and authenticate each other's identity, then
 * exchange encrypted frames in both directions - and a peer that lies
 * about a handshake message's length is rejected before the transport
 * allocates a buffer to it. This is the loopback two-instance harness
 * the issue calls for, exercising {@link SessionListener} and {@link
 * SocketSession} together over the pre-existing crypto core.
 */
class SocketSessionTest {

	/** The joiner (initiator) identity shared by the trials. */
	private static final IdentityKey ALICE =
			IdentityKey.generate("alice");

	/** The starter (responder) identity shared by the trials. */
	private static final IdentityKey BOB = IdentityKey.generate("bob");

	/** The loopback address every trial binds and connects on. */
	private static final InetAddress LOOPBACK =
			InetAddress.getLoopbackAddress();

	@Test
	void twoInstancesCompleteAndAgreeOverLoopback() throws Exception {
		ExecutorService pool = Executors.newSingleThreadExecutor();
		try (SessionListener listener = SessionListener.bindLoopback(0)) {
			int port = listener.port();
			assertTrue(port > 0, "the bound port is known before accept");

			Future<SocketSession> starter =
					pool.submit(() -> listener.accept(BOB));
			try (SocketSession joiner = SocketSession.join(LOOPBACK, port,
					ALICE, 5_000);
					SocketSession started =
							starter.get(10, TimeUnit.SECONDS)) {

				// both humans read the same glyphs, and each side
				// authenticated the other's long-term key
				assertEquals(joiner.link().sas(), started.link().sas(),
						"both sides derive the same SAS over the socket");
				assertEquals(joiner.link().sessionId(),
						started.link().sessionId());
				assertEquals(BOB.fingerprint(),
						joiner.link().peerFingerprint());
				assertEquals(ALICE.fingerprint(),
						started.link().peerFingerprint());
				assertEquals("bob", joiner.link().peerDisplayName());
				assertEquals("alice", started.link().peerDisplayName());

				// frames flow both ways over the live sockets
				byte[] up = "join payload".getBytes(StandardCharsets.UTF_8);
				joiner.send(up);
				assertArrayEquals(up, started.receive());

				byte[] down =
						"start payload".getBytes(StandardCharsets.UTF_8);
				started.send(down);
				assertArrayEquals(down, joiner.receive());
			}
		} finally {
			pool.shutdownNow();
		}
	}

	@Test
	void aCleanPeerCloseEndsTheStreamAsNull() throws Exception {
		ExecutorService pool = Executors.newSingleThreadExecutor();
		try (SessionListener listener = SessionListener.bindLoopback(0)) {
			int port = listener.port();
			Future<SocketSession> starter =
					pool.submit(() -> listener.accept(BOB));
			SocketSession joiner = SocketSession.join(LOOPBACK, port,
					ALICE, 5_000);
			try (SocketSession started = starter.get(10, TimeUnit.SECONDS)) {
				joiner.close();
				assertNull(started.receive(),
						"a clean close on a frame boundary reads as null");
			}
		} finally {
			pool.shutdownNow();
		}
	}

	@Test
	void aTamperedFrameIsRejectedAndPoisonsTheLink() throws Exception {
		ExecutorService pool = Executors.newSingleThreadExecutor();
		try (SessionListener listener = SessionListener.bindLoopback(0)) {
			int port = listener.port();
			Future<SocketSession> starter =
					pool.submit(() -> listener.accept(BOB));
			try (SocketSession joiner = SocketSession.join(LOOPBACK, port,
					ALICE, 5_000);
					SocketSession started =
							starter.get(10, TimeUnit.SECONDS)) {

				// seal a real frame, flip a ciphertext byte, and inject
				// it on the raw socket ahead of the transport's reader
				byte[] frame = joiner.link()
						.seal("hi".getBytes(StandardCharsets.UTF_8));
				frame[frame.length - 1] ^= 0x01;
				assertThrows(FrameRejected.class,
						() -> started.link().openFrame(frame),
						"a tampered frame fails authentication");
				assertTrue(started.link().isPoisoned(),
						"a bad tag poisons the link for good");
			}
		} finally {
			pool.shutdownNow();
		}
	}

	@Test
	void anOversizedHandshakeLengthIsRejectedBeforeAllocation()
			throws Exception {
		ExecutorService pool = Executors.newSingleThreadExecutor();
		try (SessionListener listener = SessionListener.bindLoopback(0)) {
			int port = listener.port();
			Future<SocketSession> starter =
					pool.submit(() -> listener.accept(BOB));

			// a hostile joiner that claims a two-gigabyte first message
			try (Socket rogue = new Socket(LOOPBACK, port);
					DataOutputStream data = new DataOutputStream(
							rogue.getOutputStream())) {
				data.writeInt(Integer.MAX_VALUE);
				data.flush();

				ExecutionException wrapped = assertThrows(
						ExecutionException.class,
						() -> starter.get(10, TimeUnit.SECONDS));
				assertTrue(wrapped.getCause() instanceof HandshakeRejected,
						"an over-cap handshake length is a rejection, not"
								+ " an allocation: "
								+ wrapped.getCause());
			}
		} finally {
			pool.shutdownNow();
		}
	}

} // end of SocketSessionTest class
