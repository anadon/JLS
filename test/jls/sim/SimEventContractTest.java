package jls.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

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
		public void react(long now, Simulator sim, Object todo) {
		}
	} // end of NoOp class

	/** The shared callback for events that must compare equal. */
	private static final Reacts CALLBACK = new NoOp();

	@Test
	void earlierTimeOrdersFirst() {

		SimEvent early = new SimEvent(1, CALLBACK, null);
		SimEvent late = new SimEvent(2, CALLBACK, null);
		assertEquals(-1, early.compareTo(late));
		assertEquals(1, late.compareTo(early));
	}

	@Test
	void equalTimesOrderByCreationSequence() {

		SimEvent first = new SimEvent(5, CALLBACK, null);
		SimEvent second = new SimEvent(5, CALLBACK, null);
		assertEquals(-1, first.compareTo(second),
				"same-time events must pop in creation order");
		assertEquals(1, second.compareTo(first));
	}

	@Test
	void selfComparisonIsZero() {

		SimEvent event = new SimEvent(7, CALLBACK, null);
		assertEquals(0, event.compareTo(event));
	}

	@Test
	void equalsRequiresTimeCallbackAndTodo() {

		SimEvent event = new SimEvent(3, CALLBACK, "todo");
		assertTrue(event.equals(new SimEvent(3, CALLBACK, "todo")),
				"same time, callback, and todo must be equal");
		assertFalse(event.equals(new SimEvent(4, CALLBACK, "todo")),
				"a different time must not be equal");
		assertFalse(event.equals(new SimEvent(3, new NoOp(), "todo")),
				"a different callback must not be equal");
		assertFalse(event.equals(new SimEvent(3, CALLBACK, "other")),
				"a different todo must not be equal");
		assertFalse(event.equals("not a SimEvent"),
				"a non-SimEvent must not be equal");
	}

	@Test
	void nullTodoEqualsNullTodoOnly() {

		SimEvent bare = new SimEvent(3, CALLBACK, null);
		assertTrue(bare.equals(new SimEvent(3, CALLBACK, null)));
		assertFalse(bare.equals(new SimEvent(3, CALLBACK, "todo")));
	}

	@Test
	void hashCodeIsTheEventTime() {

		// documented contract: the hash code is the event time
		assertEquals(42, new SimEvent(42, CALLBACK, null).hashCode());
		SimEvent a = new SimEvent(9, CALLBACK, "x");
		SimEvent b = new SimEvent(9, CALLBACK, "x");
		assertEquals(a.hashCode(), b.hashCode(),
				"equal events must hash equally");
	}

} // end of SimEventContractTest class
