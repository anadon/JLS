package jls.edit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.swing.KeyStroke;

import org.junit.jupiter.api.Test;

/**
 * Pins the delete key-binding policy (issue #127): deletion must be
 * reachable from keyboards without a forward-Delete key (every Apple
 * laptop and compact keyboard), so the editor canvas binds plain
 * Backspace alongside plain Delete on every platform, and the popup
 * menu displays the key the platform actually has.
 *
 * The editor itself cannot be constructed under surefire's
 * {@code java.awt.headless=true} (EditWindow queries
 * {@code getMenuShortcutKeyMaskEx()}, which throws HeadlessException),
 * so the policy is a pure function tested directly, and the wiring is
 * pinned by reading SimpleEditor's source from the repo tree — the
 * same compensating-control pattern as ToolkitPolicyTest and
 * HeadlessCoreRatchetTest.
 */
class DeleteKeyPolicyTest {

	private static final KeyStroke PLAIN_DELETE =
			KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
	private static final KeyStroke PLAIN_BACKSPACE =
			KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0);

	// ---- the canvas bindings (issue #127 H1, P1, P3) ----

	/** P3: forward-Delete keeps working everywhere it does today. */
	@Test
	void canvasBindsPlainDelete() {
		assertTrue(DeleteKeyPolicy.canvasBindings().contains(PLAIN_DELETE),
				"plain Delete must remain bound (P3)");
	}

	/** P1: Backspace deletes the selection — the Mac-keyboard fix. */
	@Test
	void canvasBindsPlainBackspace() {
		assertTrue(DeleteKeyPolicy.canvasBindings().contains(PLAIN_BACKSPACE),
				"plain Backspace must be bound (P1)");
	}

	/** Exactly the two unmodified keys — no modifier chords, so the
	 *  binding cannot silently depend on the platform menu mask. */
	@Test
	void canvasBindingsAreExactlyTheTwoUnmodifiedKeys() {
		List<KeyStroke> bindings = DeleteKeyPolicy.canvasBindings();
		assertEquals(2, bindings.size(), bindings.toString());
		for (KeyStroke stroke : bindings)
			assertEquals(0, stroke.getModifiers(),
					"delete bindings carry no modifiers: " + stroke);
	}

	// ---- the displayed accelerator, per platform ----

	/** macOS keyboards have no forward-Delete key: show Backspace. */
	@Test
	void macMenuShowsBackspace() {
		assertEquals(PLAIN_BACKSPACE,
				DeleteKeyPolicy.menuAccelerator("Mac OS X"));
		assertEquals(PLAIN_BACKSPACE,
				DeleteKeyPolicy.menuAccelerator("macOS"));
	}

	/** Everywhere else the Delete key exists: keep showing it. */
	@Test
	void otherPlatformsMenuShowsDelete() {
		assertEquals(PLAIN_DELETE, DeleteKeyPolicy.menuAccelerator("Linux"));
		assertEquals(PLAIN_DELETE,
				DeleteKeyPolicy.menuAccelerator("Windows 11"));
		assertEquals(PLAIN_DELETE, DeleteKeyPolicy.menuAccelerator(null));
	}

	// ---- os.name recognition ----

	@Test
	void isMacRecognizesAppleOsNames() {
		assertTrue(DeleteKeyPolicy.isMac("Mac OS X"));
		assertTrue(DeleteKeyPolicy.isMac("macOS"));
		assertTrue(DeleteKeyPolicy.isMac("OS X"));
		assertFalse(DeleteKeyPolicy.isMac("Linux"));
		assertFalse(DeleteKeyPolicy.isMac("Windows 11"));
		assertFalse(DeleteKeyPolicy.isMac(null));
	}

	// ---- the wiring in SimpleEditor ----

	/**
	 * SimpleEditor must consume the policy rather than hard-coding
	 * VK_DELETE: the delete operation's accelerator and the canvas
	 * input-map bindings both go through DeleteKeyPolicy, and no
	 * literal VK_DELETE keystroke construction remains. This is the
	 * regression pin for the wiring the headless JVM cannot exercise
	 * (constructing the editor throws HeadlessException); it fails on
	 * pre-#127 SimpleEditor, which bound
	 * {@code KeyStroke.getKeyStroke(KeyEvent.VK_DELETE,0)} directly.
	 * Issue #75 folded the delete menu item, popup item, and key binding
	 * into one shared {@link javax.swing.Action}, so the menu accelerator
	 * now comes from the policy when that Action is built.
	 */
	@Test
	void simpleEditorDelegatesDeleteBindingsToThePolicy()
			throws IOException {
		Path source = Path.of(System.getProperty("user.dir"),
				"src/jls/edit/SimpleEditor.java");
		assertTrue(Files.isRegularFile(source),
				"run from the repo root (maven sets user.dir there): "
						+ source);
		String text = Files.readString(source, StandardCharsets.UTF_8);

		assertTrue(text.contains("DeleteKeyPolicy.menuAccelerator("),
				"the delete accelerator must come from the policy");
		assertTrue(text.contains("DeleteKeyPolicy.canvasBindings()"),
				"the canvas delete bindings must come from the policy");
		assertFalse(text.contains("VK_DELETE"),
				"no hard-coded VK_DELETE binding may remain in "
						+ "SimpleEditor (issue #127 O1)");
	}

} // end of DeleteKeyPolicyTest class
