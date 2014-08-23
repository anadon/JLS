package jls.elem;

import java.math.*;
import java.util.BitSet;
import java.util.Scanner;

import javax.swing.JOptionPane;

import jls.*;
import jls.sim.*;

public abstract class SigSim extends LogicElement {

	/**
	 * Create new element.
	 * 
	 * @param circuit The circuit this element is in.
	 */
	public SigSim(Circuit circuit) {

		super(circuit);
	} // end of constructor

	//	-------------------------------------------------------------------------------
	//	Simulation
	//	-------------------------------------------------------------------------------

	/**
	 * Parse signal specification and post all events.
	 * If signal generator is in an imported circuit, do nothing.
	 * 
	 * @param sim The simulator.
	 * @param input A scanner for reading the specification.
	 */
	public void initSim(Simulator sim, Scanner input) {

		// get rid of end-of-line comments
		String newSignals = "";
		while (input.hasNextLine()) {
			String line = input.nextLine();
			String newLine = "";

			// convert hex numbers to base 10
			Scanner hex = new Scanner(line);
			while (hex.hasNext()) {
				String token = hex.next();
				if (token.matches("-?0[xX][0-9a-fA-F]+")) {
					BigInteger value;
					String suffix = token.substring(2);
					if (token.charAt(0) == '-') {
						suffix = token.substring(3);
						value = new BigInteger(suffix,16).negate();
					}
					else {
						suffix = token.substring(2);
						value = new BigInteger(suffix,16);
					}
					
					newLine += " " + value;
				}
				else {
					newLine += " " + token;
				}
			}
			if (newLine.contains("#")) {
				newSignals += newLine.substring(0,newLine.indexOf("#")) + " ";
			}
			else {
				newSignals += newLine + " ";
			}
		}

		// read signal string
		input = new Scanner(newSignals);
		while (input.hasNext()) {

			// get signal name and resolve to input pin
			String signal = input.next();
			InputPin pin = null;
			for (Element el : circuit.getElements()) {
				if (!(el instanceof InputPin)) 
					continue;
				if (signal.equals(el.getName())) {
					pin = (InputPin)el;
				}
			}
			if (pin == null) {
				specError("no input pin for signal " + signal + " - signal ignored");
			}

			// get initial value
			if (!input.hasNextBigInteger()) {
				specError("missing or invalid initial value for signal " + signal);
				return;
			}
			BigInteger value = input.nextBigInteger();

			// post initial event to input pin
			if (pin != null) {

				// make sure the value will fit
				int bits = pin.getBits();
				if (value.signum() < 0) {
					if (value.bitLength()+1 > bits) {
						specError("value " + value + " will not fit in signal " + signal);
						return;
					}
				}
				else {
					if (value.bitLength() > bits) {
						specError("value " + value + " will not fit in signal " + signal);
						return;
					}
				}

				// convert negative to positive
				if (value.signum() < 0) {
					BigInteger x = new BigInteger("2").pow(bits);
					value = value.add(x);
				}

				// post event
				BitSet bval = BitSetUtils.Create(value);
				sim.post(new SimEvent(0,pin,bval));
			}

			// get the rest of the events for this pin and add to local event list
			long time = 0;
			while (true) {
				long newTime = 0;
				if (!input.hasNext()) {
					specError("expected for, until or end for signal " + signal);
					return;
				}
				String type = input.next();
				if (type.equals("end")) {
					break;
				}
				else if (type.equals("for")) {
					if (!input.hasNextLong()) {
						specError("missing or invalid duration for signal " + signal);
						return;
					}
					newTime = time + input.nextLong();
				}
				else if (type.equals("until")) {
					if (!input.hasNextLong()) {
						specError("missing or invalid until time for signal " + signal);
						return;
					}
					newTime = input.nextLong();
					if (newTime <= time) {
						specError("until time not greater than previous time for signal " + signal);
						return;
					}
				}
				else {
					specError("expected for, until or end for signal " + signal);
					return;
				}
				if (!input.hasNextBigInteger()) {
					specError("expected value for signal " + signal);
					return;
				}
				value = input.nextBigInteger();
				if (pin != null) {

					// make sure the value will fit
					int bits = pin.getBits();
					if (value.signum() < 0) {
						if (value.bitLength()+1 > bits) {
							specError("value " + value + " will not fit in signal " + signal);
							return;
						}
					}
					else {
						if (value.bitLength() > bits) {
							specError("value " + value + " will not fit in signal " + signal);
							return;
						}
					}

					// convert negative to positive
					if (value.signum() < 0) {
						BigInteger x = new BigInteger("2").pow(bits);
						value = value.add(x);
					}

					// post event
					BitSet bval = BitSetUtils.Create(value);
					sim.post(new SimEvent(newTime,pin,bval));
				}

				// update time
				time = newTime;
			}
		}
	} // end of initSim method

	/**
	 * Shouldn't be called.
	 * 
	 * @param now The current simulation time.
	 * @param sim The simulator to post events to.
	 * @param todo The value to send.
	 */
	public void react(long now, Simulator sim, Object todo) {

		throw new UnsupportedOperationException("react in SigGen called");
	} // end of react method

	/**
	 * 
	 */
	protected abstract void specError(String error);

} // end of SigSim method
