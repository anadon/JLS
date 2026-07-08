package jls;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZOutputStream;

/**
 * Measurement harness for issue #21: prints the byte attribution of a
 * saved wiring-heavy circuit by element type, and xz sizes at presets
 * 6 and 9. Deliberately named so surefire's *Test pattern skips it in
 * CI; run on demand with: mvn test -Dtest=SizeMeasurement
 */
class SizeMeasurement {

	@Test
	void measure() throws Exception {
		// chain of 300 NOT gates: constant -> not0 -> ... -> pin
		StringBuilder t = new StringBuilder("CIRCUIT big\n");
		int id = 0;
		int constId = id++;
		t.append("ELEMENT Constant\n int id ").append(constId)
		 .append("\n int x 60\n int y 60\n int width 24\n int height 24\n Int value 1\n int base 10\n String orient \"RIGHT\"\nEND\n");
		int prev = constId;
		String prevPut = "output";
		int gates = 300;
		for (int i = 0; i < gates; i++) {
			int g = id++;
			t.append("ELEMENT NotGate\n int id ").append(g)
			 .append("\n int x ").append(120 + 12 * i).append("\n int y 120\n int width 48\n int height 24\n int bits 1\n int numInputs 1\n String orientation \"right\"\n int delay 10\nEND\n");
			int ea = id++, eb = id++;
			t.append("ELEMENT WireEnd\n int id ").append(ea).append("\n int x ").append(12 * ea)
			 .append("\n int y 240\n int width 8\n int height 8\n String put \"").append(prevPut)
			 .append("\"\n ref attach ").append(prev).append("\n ref wire ").append(eb).append("\nEND\n");
			t.append("ELEMENT WireEnd\n int id ").append(eb).append("\n int x ").append(12 * eb)
			 .append("\n int y 240\n int width 8\n int height 8\n String put \"input0\"\n ref attach ")
			 .append(g).append("\n ref wire ").append(ea).append("\nEND\n");
			prev = g;
			prevPut = "output";
		}
		int pin = id++;
		t.append("ELEMENT OutputPin\n int id ").append(pin)
		 .append("\n int x 900\n int y 120\n int width 48\n int height 24\n String name \"out\"\n int bits 1\n int watch 1\n String orient \"RIGHT\"\nEND\n");
		int ea = id++, eb = id++;
		t.append("ELEMENT WireEnd\n int id ").append(ea).append("\n int x ").append(12 * ea)
		 .append("\n int y 240\n int width 8\n int height 8\n String put \"output\"\n ref attach ").append(prev)
		 .append("\n ref wire ").append(eb).append("\nEND\n");
		t.append("ELEMENT WireEnd\n int id ").append(eb).append("\n int x ").append(12 * eb)
		 .append("\n int y 240\n int width 8\n int height 8\n String put \"input\"\n ref attach ").append(pin)
		 .append("\n ref wire ").append(ea).append("\nEND\n");
		t.append("ENDCIRCUIT\n");

		Circuit c = new Circuit("big");
		if (!c.load(new Scanner(t.toString())) || !c.finishLoad(null))
			throw new AssertionError("load failed: " + JLSInfo.loadError);

		StringWriter sw = new StringWriter();
		try (PrintWriter pw = new PrintWriter(sw)) {
			c.save(pw);
		}
		String saved = sw.toString();

		// attribute bytes per ELEMENT type
		Map<String,Integer> bytesPer = new LinkedHashMap<>();
		Map<String,Integer> countPer = new LinkedHashMap<>();
		String current = "(header)";
		for (String line : saved.split("\n")) {
			if (line.startsWith("ELEMENT ")) {
				current = line.substring(8).trim();
				countPer.merge(current, 1, Integer::sum);
			}
			bytesPer.merge(current, line.length() + 1, Integer::sum);
			if (line.equals("END"))
				current = "(header)";
		}
		System.out.println("=== uncompressed attribution (total " + saved.length() + " bytes) ===");
		for (String k : bytesPer.keySet()) {
			System.out.printf("%-12s %8d bytes  %5d blocks%n", k, bytesPer.get(k),
					countPer.getOrDefault(k, 0));
		}
		System.out.println("xz -6: " + xzSize(saved, 6) + " bytes");
		System.out.println("xz -9: " + xzSize(saved, 9) + " bytes");
	}

	private static int xzSize(String text, int preset) throws Exception {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (XZOutputStream xz = new XZOutputStream(bytes, new LZMA2Options(preset))) {
			xz.write(text.getBytes(StandardCharsets.UTF_8));
		}
		return bytes.size();
	}
}
