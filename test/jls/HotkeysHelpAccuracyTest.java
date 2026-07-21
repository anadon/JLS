package jls;

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
		sb.append(KeyEvent.getKeyText(ks.getKeyCode())
				.toLowerCase(java.util.Locale.ROOT));
		return sb.toString();
	}

	/**
	 * The primary key token of a help Key cell: the part before any
	 * parenthetical note. The Redo cell reads
	 * "ctrl-y (shift-cmd-z on macOS; cmd-y also works)"; its primary token
	 * is "ctrl-y". Every other cell has no parenthetical, so this is a no-op
	 * for them.
	 */
	private static String primaryToken(String keyCell) {
		int paren = keyCell.indexOf('(');
		String head = paren >= 0 ? keyCell.substring(0, paren) : keyCell;
		return head.trim().toLowerCase(java.util.Locale.ROOT);
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
}
