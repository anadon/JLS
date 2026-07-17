package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * The seed-directory decision for file choosers and new-circuit
 * directories (issue #130): desktop-launched sessions (deb/rpm/msi/dmg/
 * AppImage, issue #82) inherit a meaningless working directory from the
 * launcher, so nothing GUI-facing may seed from {@code user.dir} - the
 * baseline is the user's home directory, and a remembered directory
 * wins over it where one exists ({@code prevOpenDir}).
 *
 * <p>These tests pin the extracted helper's contract; the companion
 * {@link UserDirSeedTest} pins that the GUI call sites actually route
 * through it rather than reading {@code user.dir} directly.</p>
 */
class SeedDirectoryTest {

	/** The baseline seed is the user's home directory, never the
	 *  process working directory. */
	@Test
	void defaultDirectoryIsUserHome() {
		assertEquals(System.getProperty("user.home"),
				Util.defaultDirectory(),
				"chooser seeds must fall back to user.home (issue #130)");
	}

	/** No remembered directory yet (the empty-string sentinel that
	 *  prevOpenDir starts with) falls back to the default. */
	@Test
	void emptyRememberedFallsBackToDefault() {
		assertEquals(Util.defaultDirectory(), Util.seedDirectory(""));
	}

	/** Null is treated the same as "nothing remembered". */
	@Test
	void nullRememberedFallsBackToDefault() {
		assertEquals(Util.defaultDirectory(), Util.seedDirectory(null));
	}

	/** A remembered directory always wins over the default (the
	 *  prevOpenDir behavior from issue #34 is preserved - P3 of
	 *  issue #130). */
	@Test
	void rememberedDirectoryWins() {
		assertEquals("/tmp/circuits", Util.seedDirectory("/tmp/circuits"));
	}
} // end of SeedDirectoryTest class
