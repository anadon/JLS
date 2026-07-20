package jls;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * The architecture ratchet for issue #77: JLS is to grow an enforced
 * headless core - circuit model, simulation, and persistence, with the
 * GUI as a consumer - eventually extracted as {@code jls.core}. A core
 * class may not import AWT ({@code java.awt.*}), Swing
 * ({@code javax.swing.*}), or the editor ({@code jls.edit.*}).
 *
 * The intended core, as of this wave, is:
 * <ul>
 * <li>{@code jls.sim.*} - the simulation engine (InteractiveSimulator
 * is expected to leave this package for the GUI side rather than shed
 * its imports in place; either way the ratchet shrinks),</li>
 * <li>{@code jls.Circuit} and its load/save collaborators
 * ({@code FileAbstractor}, {@code LoadError}, {@code BitSetUtils}),</li>
 * <li>{@code jls.elem.*} - the model halves of the elements (today
 * each element file mixes model and rendering; the split is later
 * waves' work).</li>
 * </ul>
 *
 * BASELINE below is the exact set of core-candidate files that
 * violated the rule when the ratchet landed. The rules for maintaining
 * it are the standard shrinking-baseline pattern:
 * <ul>
 * <li>No file outside the baseline may pick up a forbidden import -
 * new violations fail immediately.</li>
 * <li>When you clean a file (or move it out of the core packages),
 * this test fails until you delete its line from BASELINE. Never add
 * lines. The baseline only shrinks; when it reaches empty, replace it
 * with a hard zero-tolerance check (or an ArchUnit rule on the real
 * {@code jls.core} package).</li>
 * </ul>
 *
 * Like NotificationRatchetTest, sources are read straight from the
 * repo tree relative to user.dir, which maven sets to the module base
 * directory - the repository root. Only import statements are matched;
 * fully-qualified in-body references are out of scope for this wave.
 */
class HeadlessCoreRatchetTest {

	/** Import prefixes forbidden in headless-core code. */
	private static final Pattern FORBIDDEN_IMPORT = Pattern.compile(
			"^\\s*import\\s+(?:static\\s+)?"
					+ "(?:java\\.awt\\.|javax\\.swing\\.|jls\\.edit\\.)");

	/** Circuit's load/save collaborators, plus Circuit itself. */
	private static final Set<String> CORE_FILES = Set.of(
			"src/jls/Circuit.java",
			"src/jls/FileAbstractor.java",
			"src/jls/LoadError.java",
			"src/jls/BitSetUtils.java");

	/** Whole packages that belong to the intended core. jls.hdl (the
	 *  HDL exporter, issue #60) was born clean and must stay clean -
	 *  it has no baseline entries and never gets one. */
	private static final Set<String> CORE_PACKAGE_PREFIXES = Set.of(
			"src/jls/sim/",
			"src/jls/elem/",
			"src/jls/hdl/");

	/**
	 * Core-candidate files that imported a forbidden package when this
	 * ratchet landed (60 files, 2026-07). DELETE lines as files are
	 * cleaned; NEVER add one. The six pure gate leaves (AndGate, NandGate,
	 * NorGate, NotGate, OrGate, XorGate) left this list once their symbol
	 * geometry moved to the headless {@link jls.elem.GateOutline} model and
	 * the {@code GeneralPath} translation stayed on the Gate/Swing side
	 * (issue #77 renderer/model split).
	 */
	private static final Set<String> BASELINE = Set.of(
			"src/jls/Circuit.java",
			"src/jls/elem/Adder.java",
			"src/jls/elem/Binder.java",
			"src/jls/elem/Clock.java",
			"src/jls/elem/Constant.java",
			"src/jls/elem/Cross.java",
			"src/jls/elem/Decoder.java",
			"src/jls/elem/DelayGate.java",
			"src/jls/elem/Display.java",
			"src/jls/elem/DisplayBool.java",
			"src/jls/elem/Element.java",
			"src/jls/elem/ElementDialog.java",
			"src/jls/elem/Entry.java",
			"src/jls/elem/Extend.java",
			"src/jls/elem/Gate.java",
			"src/jls/elem/GridTransform.java",
			"src/jls/elem/Group.java",
			"src/jls/elem/HLine.java",
			"src/jls/elem/InputPin.java",
			"src/jls/elem/InputSig.java",
			"src/jls/elem/InputVal.java",
			"src/jls/elem/JumpEnd.java",
			"src/jls/elem/JumpStart.java",
			"src/jls/elem/Memory.java",
			"src/jls/elem/Mux.java",
			"src/jls/elem/OutputPin.java",
			"src/jls/elem/OutputSig.java",
			"src/jls/elem/OutputVal.java",
			"src/jls/elem/Pause.java",
			"src/jls/elem/Pin.java",
			"src/jls/elem/Put.java",
			"src/jls/elem/Register.java",
			"src/jls/elem/SMUtil.java",
			"src/jls/elem/SigEntry.java",
			"src/jls/elem/SigGen.java",
			"src/jls/elem/Splitter.java",
			"src/jls/elem/State.java",
			"src/jls/elem/StateMachine.java",
			"src/jls/elem/Stop.java",
			"src/jls/elem/SubCircuit.java",
			"src/jls/elem/TestGen.java",
			"src/jls/elem/Text.java",
			"src/jls/elem/TriState.java",
			"src/jls/elem/TruthTable.java",
			"src/jls/elem/VLine.java",
			"src/jls/elem/ValEntry.java",
			"src/jls/elem/Wire.java",
			"src/jls/elem/WireEnd.java",
			"src/jls/sim/InteractiveSimulator.java",
			"src/jls/sim/Trace.java");

	@Test
	void coreCandidatesGainNoForbiddenImports() throws IOException {

		Path src = Path.of(System.getProperty("user.dir"), "src");
		assertTrue(Files.isDirectory(src),
				"source tree not found at " + src
						+ " (run tests from the repository root)");

		Set<String> violators = new TreeSet<>();
		try (Stream<Path> files = Files.walk(src)) {
			files.map(HeadlessCoreRatchetTest::relativize)
					.filter(HeadlessCoreRatchetTest::isCoreCandidate)
					.filter(HeadlessCoreRatchetTest::hasForbiddenImport)
					.forEach(violators::add);
		}

		// (a) the ratchet: nothing outside the baseline may violate.
		Set<String> fresh = new TreeSet<>(violators);
		fresh.removeAll(BASELINE);
		assertTrue(fresh.isEmpty(),
				"new GUI dependencies in intended-core files (issue #77): "
						+ fresh + "\ncore code must not import java.awt, "
						+ "javax.swing, or jls.edit; move the GUI concern "
						+ "out instead of adding to the baseline "
						+ "(currently " + BASELINE.size() + " grandfathered "
						+ "violators, " + violators.size() + " found now)");

		// (b) the ratchet only shrinks: every baseline entry must still
		// exist and still violate; a cleaned or moved file must be
		// struck from the list so it can never regress silently.
		Set<String> cleaned = new TreeSet<>(BASELINE);
		cleaned.removeAll(violators);
		assertTrue(cleaned.isEmpty(),
				"progress on issue #77! these baseline files no longer "
						+ "violate (cleaned, moved, or deleted): " + cleaned
						+ "\nremove them from BASELINE in "
						+ "HeadlessCoreRatchetTest so they stay clean "
						+ "(" + violators.size() + " of " + BASELINE.size()
						+ " grandfathered violators remain)");
	}

	/** Is this repo-relative path part of the intended jls.core? */
	private static boolean isCoreCandidate(String path) {

		if (!path.endsWith(".java")) {
			return false;
		}
		if (CORE_FILES.contains(path)) {
			return true;
		}
		return CORE_PACKAGE_PREFIXES.stream().anyMatch(path::startsWith);
	}

	/** Does the file import java.awt, javax.swing, or jls.edit? */
	private static boolean hasForbiddenImport(String path) {

		Path file = Path.of(System.getProperty("user.dir"), path);
		try {
			return Files.readAllLines(file, StandardCharsets.UTF_8)
					.stream()
					.anyMatch(line -> FORBIDDEN_IMPORT.matcher(line).find());
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

	/** A path relative to the repo root, with forward slashes. */
	private static String relativize(Path p) {

		Path root = Path.of(System.getProperty("user.dir"));
		return root.relativize(p.toAbsolutePath().normalize())
				.toString().replace('\\', '/');
	}
}
