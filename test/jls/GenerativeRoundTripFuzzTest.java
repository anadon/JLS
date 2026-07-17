package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

/**
 * Seeded generative round-trip fuzzing (issue #160). The existing
 * round-trip suites state the property - save -> load -> save is a
 * fixed point, and a rejected load is always a classified LoadError,
 * never an escaped exception - but evaluate it only at hand-enumerated
 * fixtures. This harness evaluates the same two properties at
 * pseudo-random points of the input space: random element mixes,
 * parameter values at and around their boundaries, names exercising
 * the escape space, and randomly wired pin pairs.
 *
 * Deterministic seeds: failures must reproduce (the in-tree precedent
 * is DragCandidateBoundTest). Dependency-free by decision - jqwik was
 * rejected under the active-maintenance policy
 * (docs/library-survey-2026-07.md), so the generator is plain
 * java.util.Random over the save-format text grammar.
 */
class GenerativeRoundTripFuzzTest {

	/** Seeds per property run; each seed generates one circuit. */
	private static final int SEEDS = 100;

	/** Bit widths at and around the boundaries elements care about. */
	private static final int[] BITS = {1, 2, 3, 4, 8, 16, 31, 32};

	private static final String[] GATES = {
			"AndGate", "OrGate", "NandGate", "NorGate", "XorGate"};

	private static final String[] LOWER_ORIENT = {"up", "down", "left", "right"};

	private static final String[] UPPER_ORIENT = {"UP", "DOWN", "LEFT", "RIGHT"};

	/** Strings that exercise the escape space pinned by issue #53. */
	private static final String[] HOSTILE_TEXT = {
			"plain", "with \"quotes\"", "back\\slash", "trailing\\",
			"line\nbreak", "\\n literal", "mixed \\\" \n end\\",
			"\"", "\\", "", "two\n\nbreaks"};

	// ------------------------------------------------------------------
	// generator
	// ------------------------------------------------------------------

	/**
	 * One generated element block. Coordinates are unique per element
	 * (the canonicalizer needs pairwise-distinct blocks).
	 */
	private static String el(String type, int id, int x, int y, String attrs) {
		return "ELEMENT " + type + "\n int id " + id + "\n int x " + x
				+ "\n int y " + y + "\n" + attrs + "END\n";
	}

	private static String esc(String s) {
		return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
	}

	private static int pick(Random rng, int[] values) {
		return values[rng.nextInt(values.length)];
	}

	private static String pick(Random rng, String[] values) {
		return values[rng.nextInt(values.length)];
	}

	/**
	 * Generate a random circuit in the on-disk text format. Every
	 * element gets a distinct grid position so canonicalization can
	 * sort blocks without ties.
	 */
	private static String generate(long seed) {
		Random rng = new Random(seed);
		StringBuilder t = new StringBuilder("CIRCUIT fuzz\n");
		int id = 0;
		int x = 0;
		int count = 3 + rng.nextInt(20);
		for (int i = 0; i < count; i++) {
			x += 120; // unique per element
			int y = 120 + 12 * rng.nextInt(4);
			switch (rng.nextInt(12)) {
			case 0 -> t.append(el(pick(rng, GATES), id++, x, y,
					" int bits " + pick(rng, BITS)
					+ "\n int numInputs " + (2 + rng.nextInt(4))
					+ "\n String orientation \"" + pick(rng, LOWER_ORIENT)
					+ "\"\n int delay " + rng.nextInt(100) + "\n"));
			case 1 -> t.append(el("NotGate", id++, x, y,
					" int bits 1\n int numInputs 1\n String orientation \""
					+ pick(rng, LOWER_ORIENT) + "\"\n int delay "
					+ rng.nextInt(50) + "\n"));
			case 2 -> t.append(el("Adder", id++, x, y,
					" int bits " + pick(rng, BITS) + "\n String orient \""
					+ pick(rng, UPPER_ORIENT) + "\"\n int delay "
					+ (1 + rng.nextInt(40)) + "\n"));
			case 3 -> t.append(el("Clock", id++, x, y,
					" int cycle " + (2 + 2 * rng.nextInt(50)) + "\n int one "
					+ (1 + rng.nextInt(50)) + "\n String orient \""
					+ pick(rng, UPPER_ORIENT) + "\"\n"));
			case 4 -> {
				// random-width constants, including values needing BigInteger
				BigInteger value = new BigInteger(1 + rng.nextInt(96), rng);
				t.append(el("Constant", id++, x, y,
						" int width 24\n int height 24\n Int value " + value
						+ "\n int base " + new int[] {2, 10, 16}[rng.nextInt(3)]
						+ "\n String orient \"" + pick(rng, UPPER_ORIENT)
						+ "\"\n"));
			}
			case 5 -> t.append(el("Decoder", id++, x, y,
					" int bits " + (1 + rng.nextInt(6)) + "\n int delay "
					+ rng.nextInt(30) + "\n String orient \""
					+ pick(rng, UPPER_ORIENT) + "\"\n"));
			case 6 -> t.append(el("Display", id++, x, y,
					" int bits " + pick(rng, BITS) + "\n int base "
					+ new int[] {2, 10, 16}[rng.nextInt(3)] + "\n"));
			case 7 -> {
				int bits = pick(rng, BITS);
				int cap = 4 << rng.nextInt(4);
				StringBuilder init = new StringBuilder();
				int rows = rng.nextInt(4);
				for (int r = 0; r < rows; r++) {
					init.append(rng.nextInt(cap)).append(' ')
							.append(rng.nextInt(1 << Math.min(bits, 16)))
							.append("\\n");
				}
				t.append(el("Memory", id++, x, y,
						" String name \"mem" + id + "\"\n String type \""
						+ (rng.nextBoolean() ? "ROM" : "RAM") + "\"\n int bits "
						+ bits + "\n int cap " + cap + "\n int time "
						+ (1 + rng.nextInt(20)) + "\n int watch 0\n"
						+ " String file \"\"\n String init \"" + init + "\"\n"));
			}
			case 8 -> t.append(el("Mux", id++, x, y,
					" int inputs " + (2 + rng.nextInt(6)) + "\n int bits "
					+ pick(rng, BITS) + "\n int delay " + rng.nextInt(30)
					+ "\n String iOrient \"" + pick(rng, UPPER_ORIENT)
					+ "\"\n String sOrient \"" + pick(rng, UPPER_ORIENT)
					+ "\"\n"));
			case 9 -> t.append(el("Register", id++, x, y,
					" String name \"reg" + id + "\"\n int bits "
					+ pick(rng, BITS) + "\n Int init "
					+ new BigInteger(1 + rng.nextInt(31), rng)
					+ "\n String orient \"" + pick(rng, UPPER_ORIENT)
					+ "\"\n int delay " + (1 + rng.nextInt(20))
					+ "\n String type \"" + (rng.nextBoolean() ? "nff" : "pff")
					+ "\"\n int watch " + rng.nextInt(2) + "\n"));
			case 10 -> t.append(el("Text", id++, x, y,
					" String text \"" + esc(pick(rng, HOSTILE_TEXT))
					+ "\"\n String fn \"Dialog\"\n int fs "
					+ (8 + rng.nextInt(24)) + "\n int bold " + rng.nextInt(2)
					+ "\n int ital " + rng.nextInt(2)
					+ "\n int color -16777216\n"));
			default -> {
				// the control input must be perpendicular to the gate
				boolean vertical = rng.nextBoolean();
				String gorient = vertical
						? (rng.nextBoolean() ? "UP" : "DOWN")
						: (rng.nextBoolean() ? "LEFT" : "RIGHT");
				String corient = vertical
						? (rng.nextBoolean() ? "LEFT" : "RIGHT")
						: (rng.nextBoolean() ? "UP" : "DOWN");
				t.append(el("TriState", id++, x, y,
						" int bits " + pick(rng, BITS) + "\n int delay "
						+ rng.nextInt(30) + "\n String Gorient \"" + gorient
						+ "\"\n String Corient \"" + corient + "\"\n"));
			}
			}
		}
		// random wired pin pairs, following the fixture pattern of the
		// element round-trip suite: source pin -> wire -> sink pin
		int pairs = rng.nextInt(4);
		for (int p = 0; p < pairs; p++) {
			int bits = pick(rng, BITS);
			x += 120;
			int src = id++;
			t.append(el("InputPin", src, x, 600,
					" String name \"wsrc" + p + "\"\n int bits " + bits
					+ "\n int watch 0\n String orient \"RIGHT\"\n"));
			int dst = id++;
			t.append(el("OutputPin", dst, x + 48, 720,
					" String name \"wdst" + p + "\"\n int bits " + bits
					+ "\n int watch 0\n String orient \"LEFT\"\n"));
			int ea = id++;
			int eb = id++;
			t.append("ELEMENT WireEnd\n int id " + ea + "\n int x " + (x + 12)
					+ "\n int y 612\n int width 8\n int height 8\n"
					+ " String put \"output\"\n ref attach " + src
					+ "\n ref wire " + eb + "\nEND\n");
			t.append("ELEMENT WireEnd\n int id " + eb + "\n int x " + (x + 48)
					+ "\n int y 732\n int width 8\n int height 8\n"
					+ " String put \"input\"\n ref attach " + dst
					+ "\n ref wire " + ea + "\nEND\n");
		}
		t.append("ENDCIRCUIT\n");
		return t.toString();
	}

	// ------------------------------------------------------------------
	// canonicalization (ref-aware; see AllElementsRoundTripTest)
	// ------------------------------------------------------------------

	/**
	 * Canonical form for saves of circuits that may contain ref lines:
	 * blocks sorted by ref-masked content, ids renumbered in sorted
	 * order, ref targets rewritten to the new ids. Sound only when the
	 * masked blocks are pairwise distinct, which the generator
	 * guarantees by giving every element a unique x coordinate.
	 */
	private static String canonicalize(String saved, long seed) {
		List<String> blocks = new ArrayList<String>();
		StringBuilder current = null;
		StringBuilder header = new StringBuilder();
		for (String line : saved.split("\n")) {
			if (line.startsWith("ELEMENT")) {
				current = new StringBuilder();
			}
			if (current == null) {
				header.append(line).append('\n');
			} else {
				current.append(line).append('\n');
			}
			if (line.equals("END") && current != null) {
				blocks.add(current.toString());
				current = null;
			}
		}
		List<String> masked = new ArrayList<String>();
		for (String b : blocks) {
			masked.add(mask(b));
		}
		List<Integer> order = new ArrayList<Integer>();
		for (int i = 0; i < blocks.size(); i++) {
			order.add(i);
		}
		order.sort((a, b) -> masked.get(a).compareTo(masked.get(b)));
		for (int i = 1; i < order.size(); i++) {
			assertTrue(!masked.get(order.get(i - 1)).equals(masked.get(order.get(i))),
					"seed " + seed + ": generated blocks must be pairwise"
					+ " distinct for canonicalization");
		}
		Map<String, String> newId = new HashMap<String, String>();
		for (int pos = 0; pos < order.size(); pos++) {
			for (String line : blocks.get(order.get(pos)).split("\n")) {
				if (line.startsWith(" int id ")) {
					newId.put(line.substring(" int id ".length()).trim(),
							Integer.toString(pos));
				}
			}
		}
		StringBuilder out = new StringBuilder(header);
		for (int pos = 0; pos < order.size(); pos++) {
			for (String line : blocks.get(order.get(pos)).split("\n")) {
				if (line.startsWith(" int id ")) {
					out.append(" int id ").append(pos).append('\n');
				} else if (line.startsWith(" ref ")) {
					String[] parts = line.trim().split("\\s+");
					out.append(" ref ").append(parts[1]).append(' ')
							.append(newId.get(parts[2])).append('\n');
				} else {
					out.append(line).append('\n');
				}
			}
		}
		return out.toString();
	}

	/** A block with its id and ref targets hidden, for stable sorting. */
	private static String mask(String block) {
		StringBuilder out = new StringBuilder();
		for (String line : block.split("\n")) {
			if (line.startsWith(" int id ")) {
				continue;
			}
			if (line.startsWith(" ref ")) {
				String[] parts = line.trim().split("\\s+");
				out.append(" ref ").append(parts[1]).append(" ?\n");
			} else {
				out.append(line).append('\n');
			}
		}
		return out.toString();
	}

	// ------------------------------------------------------------------
	// harness
	// ------------------------------------------------------------------

	private static String save(Circuit circuit) {
		StringWriter out = new StringWriter();
		try (PrintWriter writer = new PrintWriter(out)) {
			circuit.save(writer);
		}
		return out.toString();
	}

	/**
	 * Load a generated text. Returns the circuit on success and null on
	 * a classified rejection; anything thrown is a harness failure
	 * (the classify-never-crash contract of issue #58).
	 */
	private static Circuit loadClassified(String text, long seed) {
		Circuit circuit = new Circuit("fuzz");
		boolean ok;
		try {
			ok = circuit.load(new Scanner(text));
		} catch (RuntimeException | Error e) {
			throw new AssertionError("seed " + seed
					+ ": load threw instead of classifying:\n" + text, e);
		}
		if (!ok) {
			assertTrue(JLSInfo.lastLoadError != null,
					"seed " + seed + ": rejected load must publish a"
					+ " structured LoadError");
			return null;
		}
		BufferedImage img = new BufferedImage(64, 64,
				BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		try {
			ok = circuit.finishLoad(g);
		} catch (Exception | Error e) {
			throw new AssertionError("seed " + seed
					+ ": finishLoad threw instead of classifying:\n" + text, e);
		} finally {
			g.dispose();
		}
		if (!ok) {
			assertTrue(JLSInfo.lastLoadError != null,
					"seed " + seed + ": rejected finishLoad must publish a"
					+ " structured LoadError");
			return null;
		}
		return circuit;
	}

	@Test
	void generatedCircuitsRoundTripToAFixedPoint() {
		int loaded = 0;
		for (long seed = 0; seed < SEEDS; seed++) {
			String text = generate(seed);
			Circuit first = loadClassified(text, seed);
			if (first == null) {
				continue; // classified rejection is a legal outcome
			}
			loaded += 1;
			String savedOnce = save(first);
			Circuit second = loadClassified(savedOnce, seed);
			if (second == null) {
				fail("seed " + seed + ": a circuit that loaded and saved"
						+ " must load again; got: " + JLSInfo.loadError);
			}
			assertEquals(canonicalize(savedOnce, seed),
					canonicalize(save(second), seed),
					"seed " + seed + ": save -> load -> save must be a"
					+ " fixed point");
		}
		// guard the generator itself: if almost everything is rejected,
		// the property above is vacuous and the generator has drifted
		assertTrue(loaded >= SEEDS * 3 / 4,
				"generator drift: only " + loaded + " of " + SEEDS
				+ " generated circuits loaded");
	}

	@Test
	void truncatedGeneratedCircuitsAreClassifiedNeverThrown() {
		// structural mutation at the text layer: every prefix boundary
		// of a valid save must classify cleanly (issue #58's contract);
		// byte-level container mutation lives in ContainerMutationFuzzTest
		for (long seed = 0; seed < SEEDS / 4; seed++) {
			String text = generate(seed);
			Random rng = new Random(~seed);
			for (int i = 0; i < 8; i++) {
				int cut = rng.nextInt(text.length());
				Circuit circuit = loadClassified(text.substring(0, cut), seed);
				if (circuit == null) {
					continue; // classified rejection, as required
				}
				// a prefix that still loads (e.g. cut inside trailing
				// whitespace) must still round-trip
				String savedOnce = save(circuit);
				Circuit again = loadClassified(savedOnce, seed);
				assertTrue(again != null,
						"seed " + seed + ": reload after truncation-load"
						+ " failed: " + JLSInfo.loadError);
			}
		}
	}
}
