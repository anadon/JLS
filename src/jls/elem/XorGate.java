package jls.elem;

import jls.*;
import java.util.BitSet;

/**
 * n-input XOR gate(s).
 *
 * @author David A. Poplawski
 */
public final class XorGate extends Gate implements Timed {

	// identity and shared previous-settings state (#22)
	/** The kind descriptor and shared previous-settings state for XOR gates. */
	private static final Kind KIND =
			new Kind("XOR","XorGate",-1,10);

	/**
	 * Create XOR gate.
	 *
	 * @param circuit The circuit this XOR gate is in.
	 */
	public XorGate(Circuit circuit) {

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
	 * The XOR gate body: the OR outline shifted right by the gap, plus the
	 * second (back) curve of the double-curved XOR back (issue #77
	 * model/render split - the symbol as headless data). The curves
	 * reproduce the former inline construction with gap=2, from the points
	 * pright=(2s,s), ptop=(gap,0), pbottom=(gap,2s) and the back curve
	 * from pptop=(0,0), ppbottom=(0,2s).
	 */
	@Override
	protected GateOutline outline() {

		int s = JLSInfo.spacing;
		int gap = 2;
		return GateOutline.builder()
				.cubic(false, 2 * s, s, 2 * s - 1, s - 3, gap + s, 0, gap, 0)
				.cubic(true, gap, 0, gap + s / 4, s, gap + s / 4, s,
						gap, 2 * s)
				.cubic(true, gap, 2 * s, gap + s, 2 * s, 2 * s - 1, s + 3,
						2 * s, s)
				.close()
				.cubic(false, 0, 0, s / 4, s, s / 4, s, 0, 2 * s)
				.build();
	} // end of outline method

//-------------------------------------------------------------------------------
// Simulation
//-------------------------------------------------------------------------------

	/**
	 * XOR the input bits (absent inputs count as 0).
	 */
	@Override
	protected BitSet computeOutput() {

		BitSet value = new BitSet(bits);
		for (Input input : inputs) {
			BitSet inVal = input.getValue();
			if (inVal == null)
				inVal = new BitSet();
			value.xor(inVal);
		}
		return value;
	} // end of computeOutput method


} // end of XorGate class
