package jls.hdl.scan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Issue #63 P2: ports scanned by {@link VerilogHeaderScanner} from the
 * committed corpus must match Yosys {@code write_json} ground truth -
 * name set, direction, and width, per module. When {@code yosys} is
 * not installed the test SKIPS (JUnit assumption), following the
 * GhdlCompileTest pattern, so the suite stays green without an HDL
 * toolchain; a CI job with yosys arms it. The corpus expectations are
 * also hard-coded in {@link VerilogHeaderScannerTest}, so the scanner
 * is still exercised without yosys - this test proves those
 * expectations against an independent implementation.
 */
class YosysGroundTruthTest {

	/** Scratch directory for the generated JSON. */
	@TempDir
	Path tmp;

	@Test
	void scannedPortsMatchYosysWriteJson() throws Exception {
		String yosys = findOnPath("yosys");
		Assumptions.assumeTrue(yosys != null,
				"yosys not installed; skipping ground-truth check");

		List<Path> corpus = new ArrayList<Path>();
		try (DirectoryStream<Path> dir = Files.newDirectoryStream(
				Path.of("test", "resources", "hdl", "scan"), "*.v")) {
			dir.forEach(corpus::add);
		}
		Collections.sort(corpus);
		assertEquals(false, corpus.isEmpty(), "no corpus found");

		for (Path file : corpus) {
			List<ScannedModule> scanned = VerilogHeaderScanner
					.scan(Files.readString(file));
			assertTrue(scanned.size() > 0,
					file + " scanned no modules");
			Map<String, Object> json = runYosys(yosys, file);
			@SuppressWarnings("unchecked")
			Map<String, Object> modules =
					(Map<String, Object>) json.get("modules");
			assertNotNull(modules, file + ": no modules in JSON");
			for (ScannedModule module : scanned) {
				@SuppressWarnings("unchecked")
				Map<String, Object> mod =
						(Map<String, Object>) modules.get(module.name);
				assertNotNull(mod, file + ": yosys did not see module "
						+ module.name);
				@SuppressWarnings("unchecked")
				Map<String, Object> ports =
						(Map<String, Object>) mod.get("ports");
				assertNotNull(ports, file + ": no ports in JSON");
				assertEquals(ports.size(), module.ports().size(),
						file + ": port count of " + module.name);
				for (ScannedPort port : module.ports()) {
					@SuppressWarnings("unchecked")
					Map<String, Object> truth =
							(Map<String, Object>) ports.get(port.name);
					assertNotNull(truth, file + ": yosys did not see"
							+ " port " + port.name);
					assertEquals(truth.get("direction"),
							directionWord(port.direction),
							file + ": direction of " + port.name);
					List<?> bits = (List<?>) truth.get("bits");
					assertEquals(bits.size(), port.bits,
							file + ": width of " + port.name);
				}
			}
		}
	} // end of scannedPortsMatchYosysWriteJson method

	/**
	 * Runs yosys on one corpus file and parses its JSON netlist.
	 * @param yosys the yosys executable path
	 * @param file the Verilog file to read
	 * @return the parsed top-level JSON object
	 * @throws Exception if yosys fails or times out
	 */
	private Map<String, Object> runYosys(String yosys, Path file)
			throws Exception {
		Path out = tmp.resolve(file.getFileName() + ".json");
		// proc lowers always-blocks; the JSON backend refuses raw
		// processes. It does not change the port list.
		ProcessBuilder pb = new ProcessBuilder(yosys, "-q",
				"-p", "read_verilog "
						+ file.toAbsolutePath(),
				"-p", "proc",
				"-p", "write_json " + out);
		pb.redirectErrorStream(true);
		Process p = pb.start();
		p.getOutputStream().close();
		String output = new String(p.getInputStream().readAllBytes(),
				StandardCharsets.UTF_8);
		if (!p.waitFor(60, TimeUnit.SECONDS)) {
			p.destroyForcibly();
			throw new AssertionError("yosys timed out on " + file);
		}
		assertEquals(0, p.exitValue(),
				file + " does not elaborate under yosys:\n" + output);
		Object parsed = new Json(Files.readString(out)).parse();
		@SuppressWarnings("unchecked")
		Map<String, Object> top = (Map<String, Object>) parsed;
		return top;
	} // end of runYosys method

	/**
	 * @param direction a scanned direction
	 * @return the word yosys uses for it in write_json output
	 */
	private static String directionWord(
			ScannedPort.Direction direction) {
		switch (direction) {
		case IN:
			return "input";
		case OUT:
			return "output";
		default:
			return "inout";
		}
	} // end of directionWord method

	/** Locate a tool on PATH, or null (never installs anything). */
	private static String findOnPath(String tool) {
		String path = System.getenv("PATH");
		if (path == null) {
			return null;
		}
		for (String dir : path.split(File.pathSeparator)) {
			if (dir.isEmpty()) {
				continue;
			}
			Path candidate = Path.of(dir, tool);
			try {
				if (Files.isExecutable(candidate)
						&& !Files.isDirectory(candidate)) {
					return candidate.toString();
				}
			} catch (SecurityException ignored) {
				// unreadable PATH entry: keep looking
			}
		}
		return null;
	} // end of findOnPath method

	/**
	 * A minimal recursive-descent JSON reader, just enough for yosys
	 * {@code write_json} output (the repo deliberately has no JSON
	 * dependency). Objects become insertion-ordered maps, arrays
	 * lists, numbers longs or doubles.
	 */
	private static final class Json {

		/** The JSON text. */
		private final String text;
		/** Cursor into {@link #text}. */
		private int at;

		/**
		 * Builds a reader over one JSON document.
		 * @param text the JSON text
		 */
		Json(String text) {
			this.text = text;
			this.at = 0;
		} // end of Json constructor

		/**
		 * Parses the single top-level value.
		 * @return the value (Map, List, String, Long, Double,
		 *         Boolean, or null)
		 */
		Object parse() {
			Object v = value();
			skipWhitespace();
			if (at < text.length()) {
				throw new IllegalArgumentException(
						"trailing JSON at offset " + at);
			}
			return v;
		} // end of parse method

		/**
		 * Parses one value at the cursor.
		 * @return the parsed value
		 */
		private Object value() {
			skipWhitespace();
			char c = text.charAt(at);
			if (c == '{') {
				return object();
			}
			if (c == '[') {
				return array();
			}
			if (c == '"') {
				return string();
			}
			if (text.startsWith("true", at)) {
				at += 4;
				return Boolean.TRUE;
			}
			if (text.startsWith("false", at)) {
				at += 5;
				return Boolean.FALSE;
			}
			if (text.startsWith("null", at)) {
				at += 4;
				return null;
			}
			return number();
		} // end of value method

		/**
		 * Parses an object at the cursor.
		 * @return an insertion-ordered map of members
		 */
		private Map<String, Object> object() {
			Map<String, Object> map =
					new LinkedHashMap<String, Object>();
			at += 1;	// {
			skipWhitespace();
			if (text.charAt(at) == '}') {
				at += 1;
				return map;
			}
			while (true) {
				skipWhitespace();
				String key = string();
				skipWhitespace();
				expect(':');
				map.put(key, value());
				skipWhitespace();
				if (text.charAt(at) == ',') {
					at += 1;
				} else {
					expect('}');
					return map;
				}
			}
		} // end of object method

		/**
		 * Parses an array at the cursor.
		 * @return the element list
		 */
		private List<Object> array() {
			List<Object> list = new ArrayList<Object>();
			at += 1;	// [
			skipWhitespace();
			if (text.charAt(at) == ']') {
				at += 1;
				return list;
			}
			while (true) {
				list.add(value());
				skipWhitespace();
				if (text.charAt(at) == ',') {
					at += 1;
				} else {
					expect(']');
					return list;
				}
			}
		} // end of array method

		/**
		 * Parses a string literal at the cursor.
		 * @return the unescaped string
		 */
		private String string() {
			expect('"');
			StringBuilder sb = new StringBuilder();
			while (true) {
				char c = text.charAt(at);
				at += 1;
				if (c == '"') {
					return sb.toString();
				}
				if (c == '\\') {
					char e = text.charAt(at);
					at += 1;
					switch (e) {
					case 'b':
						sb.append('\b');
						break;
					case 'f':
						sb.append('\f');
						break;
					case 'n':
						sb.append('\n');
						break;
					case 'r':
						sb.append('\r');
						break;
					case 't':
						sb.append('\t');
						break;
					case 'u':
						sb.append((char) Integer.parseInt(
								text.substring(at, at + 4), 16));
						at += 4;
						break;
					default:
						sb.append(e);
						break;
					}
				} else {
					sb.append(c);
				}
			}
		} // end of string method

		/**
		 * Parses a number at the cursor.
		 * @return a Long for integral values, else a Double
		 */
		private Object number() {
			int start = at;
			while (at < text.length()
					&& "+-0123456789.eE".indexOf(text.charAt(at)) >= 0) {
				at += 1;
			}
			String num = text.substring(start, at);
			if (num.indexOf('.') < 0 && num.indexOf('e') < 0
					&& num.indexOf('E') < 0) {
				return Long.valueOf(num);
			}
			return Double.valueOf(num);
		} // end of number method

		/**
		 * Consumes one expected character.
		 * @param c the required character
		 */
		private void expect(char c) {
			if (text.charAt(at) != c) {
				throw new IllegalArgumentException("expected '" + c
						+ "' at offset " + at + " but found '"
						+ text.charAt(at) + "'");
			}
			at += 1;
		} // end of expect method

		/** Advances past whitespace. */
		private void skipWhitespace() {
			while (at < text.length()
					&& Character.isWhitespace(text.charAt(at))) {
				at += 1;
			}
		} // end of skipWhitespace method

	} // end of Json class

} // end of YosysGroundTruthTest class
