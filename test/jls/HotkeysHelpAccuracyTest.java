package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.KeyStroke;

import org.junit.jupiter.api.Test;

import jls.edit.EditOp;

/**
 * The in-app hot-key reference (resources/help/editor/editing/hotkeys.html)
 * must agree with the accelerators the editor actually binds (issue #75
 * verification hardening). The keys and the source of truth
 * ({@link EditOp#accelerator(String)} via {@link MenuAcceleratorPolicy})
 * live in two files, so a future rebinding could silently desync the help
 * page from the running app - a keyboard user reading the docs would then be
 * told the wrong key. {@link HelpTopicsTest} proves the page exists and
 * resolves; this pins its <em>content</em>.
 *
 * <p>Each documented row whose function maps to an {@link EditOp} is checked
 * against the accelerator that op carries on this (non-mac) platform: the
 * key cell must name exactly the stroke the editor binds. A changed
 * accelerator, or a deleted/renamed row, fails here until the help page is
 * brought back in step - so the doc cannot drift from the code unnoticed.
 * Escape (End Wire), the arrow keys, and Enter are documented behaviors with
 * no {@code EditOp} accelerator and are intentionally not cross-checked
 * here.</p>
 */
class HotkeysHelpAccuracyTest {

	/**
	 * Documented function label (as it appears in the hot-key table) mapped
	 * to the {@link EditOp} whose accelerator the row must describe. The
	 * order mirrors the table for readability; every entry is verified to
	 * appear in the page (a removed row fails the completeness check).
	 */
	private static final Map<String, EditOp> DOCUMENTED_OPS = documentedOps();

	private static Map<String, EditOp> documentedOps() {
		Map<String, EditOp> m = new LinkedHashMap<>();
		m.put("Select All", EditOp.SELECT_ALL);
		m.put("Cut", EditOp.CUT);
		m.put("Copy", EditOp.COPY);
		m.put("Paste", EditOp.PASTE);
		m.put("Delete", EditOp.DELETE);
		m.put("Undo", EditOp.UNDO);
		m.put("Redo", EditOp.REDO);
		m.put("Start Wire", EditOp.WIRE_START);
		m.put("View Element Contents", EditOp.VIEW_VALUE);
		m.put("Rotate Element Clockwise", EditOp.ROTATE_CW);
		m.put("Rotate Element Counter-Clockwise", EditOp.ROTATE_CCW);
		m.put("Flip Element", EditOp.FLIP);
		m.put("Watch/Unwatch Element", EditOp.WATCH);
		m.put("Add/Remove Probe", EditOp.PROBE);
		m.put("Modify Element", EditOp.MODIFY);
		m.put("Change Timing", EditOp.TIMING);
		m.put("Lock Element", EditOp.LOCK);
		return m;
	}

	/** Matches a two-cell table row: {@code <td>function</td> ... <td>key</td>}. */
	private static final Pattern ROW = Pattern.compile(
			"(?is)<tr>\\s*<td>\\s*(.*?)\\s*</td>\\s*<td>\\s*(.*?)\\s*</td>\\s*</tr>");

	/** Locate the bundled hot-key help page as an on-disk file. */
	private static Path hotkeysPage() throws Exception {
		URL url = Help.class.getResource("/help/editor/editing/hotkeys.html");
		if (url == null) {
			fail("hotkeys.html missing from the classpath");
		}
		return Path.of(url.toURI());
	}

	/** Parse the table into function-label -> key-cell (raw, HTML stripped). */
	private static Map<String, String> keyCells() throws Exception {
		String html = new String(Files.readAllBytes(hotkeysPage()),
				StandardCharsets.ISO_8859_1);
		Map<String, String> cells = new LinkedHashMap<>();
		Matcher m = ROW.matcher(html);
		while (m.find()) {
			String function = stripTags(m.group(1)).trim();
			String key = stripTags(m.group(2)).trim();
			if (!function.isEmpty()) {
				cells.put(function, key);
			}
		}
		return cells;
	}

	private static String stripTags(String s) {
		return s.replaceAll("(?s)<[^>]*>", "").replaceAll("\\s+", " ").trim();
	}

	/**
	 * The canonical dotted rendering of a keystroke in the help page's
	 * convention: modifier prefixes (ctrl-/shift-/alt-/meta-) then the
	 * lower-cased key name, e.g. {@code ctrl-a}, {@code shift-r}, {@code w},
	 * {@code delete}. Matches how hotkeys.html spells its Key column.
	 *
	 * <p>The key name is derived from the keystroke's VK code by
	 * {@link #canonicalKeyName(int)}, never from
	 * {@link KeyEvent#getKeyText(int)}: on macOS AWT renders a whole class of
	 * keys as Unicode glyphs (VK_DELETE&#8594;&#x2326;,
	 * VK_BACK_SPACE&#8594;&#x232B;, VK_ESCAPE&#8594;&#x238B;, arrows, home),
	 * so getKeyText would make this comparison platform-dependent (issue #265,
	 * macOS lane first-light finding). The modifier prefixes come from the
	 * mask bits directly - never {@link KeyEvent#getKeyModifiersText(int)},
	 * which macOS also renders as glyphs (&#x2318;&#x2325;&#x21E7;&#x2303;).
	 * The result is folded through {@link #canonicalize(String)} so both sides
	 * of the check meet in the same platform-independent space.
	 */
	private static String render(KeyStroke ks) {
		StringBuilder sb = new StringBuilder();
		// EditOp accelerators carry only the extended (…_DOWN_MASK) modifiers;
		// the legacy InputEvent masks are deprecated and never set here.
		int mods = ks.getModifiers();
		if ((mods & InputEvent.CTRL_DOWN_MASK) != 0) {
			sb.append("ctrl-");
		}
		if ((mods & InputEvent.SHIFT_DOWN_MASK) != 0) {
			sb.append("shift-");
		}
		if ((mods & InputEvent.ALT_DOWN_MASK) != 0) {
			sb.append("alt-");
		}
		if ((mods & InputEvent.META_DOWN_MASK) != 0) {
			sb.append("meta-");
		}
		sb.append(canonicalKeyName(ks.getKeyCode()));
		return canonicalize(sb.toString());
	}

	/**
	 * The canonical lowercase name of a key, derived from its
	 * {@link KeyEvent} VK code and therefore identical on every platform -
	 * unlike {@link KeyEvent#getKeyText(int)}, which on macOS returns Unicode
	 * glyphs for the special keys (&#x2326; forward-delete, &#x232B;
	 * backspace, &#x238B; escape, the arrow glyphs, &#x2196; home, …) and so
	 * would silently desync this check from the ASCII names hotkeys.html
	 * spells (issue #265). Letters and digits derive arithmetically from the
	 * VK code ({@code VK_A == 'A'}); the special keys the editor can bind are
	 * mapped explicitly to the same tokens {@link #GLYPHS} folds their macOS
	 * glyphs to. A VK code with no canonical name fails loudly so a new
	 * accelerator is given one here rather than reintroducing a
	 * platform-dependent glyph render.
	 *
	 * @param keyCode a {@code KeyEvent.VK_*} code.
	 * @return the platform-independent lowercase key name.
	 */
	private static String canonicalKeyName(int keyCode) {
		if (keyCode >= KeyEvent.VK_A && keyCode <= KeyEvent.VK_Z) {
			return String.valueOf((char) ('a' + (keyCode - KeyEvent.VK_A)));
		}
		if (keyCode >= KeyEvent.VK_0 && keyCode <= KeyEvent.VK_9) {
			return String.valueOf((char) ('0' + (keyCode - KeyEvent.VK_0)));
		}
		if (keyCode >= KeyEvent.VK_F1 && keyCode <= KeyEvent.VK_F12) {
			return "f" + (1 + (keyCode - KeyEvent.VK_F1));
		}
		switch (keyCode) {
			case KeyEvent.VK_DELETE:
				return "delete";
			case KeyEvent.VK_BACK_SPACE:
				return "backspace";
			case KeyEvent.VK_ESCAPE:
				return "escape";
			case KeyEvent.VK_ENTER:
				return "enter";
			case KeyEvent.VK_SPACE:
				return "space";
			case KeyEvent.VK_TAB:
				return "tab";
			case KeyEvent.VK_LEFT:
				return "left";
			case KeyEvent.VK_RIGHT:
				return "right";
			case KeyEvent.VK_UP:
				return "up";
			case KeyEvent.VK_DOWN:
				return "down";
			case KeyEvent.VK_HOME:
				return "home";
			case KeyEvent.VK_END:
				return "end";
			case KeyEvent.VK_PAGE_UP:
				return "page up";
			case KeyEvent.VK_PAGE_DOWN:
				return "page down";
			case KeyEvent.VK_INSERT:
				return "insert";
			default:
				throw new AssertionError("no canonical name for VK code "
						+ keyCode + " (KeyEvent.getKeyText='"
						+ KeyEvent.getKeyText(keyCode) + "'); add it to "
						+ "canonicalKeyName so the help-vs-accelerator check "
						+ "stays platform-independent");
		}
	}

	/**
	 * The Unicode glyphs macOS's AWT renders for keys and modifiers, mapped to
	 * the canonical ASCII tokens hotkeys.html and {@link #canonicalKeyName}
	 * use. Folding these (see {@link #canonicalize(String)}) means a glyph
	 * that reaches the comparison - from rendered text on macOS, or from a
	 * future glyph-bearing edit to the help page - is normalized instead of
	 * mistaken for drift. The modifier glyphs (&#x2318; command,
	 * &#x2325; option, &#x21E7; shift, &#x2303; control) are included so the
	 * mac Cmd-vs-Ctrl menu-mask path (Toolkit.getMenuShortcutKeyMaskEx) cannot
	 * cause the same class of drift on a later macOS run (issue #265 req 4).
	 */
	private static final Map<String, String> GLYPHS = glyphs();

	private static Map<String, String> glyphs() {
		Map<String, String> g = new LinkedHashMap<>();
		g.put("⌦", "delete");     // ⌦ forward delete
		g.put("⌫", "backspace");  // ⌫
		g.put("⎋", "escape");     // ⎋
		g.put("↩", "enter");      // ↩ return
		g.put("⇥", "tab");        // ⇥
		g.put("←", "left");       // ←
		g.put("→", "right");      // →
		g.put("↑", "up");         // ↑
		g.put("↓", "down");       // ↓
		g.put("↖", "home");       // ↖
		g.put("↘", "end");        // ↘
		g.put("⇞", "page up");    // ⇞
		g.put("⇟", "page down");  // ⇟
		g.put("⌘", "meta-");      // ⌘ command
		g.put("⌥", "alt-");       // ⌥ option
		g.put("⇧", "shift-");     // ⇧
		g.put("⌃", "ctrl-");      // ⌃ control
		return g;
	}

	/**
	 * Fold any macOS key or modifier glyph to its canonical ASCII token and
	 * lower-case, so the accelerator side and the parsed-help side of the
	 * comparison meet in the same platform-independent space. Applied to both
	 * {@link #render(KeyStroke)} output and each parsed help cell
	 * ({@link #primaryToken(String)}); it is idempotent on text that is
	 * already canonical.
	 *
	 * @param token a rendered accelerator or a parsed help key cell.
	 * @return the canonical lowercase token.
	 */
	private static String canonicalize(String token) {
		String t = token;
		for (Map.Entry<String, String> e : GLYPHS.entrySet()) {
			t = t.replace(e.getKey(), e.getValue());
		}
		return t.toLowerCase(java.util.Locale.ROOT);
	}

	/**
	 * The primary key token of a help Key cell: the part before any
	 * parenthetical note, canonicalized. The Redo cell reads
	 * "ctrl-y (shift-cmd-z on macOS; cmd-y also works)"; its primary token
	 * is "ctrl-y". Every other cell has no parenthetical, so this is a no-op
	 * for them. Canonicalized via {@link #canonicalize(String)} so it shares
	 * the accelerator side's normalization.
	 */
	private static String primaryToken(String keyCell) {
		int paren = keyCell.indexOf('(');
		String head = paren >= 0 ? keyCell.substring(0, paren) : keyCell;
		return canonicalize(head.trim());
	}

	/**
	 * Every documented editing-op row names exactly the accelerator the
	 * editor binds for that op on this platform, so the help page cannot
	 * drift from {@link EditOp#accelerator(String)}.
	 *
	 * @throws Exception if the page cannot be read.
	 */
	@Test
	void documentedKeysMatchTheEditOpAccelerators() throws Exception {
		// non-mac: the CI substrate, and the page's parenthetical already
		// carries the mac variants separately (checked structurally below)
		String os = "Linux";
		Map<String, String> cells = keyCells();
		List<String> problems = new ArrayList<>();
		for (Map.Entry<String, EditOp> e : DOCUMENTED_OPS.entrySet()) {
			String function = e.getKey();
			EditOp op = e.getValue();
			String cell = cells.get(function);
			if (cell == null) {
				problems.add("hotkeys.html no longer documents \"" + function
						+ "\" (expected for " + op + ")");
				continue;
			}
			String expected = render(op.accelerator(os));
			String actual = primaryToken(cell);
			if (!expected.equals(actual)) {
				problems.add("\"" + function + "\": help says '" + actual
						+ "' but " + op + " binds '" + expected + "'");
			}
		}
		assertTrue(problems.isEmpty(),
				"hotkeys.html has drifted from the editor's accelerators:\n  "
						+ String.join("\n  ", problems));
	}

	/**
	 * Sanity guard on the fixture: the page still parses into the rows this
	 * test cross-checks, so a structural rewrite that broke the table cannot
	 * make the accuracy check silently vacuous.
	 *
	 * @throws Exception if the page cannot be read.
	 */
	@Test
	void theHotkeyTableStillParses() throws Exception {
		Map<String, String> cells = keyCells();
		assertTrue(cells.size() >= DOCUMENTED_OPS.size(),
				"hotkeys.html parsed to only " + cells.size()
						+ " rows; expected at least " + DOCUMENTED_OPS.size());
		// the Escape / arrow / Enter behaviors are documented too
		assertTrue(cells.containsKey("Move Caret / Following Element / Wire End "
				+ "/ Selection"),
				"the arrow-key row is missing from hotkeys.html");
	}

	/**
	 * Headless pin for the macOS rendering class (issue #265, macOS lane
	 * first-light finding). The accelerator side names keys from their VK code
	 * via {@link #canonicalKeyName(int)}, so the Unicode glyphs macOS's
	 * {@link KeyEvent#getKeyText(int)} returns for the special keys can never
	 * reach the comparison. This asserts the VK codes that render as glyphs on
	 * macOS - VK_DELETE ("⌦"), VK_BACK_SPACE ("⌫"), VK_ESCAPE ("⎋") - yield
	 * their ASCII names on every platform. On the pre-fix code (which called
	 * {@code getKeyText(VK_DELETE).toLowerCase()}) this returned "⌦" on macOS,
	 * the exact drift that failed run 30315415693; canonicalKeyName is
	 * glyph-free by construction, verifiable headlessly on Linux.
	 */
	@Test
	void canonicalKeyNameIsGlyphFreeForTheMacRenderingClass() {
		assertEquals("delete", canonicalKeyName(KeyEvent.VK_DELETE));
		assertEquals("backspace", canonicalKeyName(KeyEvent.VK_BACK_SPACE));
		assertEquals("escape", canonicalKeyName(KeyEvent.VK_ESCAPE));
		// the letter/digit path that every ctrl-chord accelerator uses
		assertEquals("a", canonicalKeyName(KeyEvent.VK_A));
		assertEquals("z", canonicalKeyName(KeyEvent.VK_Z));
	}

	/**
	 * Headless pin that the shared normalizer folds the exact glyph forms
	 * macOS produces back to canonical ASCII (issue #265). Feeding the
	 * normalizer the glyphs {@link KeyEvent#getKeyText(int)} /
	 * {@link KeyEvent#getKeyModifiersText(int)} emit on macOS proves both
	 * sides of the check would agree even if a glyph reached the comparison.
	 * The pre-fix normalization was a bare {@code toLowerCase}, under which
	 * "⌦" stays "⌦" (verified: {@code "⌦".toLowerCase() != "delete"}), so
	 * this pin fails on the old code regardless of platform.
	 */
	@Test
	void canonicalizeFoldsTheMacGlyphs() {
		assertEquals("delete", canonicalize("⌦"));
		assertEquals("backspace", canonicalize("⌫"));
		assertEquals("escape", canonicalize("⎋"));
		// modifier glyphs, hardening the Cmd/Ctrl menu-mask path (req 4)
		assertEquals("meta-", canonicalize("⌘"));
		assertEquals("alt-", canonicalize("⌥"));
		assertEquals("shift-", canonicalize("⇧"));
		assertEquals("ctrl-", canonicalize("⌃"));
		// a full macOS-style rendered chord folds to the help spelling
		assertEquals("meta-delete", canonicalize("⌘⌦"));
	}
}
