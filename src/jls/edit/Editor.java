package jls.edit;

import java.awt.Component;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;

import jls.Circuit;
import jls.JLSInfo;
import jls.Util;
import jls.elem.Element;
import jls.elem.SubCircuit;

import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZOutputStream;

/**
 * Adds naming and file save/saveas/close capability to an edited circuit.
 * Used by application version.
 * 
 * @author David A. Poplawski
 */
@SuppressWarnings("serial")
public final class Editor extends SimpleEditor {

	/**
	 * Create new editor.
	 * 
	 * @param pane The tabbed pane this editor is a part of.
	 * @param circuit The circuit it will edit.
	 * @param name The name of the circuit.
	 * @param clipboard The clipboard.
	 */
	public Editor(JTabbedPane pane, Circuit circuit, String name, Circuit clipboard) {

		super(pane,circuit,name,clipboard);

	} // end of constructor

	/**
	 * Save circuit.
	 * 
	 * @return true if the save is successful, false if not
	 */
	public boolean save() {

		// if imported, can't save
		if (circuit.isImported()) {
			JOptionPane.showMessageDialog(getTopLevelAncestor(),
					"Can't save an imported circuit", "Error",
					JOptionPane.ERROR_MESSAGE);
			return false;
		}

		// create output file
		String dir = circuit.getDirectory() + "/";
		String fileName = dir + circuit.getName() + ".jls";
		XZOutputStream out = null;
		try {
			out = new XZOutputStream(new FileOutputStream(fileName), new LZMA2Options());
			//out.putNextEntry(new ZipEntry("JLSCircuit"));
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(getTopLevelAncestor(),
					"Can't write to " + fileName, "Error",
					JOptionPane.ERROR_MESSAGE);
			return false;
		}
		PrintWriter output = new PrintWriter(out);

		// save the circuit
		circuit.save(output);
		output.close();

		// delete checkpoint file if there is one
		new File(fileName + "~").delete();

		return true;

	} // end of save method

	/**
	 * Give new name to circuit, then save it.
	 */
	public void saveAs() {

		// if imported, can't save
		if (circuit.isImported()) {
			JOptionPane.showMessageDialog(null,
					"Can't save an imported circuit", "Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}

		// get name from user
		String oldName = circuit.getDirectory() + "/" + circuit.getName() + ".jls~";
		JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
		javax.swing.filechooser.FileFilter filter =
			new javax.swing.filechooser.FileFilter() {
			public boolean accept(File f) {
				return f.getName().endsWith(".jls") || f.isDirectory();
			}
			public String getDescription() {
				return "JLS Circuit Files";
			}
		};
		chooser.setFileFilter(filter);
		if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) 
			return;
		String fileName = chooser.getSelectedFile().getName().trim();
		if (fileName == null || fileName.equals(""))
			return;
		String tempName = fileName.replaceAll("\\.jls$","");
		if (!Util.isValidName(tempName)) {
			JOptionPane.showMessageDialog(JLSInfo.frame,"Invalid file name - must have only letters, digits & _");
			return;
		}
		if (!fileName.endsWith(".jls")) {
			fileName = fileName + ".jls";
		}

		// make sure name is not already used in some other editor
		String name = fileName.replaceAll("\\.jls$","");
		for (Component edit : tabbedParent.getComponents()) {
			if (!(edit instanceof Editor))
				continue;
			Editor otherEditor = (Editor)edit;
			if (otherEditor.getCircuit().isImported())
				continue;
			if (name.equals(otherEditor.getCircuit().getName())) {
				JOptionPane.showMessageDialog(JLSInfo.frame,
				"Circuit with this name already being edited");
				return;
			}
		}

		// change name in tab
		int pos = tabbedParent.indexOfComponent(this);
		tabbedParent.setTitleAt(pos,name);

		// change name in import menu
		for (Component edit : tabbedParent.getComponents()) {
			if (!(edit instanceof Editor))
				continue;
			Editor otherEditor = (Editor)edit;

			// don't try to change in this editor
			if (otherEditor == this) 
				continue;

			otherEditor.changeInImportMenu(circuit.getName(),name);
		}
		circuit.setName(name);
		circuit.setDirectory(chooser.getCurrentDirectory().toString());

		// save circuit
		if (save()) {

			// delete checkpoint file if there is one
			new File(oldName).delete();
		}
	} // end of saveAs method

	/**
	 * Close this editor window, save circuit if necessary and ok with user.
	 */
	public void close() {
		
		// close any subcircuits of this one that are open
		for (Component edit : tabbedParent.getComponents()) {

			// skip non-editors in tabs
			if (!(edit instanceof Editor))
				continue;

			Editor otherEditor = (Editor)edit;
			
			// skip this editor
			if (otherEditor == this)
				continue;

			// if not editing an imported subcircuit, ignore
			if (!otherEditor.getCircuit().isImported())
				continue;

			// see if the circuit it is editing is a subcircuit of the
			// one this editor is editing
			Circuit circ = otherEditor.getCircuit();
			while (circ.isImported() && circ != circuit) {
				SubCircuit sub = circ.getSubElement();
				circ = sub.getCircuit();
			}

			// if it edits a subcircuit (to any depth) of this one, close it too
			if (circuit == circ) {
				otherEditor.circuit = null;
				tabbedParent.remove(edit);
			}
		}
		
		// if this circuit is a subcircuit...
		if (circuit.isImported()) {

			// enable editing for enclosing circuit
			Element s = circuit.getSubElement();
			Circuit c = s.getCircuit();
			Editor ed = c.getEditor();
			if (ed != null) {
				ed.enableEdits();
			}
		}

		// see if this editor's circuit has been modified
		if (!circuit.isImported() && circuit.hasChanged()) {

			// see if user wants to save it
			int result = JOptionPane.showConfirmDialog(this, "Save this circuit?", "Option", JOptionPane.YES_NO_CANCEL_OPTION);
			if (result == JOptionPane.YES_OPTION) {
				if (!save())
					return;
			}
			else if (result == JOptionPane.CANCEL_OPTION) {
				return;
			}
		}
		
		// if this circuit is not imported, remove this circuit from
		// import menus of all other open circuits
		if (!circuit.isImported()) {
			for (Component edit : tabbedParent.getComponents()) {
				if (!(edit instanceof Editor))
					continue;
				Editor otherEditor = (Editor)edit;
				otherEditor.removeFromImportMenu(circuit);
			}
		}

		// remove editor from circuit it edits
		circuit.setEditor(null);

		// remove editor from tabs
		tabbedParent.remove(this);

	} // end of close method

	/**
	 * Force closure of circuit, save if needed.
	 * 
	 * @return true if shutdown succeeded, false if canceled
	 */
	public boolean shutdown() {

		// don't save circuit if imported
		if (circuit.isImported()) {
			return true;
		}

		// save file if circuit has changed
		if (circuit.hasChanged()) {

			// check with user before saving
			int options = JOptionPane.YES_NO_CANCEL_OPTION;
			int result = JOptionPane.showConfirmDialog(null,
					"Save " + circuit.getName() + "?","Save circuit?",options);
			if (result == JOptionPane.OK_OPTION){
				if (!save())
					return false;
			}
			else if (result == JOptionPane.CANCEL_OPTION)
				return false;
			else { // no save

				// get rid of checkpoint file
				String fileName = circuit.getDirectory() + "/" + circuit.getName() + ".jls";
				new File(fileName + "~").delete();
			}
		}
		return true;
	} // end of shutdown method

	/**
	 * Enable editing of the circuit.
	 */
	public void enableEdits() {

		editable.setText(" ");
		enabled = true;
	} // end of enableEdits method

} // end of Editor class
