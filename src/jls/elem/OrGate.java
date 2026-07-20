package jls.elem;

import jls.*;
import java.util.BitSet;

/**
 * n-input OR gate(s).
 *
 * @author David A. Poplawski
 */
public final class OrGate extends Gate implements Timed {

	// identity and shared previous-settings state (#22)
	/** The kind descriptor shared by all OR gates (#22). */
	private static final Kind KIND =
			new Kind("OR","OrGate",-1,10);

	/**
	 * Create OR gate.
	 *
	 * @param circuit The circuit this OR gate is in.
	 */
	public OrGate(Circuit circuit) {

		super(circuit);
	} // end of constructor

	/**
	 * The kind descriptor for this gate.
	 */
	@Override
	protected Kind kind() {

		return KIND;
	} // end of kind method

	/**
	 * The OR gate body: a curved back and two convex sides meeting at a
	 * point (issue #77 model/render split - the symbol as headless data).
	 * The three cubic curves reproduce the former inline construction from
	 * the points pright=(2s,s), ptop=(0,0), pbottom=(0,2s).
	 */
	@Override
	protected GateOutline outline() {

		int s = JLSInfo.spacing;
		return GateOutline.builder()
				.cubic(false, 2 * s, s, 2 * s - 1, s - 3, s, 0, 0, 0)
				.cubic(true, 0, 0, s / 4, s, s / 4, s, 0, 2 * s)
				.cubic(true, 0, 2 * s, s, 2 * s, 2 * s - 1, s + 3, 2 * s, s)
				.close()
				.build();
	} // end of outline method

//-------------------------------------------------------------------------------
// Simulation
//-------------------------------------------------------------------------------

	/**
	 * OR the input bits (absent inputs count as 0).
	 */
	@Override
	protected BitSet computeOutput() {

		BitSet value = new BitSet(bits);
		for (Input input : inputs) {
			BitSet inVal = input.getValue();
			if (inVal == null)
				inVal = new BitSet();
			value.or(inVal);
		}
		return value;
	} // end of computeOutput method


} // end of OrGate class
