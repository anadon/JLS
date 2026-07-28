package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import jls.boot.JlsModules;
import jls.edit.GuiExtensionPoints;
import jls.edit.Palette;
import jls.edit.PaletteEntry;
import jls.elem.ElementExtensionPoints;
import jls.elem.ElementRegistry;
import jls.elem.ElementType;
import jls.hdl.HdlEmitter;
import jls.hdl.HdlExtensionPoints;
import jls.hdl.VerilogEmitter;
import jls.hdl.VhdlEmitter;
import jls.module.ExtensionRegistry;
import jls.module.JlsModule;
import jls.module.ModuleActivationException;
import jls.module.ModuleResolutionException;
import jls.module.ModuleRuntime;

/**
 * Golden pin for wiring the compiled-in subsystems as
 * {@link JlsModule} manifests and booting them through
 * {@link ModuleRuntime} (issue #220, final DoD item). Asserts the real
 * four-module set resolves and eagerly starts in one deterministic
 * topological order — {@code core} first, its dependents id-tie-broken —
 * identically across shuffled input permutations, and that each shipped
 * subsystem's built-in contributions land on its declared seam:
 * {@code elem.element-provider} equals {@link ElementRegistry#all()},
 * {@code gui.palette-contributor} equals {@link Palette#entries()}, and
 * {@code hdl.exporter} carries the Verilog and VHDL emitters. All counts
 * are read dynamically off the live tables so new elements (#201) never
 * break this pin.
 */
class JlsModulesBootTest {

	@Test
	void bootStartsAllFourModulesInDeterministicTopoOrder() throws
			ModuleResolutionException, ModuleActivationException {
		ModuleRuntime runtime = JlsModules.boot();
		assertEquals(List.of("core", "collab", "gui", "hdl"),
				runtime.startedIds(),
				"core boots first; its dependents follow in id-sorted"
						+ " tie-break order, all eager");
		assertTrue(runtime.isStarted("core"));
		assertTrue(runtime.isStarted("gui"));
		assertTrue(runtime.isStarted("hdl"));
		assertTrue(runtime.isStarted("collab"));
	}

	@Test
	void startOrderIsIndependentOfInputPermutation() throws
			ModuleResolutionException, ModuleActivationException {
		List<String> expected = List.of("core", "collab", "gui", "hdl");
		Random shuffler = new Random(220);
		for (int permutation = 0; permutation < 25; permutation++) {
			List<JlsModule> modules =
					new ArrayList<>(JlsModules.modules());
			Collections.shuffle(modules, shuffler);
			ModuleRuntime runtime =
					ModuleRuntime.boot(modules, JlsModules.registry());
			assertEquals(expected, runtime.startedIds(),
					"start order must not depend on discovery order"
							+ " (permutation " + permutation + ")");
		}
	}

	@Test
	void coreContributesEveryRegisteredElementType() throws
			ModuleResolutionException, ModuleActivationException {
		ExtensionRegistry registry =
				JlsModules.boot().extensionRegistry();
		List<ElementType> contributed = registry.contributions(
				ElementExtensionPoints.ELEMENT_PROVIDER);
		assertEquals(ElementRegistry.all(), contributed,
				"the core module contributes exactly the shipped element"
						+ " registry table, in order");
	}

	@Test
	void guiContributesEveryPaletteEntry() throws
			ModuleResolutionException, ModuleActivationException {
		ExtensionRegistry registry =
				JlsModules.boot().extensionRegistry();
		List<PaletteEntry> contributed = registry.contributions(
				GuiExtensionPoints.PALETTE_CONTRIBUTOR);
		assertEquals(Palette.entries(), contributed,
				"the gui module contributes exactly the shipped palette"
						+ " table, in order");
	}

	@Test
	void hdlContributesBothBuiltInEmitters() throws
			ModuleResolutionException, ModuleActivationException {
		ExtensionRegistry registry =
				JlsModules.boot().extensionRegistry();
		List<HdlEmitter> emitters =
				registry.contributions(HdlExtensionPoints.EXPORTER);
		assertEquals(2, emitters.size(),
				"exactly the two built-in emitters ship");
		assertTrue(emitters.get(0) instanceof VerilogEmitter,
				"the Verilog emitter contributes first");
		assertTrue(emitters.get(1) instanceof VhdlEmitter,
				"the VHDL emitter contributes second");
	}

} // end of JlsModulesBootTest class
