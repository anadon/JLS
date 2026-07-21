package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.BitSet;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import jls.elem.Element;
import jls.elem.Memory;
import jls.elem.Register;
import jls.sim.BatchSimulator;

/**
 * End-to-end integration golden: a complete single-cycle RV32I CPU, built
 * entirely from ordinary JLS elements (adders, muxes, barrel shifters,
 * flip-flops, an instruction ROM and a data RAM), running a real program.
 *
 * <p>The fixture {@code test/fixtures/riscv-sum1to10.jls} is the CPU with the
 * program in {@code riscv/examples/sum1to10.s} baked into its instruction ROM:
 * it sums the integers 1..10 into register {@code x1} (= 55) and stores the
 * result to data-memory word 0. The clock is an input pin stepped by a
 * generated {@code -t} vector for exactly the 34 cycles the program needs.
 *
 * <p>Unlike the per-element goldens, this exercises the whole simulator at once
 * -- edge-triggered registers with combinational feedback, ROM and RAM access
 * timing, gated memory writes, splitters/binders, multiplexers, the adder, and
 * multi-thousand-event scheduling -- against an independent oracle (the
 * reference emulator that produced the expected values). Any change that alters
 * gate logic, edge timing, memory semantics, or value propagation in a way that
 * breaks a running processor fails here even if the unit goldens still pass.
 *
 * <p>See {@code riscv/README.md} for how the circuit and fixture are generated.
 */
class RiscvCpuGoldenTest {

	private static final Path FIXTURE =
			Path.of("test", "fixtures", "riscv-sum1to10.jls");

	/** The program runs 34 dynamic instructions (computed by the reference
	 * emulator); one clock rising edge per instruction. */
	private static final int STEPS = 34;
	private static final int HALF = 1000;   // half clock period, sim time units

	/** A clk waveform with exactly STEPS rising edges, then held low. */
	private static String clockVector() {
		StringBuilder b = new StringBuilder("clk 0");
		for (int k = 1; k <= 2 * STEPS; k++) {
			b.append(" until ").append(k * HALF).append(' ')
					.append(k % 2 == 1 ? 1 : 0);
		}
		return b.append(" end\n").toString();
	}

	@Test
	void sumsOneToTenInHardware() throws Exception {
		String text = Files.readString(FIXTURE, StandardCharsets.UTF_8);
		Circuit circuit = new Circuit("riscv");
		assertTrue(circuit.load(new Scanner(text)),
				() -> "load failed: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
				() -> "finishLoad failed: " + JLSInfo.loadError);

		Path vectorFile = Files.createTempFile("riscv-clk", ".txt");
		Files.writeString(vectorFile, clockVector());
		try {
			BatchSimulator sim = new BatchSimulator();
			sim.setCircuit(circuit);
			sim.setTimeLimit(2L * STEPS * HALF);
			sim.setTestFile(vectorFile.toString());
			sim.addTestGen();
			sim.runSim();
		} finally {
			Files.deleteIfExists(vectorFile);
		}

		// x1 = sum(1..10) = 55; x2 = 11 (loop counter past limit); x3 = 11.
		assertEquals(55, register(circuit, "x1"), "x1 must hold sum(1..10)");
		assertEquals(11, register(circuit, "x2"), "x2 (loop counter)");
		assertEquals(11, register(circuit, "x3"), "x3 (loop limit)");
		// the store put 55 into data memory word 0.
		assertEquals(55, memoryWord(circuit, "dmem", 0),
				"data memory word 0 must hold the stored sum");
	}

	private static long register(Circuit circuit, String name) {
		for (Element el : circuit.getElements()) {
			if (el instanceof Register r && name.equals(r.getName())) {
				BitSet v = r.getCurrentValue();
				return v == null ? 0 : BitSetUtils.ToLong(v);
			}
		}
		throw new AssertionError("no register named " + name);
	}

	private static long memoryWord(Circuit circuit, String name, int addr) {
		for (Element el : circuit.getElements()) {
			if (el instanceof Memory m && name.equals(m.getName())) {
				BitSet v = m.getCurrentValue(addr);
				return v == null ? 0 : BitSetUtils.ToLong(v);
			}
		}
		throw new AssertionError("no memory named " + name);
	}
}
