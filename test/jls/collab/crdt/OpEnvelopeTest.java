package jls.collab.crdt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import jls.collab.op.CircuitOp;
import jls.collab.op.OpRejected;
import jls.collab.op.ToggleWatched;
import jls.elem.ElementId;

/**
 * The envelope frame (issue #171): serialization is deterministic and
 * {@code read} is its exact inverse through the real op frame path;
 * the reader rejects malformed input rather than repairing it (the
 * issue #170 network-surface discipline); and the envelope invariant —
 * the clock's origin counter equals the sequence number — holds at
 * construction and at parse.
 */
class OpEnvelopeTest {

	/** A representative op payload addressed by stable id. */
	private static final CircuitOp OP =
			new ToggleWatched(ElementId.legacy(7));

	/**
	 * A well-formed two-peer envelope: bob's op number 2, emitted after
	 * bob observed three ops from alice.
	 *
	 * @return the envelope.
	 */
	private static OpEnvelope sample() {

		return new OpEnvelope(new OpId("bob", 2),
				VectorClock.of(Map.of("alice", 3L, "bob", 2L)), OP);
	} // end of sample method

	/**
	 * Serialize an envelope to text.
	 *
	 * @param envelope The envelope.
	 *
	 * @return the frame bytes as a string.
	 */
	private static String saved(OpEnvelope envelope) {

		StringWriter text = new StringWriter();
		try (PrintWriter output = new PrintWriter(text)) {
			envelope.save(output);
		}
		return text.toString().replace(System.lineSeparator(), "\n");
	} // end of saved method

	/** Save then read returns an equal envelope with equal bytes. */
	@Test
	void saveAndReadAreExactInverses() throws Exception {

		OpEnvelope envelope = sample();
		String bytes = saved(envelope);
		OpEnvelope reread = OpEnvelope.read(new Scanner(bytes));
		assertEquals(envelope, reread);
		assertEquals(bytes, saved(reread),
				"the same envelope always writes the same bytes");
	} // end of saveAndReadAreExactInverses method

	/** The frame is the documented grammar, clock sorted by peer. */
	@Test
	void frameMatchesTheDocumentedGrammar() {

		assertEquals(String.join("\n",
				"ENVELOPE",
				" String origin \"bob\"",
				" long seq 2",
				" long peers 2",
				" clock alice 3",
				" clock bob 2",
				"OP ToggleWatched",
				" String id \"legacy:7\"",
				"END",
				"END ENVELOPE") + "\n", saved(sample()));
	} // end of frameMatchesTheDocumentedGrammar method

	/** The constructor enforces the clock/sequence invariant. */
	@Test
	void constructorRejectsClockSequenceMismatch() {

		assertThrows(IllegalArgumentException.class,
				() -> new OpEnvelope(new OpId("bob", 3),
						VectorClock.of(Map.of("bob", 2L)), OP));
		assertThrows(IllegalArgumentException.class,
				() -> new OpEnvelope(new OpId("bob", 1),
						VectorClock.EMPTY, OP));
		assertThrows(IllegalArgumentException.class,
				() -> new OpEnvelope(new OpId("bob", 1),
						VectorClock.of(Map.of("bob", 1L)), null));
	} // end of constructorRejectsClockSequenceMismatch method

	/** Op ids validate their components. */
	@Test
	void opIdRejectsMalformedComponents() {

		assertThrows(IllegalArgumentException.class,
				() -> new OpId("BOB", 1));
		assertThrows(IllegalArgumentException.class,
				() -> new OpId("bob", 0));
		assertThrows(IllegalArgumentException.class,
				() -> new OpId("bob", -4));
	} // end of opIdRejectsMalformedComponents method

	/** Op ids order by origin then sequence, deterministically. */
	@Test
	void opIdsOrderByOriginThenSequence() {

		OpId a1 = new OpId("alice", 1);
		OpId a2 = new OpId("alice", 2);
		OpId b1 = new OpId("bob", 1);
		assertTrue(a1.compareTo(a2) < 0);
		assertTrue(a2.compareTo(b1) < 0);
		assertEquals(0, a1.compareTo(new OpId("alice", 1)));
		assertEquals("alice#1", a1.toString());
	} // end of opIdsOrderByOriginThenSequence method

	/** Every malformed frame is a rejection, never a repair. */
	@Test
	void readerRejectsMalformedFrames() {

		String good = saved(sample());
		String[] malformed = {
				// wrong header
				good.replace("ENVELOPE\n String", "ENVELOP\n String"),
				// origin not in the peer alphabet
				good.replace("origin \"bob\"", "origin \"BOB\""),
				// non-canonical sequence number
				good.replace(" long seq 2", " long seq 02"),
				// negative peers count
				good.replace(" long peers 2", " long peers -1"),
				// peers count understates the clock lines
				good.replace(" long peers 2", " long peers 1"),
				// peers count overstates the clock lines
				good.replace(" long peers 2", " long peers 3"),
				// clock peers out of ascending order
				good.replace(" clock alice 3\n clock bob 2",
						" clock bob 2\n clock alice 3"),
				// duplicate clock peer
				good.replace(" clock alice 3", " clock bob 2"),
				// zero clock counter
				good.replace(" clock alice 3", " clock alice 0"),
				// clock counter not a number
				good.replace(" clock alice 3", " clock alice x"),
				// clock line missing its counter
				good.replace(" clock alice 3", " clock alice"),
				// origin absent from the clock
				good.replace("origin \"bob\"", "origin \"carol\""),
				// origin clock counter disagrees with seq
				good.replace(" clock bob 2", " clock bob 4"),
				// unknown op kind inside the frame
				good.replace("OP ToggleWatched", "OP Nonsense"),
				// missing trailer
				good.replace("END ENVELOPE\n", ""),
				// truncated mid-frame
				"ENVELOPE\n String origin \"bob\"\n",
				// empty input
				"",
		};
		for (String frame : malformed) {
			assertThrows(OpRejected.class,
					() -> OpEnvelope.read(new Scanner(frame)),
					() -> "accepted malformed frame:\n" + frame);
		}
	} // end of readerRejectsMalformedFrames method

	/** A clock claiming more peers than the cap is refused. */
	@Test
	void readerRejectsOversizedClocks() {

		String flooded = saved(sample())
				.replace(" long peers 2", " long peers 257");
		assertThrows(OpRejected.class,
				() -> OpEnvelope.read(new Scanner(flooded)));
	} // end of readerRejectsOversizedClocks method

	/** Reading consumes exactly one frame; a second read continues. */
	@Test
	void framesConcatenateCleanly() throws Exception {

		OpEnvelope first = sample();
		OpEnvelope second = new OpEnvelope(new OpId("bob", 3),
				VectorClock.of(Map.of("alice", 3L, "bob", 3L)), OP);
		Scanner input = new Scanner(saved(first) + saved(second));
		assertEquals(first, OpEnvelope.read(input));
		assertEquals(second, OpEnvelope.read(input));
	} // end of framesConcatenateCleanly method

} // end of OpEnvelopeTest class
