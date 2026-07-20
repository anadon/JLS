package jls.ui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;

/**
 * Completeness ratchet for the dialog-construction sweep (issue #162):
 * every {@code ElementDialog} subclass in the compiled tree must be
 * represented in {@link DialogConstructionSmokeTest} - directly, via a
 * family representative, or through a documented exemption. A new
 * element dialog that ships without sweep coverage fails here, in the
 * headless run, before it can execute for the first time in front of a
 * student.
 *
 * Headless by design: this test reads bytecode only (same importer as
 * ArchitectureRulesTest) and never constructs a dialog.
 */
class DialogCoverageRatchetTest {

	/**
	 * Element classes the sweep constructs by name
	 * ({@code constructAndCancel} calls). Keep in sync with
	 * DialogConstructionSmokeTest - this list failing to match the
	 * sweep is exactly the signal the ratchet exists to give.
	 */
	private static final Set<String> SWEPT = Set.of(
			"Adder", "AndGate", "Binder", "Clock", "Constant", "Decoder",
			"DelayGate", "Display", "Element", "Extend", "InputPin",
			"JumpEnd", "JumpStart", "Memory", "Mux", "OutputPin",
			"Register", "ShiftRegister", "SigGen", "Splitter",
			"StateMachine", "SubCircuit", "Text", "TriState",
			"TruthTable");

	/**
	 * Dialog-owning classes covered through a subclass the sweep
	 * constructs, or exempt for a stated structural reason. Shrink,
	 * never grow, the exemptions.
	 */
	private static final Map<String, String> REPRESENTED = Map.of(
			"Gate", "abstract; its dialog is swept via AndGate,"
					+ " DelayGate, and Extend",
			"Group", "abstract; its ranges dialog is swept via Binder"
					+ " and Splitter",
			"Pin", "abstract; its dialog is swept via InputPin and"
					+ " OutputPin",
			"State", "constructed only inside StateMachine's"
					+ " StateEditor, which the StateMachine sweep entry"
					+ " opens");

	@Test
	void everyElementDialogIsSweptOrExempt() {
		JavaClasses classes = new ClassFileImporter()
				.importPath("target/classes");
		Set<String> unrepresented = new TreeSet<String>();
		for (JavaClass c : classes) {
			if (!c.isAssignableTo("jls.elem.ElementDialog")
					|| "jls.elem.ElementDialog".equals(c.getName())) {
				continue;
			}
			// dialogs are inner classes; charge them to the top-level
			// element that owns them
			String owner = c.getName()
					.replace("jls.elem.", "")
					.replaceAll("[$.].*", "");
			if (!SWEPT.contains(owner)
					&& !REPRESENTED.containsKey(owner)) {
				unrepresented.add(owner + " (" + c.getName() + ")");
			}
		}
		assertTrue(unrepresented.isEmpty(),
				"element dialogs with no construction coverage - add"
						+ " them to DialogConstructionSmokeTest (and"
						+ " this ratchet's SWEPT list) or document an"
						+ " exemption: " + unrepresented);
	}

} // end of DialogCoverageRatchetTest class
