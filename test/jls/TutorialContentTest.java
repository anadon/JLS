package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * Guards the tutorial refresh from issue #73: the four tutorial pages
 * must be bundled on the classpath in menu order, every image they
 * reference must resolve exactly (the way a jar resolves, case
 * sensitively), the applet-era copy ("takes a while over the network")
 * must stay gone, and the orphaned 21&nbsp;KB {@code tutorial.html}
 * master file must not creep back into the jar. All checks run
 * headless against classpath resources only, so they hold for the
 * packed jar as well as an IDE run.
 */
class TutorialContentTest {

	/** Matches img src attributes, quoted or not. */
	private static final Pattern IMG = Pattern.compile(
			"(?i)\\bsrc\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>\"'][^\\s>]*))");

	/**
	 * Read a bundled tutorial resource as text.
	 *
	 * @param name File name within the jls/tutorial classpath directory.
	 * @return the resource content, decoded as UTF-8.
	 * @throws Exception if the resource is missing or unreadable.
	 */
	private static String resource(String name) throws Exception {
		try (InputStream in = Tutorial.class
				.getResourceAsStream("tutorial/" + name)) {
			assertNotNull(in, "jls/tutorial/" + name
					+ " missing from classpath");
			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	@Test
	void pageMetadataIsConsistent() {
		assertEquals(Tutorial.PAGES.length, Tutorial.TITLES.length,
				"every page needs a title");
		assertEquals(4, Tutorial.pageCount(),
				"the tutorial is a four-part sequence");
		for (int i = 0; i < Tutorial.pageCount(); i++) {
			// menu order == reading order: page i is tutorial(i+1).html
			assertEquals("tutorial" + (i + 1) + ".html",
					Tutorial.pageFile(i));
			assertFalse(Tutorial.pageTitle(i).isBlank(),
					"page " + i + " needs a title");
		}
	}

	@Test
	void allPagesAreBundled() throws Exception {
		for (int i = 0; i < Tutorial.pageCount(); i++) {
			String text = resource(Tutorial.pageFile(i));
			assertTrue(text.length() > 500,
					Tutorial.pageFile(i) + " is suspiciously short");
		}
	}

	@Test
	void appletEraCopyIsGone() throws Exception {
		for (int i = 0; i < Tutorial.pageCount(); i++) {
			String lower = resource(Tutorial.pageFile(i))
					.toLowerCase(Locale.ROOT);
			assertFalse(lower.contains("applet"),
					Tutorial.pageFile(i) + " still mentions applets");
			assertFalse(lower.contains("network"),
					Tutorial.pageFile(i)
							+ " still carries applet-era network copy");
		}
	}

	@Test
	void orphanedMasterFileStaysDeleted() {
		assertNull(Tutorial.class.getResource("tutorial/tutorial.html"),
				"tutorial.html was an unreachable applet-era master"
						+ " copy; ship only the four navigable pages");
	}

	@Test
	void everyReferencedImageResolves() throws Exception {
		for (int i = 0; i < Tutorial.pageCount(); i++) {
			String text = resource(Tutorial.pageFile(i));
			Matcher m = IMG.matcher(text);
			int images = 0;
			while (m.find()) {
				String ref = m.group(1) != null ? m.group(1)
						: m.group(2) != null ? m.group(2) : m.group(3);
				assertNotNull(
						Tutorial.class.getResource("tutorial/" + ref),
						Tutorial.pageFile(i) + " references missing"
								+ " image " + ref);
				images++;
			}
			assertTrue(images > 0, Tutorial.pageFile(i)
					+ " should illustrate its circuit with at least"
					+ " one image");
		}
	}
}
