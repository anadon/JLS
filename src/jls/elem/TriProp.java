package jls.elem;

/**
 * Elements that are able to propagate a tri-state value must implement this.
 */
public interface TriProp {
	
	/**
	 * Set this element to tri-state or not and propagate to output(s).
	 * 
	 * @param which True to set to tri-state, false otherwise.
	 */
	public void setTriState(boolean which);

} // end of TriProp interface
