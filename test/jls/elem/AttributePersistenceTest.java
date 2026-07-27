package jls.elem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import jls.Circuit;
import jls.JLSInfo;

/**
 * Declarative attribute persistence (#23): a single per-element attribute
 * declaration drives save, load dispatch, and copy. These tests pin the
 * three consistency properties for the converted pilot (Element base
 * attributes + Constant): byte-exact save output, loader compatibility
 * including the legacy long-typed value, and copy field-equivalence
 * (the historical drift defect broke only paste/undo).
 */
class AttributePersistenceTest {

	private static Circuit load(String text) throws Exception {
		Circuit circuit = new Circuit("attrtest");
		assertTrue(circuit.load(new Scanner(text)), () -> "load: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null), () -> "finishLoad: " + JLSInfo.loadError);
		return circuit;
	}

	private static String saveElement(Element el) {
		StringWriter out = new StringWriter();
		try (PrintWriter writer = new PrintWriter(out)) {
			el.save(writer);
		}
		return out.toString();
	}

	@Test
	void savedBytesMatchTheHandwrittenFormat() throws Exception {
		Circuit circuit = load("CIRCUIT attrtest\n"
				+ "ELEMENT Constant\n"
				+ " int id 0\n int x 60\n int y 72\n int width 24\n int height 24\n"
				+ " int fixed 1\n int trpos 3\n"
				+ " Int value 255\n int base 16\n String orient \"LEFT\"\n"
				+ "END\n"
				+ "ENDCIRCUIT\n");
		Element constant = circuit.getElements().iterator().next();
		constant.setID(0);

		// the sid line is the one post-handwritten addition: permanent
		// element identity (#165), minted deterministically at load for
		// pre-sid files like this fixture
		assertEquals("ELEMENT Constant\n"
				+ " int id 0\n"
				+ " int x 60\n"
				+ " int y 72\n"
				+ " int width 24\n"
				+ " int height 24\n"
				+ " int fixed 1\n"
				+ " int trpos 3\n"
				+ " String sid \"legacy:0\"\n"
				+ " Int value 255\n"
				+ " int base 16\n"
				+ " String orient \"LEFT\"\n"
				+ "END\n",
				saveElement(constant).replace(System.lineSeparator(), "\n"),
				"registry-driven save must be byte-identical to the handwritten format");
	}

	@Test
	void defaultsAreOmittedExactlyAsBefore() throws Exception {
		// no fixed, no trpos: those lines must be absent
		Circuit circuit = load("CIRCUIT attrtest\n"
				+ "ELEMENT Constant\n"
				+ " int id 0\n int x 60\n int y 72\n int width 24\n int height 24\n"
				+ " Int value 5\n int base 10\n String orient \"RIGHT\"\n"
				+ "END\n"
				+ "ENDCIRCUIT\n");
		Element constant = circuit.getElements().iterator().next();
		String saved = saveElement(constant);
		assertTrue(!saved.contains("fixed") && !saved.contains("trpos"),
				"omitted attributes must stay omitted:\n" + saved);
	}

	@Test
	void legacyLongValueStillLoads() throws Exception {
		// old JLS versions saved the constant value as a long
		Circuit circuit = load("CIRCUIT attrtest\n"
				+ "ELEMENT Constant\n"
				+ " int id 0\n int x 60\n int y 72\n int width 24\n int height 24\n"
				+ " long value 42\n int base 10\n String orient \"RIGHT\"\n"
				+ "END\n"
				+ "ENDCIRCUIT\n");
		Element constant = circuit.getElements().iterator().next();
		assertTrue(saveElement(constant).contains(" Int value 42"),
				"a legacy long value must load into the BigInteger attribute");
	}

	@Test
	void copyIsFieldEquivalent() throws Exception {
		Circuit circuit = load("CIRCUIT attrtest\n"
				+ "ELEMENT Constant\n"
				+ " int id 0\n int x 96\n int y 132\n int width 24\n int height 24\n"
				+ " int trpos 7\n"
				+ " Int value 12345678901234567890\n int base 16\n String orient \"UP\"\n"
				+ "END\n"
				+ "ENDCIRCUIT\n");
		Element constant = circuit.getElements().iterator().next();
		Element copy = constant.copy();
		assertNotNull(copy);
		constant.setID(0);
		copy.setID(0);
		// the stable id is deliberately not field-equivalent: a copy is
		// a new element and mints a fresh identity (#165 P3)
		assertEquals(withoutSid(saveElement(constant)),
				withoutSid(saveElement(copy)),
				"a copy must serialize identically to its original");
	}

	@Test
	void memorySyncWriteAttributeRoundTrips() throws Exception {
		// the synchronous-write mode (issue #199) saves as "int sync 1"
		Circuit circuit = load("CIRCUIT attrtest\n"
				+ "ELEMENT Memory\n"
				+ " int id 0\n int x 60\n int y 60\n int width 48\n int height 64\n"
				+ " String name \"m1\"\n String type \"RAM\"\n"
				+ " int bits 8\n int cap 4\n int time 10\n int sync 1\n"
				+ " int watch 0\n String file \"\"\n String init \"\"\n"
				+ "END\n"
				+ "ENDCIRCUIT\n");
		Memory mem = (Memory) circuit.getElements().iterator().next();
		assertTrue(mem.isSyncWrite(),
				"int sync 1 must load the synchronous-write mode");
		assertTrue(saveElement(mem).contains(" int sync 1"),
				"the synchronous-write mode must survive a re-save");
		Memory copy = (Memory) mem.copy();
		assertTrue(copy.isSyncWrite(),
				"copy must carry the synchronous-write mode (#23 drift)");
	}

	@Test
	void memoryWithoutSyncAttributeStaysAsync() throws Exception {
		// every pre-#199 file omits sync and must keep today's
		// level-sensitive writes, byte-identically on re-save
		Circuit circuit = load("CIRCUIT attrtest\n"
				+ "ELEMENT Memory\n"
				+ " int id 0\n int x 60\n int y 60\n int width 48\n int height 64\n"
				+ " String name \"m1\"\n String type \"RAM\"\n"
				+ " int bits 8\n int cap 4\n int time 10\n"
				+ " int watch 0\n String file \"\"\n String init \"\"\n"
				+ "END\n"
				+ "ENDCIRCUIT\n");
		Memory mem = (Memory) circuit.getElements().iterator().next();
		assertFalse(mem.isSyncWrite(),
				"an omitted sync attribute must default to async");
		assertFalse(saveElement(mem).contains("sync"),
				"the default must stay omitted on re-save");
	}

	/** Saved text minus the sid line, which copy() deliberately skips. */
	private static String withoutSid(String saved) {
		StringBuilder kept = new StringBuilder();
		for (String line : saved.split("\n")) {
			if (!line.startsWith(" String sid ")) {
				kept.append(line).append('\n');
			}
		}
		return kept.toString();
	}
}
