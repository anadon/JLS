package jls.elem;

import jls.*;
import java.util.BitSet;

/**
 * n-input AND gate(s).
 *
 * @author David A. Poplawski
 */
public final class AndGate extends Gate implements Timed {

	// identity and shared previous-settings state (#22)
	/** The kind descriptor and shared previous-settings state for AND gates. */
	private static final Kind KIND =
			new Kind("AND","AndGate",-1,10);

	/**
	 * Create AND gate.
	 *
	 * @param circuit The circuit this AND gate is in.
	 */
	public AndGate(Circuit circuit) {

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
	 * The AND gate body: a flat back and side with a semicircular front
	 * (issue #77 model/render split - the symbol as headless data).
	 */
	@Override
	protected GateOutline outline() {

		int s = JLSInfo.spacing;
		return GateOutline.builder()
				.line(false, s, 0, 0, 0)
				.line(true, 0, 0, 0, s * 2)
				.line(true, 0, s * 2, s, s * 2)
				.arc(true, 0, 0, s * 2, s * 2, -90, 180)
				.close()
				.build();
	} // end of outline method

//-------------------------------------------------------------------------------
// Simulation
//-------------------------------------------------------------------------------

	/**
	 * AND the input bits (absent inputs count as 0).
	 */
	@Override
	protected BitSet computeOutput() {

		BitSet value = new BitSet(bits);
		value.set(0,bits);
		for (Input input : inputs) {
			BitSet inVal = input.getValue();
			if (inVal == null)
				inVal = new BitSet();
			value.and(inVal);
		}
		return value;
	} // end of computeOutput method


} // end of AndGate class
