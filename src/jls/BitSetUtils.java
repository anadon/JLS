package jls;

import java.util.*;
import java.math.*;

/**
 * Common (static) utility methods dealing with BitSets.
 * 
 * @author David A. Poplawski
 */
public final class BitSetUtils {
	
	/**
	 * Private constructor to keep this class from being instantiated.
	 */
	private BitSetUtils() {}
	
	/**
	 * Create a bitset from a positive long.
	 * 
	 * @param value The long value.
	 * 
	 * @return The corresponding BitSet.
	 * 
	 * @throws IllegalArgumentException if value is negative
	 */
	public static BitSet Create(long value) {
		
		if (value < 0)
			throw new
			IllegalArgumentException("value must be greater than zero (" + value + ")");
		BitSet newBS = new BitSet();
		int highBit = (int) Math.ceil(Math.log(value + 1) / Math.log(2));
		for (int index = 0; index < highBit; index += 1) {
			long bit = (long) Math.pow(2, index);
			newBS.set(index, (bit & value) == bit);
		}
		return newBS;
	} // end of Create method
	
	/**
	 * Create a bitset from a positive BigInteger.
	 * 
	 * @param value The BigInteger value.
	 * 
	 * @return The corresponding BitSet.
	 * 
	 * @throws IllegalArgumentException if value is negative
	 */
	public static BitSet Create(BigInteger value) {
		
		if (value.compareTo(BigInteger.ZERO) < 0)
			throw new
			IllegalArgumentException("value must be greater than zero (" + value + ")");
		int numbits = value.bitLength()+1;
		BitSet newBS = new BitSet(numbits);
		for (int index = 0; index < numbits; index += 1) {
			if (value.testBit(index)) {
				newBS.set(index);
			}
		}
		return newBS;
	} // end of Create method
	
	 /**
     * Convert a BitSet to a string representation in the specified radix.
     * 
     * @param bs The BitSet to convert.
     * @param radix The radix to convert the BitSet to.
     * 
     * @return A string representation of the BitSet in the specified radix.
     */
    public static String ToString(BitSet bs, int radix) {

        BigInteger bi = BigInteger.ZERO;
        
        for (int index = bs.nextSetBit(0); index >= 0;
		index = bs.nextSetBit(index + 1)) {
            bi = bi.setBit(index);
        }
        
        return bi.toString(radix).toUpperCase();
    } // end of ToString method

	/**
    * Convert a BitSet to a string representation in base 10 assuming two's complement.
    * 
    * @param bs The BitSet to convert.
    * @param bits The number of bits.
    * 
    * @return A string representation of the BitSet.
    */
   public static String ToStringSigned(BitSet bs, int bits) {

	   // if bits = 0, can't do signed converstion
	   if (bits == 0)
		   return "unknown";
	   
	   // if positive do normal conversion
	   if (!bs.get(bits-1))
		   return ToString(bs,10);
	   
	   // flip the bits
	   BitSet temp = (BitSet)bs.clone();
	   temp.flip(0,bits);
	   long val = ToLong(temp)+1;
	   return "-" + Long.toString(val);
   } // end of ToStringSigned method
    
    /**
     * Convert a bitset to an int.
     * 
     * @param bs The bitset.
     * 
     * @return the corresponding int.
     */
    public static int ToInt(BitSet bs) {
    	
    	int pow = 1;
    	int value = 0;
    	for (int i=0; i<bs.length(); i+=1) {
    		if (bs.get(i))
    			value += pow;
    		pow *= 2;
    	}
    	return value;
    } // end of ToInt method

    /**
     * Convert a bitset to a long.
     * 
     * @param bs The bitset.
     * 
     * @return the corresponding long.
     */
    public static long ToLong(BitSet bs) {
    	
    	long pow = 1;
    	long value = 0;
    	for (int i=0; i<bs.length(); i+=1) {
    		if (bs.get(i))
    			value += pow;
    		pow *= 2;
    	}
    	return value;
    } // end of ToLong method
    
    /**
     * Convert a bitset to a BigInteger.
     * 
     * @param bs The bitset.
     * 
     * @return the corresponding BigInteger.
     */
    public static BigInteger ToBigInteger(BitSet bs) {
    	
    	BigInteger value = BigInteger.ZERO;
    	for (int i=0; i<bs.length(); i+=1) {
    		if (bs.get(i))
    			value = value.setBit(i);
    	}
    	return value;
    } // end of ToBigInteger method
    
    /**
     * Compute the sum of the two input bitsets.
     * 
     * @param carryIn The carry in to the lower order bit position.
     * @param bs1 One operand.
     * @param bs2 Another operand.
     *
     * @return The sum.
     */
    public static BitSet SumCarry(boolean carryIn, BitSet bs1, BitSet bs2) {

            BitSet sum = new BitSet();
            boolean carry = carryIn;
            int size = Math.max(bs1.size(),bs2.size());

            for (int index = 0; index < size; index += 1) {
                boolean bit1 = bs1.get(index);
                boolean bit2 = bs2.get(index);

                if (bit1 && bit2 && carry) {
                    carry = true;
                    sum.set(index, true);
                }
                else if ((bit1 && bit2) || ((bit1 || bit2) && carry)) {
                    carry = true;
                    sum.set(index, false);
                }
                else if (bs1.get(index) || bs2.get(index) || carry) {
                    carry = false;
                    sum.set(index, true);
                }
                else {
                    carry = false;
                    sum.set(index, false);
                }
            }
            if (carry)
                sum.set(size);
            return sum;
        } // end of SumCarry method

	/**
	 * Convert a bitset into displayable values (hex,unsigned,signed).
	 * A null bitset is converted to "HiZ".
	 * 
	 * @param value The BitSet.
	 * @param bits The number of bits in the value.
	 * 
	 * @return A string in the form "0xn (n unsigned, n signed)" or "HiZ".
	 */
	public static String toDisplay(BitSet value, int bits) {
		
		if (value == null)
			return "HiZ";
		String str = "0x" + BitSetUtils.ToString(value,16);
		str += " (" + BitSetUtils.ToString(value,10) + " unsigned, ";
		str += BitSetUtils.ToStringSigned(value,bits) + " signed)";
		return str;
	} // end of toDisplay method
    
} // end of BitSetUtils class
