package jls.elem;

import jls.*;
import java.util.BitSet;

/**
 * n-input NOR gate(s).
 *
 * @author David A. Poplawski
 */
public final class NorGate extends Gate implements Timed {

	// identity and shared previous-settings state (#22)
	/** This gate type's identity and shared previous-settings state (#22). */
	private static final Kind KIND =
			new Kind("NOR","NorGate",-1,5);

	/**
	 * Create NOR gate.
	 *
	 * @param circuit The circuit this NOR gate is in.
	 */
	public NorGate(Circuit circuit) {

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
	 * The NOR gate body: the OR outline with an inversion bubble at the
	 * front (issue #77 model/render split - the symbol as headless data).
	 * The cubic curves reproduce the former inline construction from the
	 * points pright=(2s-d,s), ptop=(0,0), pbottom=(0,2s).
	 */
	@Override
	protected GateOutline outline() {

		int s = JLSInfo.spacing;
		int d = JLSInfo.pointDiameter;
		return GateOutline.builder()
				.cubic(false, 2 * s - d, s, 2 * s - d - 1, s - 3, s, 0, 0, 0)
				.cubic(true, 0, 0, s / 4, s, s / 4, s, 0, 2 * s)
				.cubic(true, 0, 2 * s, s, 2 * s, 2 * s - d - 1, s + 3,
						2 * s - d, s)
				.close()
				.ellipse(false, 2 * s - d, s - d / 2, d, d)
				.build();
	} // end of outline method

//-------------------------------------------------------------------------------
// Simulation
//-------------------------------------------------------------------------------

	/**
	 * NOR the input bits (absent inputs count as 0).
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
		value.flip(0,bits);
		return value;
	} // end of computeOutput method


} // end of NorGate class
