package jls.edit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jls.elem.ElementRegistry;
import jls.elem.ElementType;

/**
 * The static, ordered palette table (issue #78): every element a user
 * can create from the editor toolbar and "elements" menu, one
 * {@link PaletteEntry} per type, grouped exactly the way the historical
 * hand-coded toolbar grouped them. {@code SimpleEditor.makeElements}
 * generates the toolbar from this table, so adding an element to the
 * palette is one row here instead of a hand-rolled button block - and
 * the authoring contract ({@code PaletteContractTest}) makes a
 * registered element type without a palette row a build failure.
 *
 * The only toolbar control not in this table is the Import button: it
 * shows the menu of open subcircuits instead of placing a registered
 * element type, so it stays hand-coded in
 * {@code SimpleEditor.makeElements} (and {@code SubCircuit} is a
 * documented non-palette type, along with the never-placed
 * {@code WireEnd} and the batch-only {@code TestGen}).
 */
public final class Palette {

	/**
	 * One toolbar group: a run of consecutive palette entries rendered
	 * as a {@code GridLayout} panel of the declared shape, in the order
	 * the groups appear on the toolbar. The standalone group's buttons
	 * sit directly on the toolbar with no panel around them, preserving
	 * the historical layout of the lone annotation-text button.
	 */
	public enum Group {

		/** Logic gates, from AND through the tri-state gate. */
		GATES(2, 4, false),

		/** Wiring aids: named wires, pins, bundling, constants. */
		WIRE_WORKS(2, 5, false),

		/** Storage elements: register, register file and memory. */
		MEMORY(2, 2, false),

		/** Combinational building blocks and the clock. */
		COMBINATIONAL(2, 3, false),

		/** Simulator control: pause and stop. */
		TIMING(2, 1, false),

		/** Test and observation: signal generator and display. */
		TEST(2, 1, false),

		/** Complex, dialog-driven elements. */
		COMPLEX(2, 1, false),

		/** The standalone annotation-text button. */
		ANNOTATION(1, 1, true);

		/** Rows of the group's GridLayout panel. */
		private final int rows;

		/** Columns of the group's GridLayout panel. */
		private final int cols;

		/** True if the group's buttons sit directly on the toolbar. */
		private final boolean standalone;

		/**
		 * Create a group.
		 *
		 * @param rows Rows of the group's GridLayout panel.
		 * @param cols Columns of the group's GridLayout panel.
		 * @param standalone True if the group's buttons sit directly on
		 *            the toolbar instead of inside a panel.
		 */
		Group(int rows, int cols, boolean standalone) {

			this.rows = rows;
			this.cols = cols;
			this.standalone = standalone;
		} // end of constructor

		/**
		 * Rows of the group's GridLayout panel.
		 *
		 * @return the row count.
		 */
		public int rows() {

			return rows;
		} // end of rows method

		/**
		 * Columns of the group's GridLayout panel.
		 *
		 * @return the column count.
		 */
		public int cols() {

			return cols;
		} // end of cols method

		/**
		 * Whether the group's buttons sit directly on the toolbar.
		 *
		 * @return true for the standalone group, false for panel groups.
		 */
		public boolean standalone() {

			return standalone;
		} // end of standalone method

	} // end of Group enum

	/**
	 * Every palette entry, in exact toolbar order (left to right, and
	 * within a group in GridLayout fill order: across the top row, then
	 * across the bottom row). Entries of one group are consecutive.
	 */
	private static final List<PaletteEntry> ENTRIES = List.of(
			entry(Group.GATES, "AndGate", "and", "AND",
					"AND gate", "AND"),
			entry(Group.GATES, "OrGate", "or", "OR",
					"OR gate", "OR"),
			entry(Group.GATES, "NotGate", "not", "NOT",
					"NOT gate", "NOT"),
			entry(Group.GATES, "XorGate", "xor", "XOR",
					"exclusive OR gate", "XOR"),
			entry(Group.GATES, "NandGate", "nand", "NAND",
					"NAND gate", "NAND"),
			entry(Group.GATES, "NorGate", "nor", "NOR",
					"NOR gate", "NOR"),
			entry(Group.GATES, "DelayGate", "delay", "DELAY",
					"user defined signal delay", "DELAY"),
			entry(Group.GATES, "TriState", "tristate", "TriState",
					"tri-state gate", "TRISTATE"),
			entry(Group.WIRE_WORKS, "JumpStart", "jumpstart", "START",
					"name a wire", "start"),
			entry(Group.WIRE_WORKS, "JumpEnd", "jumpend", "END",
					"connect to a named wire", "end"),
			entry(Group.WIRE_WORKS, "InputPin", "ipin", "I-PIN",
					"input pin", "Input"),
			entry(Group.WIRE_WORKS, "OutputPin", "opin", "O-PIN",
					"output pin", "Output"),
			entry(Group.WIRE_WORKS, "Splitter", "split", "SPLIT",
					"unbundle wires", "unbundle"),
			entry(Group.WIRE_WORKS, "Binder", "bind", "BIND",
					"bundle wires", "bundle"),
			entry(Group.WIRE_WORKS, "Constant", "const", "CONST",
					"constant value", "const"),
			entry(Group.WIRE_WORKS, "Extend", "extend", "1-to-N",
					"make N copies of the input", "extend"),
			entry(Group.WIRE_WORKS, "FieldExtend", "fieldextend", "FIELD",
					"sign- or zero-extend a field to a wider bus", "extend"),
			entry(Group.MEMORY, "Register", "register", "REG",
					"register (various triggering)", "register"),
			entry(Group.MEMORY, "RegisterFile", "registerfile", "REGFILE",
					"multi-port register file", "register"),
			entry(Group.MEMORY, "Memory", "memory", "MEMORY",
					"memory, various types", "memory"),
			entry(Group.COMBINATIONAL, "Mux", "mux", "MUX",
					"multiplexor", "mux"),
			entry(Group.COMBINATIONAL, "Decoder", "decoder", "DEC",
					"decoder", "decoder"),
			entry(Group.COMBINATIONAL, "ShiftRegister", "shiftregister",
					"SHIFT", "shift register (combinational shifter)",
					"shiftregister"),
			entry(Group.COMBINATIONAL, "Adder", "adder", "ADDER",
					"adder", "adder"),
			entry(Group.COMBINATIONAL, "Clock", "clock", "CLOCK",
					"clock", "clock"),
			entry(Group.TIMING, "Pause", "pause", "PAUSE",
					"pause simulator when asserted", "pause"),
			entry(Group.TIMING, "Stop", "stop", "STOP",
					"stop simulator when asserted", "stop"),
			entry(Group.TEST, "SigGen", "siggen", "SIGGEN",
					"generate test signals", "siggen"),
			entry(Group.TEST, "Display", "display", "DISPLAY",
					"display circuit value", "display"),
			entry(Group.COMPLEX, "StateMachine", "statemachine",
					"ST. MAC.", "state machine", "stmach"),
			entry(Group.COMPLEX, "TruthTable", "truth", "Truth Table",
					"truth table", "truth"),
			entry(Group.ANNOTATION, "Text", "text", "TEXT",
					"text (for annotations)", "text"));

	/** The groups in toolbar order. */
	private static final List<Group> GROUPS = List.of(Group.values());

	/**
	 * This class is a static table; it is never instantiated.
	 */
	private Palette() {

	} // end of constructor

	/**
	 * Build one palette entry, resolving its tag through the
	 * {@link ElementRegistry} so a palette row naming an unregistered
	 * element tag fails at class-initialization time (the same guard the
	 * hand-coded toolbar's slug lookup used to apply per button).
	 *
	 * @param group The toolbar group the entry belongs to.
	 * @param tag The canonical registry tag of the element type.
	 * @param iconName Base name of the icon gif.
	 * @param fallbackText Button text shown when the icon is missing.
	 * @param tooltip The tooltip and accessible display name.
	 * @param helpTopic The {@code Map.jhm} topic documenting the element.
	 * @return the new entry.
	 */
	private static PaletteEntry entry(Group group, String tag,
			String iconName, String fallbackText, String tooltip,
			String helpTopic) {

		ElementType type = ElementRegistry.forTag(tag);
		if (type == null) {
			throw new IllegalStateException(
					"palette entry names unregistered element tag " + tag);
		}
		return new PaletteEntry(type, group, iconName, fallbackText,
				tooltip, helpTopic);
	} // end of entry method

	/**
	 * Every palette entry, in exact toolbar order.
	 *
	 * @return the entries, unmodifiable.
	 */
	public static List<PaletteEntry> entries() {

		return ENTRIES;
	} // end of entries method

	/**
	 * The toolbar groups, in toolbar order.
	 *
	 * @return the groups, unmodifiable.
	 */
	public static List<Group> groups() {

		return GROUPS;
	} // end of groups method

	/**
	 * The palette entries of one group, in toolbar order.
	 *
	 * @param group The group to select.
	 * @return that group's entries, unmodifiable.
	 */
	public static List<PaletteEntry> entries(Group group) {

		List<PaletteEntry> selected = new ArrayList<>();
		for (PaletteEntry entry : ENTRIES) {
			if (entry.group() == group) {
				selected.add(entry);
			}
		}
		return Collections.unmodifiableList(selected);
	} // end of entries method

} // end of Palette class
