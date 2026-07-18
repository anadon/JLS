package jls.elem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

/**
 * Pins the sealed element taxonomy (#95): the element set is closed by
 * design (#80 removed the plugin mechanism), so the type system says so.
 * Each test asserts one node of the adjudicated {@code permits} tree from
 * the 2026-07-17 decision on issue #95; a new element subtype must appear
 * here as well as in the {@code permits} clause, which is the authoring
 * checklist the compiler now enforces at every exhaustive dispatch site.
 */
class SealedHierarchyTest {

	private static Set<String> permitted(Class<?> sealedRoot) {
		assertTrue(sealedRoot.isSealed(),
				() -> sealedRoot.getName() + " must be sealed");
		return Arrays.stream(sealedRoot.getPermittedSubclasses())
				.map(Class::getSimpleName)
				.collect(Collectors.toCollection(TreeSet::new));
	}

	private static Set<String> names(String... simpleNames) {
		return new TreeSet<>(Arrays.asList(simpleNames));
	}

	@Test
	void elementRootPermitsExactlyTheThreeBranches() {
		assertEquals(names("DisplayElement", "LogicElement", "Wire"),
				permitted(Element.class));
		assertTrue(Modifier.isAbstract(Element.class.getModifiers()),
				"Element is never instantiated directly");
	}

	@Test
	void logicElementPermitsTheFourIntermediatesAndEighteenLeaves() {
		assertEquals(names(
				// intermediates
				"Gate", "Group", "Pin", "SigSim",
				// concrete leaves
				"Adder", "Clock", "Constant", "Decoder", "Display",
				"JumpEnd", "JumpStart", "Memory", "Mux", "Pause",
				"Register", "ShiftRegister", "StateMachine", "Stop",
				"SubCircuit", "TriState", "TruthTable", "WireEnd"),
				permitted(LogicElement.class));
	}

	@Test
	void gatePermitsTheEightGateKinds() {
		assertEquals(names("AndGate", "DelayGate", "Extend", "NandGate",
				"NorGate", "NotGate", "OrGate", "XorGate"),
				permitted(Gate.class));
	}

	@Test
	void smallIntermediatesPermitTheirPairs() {
		assertEquals(names("Binder", "Splitter"), permitted(Group.class));
		assertEquals(names("InputPin", "OutputPin"), permitted(Pin.class));
		assertEquals(names("SigGen", "TestGen"), permitted(SigSim.class));
		assertEquals(names("Text"), permitted(DisplayElement.class));
	}

	@Test
	void putIsASeparateSealedRootNotUnderElement() {
		assertEquals(names("Input", "Output"), permitted(Put.class));
		assertTrue(!Element.class.isAssignableFrom(Put.class),
				"Put is a separate root per the #95 adjudication");
	}

	@Test
	void everyPermittedConcreteTypeIsFinal() {
		for (Class<?> root : new Class<?>[] { Element.class,
				LogicElement.class, Gate.class, Group.class, Pin.class,
				SigSim.class, DisplayElement.class, Put.class }) {
			for (Class<?> sub : root.getPermittedSubclasses()) {
				int mods = sub.getModifiers();
				assertTrue(Modifier.isAbstract(mods) || Modifier.isFinal(mods),
						() -> sub.getName()
								+ " must be final (leaf) or abstract sealed"
								+ " (intermediate)");
				if (Modifier.isAbstract(mods)) {
					assertTrue(sub.isSealed(),
							() -> sub.getName()
									+ " is an intermediate and must be sealed");
				}
			}
		}
	}
}
