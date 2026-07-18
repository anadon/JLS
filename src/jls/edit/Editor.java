package jls.edit;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JTabbedPane;

import jls.Circuit;
import jls.FileAbstractor;
import jls.JLSInfo;
import jls.TellUser;
import jls.Util;
import jls.elem.Element;
import jls.elem.SubCircuit;

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
	 *
	 * @see jls.ui.EditorGestureSupport#EditorGestureSupport()
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
			TellUser.error(getTopLevelAncestor(),
					"Can't save an imported circuit", "Error");
			return false;
		}

		// serialize the circuit, then write it atomically
		String dir = circuit.getDirectory() + "/";
		String fileName = dir + circuit.getName() + ".jls";
		StringWriter text = new StringWriter();
		try (PrintWriter output = new PrintWriter(text)) {
			circuit.save(output);
		}
		try {
			// the circuit remembers its container: XZ by default, plain
			// text if the user chose it in Save As (issue #129)
			FileAbstractor.writeCircuit(new File(fileName), text.toString(),
					circuit.getSaveContainer());
		} catch (IOException ex) {
			TellUser.error(getTopLevelAncestor(),
					"Can't write to " + fileName + ": " + ex.getMessage(), "Error");
			return false;
		}

		// the write succeeded, so unsaved-changes protection can stand down
		circuit.clearChanged();

		// delete checkpoint file if there is one, superseding any
		// checkpoint still queued with pre-save content
		cancelCheckpoint(fileName + "~");

		return true;

	} // end of save method

	/**
	 * Give new name to circuit, then save it.
	 */
	public void saveAs() {

		// if imported, can't save
		if (circuit.isImported()) {
			TellUser.error(null,
					"Can't save an imported circuit", "Error");
			return;
		}

		// get name from user
		String oldName = circuit.getDirectory() + "/" + circuit.getName() + ".jls~";
		JFileChooser chooser = new JFileChooser(Util.defaultDirectory());
		// the file-type choice picks the on-disk container (issue #129):
		// XZ (the default) or plain text for version control and for JLS
		// forks without an XZ reader; both save under the .jls name
		javax.swing.filechooser.FileFilter filter =
			new javax.swing.filechooser.FileFilter() {
			/**
			 * Accept .jls files and directories in the Save As chooser.
			 *
			 * @param f The file being offered to the filter.
			 * @return true to show f in the chooser.
			 */
			@Override
			public boolean accept(File f) {
				return f.getName().endsWith(".jls") || f.isDirectory();
			}
			/**
			 * Label for the XZ (default) container choice in the chooser.
			 *
			 * @return the filter's description text.
			 */
			@Override
			public String getDescription() {
				return "JLS Circuit Files (XZ compressed, the default)";
			}
		};
		javax.swing.filechooser.FileFilter textFilter =
			new javax.swing.filechooser.FileFilter() {
			/**
			 * Accept .jls files and directories in the Save As chooser.
			 *
			 * @param f The file being offered to the filter.
			 * @return true to show f in the chooser.
			 */
			@Override
			public boolean accept(File f) {
				return f.getName().endsWith(".jls") || f.isDirectory();
			}
			/**
			 * Label for the plain-text container choice in the chooser.
			 *
			 * @return the filter's description text.
			 */
			@Override
			public String getDescription() {
				return "JLS Circuit Files (plain text: diffable, fork-readable)";
			}
		};
		chooser.addChoosableFileFilter(filter);
		chooser.addChoosableFileFilter(textFilter);
		chooser.setFileFilter(filter);
		if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
			return;
		String fileName = chooser.getSelectedFile().getName().trim();
		if (fileName == null || fileName.isEmpty())
			return;
		String tempName = fileName.replaceAll("\\.jls$","");
		if (!Util.isValidName(tempName)) {
			TellUser.error(JLSInfo.frame,"Invalid file name - must have only letters, digits & _", "Error");
			return;
		}
		if (!fileName.endsWith(".jls")) {
			fileName = fileName + ".jls";
		}

		// make sure name is not already used in some other editor
		String name = fileName.replaceAll("\\.jls$","");
		List<Circuit> edited = new ArrayList<Circuit>();
		for (Component edit : tabbedParent.getComponents()) {
			if (!(edit instanceof Editor))
				continue;
			edited.add(((Editor)edit).getCircuit());
		}
		if (nameUsedByAnotherEditor(name,circuit,edited)) {
			TellUser.error(JLSInfo.frame,
			"Circuit with this name already being edited", "Error");
			return;
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
		// remember the chosen container so plain Save keeps it (#129);
		// "All Files" or the default filter both mean the XZ default
		circuit.setSaveContainer(chooser.getFileFilter() == textFilter
				? FileAbstractor.Container.PLAIN_TEXT
				: FileAbstractor.Container.XZ);

		// save circuit
		if (save()) {

			// delete checkpoint file if there is one
			cancelCheckpoint(oldName);
		}
	} // end of saveAs method

	/**
	 * See if a proposed circuit name is already used by a circuit open in
	 * some other editor.  The circuit being saved is skipped (a circuit
	 * doesn't collide with itself, so Save As under the current name is
	 * allowed), as are imported circuits, which can't be saved anyway.
	 *
	 * @param name The proposed circuit name (no .jls extension).
	 * @param self The circuit being saved.
	 * @param edited The circuits open in sibling editors, including self.
	 *
	 * @return true if another editor's circuit already has the name.
	 *
	 * @see jls.edit.SaveAsNameCheckTest#distinctSiblingWithSameNameIsStillACollision()
	 * @see jls.edit.SaveAsNameCheckTest#importedCircuitsAreSkipped()
	 * @see jls.edit.SaveAsNameCheckTest#savingUnderAnotherEditorsNameIsACollision()
	 * @see jls.edit.SaveAsNameCheckTest#savingUnderOwnCurrentNameIsNotACollision()
	 * @see jls.edit.SaveAsNameCheckTest#unusedNameIsNotACollision()
	 */
	static boolean nameUsedByAnotherEditor(String name, Circuit self,
			Iterable<Circuit> edited) {

		for (Circuit other : edited) {

			// don't compare the circuit being saved with itself
			if (other == self)
				continue;

			if (other.isImported())
				continue;
			if (name.equals(other.getName()))
				return true;
		}
		return false;
	} // end of nameUsedByAnotherEditor method

	/**
	 * Close this editor window, save circuit if necessary and ok with user.
	 */
	@Override
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
			TellUser.Answer result = TellUser.confirmOrCancel(this,
					"Save this circuit?", "Option");
			if (result == TellUser.Answer.YES) {
				if (!save())
					return;
			}
			else if (result == TellUser.Answer.CANCEL) {
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
			TellUser.Answer result = TellUser.confirmOrCancel(null,
					"Save " + circuit.getName() + "?","Save circuit?");
			if (result == TellUser.Answer.YES){
				if (!save())
					return false;
			}
			else if (result == TellUser.Answer.CANCEL)
				return false;
			else { // no save

				// get rid of checkpoint file
				String fileName = circuit.getDirectory() + "/" + circuit.getName() + ".jls";
				cancelCheckpoint(fileName + "~");
			}
		}
		return true;
	} // end of shutdown method

	/**
	 * Enable editing of the circuit.
	 */
	public void enableEdits() {

		hideDisabledBanner();
		enabled = true;
	} // end of enableEdits method

} // end of Editor class
