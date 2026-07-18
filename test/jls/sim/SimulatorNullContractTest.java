package jls.sim;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Pinned tests for the simulator's two-phase-lifecycle contracts made
 * explicit by the #93 nullness pass: entry points that previously
 * dereferenced a still-null field (an eventual NullPointerException
 * far from the caller's mistake) now fail eagerly with an
 * IllegalStateException naming the missing setup call.
 */
class SimulatorNullContractTest {

	/**
	 * Running a simulation before setCircuit is a caller bug reported
	 * eagerly, not a NullPointerException from deep inside
	 * initSimulation.
	 */
	@Test
	void simulatingWithoutACircuitReportsTheMissingSetupCall() {

		BatchSimulator sim = new BatchSimulator();
		IllegalStateException ex = assertThrows(
				IllegalStateException.class, () -> sim.runSim());
		assertTrue(ex.getMessage().contains("setCircuit"),
				"the error must name the missing call, got: "
						+ ex.getMessage());
	}

	/**
	 * writeVcd without a preceding setVcdFile used to reach
	 * Paths.get(null); now it reports the missing setup call.
	 */
	@Test
	void writeVcdWithoutAFileReportsTheMissingSetupCall() {

		BatchSimulator sim = new BatchSimulator();
		IllegalStateException ex = assertThrows(
				IllegalStateException.class, () -> sim.writeVcd());
		assertTrue(ex.getMessage().contains("setVcdFile"),
				"the error must name the missing call, got: "
						+ ex.getMessage());
	}

} // end of SimulatorNullContractTest class
