package jls.elem;

import jls.*;
import java.util.BitSet;

/**
 * Not gate(s) (inverter).
 *
 * @author David A. Poplawski
 */
public final class NotGate extends Gate implements Timed {

	// identity and shared previous-settings state (#22)
	/** Kind descriptor for NOT gates: display name "NOT", save tag "NotGate", one fixed input, default delay 5. */
	private static final Kind KIND =
			new Kind("NOT","NotGate",1,5);

	/**
	 * Create NOT gate.
	 *
	 * @param circuit The circuit this NOT gate is in.
	 */
	public NotGate(Circuit circuit) {

		super(circuit);
	} // end of construcor

	/**
	 * The kind descriptor for this gate.
	 */
	@Override
	protected Kind kind() {

		return KIND;
	} // end of kind method

	/**
	 * The NOT gate body: a triangle with an inversion bubble at its tip
	 * (issue #77 model/render split - the symbol as headless data).
	 */
	@Override
	protected GateOutline outline() {

		int s = JLSInfo.spacing;
		int d = JLSInfo.pointDiameter;
		return GateOutline.builder()
				.line(false, 0, 0, 2 * s - d, s)
				.line(true, 2 * s - d, s, 0, 2 * s)
				.line(true, 0, 2 * s, 0, 0)
				.close()
				.ellipse(false, 2 * s - d, s - d / 2, d, d)
				.build();
	} // end of outline method

//-------------------------------------------------------------------------------
// Simulation
//-------------------------------------------------------------------------------

	/**
	 * NOT the input bits (an absent input counts as 0).
	 */
	@Override
	protected BitSet computeOutput() {

		BitSet value = inputs.get(0).getValue();
		if (value == null)
			value = new BitSet();
		else
			value = (BitSet)value.clone();
		value.flip(0,bits);
		return value;
	} // end of computeOutput method


} // end of NotGate class
