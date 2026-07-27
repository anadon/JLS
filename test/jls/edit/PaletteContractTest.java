package jls.edit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import jls.elem.ElementRegistry;
import jls.elem.ElementType;

/**
 * The palette authoring contract (issue #78): the {@link Palette} table
 * must stay a total, well-formed description of the editor toolbar, so
 * that adding an element type is one registry row plus one palette row -
 * and forgetting the palette row (or writing a bad one) is a build
 * failure instead of an element that silently never appears in the GUI.
 *
 * Everything here is checked headlessly on the static table: totality
 * against {@link ElementRegistry#all()} minus the documented non-palette
 * set, icon resources on the classpath, tooltip/fallback-text uniqueness,
 * grid capacity, and help-topic presence (the topics' resolution to
 * bundled pages is {@code jls.HelpTopicsTest}'s job). GUI parity - the
 * generated buttons carrying the {@code palette.<tag>} component names
 * and behaving mid-placement - is pinned by the display-tagged
 * {@code ComponentIdentityTest} and {@code MidPlacementPaletteFeedbackTest}.
 */
class PaletteContractTest {

	/**
	 * Registered element types that deliberately have no palette entry:
	 * SubCircuit is created through the hand-coded Import button (it
	 * imports an open circuit rather than placing a blank element),
	 * WireEnd exists only as the endpoint of drawn wires, and TestGen is
	 * the batch-mode stand-in for a signal generator. Every other
	 * registered type must have exactly one palette row.
	 */
	private static final Set<String> NON_PALETTE_TAGS =
			Set.of("SubCircuit", "WireEnd", "TestGen");

	@Test
	void paletteIsTotalOverTheElementRegistry() {

		Set<String> expected = new TreeSet<String>();
		for (ElementType type : ElementRegistry.all()) {
			if (!NON_PALETTE_TAGS.contains(type.tag())) {
				expected.add(type.tag());
			}
		}
		Set<String> actual = new TreeSet<String>();
		for (PaletteEntry entry : Palette.entries()) {
			assertTrue(actual.add(entry.type().tag()),
					"duplicate palette entry for tag " + entry.type().tag());
		}
		assertEquals(expected, actual,
				"the palette must have exactly one entry per registered "
						+ "element type outside the documented non-palette "
						+ "set " + NON_PALETTE_TAGS + "; add the missing "
						+ "Palette row (or remove the stale one)");
	}

	/**
	 * Palette entries whose icon gif has never shipped: the toolbar has
	 * always shown their fallback text instead (the missing-icon
	 * fallback in {@code SimpleEditor.getImage} exists for exactly this
	 * case - the issue #78 observation). Pinned here so the historical
	 * gap stays visible without blocking the build; if the icon is ever
	 * drawn, this set must shrink or the exactness check below fails.
	 */
	private static final Set<String> KNOWN_MISSING_ICONS =
			Set.of("ShiftRegister");

	@Test
	void everyIconResourceExistsOnTheClasspath() {

		Set<String> missing = new TreeSet<String>();
		for (PaletteEntry entry : Palette.entries()) {
			if (SimpleEditor.class.getResource(
					"images/" + entry.iconName() + ".gif") == null) {
				missing.add(entry.type().tag());
			}
		}
		assertEquals(KNOWN_MISSING_ICONS, missing,
				"every palette entry outside the documented "
						+ "missing-icon set must name a bundled gif in "
						+ "jls/edit/images/ (and a newly drawn icon must "
						+ "leave the documented set)");
	}

	@Test
	void tooltipsAreNonBlankAndUnique() {

		Set<String> seen = new HashSet<String>();
		for (PaletteEntry entry : Palette.entries()) {
			assertFalse(entry.tooltip().isBlank(),
					"blank tooltip for tag " + entry.type().tag());
			assertTrue(seen.add(entry.tooltip()),
					"duplicate tooltip '" + entry.tooltip()
							+ "' (tag " + entry.type().tag() + ")");
		}
	}

	@Test
	void fallbackTextsAreNonBlankAndUnique() {

		Set<String> seen = new HashSet<String>();
		for (PaletteEntry entry : Palette.entries()) {
			assertFalse(entry.fallbackText().isBlank(),
					"blank fallback text for tag " + entry.type().tag());
			assertTrue(seen.add(entry.fallbackText()),
					"duplicate fallback text '" + entry.fallbackText()
							+ "' (tag " + entry.type().tag() + ")");
		}
	}

	@Test
	void everyGroupIsNonEmptyAndFitsItsDeclaredGrid() {

		for (Palette.Group group : Palette.groups()) {
			List<PaletteEntry> entries = Palette.entries(group);
			assertFalse(entries.isEmpty(),
					"palette group " + group + " has no entries");
			int capacity = group.rows() * group.cols();
			assertTrue(entries.size() <= capacity,
					"palette group " + group + " declares a "
							+ group.rows() + "x" + group.cols()
							+ " grid (" + capacity + " slots) but has "
							+ entries.size() + " entries; grow the grid "
							+ "or move an entry to another group");
		}
	}

	@Test
	void entriesComeInToolbarGroupOrder() {

		// makeElements renders one group at a time, so the flat table
		// must list each group's entries consecutively, in group order;
		// interleaving would silently reorder the generated toolbar
		// relative to the declared table.
		List<PaletteEntry> regrouped = new ArrayList<PaletteEntry>();
		for (Palette.Group group : Palette.groups()) {
			regrouped.addAll(Palette.entries(group));
		}
		assertEquals(regrouped, Palette.entries(),
				"Palette.entries() must list each group's entries "
						+ "consecutively, in toolbar group order");
	}

	@Test
	void everyHelpTopicIsNonBlank() {

		// topic ids resolving to bundled pages is HelpTopicsTest's job;
		// this pins that no entry is authored without one
		for (PaletteEntry entry : Palette.entries()) {
			assertFalse(entry.helpTopic().isBlank(),
					"blank help topic for tag " + entry.type().tag());
		}
	}
}
