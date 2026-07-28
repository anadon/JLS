package jls.hdl;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Shared, cross-platform {@code PATH} lookup for the external HDL
 * toolchain the golden tests shell out to (issues #60/#61/#63/#111).
 *
 * <p>Previously five test classes ({@code IverilogCompileTest},
 * {@code GhdlCompileTest}, {@code YosysGroundTruthTest},
 * {@code ImportPipelineTest}, {@code AutogradeBridgeExampleTest})
 * each carried a copy of a {@code findOnPath} helper that built
 * {@code Path.of(dir, tool)} and tested {@link Files#isExecutable}
 * with <em>no</em> {@code .exe} suffix and <em>no</em> {@code PATHEXT}
 * handling. On Windows that never resolves {@code iverilog.exe} /
 * {@code ghdl.exe} / {@code yosys.exe}, so even with the
 * {@code oss-cad-suite} bundle on {@code PATH} every HDL-simulator
 * suite silently {@code assumeTrue}-skips (issue #111 Stage W2
 * blocker). This class is the one shared locator that fixes them all.
 *
 * <p>Windows resolution semantics: the bare name is tried first (which
 * covers unix, and Windows files that already carry an explicit
 * extension), then each extension from {@code PATHEXT} — defaulting to
 * {@code .COM;.EXE;.BAT;.CMD} when unset — is appended, matched
 * <em>case-insensitively</em>. The case-insensitive match is done by
 * scanning the directory rather than by re-probing a constructed path,
 * so a {@code .EXE} entry in {@code PATHEXT} resolves a lowercase
 * {@code iverilog.exe} even on a case-sensitive filesystem — which is
 * what lets the Linux unit tests assert Windows-style resolution
 * headlessly.
 *
 * <p>{@link #find(String, String, String)} takes the {@code PATH} and
 * {@code PATHEXT} strings as parameters (seam injection) so tests drive
 * it against temp directories without touching the real environment;
 * the {@link #findOnPath(String)} convenience passes the real
 * {@code System.getenv} values for the production call sites.
 */
public final class ToolLocator {

	/** Windows' documented default when {@code PATHEXT} is unset. */
	private static final String DEFAULT_PATHEXT = ".COM;.EXE;.BAT;.CMD";

	/** No instances; the class is a lookup function. */
	private ToolLocator() {

	} // end of constructor

	/**
	 * Locates a tool on the process {@code PATH}, honoring
	 * {@code PATHEXT} on Windows, or returns {@code null}. Never
	 * installs anything; the production (non-unit-test) call sites use
	 * this form.
	 *
	 * @param tool The bare tool name, e.g. {@code "iverilog"}.
	 *
	 * @return the absolute path of the first match as a string, or
	 * {@code null} when the tool is not found.
	 */
	public static String findOnPath(String tool) {

		return find(tool, System.getenv("PATH"), System.getenv("PATHEXT"))
				.map(Path::toString)
				.orElse(null);
	} // end of findOnPath method

	/**
	 * Searches an explicit {@code PATH} for a tool, applying Windows
	 * {@code PATHEXT} resolution. Split out from
	 * {@link #findOnPath(String)} so tests exercise the search against
	 * temp directories without touching the real environment.
	 *
	 * @param tool The bare tool name, e.g. {@code "iverilog"}; a name
	 * that already carries an extension is matched by the bare-name
	 * probe.
	 *
	 * @param path A {@code PATH}-style list of directories separated by
	 * {@link File#pathSeparator}; {@code null} finds nothing.
	 *
	 * @param pathext A {@code PATHEXT}-style {@code ;}-separated list of
	 * executable extensions; {@code null} or blank uses the Windows
	 * default {@code .COM;.EXE;.BAT;.CMD}.
	 *
	 * @return the first executable regular file matching the tool name
	 * or one of its {@code PATHEXT} variants, or empty.
	 */
	public static Optional<Path> find(String tool, String path,
			String pathext) {

		if (tool == null || path == null) {
			return Optional.empty();
		}
		List<String> names = candidateNames(tool, pathext);
		for (String dir : path.split(File.pathSeparator)) {
			if (dir.isEmpty()) {
				continue;
			}
			Optional<Path> hit = findInDir(dir, names);
			if (hit.isPresent()) {
				return hit;
			}
		}
		return Optional.empty();
	} // end of find method

	/**
	 * Builds the ordered list of names to accept: the bare name first,
	 * then the bare name with each {@code PATHEXT} extension appended.
	 *
	 * @param tool The bare tool name.
	 *
	 * @param pathext The raw {@code PATHEXT} value, or null/blank.
	 *
	 * @return the priority-ordered acceptable names.
	 */
	private static List<String> candidateNames(String tool, String pathext) {

		List<String> names = new ArrayList<>();
		names.add(tool);
		String raw = (pathext == null || pathext.isBlank())
				? DEFAULT_PATHEXT : pathext;
		for (String ext : raw.split(";")) {
			String trimmed = ext.trim();
			if (!trimmed.isEmpty()) {
				names.add(tool + trimmed);
			}
		}
		return names;
	} // end of candidateNames method

	/**
	 * Scans one {@code PATH} directory for the first acceptable name,
	 * matching case-insensitively so Windows-style {@code .EXE}
	 * resolves a lowercase file on a case-sensitive filesystem too.
	 *
	 * @param dir The directory to scan.
	 *
	 * @param names The priority-ordered acceptable names.
	 *
	 * @return the first executable regular file that matches, or empty.
	 */
	private static Optional<Path> findInDir(String dir, List<String> names) {

		Path dirPath;
		try {
			dirPath = Path.of(dir);
		} catch (InvalidPathException e) {
			// junk PATH entry; a shell would skip it too
			return Optional.empty();
		}
		if (!Files.isDirectory(dirPath)) {
			return Optional.empty();
		}
		List<Path> entries = new ArrayList<>();
		try (DirectoryStream<Path> stream =
				Files.newDirectoryStream(dirPath)) {
			for (Path entry : stream) {
				entries.add(entry);
			}
		} catch (IOException | SecurityException e) {
			// unreadable directory; keep looking on the next PATH entry
			return Optional.empty();
		}
		for (String name : names) {
			for (Path entry : entries) {
				if (!entry.getFileName().toString()
						.equalsIgnoreCase(name)) {
					continue;
				}
				try {
					if (Files.isExecutable(entry)
							&& !Files.isDirectory(entry)) {
						return Optional.of(entry);
					}
				} catch (SecurityException ignored) {
					// unreadable entry; keep looking
				}
			}
		}
		return Optional.empty();
	} // end of findInDir method

} // end of ToolLocator class
