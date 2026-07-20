package jls.elem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import jls.Circuit;

/**
 * The capability-interface contract for issue #78: the boolean-gated
 * capability pairs on {@link Element} are being replaced by capability
 * interfaces that an element implements once, in the type system, so
 * that call sites {@code instanceof}-check the capability instead of the
 * base class branching on concrete leaf types (the audit's "base knows
 * its leaves" smell).
 *
 * <p>This test pins the equivalence between each capability interface
 * and the predicate it supersedes, so that adding a new element - or
 * flipping a predicate - without also declaring (or removing) the
 * interface is a build failure rather than a silent divergence. It is
 * the enforcement half of the same discipline the registry-integrity
 * test provides for loadability: the registry is walked as the total,
 * authoritative list of loadable element types.
 *
 * <p>Interfaces covered this wave: {@link Timed} (supersedes
 * {@code hasTiming()} + the {@code getDelay()}/{@code setDelay()} pair,
 * and retires the {@code instanceof Memory} branch in
 * {@link Element#changeTiming()}) and {@link Watchable} (supersedes
 * {@code canWatch()} + the watch-state accessors). The remaining
 * capabilities named on #78 - {@code Rotatable}, {@code Editable},
 * {@code QuickEditable} - are deferred: {@code canRotate()} is a
 * runtime attachment gate rather than a class-level capability, and the
 * other two carry Swing/editor method signatures that cannot live on a
 * headless-core interface until the GUI method moves out (issue #77's
 * renderer split). See the class comment on {@link Timed}.
 */
class CapabilityInterfaceTest {

	/**
	 * An element implements {@link Timed} exactly when it reports
	 * {@code hasTiming()} - no timed element lacks the interface, and no
	 * non-timed element carries it.
	 */
	@Test
	void timedInterfaceMatchesHasTiming() {

		Circuit circuit = new Circuit("capabilitycheck");
		for (ElementType type : ElementRegistry.all()) {
			Element made = type.create(circuit);
			assertEquals(made.hasTiming(), made instanceof Timed,
					"tag '" + type.tag() + "' (" + made.getClass()
							.getSimpleName() + "): hasTiming()=="
							+ made.hasTiming() + " but instanceof Timed=="
							+ (made instanceof Timed)
							+ "; a timed element must implement Timed "
							+ "(issue #78 capability interfaces)");
		}
	}

	/**
	 * An element implements {@link Watchable} exactly when it reports
	 * {@code canWatch()}.
	 */
	@Test
	void watchableInterfaceMatchesCanWatch() {

		Circuit circuit = new Circuit("capabilitycheck");
		for (ElementType type : ElementRegistry.all()) {
			Element made = type.create(circuit);
			assertEquals(made.canWatch(), made instanceof Watchable,
					"tag '" + type.tag() + "' (" + made.getClass()
							.getSimpleName() + "): canWatch()=="
							+ made.canWatch() + " but instanceof Watchable=="
							+ (made instanceof Watchable)
							+ "; a watchable element must implement Watchable "
							+ "(issue #78 capability interfaces)");
		}
	}

	/**
	 * Among timed elements, {@code usesAccessTime()} is true for exactly
	 * the memory element - the distinction that used to be an
	 * {@code instanceof Memory} branch in {@link Element#changeTiming()}.
	 */
	@Test
	void onlyMemoryUsesAccessTime() {

		Circuit circuit = new Circuit("capabilitycheck");
		boolean sawMemory = false;
		for (ElementType type : ElementRegistry.all()) {
			Element made = type.create(circuit);
			if (!(made instanceof Timed timed)) {
				continue;
			}
			boolean isMemory = made instanceof Memory;
			assertEquals(isMemory, timed.usesAccessTime(),
					"tag '" + type.tag() + "' (" + made.getClass()
							.getSimpleName() + "): usesAccessTime() must be "
							+ "true only for the memory element");
			sawMemory |= isMemory;
		}
		assertTrue(sawMemory,
				"the Memory element must be registered and Timed");
	}

	/**
	 * A sanity floor on the interfaces: the memory element carries both
	 * capabilities (it is timed with access-time semantics and watchable),
	 * and a pure combinational gate is timed but not watchable.
	 */
	@Test
	void representativeElementsCarryTheExpectedCapabilities() {

		Circuit circuit = new Circuit("capabilitycheck");
		Element memory = ElementRegistry.forTag("Memory").create(circuit);
		assertTrue(memory instanceof Timed, "Memory must be Timed");
		assertTrue(memory instanceof Watchable, "Memory must be Watchable");
		assertTrue(((Timed) memory).usesAccessTime(),
				"Memory must use access-time semantics");

		Element andGate = ElementRegistry.forTag("AndGate").create(circuit);
		assertTrue(andGate instanceof Timed, "AndGate must be Timed");
		assertFalse(andGate instanceof Watchable,
				"a plain gate is not watchable");
		assertFalse(((Timed) andGate).usesAccessTime(),
				"a gate uses propagation delay, not access time");
	}

} // end of CapabilityInterfaceTest class
