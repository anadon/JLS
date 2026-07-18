package jls.hdl.yosys;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Finds the user-installed Yosys binary (issue #61). Yosys is an
 * external dependency JLS never bundles; the importer looks for it on
 * the {@code PATH} exactly the way a shell would, and the caller
 * turns absence into the explanatory dependency dialog (addendum:
 * the menu item stays enabled and explains, never disables silently
 * and never throws). Entries that are not valid paths on this
 * platform are skipped, matching shell behavior for junk in
 * {@code PATH}.
 */
public final class YosysLocator {

	/** The binary names looked for, in order, in each directory. */
	private static final String[] NAMES = { "yosys", "yosys.exe" };

	/** No instances; the class is a lookup function. */
	private YosysLocator() {

	} // end of constructor

	/**
	 * Searches the process's {@code PATH} for a Yosys binary.
	 *
	 * @return the first executable found, or empty when there is
	 * none.
	 */
	public static Optional<Path> find() {

		return find(System.getenv("PATH"));
	} // end of find method

	/**
	 * Searches an explicit search path for a Yosys binary. Split out
	 * from {@link #find()} so tests can exercise the search without
	 * touching the real environment.
	 *
	 * @param searchPath A {@code PATH}-style list of directories
	 * separated by {@link File#pathSeparator}; null or empty finds
	 * nothing.
	 *
	 * @return the first executable regular file named {@code yosys}
	 * (or {@code yosys.exe}) in the listed directories, or empty.
	 */
	public static Optional<Path> find(String searchPath) {

		if (searchPath == null || searchPath.isEmpty()) {
			return Optional.empty();
		}
		for (String entry : searchPath.split(File.pathSeparator)) {
			if (entry.isEmpty()) {
				continue;
			}
			for (String name : NAMES) {
				Path candidate;
				try {
					candidate = Path.of(entry, name);
				}
				catch (InvalidPathException e) {
					// junk PATH entry; a shell would skip it too
					break;
				}
				if (Files.isRegularFile(candidate)
						&& Files.isExecutable(candidate)) {
					return Optional.of(candidate);
				}
			}
		}
		return Optional.empty();
	} // end of find method

} // end of YosysLocator class
