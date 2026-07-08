package jls.elem;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

		assertEquals("ELEMENT Constant\n"
				+ " int id 0\n"
				+ " int x 60\n"
				+ " int y 72\n"
				+ " int width 24\n"
				+ " int height 24\n"
				+ " int fixed 1\n"
				+ " int trpos 3\n"
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
		assertEquals(saveElement(constant), saveElement(copy),
				"a copy must serialize identically to its original");
	}
}
