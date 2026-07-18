package jls.elem;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;

import jls.Circuit;

/**
 * The core descriptor for one loadable element type (issue #78): its
 * canonical save-file tag, any historical alias tags, the element class,
 * and the factory that creates a blank instance for the loader to fill.
 *
 * This is the core half of the two-layer element descriptor decided on
 * issue #78: {@code ElementType} carries only what loading, saving, and
 * headless tooling (the HDL importer's cell map, #61; the save-format
 * alias table, #79) need. GUI concerns - palette icon, category, help
 * topic, creation dialog - belong to a separate GUI-side palette entry
 * and never appear here.
 *
 * A descriptor's factory replaces the historical
 * {@code Class.forName("jls.elem." + tag)} reflection in
 * {@link jls.Circuit#load(java.util.Scanner)}: the tag is decoupled from
 * the class name (aliases can preserve every historical tag across
 * renames), and a misspelled tag fails as a clean load error instead of
 * a reflection exception. All descriptors live in the
 * {@link ElementRegistry} table, whose integrity test enforces that the
 * table stays total over the loadable element classes.
 */
public final class ElementType {

	/** The canonical tag written after {@code ELEMENT} in save files. */
	private final String tag;

	/** Historical tags that must keep loading as this type (issue #79). */
	private final Set<String> aliases;

	/** The concrete element class this descriptor stands for. */
	private final Class<? extends Element> elementClass;

	/** Creates a blank element for the loader to fill. */
	private final Function<Circuit, Element> factory;

	/**
	 * Create a descriptor for one loadable element type.
	 *
	 * @param tag The canonical save-file tag (written after
	 *            {@code ELEMENT}); must be non-blank.
	 * @param elementClass The concrete element class the factory
	 *            produces.
	 * @param factory Creates a blank element of that class in a given
	 *            circuit, ready for the loader to fill.
	 * @param aliases Historical tags that must keep resolving to this
	 *            type; each must be non-blank and distinct from the tag.
	 */
	public ElementType(String tag, Class<? extends Element> elementClass,
			Function<Circuit, Element> factory, String... aliases) {

		if (tag == null || tag.isBlank()) {
			throw new IllegalArgumentException("blank element tag");
		}
		if (elementClass == null) {
			throw new IllegalArgumentException(
					"no element class for tag " + tag);
		}
		if (factory == null) {
			throw new IllegalArgumentException("no factory for tag " + tag);
		}
		this.tag = tag;
		this.elementClass = elementClass;
		this.factory = factory;
		Set<String> all = new LinkedHashSet<>();
		for (String alias : aliases) {
			if (alias == null || alias.isBlank()) {
				throw new IllegalArgumentException(
						"blank alias for tag " + tag);
			}
			if (alias.equals(tag) || !all.add(alias)) {
				throw new IllegalArgumentException(
						"duplicate alias '" + alias + "' for tag " + tag);
			}
		}
		this.aliases = Collections.unmodifiableSet(all);
	} // end of constructor

	/**
	 * The canonical save-file tag.
	 *
	 * @return the tag written after {@code ELEMENT} in save files.
	 */
	public String tag() {

		return tag;
	} // end of tag method

	/**
	 * The historical alias tags, in declaration order.
	 *
	 * @return an unmodifiable set of aliases; empty when the type has
	 *         never been renamed.
	 */
	public Set<String> aliases() {

		return aliases;
	} // end of aliases method

	/**
	 * The concrete element class this descriptor stands for.
	 *
	 * @return the class the factory produces.
	 */
	public Class<? extends Element> elementClass() {

		return elementClass;
	} // end of elementClass method

	/**
	 * Create a blank element of this type for the loader to fill.
	 *
	 * @param circuit The circuit the new element belongs to.
	 *
	 * @return the new, unloaded element.
	 */
	public Element create(Circuit circuit) {

		return factory.apply(circuit);
	} // end of create method

} // end of ElementType class
