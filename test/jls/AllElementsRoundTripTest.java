package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import jls.elem.Element;
import jls.elem.Wire;
import jls.elem.WireEnd;

/**
 * Round-trip and copy-equivalence coverage for (nearly) every element
 * type (issues #14 and #23). One fixture circuit contains an instance of
 * each save-file element type; the tests assert that
 *
 * 1. save -> load -> save is a fixed point (the second save is
 *    byte-identical to the first), and
 * 2. copy() preserves every saved attribute (except the save-time id,
 *    which is deliberately never copied) - pinning the historical
 *    defect class where a field added to save/load was missed in copy
 *    and broke only paste/undo.
 *
 * This is the control instrument for converting element persistence to
 * the declarative attribute registry: a conversion is correct only if
 * this suite stays green. Not covered here: SubCircuit (needs a nested
 * circuit file), StateMachine's state list, Binder/Splitter (group
 * plumbing with paired ranges) - those keep their handwritten
 * persistence documented as such until a fixture exists.
 */
class AllElementsRoundTripTest {

	private static String el(String type, int id, int x, int y, String attrs) {
		return "ELEMENT " + type + "\n int id " + id + "\n int x " + x
				+ "\n int y " + y + "\n" + attrs + "END\n";
	}

	/** One instance of each covered element type, plus one real wire. */
	private static String fixture() {
		StringBuilder t = new StringBuilder("CIRCUIT everything\n");
		int id = 0;
		int x = 120;
		// the seven gate kinds (lowercase orientation enum)
		for (String g : new String[] {"AndGate", "OrGate", "NandGate",
				"NorGate", "XorGate"}) {
			t.append(el(g, id++, x += 120, 120,
					" int bits 2\n int numInputs 3\n String orientation \"up\"\n int delay 10\n"));
		}
		t.append(el("NotGate", id++, x += 120, 120,
				" int bits 1\n int numInputs 1\n String orientation \"left\"\n int delay 5\n"));
		t.append(el("DelayGate", id++, x += 120, 120,
				" int bits 1\n int numInputs 1\n String orientation \"right\"\n int delay 20\n"));
		t.append(el("Extend", id++, x += 120, 120,
				" int bits 4\n int numInputs 1\n String orientation \"right\"\n int delay 0\n"));
		t.append(el("Adder", id++, x += 120, 120,
				" int bits 4\n String orient \"UP\"\n int delay 12\n"));
		t.append(el("Clock", id++, x += 120, 120,
				" int cycle 20\n int one 10\n String orient \"DOWN\"\n"));
		t.append(el("Constant", id++, x += 120, 120,
				" Int value 255\n int base 16\n String orient \"LEFT\"\n"));
		t.append(el("Decoder", id++, x += 120, 120,
				" int bits 3\n int delay 10\n String orient \"DOWN\"\n"));
		t.append(el("Display", id++, x += 120, 120,
				" int bits 4\n int base 16\n"));
		t.append(el("InputPin", id++, x += 120, 120,
				" String name \"in1\"\n int bits 4\n int watch 0\n String orient \"RIGHT\"\n"));
		t.append(el("OutputPin", id++, x += 120, 120,
				" String name \"out1\"\n int bits 4\n int watch 1\n String orient \"LEFT\"\n"));
		t.append(el("JumpStart", id++, x += 120, 120,
				" String name \"js\"\n int bits 4\n int watch 0\n String orientation \"RIGHT\"\n"));
		t.append(el("JumpEnd", id++, x += 120, 120,
				" String name \"js\"\n int bits 4\n String orientation \"LEFT\"\n"));
		t.append(el("Memory", id++, x += 120, 360,
				" String name \"mem0\"\n String type \"ROM\"\n int bits 8\n int cap 40\n"
				+ " int time 10\n int watch 0\n String file \"\"\n String init \"0 1\\n1 255\\n\"\n"));
		t.append(el("Mux", id++, x += 120, 120,
				" int inputs 4\n int bits 2\n int delay 10\n String iOrient \"DOWN\"\n String sOrient \"LEFT\"\n"));
		t.append(el("Pause", id++, x += 120, 120, ""));
		t.append(el("Register", id++, x += 120, 120,
				" String name \"reg\"\n int bits 4\n Int init 3\n String orient \"RIGHT\"\n"
				+ " int delay 8\n String type \"nff\"\n int watch 1\n"));
		t.append(el("ShiftRegister", id++, x += 120, 120,
				" String type \"ArithmeticRight\"\n int bits 8\n int delay 25\n"
				+ " String iOrient \"DOWN\"\n String sOrient \"LEFT\"\n"));
		t.append(el("SigGen", id++, x += 120, 120,
				" int width 48\n int height 24\n String signals \"in1 0 1 0 1\\n\"\n"));
		t.append(el("Stop", id++, x += 120, 120, ""));
		t.append(el("Text", id++, x += 120, 120,
				" String text \"hello \\\"world\\\"\"\n String fn \"Dialog\"\n int fs 12\n"
				+ " int bold 1\n int ital 0\n int color -16777216\n"));
		t.append(el("TriState", id++, x += 120, 120,
				" int bits 1\n int delay 10\n String Gorient \"UP\"\n String Corient \"LEFT\"\n"));
		t.append(el("TruthTable", id++, x += 120, 360,
				" String name \"tt\"\n int delay 10\n int rows 2\n int cols 2\n"
				+ " String input \"a\"\n String output \"z\"\n"
				+ " pair 0 0\n pair 0 1\n pair 1 1\n pair 1 0\n"));
		// a real wire: NotGate output attached to an OutputPin input
		int src = id++;
		t.append(el("InputPin", src, x += 120, 600,
				" String name \"wsrc\"\n int bits 1\n int watch 0\n String orient \"RIGHT\"\n"));
		int dst = id++;
		t.append(el("OutputPin", dst, x + 240, 600,
				" String name \"wdst\"\n int bits 1\n int watch 0\n String orient \"LEFT\"\n"));
		int ea = id++;
		int eb = id++;
		t.append("ELEMENT WireEnd\n int id " + ea + "\n int x " + (x + 48)
				+ "\n int y 612\n int width 8\n int height 8\n String put \"output\"\n"
				+ " ref attach " + src + "\n ref wire " + eb + "\nEND\n");
		t.append("ELEMENT WireEnd\n int id " + eb + "\n int x " + (x + 240)
				+ "\n int y 612\n int width 8\n int height 8\n String put \"input\"\n"
				+ " ref attach " + dst + "\n ref wire " + ea + "\nEND\n");
		t.append("ENDCIRCUIT\n");
		return t.toString();
	}

	private static Circuit load(String text) throws Exception {
		Circuit circuit = new Circuit("everything");
		assertTrue(circuit.load(new Scanner(text)),
				() -> "load failed: " + JLSInfo.loadError);
		BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = img.createGraphics();
		boolean ok = circuit.finishLoad(g);
		g.dispose();
		assertTrue(ok, () -> "finishLoad failed: " + JLSInfo.loadError);
		return circuit;
	}

	private static String save(Circuit circuit) {
		StringWriter out = new StringWriter();
		try (PrintWriter writer = new PrintWriter(out)) {
			circuit.save(writer);
		}
		return out.toString();
	}

	private static String saveElement(Element el) {
		StringWriter out = new StringWriter();
		try (PrintWriter writer = new PrintWriter(out)) {
			el.save(writer);
		}
		return out.toString();
	}

	@Test
	void saveLoadIsAFixedPointForEveryElementType() throws Exception {
		Circuit first = load(fixture());
		String savedOnce = save(first);
		Circuit second = load(savedOnce);
		assertEquals(canonicalize(savedOnce), canonicalize(save(second)),
				"save -> load -> save must be a fixed point");
	}

	/**
	 * Reduce a saved file to a canonical form. Elements live in a HashSet,
	 * so block order and the ids assigned at save time are not stable
	 * across load instances; sort the blocks and renumber ids (and the
	 * ref lines that use them) in sorted-block order.
	 */
	private static String canonicalize(String saved) {
		// split into header and ELEMENT..END blocks
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
		// sort by content with ids and ref targets masked
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
					"fixture blocks must be pairwise distinct for canonicalization");
		}
		// renumber: old id -> position in sorted order
		java.util.Map<String, String> newId = new java.util.HashMap<String, String>();
		for (int pos = 0; pos < order.size(); pos++) {
			String block = blocks.get(order.get(pos));
			for (String line : block.split("\n")) {
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

	@Test
	void copyPreservesEverySavedAttribute() throws Exception {
		Circuit circuit = load(fixture());
		int checked = 0;
		for (Element el : circuit.getElements()) {
			// wires and wire ends copy through the selection machinery,
			// not Element.copy(); skip them here
			if (el instanceof Wire || el instanceof WireEnd) {
				continue;
			}
			Element copy = el.copy();
			assertEquals(withoutId(saveElement(el)), withoutId(saveElement(copy)),
					"copy of " + el.getClass().getSimpleName()
					+ " must save identically (except id)");
			checked += 1;
		}
		assertTrue(checked >= 25, "expected to check every fixture element,"
				+ " checked only " + checked);
	}

	/**
	 * Saved text minus the id and sid lines: save() writes both, but
	 * copy() deliberately skips them (the save-time id is reassigned per
	 * save; a copy is a new element with a freshly minted stable id,
	 * issue #165 P3).
	 */
	private static String withoutId(String saved) {
		List<String> kept = new ArrayList<String>();
		for (String line : saved.split("\n")) {
			if (!line.startsWith(" int id ")
					&& !line.startsWith(" String sid ")) {
				kept.add(line);
			}
		}
		return String.join("\n", kept);
	}
}
