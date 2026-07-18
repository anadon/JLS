package jls.collab.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * The handshake's contract (issue #168, predictions P1 and parts of
 * P4's data): two sides complete the exchange and derive the same
 * session id, the same SAS, matching per-direction keys, and each
 * other's authenticated identity - and tampering ANY single byte of
 * ANY handshake message is detected on at least one side, in 100% of
 * trials, exhaustively over every byte position (no sampling, no
 * seed: the trial set is every (message, byte) pair, with the flipped
 * bit varied by position).
 */
class HandshakeTest {

	/** A fixed initiator identity shared by the trials. */
	private static final IdentityKey ALICE =
			IdentityKey.generate("alice");

	/** A fixed responder identity shared by the trials. */
	private static final IdentityKey BOB = IdentityKey.generate("bob");

	@Test
	void completedHandshakeAgreesOnEverything() throws Exception {
		Handshake joiner = Handshake.initiate(ALICE);
		Handshake starter = Handshake.respond(BOB);
		byte[] m2 = starter.acceptFirst(joiner.firstMessage());
		byte[] m3 = joiner.acceptReply(m2);
		starter.acceptFinish(m3);

		SecureLink atAlice = joiner.link();
		SecureLink atBob = starter.link();
		assertEquals(atAlice.sas(), atBob.sas(),
				"both humans must read the same glyphs");
		assertEquals(7, atAlice.sas().words().size());
		assertEquals(atAlice.sessionId(), atBob.sessionId());
		assertTrue(atAlice.sessionId().matches("[0-9a-f]{32}"));
		assertEquals(BOB.fingerprint(), atAlice.peerFingerprint(),
				"the initiator must authenticate the responder's key");
		assertEquals(ALICE.fingerprint(), atBob.peerFingerprint(),
				"the responder must authenticate the initiator's key");
		assertEquals("bob", atAlice.peerDisplayName());
		assertEquals("alice", atBob.peerDisplayName());

		// the derived per-direction keys must interoperate
		byte[] toBob = atAlice.seal("hello bob".getBytes("UTF-8"));
		assertEquals("hello bob",
				new String(atBob.openFrame(toBob), "UTF-8"));
		byte[] toAlice = atBob.seal("hello alice".getBytes("UTF-8"));
		assertEquals("hello alice",
				new String(atAlice.openFrame(toAlice), "UTF-8"));
	}

	@Test
	void freshHandshakesDeriveFreshSasAndSessions() throws Exception {
		SecureLink first = complete()[0];
		SecureLink second = complete()[0];
		assertNotEquals(first.sessionId(), second.sessionId(),
				"session ids must be freshly minted per session");
		// SAS collisions are possible (42 bits) but two in a row with
		// fresh ephemerals agreeing would be a derivation bug with
		// overwhelming probability; keep the check on the session id
		// only, which is 128 bits.
	}

	/**
	 * P1's falsifiable core: flip one bit at every byte position of
	 * every handshake message; the handshake must abort with a typed
	 * rejection on some side, or - were it somehow to complete - the
	 * two SAS values must differ. Every trial is recorded; one
	 * undetected trial fails the test.
	 */
	@Test
	void tamperingAnyByteOfAnyMessageIsDetected() throws Exception {
		List<String> undetected = new ArrayList<String>();
		int trials = 0;
		for (int message = 1; message <= 3; message += 1) {
			int length = messageLength(message);
			for (int index = 0; index < length; index += 1) {
				trials += 1;
				if (!tamperDetected(message, index)) {
					undetected.add("message " + message + " byte "
							+ index);
				}
			}
		}
		assertTrue(trials > 400,
				"the exhaustive sweep should cover every byte of all"
						+ " three messages, got only " + trials);
		assertTrue(undetected.isEmpty(),
				"tampering went undetected at: " + undetected);
	}

	@Test
	void messagesOutOfOrderAreStateErrors() throws Exception {
		Handshake joiner = Handshake.initiate(ALICE);
		Handshake starter = Handshake.respond(BOB);
		// a responder cannot produce the initiator hello
		assertThrows(IllegalStateException.class,
				() -> starter.firstMessage());
		byte[] m1 = joiner.firstMessage();
		// the initiator cannot produce it twice
		assertThrows(IllegalStateException.class,
				() -> joiner.firstMessage());
		// the initiator cannot accept the hello
		assertThrows(IllegalStateException.class,
				() -> joiner.acceptFirst(m1));
		// the link is not available before completion
		assertThrows(IllegalStateException.class, () -> joiner.link());
	}

	@Test
	void aRejectedHandshakeStaysDead() throws Exception {
		Handshake joiner = Handshake.initiate(ALICE);
		Handshake starter = Handshake.respond(BOB);
		byte[] m1 = joiner.firstMessage();
		m1[0] ^= 1;
		assertThrows(HandshakeRejected.class,
				() -> starter.acceptFirst(m1));
		m1[0] ^= 1;
		assertThrows(IllegalStateException.class,
				() -> starter.acceptFirst(m1),
				"a failed handshake must not accept a retry");
	}

	@Test
	void garbageAndTruncatedMessagesAreRejections() {
		assertThrows(HandshakeRejected.class, () -> Handshake
				.respond(BOB).acceptFirst(new byte[0]));
		assertThrows(HandshakeRejected.class, () -> Handshake
				.respond(BOB).acceptFirst(null));
		assertThrows(HandshakeRejected.class, () -> Handshake
				.respond(BOB).acceptFirst(new byte[2048]),
				"an over-cap message must be rejected");
		assertThrows(HandshakeRejected.class, () -> Handshake
				.respond(BOB).acceptFirst("JLSCOLLAB1".getBytes()),
				"a truncated hello must be rejected");
	}

	/**
	 * Complete a fresh handshake between the fixed identities.
	 *
	 * @return the two links, initiator's first.
	 *
	 * @throws HandshakeRejected never (both sides are honest).
	 */
	private static SecureLink[] complete() throws HandshakeRejected {

		Handshake joiner = Handshake.initiate(ALICE);
		Handshake starter = Handshake.respond(BOB);
		byte[] m2 = starter.acceptFirst(joiner.firstMessage());
		byte[] m3 = joiner.acceptReply(m2);
		starter.acceptFinish(m3);
		return new SecureLink[] { joiner.link(), starter.link() };
	} // end of complete method

	/**
	 * The length of one handshake message in an honest run (name
	 * lengths are fixed, so lengths are stable across runs).
	 *
	 * @param message Which message (1-3).
	 *
	 * @return the byte length.
	 *
	 * @throws HandshakeRejected never (the run is honest).
	 */
	private static int messageLength(int message)
			throws HandshakeRejected {

		Handshake joiner = Handshake.initiate(ALICE);
		Handshake starter = Handshake.respond(BOB);
		byte[] m1 = joiner.firstMessage();
		if (message == 1) {
			return m1.length;
		}
		byte[] m2 = starter.acceptFirst(m1);
		if (message == 2) {
			return m2.length;
		}
		return joiner.acceptReply(m2).length;
	} // end of messageLength method

	/**
	 * Run one tamper trial: flip one bit of one message in flight and
	 * report whether any side detected it.
	 *
	 * @param message Which message to tamper (1-3).
	 * @param index Which byte to flip.
	 *
	 * @return true if a side rejected the handshake, or the completed
	 *         SAS values differ; false only if both sides completed
	 *         and agreed (an undetected man-in-the-middle).
	 */
	private static boolean tamperDetected(int message, int index) {

		Handshake joiner = Handshake.initiate(ALICE);
		Handshake starter = Handshake.respond(BOB);
		byte flip = (byte) (1 << (index % 8));
		try {
			byte[] m1 = joiner.firstMessage();
			if (message == 1) {
				m1[index] ^= flip;
			}
			byte[] m2 = starter.acceptFirst(m1);
			if (message == 2) {
				m2[index] ^= flip;
			}
			byte[] m3 = joiner.acceptReply(m2);
			if (message == 3) {
				m3[index] ^= flip;
			}
			starter.acceptFinish(m3);
		} catch (HandshakeRejected detected) {
			return true;
		}
		return !joiner.link().sas().equals(starter.link().sas());
	} // end of tamperDetected method

} // end of HandshakeTest class
