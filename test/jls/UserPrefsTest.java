package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.AbstractPreferences;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * The preference round-trip tests for issue #76 (hypothesis H3): user
 * choices survive "restart", modeled as a fresh {@link UserPrefs} over
 * the same backing node.  An in-memory {@link AbstractPreferences}
 * stands in for the platform store so the tests are hermetic - they
 * never touch the developer's real preferences - and the null-node
 * constructor covers the sandboxed-environment fallback the issue
 * names as H3's only accepted degradation.
 */
class UserPrefsTest {

	@AfterEach
	void restoreDefaultTheme() {
		Theme.DEFAULT.apply();
	}

	@Test
	void themeChoiceSurvivesARestart() {
		MemoryPreferences node = new MemoryPreferences();
		new UserPrefs(node).rememberTheme("classic");

		new UserPrefs(node).applyStartup();		// the "restart"
		assertSame(Theme.CLASSIC, Theme.active());
		assertEquals(Color.green, JLSInfo.touchColor,
				"classic touch color must be live after restart");
	}

	@Test
	void colorOverridesSurviveARestartOnTopOfTheTheme() {
		MemoryPreferences node = new MemoryPreferences();
		UserPrefs store = new UserPrefs(node);
		store.rememberGridColor(new Color(1, 2, 3));
		store.rememberBackgroundColor(new Color(4, 5, 6));

		new UserPrefs(node).applyStartup();
		assertSame(Theme.DEFAULT, Theme.active());
		assertEquals(new Color(1, 2, 3), JLSInfo.gridColor);
		assertEquals(new Color(4, 5, 6), JLSInfo.backgroundColor);
	}

	@Test
	void choosingAThemeDropsStaleColorOverrides() {
		MemoryPreferences node = new MemoryPreferences();
		UserPrefs store = new UserPrefs(node);
		store.rememberGridColor(new Color(1, 2, 3));
		store.rememberTheme("classic");

		new UserPrefs(node).applyStartup();
		assertEquals(Theme.CLASSIC.grid(), JLSInfo.gridColor,
				"a theme choice must reset the grid to the theme's own");
	}

	@Test
	void missingStoreFallsBackToDefaultsWithoutFailing() {
		new UserPrefs(new MemoryPreferences()).applyStartup();
		assertSame(Theme.DEFAULT, Theme.active());
		assertEquals(Theme.DEFAULT.grid(), JLSInfo.gridColor);
	}

	@Test
	void sandboxedInMemoryStoreWorksWithinTheSession() {
		UserPrefs store = new UserPrefs(null);	// no backing store
		store.rememberTheme("classic");
		store.rememberGridColor(new Color(7, 8, 9));
		store.applyStartup();
		assertSame(Theme.CLASSIC, Theme.active());
		assertEquals(new Color(7, 8, 9), JLSInfo.gridColor);
	}

	@Test
	void corruptStoredValuesAreIgnored() {
		MemoryPreferences node = new MemoryPreferences();
		node.put("theme", "no-such-theme");
		node.put("gridColor", "not-a-number");

		new UserPrefs(node).applyStartup();
		assertSame(Theme.DEFAULT, Theme.active());
		assertEquals(Theme.DEFAULT.grid(), JLSInfo.gridColor);
	}

	/** A hermetic, purely in-memory java.util.prefs node. */
	private static final class MemoryPreferences
			extends AbstractPreferences {

		private final Map<String, String> values =
				new HashMap<String, String>();

		MemoryPreferences() {
			super(null, "");
		}

		@Override
		protected void putSpi(String key, String value) {
			values.put(key, value);
		}

		@Override
		protected String getSpi(String key) {
			return values.get(key);
		}

		@Override
		protected void removeSpi(String key) {
			values.remove(key);
		}

		@Override
		protected void removeNodeSpi() {
			values.clear();
		}

		@Override
		protected String[] keysSpi() {
			return values.keySet().toArray(new String[0]);
		}

		@Override
		protected String[] childrenNamesSpi() {
			return new String[0];
		}

		@Override
		protected AbstractPreferences childSpi(String name) {
			throw new UnsupportedOperationException(
					"flat node only in this test");
		}

		@Override
		protected void syncSpi() {
			// nothing to sync
		}

		@Override
		protected void flushSpi() {
			// nothing to flush
		}
	}

} // end of UserPrefsTest class
