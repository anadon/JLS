package jls;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jls.elem.Element;

/**
 * Headless draw-path smoke coverage (issues #91 layer 1 / #162): every
 * palette element's draw() runs against both export canvases - the
 * raster BufferedImage path and the JFreeSVG path (#154) - via
 * Circuit.exportImage on a fixture containing one instance of every
 * drawable element type. None of this needs a display: paint code is
 * plain Graphics2D.
 *
 * The assertions are deliberately smoke-grade (draws without throwing,
 * produces a decodable image / well-formed SVG with the elements
 * plausibly present); pixel-exact rendering is not pinned, for the same
 * font-metrics reason as CliImageExportTest. The completeness sweep at
 * the bottom keeps the fixture honest as the palette grows.
 */
class ElementDrawSmokeTest {

	@TempDir
	Path tmp;

	/** Element classes deliberately absent from the draw fixture. */
	private static final Set<String> EXEMPT = Set.of(
			// created by the simulator for -t runs, never placed/drawn
			"TestGen");

	/** One instance of every drawable element type, wired plausibly. */
	private static String fixture() {
		CircuitTextBuilder cb = new CircuitTextBuilder();
		for (String g : new String[] {"AndGate", "OrGate", "NandGate",
				"NorGate", "XorGate"}) {
			cb.gate(g, 2, 2);
		}
		cb.gate("NotGate", 1, 1);
		cb.gate("DelayGate", 1, 1);
		cb.constant(0xAB);
		cb.extend(4);
		cb.adder(4);
		cb.clock(20, 10);
		cb.decoder(2);
		cb.display(4, 16);
		cb.memory("ROM", 8, 4, "0 5\\n1 9");
		cb.mux(4, 2);
		cb.pause();
		cb.register(4, 3, "nff");
		cb.shifter("ArithmeticRight", 8);
		cb.stateMachine(1, 1);
		cb.stop();
		cb.text("hello \\\"draw\\\"");
		cb.triState(1);
		cb.truthTable("tt", new String[] {"a"}, new String[] {"z"},
				new int[][] {{0, 1}, {1, 0}});
		cb.binder(4, new int[][] {{3, 2}, {1, 0}});
		cb.splitter(4, new int[][] {{3, 2}, {1, 0}});
		cb.jumpStart("js", 4);
		cb.jumpEnd("js", 4);
		cb.sigGen("in1 0 for 50 1 end");
		cb.subCircuit("sub");
		int in = cb.inputPin("in1", 1);
		int out = cb.outputPin("watched", 1);
		// one real wire so Wire and WireEnd draw too
		cb.wire(in, "output", out, "input");
		return cb.build();
	}

	private Circuit load() throws Exception {
		Circuit circuit = new Circuit("golden");
		assertTrue(circuit.load(new Scanner(fixture())),
				() -> "load failed: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
				() -> "finishLoad failed: " + JLSInfo.loadError);
		return circuit;
	}

	@Test
	void everyElementDrawsOnTheRasterExportPath() throws Exception {
		Circuit circuit = load();
		File png = tmp.resolve("draw.png").toFile();
		circuit.exportImage(png.getAbsolutePath());
		BufferedImage image = ImageIO.read(png);
		assertNotNull(image, "export did not produce a decodable PNG");
		assertTrue(image.getWidth() > 100 && image.getHeight() > 50,
				"implausibly small render: " + image.getWidth() + "x"
						+ image.getHeight());
	}

	@Test
	void everyElementDrawsOnTheSvgExportPath() throws Exception {
		Circuit circuit = load();
		File svg = tmp.resolve("draw.svg").toFile();
		circuit.exportImage(svg.getAbsolutePath());
		String text = Files.readString(svg.toPath(), StandardCharsets.UTF_8);
		assertTrue(text.contains("<svg") && text.contains("</svg>"),
				"export did not produce a well-formed SVG document");
		// the watched pin's label proves text drawing ran
		assertTrue(text.contains("watched"),
				"pin labels should appear in the SVG");
	}

	@Test
	void theFixtureCoversEveryDrawableElementType() throws Exception {
		// the smoke above is only as good as the fixture is complete:
		// sweep the palette reflectively (ElementConstructorContractTest
		// pattern) and fail when a type is neither drawn nor exempt
		Set<String> present = new TreeSet<>();
		for (Element el : load().getElements()) {
			present.add(el.getClass().getSimpleName());
		}

		File dir = new File(Element.class.getProtectionDomain()
				.getCodeSource().getLocation().toURI());
		File pkg = new File(dir, "jls/elem");
		assertTrue(pkg.isDirectory(), "compiled classes not found at " + pkg);
		Set<String> missing = new TreeSet<>();
		int checked = 0;
		for (File f : pkg.listFiles()) {
			String name = f.getName();
			if (!name.endsWith(".class") || name.contains("$")) {
				continue;
			}
			String simple = name.replace(".class", "");
			Class<?> c = Class.forName("jls.elem." + simple);
			if (!Element.class.isAssignableFrom(c)
					|| Modifier.isAbstract(c.getModifiers())
					|| c == Element.class) {
				continue;
			}
			checked++;
			if (!present.contains(simple) && !EXEMPT.contains(simple)) {
				missing.add(simple);
			}
		}
		assertTrue(checked > 25, "sweep found too few element classes ("
				+ checked + ") - wrong directory?");
		assertTrue(missing.isEmpty(),
				"drawable element types missing from the draw fixture"
						+ " (add them to fixture(), or an exemption with"
						+ " a reason): " + missing);
	}
}
