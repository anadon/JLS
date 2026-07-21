package jls.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import jls.Circuit;

/**
 * Tool-bar palette buttons expose an accessible name (issue #75
 * accessibility, item 6). The palette buttons are icon-only, so their shared
 * {@link javax.swing.Action}'s {@code NAME} is the empty string; without an
 * explicit accessible name a screen reader focusing a palette button would
 * announce nothing. {@code SimpleEditor.makeElement} sets the accessible name
 * from the tooltip (the human-readable element name), and the buttons are
 * keyboard-focusable so a keyboard/AT user can reach and identify them.
 *
 * <p>This was an unobserved real gap: tests located palette buttons by
 * tooltip, so the tooltip was incidentally relied on, but nothing asserted an
 * accessible <em>name</em> existed. This test reads
 * {@code getAccessibleContext().getAccessibleName()} on the real buttons.</p>
 *
 * <p>Tagged {@code display}: boots a real editor tool bar, so it runs under
 * the #162 substrate
 * (xvfb-run -a mvn -B verify -Djls.test.headless=false).</p>
 */
@Tag("display")
@Timeout(60)
class PaletteButtonAccessibilityTest {

	@BeforeEach
	void requireDisplay() {
		Assumptions.assumeFalse(GraphicsEnvironment.isHeadless(),
				"display suite: skipped in headless runs");
	}

	/**
	 * Every tool-bar palette button exposes a non-blank accessible name and
	 * is keyboard-focusable, and a representative button's name is its
	 * element label.
	 *
	 * @throws Exception on EDT failure.
	 */
	@Test
	void everyPaletteButtonHasAnAccessibleNameAndIsFocusable()
			throws Exception {
		try (EditorGestureSupport ui =
				new EditorGestureSupport(new Circuit("palette"))) {
			List<JButton> buttons = new ArrayList<>();
			collectButtons(ui.editor, buttons);
			assertTrue(buttons.size() >= 8,
					"the tool bar has the element palette buttons (found "
							+ buttons.size() + ")");

			for (JButton button : buttons) {
				String name =
						button.getAccessibleContext().getAccessibleName();
				assertFalse(name == null || name.isBlank(),
						"palette button (tooltip '" + button.getToolTipText()
								+ "') exposes a non-blank accessible name, got '"
								+ name + "'");
				assertTrue(button.isFocusable(),
						"palette button '" + name + "' is keyboard-focusable");
			}

			// a representative button's accessible name is its element label
			JButton andButton = findByTooltip(buttons, "AND gate");
			assertTrue(andButton != null, "found the AND gate button");
			assertEquals("AND gate",
					andButton.getAccessibleContext().getAccessibleName(),
					"the AND gate button announces its element name");
		}
	}

	/** Collect every JButton with a tooltip (the palette buttons). */
	private static void collectButtons(Container root, List<JButton> out) {
		for (Component c : root.getComponents()) {
			if (c instanceof JButton button
					&& button.getToolTipText() != null) {
				out.add(button);
			}
			if (c instanceof Container inner) {
				collectButtons(inner, out);
			}
		}
	}

	private static JButton findByTooltip(List<JButton> buttons, String tip) {
		for (JButton b : buttons) {
			if (tip.equals(b.getToolTipText())) {
				return b;
			}
		}
		return null;
	}
}
