package jls.collab.net;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * The frame layer's hostile-input contract (issue #168, prediction
 * P2): every frame in a corpus of oversized, truncated, replayed,
 * reordered, tampered, and garbage frames is rejected with the typed
 * {@link FrameRejected}, an over-cap length is rejected before a
 * single body byte is read or buffered, a failed link stays failed,
 * and the corpus run creates no threads.
 */
class SecureLinkTest {

	/** The initiator identity for link setup. */
	private static final IdentityKey ALICE =
			IdentityKey.generate("alice");

	/** The responder identity for link setup. */
	private static final IdentityKey BOB = IdentityKey.generate("bob");

	@Test
	void payloadsRoundTripInBothDirections() throws Exception {
		SecureLink[] links = freshLinks();
		byte[] payload = "a frame of opaque bytes"
				.getBytes(StandardCharsets.UTF_8);
		assertArrayEquals(payload,
				links[1].openFrame(links[0].seal(payload)));
		assertArrayEquals(payload,
				links[0].openFrame(links[1].seal(payload)));
		assertArrayEquals(new byte[0],
				links[1].openFrame(links[0].seal(new byte[0])),
				"an empty payload is a legal frame");
	}

	@Test
	void streamReadingDeliversFramesInOrder() throws Exception {
		SecureLink[] links = freshLinks();
		byte[] first = links[0].seal("first".getBytes("UTF-8"));
		byte[] second = links[0].seal("second".getBytes("UTF-8"));
		byte[] wire = new byte[first.length + second.length];
		System.arraycopy(first, 0, wire, 0, first.length);
		System.arraycopy(second, 0, wire, first.length, second.length);
		InputStream stream = new ByteArrayInputStream(wire);
		assertEquals("first", new String(links[1].open(stream), "UTF-8"));
		assertEquals("second", new String(links[1].open(stream), "UTF-8"));
		assertNull(links[1].open(stream),
				"a clean end between frames reads as null");
	}

	@Test
	void hostileCorpusIsRejectedWithTypedErrors() throws Exception {
		int threadsBefore = Thread.activeCount();

		// oversized declared length
		SecureLink[] links = freshLinks();
		byte[] oversized = ByteBuffer.allocate(8)
				.putInt(SecureLink.MAX_PAYLOAD_BYTES + 17).putInt(0)
				.array();
		assertThrows(FrameRejected.class, () -> links[1]
				.openFrame(oversized));

		// truncated wire bytes
		SecureLink[] truncated = freshLinks();
		byte[] whole = truncated[0].seal("payload".getBytes("UTF-8"));
		byte[] cut = new byte[whole.length - 1];
		System.arraycopy(whole, 0, cut, 0, cut.length);
		assertThrows(FrameRejected.class, () -> truncated[1]
				.open(new ByteArrayInputStream(cut)));

		// garbage that was never a frame
		SecureLink[] garbage = freshLinks();
		byte[] noise = ByteBuffer.allocate(24).putInt(20)
				.putLong(0x6a6c73L).putLong(0x6e6f697365L).putInt(7)
				.array();
		assertThrows(FrameRejected.class, () -> garbage[1]
				.openFrame(noise));

		// a replayed frame
		SecureLink[] replayed = freshLinks();
		byte[] once = replayed[0].seal("once".getBytes("UTF-8"));
		replayed[1].openFrame(once);
		assertThrows(FrameRejected.class, () -> replayed[1]
				.openFrame(once), "a replayed frame must be rejected");

		// frames delivered out of order
		SecureLink[] reordered = freshLinks();
		reordered[0].seal("first".getBytes("UTF-8"));
		byte[] second = reordered[0].seal("second".getBytes("UTF-8"));
		assertThrows(FrameRejected.class, () -> reordered[1]
				.openFrame(second),
				"a skipped-ahead frame must be rejected");

		// a declared length shorter than the tag itself
		SecureLink[] short1 = freshLinks();
		byte[] tiny = ByteBuffer.allocate(9).putInt(5).array();
		assertThrows(FrameRejected.class, () -> short1[1]
				.openFrame(tiny));

		assertTrue(Thread.activeCount() <= threadsBefore,
				"rejecting hostile frames must not create threads");
	}

	@Test
	void everyTamperedByteOfAFrameIsRejected() throws Exception {
		byte[] payload = "attribution matters"
				.getBytes(StandardCharsets.UTF_8);
		int length = freshLinks()[0].seal(payload).length;
		for (int index = 4; index < length; index += 1) {
			SecureLink[] links = freshLinks();
			byte[] frame = links[0].seal(payload);
			frame[index] ^= (byte) (1 << (index % 8));
			final byte[] tampered = frame;
			assertThrows(FrameRejected.class,
					() -> links[1].openFrame(tampered),
					"flipping ciphertext byte " + index
							+ " must be rejected");
		}
	}

	@Test
	void anOversizedLengthIsRejectedBeforeAnyBodyRead()
			throws Exception {
		SecureLink[] links = freshLinks();
		byte[] prefix = ByteBuffer.allocate(4)
				.putInt(Integer.MAX_VALUE).array();
		InputStream trap = new InputStream() {
			/** How many bytes have been served so far. */
			private int served;

			@Override
			public int read() {
				if (served >= 4) {
					fail("the frame body was read despite an over-cap"
							+ " declared length");
				}
				int next = prefix[served] & 0xff;
				served += 1;
				return next;
			}
		};
		assertThrows(FrameRejected.class, () -> links[1].open(trap));
	}

	@Test
	void aFailedLinkStaysFailed() throws Exception {
		SecureLink[] links = freshLinks();
		byte[] good = links[0].seal("good".getBytes("UTF-8"));
		byte[] bad = good.clone();
		bad[bad.length - 1] ^= 1;
		assertThrows(FrameRejected.class, () -> links[1].openFrame(bad));
		assertTrue(links[1].isPoisoned());
		assertThrows(FrameRejected.class, () -> links[1]
				.openFrame(good),
				"a poisoned link must reject even valid frames");
		assertThrows(FrameRejected.class, () -> links[1]
				.seal("out".getBytes("UTF-8")),
				"a poisoned link must reject sends too");
	}

	@Test
	void overCapPayloadsCannotBeSealed() throws Exception {
		SecureLink[] links = freshLinks();
		assertThrows(FrameRejected.class, () -> links[0]
				.seal(new byte[SecureLink.MAX_PAYLOAD_BYTES + 1]));
		// and the boundary itself is legal
		SecureLink[] fresh = freshLinks();
		byte[] max = new byte[SecureLink.MAX_PAYLOAD_BYTES];
		assertArrayEquals(max, fresh[1].openFrame(fresh[0].seal(max)));
	}

	/**
	 * Complete a fresh handshake and hand back the two links.
	 *
	 * @return the links, initiator's first.
	 *
	 * @throws HandshakeRejected never (both sides are honest).
	 */
	private static SecureLink[] freshLinks() throws HandshakeRejected {

		Handshake joiner = Handshake.initiate(ALICE);
		Handshake starter = Handshake.respond(BOB);
		byte[] m2 = starter.acceptFirst(joiner.firstMessage());
		byte[] m3 = joiner.acceptReply(m2);
		starter.acceptFinish(m3);
		return new SecureLink[] { joiner.link(), starter.link() };
	} // end of freshLinks method

} // end of SecureLinkTest class
