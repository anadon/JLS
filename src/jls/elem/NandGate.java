package jls.elem;

import jls.*;
import java.util.BitSet;

/**
 * n-input NAND gate(s).
 *
 * @author David A. Poplawski
 */
public final class NandGate extends Gate implements Timed {

	// identity and shared previous-settings state (#22)
	/** The kind descriptor for NAND gates (identity and shared previous settings). */
	private static final Kind KIND =
			new Kind("NAND","NandGate",-1,5);

	/**
	 * Create NAND gate
	 *
	 * @param circuit The circuit this AND gate is in.
	 */
	public NandGate(Circuit circuit) {

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
	 * The NAND gate body: the AND outline with an inversion bubble at the
	 * front (issue #77 model/render split - the symbol as headless data).
	 */
	@Override
	protected GateOutline outline() {

		int s = JLSInfo.spacing;
		int d = JLSInfo.pointDiameter;
		return GateOutline.builder()
				.line(false, s / 2, 0, 0, 0)
				.line(true, 0, 0, 0, s * 2)
				.line(true, 0, s * 2, s / 2, s * 2)
				.arc(true, -s / 2, 0, s * 2, s * 2, -90, 180)
				.close()
				.ellipse(false, s + d, s - d / 2, d, d)
				.build();
	} // end of outline method

//-------------------------------------------------------------------------------
// Simulation
//-------------------------------------------------------------------------------

	/**
	 * NAND the input bits (absent inputs count as 0).
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
		value.flip(0,bits);
		return value;
	} // end of computeOutput method


} // end of NandGate class
