package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import jls.elem.Element;
import jls.elem.LogicElement;
import jls.sim.Simulator;

/**
 * Canonical simulation seeding (issue #181). Same-time events fire in
 * posting order (SimEvent seq), so the order initSim is invoked in
 * decides how order-sensitive circuits - cross-coupled latches,
 * multi-driver nets - settle. Seeding used to iterate the element
 * HashSet, whose identity-hash order varies between runs and machines;
 * it must instead follow the circuit's canonical stable-id order
 * ({@link Circuit#getElementsInStableOrder()}), the same order the
 * canonical save uses, so simulated values are a pure function of
 * circuit content.
 */
class SimulationSeedOrderTest {

	/** The wire-heavy fixture shared with DeterministicSaveTest. */
	private static final Path FORK_FIXTURE =
			Path.of("test", "fixtures", "fork-4.6-shiftregister.jls");

	/** A logic element that records when initSim visits it. */
	private static final class Probe extends LogicElement {

		private final List<Element> visits;

		Probe(Circuit circuit, List<Element> visits) {
			super(circuit);
			this.visits = visits;
		}

		@Override
		public void initSim(Simulator sim) {
			visits.add(this);
		}
	}

	/** A simulator exposing the protected seed phase. */
	private static final class SeedSimulator extends Simulator {

		SeedSimulator() {
			me = this;
		}

		void seed() {
			initSimulation();
		}

		@Override
		public void stop() {
		}

		@Override
		public void pause(boolean which) {
		}
	}

	@Test
	void stableOrderIsSortedByStableId() throws Exception {
		Circuit circuit = new Circuit("");
		String text = Files.readString(FORK_FIXTURE, StandardCharsets.UTF_8);
		assertTrue(circuit.load(new Scanner(text)),
				() -> "load failed: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
				() -> "finishLoad failed: " + JLSInfo.loadError);

		List<Element> ordered = circuit.getElementsInStableOrder();
		assertEquals(circuit.getElements().size(), ordered.size(),
				"the stable order must contain every element");
		assertEquals(new HashSet<Element>(circuit.getElements()),
				new HashSet<Element>(ordered),
				"the stable order must contain exactly the circuit's "
						+ "elements");
		for (int i = 1; i < ordered.size(); i++) {
			assertTrue(ordered.get(i - 1).getStableId()
					.compareTo(ordered.get(i).getStableId()) < 0,
					"elements must be sorted by stable id, violated at "
							+ i);
		}
	}

	@Test
	void initSimIsSeededInStableIdOrder() {
		// enough probes that the HashSet's identity-hash order matching
		// creation order by accident is out of the question
		Circuit circuit = new Circuit("seed");
		List<Element> visits = new ArrayList<Element>();
		for (int i = 0; i < 24; i++) {
			circuit.addElement(new Probe(circuit, visits));
		}

		SeedSimulator sim = new SeedSimulator();
		sim.setCircuit(circuit);
		sim.seed();

		assertEquals(circuit.getElementsInStableOrder(), visits,
				"initSim must visit elements in canonical stable-id "
						+ "order - the time-0 event seq numbers, and with "
						+ "them every order-sensitive settled value, "
						+ "depend on it");
	}
}
