package jls.edit;

import jls.elem.ElementType;

/**
 * The GUI-side descriptor for one editor palette element (issue #78):
 * the second half of the two-layer element descriptor split. The core
 * half, {@link ElementType}, carries what loading, saving, and headless
 * tooling need (tag, aliases, class, factory) and lives in
 * {@code jls.elem}; this class carries only what the editor palette
 * needs to present that type - the icon resource, the fallback button
 * text shown when the icon is missing, the tooltip/display name, the
 * toolbar group, and the help topic - and never leaks into the core.
 *
 * A palette entry deliberately carries no capability set: capabilities
 * are the {@code instanceof} interfaces ({@code Rotatable},
 * {@code Editable}, ...) pinned by {@code CapabilityInterfaceTest}, and
 * duplicating them here would reintroduce the drift the interfaces
 * removed.
 *
 * All entries live in the static {@link Palette} table, whose authoring
 * contract ({@code PaletteContractTest}) enforces that the table stays
 * total over the registered element types and that every icon, tooltip,
 * and help topic is real.
 */
public final class PaletteEntry {

	/** The core descriptor of the element type this entry creates. */
	private final ElementType type;

	/** The toolbar group this entry belongs to. */
	private final Palette.Group group;

	/** Base name of the icon (a gif in {@code jls/edit/images/}). */
	private final String iconName;

	/** Button text shown when the icon resource is missing. */
	private final String fallbackText;

	/** The tooltip, which is also the accessible display name. */
	private final String tooltip;

	/** The {@code Map.jhm} topic documenting this element. */
	private final String helpTopic;

	/**
	 * Create a descriptor for one palette element. Package-private: the
	 * {@link Palette} table is the only author of entries.
	 *
	 * @param type The core descriptor of the element type the entry
	 *            creates.
	 * @param group The toolbar group the entry belongs to.
	 * @param iconName Base name of the icon gif in
	 *            {@code jls/edit/images/}; must be non-blank.
	 * @param fallbackText Button text shown when the icon resource is
	 *            missing; must be non-blank.
	 * @param tooltip The tooltip and accessible display name; must be
	 *            non-blank.
	 * @param helpTopic The {@code Map.jhm} topic documenting the
	 *            element; must be non-blank.
	 */
	PaletteEntry(ElementType type, Palette.Group group, String iconName,
			String fallbackText, String tooltip, String helpTopic) {

		if (type == null) {
			throw new IllegalArgumentException("no element type");
		}
		if (group == null) {
			throw new IllegalArgumentException(
					"no palette group for tag " + type.tag());
		}
		if (iconName == null || iconName.isBlank()) {
			throw new IllegalArgumentException(
					"blank icon name for tag " + type.tag());
		}
		if (fallbackText == null || fallbackText.isBlank()) {
			throw new IllegalArgumentException(
					"blank fallback text for tag " + type.tag());
		}
		if (tooltip == null || tooltip.isBlank()) {
			throw new IllegalArgumentException(
					"blank tooltip for tag " + type.tag());
		}
		if (helpTopic == null || helpTopic.isBlank()) {
			throw new IllegalArgumentException(
					"blank help topic for tag " + type.tag());
		}
		this.type = type;
		this.group = group;
		this.iconName = iconName;
		this.fallbackText = fallbackText;
		this.tooltip = tooltip;
		this.helpTopic = helpTopic;
	} // end of constructor

	/**
	 * The core descriptor of the element type this entry creates.
	 *
	 * @return the {@link ElementType}; its factory creates the element
	 *         when the palette button is pressed.
	 */
	public ElementType type() {

		return type;
	} // end of type method

	/**
	 * The toolbar group this entry belongs to.
	 *
	 * @return the group whose GridLayout panel holds the entry's button.
	 */
	public Palette.Group group() {

		return group;
	} // end of group method

	/**
	 * The base name of the icon resource.
	 *
	 * @return the name of a gif in {@code jls/edit/images/}, without
	 *         directory or extension.
	 */
	public String iconName() {

		return iconName;
	} // end of iconName method

	/**
	 * The button text shown when the icon resource is missing.
	 *
	 * @return the fallback text; empty text is never used, so a missing
	 *         icon still leaves a usable, labeled button.
	 */
	public String fallbackText() {

		return fallbackText;
	} // end of fallbackText method

	/**
	 * The tooltip, which also serves as the accessible name of the
	 * palette button and its mirror menu item (#75).
	 *
	 * @return the human-readable display name of the element.
	 */
	public String tooltip() {

		return tooltip;
	} // end of tooltip method

	/**
	 * The help topic documenting this element.
	 *
	 * @return a topic id present in {@code help/Map.jhm}.
	 */
	public String helpTopic() {

		return helpTopic;
	} // end of helpTopic method

} // end of PaletteEntry class
