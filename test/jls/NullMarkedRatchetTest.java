package jls;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Nullness-enforcement ratchet (issue #93): once a package declares
 * {@code @NullMarked} on its {@code package-info.java}, NullAway
 * enforces its nullness contracts on the default {@code -Werror}
 * build, and the package never becomes unmarked again. This test makes
 * the convention executable: every package on the marked list below
 * must keep its {@code @NullMarked} declaration. The list only grows -
 * add a package here when its rollout PR lands, never remove one.
 */
class NullMarkedRatchetTest {

	/**
	 * Source packages (as directories) whose nullness is
	 * compiler-enforced. Add, never remove.
	 */
	private static final List<String> MARKED = List.of(
			"src/jls/sim",
			"src/jls/util");

	/**
	 * Every package on the marked list still declares
	 * {@code @NullMarked} on its package-info.
	 *
	 * @throws IOException if a package-info cannot be read.
	 */
	@Test
	void markedPackagesStayMarked() throws IOException {
		List<String> unmarked = new ArrayList<String>();
		for (String pkg : MARKED) {
			Path info = Path.of(pkg, "package-info.java");
			String text = Files.exists(info)
					? Files.readString(info) : "";
			if (!text.contains("@NullMarked")) {
				unmarked.add(pkg);
			}
		}
		assertTrue(unmarked.isEmpty(),
				"@NullMarked packages never become unmarked (issue #93);"
						+ " restore the annotation in: " + unmarked);
	}

} // end of NullMarkedRatchetTest class
