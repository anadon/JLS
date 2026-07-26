package jls.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.BitSet;

import org.junit.jupiter.api.Test;

import jls.BitSetUtils;
import jls.sim.SimEvent.MemoryWrite;
import jls.sim.SimEvent.NewValue;
import jls.sim.SimEvent.PinChanged;

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
 * SimEvent into a record or makes equals structural.  The todo is the
 * sealed typed payload (issue #95); the payload records' structural
 * equality is what makes two same-shaped postings coalesce.</p>
 */
class SimEventDedupTest {

	/** A callback that counts how many times it reacts. */
	private static final class Recorder implements Reacts {

		int reacts = 0;

		@Override
		public void initSim(Simulator sim) {
		}

		@Override
		public void react(long now, Simulator sim, SimEvent.Payload todo) {
			reacts += 1;
		}
	}

	/** A minimal concrete Simulator exposing the shared event loop. */
	private static final class PlainSimulator extends Simulator {

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
	 * Build a NewValue payload carrying the given small value.
	 *
	 * @param value The value the payload's BitSet should hold.
	 *
	 * @return the payload.
	 */
	private static NewValue value(long value) {

		return new NewValue(BitSet.valueOf(new long[] { value }));
	}

	/**
	 * Two events with the same (time, callBack, todo) but different
	 * sequence numbers are equal with equal hash codes -- the dedup
	 * contract -- while compareTo still tells them apart by seq.
	 */
	@Test
	void equalsIgnoresSeqButCompareToDoesNot() {

		Recorder cb = new Recorder();
		SimEvent first = new SimEvent(5, cb, value(1));
		SimEvent second = new SimEvent(5, cb, value(1));

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
		sim.post(new SimEvent(3, cb, new PinChanged()));
		sim.post(new SimEvent(3, cb, new PinChanged()));

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
		sim.post(new SimEvent(3, cb, value(1)));
		sim.post(new SimEvent(3, cb, value(2)));

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
		sim.post(new SimEvent(3, cb, new PinChanged()));
		sim.run();
		assertEquals(1, cb.reacts, "the first posting reacts");

		sim.post(new SimEvent(3, cb, new PinChanged()));
		assertEquals(1, sim.eventQueue.size(),
				"an equal event must be accepted after the dequeue");
		sim.run();
		assertEquals(2, cb.reacts, "the re-posted event reacts too");
	}

	/**
	 * Payload kinds that replaced identity-equality inner classes
	 * (Memory's MemAction, TruthTable's Out) coalesce structurally
	 * like every other payload: two same-shaped pending memory writes
	 * are one event.  Under the pre-#95 identity classes both would
	 * have fired; the records make dedup uniform across all payload
	 * kinds.  The duplicate op was idempotent, so simulated values are
	 * unchanged -- only duplicate activity-history entries and trace
	 * samples disappear.  This test pins that behavior change as
	 * intentional (issue #95).
	 */
	@Test
	void formerIdentityPayloadsNowCoalesceStructurally() {

		PlainSimulator sim = new PlainSimulator();
		Recorder cb = new Recorder();
		sim.post(new SimEvent(4, cb,
				new MemoryWrite(9, BitSet.valueOf(new long[] { 5 }))));
		sim.post(new SimEvent(4, cb,
				new MemoryWrite(9, BitSet.valueOf(new long[] { 5 }))));

		assertEquals(1, sim.eventQueue.size(),
				"same-shaped memory writes must coalesce (issue #95)");

		sim.run();
		assertEquals(1, cb.reacts, "one surviving event, one react");
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
			public void react(long now, Simulator sim,
					SimEvent.Payload todo) {
				order.append(BitSetUtils.ToInt(
						((NewValue)todo).value()));
			}
		};
		sim.post(new SimEvent(7, logger, value(3)));
		sim.post(new SimEvent(2, logger, value(1)));
		sim.post(new SimEvent(2, logger, value(2)));

		sim.run();
		assertTrue(order.toString().equals("123"),
				"expected time-then-seq order 123, got " + order);
	}

} // end of SimEventDedupTest class
