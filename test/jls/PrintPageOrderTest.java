package jls;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.junit.jupiter.api.Test;

import jls.edit.CircuitRenderer;
import jls.elem.Element;
import jls.elem.StateMachine;
import jls.elem.TruthTable;

/**
 * Canonical printed page order (issue #182). Circuit.addToBook used to
 * iterate the element HashSet in its three page-collection passes, so
 * two prints of one circuit could order the state-machine, truth-table,
 * and subcircuit pages differently between runs. The pages must follow
 * the circuit's canonical stable-id order
 * ({@link Circuit#getElementsInStableOrder()}) - the same order the
 * canonical save uses - making the page sequence a pure function of
 * circuit content.
 */
class PrintPageOrderTest {

	private static Circuit load() throws Exception {
		CircuitTextBuilder cb = new CircuitTextBuilder();
		cb.stateMachine(1, 2);
		cb.stateMachine(2, 2);
		cb.stateMachine(3, 2);
		cb.truthTable("ttA", new String[] {"a"}, new String[] {"z"},
				new int[][] {{0, 1}, {1, 0}});
		cb.truthTable("ttB", new String[] {"b"}, new String[] {"y"},
				new int[][] {{0, 0}, {1, 1}});

		Circuit circuit = new Circuit("pageorder");
		assertTrue(circuit.load(new Scanner(cb.build())),
				() -> "load failed: " + JLSInfo.loadError);
		assertTrue(circuit.finishLoad(null),
				() -> "finishLoad failed: " + JLSInfo.loadError);
		return circuit;
	}

	@Test
	void bookedPagesFollowStableIdOrder() throws Exception {
		Circuit circuit = load();
		Book book = new Book();
		CircuitRenderer.of(circuit).addToBook(book, new PageFormat());

		List<Printable> pages = new ArrayList<Printable>();
		for (int i = 0; i < book.getNumberOfPages(); i++) {
			pages.add(book.getPrintable(i));
		}
		assertTrue(pages.get(0) instanceof CircuitRenderer,
				"the circuit's own page (its renderer) must come first");

		// the state-machine pages, in booked order, must be the state
		// machines in canonical stable-id order; likewise truth tables
		List<Element> expectedMachines = new ArrayList<Element>();
		List<Element> expectedTables = new ArrayList<Element>();
		for (Element el : circuit.getElementsInStableOrder()) {
			if (el instanceof StateMachine) {
				expectedMachines.add(el);
			} else if (el instanceof TruthTable) {
				expectedTables.add(el);
			}
		}
		assertEquals(3, expectedMachines.size(),
				"the fixture must contain three state machines");
		assertEquals(2, expectedTables.size(),
				"the fixture must contain two truth tables");

		// the state-machine diagram page moved off the model (issue #77):
		// each machine now books a StateMachineRenderer.DiagramPage that
		// carries its machine, so the order check reads the machine back
		// off the page.
		List<Element> bookedMachines = new ArrayList<Element>();
		// truth tables and state-machine diagrams are booked as wrapper
		// pages now (issue #77): neither element implements Printable, so
		// the booked order is checked against the wrapped elements.
		List<Element> bookedTables = new ArrayList<Element>();
		for (Printable page : pages) {
			if (page instanceof jls.edit.StateMachineRenderer.DiagramPage) {
				bookedMachines.add(
						((jls.edit.StateMachineRenderer.DiagramPage) page)
								.machine());
			} else if (page instanceof jls.edit.TruthTablePrintable ttp) {
				bookedTables.add(ttp.getTruthTable());
			}
		}
		assertEquals(expectedMachines, bookedMachines,
				"state-machine pages must be booked in stable-id order");
		assertEquals(expectedTables, bookedTables,
				"truth-table pages must be booked in stable-id order");
	}
}
