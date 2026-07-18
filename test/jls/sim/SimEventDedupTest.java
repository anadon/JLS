package jls.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Pins the intentionally non-structural equality of {@link SimEvent}
 * and the duplicate-event coalescing built on it (issue #94).
 *
 * <p>SimEvent is an immutable value carrier, but it is deliberately
 * <em>not</em> a record: {@code equals} compares {@code (time,
 * callBack, todo)} and excludes the always-unique {@code seq}, while
 * {@code compareTo} orders by {@code (time, seq)}.  A record's
 * generated structural {@code equals} would include {@code seq},
 * making every event distinct and silently disabling the simulator's
 * {@code dupCheck} dedup set.  These tests fail if anyone "cleans up"
 * SimEvent into a record or makes equals structural.</p>
 */
class SimEventDedupTest {

	/** A callback that counts how many times it reacts. */
	private static final class Recorder implements Reacts {

		int reacts = 0;

		@Override
		public void initSim(Simulator sim) {
		}

		@Override
		public void react(long now, Simulator sim, Object todo) {
			reacts += 1;
		}
	}

	/** A minimal concrete Simulator exposing the shared event loop. */
	private static final class PlainSimulator extends Simulator {

		/** Create the simulator and set its self-reference. */
		PlainSimulator() {
			me = this;
		}

		/** Run the shared event loop. */
		void run() {
			runEventLoop();
		}

		@Override
		public void stop() {
		}

		@Override
		public void pause(boolean which) {
		}
	}

	/**
	 * Two events with the same (time, callBack, todo) but different
	 * sequence numbers are equal with equal hash codes -- the dedup
	 * contract -- while compareTo still tells them apart by seq.
	 */
	@Test
	void equalsIgnoresSeqButCompareToDoesNot() {

		Recorder cb = new Recorder();
		SimEvent first = new SimEvent(5, cb, "todo");
		SimEvent second = new SimEvent(5, cb, "todo");

		assertEquals(first, second,
				"same (time, callBack, todo) must be equal");
		assertEquals(first.hashCode(), second.hashCode(),
				"equal events must have equal hash codes");
		assertNotEquals(0, first.compareTo(second),
				"compareTo must still order by the unique seq");
	}

	/**
	 * The decided regression (issue #94 comment): posting two events
	 * with the same (time, callBack, todo) and different seq leaves
	 * exactly one in the queue, and it reacts exactly once.
	 */
	@Test
	void duplicatePostingCoalescesToOneEvent() {

		PlainSimulator sim = new PlainSimulator();
		Recorder cb = new Recorder();
		sim.post(new SimEvent(3, cb, null));
		sim.post(new SimEvent(3, cb, null));

		assertEquals(1, sim.eventQueue.size(),
				"the duplicate posting must be coalesced");

		sim.run();
		assertEquals(1, cb.reacts, "one surviving event, one react");
	}

	/**
	 * Events that differ in their todo payload are not duplicates:
	 * both survive posting and both react.
	 */
	@Test
	void distinctTodoPayloadsBothSurvive() {

		PlainSimulator sim = new PlainSimulator();
		Recorder cb = new Recorder();
		sim.post(new SimEvent(3, cb, "a"));
		sim.post(new SimEvent(3, cb, "b"));

		assertEquals(2, sim.eventQueue.size(),
				"different todo payloads must not be coalesced");

		sim.run();
		assertEquals(2, cb.reacts, "both events must react");
	}

	/**
	 * Dequeuing an event releases its dedup entry: after the loop has
	 * consumed an event, an equal event may be posted again.
	 */
	@Test
	void dequeueReleasesTheDedupEntry() {

		PlainSimulator sim = new PlainSimulator();
		Recorder cb = new Recorder();
		sim.post(new SimEvent(3, cb, null));
		sim.run();
		assertEquals(1, cb.reacts, "the first posting reacts");

		sim.post(new SimEvent(3, cb, null));
		assertEquals(1, sim.eventQueue.size(),
				"an equal event must be accepted after the dequeue");
		sim.run();
		assertEquals(2, cb.reacts, "the re-posted event reacts too");
	}

	/**
	 * Events dequeue in (time, seq) order: earlier times first, and
	 * FIFO by sequence number within the same time.
	 */
	@Test
	void eventsDequeueInTimeThenPostingOrder() {

		PlainSimulator sim = new PlainSimulator();
		StringBuilder order = new StringBuilder();
		Reacts logger = new Reacts() {
			@Override
			public void initSim(Simulator sim) {
			}
			@Override
			public void react(long now, Simulator sim, Object todo) {
				order.append(todo);
			}
		};
		sim.post(new SimEvent(7, logger, "c"));
		sim.post(new SimEvent(2, logger, "a"));
		sim.post(new SimEvent(2, logger, "b"));

		sim.run();
		assertTrue(order.toString().equals("abc"),
				"expected time-then-seq order abc, got " + order);
	}

} // end of SimEventDedupTest class
