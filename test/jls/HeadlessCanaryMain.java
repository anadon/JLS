package jls;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.BitSet;
import java.util.Scanner;

import jls.elem.Element;
import jls.elem.OutputPin;
import jls.sim.BatchSimulator;

/**
 * The subprocess body of {@link HeadlessCoreCanaryTest} (issue #77 P2).
 * It performs the whole headless-core round trip - load a {@code .jls}
 * file, finish the load with no graphics, run a real {@link BatchSimulator}
 * to quiescence, and print the settled output values - using only the
 * headless core ({@code jls.Circuit}, {@code jls.sim}, {@code jls.elem}).
 * The test runs this in a fresh JVM under {@code -Xlog:class+load} and
 * asserts no AWT/Swing/editor class was ever loaded, which proves the core
 * is embeddable in a headless host at runtime, not just by static analysis.
 *
 * <p>It lives in the test tree (not shipped) and is invoked by class name;
 * it must touch nothing GUI, or it would defeat the very property it exists
 * to demonstrate.
 */
public final class HeadlessCanaryMain {

	private HeadlessCanaryMain() {
	} // no instances

	/**
	 * Load, simulate, and print settled output values.
	 *
	 * @param args One element: the path to the {@code .jls} file.
	 * @throws Exception if the file cannot be read or loaded.
	 */
	public static void main(String[] args) throws Exception {

		if (args.length != 1) {
			System.err.println("usage: HeadlessCanaryMain <file.jls>");
			System.exit(2);
		}
		String text = Files.readString(Path.of(args[0]));

		Circuit circuit = new Circuit("canary");
		if (!circuit.load(new Scanner(text))) {
			System.err.println("load failed: " + JLSInfo.loadError);
			System.exit(1);
		}
		// no graphics: the headless embedder has none
		if (!circuit.finishLoad((jls.core.TextMetrics) null)) {
			System.err.println("finishLoad failed: " + JLSInfo.loadError);
			System.exit(1);
		}

		BatchSimulator sim = new BatchSimulator();
		sim.setCircuit(circuit);
		sim.setTimeLimit(1_000_000);
		sim.runSim();

		StringBuilder out = new StringBuilder();
		for (Element el : circuit.getElements()) {
			if (el instanceof OutputPin pin) {
				BitSet value = pin.getCurrentValue();
				out.append(pin.getName()).append('=')
						.append(value == null ? "off"
								: BitSetUtils.ToLong(value))
						.append(' ');
			}
		}
		System.out.println("settled: " + out.toString().trim());
	} // end of main method

} // end of HeadlessCanaryMain class
