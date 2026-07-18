package jls.elem;

import java.util.Map;
import java.util.Set;

/**
 * The save-format element tag namespace (issue #79).
 *
 * <p>A {@code .jls} save names each element's type with a tag token
 * ({@code ELEMENT <tag>}). Historically the loader resolved a tag by
 * reflection against a hardcoded package
 * ({@code Class.forName("jls.elem." + tag)}), which made every element
 * class name silently load-bearing format data: renaming a class was a
 * save-format break that no compiler and no test caught, and any class
 * in the package - element or not - was reachable from file content.
 * This table decouples tag from class. The canonical tags are frozen
 * data: they coincide with today's simple class names because that is
 * what every historical file contains, and they never change again.
 * The classes they map to are ordinary compile-time references, so a
 * class rename now breaks this table's compilation instead of the
 * format, and is fixed by re-pointing the mapping - the tag itself
 * stays frozen, which is why a rename needs no format-version bump
 * (no old reader ever sees a new tag).</p>
 *
 * <p>{@code docs/file-format.md} section 7 is the normative statement
 * of this namespace; {@code FileFormatSpecTest} keeps the spec's tag
 * table, the writers' actual output, and this class in lock-step.
 * When the element registry (issue #78) lands, this table is the
 * save-format column it absorbs.</p>
 */
public final class SaveTags {

	/**
	 * The canonical writable tags: exactly the tags a conformant
	 * writer may emit, one per savable element type (spec section 7).
	 * Keys are frozen format data; values are compile-time class
	 * references.
	 */
	private static final Map<String, Class<? extends Element>> WRITABLE =
			Map.ofEntries(
					Map.entry("Adder", Adder.class),
					Map.entry("AndGate", AndGate.class),
					Map.entry("Binder", Binder.class),
					Map.entry("Clock", Clock.class),
					Map.entry("Constant", Constant.class),
					Map.entry("Decoder", Decoder.class),
					Map.entry("DelayGate", DelayGate.class),
					Map.entry("Display", Display.class),
					Map.entry("Extend", Extend.class),
					Map.entry("InputPin", InputPin.class),
					Map.entry("JumpEnd", JumpEnd.class),
					Map.entry("JumpStart", JumpStart.class),
					Map.entry("Memory", Memory.class),
					Map.entry("Mux", Mux.class),
					Map.entry("NandGate", NandGate.class),
					Map.entry("NorGate", NorGate.class),
					Map.entry("NotGate", NotGate.class),
					Map.entry("OrGate", OrGate.class),
					Map.entry("OutputPin", OutputPin.class),
					Map.entry("Pause", Pause.class),
					Map.entry("Register", Register.class),
					Map.entry("ShiftRegister", ShiftRegister.class),
					Map.entry("SigGen", SigGen.class),
					Map.entry("Splitter", Splitter.class),
					Map.entry("StateMachine", StateMachine.class),
					Map.entry("Stop", Stop.class),
					Map.entry("SubCircuit", SubCircuit.class),
					Map.entry("Text", Text.class),
					Map.entry("TriState", TriState.class),
					Map.entry("TruthTable", TruthTable.class),
					Map.entry("WireEnd", WireEnd.class),
					Map.entry("XorGate", XorGate.class));

	/**
	 * Tags a reader accepts but a conformant writer never emits
	 * (loadable-but-not-writable, the issue's "synthetic" category).
	 * {@code TestGen} exists for the batch test facility only.
	 */
	private static final Map<String, Class<? extends Element>>
			LOADABLE_ONLY = Map.of("TestGen", TestGen.class);

	/**
	 * Alias tag &rarr; canonical tag. An alias is added when a class
	 * whose old simple name was itself once written as a tag is
	 * renamed, so files written in the interim keep loading. Empty
	 * today: no persisted type has ever been renamed (the
	 * {@code edu.mtu.cs.jls} &rarr; {@code jls} move kept every simple
	 * name). Alias keys must never collide with canonical tags;
	 * {@code SaveTagsTest} enforces that.
	 */
	private static final Map<String, String> ALIASES = Map.of();

	/**
	 * This class is a static table; it is never instantiated.
	 */
	private SaveTags() {
	} // end of SaveTags constructor

	/**
	 * Resolve a save-file element tag to the element class that reads
	 * it, applying the alias table first.
	 *
	 * @param tag The tag token following ELEMENT in a save file.
	 *
	 * @return the element class, or null if the tag is not part of the
	 *         save format (the caller owns the user-facing diagnostic).
	 */
	public static Class<? extends Element> resolve(String tag) {

		String canonical = ALIASES.getOrDefault(tag, tag);
		Class<? extends Element> type = WRITABLE.get(canonical);
		if (type != null)
			return type;
		return LOADABLE_ONLY.get(canonical);
	} // end of resolve method

	/**
	 * The canonical writable tag set, for tests that hold this table,
	 * the writers, and the normative spec in lock-step.
	 *
	 * @return an unmodifiable set of every tag a conformant writer may
	 *         emit.
	 */
	public static Set<String> writableTags() {

		return WRITABLE.keySet();
	} // end of writableTags method

	/**
	 * The loadable-but-not-writable tag set (spec section 7's
	 * additional notes).
	 *
	 * @return an unmodifiable set of every tag a reader accepts that a
	 *         conformant writer never emits.
	 */
	public static Set<String> loadableOnlyTags() {

		return LOADABLE_ONLY.keySet();
	} // end of loadableOnlyTags method

	/**
	 * The alias table, for tests that check alias hygiene (no
	 * collisions with canonical tags, no dangling targets).
	 *
	 * @return an unmodifiable map from alias tag to canonical tag.
	 */
	public static Map<String, String> aliases() {

		return ALIASES;
	} // end of aliases method

} // end of SaveTags class
