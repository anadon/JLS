package jls.elem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import jls.Circuit;
import jls.CircuitTextBuilder;
import jls.JLSInfo;

/**
 * The capability-interface contract for issue #78: the boolean-gated
 * capability pairs on {@link Element} have been replaced by capability
 * interfaces that an element implements once, in the type system, so
 * that call sites {@code instanceof}-check the capability instead of the
 * base class branching on concrete leaf types (the audit's "base knows
 * its leaves" smell).
 *
 * <p>This test pins, for every registered element type, exactly which
 * capabilities it declares, so that adding a new element - or moving a
 * capability - without updating this expectation is a build failure
 * rather than a silent divergence. It is the enforcement half of the
 * same discipline the registry-integrity test provides for loadability:
 * the registry is walked as the total, authoritative list of loadable
 * element types.
 *
 * <p>Interfaces covered: {@link Timed} (supersedes {@code hasTiming()}
 * plus the delay accessors), {@link Watchable} (supersedes
 * {@code canWatch()} plus the watch-state accessors), {@link Rotatable}
 * (supersedes {@code canRotate()}/{@code canFlip()} plus
 * {@code rotate()}/{@code flip()}), {@link Editable} (supersedes
 * {@code canChange()}), and {@link QuickEditable} (supersedes
 * {@code quickChange()}). The rotate/flip predicates survive as
 * instance methods on {@link Rotatable} because they are attachment
 * gates, not constants; on a freshly created (unattached) element they
 * report the class-level capability, which this test also pins.
 */
class CapabilityInterfaceTest {

	/** The registered tags whose elements are {@link Timed}. */
	private static final Set<String> TIMED = Set.of(
			"Adder", "AndGate", "Decoder", "DelayGate", "Memory", "Mux",
			"NandGate", "NorGate", "NotGate", "OrGate", "Register",
			"ShiftRegister", "StateMachine", "TriState", "TruthTable",
			"XorGate");

	/** The registered tags whose elements are {@link Watchable}. */
	private static final Set<String> WATCHABLE = Set.of(
			"InputPin", "JumpStart", "Memory", "OutputPin", "Register");

	/** The registered tags whose elements are {@link Rotatable}. */
	private static final Set<String> ROTATABLE = Set.of(
			"Adder", "AndGate", "Binder", "Clock", "Constant", "Decoder",
			"DelayGate", "Display", "Extend", "InputPin", "JumpEnd",
			"JumpStart", "Mux", "NandGate", "NorGate", "NotGate", "OrGate",
			"OutputPin", "Register", "ShiftRegister", "Splitter",
			"SubCircuit", "TriState", "XorGate");

	/**
	 * The {@link Rotatable} tags that only flip: their
	 * {@code canRotate()} stays false even when unattached.
	 */
	private static final Set<String> FLIP_ONLY = Set.of(
			"JumpEnd", "JumpStart", "SubCircuit");

	/**
	 * The {@link Rotatable} tags whose predicates are pinned false even
	 * when unattached: the display keeps its historical rotate/flip
	 * overrides (so it stays {@code Rotatable}, faithful to the base
	 * code) but refuses both because its puts implement the Stop
	 * behavior.
	 */
	private static final Set<String> NEVER_REORIENTS = Set.of("Display");

	/** The registered tags whose elements are {@link Editable}. */
	private static final Set<String> EDITABLE = Set.of(
			"Clock", "Constant", "Memory", "Register", "SigGen",
			"StateMachine", "SubCircuit", "Text", "TruthTable");

	/** The registered tags whose elements are {@link QuickEditable}. */
	private static final Set<String> QUICK_EDITABLE = Set.of("Constant");

	/**
	 * Assert that a type declares an interface exactly when its tag is in
	 * the expected set, in both directions.
	 *
	 * @param type The registered element type.
	 * @param made An instance of the type.
	 * @param capability The capability interface to check.
	 * @param expected The tags expected to declare the capability.
	 */
	private static void assertCapability(ElementType type, Element made,
			Class<?> capability, Set<String> expected) {

		assertEquals(expected.contains(type.tag()),
				capability.isInstance(made),
				"tag '" + type.tag() + "' (" + made.getClass()
						.getSimpleName() + "): instanceof "
						+ capability.getSimpleName() + " must be "
						+ expected.contains(type.tag())
						+ " (issue #78 capability interfaces; update the "
						+ "expected set if the capability really moved)");
	}

	/**
	 * Every registered type declares exactly the expected capability
	 * interfaces - no capability is missing, and none is carried by an
	 * element that should not have it.
	 */
	@Test
	void capabilityInterfacesMatchExpectedSetsForAllRegisteredTypes() {

		Circuit circuit = new Circuit("capabilitycheck");
		for (ElementType type : ElementRegistry.all()) {
			Element made = type.create(circuit);
			assertCapability(type, made, Timed.class, TIMED);
			assertCapability(type, made, Watchable.class, WATCHABLE);
			assertCapability(type, made, Rotatable.class, ROTATABLE);
			assertCapability(type, made, Editable.class, EDITABLE);
			assertCapability(type, made, QuickEditable.class,
					QUICK_EDITABLE);
		}
	}

	/**
	 * A loadable fixture containing one unattached instance of every
	 * {@link Rotatable} element type (and nothing wired, so every
	 * attachment gate is open).
	 *
	 * @return The circuit file text.
	 */
	private static String unattachedRotatableFixture() {

		CircuitTextBuilder cb = new CircuitTextBuilder();
		for (String g : new String[] {"AndGate", "OrGate", "NandGate",
				"NorGate", "XorGate"}) {
			cb.gate(g, 2, 2);
		}
		cb.gate("NotGate", 1, 1);
		cb.gate("DelayGate", 1, 1);
		cb.constant(0xAB);
		cb.extend(4);
		cb.adder(4);
		cb.clock(20, 10);
		cb.decoder(2);
		cb.display(4, 16);
		cb.mux(4, 2);
		cb.register(4, 3, "nff");
		cb.shifter("ArithmeticRight", 8);
		cb.triState(1);
		cb.binder(4, new int[][] {{3, 2}, {1, 0}});
		cb.splitter(4, new int[][] {{3, 2}, {1, 0}});
		cb.jumpStart("js", 4);
		cb.jumpEnd("js", 4);
		cb.subCircuit("sub");
		cb.inputPin("in1", 1);
		cb.outputPin("out1", 1);
		return cb.build();
	}

	/**
	 * The surviving {@code canRotate()}/{@code canFlip()} predicates on
	 * every {@link Rotatable} type report the class-level capability
	 * when nothing is attached: both true, except that the flip-only
	 * elements refuse to rotate and the {@link #NEVER_REORIENTS} tags
	 * refuse both. This is the predicate half of the capability
	 * contract - a dropped override (predicates falling back to
	 * {@link Rotatable}'s false defaults) fails here instead of
	 * silently removing rotate/flip from the editor and the collab
	 * ops. (The predicates need initialized puts, so the elements are
	 * loaded headlessly rather than created raw.)
	 */
	@Test
	void unattachedRotatableElementsReportClassLevelPredicates()
			throws Exception {

		assertTrue(ROTATABLE.containsAll(FLIP_ONLY),
				"every flip-only tag must be Rotatable");
		assertTrue(ROTATABLE.containsAll(NEVER_REORIENTS),
				"every never-reorients tag must be Rotatable");

		Map<Class<?>, String> tagOf = new HashMap<>();
		Circuit scratch = new Circuit("capabilitycheck");
		for (ElementType type : ElementRegistry.all()) {
			tagOf.put(type.create(scratch).getClass(), type.tag());
		}

		Circuit circuit = new Circuit("capabilitycheck");
		assertTrue(circuit.load(new Scanner(unattachedRotatableFixture())),
				() -> "load failed: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
				() -> "finishLoad failed: " + JLSInfo.loadError);

		Set<String> seen = new TreeSet<>();
		for (Element el : circuit.getElements()) {
			if (!(el instanceof Rotatable rotatable)) {
				continue;
			}
			String tag = tagOf.get(el.getClass());
			assertNotNull(tag, el.getClass().getName()
					+ ": every Rotatable element must be registered");
			seen.add(tag);
			boolean expectFlip = !NEVER_REORIENTS.contains(tag);
			boolean expectRotate = expectFlip && !FLIP_ONLY.contains(tag);
			assertEquals(expectFlip, rotatable.canFlip(),
					"tag '" + tag + "': unattached canFlip() must be "
							+ expectFlip);
			assertEquals(expectRotate, rotatable.canRotate(),
					"tag '" + tag + "': unattached canRotate() must be "
							+ expectRotate);
		}
		assertEquals(new TreeSet<>(ROTATABLE), seen,
				"the fixture must cover every Rotatable tag");
	}

	/**
	 * Among timed elements, {@code usesAccessTime()} is true for exactly
	 * the memory element - the distinction that used to be an
	 * {@code instanceof Memory} branch in the base class's timing dialog.
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
	 * A sanity floor on the interfaces: the memory element carries the
	 * expected capability bundle (timed with access-time semantics,
	 * watchable, editable, but fixed in orientation), and a pure
	 * combinational gate is timed and rotatable but nothing else.
	 */
	@Test
	void representativeElementsCarryTheExpectedCapabilities() {

		Circuit circuit = new Circuit("capabilitycheck");
		Element memory = ElementRegistry.forTag("Memory").create(circuit);
		assertTrue(memory instanceof Timed, "Memory must be Timed");
		assertTrue(memory instanceof Watchable, "Memory must be Watchable");
		assertTrue(memory instanceof Editable, "Memory must be Editable");
		assertFalse(memory instanceof Rotatable,
				"Memory has a fixed orientation");
		assertTrue(((Timed) memory).usesAccessTime(),
				"Memory must use access-time semantics");

		Element andGate = ElementRegistry.forTag("AndGate").create(circuit);
		assertTrue(andGate instanceof Timed, "AndGate must be Timed");
		assertTrue(andGate instanceof Rotatable, "AndGate must be Rotatable");
		assertFalse(andGate instanceof Watchable,
				"a plain gate is not watchable");
		assertFalse(andGate instanceof Editable,
				"a plain gate has no change dialog");
		assertFalse(((Timed) andGate).usesAccessTime(),
				"a gate uses propagation delay, not access time");
	}

} // end of CapabilityInterfaceTest class
