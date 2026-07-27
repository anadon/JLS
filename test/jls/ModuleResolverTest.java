package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import jls.module.Activation;
import jls.module.ModuleManifest;
import jls.module.ModuleResolutionException;
import jls.module.ModuleResolver;

/**
 * Pins the four falsifiable-acceptance behaviors of the module
 * resolver from issue #220 — missing hard dependency fails loudly
 * naming the token and the requirer; a cycle reports its full path;
 * an absent optional resolves gracefully without pulling anything in;
 * the resolved order is identical across shuffled input permutations —
 * plus the supporting rules: duplicate ids, capability-token
 * satisfaction, provider ambiguity, requires-implies-after, and
 * ordering-only before/after edges (grand-architecture §4.2 rules
 * 1-6).
 */
class ModuleResolverTest {

	/** Shorthand manifest: all sets default empty, activation eager. */
	private static ModuleManifest mod(String id, Set<String> provides,
			Set<String> requires, Set<String> optional,
			Set<String> after, Set<String> before) {
		return new ModuleManifest(id, 1, provides, requires, optional,
				after, before, new Activation.Eager());
	}

	/** A manifest with an id and nothing else. */
	private static ModuleManifest bare(String id) {
		return mod(id, Set.of(), Set.of(), Set.of(), Set.of(),
				Set.of());
	}

	/** The resolved order as a list of ids. */
	private static List<String> ids(List<ModuleManifest> order) {
		return order.stream().map(ModuleManifest::id).toList();
	}

	@Test
	void missingHardDependencyFailsNamingTokenAndRequirer() {
		ModuleManifest orphan = mod("hdl.export", Set.of(),
				Set.of("board-constraints"), Set.of(), Set.of(),
				Set.of());
		ModuleResolutionException ex = assertThrows(
				ModuleResolutionException.class,
				() -> ModuleResolver.resolve(List.of(orphan)));
		assertTrue(ex.getMessage().contains("hdl.export"),
				"the requirer must be named: " + ex.getMessage());
		assertTrue(ex.getMessage().contains("board-constraints"),
				"the missing token must be named: " + ex.getMessage());
	}

	@Test
	void ambiguousProvidersFailNamingAllOfThem() {
		ModuleManifest first = mod("yosys", Set.of("synth"), Set.of(),
				Set.of(), Set.of(), Set.of());
		ModuleManifest second = mod("abc", Set.of("synth"), Set.of(),
				Set.of(), Set.of(), Set.of());
		ModuleManifest user = mod("hdl.export", Set.of(),
				Set.of("synth"), Set.of(), Set.of(), Set.of());
		ModuleResolutionException ex = assertThrows(
				ModuleResolutionException.class,
				() -> ModuleResolver.resolve(
						List.of(first, second, user)));
		assertTrue(ex.getMessage().contains("synth"),
				"the ambiguous token must be named: "
						+ ex.getMessage());
		assertTrue(ex.getMessage().contains("yosys")
				&& ex.getMessage().contains("abc"),
				"every competing provider must be named: "
						+ ex.getMessage());
	}

	@Test
	void duplicateModuleIdsFail() {
		ModuleResolutionException ex = assertThrows(
				ModuleResolutionException.class,
				() -> ModuleResolver.resolve(
						List.of(bare("core"), bare("core"))));
		assertTrue(ex.getMessage().contains("core"),
				"the duplicated id must be named: " + ex.getMessage());
	}

	@Test
	void optionalAbsentResolvesGracefullyAndPullsNothingIn() throws
			ModuleResolutionException {
		ModuleManifest soft = mod("gui", Set.of(), Set.of(),
				Set.of("board-constraints"), Set.of(), Set.of());
		assertEquals(List.of("gui"),
				ids(ModuleResolver.resolve(List.of(soft))),
				"an absent optional is a no-op, never a gate");
	}

	@Test
	void requiresImpliesAfterAndTokensResolveToProviders() throws
			ModuleResolutionException {
		// "user" requires the capability token, not the concrete id
		ModuleManifest provider = mod("elem.registry",
				Set.of("element-registry"), Set.of(), Set.of(),
				Set.of(), Set.of());
		ModuleManifest user = mod("aaa.user", Set.of(),
				Set.of("element-registry"), Set.of(), Set.of(),
				Set.of());
		assertEquals(List.of("elem.registry", "aaa.user"),
				ids(ModuleResolver.resolve(List.of(user, provider))),
				"the provider must start before its requirer even"
						+ " though the tie-break alone would not");
	}

	@Test
	void afterAloneOrdersWhenPresentAndPullsNothingWhenAbsent() throws
			ModuleResolutionException {
		ModuleManifest late = mod("aaa.late", Set.of(), Set.of(),
				Set.of(), Set.of("board", "ghost"), Set.of());
		ModuleManifest board = bare("board");
		assertEquals(List.of("board", "aaa.late"),
				ids(ModuleResolver.resolve(List.of(late, board))),
				"after must order against a present provider");
		assertEquals(List.of("aaa.late"),
				ids(ModuleResolver.resolve(List.of(late))),
				"after must pull nothing in when the token is absent");
	}

	@Test
	void beforeOrdersAgainstThePresentProvider() throws
			ModuleResolutionException {
		ModuleManifest early = mod("zzz.early", Set.of(), Set.of(),
				Set.of(), Set.of(), Set.of("aaa"));
		ModuleManifest target = bare("aaa");
		assertEquals(List.of("zzz.early", "aaa"),
				ids(ModuleResolver.resolve(List.of(target, early))),
				"before must win over the id-sorted tie-break");
	}

	@Test
	void independentModulesEmergeIdSorted() throws
			ModuleResolutionException {
		assertEquals(List.of("batch", "collab", "core", "gui", "hdl"),
				ids(ModuleResolver.resolve(List.of(bare("gui"),
						bare("hdl"), bare("core"), bare("batch"),
						bare("collab")))));
	}

	@Test
	void providingYourOwnRequirementIsSatisfiedWithoutACycle() throws
			ModuleResolutionException {
		ModuleManifest self = mod("core", Set.of("kernel"),
				Set.of("kernel"), Set.of(), Set.of(), Set.of());
		assertEquals(List.of("core"),
				ids(ModuleResolver.resolve(List.of(self))));
	}

	@Test
	void requiresCycleReportsTheFullPath() {
		ModuleManifest a = mod("a", Set.of(), Set.of("b"), Set.of(),
				Set.of(), Set.of());
		ModuleManifest b = mod("b", Set.of(), Set.of("c"), Set.of(),
				Set.of(), Set.of());
		ModuleManifest c = mod("c", Set.of(), Set.of("a"), Set.of(),
				Set.of(), Set.of());
		ModuleResolutionException ex = assertThrows(
				ModuleResolutionException.class,
				() -> ModuleResolver.resolve(List.of(a, b, c)));
		assertTrue(ex.getMessage().contains("a -> b -> c -> a"),
				"the full cycle path must be reported: "
						+ ex.getMessage());
	}

	@Test
	void orderingOnlyCycleIsAlsoLoud() {
		ModuleManifest a = mod("a", Set.of(), Set.of(), Set.of(),
				Set.of("b"), Set.of());
		ModuleManifest b = mod("b", Set.of(), Set.of(), Set.of(),
				Set.of("a"), Set.of());
		ModuleResolutionException ex = assertThrows(
				ModuleResolutionException.class,
				() -> ModuleResolver.resolve(List.of(a, b)));
		assertTrue(ex.getMessage().contains("a -> b -> a"),
				"an after/after cycle must report its path: "
						+ ex.getMessage());
	}

	@Test
	void resolvedOrderIsIdenticalAcrossInputPermutations() throws
			ModuleResolutionException {
		// a small layered graph with real ties to break: core first,
		// then registry (requires core), then three siblings that all
		// require the registry token, one ordered before another
		List<ModuleManifest> modules = new ArrayList<>(List.of(
				mod("core", Set.of("kernel"), Set.of(), Set.of(),
						Set.of(), Set.of()),
				mod("elem.registry", Set.of("element-registry"),
						Set.of("kernel"), Set.of(), Set.of(),
						Set.of()),
				mod("gui", Set.of(), Set.of("element-registry"),
						Set.of("board"), Set.of(), Set.of()),
				mod("hdl", Set.of(), Set.of("element-registry"),
						Set.of(), Set.of(), Set.of("batch")),
				mod("batch", Set.of(), Set.of("element-registry"),
						Set.of(), Set.of(), Set.of())));
		List<String> expected = ids(ModuleResolver.resolve(modules));
		assertEquals(
				List.of("core", "elem.registry", "gui", "hdl",
						"batch"),
				expected,
				"layers in dependency order, ties id-sorted, before"
						+ " respected");
		Random shuffler = new Random(220);
		for (int permutation = 0; permutation < 50; permutation++) {
			Collections.shuffle(modules, shuffler);
			assertEquals(expected,
					ids(ModuleResolver.resolve(modules)),
					"resolved order must not depend on discovery"
							+ " order (permutation " + permutation
							+ ": " + ids(modules) + ")");
		}
	}

} // end of ModuleResolverTest class
