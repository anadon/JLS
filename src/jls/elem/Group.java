package jls.elem;

import jls.core.Geometry;
import jls.core.Orientation;
import jls.*;

import java.io.*;

import java.util.*;

/**
 * Superclass of binder/splitter.
 * Contains common info and method.
 * 
 * @author David A. Poplawski
 */
public abstract sealed class Group extends LogicElement
		permits Binder, Splitter {
	
	// default values
	/** The bundled-side width a new binder/splitter starts with. */
	private static final int defaultBits = 2;

	// one constraint string, two surfaces: dialog and loader (issue #52)
	/** The bundle-width rule, shared by the dialog and the loader. */
	static final String BITS_CONSTRAINT = "Must be at least 2 bits";
	/** The wire-index rule, shared by the dialog and the loader. */
	static final String INDEX_CONSTRAINT =
			"group wire indices must be non-negative";

	/**
	 * The bundle-width rule, shared by the dialog and the loader.
	 *
	 * @param bits The proposed number of bundled bits.
	 *
	 * @return the violated constraint message, or null if valid.
	 *
	 * @jls.testedby jls.elem.DialogValidationTest#groupBitsRuleIsOneStringOnTwoSurfaces()
	 */
	public static String checkBits(int bits) {

		return bits < 2 ? BITS_CONSTRAINT : null;
	} // end of checkBits method

	// saved properties
	/** The number of bits on the bundled side. */
	protected int bits = defaultBits;
	//protected SortedSet<GetRanges.Entry> ranges;
	/** The bit groups, one entry per put on the narrow side. */
	protected ArrayList<Entry> ranges;
	/** True if the bundled wire is tri-state. */
	protected boolean triState = false;
	/** Which way the element faces (the direction of the bundled side). */
	protected Orientation orientation = Orientation.RIGHT;

	// running properties
	/** True if the save file being loaded says the bundle is tri-state. */
	protected boolean loadTriState = false;
	/** True if loaded from the newer save format that allows non-contiguous bit groups. */
	protected boolean noncontig = false; // False only for legacy saves
	
	/**
	 * Create a new splitter/binder element.
	 * 
	 * @param circuit The circuit this element is part of.
	 */
	public Group(Circuit circuit) {
		super(circuit);
		ranges = new ArrayList<Entry>();
	} // end of constructor
	
	/**
	 * Initialize internal info for this element.
	 * 
	 * @param g The Graphics object to use.
	 */
	@Override
	public void init(java.awt.Graphics g) {

		// no need to do anything if no graphics object
		if (g == null)
			return;

		// do nothing if element already has a size
		if (width != 0 || height != 0)
			return;

		// set up
		java.awt.FontMetrics fm = g.getFontMetrics();
		int s = Geometry.SPACING;
		int puts = ranges.size();

		// the size decomposes into an across axis (from the narrow puts
		// to the bundle side, sized by the widest range label and
		// snapped to the grid) and an along axis (one grid step per
		// put); orientation decides which axis is which (issue #124,
		// re-derived from AmityWilder's vertical-groups branch)
		int across = 0;
		for(Entry e : ranges) {
			across = Math.max(across, fm.stringWidth(e.toCircuitString()));
		}
		across = (across+2*s)/s*s;
		int along = (puts+1)*s;

		if (orientation == Orientation.LEFT
				|| orientation == Orientation.RIGHT) {
			width = across;
			height = along;
		}
		else {
			width = along;
			height = across;
		}

	} // end of init method
	
	/**
	 * The direction this group faces (issue #77: read by the GUI-side
	 * renderer and dialog).
	 *
	 * @return the current orientation.
	 */
	public Orientation getOrientation() {

		return orientation;
	} // end of getOrientation method

	/**
	 * Set the orientation (issue #77: applied by the GUI-side dialog).
	 *
	 * @param orientation The new orientation.
	 */
	public void setOrientation(Orientation orientation) {

		this.orientation = orientation;
	} // end of setOrientation method

	/**
	 * The number of bits on the bundled side (issue #77: read by the
	 * GUI-side dialog).
	 *
	 * @return the number of bundled bits.
	 */
	public int getBits() {

		return bits;
	} // end of getBits method

	/**
	 * Set the number of bundled bits (issue #77: applied by the GUI-side
	 * dialog).
	 *
	 * @param bits The new number of bundled bits.
	 */
	public void setBits(int bits) {

		this.bits = bits;
	} // end of setBits method

	/**
	 * Append a bit-group entry to this group's ranges (issue #77: called
	 * by the GUI-side creation dialog as the user picks bit groups).
	 *
	 * @param entry The bit-group entry to add.
	 */
	public void addRange(Entry entry) {

		ranges.add(entry);
	} // end of addRange method

	/**
	 * Set an int instance variable value (during a load).
	 * 
	 * @param name The instance variable name.
	 * @param value The instance variable value.
	 */
	@Override
	public void setValue(String name, int value) {
		
		if (name.equals("bits")) {
			String violated = checkBits(value);
			if (violated != null) {
				throw new IllegalArgumentException(violated);
			}
			bits = value;
		} else if (name.equals("tristate")) {
			if (value == 1)
				loadTriState = true;
		} else {
			super.setValue(name,value);
		}
	} // end of setValue method
	
	/**
	 * Set a String instance variable value (during a load).
	 * 
	 * @param name The instance variable name.
	 * @param value The instance variable value.
	 */
	@Override
	public void setValue(String name, String value) {
		
		if (name.equals("orient")) {
			// unknown strings leave the orientation unchanged, matching
			// the historical loaders (issue #124: all four orientations)
			orientation = Orientation.parse(value, orientation);
		} else if(name.equals("noncontig")) {
			if(value.equals("true")) noncontig = true;
			else noncontig = false;
		} else {
			super.setValue(name,value);
		}
	} // end of setValue method
	
	/**
	 * This method will flip a group
	 * @param g The current graphics context to facilitate recalculation of size when flipping
	 * @jls.testedby jls.elem.GroupOrientationTest#flipTogglesVerticalOrientations()
	 */
	@Override
	public void flip(java.awt.Graphics g)
	{
		orientation = orientation.flipped();
		inputs.clear();
		outputs.clear();
		width = 0;
		height = 0;
	}

	/**
	 * This method will rotate the group a quarter turn (issue #124).
	 * Subclasses re-run init to rebuild the puts on the new axes.
	 * @param direction The direction to rotate
	 * @param g The current graphics context for use in recalculating size
	 * @jls.testedby jls.elem.GroupOrientationTest#rotateCyclesAllFourOrientations()
	 */
	@Override
	public void rotate(Orientation direction, java.awt.Graphics g)
	{
		if(direction == Orientation.LEFT)
		{
			orientation = orientation.ccw();
		}
		else if(direction == Orientation.RIGHT)
		{
			orientation = orientation.cw();
		}
		inputs.clear();
		outputs.clear();
		width = 0;
		height = 0;
	}

	/**
	 * Tells if a Group is capable of rotating: the same no-attachments
	 * constraint as flipping, since both rebuild every put.
	 * @return False if any input or output has a wire attached, True otherwise
	 * @jls.testedby jls.elem.GroupOrientationTest#attachedGroupRefusesRotateAndFlip()
	 * @jls.testedby jls.elem.GroupOrientationTest#rotateCyclesAllFourOrientations()
	 */
	@Override
	public boolean canRotate()
	{
		return canFlip();
	}

	/**
	 * Tells if a Group is capable of flippiing, can only flip when inputs or outputs have no attachments.
	 * @return False if any input or output has a wire attached, True otherwise
	 * @jls.testedby jls.elem.GroupOrientationTest#attachedGroupRefusesRotateAndFlip()
	 * @jls.testedby jls.elem.GroupOrientationTest#flipTogglesVerticalOrientations()
	 */
	@Override
	public boolean canFlip()
	{
		boolean success = true;
		for(Input i : inputs)
		{
			if(i.isAttached())
			{
				success = false;
				break;
			}
		}
		for(Output o : outputs)
		{
			if(o.isAttached())
			{
				success = false;
				break;
			}
		}
		return success;
	}
	
	/**
	 * Set a pair of int instance variable values (during a load).
	 * 
	 * @param v1 The first value.
	 * @param v2 The second value.
	 */
	@Override
	public void setPair(int v1, int v2) {
		// a malformed file used to NPE/AIOOBE here; reject with a real
		// message and bound the sparse-list growth (issue #52)
		if (v1 < 0 || v2 < 0) {
			throw new IllegalArgumentException(
					INDEX_CONSTRAINT + " (" + v1 + "," + v2 + ")");
		}
		if (noncontig && v1 > 4096) {
			throw new IllegalArgumentException(
					"group wire index " + v1 + " is implausibly large");
		}
		if(noncontig) {
			// Newer save file: more complex, but more features
			Entry e;
			if(ranges.size() <= v1 || ranges.get(v1) == null) {
				e = new Entry(new int[]{});
			}
			else {
				e = ranges.get(v1);
			}
			int[] s1 = e.getValues().clone();
			int[] s2 = new int[s1.length + 1];
			System.arraycopy(s1, 0, s2, 0, s1.length);
			s2[s2.length - 1] = v2;
			e.setValues(s2);
			while(ranges.size() <= v1) {
				ranges.add(null);
			}
			ranges.set(v1, e);
		} else {
			// Legacy save file: sorted, contiguous ranges. A legitimate
			// GUI-authored legacy group assigns each bundle bit to
			// exactly one put, so its contiguous ranges are disjoint and
			// ascending (issue #198, O5: the dialog's `picked` flag
			// forbids reusing a bit). A descending or overlapping range
			// is instead the signature of a file that meant the
			// (put index, bundle bit) "noncontig" reading but omitted
			// the required `noncontig "true"` flag - which would
			// otherwise silently mis-load as a different element (issue
			// #198, O2). Reject it with a message that names the fix
			// rather than mis-loading.
			if (v1 > v2) {
				throw new IllegalArgumentException(noncontigHint(v1, v2));
			}
			Entry candidate = new Entry(v1, v2);
			for (Entry existing : ranges) {
				if (existing != null && rangesOverlap(existing, candidate)) {
					throw new IllegalArgumentException(
							noncontigHint(v1, v2));
				}
			}
			ranges.add(candidate);
		}
	} // end of setPair method

	/**
	 * Whether two contiguous legacy ranges share any bit index. Both
	 * entries are ascending, non-empty contiguous ranges here (built by
	 * {@link Entry#Entry(int,int)} in the legacy load path), so an
	 * endpoint comparison suffices.
	 *
	 * @param a One range.
	 * @param b The other range.
	 * @return True if the ranges intersect.
	 */
	private static boolean rangesOverlap(Entry a, Entry b) {
		if (a.getSize() == 0 || b.getSize() == 0) {
			return false;
		}
		return a.getMin() <= b.getMax() && b.getMin() <= a.getMax();
	} // end of rangesOverlap method

	/**
	 * The load-error message for a legacy (flag-absent) {@code pair} set
	 * that overlaps or descends - the signature of a misread
	 * {@code noncontig} file (issue #198). Names the missing flag so a
	 * third-party writer can fix its output.
	 *
	 * @param v1 The offending pair's first value.
	 * @param v2 The offending pair's second value.
	 * @return The message, consumed as a {@code LoadError} detail.
	 */
	private String noncontigHint(int v1, int v2) {
		return "this " + getClass().getSimpleName().toLowerCase()
				+ "'s bit routing has an invalid or overlapping range at "
				+ "pair (" + v1 + " " + v2 + "): a legacy contiguous group "
				+ "never reuses a bit or lists a descending range. A file "
				+ "written to the (index, bundle bit) pair semantics must "
				+ "declare `String noncontig \"true\"` on this element - "
				+ "the flag JLS writes for every group - or be re-saved "
				+ "from JLS";
	} // end of noncontigHint method
	
	/**
	 * The bit routing of this binder/splitter, one entry per bundled
	 * put, in put order: entry k holds the bundle-side bit indices the
	 * k-th narrow put's bits map to, in narrow-put bit order (bit 0
	 * first). For a Binder the narrow puts are its inputs; for a
	 * Splitter, its outputs. Exposed for consumers of the circuit
	 * model, e.g. the HDL exporter (issue #60); the Entry class itself
	 * stays internal.
	 *
	 * @return a fresh list of fresh index arrays.
	 */
	public ArrayList<int[]> getRangeIndices() {

		ArrayList<int[]> indices = new ArrayList<int[]>(ranges.size());
		for (Entry e : ranges) {
			indices.add(e.getValues());
		}
		return indices;
	} // end of getRangeIndices method

	/**
	 * Save this element.
	 *
	 * @param output The output writer.
	 *
	 * @jls.testedby jls.elem.GroupOrientationTest#orientOf()
	 */
	@Override
	public void save(PrintWriter output) {

		super.save(output);
		output.println(" int bits " + bits);
		output.println(" String orient \"" + orientation.toString() + "\"");
		// noncontig is always true for newer files. Without this, JLS assumes false (legacy save)
		output.println(" String noncontig \"true\"");
		if (triState) {
			output.println(" int tristate 1");
		}
		else {
			output.println(" int tristate 0");
		}
		
		int r = 0;
		for(Entry e : ranges) {
			for(int i : e.getValues()) {
				output.println(" pair " + r + " " + i);
			}
			r += 1;
		}
		output.println("END");
	} // end of save method

	/**
	 * A vertically-oriented group is a format-version-2 feature: a
	 * version-1 reader ignores the UP/DOWN orient value and silently
	 * loads the group horizontal, so files that use it must declare
	 * version 2 to be refused cleanly by older readers instead
	 * (issue #124 per the docs/file-format.md section 9 policy).
	 *
	 * @return 2 if this group is vertical, 1 otherwise.
	 */
	@Override
	public int saveFormatVersion() {

		if (orientation == Orientation.UP
				|| orientation == Orientation.DOWN) {
			return 2;
		}
		return 1;
	} // end of saveFormatVersion method
	
	/**
	 * Copy values to new object.
	 * 
	 * @param el The new object.
	 */
	@Override
	public void copy(Element el) {

		Group it = (Group)el;
		it.bits = bits;
		it.ranges = new ArrayList<Entry>(ranges);
		it.triState = triState;
		it.orientation = orientation;
		for (Input input : inputs) {
			it.inputs.add(input.copy(it));
		}
		for (Output output : outputs) {
			it.outputs.add(output.copy(it));
		}
		super.copy(el);
		return;
	} // end of copy method
	
	/**
	 * A bit range entry.
	 */
	public static class Entry {
		
		// properties
		//private int from;
		//private int to;
		/** The bit indices this entry covers, in stored order. */
		private int[] values;
		/** True once these bits have been bundled (so they can't be picked again). */
		private boolean picked;
		
		/**
		 * Create an entry storing the given values
		 * @param values what values to save
		 */
		public Entry(int[] values) {
			this.values = values.clone();
		}
		
		/**
		 * Create an entry storing every value between min and max (inclusive)
		 * @param min lowest value to store
		 * @param max highest value to store
		 */
		public Entry(int min, int max) {
			this.values = new int[1 + max - min];
			for(int i = min; i <= max; i += 1) {
				values[i - min] = i;
			}
		}
		
		/**
		 * Find minimum value.
		 * 
		 * @return lowest index in values, or -1 if values is empty
		 */
		public int getMin() {
			if(values != null && values.length > 0)
				return values[0];
			else
				return -1;
		}
		
		/**
		 * Find maximum value.
		 * 
		 * @return highest index in values, or -1 if values is empty
		 */
		public int getMax() {
			if(values != null && values.length > 0)
				return values[values.length - 1];
			else
				return -1;
		}
		
		/**
		 * Return a copy of the saved set of indices
		 * 
		 * @return int[] of indices
		 */
		public int[] getValues() {
			return values.clone();
		}
		
		/**
		 * Save a new set of values
		 * 
		 * @param values New set of values to use
		 */
		public void setValues(int[] values) {
			this.values = values.clone();
		}
		
		/**
		 * Return number of stored elements
		 *
		 * @return the number of stored indices
		 */
		public int getSize() {
			return values.length;
		}
		
		/**
		 * Set/reset picked flag.
		 * 
		 * @param which True to set, false to reset.
		 */
		public void setPicked(boolean which) {
			
			picked = which;
		} // end of setPicked method
		
		/**
		 * See if picked.
		 * 
		 * @return true if picked, false if not.
		 */
		public boolean isPicked() {
			
			return picked;
		} // end of isPicked method
		
		/**
		 * Convert to a string, either a single number if from and to are equal,
		 * or a range if not (e.g., "9 - 5"). Note if range is non-continuous
		 * 
		 * @return The string.
		 */
		@Override
		public String toString() {
			
			String p = "";
			if (picked) {
				p = " (bundled)";
			}
			return toCircuitString() + p;
		} // end of toString method
		
		/**
		 * Render this entry's indices as a circuit-facing label: a single
		 * number when it holds one bit, a "max-min" range when the indices
		 * are contiguous, or a comma-separated list of runs otherwise.
		 *
		 * @return the label string.
		 */
		public String toCircuitString() {
			if(getMin() == getMax()) {
				return String.valueOf(getMin());	
			}
			else if (getMax() - getMin() + 1 == values.length) {
				return getMax() + "-" + getMin();
			}
			else {
				String s = "";
				ArrayList<Integer> l = new ArrayList<Integer>();
				int i;
				
				// Create a reversed version for consistency with dialog
				int[] v = new int[values.length];
				for(i = 0; i < values.length; i += 1) {
					v[values.length - i - 1] = values[i];
				}
				
				i = 0;
				while(i < v.length) {
					if(l.isEmpty())
						l.add(v[i]);
					else {
						if(v[i] == v[i-1] - 1)
							l.add(v[i]);
						else {
							if(!s.isEmpty())
								s += ", ";
							if(l.size() > 1)
								s += l.get(0) + "-" + l.get(l.size() - 1);
							else if(!l.isEmpty())
								s += l.get(0);
							l.clear();
							l.add(v[i]);
						}
					}
					i += 1;
				}
				if(!s.isEmpty() && !l.isEmpty())
					s += ", ";
				if(l.size() > 1)
					s += l.get(0) + "-" + l.get(l.size() - 1);
				else if(!l.isEmpty())
					s += l.get(0);
				l.clear();
				return s;
			}
		} // end of toCircuitString method
		
	} // end of Entry class
	
} // end of Group class
