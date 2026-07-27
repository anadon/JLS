package jls.collab.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * The chaos decorator's own contract (issues #163/#168, the G3/G4
 * apparatus): every fate is a pure function of the seed, dropped
 * frames are counted and everything else arrives, duplicates are
 * exact copies, reordering permutes but never loses, a partition
 * buffers frames that a heal then delivers without loss, and with
 * every probability at zero the decorator is invisible. Every
 * assertion message carries the seed, so a failing schedule replays
 * exactly.
 */
class ChaosTransportTest {

	/** The one seed all fixed-seed trials share, quoted on failure. */
	private static final long SEED = 0x1EC5_CAFEL;

	@Test
	void withAllProbabilitiesZeroTheDecoratorIsInvisible()
			throws Exception {
		LoopbackTransport.Pair pair = LoopbackTransport.pair();
		ChaosTransport calm =
				new ChaosTransport(pair.left(), SEED, 0.0, 0.0, 0.0);
		List<String> sent = labels("calm", 20);
		for (String label : sent) {
			calm.send(label.getBytes(StandardCharsets.UTF_8));
		}
		calm.close();
		assertEquals(sent, drain(pair.right()),
				"zero probabilities deliver exactly once, in order"
						+ " (seed " + SEED + ")");
		assertEquals(0, calm.droppedCount(), "seed " + SEED);
		assertEquals(0, calm.duplicatedCount(), "seed " + SEED);
		assertEquals(0, calm.heldBackCount(), "seed " + SEED);
	}

	@Test
	void dropsAreCountedAndEverythingElseArrives() throws Exception {
		LoopbackTransport.Pair pair = LoopbackTransport.pair();
		ChaosTransport lossy =
				new ChaosTransport(pair.left(), SEED, 0.5, 0.0, 0.0);
		List<String> sent = labels("lossy", 40);
		for (String label : sent) {
			lossy.send(label.getBytes(StandardCharsets.UTF_8));
		}
		lossy.close();
		List<String> received = drain(pair.right());
		assertTrue(lossy.droppedCount() > 0,
				"a 50% drop rate over 40 frames drops some (seed "
						+ SEED + ")");
		assertTrue(lossy.droppedCount() < sent.size(),
				"a 50% drop rate over 40 frames spares some (seed "
						+ SEED + ")");
		assertEquals(sent.size() - lossy.droppedCount(),
				received.size(),
				"every undropped frame arrives, no more, no less"
						+ " (seed " + SEED + ")");
		assertTrue(sent.containsAll(received),
				"chaos never invents a frame (seed " + SEED + ")");
		// survivors keep their relative order: drop-only chaos is a
		// subsequence, not a shuffle
		assertEquals(received,
				sent.stream().filter(received::contains).toList(),
				"drop-only chaos preserves survivor order (seed "
						+ SEED + ")");
	}

	@Test
	void duplicatesAreExactExtraCopies() throws Exception {
		LoopbackTransport.Pair pair = LoopbackTransport.pair();
		ChaosTransport stutter =
				new ChaosTransport(pair.left(), SEED, 0.0, 0.5, 0.0);
		List<String> sent = labels("dup", 30);
		for (String label : sent) {
			stutter.send(label.getBytes(StandardCharsets.UTF_8));
		}
		stutter.close();
		List<String> received = drain(pair.right());
		assertTrue(stutter.duplicatedCount() > 0,
				"a 50% duplicate rate over 30 frames doubles some"
						+ " (seed " + SEED + ")");
		assertEquals(sent.size() + stutter.duplicatedCount(),
				received.size(),
				"each duplicate is exactly one extra copy (seed "
						+ SEED + ")");
		assertEquals(new HashSet<>(sent), new HashSet<>(received),
				"duplication adds copies, never new frames (seed "
						+ SEED + ")");
	}

	@Test
	void reorderingPermutesButNeverLoses() throws Exception {
		LoopbackTransport.Pair pair = LoopbackTransport.pair();
		ChaosTransport shuffled =
				new ChaosTransport(pair.left(), SEED, 0.0, 0.0, 0.5);
		List<String> sent = labels("swap", 30);
		for (String label : sent) {
			shuffled.send(label.getBytes(StandardCharsets.UTF_8));
		}
		shuffled.close();
		List<String> received = drain(pair.right());
		assertTrue(shuffled.heldBackCount() > 0,
				"a 50% holdback rate over 30 frames reorders some"
						+ " (seed " + SEED + ")");
		assertEquals(sent.size(), received.size(),
				"reordering loses nothing, even at close (seed "
						+ SEED + ")");
		assertEquals(new HashSet<>(sent), new HashSet<>(received),
				"reordering is a permutation of what was sent (seed "
						+ SEED + ")");
		assertNotEquals(sent, received,
				"with this seed the permutation is not the identity"
						+ " (seed " + SEED + ")");
	}

	@Test
	void aPartitionBuffersAndAHealDeliversWithoutLoss()
			throws Exception {
		LoopbackTransport.Pair pair = LoopbackTransport.pair();
		ChaosTransport flaky =
				new ChaosTransport(pair.left(), SEED, 0.0, 0.0, 0.0);
		flaky.send(frame("before"));
		flaky.partition();
		List<String> during = labels("during", 5);
		for (String label : during) {
			flaky.send(label.getBytes(StandardCharsets.UTF_8));
		}
		// the partitioned frames must not have reached the loopback
		// queue: only the pre-partition frame is pending
		assertEquals("before", new String(pair.right().receive(),
				StandardCharsets.UTF_8));
		flaky.heal();
		flaky.send(frame("after"));
		flaky.close();
		List<String> received = drain(pair.right());
		List<String> expected = new ArrayList<>(during);
		expected.add("after");
		assertEquals(expected, received,
				"a heal flushes every partitioned frame, in order,"
						+ " before new traffic (seed " + SEED + ")");
	}

	@Test
	void theSameSeedReplaysTheSameSchedule() throws Exception {
		assertEquals(chaosSchedule(SEED), chaosSchedule(SEED),
				"one seed, one schedule (seed " + SEED + ")");
		assertNotEquals(chaosSchedule(SEED), chaosSchedule(SEED + 1),
				"these two adjacent seeds happen to differ, so the"
						+ " schedule really derives from the seed");
	}

	/**
	 * Run a fixed 60-frame send schedule under all three fates at
	 * once and return exactly what the peer received.
	 *
	 * @param seed The chaos seed to drive the schedule with.
	 *
	 * @return the received labels, in arrival order.
	 *
	 * @throws Exception if a send, receive, or close fails.
	 */
	private static List<String> chaosSchedule(long seed)
			throws Exception {

		LoopbackTransport.Pair pair = LoopbackTransport.pair();
		ChaosTransport chaos =
				new ChaosTransport(pair.left(), seed, 0.2, 0.2, 0.2);
		List<String> sent = labels("replay", 30);
		for (String label : sent) {
			// 30 sends stay well under the 64-frame pending cap even
			// with every duplicate, so no draining is needed mid-send
			chaos.send(label.getBytes(StandardCharsets.UTF_8));
		}
		chaos.close();
		return drain(pair.right());
	} // end of chaosSchedule method

	/**
	 * Receive until the clean end of the stream.
	 *
	 * @param transport The receiving endpoint.
	 *
	 * @return every received payload, decoded, in arrival order.
	 *
	 * @throws Exception if a receive fails.
	 */
	private static List<String> drain(Transport transport)
			throws Exception {

		List<String> received = new ArrayList<>();
		byte[] payload;
		while ((payload = transport.receive()) != null) {
			received.add(new String(payload, StandardCharsets.UTF_8));
		}
		return received;
	} // end of drain method

	/**
	 * Build distinct labels for one trial.
	 *
	 * @param prefix The trial's label prefix.
	 * @param count How many labels to build.
	 *
	 * @return the labels, in order.
	 */
	private static List<String> labels(String prefix, int count) {

		List<String> all = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			all.add(prefix + " " + i);
		}
		return all;
	} // end of labels method

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

} // end of ChaosTransportTest class
