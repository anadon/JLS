package jls.collab.net;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

/**
 * The in-memory transport pair's contract (issues #163/#168): frames
 * flow in order in both directions, a blocking receive parks a real
 * second thread until a frame or a close arrives, either endpoint's
 * close reads as a clean null after the pending frames drain, an
 * over-cap payload is rejected with the same {@link
 * SecureLink#MAX_PAYLOAD_BYTES} cap the socket path enforces, a
 * sender running unboundedly ahead fails fast instead of hanging, and
 * frames are defensive copies rather than shared buffers.
 */
class LoopbackTransportTest {

	@Test
	void framesArriveInOrderInBothDirections() throws Exception {
		LoopbackTransport.Pair pair = LoopbackTransport.pair();
		for (int i = 0; i < 10; i++) {
			pair.left().send(frame("left " + i));
			pair.right().send(frame("right " + i));
		}
		for (int i = 0; i < 10; i++) {
			assertArrayEquals(frame("left " + i), pair.right().receive(),
					"left-to-right frames arrive in send order");
			assertArrayEquals(frame("right " + i), pair.left().receive(),
					"right-to-left frames arrive in send order");
		}
	}

	@Test
	void anEmptyPayloadIsALegalFrame() throws Exception {
		LoopbackTransport.Pair pair = LoopbackTransport.pair();
		pair.left().send(new byte[0]);
		pair.left().send(frame("after"));
		assertArrayEquals(new byte[0], pair.right().receive(),
				"an empty payload is a frame, not an end of stream");
		assertArrayEquals(frame("after"), pair.right().receive());
	}

	@Test
	void aBlockedReceiveWakesWhenAFrameArrives() throws Exception {
		LoopbackTransport.Pair pair = LoopbackTransport.pair();
		ExecutorService pool = Executors.newSingleThreadExecutor();
		try {
			Future<byte[]> blocked =
					pool.submit(() -> pair.right().receive());
			// the receiver parks first; the send must wake it
			Thread.sleep(50);
			pair.left().send(frame("wake"));
			assertArrayEquals(frame("wake"),
					blocked.get(10, TimeUnit.SECONDS),
					"a parked receive returns the frame that wakes it");
		} finally {
			pool.shutdownNow();
		}
	}

	@Test
	void twoThreadsInterleaveWithoutLossOrReorder() throws Exception {
		LoopbackTransport.Pair pair = LoopbackTransport.pair();
		int frames = 500;
		ExecutorService pool = Executors.newSingleThreadExecutor();
		try {
			Future<?> producer = pool.submit(() -> {
				for (int i = 0; i < frames; i++) {
					sendYieldingOnBackpressure(pair.left(),
							frame("n" + i));
				}
				pair.left().close();
				return null;
			});
			List<String> received = new ArrayList<>();
			byte[] got;
			while ((got = pair.right().receive()) != null) {
				received.add(new String(got, StandardCharsets.UTF_8));
			}
			producer.get(10, TimeUnit.SECONDS);
			assertEquals(frames, received.size(),
					"every frame crosses the thread boundary");
			for (int i = 0; i < frames; i++) {
				assertEquals("n" + i, received.get(i),
						"frames stay in send order across threads");
			}
		} finally {
			pool.shutdownNow();
		}
	}

	@Test
	void aPeerCloseReadsAsNullAfterPendingFramesDrain() throws Exception {
		LoopbackTransport.Pair pair = LoopbackTransport.pair();
		pair.left().send(frame("last words"));
		pair.left().close();
		assertArrayEquals(frame("last words"), pair.right().receive(),
				"frames sent before the close still arrive");
		assertNull(pair.right().receive(),
				"a clean close on a frame boundary reads as null");
		assertNull(pair.right().receive(),
				"the stream stays at end of file forever");
	}

	@Test
	void aBlockedReceiveWakesAsNullOnSelfClose() throws Exception {
		LoopbackTransport.Pair pair = LoopbackTransport.pair();
		ExecutorService pool = Executors.newSingleThreadExecutor();
		try {
			Future<byte[]> blocked =
					pool.submit(() -> pair.right().receive());
			Thread.sleep(50);
			pair.right().close();
			assertNull(blocked.get(10, TimeUnit.SECONDS),
					"closing this endpoint wakes its own parked receive"
							+ " with a clean null");
		} finally {
			pool.shutdownNow();
		}
	}

	@Test
	void sendAfterCloseThrows() throws Exception {
		LoopbackTransport.Pair pair = LoopbackTransport.pair();
		pair.left().close();
		assertThrows(IOException.class,
				() -> pair.left().send(frame("too late")),
				"a closed endpoint refuses to send");
	}

	@Test
	void closeIsIdempotent() throws Exception {
		LoopbackTransport.Pair pair = LoopbackTransport.pair();
		pair.left().close();
		pair.left().close();
		assertNull(pair.right().receive(),
				"a double close still reads as one clean end");
		assertNull(pair.left().receive(),
				"the closing side reads its own clean end");
	}

	@Test
	void anOversizedPayloadIsRejectedWithTheSocketCap() throws Exception {
		LoopbackTransport.Pair pair = LoopbackTransport.pair();
		byte[] atCap = new byte[SecureLink.MAX_PAYLOAD_BYTES];
		pair.left().send(atCap);
		assertArrayEquals(atCap, pair.right().receive(),
				"a payload exactly at the cap passes");
		byte[] overCap = new byte[SecureLink.MAX_PAYLOAD_BYTES + 1];
		FrameRejected rejected = assertThrows(FrameRejected.class,
				() -> pair.left().send(overCap),
				"one byte over the cap is a typed rejection");
		assertTrue(rejected.getMessage()
				.contains(String.valueOf(SecureLink.MAX_PAYLOAD_BYTES)),
				"the rejection names the same cap the secure link"
						+ " enforces: " + rejected.getMessage());
	}

	@Test
	void aSenderRunningUnboundedlyAheadFailsFast() throws Exception {
		LoopbackTransport.Pair pair = LoopbackTransport.pair();
		for (int i = 0; i < LoopbackTransport.MAX_PENDING_FRAMES; i++) {
			pair.left().send(frame("pending " + i));
		}
		assertThrows(IOException.class,
				() -> pair.left().send(frame("one too many")),
				"the pending cap fails fast instead of hanging");
		// draining one frame reopens the window
		assertArrayEquals(frame("pending 0"), pair.right().receive());
		pair.left().send(frame("fits again"));
		for (int i = 1; i < LoopbackTransport.MAX_PENDING_FRAMES; i++) {
			assertArrayEquals(frame("pending " + i),
					pair.right().receive(),
					"no pending frame was lost to the cap rejection");
		}
		assertArrayEquals(frame("fits again"), pair.right().receive());
	}

	@Test
	void framesAreDefensiveCopies() throws Exception {
		LoopbackTransport.Pair pair = LoopbackTransport.pair();
		byte[] reused = frame("original");
		pair.left().send(reused);
		reused[0] = '!';
		assertArrayEquals(frame("original"), pair.right().receive(),
				"mutating the caller's buffer after send cannot alter"
						+ " the frame in flight");
	}

	@Test
	void anInterruptedReceiveRestoresTheInterruptStatus()
			throws Exception {
		LoopbackTransport.Pair pair = LoopbackTransport.pair();
		ExecutorService pool = Executors.newSingleThreadExecutor();
		try {
			Future<Boolean> outcome = pool.submit(() -> {
				try {
					pair.right().receive();
					return false;
				} catch (IOException expected) {
					return Thread.currentThread().isInterrupted();
				}
			});
			Thread.sleep(50);
			pool.shutdownNow(); // interrupts the parked receive
			assertTrue(outcome.get(10, TimeUnit.SECONDS),
					"the interrupt surfaces as an IOException with the"
							+ " thread's interrupt status restored");
		} finally {
			pool.shutdownNow();
		}
	}

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

	/**
	 * Send a frame, yielding and retrying while the peer's pending
	 * cap is full. The loopback pair fails fast where TCP would block,
	 * so a producer outrunning its consumer converts the rejection
	 * into cooperative backpressure here; any other failure
	 * propagates.
	 *
	 * @param transport The endpoint to send on.
	 * @param payload The frame to send.
	 *
	 * @throws Exception if the send fails for any reason but a full
	 *             pending cap.
	 */
	private static void sendYieldingOnBackpressure(Transport transport,
			byte[] payload) throws Exception {

		while (true) {
			try {
				transport.send(payload);
				return;
			} catch (IOException maybeFull) {
				String reason = String.valueOf(maybeFull.getMessage());
				if (!reason.contains("frames pending")) {
					throw maybeFull;
				}
				Thread.yield();
			}
		}
	} // end of sendYieldingOnBackpressure method

} // end of LoopbackTransportTest class
