package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jls.sim.BatchSimulator;

/**
 * Golden tests for the batch-mode VCD export (issue #72, spec in
 * docs/batch-interface.md): a small sequential circuit and a
 * test-vector-driven circuit each produce a VCD that (a) is
 * structurally well-formed IEEE 1364-2001 section 18 (checked by a
 * parser written from the spec document, not from the emitter),
 * (b) matches a committed golden byte for byte (the ordering fix in
 * displayResults/toVcd makes this deterministic), and (c) contains
 * only 0/1/z values (JLS is two-state plus HiZ; 'x' never appears).
 * GTKWave/Surfer validation stays manual, outside CI.
 */
class VcdExportGoldenTest {

	@TempDir
	Path tmp;

	/**
	 * A 4-bit register clocked from a free-running clock (cycle 20,
	 * high 10) capturing constant 5, with a watched 4-bit pin q on Q
	 * and a watched 1-bit pin c on a second identical clock. Run with
	 * time limit 100.
	 */
	private static String waveCircuit() {
		StringBuilder t = new StringBuilder("CIRCUIT wave\n");
		t.append("ELEMENT Clock\n int id 0\n int x 60\n int y 60\n int width 24\n int height 24\n int cycle 20\n int one 10\n String orient \"RIGHT\"\nEND\n");
		t.append("ELEMENT Constant\n int id 1\n int x 60\n int y 120\n int width 24\n int height 24\n Int value 5\n int base 10\n String orient \"RIGHT\"\nEND\n");
		t.append("ELEMENT Register\n int id 2\n int x 240\n int y 120\n int width 60\n int height 60\n String name \"reg2\"\n int bits 4\n Int init 0\n String orient \"RIGHT\"\n int delay 5\n String type \"pff\"\n int watch 0\nEND\n");
		t.append("ELEMENT OutputPin\n int id 3\n int x 480\n int y 120\n int width 48\n int height 24\n String name \"q\"\n int bits 4\n int watch 1\n String orient \"RIGHT\"\nEND\n");
		t.append("ELEMENT Clock\n int id 4\n int x 60\n int y 240\n int width 24\n int height 24\n int cycle 20\n int one 10\n String orient \"RIGHT\"\nEND\n");
		t.append("ELEMENT OutputPin\n int id 5\n int x 480\n int y 240\n int width 48\n int height 24\n String name \"c\"\n int bits 1\n int watch 1\n String orient \"RIGHT\"\nEND\n");
		int id = 6;
		id = wire(t, id, 0, "output", 2, "C");
		id = wire(t, id, 1, "output", 2, "D");
		id = wire(t, id, 2, "Q", 3, "input");
		id = wire(t, id, 4, "output", 5, "input");
		t.append("ENDCIRCUIT\n");
		return t.toString();
	}

	/**
	 * The committed golden VCD for waveCircuit with time limit 100.
	 * Signals in name order (c before q); the register output settles
	 * at 15 (first rising clock edge at 10 plus register delay 5); the
	 * clock pin toggles every 10 time units until the limit.
	 */
	private static final String WAVE_GOLDEN =
			"$comment JLS batch simulation trace $end\n"
			+ "$timescale 1 ns $end\n"
			+ "$scope module wave $end\n"
			+ "$var wire 1 ! c $end\n"
			+ "$var wire 4 \" q [3:0] $end\n"
			+ "$upscope $end\n"
			+ "$enddefinitions $end\n"
			+ "#0\n"
			+ "$dumpvars\n"
			+ "0!\n"
			+ "b0 \"\n"
			+ "$end\n"
			+ "#10\n"
			+ "1!\n"
			+ "#15\n"
			+ "b101 \"\n"
			+ "#20\n"
			+ "0!\n"
			+ "#30\n"
			+ "1!\n"
			+ "#40\n"
			+ "0!\n"
			+ "#50\n"
			+ "1!\n"
			+ "#60\n"
			+ "0!\n"
			+ "#70\n"
			+ "1!\n"
			+ "#80\n"
			+ "0!\n"
			+ "#90\n"
			+ "1!\n"
			+ "#100\n"
			+ "0!\n";

	/** The committed golden batch stdout for the same run. */
	private static final String WAVE_STDOUT_GOLDEN =
			"Simulation Time Limit at 100\n"
			+ "Output Pin c: 0x0 (0 unsigned, 0 signed)\n"
			+ "Output Pin q: 0x5 (5 unsigned, 5 signed)\n";

	/**
	 * An input pin a driven by a -t test vector into a watched pin
	 * out, plus a watched ROM whose active-low chip select is driven
	 * by test-vector signal b, so the memory output starts HiZ and
	 * later delivers the addressed word: exercises the z mapping.
	 */
	private static String stimCircuit() {
		StringBuilder t = new StringBuilder("CIRCUIT stim\n");
		t.append("ELEMENT InputPin\n int id 0\n int x 60\n int y 60\n int width 48\n int height 24\n String name \"a\"\n int bits 1\n int watch 0\n String orient \"RIGHT\"\nEND\n");
		t.append("ELEMENT OutputPin\n int id 1\n int x 480\n int y 60\n int width 48\n int height 24\n String name \"out\"\n int bits 1\n int watch 1\n String orient \"RIGHT\"\nEND\n");
		t.append("ELEMENT Memory\n int id 2\n int x 240\n int y 240\n int width 96\n int height 96\n String name \"mem\"\n String type \"ROM\"\n int bits 8\n int cap 4\n int time 10\n int watch 1\n String file \"\"\n String init \"1 9\"\nEND\n");
		t.append("ELEMENT InputPin\n int id 3\n int x 60\n int y 120\n int width 48\n int height 24\n String name \"b\"\n int bits 1\n int watch 0\n String orient \"RIGHT\"\nEND\n");
		t.append("ELEMENT Constant\n int id 4\n int x 60\n int y 180\n int width 24\n int height 24\n Int value 1\n int base 10\n String orient \"RIGHT\"\nEND\n");
		t.append("ELEMENT Constant\n int id 5\n int x 60\n int y 240\n int width 24\n int height 24\n Int value 0\n int base 10\n String orient \"RIGHT\"\nEND\n");
		int id = 6;
		id = wire(t, id, 0, "output", 1, "input");
		id = wire(t, id, 3, "output", 2, "CS");
		id = wire(t, id, 4, "output", 2, "address");
		id = wire(t, id, 5, "output", 2, "OE");
		t.append("ENDCIRCUIT\n");
		return t.toString();
	}

	/**
	 * The stimulus, in the documented -t grammar: comments, a hex
	 * value, both "for" and "until" steps.
	 */
	private static final String STIM_VECTORS =
			"# stimulus for the -t grammar (docs/batch-interface.md)\n"
			+ "a 0 for 10 0x1 until 30 0 end\n"
			+ "b 1 for 12 0 end\n";

	/**
	 * The committed golden VCD for stimCircuit with time limit 50:
	 * mem is HiZ (bz) until the ROM delivers word 9 (b1001) at 22
	 * (chip select at 12 plus access time 10); out follows the a
	 * vector at 10 and 30.
	 */
	private static final String STIM_GOLDEN =
			"$comment JLS batch simulation trace $end\n"
			+ "$timescale 1 ns $end\n"
			+ "$scope module stim $end\n"
			+ "$var wire 8 ! mem [7:0] $end\n"
			+ "$var wire 1 \" out $end\n"
			+ "$upscope $end\n"
			+ "$enddefinitions $end\n"
			+ "#0\n"
			+ "$dumpvars\n"
			+ "bz !\n"
			+ "0\"\n"
			+ "$end\n"
			+ "#10\n"
			+ "1\"\n"
			+ "#22\n"
			+ "b1001 !\n"
			+ "#30\n"
			+ "0\"\n";

	private static int wire(StringBuilder t, int id, int from,
			String fromPut, int to, String toPut) {
		int a = id, b = id + 1;
		wireEnd(t, a, from, fromPut, b);
		wireEnd(t, b, to, toPut, a);
		return id + 2;
	}

	private static void wireEnd(StringBuilder t, int id, int attach,
			String put, int other) {
		t.append("ELEMENT WireEnd\n int id ").append(id)
			.append("\n int x ").append(12 * id)
			.append("\n int y 600\n int width 8\n int height 8\n String put \"").append(put)
			.append("\"\n ref attach ").append(attach)
			.append("\n ref wire ").append(other).append("\nEND\n");
	}

	private static Circuit load(String circuitText, String name)
			throws Exception {
		Circuit circuit = new Circuit(name);
		assertTrue(circuit.load(new Scanner(circuitText)),
				() -> "load failed: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
				() -> "finishLoad failed: " + JLSInfo.loadError);
		return circuit;
	}

	@Test
	void clockedRegisterVcdMatchesGoldenByteForByte() throws Exception {
		Circuit circuit = load(waveCircuit(), "wave");
		BatchSimulator sim = new BatchSimulator();
		sim.setCircuit(circuit);
		sim.setTimeLimit(100);
		Path out = tmp.resolve("wave.vcd");
		sim.setVcdFile(out.toString());
		sim.runSim();
		assertEquals(WAVE_GOLDEN, sim.toVcd(),
				"the VCD text must match the committed golden exactly");
		sim.writeVcd();
		assertEquals(WAVE_GOLDEN,
				new String(Files.readAllBytes(out), StandardCharsets.UTF_8),
				"the VCD file must match the committed golden byte for byte");
	}

	@Test
	void testVectorStimulusVcdMatchesGoldenAndCoversHiZ() throws Exception {
		Path vectors = tmp.resolve("stim.txt");
		Files.write(vectors, STIM_VECTORS.getBytes(StandardCharsets.UTF_8));
		Circuit circuit = load(stimCircuit(), "stim");
		BatchSimulator sim = new BatchSimulator();
		sim.setCircuit(circuit);
		sim.setTimeLimit(50);
		sim.setTestFile(vectors.toString());
		sim.addTestGen();
		sim.setVcdFile(tmp.resolve("stim.vcd").toString());
		sim.runSim();
		String vcd = sim.toVcd();
		assertEquals(STIM_GOLDEN, vcd,
				"the -t-driven VCD must match the committed golden exactly");
		assertTrue(vcd.contains("bz !"),
				"the undriven ROM output must appear as HiZ (z)");
	}

	/**
	 * Structural check written from docs/batch-interface.md section 4
	 * (not from the emitter): header order, one $var per watched
	 * signal with unique identifier codes, $enddefinitions before the
	 * value section, strictly increasing timestamps, every value
	 * change references a declared identifier, and values use only
	 * 0/1/z (never x).
	 */
	@Test
	void vcdIsStructurallyWellFormedAndTwoStatePlusHiZ() throws Exception {
		for (String vcd : new String[] { WAVE_GOLDEN, STIM_GOLDEN }) {
			List<String> lines = new ArrayList<String>();
			for (String line : vcd.split("\n", -1)) {
				if (!line.isEmpty()) {
					lines.add(line);
				}
			}
			int at = 0;
			assertTrue(lines.get(at++).matches("\\$comment .* \\$end"));
			assertEquals("$timescale 1 ns $end", lines.get(at++));
			assertTrue(lines.get(at++).matches("\\$scope module \\w+ \\$end"));
			Set<String> codes = new HashSet<String>();
			Pattern var = Pattern.compile(
					"\\$var wire (\\d+) ([\\x21-\\x7e]+) (\\w+)( \\[\\d+:0\\])? \\$end");
			while (lines.get(at).startsWith("$var")) {
				Matcher m = var.matcher(lines.get(at));
				assertTrue(m.matches(), "bad $var line: " + lines.get(at));
				int bits = Integer.parseInt(m.group(1));
				assertTrue(bits >= 1, "width must be positive");
				assertEquals(bits > 1, m.group(4) != null,
						"vectors and only vectors carry a [msb:0] range");
				assertTrue(codes.add(m.group(2)),
						"identifier codes must be unique");
				at++;
			}
			assertFalse(codes.isEmpty(), "at least one watched signal");
			assertEquals("$upscope $end", lines.get(at++));
			assertEquals("$enddefinitions $end", lines.get(at++));

			// value section: #0, $dumpvars with one entry per signal,
			// then strictly increasing timestamps with value changes
			assertEquals("#0", lines.get(at++));
			assertEquals("$dumpvars", lines.get(at++));
			Set<String> dumped = new HashSet<String>();
			while (!lines.get(at).equals("$end")) {
				dumped.add(assertValueChange(lines.get(at), codes));
				at++;
			}
			assertEquals(codes, dumped,
					"$dumpvars must dump every declared signal exactly once");
			at++; // skip $end
			long lastTime = 0;
			while (at < lines.size()) {
				String line = lines.get(at);
				if (line.startsWith("#")) {
					long time = Long.parseLong(line.substring(1));
					assertTrue(time > lastTime,
							"timestamps must be strictly increasing");
					lastTime = time;
				} else {
					assertValueChange(line, codes);
				}
				at++;
			}
		}
	}

	/**
	 * Assert one value-change line: scalar [01z]<code> or vector
	 * b[01z]+ <code> per the spec, referencing a declared code, and
	 * never containing 'x'.
	 *
	 * @return the referenced identifier code.
	 */
	private static String assertValueChange(String line, Set<String> codes) {
		Matcher scalar = Pattern.compile("([01z])([\\x21-\\x7e]+)").matcher(line);
		Matcher vector = Pattern.compile("b([01z]+) ([\\x21-\\x7e]+)").matcher(line);
		String code;
		if (vector.matches()) {
			code = vector.group(2);
		} else {
			assertTrue(scalar.matches(), "bad value change: " + line);
			code = scalar.group(2);
		}
		assertTrue(codes.contains(code),
				"value change for undeclared signal: " + line);
		assertFalse(line.contains("x"), "JLS values are 0/1/z only: " + line);
		return code;
	}

	@Test
	void cliVcdFlagWritesTheGoldenFileAndGoldenStdout() throws Exception {
		Files.write(tmp.resolve("wave.jls"),
				waveCircuit().getBytes(StandardCharsets.UTF_8));

		String java = System.getProperty("java.home")
				+ File.separator + "bin" + File.separator + "java";
		List<String> cmd = new ArrayList<String>();
		cmd.add(java);
		cmd.addAll(jls.CoverageAgent.jvmArgs());
		cmd.add("-Djava.awt.headless=true");
		cmd.add("-cp");
		cmd.add(System.getProperty("java.class.path"));
		cmd.add("jls.JLS");
		cmd.add("-b");
		cmd.add("-d");
		cmd.add("100");
		cmd.add("-vcd");
		cmd.add("out.vcd");
		cmd.add("wave.jls");
		ProcessBuilder pb = new ProcessBuilder(cmd);
		pb.directory(tmp.toFile());
		pb.environment().remove("JAVA_TOOL_OPTIONS");
		Process p = pb.start();
		p.getOutputStream().close();
		String stdout = drain(p.getInputStream());
		String stderr = drain(p.getErrorStream());
		assertTrue(p.waitFor(60, TimeUnit.SECONDS), "CLI run timed out");
		assertEquals(0, p.exitValue(), stderr);
		assertEquals("", stderr, "a successful run writes nothing to stderr");
		assertEquals(WAVE_STDOUT_GOLDEN, stdout.replace("\r\n", "\n"),
				"batch stdout must match the committed golden");

		byte[] written = Files.readAllBytes(tmp.resolve("out.vcd"));
		assertEquals(WAVE_GOLDEN,
				new String(written, StandardCharsets.UTF_8),
				"-vcd must write the committed golden byte for byte");

		Circuit reloaded = load(waveCircuit(), "wave");
		assertNotNull(reloaded); // the fixture itself must stay loadable
	}

	private static String drain(InputStream in) throws Exception {
		return new String(in.readAllBytes(), StandardCharsets.UTF_8);
	}
}
