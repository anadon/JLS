package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.KeyStroke;

import org.junit.jupiter.api.Test;

/**
 * Pins the menu-bar accelerator, mnemonic, and canvas key-binding
 * policy (issue #75). Before the policy existed the menu bar carried
 * no accelerator or mnemonic outside Simulator Run/Stop (F5/F7), so no
 * file operation was reachable — or even discoverable — from the
 * keyboard, and rotate/flip had no binding at all.
 *
 * The GUI cannot be constructed under surefire's
 * {@code java.awt.headless=true}, so the policy is a pure function of
 * an injected {@code os.name} tested directly, and the wiring in
 * JLSStart and SimpleEditor is pinned by reading their source from the
 * repo tree — the same compensating-control pattern as
 * DeleteKeyPolicyTest and ToolkitPolicyTest.
 */
class MenuAcceleratorPolicyTest {

	// ---- platform modifier ----

	/** The menu modifier is Command on macOS and Ctrl elsewhere,
	 *  matching what Toolkit.getMenuShortcutKeyMaskEx() reports on
	 *  those platforms at runtime. */
	@Test
	void macUsesCommandOthersUseControl() {
		assertEquals(InputEvent.META_DOWN_MASK,
				MenuAcceleratorPolicy.menuMask("Mac OS X"));
		assertEquals(InputEvent.META_DOWN_MASK,
				MenuAcceleratorPolicy.menuMask("macOS"));
		assertEquals(InputEvent.CTRL_DOWN_MASK,
				MenuAcceleratorPolicy.menuMask("Linux"));
		assertEquals(InputEvent.CTRL_DOWN_MASK,
				MenuAcceleratorPolicy.menuMask("Windows 11"));
		assertEquals(InputEvent.CTRL_DOWN_MASK,
				MenuAcceleratorPolicy.menuMask(null));
	}

	@Test
	void isMacRecognizesAppleOsNames() {
		assertTrue(MenuAcceleratorPolicy.isMac("Mac OS X"));
		assertTrue(MenuAcceleratorPolicy.isMac("macOS"));
		assertTrue(MenuAcceleratorPolicy.isMac("OS X"));
		assertFalse(MenuAcceleratorPolicy.isMac("Linux"));
		assertFalse(MenuAcceleratorPolicy.isMac("Windows 11"));
		assertFalse(MenuAcceleratorPolicy.isMac(null));
	}

	// ---- file-operation accelerators (issue #75 P1) ----

	/** N/O/S/Q ride the platform modifier on every platform, and all
	 *  file accelerators are pairwise distinct. */
	@Test
	void fileAcceleratorsUseThePlatformMask() {
		for (String os : new String[] { "Mac OS X", "Linux",
				"Windows 11" }) {
			int mask = MenuAcceleratorPolicy.menuMask(os);
			assertEquals(KeyStroke.getKeyStroke(KeyEvent.VK_N, mask),
					MenuAcceleratorPolicy.newCircuit(os), os);
			assertEquals(KeyStroke.getKeyStroke(KeyEvent.VK_O, mask),
					MenuAcceleratorPolicy.open(os), os);
			assertEquals(KeyStroke.getKeyStroke(KeyEvent.VK_S, mask),
					MenuAcceleratorPolicy.save(os), os);
			assertEquals(KeyStroke.getKeyStroke(KeyEvent.VK_Q, mask),
					MenuAcceleratorPolicy.exit(os), os);
			Set<KeyStroke> all = new HashSet<>(List.of(
					MenuAcceleratorPolicy.newCircuit(os),
					MenuAcceleratorPolicy.open(os),
					MenuAcceleratorPolicy.save(os),
					MenuAcceleratorPolicy.saveAs(os),
					MenuAcceleratorPolicy.exit(os)));
			assertEquals(5, all.size(),
					"file accelerators must be distinct on " + os);
		}
	}

	/** Save As is exactly Save plus Shift, the desktop convention. */
	@Test
	void saveAsIsShiftedSave() {
		for (String os : new String[] { "Mac OS X", "Linux",
				"Windows 11" }) {
			assertEquals(KeyStroke.getKeyStroke(KeyEvent.VK_S,
					MenuAcceleratorPolicy.menuMask(os)
							| InputEvent.SHIFT_DOWN_MASK),
					MenuAcceleratorPolicy.saveAs(os), os);
		}
	}

	// ---- redo: macOS convention, old binding kept as alias ----

	/** macOS displays the conventional Shift+Cmd+Z, not Ctrl+Y. */
	@Test
	void macRedoIsShiftCommandZ() {
		assertEquals(KeyStroke.getKeyStroke(KeyEvent.VK_Z,
				InputEvent.META_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK),
				MenuAcceleratorPolicy.redoDisplayed("Mac OS X"));
	}

	/** The adjudicated alias rule: the historical Cmd+Y stays bound on
	 *  macOS from day one, alongside the new conventional stroke. */
	@Test
	void macRedoKeepsTheOldBindingAsAnAlias() {
		List<KeyStroke> bindings =
				MenuAcceleratorPolicy.redoBindings("Mac OS X");
		assertEquals(2, bindings.size(), bindings.toString());
		assertEquals(MenuAcceleratorPolicy.redoDisplayed("Mac OS X"),
				bindings.get(0),
				"the displayed accelerator must be bound first");
		assertTrue(bindings.contains(KeyStroke.getKeyStroke(
				KeyEvent.VK_Y, InputEvent.META_DOWN_MASK)),
				"Cmd+Y must stay bound as the day-one alias (#75)");
	}

	/** Windows/Linux redo is exactly the historical Ctrl+Y — displayed
	 *  and bound, nothing added, nothing removed. */
	@Test
	void otherPlatformsRedoStaysControlY() {
		KeyStroke ctrlY = KeyStroke.getKeyStroke(KeyEvent.VK_Y,
				InputEvent.CTRL_DOWN_MASK);
		for (String os : new String[] { "Linux", "Windows 11", null }) {
			assertEquals(ctrlY,
					MenuAcceleratorPolicy.redoDisplayed(os));
			assertEquals(List.of(ctrlY),
					MenuAcceleratorPolicy.redoBindings(os));
		}
	}

	// ---- rotate/flip canvas bindings ----

	/** Rotate is R/Shift+R and flip is F: plain keys with no menu-mask
	 *  modifier (so they can never collide with a menu chord), all
	 *  three distinct. */
	@Test
	void rotateAndFlipArePlainKeys() {
		assertEquals(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0),
				MenuAcceleratorPolicy.rotateCwStroke());
		assertEquals(KeyStroke.getKeyStroke(KeyEvent.VK_R,
				InputEvent.SHIFT_DOWN_MASK),
				MenuAcceleratorPolicy.rotateCcwStroke());
		assertEquals(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0),
				MenuAcceleratorPolicy.flipStroke());
		Set<KeyStroke> all = new HashSet<>(List.of(
				MenuAcceleratorPolicy.rotateCwStroke(),
				MenuAcceleratorPolicy.rotateCcwStroke(),
				MenuAcceleratorPolicy.flipStroke()));
		assertEquals(3, all.size(), "rotate/flip strokes must differ");
	}

	/** View-current-value is plain V: its historical menu-mask+S
	 *  stroke shadowed the menu bar's Save accelerator whenever the
	 *  canvas had focus, which is the exact trap #75 closes (P1:
	 *  Ctrl/Cmd+S saves from any focus location). */
	@Test
	void viewValueIsPlainVNotMaskedS() {
		assertEquals(KeyStroke.getKeyStroke(KeyEvent.VK_V, 0),
				MenuAcceleratorPolicy.viewValueStroke());
		for (String os : new String[] { "Mac OS X", "Linux",
				"Windows 11" }) {
			assertFalse(MenuAcceleratorPolicy.viewValueStroke().equals(
					MenuAcceleratorPolicy.save(os)),
					"view must never shadow Save on " + os);
		}
	}

	/** Wire-start is plain W: issue #75 moved it off the Ctrl/Cmd+W chord
	 *  it used to share with toggle-watch (the same stroke was shown on
	 *  two popup items), so it is a plain key like rotate/flip and can
	 *  never collide with the now-sole Ctrl/Cmd+W watch binding. */
	@Test
	void wireStartIsPlainW() {
		assertEquals(KeyStroke.getKeyStroke(KeyEvent.VK_W, 0),
				MenuAcceleratorPolicy.wireStartStroke());
		for (String os : new String[] { "Mac OS X", "Linux",
				"Windows 11" }) {
			assertFalse(MenuAcceleratorPolicy.wireStartStroke().equals(
					KeyStroke.getKeyStroke(KeyEvent.VK_W,
							MenuAcceleratorPolicy.menuMask(os))),
					"wire-start must never collide with Ctrl/Cmd+W watch on "
							+ os);
		}
	}

	// ---- menu mnemonics ----

	/** Every top-level menu gets its own mnemonic; Alt+letter must be
	 *  unambiguous across the menu bar. Issue #75 added the Edit and
	 *  Element menus, so all six must stay distinct. */
	@Test
	void menuMnemonicsAreDistinct() {
		Set<Integer> mnemonics = new HashSet<>(List.of(
				MenuAcceleratorPolicy.fileMenuMnemonic(),
				MenuAcceleratorPolicy.editMenuMnemonic(),
				MenuAcceleratorPolicy.elementMenuMnemonic(),
				MenuAcceleratorPolicy.simulatorMenuMnemonic(),
				MenuAcceleratorPolicy.globalMenuMnemonic(),
				MenuAcceleratorPolicy.helpMenuMnemonic()));
		assertEquals(6, mnemonics.size(),
				"top-level menu mnemonics must be distinct");
	}

	// ---- the wiring in JLSStart ----

	/**
	 * JLSStart must consume the policy rather than leaving the menu
	 * bar bare: every file operation's accelerator and every top-level
	 * menu's mnemonic goes through MenuAcceleratorPolicy. This is the
	 * regression pin for wiring the headless JVM cannot exercise; it
	 * fails on pre-#75 JLSStart, whose only accelerators were F5/F7.
	 */
	@Test
	void jlsStartWiresTheMenuBarThroughThePolicy() throws IOException {
		String text = readSource("src/jls/JLSStart.java");

		assertTrue(text.contains(
				"newc.setAccelerator(MenuAcceleratorPolicy.newCircuit("),
				"File>New accelerator must come from the policy");
		assertTrue(text.contains(
				"open.setAccelerator(MenuAcceleratorPolicy.open("),
				"File>Open accelerator must come from the policy");
		assertTrue(text.contains(
				"saveItem.setAccelerator(MenuAcceleratorPolicy.save("),
				"File>Save accelerator must come from the policy");
		assertTrue(text.contains(
				"saveAs.setAccelerator(MenuAcceleratorPolicy.saveAs("),
				"File>Save As accelerator must come from the policy");
		assertTrue(text.contains(
				"exit.setAccelerator(MenuAcceleratorPolicy.exit("),
				"File>Exit accelerator must come from the policy");
		assertTrue(text.contains(
				"setMnemonic(MenuAcceleratorPolicy.fileMenuMnemonic())"),
				"the File menu must have the policy mnemonic");
		assertTrue(text.contains(
				"setMnemonic(MenuAcceleratorPolicy.simulatorMenuMnemonic())"),
				"the Simulator menu must have the policy mnemonic");
		assertTrue(text.contains(
				"setMnemonic(MenuAcceleratorPolicy.globalMenuMnemonic())"),
				"the Global menu must have the policy mnemonic");
		assertTrue(text.contains(
				"setMnemonic(MenuAcceleratorPolicy.helpMenuMnemonic())"),
				"the Help menu must have the policy mnemonic");
		assertTrue(text.contains("ed.focusOnCanvas()"),
				"tab selection must hand keyboard focus to the canvas");
	}

	// ---- the wiring in SimpleEditor ----

	/**
	 * SimpleEditor must consume the policy for redo and rotate/flip,
	 * and the focus-follows-mouse grab must stay gone: no
	 * {@code requestFocusInWindow(true)} hover grab, a plain
	 * {@code requestFocusInWindow()} click-to-focus instead, and no
	 * hard-coded VK_Y redo stroke. Fails on pre-#75 SimpleEditor,
	 * which grabbed focus in mouseEntered and bound redo to a literal
	 * Ctrl+Y on every platform.
	 */
	@Test
	void simpleEditorDelegatesBindingsAndDropsHoverFocus()
			throws IOException {
		String text = readSource("src/jls/edit/SimpleEditor.java");

		assertTrue(text.contains("MenuAcceleratorPolicy.redoDisplayed("),
				"the redo menu accelerator must come from the policy");
		assertTrue(text.contains("MenuAcceleratorPolicy.redoBindings("),
				"the canvas redo bindings must come from the policy");
		assertFalse(text.contains("VK_Y"),
				"no hard-coded VK_Y redo binding may remain");
		assertTrue(text.contains("MenuAcceleratorPolicy.rotateCwStroke()"),
				"rotate clockwise must be bound through the policy");
		assertTrue(text.contains("MenuAcceleratorPolicy.rotateCcwStroke()"),
				"rotate counter-clockwise must be bound through the policy");
		assertTrue(text.contains("MenuAcceleratorPolicy.flipStroke()"),
				"flip must be bound through the policy");
		assertTrue(text.contains("MenuAcceleratorPolicy.viewValueStroke()"),
				"view-value must be bound through the policy");
		assertFalse(text.contains("KeyEvent.VK_S,Toolkit"),
				"no canvas menu-mask+S binding may remain: it would "
						+ "shadow the menu bar's Save accelerator (#75 P1)");
		assertFalse(text.contains("requestFocusInWindow(true)"),
				"the focus-follows-mouse hover grab must stay gone (#75)");
		assertTrue(text.contains("requestFocusInWindow();"),
				"the canvas must take focus on click instead");
	}

	/**
	 * Read a source file from the repo tree (maven runs tests with
	 * user.dir at the repo root).
	 *
	 * @param relative repo-relative path of the source file.
	 *
	 * @return the file's text.
	 *
	 * @throws IOException if the file cannot be read.
	 */
	private static String readSource(String relative) throws IOException {
		Path source = Path.of(System.getProperty("user.dir"), relative);
		assertTrue(Files.isRegularFile(source),
				"run from the repo root (maven sets user.dir there): "
						+ source);
		return Files.readString(source, StandardCharsets.UTF_8);
	} // end of readSource method

} // end of MenuAcceleratorPolicyTest class
