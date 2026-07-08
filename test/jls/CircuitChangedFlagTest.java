package jls;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;

/**
 * The unsaved-changes flag must survive serialization (issue #41, audit
 * finding H1): Circuit.save used to clear it as a side effect, so a save
 * whose file write later failed silently disabled the "save changes?"
 * prompt and the checkpoint pipeline. Serialization now observes; only an
 * explicit clearChanged() — called after a successful write — clears.
 */
class CircuitChangedFlagTest {

	private static String serialize(Circuit circuit) {
		StringWriter text = new StringWriter();
		try (PrintWriter output = new PrintWriter(text)) {
			circuit.save(output);
		}
		return text.toString();
	}

	@Test
	void serializationDoesNotClearChangedFlag() {
		Circuit circuit = new Circuit("flagged");
		circuit.markChanged();
		assertTrue(circuit.hasChanged());

		serialize(circuit);

		assertTrue(circuit.hasChanged(),
				"serializing (for a save, snapshot, checkpoint or crash dump) "
				+ "must not clear the unsaved-changes flag");
	}

	@Test
	void clearChangedClearsTheFlag() {
		Circuit circuit = new Circuit("flagged");
		circuit.markChanged();

		circuit.clearChanged();

		assertFalse(circuit.hasChanged());
	}

	@Test
	void newCircuitStartsUnchanged() {
		assertFalse(new Circuit("fresh").hasChanged());
	}
}
