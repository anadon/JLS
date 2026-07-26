package jls.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.BitSet;

import org.junit.jupiter.api.Test;

import jls.sim.SimEvent.NewValue;
import jls.sim.SimEvent.PinChanged;

/**
 * Ordering and identity contract of {@link SimEvent}, pinned by the
 * PIT mutation trial (issue #161): all seven of the class's surviving
 * mutants sat in {@code compareTo}, {@code equals}, and
 * {@code hashCode} — the event queue exercised them constantly, but
 * nothing asserted their results directly.  The simulator's
 * determinism rests on exactly these methods: events at the same time
 * must pop in creation order (the {@code seq} tiebreak), or
 * simulation output would depend on {@link java.util.PriorityQueue}
 * internals.
 */
class SimEventContractTest {

	/** A callback that does nothing; only its identity matters here. */
	private static final class NoOp implements Reacts {

		@Override
		public void initSim(Simulator sim) {
		}

		@Override
		public void react(long now, Simulator sim, SimEvent.Payload todo) {
		}
	} // end of NoOp class

	/** The shared callback for events that must compare equal. */
	private static final Reacts CALLBACK = new NoOp();

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

	@Test
	void earlierTimeOrdersFirst() {

		SimEvent early = new SimEvent(1, CALLBACK, new PinChanged());
		SimEvent late = new SimEvent(2, CALLBACK, new PinChanged());
		assertEquals(-1, early.compareTo(late));
		assertEquals(1, late.compareTo(early));
	}

	@Test
	void equalTimesOrderByCreationSequence() {

		SimEvent first = new SimEvent(5, CALLBACK, new PinChanged());
		SimEvent second = new SimEvent(5, CALLBACK, new PinChanged());
		assertEquals(-1, first.compareTo(second),
				"same-time events must pop in creation order");
		assertEquals(1, second.compareTo(first));
	}

	@Test
	void selfComparisonIsZero() {

		SimEvent event = new SimEvent(7, CALLBACK, new PinChanged());
		assertEquals(0, event.compareTo(event));
	}

	@Test
	void equalsRequiresTimeCallbackAndTodo() {

		SimEvent event = new SimEvent(3, CALLBACK, value(1));
		assertTrue(event.equals(new SimEvent(3, CALLBACK, value(1))),
				"same time, callback, and todo must be equal");
		assertFalse(event.equals(new SimEvent(4, CALLBACK, value(1))),
				"a different time must not be equal");
		assertFalse(event.equals(new SimEvent(3, new NoOp(), value(1))),
				"a different callback must not be equal");
		assertFalse(event.equals(new SimEvent(3, CALLBACK, value(2))),
				"a different todo must not be equal");
		assertFalse(event.equals("not a SimEvent"),
				"a non-SimEvent must not be equal");
	}

	@Test
	void pinChangedTodoEqualsPinChangedTodoOnly() {

		SimEvent bare = new SimEvent(3, CALLBACK, new PinChanged());
		assertTrue(bare.equals(new SimEvent(3, CALLBACK, new PinChanged())));
		assertFalse(bare.equals(new SimEvent(3, CALLBACK, value(1))));
	}

	@Test
	void hashCodeIsTheEventTime() {

		// documented contract: the hash code is the event time
		assertEquals(42,
				new SimEvent(42, CALLBACK, new PinChanged()).hashCode());
		SimEvent a = new SimEvent(9, CALLBACK, value(1));
		SimEvent b = new SimEvent(9, CALLBACK, value(1));
		assertEquals(a.hashCode(), b.hashCode(),
				"equal events must hash equally");
	}

} // end of SimEventContractTest class
