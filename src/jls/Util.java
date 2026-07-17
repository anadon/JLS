package jls;

import jls.elem.*;
import java.awt.Point;
import java.math.BigInteger;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * General utility methods used from all over the place.
 * 
 * @author David A. Poplawski
 */
public final class Util {
	
	/**
	 * Private constructor to keep this class from being instantiated.
	 */
	private Util() {}
	
	/**
	 * Copy elements from one set to another circuit.
	 * 
	 * @param from A set of elements.
	 * @param to A circuit.
	 * 
	 * @return the point of minimum x,y coordinates of all elements.
	 */
	public static Point copy(Set<Element> from, Circuit to) {

		// create set of copied ends (includes attached
		// wire ends that won't be in the selected set)
		Set<WireEnd>ends = new HashSet<WireEnd>();
		
		// first copy everything except wires and wire ends
		int minx = 0;
		int miny = 0;
		boolean firstTime = true;
		for (Element el : from) {
			if (el instanceof Wire || el instanceof WireEnd)
				continue;
			int x = el.getX();
			int y = el.getY();
			if (firstTime) {
				firstTime = false;
				minx = x;
				miny = y;
			}
			if (x < minx)
				minx = x;
			if (y < miny)
				miny = y;
			Element copy = el.copy();
			to.addElement(copy);
			
			// copy all attached wire ends (since they don't show up in
			// the selected set)
			for (Put p : el.getAllPuts()) {
				if (!p.isAttached())
					continue;
				WireEnd oldEnd = p.getWireEnd();
				WireEnd newEnd = oldEnd.copy();
				to.addElement(newEnd);
				newEnd.setPut(p.getCopy());
				p.getCopy().setAttached(newEnd);
				ends.add(oldEnd); // for wire check later
			}
		}
		
		// now copy all remaining wire ends
		for (Element el : from) {
			if (!(el instanceof WireEnd))
				continue;
			WireEnd end = (WireEnd)el;
			int x = end.getX();
			int y = end.getY();
			if (firstTime) {
				firstTime = false;
				minx = x;
				miny = y;
			}
			if (x < minx)
				minx = x;
			if (y < miny)
				miny = y;
			to.addElement(end.copy());
			ends.add(end);
		}
		
		// copy wires
		for (Element el : from) {
			if (!(el instanceof Wire))
				continue;
			Wire wire = (Wire)el;
			WireEnd end1 = wire.getEnd();
			WireEnd end2 = wire.getOtherEnd(end1);
			
			// don't copy wires if both ends aren't being copied
			if (!ends.contains(end1) || !ends.contains(end2)) {
				continue;
			}
			
			// create new wire and add to new ends
			Wire newWire = new Wire(end1.getCopy(),end2.getCopy());
			String probe = wire.getProbe();
			if (probe != null)
				newWire.attachProbe(probe);
			end1.getCopy().addWire(newWire);
			end2.getCopy().addWire(newWire);
			to.addElement(newWire);
		}
		
		// get rid of all wire ends with no wires
		Set<Element>temp = new HashSet<Element>();
		temp.addAll(to.getElements());
		for (Element el : temp) {
			if (el instanceof WireEnd) {
				WireEnd end = (WireEnd)el;
				if (end.degree() == 0) {
					to.remove(end);
				}
			}
		}
		
		// return min point
		return new Point(minx,miny);
	} // end of copy method

	/**
	 * Partition all wires and wire ends into wire nets.
	 * 
	 * @param circ The circuit to partition.
	 */
	public static void partition(Circuit circ) {
		
		LinkedList<WireEnd>ends = new LinkedList<WireEnd>();
		for (Element el : circ.getElements()) {
			if (el instanceof WireEnd) {
				WireEnd end = (WireEnd)el;
				ends.add(end);
			}
		}

		// partition ends into wire nets
		Set<WireNet>nets = new HashSet<WireNet>();
		while (!ends.isEmpty()) {
			
			// start visit list and new wire net
			LinkedList<WireEnd>visit = new LinkedList<WireEnd>();
			WireEnd end = ends.remove();
			visit.add(end);
			WireNet net = new WireNet();
			nets.add(net);
			
			// visit ends in visit list until empty
			Set<WireEnd>visited = new HashSet<WireEnd>();
			while (!visit.isEmpty()) {
				
				// get wire end, add to wire net
				WireEnd vend = visit.remove();
				ends.remove(vend);
				visited.add(vend);
				net.add(vend);
				vend.setNet(net);
				if (vend.isAttached()) {
					net.setBits(vend.getPut().getBits());
					if (vend.getPut() instanceof Output) {
						net.setInput();
					}
				}
				
				// add wires to wire net and add opposite wire ends to visit list
				for (Wire wire : vend.getWires()) {
					WireEnd otherEnd = wire.getOtherEnd(vend);
					if (!visited.contains(otherEnd)) {
						visit.add(otherEnd);
						net.add(wire);
						wire.setNet(net);
					}
				}
			}
		}
		
		// propagate tri-state
		for (WireNet net : nets) {
			for (WireEnd end : net.getAllEnds()) {
				if (!end.isAttached())
					continue;
				if (end.getPut() instanceof Output) {
					Output out = (Output)end.getPut();
					if (out.isTriState()) {
						net.setTriState(true);
					}
				}
			}
		}
	} // end of partition method
	
	/**
	 * Check to make sure a name (string) consists only of letters, digits and underscore,
	 * is at least one character long, and starts with a letter.
	 * 
	 * @param str The string to check.
	 * 
	 * @return true if the name is valid, false if not.
	 */
	public static boolean isValidName(String str) {
		
		if (str.length() == 0)
			return false;
		for (int i=0; i<str.length(); i+=1) {
			char c = str.charAt(i);
			if (Character.isLetter(c))
				continue;
			if (Character.isDigit(c) && i > 0)
				continue;
			if (c == '_' && i > 0)
				continue;
			return false;
		}
		return true;
	} // end of isValidName method
	
	/**
	 * Check for a valid circuit file name.
	 * A valid name can be a path name (with slashes or backslashes), but the actual
	 * file name must be a valid name as defined by isValidName.
	 * Any character in the string before the last slash/backslash is valid.
	 * 
	 * @param str The string to check.
	 * 
	 * @return the base name (minus directory prefix) if valid, or null if not valid.
	 */
	public static String isValidFileName(String str) {

		// accept both separators regardless of platform: splitting only
		// on the platform one rejected C:/work/foo.jls on Windows (#51)
		int last = Math.max(str.lastIndexOf('/'), str.lastIndexOf('\\'));
		String circuitName = str;
		if (last != -1) {
			circuitName = str.substring(last+1);
		}
		if (isValidName(circuitName)) {
			return circuitName;
		}
		else {
			return null;
		}
		
	} // end of isValidFileName method

	/**
	 * The directory used to seed file choosers and new-circuit
	 * directories when nothing better is remembered.  This is the
	 * user's home directory, not the process working directory:
	 * a desktop-launched session (deb/rpm/msi/dmg/AppImage, issue #82)
	 * inherits whatever directory the launcher happened to run in -
	 * commonly the filesystem root, the install directory, or an
	 * AppImage mount point - none of which is anywhere a user keeps
	 * circuits (issue #130).
	 *
	 * @return the user's home directory.
	 */
	public static String defaultDirectory() {

		return System.getProperty("user.home");
	} // end of defaultDirectory method

	/**
	 * Pick the directory a file chooser should open in: a remembered
	 * directory when one exists, otherwise the default directory
	 * (issue #130).
	 *
	 * @param remembered A previously-used directory, or null/empty if
	 * none has been recorded yet.
	 *
	 * @return remembered if non-null and non-empty, otherwise
	 * defaultDirectory().
	 */
	public static String seedDirectory(String remembered) {

		if (remembered == null || remembered.equals(""))
			return defaultDirectory();
		return remembered;
	} // end of seedDirectory method

	/**
	 * Convert value to a string in the given base.
	 * 
	 * @param The value to convert.
	 * @param base The base.
	 * @param True if prefix or suffix needed, false if not.
	 * 
	 * @return the string.
	 */
	public static String convert(BigInteger value, int base, boolean extra) {
		
		if (base == 2) {
			if (extra)
				return value.toString(2)+"B";
			else
				return value.toString(2);
		}
		else if (base == 10) {
			return value.toString();
		}
		else if (base == 16) {
			if (extra)
				return "0x"+value.toString(16);
			else
				return value.toString(16);
		}
		return ""; // shouldn't happen
	} // end of convert method

} // end of Util class
