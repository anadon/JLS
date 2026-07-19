package jls.collab.op;

import java.util.Set;

/**
 * The closed element-type vocabulary for network-delivered payloads
 * (issue #170): the exact set of type tokens a collaboration snapshot
 * or op may name, checked <em>before</em> any token reaches the
 * reflective instantiation in {@code Circuit.load}. The local file
 * loader stays as it is - its package-prefix scoping plus the
 * {@code Element} subclass and {@code (Circuit)} constructor checks
 * are an acceptable gate for files the user chose to open - but a
 * session peer is not the user: every byte from the network must mean
 * a validated circuit operation or a typed rejection, so class
 * selection from a peer-controlled string is confined to this list.
 *
 * The list is exactly the tokens the save-format writers produce:
 * every palette-creatable element (the set pinned against the help
 * system by {@code HelpTopicsTest}) plus {@code WireEnd}, the one
 * non-palette element that appears in save files (wires are rebuilt
 * from WireEnd references; {@code Wire} itself is never an
 * {@code ELEMENT} token). {@code ElementVocabularyTest} mechanically
 * cross-checks all three views: this list, the writer literals in
 * {@code jls.elem}, and the palette contract.
 *
 * This is the stopgap constant list the issue #170 plan sanctions
 * until the element registry (issue #78) exists; when the registry
 * lands, this class should delegate to it and the reconciliation is
 * to be recorded on issue #78.
 */
public final class ElementVocabulary {

	/**
	 * Every legal element type token, exactly as written after
	 * {@code ELEMENT} in the save format. Additions require a palette
	 * element with the save-format writer and help topic to match -
	 * {@code ElementVocabularyTest} fails until all three agree.
	 */
	private static final Set<String> ALLOWED = Set.of(
			"Adder", "AndGate", "Binder", "Clock", "Constant",
			"Decoder", "DelayGate", "Display", "Extend", "InputPin",
			"JumpEnd", "JumpStart", "Memory", "Mux", "NandGate",
			"NorGate", "NotGate", "OrGate", "OutputPin", "Pause",
			"Register", "ShiftRegister", "SigGen", "Splitter",
			"StateMachine", "Stop", "SubCircuit", "Text", "TriState",
			"TruthTable", "WireEnd", "XorGate");

	/**
	 * Not instantiable - this class is only a vocabulary of static members.
	 */
	private ElementVocabulary() {

	} // end of constructor

	/**
	 * The whole vocabulary, for tests and registry reconciliation.
	 *
	 * @return the immutable set of legal element type tokens.
	 */
	public static Set<String> allowedTypes() {

		return ALLOWED;
	} // end of allowedTypes method

	/**
	 * Whether a type token is in the vocabulary.
	 *
	 * @param type The token to check; null is simply not allowed.
	 *
	 * @return true exactly when the token may reach instantiation.
	 */
	public static boolean isAllowed(String type) {

		return type != null && ALLOWED.contains(type);
	} // end of isAllowed method

	/**
	 * Gate a network-delivered type token: every payload path that
	 * leads to element instantiation must pass its token through here
	 * first, so a hostile token dies as a typed rejection before any
	 * class lookup happens.
	 *
	 * @param type The peer-supplied token.
	 *
	 * @throws OpRejected if the token is not in the vocabulary.
	 */
	public static void requireAllowed(String type) throws OpRejected {

		if (!isAllowed(type)) {
			throw new OpRejected("'" + clip(type)
					+ "' is not a collaboration element type");
		}
	} // end of requireAllowed method

	/**
	 * Clip untrusted text for an error message.
	 *
	 * @param text The text, possibly null.
	 *
	 * @return at most 60 characters of it.
	 */
	private static String clip(String text) {

		if (text == null) {
			return "(no type)";
		}
		return text.length() <= 60 ? text : text.substring(0, 60) + "…";
	} // end of clip method

} // end of ElementVocabulary class
