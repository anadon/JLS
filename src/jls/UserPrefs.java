package jls;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.jspecify.annotations.Nullable;

/**
 * Persistent user preferences (issue #76).
 *
 * Before this class existed nothing the user adjusted survived a
 * restart.  This is a thin wrapper over {@link java.util.prefs.Preferences}
 * (whose OS-native backend needs no pathing code of our own) that
 * degrades to an in-memory map when the backing store is unavailable -
 * for example in a sandboxed environment - so callers never see an
 * exception, they just lose persistence.
 *
 * Stored today: the color theme name, the user's grid and
 * background color overrides, and the user's undo depth.  The two
 * color overrides are cleared whenever a theme is chosen, so picking a
 * theme takes full effect.
 *
 * @author David A. Poplawski
 */
public final class UserPrefs {

	// preference keys
	/** The key the color theme name is stored under. */
	private static final String THEME_KEY = "theme";
	/** The key the grid color override is stored under. */
	private static final String GRID_KEY = "gridColor";
	/** The key the background color override is stored under. */
	private static final String BACKGROUND_KEY = "backgroundColor";
	/** The key the undo depth override is stored under. */
	private static final String UNDO_DEPTH_KEY = "undoDepth";

	// the backing node, or null when only the in-memory map is used
	/** The backing preferences node, or null when only the in-memory map is used. */
	private final @Nullable Preferences node;

	// in-memory fallback (and write-through cache) for sandboxed runs
	/** In-memory fallback (and write-through cache) for sandboxed runs. */
	private final Map<String, String> memory = new HashMap<String, String>();

	/**
	 * Create a store over the given preferences node.  Package-visible
	 * so tests can inject an in-memory node.
	 *
	 * @param node The backing node, or null for in-memory-only.
	 */
	UserPrefs(@Nullable Preferences node) {

		this.node = node;
	} // end of constructor

	/**
	 * Open the user's JLS preference store, degrading to an in-memory
	 * store when the platform backing store cannot be reached.
	 *
	 * @return the store, never null.
	 */
	public static UserPrefs open() {

		try {
			return new UserPrefs(Preferences.userNodeForPackage(JLSInfo.class));
		}
		catch (SecurityException | IllegalStateException ex) {
			return new UserPrefs(null);
		}
	} // end of open method

	/**
	 * Apply every stored preference to the running application: the
	 * saved theme (or the default theme if none is stored) and then
	 * any grid/background color overrides on top of it.  Call once at
	 * GUI startup, before any editor window exists.
	 */
	public void applyStartup() {

		Theme.byName(get(THEME_KEY)).apply();
		overrideColors(parseColor(get(GRID_KEY)),
				parseColor(get(BACKGROUND_KEY)));
		applyUndoDepth(get(UNDO_DEPTH_KEY));
	} // end of applyStartup method

	/**
	 * Lay the user's stored undo depth (if any) over the current
	 * default.
	 *
	 * @param stored The stored undo-depth string, or null for none.
	 */
	private static void applyUndoDepth(@Nullable String stored) {

		JLSInfo.undoStackDepth = parseUndoDepth(stored);
	} // end of applyUndoDepth method

	/**
	 * Lay the user's stored color overrides (if any) over the colors
	 * the theme just installed.
	 *
	 * @param grid The stored grid color, or null for none.
	 * @param background The stored background color, or null for none.
	 */
	private static void overrideColors(@Nullable Color grid, @Nullable Color background) {

		if (grid != null) {
			JLSInfo.gridColor = grid;
		}
		if (background != null) {
			JLSInfo.backgroundColor = background;
		}
	} // end of overrideColors method

	/**
	 * Persist the chosen theme and drop any grid/background color
	 * overrides so the theme's own colors take effect everywhere.
	 *
	 * @param name The theme name (see {@link Theme#name()}).
	 */
	public void rememberTheme(String name) {

		put(THEME_KEY, name);
		remove(GRID_KEY);
		remove(BACKGROUND_KEY);
	} // end of rememberTheme method

	/**
	 * Persist the user's grid color choice.
	 *
	 * @param color The new grid color.
	 */
	public void rememberGridColor(Color color) {

		put(GRID_KEY, Integer.toString(color.getRGB()));
	} // end of rememberGridColor method

	/**
	 * Persist the user's editor background color choice.
	 *
	 * @param color The new background color.
	 */
	public void rememberBackgroundColor(Color color) {

		put(BACKGROUND_KEY, Integer.toString(color.getRGB()));
	} // end of rememberBackgroundColor method

	/**
	 * Persist the user's undo depth choice.
	 *
	 * @param depth The new undo depth.
	 */
	public void rememberUndoDepth(int depth) {

		put(UNDO_DEPTH_KEY, Integer.toString(depth));
	} // end of rememberUndoDepth method

	/**
	 * The stored value for a key, from the backing node when present,
	 * otherwise from the in-memory map.
	 *
	 * @param key The preference key.
	 *
	 * @return the stored value, or null when none is stored.
	 */
	private @Nullable String get(String key) {

		if (node != null) {
			try {
				return node.get(key, memory.get(key));
			}
			catch (IllegalStateException ex) {
				// node removed underneath us; fall through to memory
			}
		}
		return memory.get(key);
	} // end of get method

	/**
	 * Store a value under a key, in memory always and in the backing
	 * node when one is available.
	 *
	 * @param key The preference key.
	 * @param value The value to store.
	 */
	private void put(String key, String value) {

		memory.put(key, value);
		if (node != null) {
			try {
				node.put(key, value);
				node.flush();
			}
			catch (IllegalStateException | BackingStoreException ex) {
				// persistence unavailable; the in-memory copy stands
			}
		}
	} // end of put method

	/**
	 * Remove a stored value, from memory and from the backing node
	 * when one is available.
	 *
	 * @param key The preference key.
	 */
	private void remove(String key) {

		memory.remove(key);
		if (node != null) {
			try {
				node.remove(key);
				node.flush();
			}
			catch (IllegalStateException | BackingStoreException ex) {
				// persistence unavailable; the in-memory copy stands
			}
		}
	} // end of remove method

	/**
	 * Decode a color stored by {@code rememberGridColor} or
	 * {@code rememberBackgroundColor}.
	 *
	 * @param value The stored string, or null.
	 *
	 * @return the color, or null when the value is missing or corrupt.
	 */
	private static @Nullable Color parseColor(@Nullable String value) {

		if (value == null) {
			return null;
		}
		try {
			return new Color(Integer.parseInt(value), true);
		}
		catch (NumberFormatException ex) {
			return null;
		}
	} // end of parseColor method

	/**
	 * Decode a value stored by {@code rememberUndoDepth}.
	 *
	 * @param value The stored string, or null.
	 *
	 * @return the undo depth, or the current
	 *         {@link JLSInfo#undoStackDepth} when the value is missing or
	 *         corrupt.
	 */
	private static int parseUndoDepth(@Nullable String value) {

		if (value == null) {
			return JLSInfo.undoStackDepth;
		}
		try {
			return Integer.parseInt(value);
		}
		catch (NumberFormatException ex) {
			return JLSInfo.undoStackDepth;
		}
	} // end of parseUndoDepth method

} // end of UserPrefs class
