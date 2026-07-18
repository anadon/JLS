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

	/**
	 * The collaboration layering rule from issue #163: everything under
	 * jls.collab except the (future) jls.collab.ui package is headless -
	 * ops, session, CRDT, and network code never touch Swing. UI effects
	 * route through listeners marshalled onto the EDT by jls.collab.ui,
	 * the one collab package allowed to import it. Zero-tolerance from
	 * the start: the first collab package (jls.collab.op, issue #167)
	 * is born clean.
	 */
	@Test
	void collabLayersAreHeadless() {
		noClasses()
				.that().resideInAPackage("jls.collab..")
				.and().resideOutsideOfPackage("jls.collab.ui..")
				.should().dependOnClassesThat()
				.resideInAPackage("javax.swing..")
				.because("only jls.collab.ui may touch Swing; ops and"
						+ " the replication stack are headless by"
						+ " construction (issues #163/#167)")
				.check(classes);
	}

	/**
	 * Collaboration layering rule 1 from issue #163: the transport
	 * package moves opaque signed frames and knows nothing of
	 * circuits. It must not depend on the circuit model, the editor,
	 * the simulator, or any collab layer above it - not even the
	 * operation vocabulary, which reaches the wire only as bytes
	 * inside a frame. The package does not exist yet (issue #168
	 * creates it); allowEmptyShould pins the rule now so the package
	 * is born clean, the same way collabLayersAreHeadless predated
	 * everything but jls.collab.op.
	 */
	@Test
	void transportKnowsNothingOfCircuits() {
		noClasses()
				.that().resideInAPackage("jls.collab.net..")
				.should().dependOnClassesThat()
				.resideInAnyPackage("jls.elem..", "jls.edit..",
						"jls.sim..", "jls.collab.op..",
						"jls.collab.session..", "jls.collab.crdt..",
						"jls.collab.ui..")
				.orShould().dependOnClassesThat()
				.haveFullyQualifiedName("jls.Circuit")
				.because("jls.collab.net moves opaque signed frames"
						+ " and imports nothing of the circuit model"
						+ " or the layers above it (issue #163 rule 1,"
						+ " built by #168)")
				.allowEmptyShould(true)
				.check(classes);
	}

	/**
	 * The issue #170 serialization prohibition, bytecode half (the
	 * source-text half is CollabSecurityRatchetTest): no class in the
	 * application depends on Java object serialization streams. The
	 * collaboration wire format is the textual save-format grammar;
	 * deserializing hostile bytes with ObjectInputStream is the
	 * classic remote-execution surface and is banned everywhere,
	 * zero-tolerance from a clean baseline.
	 */
	@Test
	void nothingUsesJavaObjectSerializationStreams() {
		noClasses()
				.should().dependOnClassesThat()
				.haveNameMatching(
						"java\\.io\\.Object(Input|Output)Stream")
				.because("network and file payloads are textual"
						+ " save-format grammar with typed rejection,"
						+ " never Java object serialization"
						+ " (issue #170)")
				.check(classes);
	}

	/**
	 * Collaboration layering rule 2 from issue #163: the replication
	 * stack (session roster/presence/token, CRDT merge/op log) sits
	 * between the operation vocabulary and the transport. It depends
	 * downward on jls.collab.op and jls.collab.net only - never on
	 * the editor or on jls.collab.ui; UI effects route through
	 * listeners that jls.collab.ui marshals onto the EDT. Swing
	 * itself is already forbidden by collabLayersAreHeadless. The
	 * packages do not exist yet (issues #169/#171 create them);
	 * allowEmptyShould pins the rule so they are born clean.
	 */
	@Test
	void replicationStackDependsDownwardOnly() {
		noClasses()
				.that().resideInAnyPackage("jls.collab.session..",
						"jls.collab.crdt..")
				.should().dependOnClassesThat()
				.resideInAnyPackage("jls.edit..", "jls.collab.ui..")
				.because("session and CRDT code depends downward on"
						+ " the op vocabulary and the transport only;"
						+ " UI effects go through listeners (issue"
						+ " #163 rule 2, built by #169/#171)")
				.allowEmptyShould(true)
				.check(classes);
	}

	/**
	 * The issue #170 transport confinement, bytecode half: socket
	 * endpoints may only be touched from the (future) jls.collab.net
	 * package, the one place the #163 architecture allows transport.
	 * Zero-tolerance from before that package exists - which also
	 * means batch mode and the default GUI start cannot bind a
	 * listener (issue #170 P4), because nothing can construct one.
	 */
	@Test
	void socketEndpointsAreConfinedToCollabNet() {
		noClasses()
				.that().resideOutsideOfPackage("jls.collab.net..")
				.should().dependOnClassesThat()
				.haveNameMatching(
						"java\\.net\\.(Socket|ServerSocket"
						+ "|DatagramSocket|MulticastSocket)"
						+ "|java\\.nio\\.channels\\.(Asynchronous)?"
						+ "(Server)?SocketChannel"
						+ "|java\\.nio\\.channels\\.DatagramChannel"
						+ "|javax\\.net\\.(SocketFactory"
						+ "|ServerSocketFactory)"
						+ "|javax\\.net\\.ssl\\.SSL(Server)?Socket"
						+ "(Factory)?")
				.because("transport lives only in jls.collab.net"
						+ " (issues #163/#170)")
				.check(classes);
	}

	/**
	 * The issue #170 data-only rule for the collaboration layer: no
	 * class under jls.collab depends on java.lang.reflect. A network
	 * payload that names an element type passes the token through the
	 * ElementVocabulary allowlist and hands instantiation to the
	 * loader; collab code itself never selects classes or invokes
	 * members reflectively.
	 */
	@Test
	void collabDependsOnNoReflection() {
		noClasses()
				.that().resideInAPackage("jls.collab..")
				.should().dependOnClassesThat()
				.resideInAPackage("java.lang.reflect..")
				.because("the collaboration vocabulary is data-only;"
						+ " element instantiation is gated by the"
						+ " ElementVocabulary allowlist (issue #170)")
				.check(classes);
	}
}
