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
import jls.sim.SimEvent;
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
 *
 * The seed order is observed through the events the elements post:
 * every Constant posts exactly one time-0 event from its initSim, so
 * a post-recording simulator sees the initSim visitation order as its
 * event-callback order. (The element hierarchy is sealed, issue #95,
 * so a recording test-double element is deliberately impossible.)
 */
class SimulationSeedOrderTest {

	/** The wire-heavy fixture shared with DeterministicSaveTest. */
	private static final Path FORK_FIXTURE =
			Path.of("test", "fixtures", "fork-4.6-shiftregister.jls");

	/** A simulator that records the callback of every posted event. */
	private static final class RecordingSimulator extends Simulator {

		/** The event callbacks, in posting order. */
		private final List<Object> posted = new ArrayList<Object>();

		@Override
		public void post(SimEvent event) {
			posted.add(event.getCallBack());
			super.post(event);
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

	private static Circuit load(String text) throws Exception {
		Circuit circuit = new Circuit("");
		assertTrue(circuit.load(new Scanner(text)),
				() -> "load failed: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
				() -> "finishLoad failed: " + JLSInfo.loadError);
		return circuit;
	}

	@Test
	void stableOrderIsSortedByStableId() throws Exception {
		Circuit circuit = load(Files.readString(FORK_FIXTURE,
				StandardCharsets.UTF_8));

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
	void initSimIsSeededInStableIdOrder() throws Exception {
		// enough constants that the HashSet's identity-hash order
		// matching stable order by accident is out of the question
		CircuitTextBuilder cb = new CircuitTextBuilder();
		for (int i = 0; i < 16; i++) {
			cb.constant(i + 1);
		}
		Circuit circuit = load(cb.build());

		RecordingSimulator sim = new RecordingSimulator();
		sim.setCircuit(circuit);
		sim.seed();

		// every element in this circuit is a Constant, and each posts
		// exactly one time-0 event from initSim, so the posted-callback
		// order IS the initSim visitation order
		assertEquals(circuit.getElementsInStableOrder(), sim.posted,
				"initSim must visit elements in canonical stable-id "
						+ "order - the time-0 event seq numbers, and with "
						+ "them every order-sensitive settled value, "
						+ "depend on it");
	}
}
