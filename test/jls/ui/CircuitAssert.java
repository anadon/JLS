package jls.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jls.Circuit;
import jls.elem.Element;
import jls.elem.Input;
import jls.elem.JumpEnd;
import jls.elem.JumpStart;
import jls.elem.LogicElement;
import jls.elem.Output;
import jls.elem.Put;
import jls.elem.WireEnd;
import jls.elem.WireNet;

/**
 * Layer-1 (headless, model-level) assertions over {@link Circuit} state:
 * element presence, electrical connectivity, and element characteristics
 * (bit width, watched flag). See {@link jls.ui the package javadoc} for
 * the layering plan (Swing interaction and rendering come later).
 *
 * <p>Connectivity here is <em>net-aware</em>: {@code assertConnected}
 * starts at the source element's output puts, walks every
 * {@link WireNet} reachable through wires, and bridges nets through
 * same-named {@link JumpStart}/{@link JumpEnd} aliases. It is electrical
 * reachability, not logical: it does not pass <em>through</em> gates or
 * other logic elements, and it is directional at the endpoints (source
 * output put to destination input put).</p>
 */
public final class CircuitAssert {

	private CircuitAssert() {
	}

	// ------------------------------------------------------------------
	// element presence
	// ------------------------------------------------------------------

	/**
	 * Assert exactly one element of the given type is present and return
	 * it. Fails on zero matches (absent) and on more than one (ambiguous
	 * -- use the named overload or query positionally instead).
	 */
	public static <T extends Element> T assertElementPresent(Circuit circuit,
			Class<T> type) {
		List<T> matches = new ArrayList<T>();
		for (Element el : circuit.getElements()) {
			if (type.isInstance(el)) {
				matches.add(type.cast(el));
			}
		}
		if (matches.isEmpty()) {
			fail("no " + type.getSimpleName() + " present in circuit \""
					+ circuit.getName() + "\"");
		}
		if (matches.size() > 1) {
			fail(matches.size() + " " + type.getSimpleName()
					+ " elements present in circuit \"" + circuit.getName()
					+ "\"; use the (type, name) overload to pick one");
		}
		return matches.get(0);
	}

	/**
	 * Assert an element of the given type with the given name (per
	 * {@link LogicElement#getName()}) is present and return it.
	 */
	public static <T extends LogicElement> T assertElementPresent(
			Circuit circuit, Class<T> type, String name) {
		for (Element el : circuit.getElements()) {
			if (type.isInstance(el)
					&& name.equals(((LogicElement) el).getName())) {
				return type.cast(el);
			}
		}
		fail("no " + type.getSimpleName() + " named \"" + name
				+ "\" present in circuit \"" + circuit.getName() + "\"");
		return null; // unreachable
	}

	// ------------------------------------------------------------------
	// connectivity (relational location)
	// ------------------------------------------------------------------

	/**
	 * Assert an output of {@code source} is electrically connected to an
	 * input of {@code dest}: some wire net reachable from a source output
	 * put -- following wires and bridging same-named jump aliases --
	 * touches an input put of {@code dest}.
	 */
	public static void assertConnected(Circuit circuit, LogicElement source,
			LogicElement dest) {
		assertTrue(reaches(circuit, source, dest),
				describe(source) + " has no output wired (directly or via "
						+ "jump aliases) to an input of " + describe(dest));
	}

	/** The exact negation of {@link #assertConnected}. */
	public static void assertNotConnected(Circuit circuit,
			LogicElement source, LogicElement dest) {
		assertFalse(reaches(circuit, source, dest),
				describe(source) + " has an output wired to an input of "
						+ describe(dest) + " but must not");
	}

	/**
	 * Electrical reachability from {@code source}'s output puts to an
	 * input put of {@code dest}: breadth-first over wire nets, where two
	 * nets are bridged when one touches a {@link JumpStart} or
	 * {@link JumpEnd} and the other touches a jump element with the same
	 * name. Wire nets are built by {@link Circuit#finishLoad}, so this
	 * works on any loaded circuit, headless.
	 */
	private static boolean reaches(Circuit circuit, LogicElement source,
			LogicElement dest) {
		Deque<WireNet> frontier = new ArrayDeque<WireNet>();
		Set<WireNet> seen = new HashSet<WireNet>();
		for (Put put : source.getAllPuts()) {
			if (put instanceof Output && put.isAttached()) {
				enqueue(put.getWireEnd().getNet(), frontier, seen);
			}
		}
		while (!frontier.isEmpty()) {
			WireNet net = frontier.remove();
			for (WireEnd end : net.getAllEnds()) {
				if (!end.isAttached()) {
					continue;
				}
				Put put = end.getPut();
				LogicElement el = put.getElement();
				if (el == dest && put instanceof Input) {
					return true;
				}
				String alias = jumpAlias(el);
				if (alias == null) {
					continue;
				}
				// bridge to every net touching a same-named jump element
				for (Element other : circuit.getElements()) {
					if (other == el || !(other instanceof LogicElement)) {
						continue;
					}
					LogicElement otherLogic = (LogicElement) other;
					if (!alias.equals(jumpAlias(otherLogic))) {
						continue;
					}
					for (Put p : otherLogic.getAllPuts()) {
						if (p.isAttached()) {
							enqueue(p.getWireEnd().getNet(), frontier, seen);
						}
					}
				}
			}
		}
		return false;
	}

	private static String jumpAlias(LogicElement el) {
		if (el instanceof JumpStart) {
			return ((JumpStart) el).getName();
		}
		if (el instanceof JumpEnd) {
			return ((JumpEnd) el).getName();
		}
		return null;
	}

	private static void enqueue(WireNet net, Deque<WireNet> frontier,
			Set<WireNet> seen) {
		if (net != null && seen.add(net)) {
			frontier.add(net);
		}
	}

	// ------------------------------------------------------------------
	// characteristics
	// ------------------------------------------------------------------

	/**
	 * Assert the element's declared bit width (per
	 * {@link LogicElement#getBits()}). Fails with a clear message on
	 * elements that have no single bit width (use
	 * {@link #assertPutBits} for those).
	 */
	public static void assertBits(LogicElement el, int expected) {
		int actual;
		try {
			actual = el.getBits();
		} catch (UnsupportedOperationException noBits) {
			fail(describe(el) + " has no element-level bit width; "
					+ "assert a specific put with assertPutBits instead");
			return; // unreachable
		}
		assertEquals(expected, actual, "bit width of " + describe(el));
	}

	/** Assert the bit width of a named input/output put of the element. */
	public static void assertPutBits(LogicElement el, String putName,
			int expected) {
		Put put = el.getPut(putName);
		if (put == null) {
			fail(describe(el) + " has no put named \"" + putName + "\"");
		}
		assertEquals(expected, put.getBits(),
				"bit width of put \"" + putName + "\" of " + describe(el));
	}

	/** Assert the element's watched flag (per {@link Element#isWatched()}). */
	public static void assertWatched(Element el, boolean expected) {
		assertEquals(expected, el.isWatched(),
				"watched flag of " + describe(el));
	}

	// ------------------------------------------------------------------
	// diagnostics
	// ------------------------------------------------------------------

	/** A human-readable identification of an element for failure messages. */
	static String describe(Element el) {
		StringBuilder sb = new StringBuilder(el.getClass().getSimpleName());
		if (el instanceof LogicElement) {
			String name = ((LogicElement) el).getName();
			if (name != null && !name.isEmpty()) {
				sb.append(" \"").append(name).append('"');
			}
		}
		sb.append(" at (").append(el.getX()).append(',').append(el.getY())
				.append(')');
		return sb.toString();
	}
}
