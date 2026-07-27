package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import jls.module.Activation;
import jls.module.ModuleManifest;

/**
 * Pins the {@link ModuleManifest} contract (issue #220): the record
 * validates on construction — non-blank id and tokens, no axis naming
 * the module itself — and holds immutable defensive copies whatever
 * the caller passed (issue #94 discipline), so a manifest is safe to
 * share the moment it exists. Also pins the {@link Activation}
 * variants' own validation.
 */
class ModuleManifestTest {

	/** A manifest with the given id and empty axes. */
	private static ModuleManifest bare(String id) {
		return new ModuleManifest(id, 1, Set.of(), Set.of(), Set.of(),
				Set.of(), Set.of(), new Activation.Eager());
	}

	@Test
	void componentsSurviveConstructionVerbatim() {
		ModuleManifest m = new ModuleManifest("hdl.export", 3,
				Set.of("hdl-export", "verilog"),
				Set.of("core", "element-registry"),
				Set.of("board-constraints"),
				Set.of("core"),
				Set.of("batch"),
				new Activation.OnCommand("export-verilog"));
		assertEquals("hdl.export", m.id());
		assertEquals(3, m.apiVersion());
		assertEquals(Set.of("hdl-export", "verilog"), m.provides());
		assertEquals(Set.of("core", "element-registry"), m.requires());
		assertEquals(Set.of("board-constraints"), m.optional());
		assertEquals(Set.of("core"), m.after());
		assertEquals(Set.of("batch"), m.before());
		assertEquals(new Activation.OnCommand("export-verilog"),
				m.activation());
	}

	@Test
	void blankIdRejected() {
		assertThrows(IllegalArgumentException.class, () -> bare(""));
		assertThrows(IllegalArgumentException.class, () -> bare("  "));
	}

	@Test
	void blankTokensRejectedOnEveryAxis() {
		Set<String> bad = Set.of(" ");
		Set<String> ok = Set.of();
		Activation eager = new Activation.Eager();
		assertThrows(IllegalArgumentException.class,
				() -> new ModuleManifest("m", 1, bad, ok, ok, ok, ok,
						eager),
				"blank provides token must be rejected");
		assertThrows(IllegalArgumentException.class,
				() -> new ModuleManifest("m", 1, ok, bad, ok, ok, ok,
						eager),
				"blank requires token must be rejected");
		assertThrows(IllegalArgumentException.class,
				() -> new ModuleManifest("m", 1, ok, ok, bad, ok, ok,
						eager),
				"blank optional token must be rejected");
		assertThrows(IllegalArgumentException.class,
				() -> new ModuleManifest("m", 1, ok, ok, ok, bad, ok,
						eager),
				"blank after token must be rejected");
		assertThrows(IllegalArgumentException.class,
				() -> new ModuleManifest("m", 1, ok, ok, ok, ok, bad,
						eager),
				"blank before token must be rejected");
	}

	@Test
	void selfEdgesRejectedOnDependencyAndOrderingAxes() {
		Set<String> self = Set.of("m");
		Set<String> ok = Set.of();
		Activation eager = new Activation.Eager();
		assertThrows(IllegalArgumentException.class,
				() -> new ModuleManifest("m", 1, ok, self, ok, ok, ok,
						eager),
				"a module must not require itself");
		assertThrows(IllegalArgumentException.class,
				() -> new ModuleManifest("m", 1, ok, ok, self, ok, ok,
						eager),
				"a module must not optionally use itself");
		assertThrows(IllegalArgumentException.class,
				() -> new ModuleManifest("m", 1, ok, ok, ok, self, ok,
						eager),
				"a module must not order after itself");
		assertThrows(IllegalArgumentException.class,
				() -> new ModuleManifest("m", 1, ok, ok, ok, ok, self,
						eager),
				"a module must not order before itself");
	}

	@Test
	void providesMayRestateTheConcreteId() {
		// redundant but harmless: the concrete id is always provided
		ModuleManifest m = new ModuleManifest("core", 1, Set.of("core"),
				Set.of(), Set.of(), Set.of(), Set.of(),
				new Activation.Eager());
		assertTrue(m.provides().contains("core"));
	}

	@Test
	void setsAreDefensivelyCopiedAndImmutable() {
		Set<String> mutable = new HashSet<>();
		mutable.add("cap");
		ModuleManifest m = new ModuleManifest("m", 1, mutable, Set.of(),
				Set.of(), Set.of(), Set.of(), new Activation.Eager());
		mutable.add("smuggled");
		assertEquals(Set.of("cap"), m.provides(),
				"later caller mutation must not reach the record");
		assertThrows(UnsupportedOperationException.class,
				() -> m.provides().add("nope"),
				"the record's sets must be immutable");
	}

	@Test
	void activationTriggersValidateTheirIdentifiers() {
		assertEquals("export-verilog",
				new Activation.OnCommand("export-verilog").command());
		assertEquals("circuit-opened",
				new Activation.OnEvent("circuit-opened").event());
		assertThrows(IllegalArgumentException.class,
				() -> new Activation.OnCommand(" "));
		assertThrows(IllegalArgumentException.class,
				() -> new Activation.OnEvent(""));
		assertEquals(new Activation.Eager(), new Activation.Eager());
		assertEquals(new Activation.OnDemand(),
				new Activation.OnDemand());
	}

} // end of ModuleManifestTest class
