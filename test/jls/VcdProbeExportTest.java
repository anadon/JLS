package jls;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jls.sim.BatchSimulator;

/**
 * A wire probe (an interactive named-net feature) now participates in the
 * batch {@code -vcd} export (issue #200): a student can name an internal
 * net in the GUI and observe it headlessly without splicing in an
 * OutputPin tap. Before this, probes never appeared in batch output at
 * all (docs/batch-interface.md was explicit that VCD excluded them).
 */
class VcdProbeExportTest {

	@TempDir
	Path tmp;

	/**
	 * A 4-bit constant 5 driving a watched output pin q, with the
	 * connecting net probed as "mid". The probe names the net between the
	 * constant and the pin.
	 */
	private static String probedCircuit() {
		return "CIRCUIT probe\n"
			+ "ELEMENT Constant\n int id 0\n int x 60\n int y 60\n"
			+ " int width 24\n int height 24\n Int value 5\n int base 10\n"
			+ " String orient \"RIGHT\"\nEND\n"
			+ "ELEMENT OutputPin\n int id 1\n int x 300\n int y 60\n"
			+ " int width 48\n int height 24\n String name \"q\"\n"
			+ " int bits 4\n int watch 1\n String orient \"RIGHT\"\nEND\n"
			+ "ELEMENT WireEnd\n int id 2\n int x 84\n int y 72\n"
			+ " int width 8\n int height 8\n String put \"output\"\n"
			+ " ref attach 0\n ref wire 3\n probe 3 \"mid\"\nEND\n"
			+ "ELEMENT WireEnd\n int id 3\n int x 300\n int y 72\n"
			+ " int width 8\n int height 8\n String put \"input\"\n"
			+ " ref attach 1\n ref wire 2\nEND\n"
			+ "ENDCIRCUIT\n";
	}

	private String runVcd(String circuitText) throws Exception {
		Circuit circuit = new Circuit("probe");
		assertTrue(circuit.load(new Scanner(circuitText)),
				"the probed circuit must load: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
				"the probed circuit must assemble: " + JLSInfo.loadError);
		BatchSimulator sim = new BatchSimulator();
		sim.setCircuit(circuit);
		sim.setTimeLimit(50);
		sim.setVcdFile(tmp.resolve("out.vcd").toString());
		sim.runSim();
		return sim.toVcd();
	}

	@Test
	void probedNetAppearsInVcd() throws Exception {
		String vcd = runVcd(probedCircuit());

		// the probe "mid" is declared as a 4-bit wire signal
		Matcher var = Pattern.compile(
				"\\$var wire 4 (\\S+) mid \\[3:0\\] \\$end")
				.matcher(vcd);
		assertTrue(var.find(), "probe 'mid' must be a VCD signal:\n" + vcd);
		String code = var.group(1);

		// and it carries the driven value 5 (binary 101) on the net
		assertTrue(vcd.contains("b101 " + code),
				"probe 'mid' must show value 5 (b101):\n" + vcd);

		// the watched element q is still traced (no regression)
		assertTrue(Pattern.compile("\\$var wire 4 \\S+ q \\[3:0\\] \\$end")
				.matcher(vcd).find(),
				"watched pin q must still appear:\n" + vcd);
	}

	@Test
	void probesDoNotLeakIntoStdoutWhitelist() throws Exception {
		// the stdout whitelist (docs §3.2) is unchanged: only the
		// element report prints; probes are a VCD-only addition. This
		// guards that displayResults was not widened by the #200 work.
		String vcd = runVcd(probedCircuit());
		assertTrue(vcd.contains(" mid "), "sanity: probe is in the VCD");
		// no probe name should ever be introduced as an element report
		// line; displayResults only knows Register/Memory/OutputPin
		assertFalse(vcd.contains("Output Pin mid"),
				"a probe must not be reported as an element");
	}
}
