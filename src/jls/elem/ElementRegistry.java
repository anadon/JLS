package jls.elem;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The table of every loadable element type (issue #78): one
 * {@link ElementType} descriptor per save-file tag, looked up by
 * {@link jls.Circuit#load(java.util.Scanner)} instead of
 * {@code Class.forName} reflection.
 *
 * Registration is manual (the decision recorded on #78): each descriptor
 * is one line below, and the registry-integrity test
 * ({@code jls.ElementRegistryTest}) enforces totality - every concrete
 * {@link Element} subclass with a public {@code (Circuit)} constructor
 * must be registered, and every registered factory must produce exactly
 * its declared class - so forgetting the line is a build failure, not a
 * "no element type named ..." load error discovered by a user.
 *
 * Tags currently equal the class simple names because that is what every
 * historical save file contains; the alias mechanism
 * ({@link ElementType#aliases()}) exists so a future rename can keep
 * every historical tag loading (issue #79's alias table).
 */
public final class ElementRegistry {

	/**
	 * Every loadable element type, in palette-independent alphabetical
	 * order. {@code TestGen} is included even though JLS never saves one:
	 * it is the batch-mode stand-in for a signal generator, and the
	 * pre-registry loader accepted a hand-written {@code ELEMENT TestGen},
	 * so the registry keeps that file loadable.
	 */
	private static final List<ElementType> ALL = List.of(
			new ElementType("Adder", Adder.class, Adder::new),
			new ElementType("AndGate", AndGate.class, AndGate::new),
			new ElementType("Binder", Binder.class, Binder::new),
			new ElementType("Clock", Clock.class, Clock::new),
			new ElementType("Constant", Constant.class, Constant::new),
			new ElementType("Decoder", Decoder.class, Decoder::new),
			new ElementType("DelayGate", DelayGate.class, DelayGate::new),
			new ElementType("Display", Display.class, Display::new),
			new ElementType("Extend", Extend.class, Extend::new),
			new ElementType("InputPin", InputPin.class, InputPin::new),
			new ElementType("JumpEnd", JumpEnd.class, JumpEnd::new),
			new ElementType("JumpStart", JumpStart.class, JumpStart::new),
			new ElementType("Memory", Memory.class, Memory::new),
			new ElementType("Mux", Mux.class, Mux::new),
			new ElementType("NandGate", NandGate.class, NandGate::new),
			new ElementType("NorGate", NorGate.class, NorGate::new),
			new ElementType("NotGate", NotGate.class, NotGate::new),
			new ElementType("OrGate", OrGate.class, OrGate::new),
			new ElementType("OutputPin", OutputPin.class, OutputPin::new),
			new ElementType("Pause", Pause.class, Pause::new),
			new ElementType("Register", Register.class, Register::new),
			new ElementType("ShiftRegister", ShiftRegister.class,
					ShiftRegister::new),
			new ElementType("SigGen", SigGen.class, SigGen::new),
			new ElementType("Splitter", Splitter.class, Splitter::new),
			new ElementType("StateMachine", StateMachine.class,
					StateMachine::new),
			new ElementType("Stop", Stop.class, Stop::new),
			new ElementType("SubCircuit", SubCircuit.class, SubCircuit::new),
			new ElementType("TestGen", TestGen.class, TestGen::new),
			new ElementType("Text", Text.class, Text::new),
			new ElementType("TriState", TriState.class, TriState::new),
			new ElementType("TruthTable", TruthTable.class, TruthTable::new),
			new ElementType("WireEnd", WireEnd.class, WireEnd::new),
			new ElementType("XorGate", XorGate.class, XorGate::new));

	/** Lookup by canonical tag and by every alias. */
	private static final Map<String, ElementType> BY_TAG = index();

	/**
	 * Build the tag/alias lookup map, refusing duplicates.
	 *
	 * @return an unmodifiable map from every tag and alias to its
	 *         descriptor.
	 */
	private static Map<String, ElementType> index() {

		Map<String, ElementType> map = new LinkedHashMap<>();
		for (ElementType type : ALL) {
			if (map.put(type.tag(), type) != null) {
				throw new IllegalStateException(
						"duplicate element tag " + type.tag());
			}
			for (String alias : type.aliases()) {
				if (map.put(alias, type) != null) {
					throw new IllegalStateException(
							"duplicate element tag " + alias);
				}
			}
		}
		return Collections.unmodifiableMap(map);
	} // end of index method

	/**
	 * This class is a static table; it is never instantiated.
	 */
	private ElementRegistry() {

	} // end of constructor

	/**
	 * Look up the element type a save-file tag names.
	 *
	 * @param tag The tag read after {@code ELEMENT} in a save file (or an
	 *            alias of one).
	 *
	 * @return the descriptor, or null if no element type has that tag.
	 */
	public static ElementType forTag(String tag) {

		return BY_TAG.get(tag);
	} // end of forTag method

	/**
	 * Every registered element type, one descriptor each (aliases do not
	 * repeat a descriptor).
	 *
	 * @return the descriptors in registration order, unmodifiable.
	 */
	public static List<ElementType> all() {

		return ALL;
	} // end of all method

} // end of ElementRegistry class
