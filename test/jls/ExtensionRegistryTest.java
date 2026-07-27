package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import jls.module.ExtensionPoint;
import jls.module.ExtensionRegistry;

/**
 * Pins the {@link ExtensionRegistry} contract (issue #223): declared
 * points round-trip contributions in deterministic contribution
 * order; undeclared points and duplicate declarations fail loudly
 * with the offending names in the message; type safety binds at
 * contribution time via the point's contract token; and dispensed
 * lists are immutable snapshots.
 */
class ExtensionRegistryTest {

	/** A point over a contract with two easy implementations. */
	private static final ExtensionPoint<CharSequence> TEXT =
			new ExtensionPoint<CharSequence>("test.text",
					CharSequence.class);

	/** A second declared point, to prove streams stay separate. */
	private static final ExtensionPoint<Runnable> COMMAND =
			new ExtensionPoint<Runnable>("test.command",
					Runnable.class);

	/** A registry declaring both test points. */
	private static ExtensionRegistry registry() {
		return new ExtensionRegistry(List.of(TEXT, COMMAND));
	}

	@Test
	void contributionsRoundTripPerPoint() {
		ExtensionRegistry registry = registry();
		Runnable command = () -> {
		};
		registry.contribute(TEXT, "mod.a", "alpha");
		registry.contribute(COMMAND, "mod.b", command);
		registry.contribute(TEXT, "mod.b", "beta");
		assertEquals(List.of("alpha", "beta"),
				registry.contributions(TEXT));
		assertEquals(List.of(command),
				registry.contributions(COMMAND));
		assertEquals(Set.of(TEXT, COMMAND), registry.points());
	}

	@Test
	void declaredButEmptyPointDispensesEmptyList() {
		assertEquals(List.of(), registry().contributions(TEXT));
	}

	@Test
	void orderIsContributionOrderAcrossPermutations() {
		ExtensionRegistry forward = registry();
		forward.contribute(TEXT, "mod.a", "alpha");
		forward.contribute(TEXT, "mod.b", "beta");
		forward.contribute(TEXT, "mod.c", "gamma");
		assertEquals(List.of("alpha", "beta", "gamma"),
				forward.contributions(TEXT));

		ExtensionRegistry backward = registry();
		backward.contribute(TEXT, "mod.c", "gamma");
		backward.contribute(TEXT, "mod.b", "beta");
		backward.contribute(TEXT, "mod.a", "alpha");
		assertEquals(List.of("gamma", "beta", "alpha"),
				backward.contributions(TEXT));
	}

	@Test
	void undeclaredPointRejectedNamingPointAndContributor() {
		ExtensionPoint<String> undeclared =
				new ExtensionPoint<String>("test.undeclared",
						String.class);
		IllegalArgumentException thrown = assertThrows(
				IllegalArgumentException.class,
				() -> registry().contribute(undeclared, "mod.rogue",
						"x"));
		assertTrue(thrown.getMessage().contains("test.undeclared"),
				"message must name the point: " + thrown.getMessage());
		assertTrue(thrown.getMessage().contains("mod.rogue"),
				"message must name the contributor: "
						+ thrown.getMessage());
		assertThrows(IllegalArgumentException.class,
				() -> registry().contributions(undeclared),
				"reads of undeclared points fail just as loudly");
	}

	@Test
	void sameIdDifferentContractIsNotAMatch() {
		ExtensionPoint<String> impostor =
				new ExtensionPoint<String>(TEXT.id(), String.class);
		assertThrows(IllegalArgumentException.class,
				() -> registry().contribute(impostor, "mod.rogue",
						"x"),
				"a same-id point over another contract is undeclared");
	}

	@Test
	void duplicateIdsAtConstructionRejectedNamingBoth() {
		ExtensionPoint<String> asString =
				new ExtensionPoint<String>("test.dup", String.class);
		ExtensionPoint<Runnable> asRunnable =
				new ExtensionPoint<Runnable>("test.dup",
						Runnable.class);
		IllegalArgumentException thrown = assertThrows(
				IllegalArgumentException.class,
				() -> new ExtensionRegistry(
						List.of(asString, asRunnable)));
		assertTrue(thrown.getMessage().contains("test.dup"),
				"message must name the id: " + thrown.getMessage());
		assertTrue(thrown.getMessage()
				.contains(String.class.getName()),
				"message must name the first contract: "
						+ thrown.getMessage());
		assertTrue(thrown.getMessage()
				.contains(Runnable.class.getName()),
				"message must name the second contract: "
						+ thrown.getMessage());
	}

	@Test
	void blankModuleIdRejected() {
		assertThrows(IllegalArgumentException.class,
				() -> registry().contribute(TEXT, " ", "alpha"));
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void wrongTypedRawContributionFailsAtTheBoundary() {
		ExtensionRegistry registry = registry();
		ExtensionPoint raw = TEXT;
		assertThrows(ClassCastException.class,
				() -> registry.contribute(raw, "mod.rogue",
						Integer.valueOf(7)),
				"the contract token casts at contribute time, so a "
						+ "raw-typed wrong contribution never enters "
						+ "the registry");
		assertEquals(List.of(), registry.contributions(TEXT),
				"the rejected contribution must not be recorded");
	}

	@Test
	void dispensedListsAreImmutableSnapshots() {
		ExtensionRegistry registry = registry();
		registry.contribute(TEXT, "mod.a", "alpha");
		List<CharSequence> dispensed = registry.contributions(TEXT);
		assertThrows(UnsupportedOperationException.class,
				() -> dispensed.add("beta"));
		registry.contribute(TEXT, "mod.b", "beta");
		assertEquals(List.of("alpha"), dispensed,
				"a dispensed list is a snapshot; later contributions "
						+ "do not mutate it");
		assertEquals(List.of("alpha", "beta"),
				registry.contributions(TEXT));
	}

	@Test
	void pointsSetIsImmutable() {
		ExtensionRegistry registry = registry();
		assertThrows(UnsupportedOperationException.class,
				() -> registry.points().remove(TEXT));
	}

} // end of ExtensionRegistryTest class
