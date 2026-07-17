package jls;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Executable architecture rules (issue #155). The in-tree ratchets
 * scan source text (NotificationRatchetTest, HeadlessCoreRatchetTest);
 * these rules check the compiled bytecode, which catches what a text
 * scan cannot - fully qualified references without an import,
 * dependencies created by field/method signatures - and cannot be
 * fooled by mentions in comments or strings.
 *
 * Rules that already hold are zero-tolerance. Rules with standing
 * violations pin the exact offender list as a shrinking baseline,
 * the same pattern HeadlessCoreRatchetTest documents: never add a
 * name, delete names as they are cleaned, and when the list is empty
 * collapse the rule to zero-tolerance.
 */
class ArchitectureRulesTest {

	private static JavaClasses classes;

	@BeforeAll
	static void importCompiledClasses() {
		// target/classes is where surefire's classpath points anyway;
		// importing once keeps the per-rule cost to milliseconds
		classes = new ClassFileImporter()
				.importPath("target/classes");
	}

	/**
	 * The bytecode half of the issue #81 reporter discipline: only
	 * TellUser may touch JOptionPane. NotificationRatchetTest enforces
	 * the same rule on source text (and so also polices comments that
	 * would normalize new uses); this one catches a fully qualified
	 * javax.swing.JOptionPane.showMessageDialog(...) that never
	 * imports the class.
	 */
	@Test
	void onlyTellUserDependsOnJOptionPane() {
		noClasses()
				.that().doNotHaveFullyQualifiedName("jls.TellUser")
				.should().dependOnClassesThat()
				.haveFullyQualifiedName("javax.swing.JOptionPane")
				.because("every user notification flows through the"
						+ " headless-aware TellUser reporter (issue #81)")
				.check(classes);
	}

	/**
	 * The HDL emitters are internal to jls.hdl; the one sanctioned
	 * consumer outside the package is the CLI wiring point in
	 * JLSStart, which instantiates the emitter chosen by the output
	 * file extension (issue #60). Element classes must not grow
	 * direct emitter knowledge - per-element HDL text lives behind
	 * the exporter walk.
	 */
	@Test
	void hdlInternalsAreOnlyWiredFromTheCli() {
		noClasses()
				.that().resideOutsideOfPackage("jls.hdl..")
				.and().doNotHaveFullyQualifiedName("jls.JLSStart")
				.should().dependOnClassesThat()
				.resideInAPackage("jls.hdl..")
				.because("HDL export is reached through the JLSStart"
						+ " wiring point only (issue #60)")
				.check(classes);
	}

	/**
	 * The headless-core direction (issue #77): the simulation engine
	 * must not depend on the editor. This does not hold yet - the
	 * simulator base class and both concrete simulators reach into
	 * jls.edit today, which is exactly the debt #77 records. The
	 * baseline below pins the offenders; anything new in jls.sim
	 * picking up an editor dependency fails immediately.
	 */
	@Test
	void onlyTheKnownDebtInSimDependsOnEdit() {
		// the name pattern covers the classes' anonymous/nested
		// classes too - they compile from the same source files
		noClasses()
				.that().resideInAPackage("jls.sim..")
				.and().haveNameNotMatching(
						"jls\\.sim\\.(Simulator|InteractiveSimulator|"
						+ "BatchSimulator)(\\$.*)?")
				.should().dependOnClassesThat()
				.resideInAPackage("jls.edit..")
				.because("the simulation engine belongs to the headless"
						+ " core (issue #77); the three named classes are"
						+ " the recorded debt - shrink the list, never"
						+ " grow it")
				.check(classes);
	}
}
