package jls;

import java.nio.charset.StandardCharsets;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;
import java.util.Vector;

import javax.print.PrintService;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jls.edit.Editor;
import jls.elem.Element;
import jls.hdl.HdlEmitter;
import jls.hdl.HdlExportException;
import jls.hdl.HdlExporter;
import jls.hdl.VerilogEmitter;
import jls.hdl.VhdlEmitter;
import jls.elem.LogicElement;
import jls.elem.Memory;
import jls.elem.OutputPin;
import jls.elem.Register;
import jls.elem.SubCircuit;
import jls.sim.BatchSimulator;
import jls.sim.InteractiveSimulator;

import org.jspecify.annotations.Nullable;


/**
 * Application entry point and main window of JLS. Parses the command line,
 * then either launches the interactive Swing GUI (a tabbed set of circuit
 * editors with an attached simulator) or runs one of the headless one-shot
 * modes - batch simulation, image export, HDL export, plain-text re-save, or
 * printing a named circuit. As the GUI frame it owns the menu bar, the editor
 * tabs, the shared cut-and-paste clipboard, and the interactive simulator, and
 * drives orderly startup and shutdown.
 */
@SuppressWarnings("serial")
public class JLSStart extends JFrame implements ChangeListener {

	// properties
	/** Simulation time limit (-d flag), defaulting to JLSInfo.defaultTimeLimit. */
	private static long timeLimit = JLSInfo.defaultTimeLimit;
	/** Startup parameter file name (-s flag), or null if none given. */
	private static @Nullable String paramFile = null;
	/** Test input file name (-t flag), or null if none given. */
	private static @Nullable String testFile = null;
	/** Circuit (.jls) file named on the command line, or null if none given. */
	private static @Nullable String startFile = null;
	/** Image export output file name (-i flag operand), or null to derive it from the circuit name. */
	private static @Nullable String imageFile = null;
	/** VCD waveform output file name (-vcd flag), or null if none given. */
	private static @Nullable String vcdFile = null;
	/** HDL export output file name (-export flag), or null if none given. */
	private static @Nullable String exportFile = null;
	/** Plain-text re-save output file name (-savetext flag), or null if none given. */
	private static @Nullable String textSaveFile = null;
	/** True if -p or -v asked for the circuit to be printed. */
	private static boolean printCircuit = false;
	/** True to print only the top level (-v) rather than all subcircuits (-p). */
	private static boolean printCircuitTop;
	/** Printer name from -p, -v or -r, or null if none given. */
	private static @Nullable String printer = null;
	/** The interactive simulator, shared by all open circuit editors. */
	private InteractiveSimulator interSim;
	/** The default exception handler, saved by start() and kept aware of the current circuit. */
	private static @Nullable DefaultExceptionHandler exHandler = null;

	/** The frame's content pane. */
	private Container window;
	/** Splits the editor tabs (top) from the simulator (bottom). */
	private JSplitPane both;
	/** The tabbed pane holding one editor per open circuit. */
	private JTabbedPane edits;
	/** Pseudo-circuit holding the elements most recently cut or copied. */
	private Circuit clipboard = new Circuit("clipboard");		// for cut and paste
	/** Persistent user preferences, opened at startup. */
	private final UserPrefs prefs = UserPrefs.open();	// persistent user preferences (#76)

	/**
	 * Start up JLS.
	 *
	 * @param exh The default exception handler.
	 */
	public static void start(DefaultExceptionHandler exh) {

		// save exception handler reference
		exHandler = exh;

		// if print of circuit wanted, and there is a circuit specified, then print it
		if (printCircuit && startFile != null) {
			printCirc(printCircuitTop);
			return;
		}

		// start up JLS

		// if batch mode, load circuit and run simulator
		if (JLSInfo.batch) {

			// from Zack, for MAC's?
			System.setProperty("java.awt.headless", "true");

			if (startFile == null) {
				System.err.println("jls: error: batch mode requires a circuit file");
				System.exit(1);
				return;
			}

			// open file and create scanner
			String name = "";
			if (startFile.endsWith(".jls~")) {
				name = startFile.replaceAll("\\.jls~$","");
			} else {
				name = startFile.replaceAll("\\.jls$","");
			}
			String cname = Util.isValidFileName(name);
			if (cname == null) {
				System.err.println("jls: error: " + startFile
						+ " is not a valid circuit file name");
				System.exit(1);
				return;
			}

			Scanner input = FileAbstractor.openCircuit(startFile);
			if (input == null) {
				System.err.println("jls: error: can't open " + startFile
						+ ": " + JLSInfo.loadError);
				System.exit(1);
				return;
			}

			// create new circuit
			Circuit circ = new Circuit(cname);

			// read circuit from file
			boolean loadOK = circ.load(input);
			if (loadOK && input.hasNext()) {
				// file shouldn't have anything after ENDCIRCUIT; without
				// a message the failure would be reported blank (#58)
				loadOK = false;
				JLSInfo.setLoadError(LoadError.of(
						LoadError.Category.MALFORMED,
						"there is extra content after the ENDCIRCUIT trailer",
						"The file may contain more than one circuit or "
								+ "trailing garbage; re-save it from JLS."));
			}
			input.close();
			if (!loadOK) {
				System.err.println("jls: error: " + startFile
						+ " is not a valid circuit file: " + JLSInfo.loadError);
				System.exit(1);
			}

			// finish up load
			try {
				if (!circ.finishLoad(null)) {
					System.err.println("jls: error: " + startFile
							+ " is not a valid circuit file: " + JLSInfo.loadError);
					System.exit(1);
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("jls: error: " + startFile
						+ " is not a valid circuit file: " + JLSInfo.loadError);
				System.exit(1);
			}

			// save circuit for trace (hopefully not needed!)
			exHandler.setCircuit(circ);

			// process parameter file
			if (paramFile != null)
				processParamFile(paramFile,circ);

			// set up simulator
			BatchSimulator batchSim = new BatchSimulator();
			JLSInfo.sim = batchSim;
			batchSim.setCircuit(circ);
			batchSim.setTimeLimit(timeLimit);
			batchSim.setTestFile(testFile);
			batchSim.addTestGen();
			// enable trace accumulation for VCD export before the run
			// (issue #72)
			batchSim.setVcdFile(vcdFile);

			// run simulator
			batchSim.runSim();

			// display results
			batchSim.displayOutcome();
			displayResults(circ,"");

			// print trace if requested (the AWT print surface lives
			// GUI-side, outside the headless core - issue #77)
			if (JLSInfo.printTrace) {
				BatchTracePrinter.printTrace(
						batchSim.getTraceSamples(),printer);
			}

			// write VCD waveform file if requested (issue #72)
			if (vcdFile != null) {
				try {
					batchSim.writeVcd();
				} catch (IOException e) {
					System.err.println("jls: error: can't write VCD file "
							+ vcdFile + ": " + e.getMessage());
					System.exit(1);
				}
			}
		}
		
		else if (JLSInfo.imgexport) {
			// from Zack, for MAC's?
			System.setProperty("java.awt.headless", "true");

			if (startFile == null) {
				System.err.println("jls: error: image export requires a circuit file");
				System.exit(1);
				return;
			}

			// open file and create scanner
			String name = "";
			if (startFile.endsWith(".jls~")) {
				name = startFile.replaceAll("\\.jls~$","");
			}
			else {
				name = startFile.replaceAll("\\.jls$","");
			}
			String cname = Util.isValidFileName(name);
			if (cname == null) {
				System.err.println("jls: error: " + startFile
						+ " is not a valid circuit file name");
				System.exit(1);
				return;
			}
			Scanner input = FileAbstractor.openCircuit(startFile);
			if (input == null) {
				System.err.println("jls: error: can't open " + startFile
						+ ": " + JLSInfo.loadError);
				System.exit(1);
				return;
			}

			// create new circuit
			Circuit circ = new Circuit(cname);

			// read circuit from file
			boolean loadOK = circ.load(input);
			if (loadOK && input.hasNext()) {
				// file shouldn't have anything after ENDCIRCUIT; without
				// a message the failure would be reported blank (#58)
				loadOK = false;
				JLSInfo.setLoadError(LoadError.of(
						LoadError.Category.MALFORMED,
						"there is extra content after the ENDCIRCUIT trailer",
						"The file may contain more than one circuit or "
								+ "trailing garbage; re-save it from JLS."));
			}
			input.close();
			if (!loadOK) {
				System.err.println("jls: error: " + startFile
						+ " is not a valid circuit file: " + JLSInfo.loadError);
				System.exit(1);
			}

			// finish up load
			try {
				if (!circ.finishLoad(null)) {
					System.err.println("jls: error: " + startFile
							+ " is not a valid circuit file: " + JLSInfo.loadError);
					System.exit(1);
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("jls: error: " + startFile
						+ " is not a valid circuit file: " + JLSInfo.loadError);
				System.exit(1);
			}
			// export to the caller-chosen path, or PNG named after the
			// circuit by default; the format follows the file extension
			// (issue #71)
			String outFile = (imageFile != null) ? imageFile : name + ".png";
			try {
				circ.exportImage(outFile);
			} catch (Exception e) {
				System.err.println("jls: error: can't export image to "
						+ outFile + ": " + e);
				System.exit(1);
			}
		}

		else if (JLSInfo.hdlexport) {

			// HDL export (issue #60) is headless like batch mode
			System.setProperty("java.awt.headless", "true");

			if (startFile == null) {
				System.err.println("jls: error: HDL export requires a circuit file");
				System.exit(1);
				return;
			}
			if (exportFile == null) {
				System.err.println("jls: error: HDL export requires an output file");
				System.exit(1);
				return;
			}
			Circuit circ = loadCircuitHeadless(startFile);

			// the output extension picked the language at parse time:
			// .v is Verilog-2005, .vhd/.vhdl is VHDL (#60)
			HdlEmitter emitter =
					exportFile.toLowerCase(java.util.Locale.ROOT)
							.endsWith(".v")
					? new VerilogEmitter() : new VhdlEmitter();

			// walk and render; a rejection lists every offending
			// element and writes nothing
			HdlExporter.Result result;
			try {
				result = HdlExporter.export(circ, emitter);
			} catch (HdlExportException e) {
				System.err.println("jls: error: " + e.getMessage());
				System.exit(1);
				return; // unreachable; keeps result definitely assigned
			}
			for (String warning : result.warnings) {
				System.err.println("jls: warning: " + warning);
			}

			// write to a temp file and rename so a partial export can
			// never reach the target path (same pattern as circuit save)
			Path target = Path.of(exportFile);
			Path temp = Path.of(exportFile + ".tmp");
			try {
				java.nio.file.Files.writeString(temp, result.text,
						StandardCharsets.UTF_8);
				try {
					java.nio.file.Files.move(temp, target,
							java.nio.file.StandardCopyOption.REPLACE_EXISTING,
							java.nio.file.StandardCopyOption.ATOMIC_MOVE);
				} catch (java.nio.file.AtomicMoveNotSupportedException ex) {
					java.nio.file.Files.move(temp, target,
							java.nio.file.StandardCopyOption.REPLACE_EXISTING);
				}
			} catch (IOException e) {
				try {
					java.nio.file.Files.deleteIfExists(temp);
				} catch (IOException ignored) {
					// the error below already covers it
				}
				System.err.println("jls: error: can't write " + exportFile
						+ ": " + e.getMessage());
				System.exit(1);
			}
		}

		else if (JLSInfo.textsave) {

			// plain-text re-save (issue #129) is headless like batch mode
			System.setProperty("java.awt.headless", "true");

			if (startFile == null) {
				System.err.println("jls: error: plain-text save requires a circuit file");
				System.exit(1);
				return;
			}
			if (textSaveFile == null) {
				System.err.println("jls: error: plain-text save requires an output file");
				System.exit(1);
				return;
			}
			Circuit circ = loadCircuitHeadless(startFile);

			// like Save As, the circuit takes the output file's name, so
			// the file name and its CIRCUIT line cannot disagree
			String textSaveName = Util.isValidFileName(
					textSaveFile.replaceAll("\\.jls$", ""));
			if (textSaveName == null) {
				System.err.println("jls: error: " + textSaveFile
						+ " is not a valid circuit file name");
				System.exit(1);
				return;
			}
			circ.setName(textSaveName);

			// serialize and write uncompressed; writeCircuit keeps the
			// temp-file/atomic-rename guarantee of ordinary saves
			StringWriter text = new StringWriter();
			try (PrintWriter output = new PrintWriter(text)) {
				circ.save(output);
			}
			try {
				FileAbstractor.writeCircuit(new File(textSaveFile),
						text.toString(),
						FileAbstractor.Container.PLAIN_TEXT);
			} catch (IOException e) {
				System.err.println("jls: error: can't write " + textSaveFile
						+ ": " + e.getMessage());
				System.exit(1);
			}
		}

		else {

			// start up GUI
			Runnable mainwindow = new Runnable() {

				/**
				 * Build the main JLS window on the event dispatch thread,
				 * reporting a headless display as a fatal startup error.
				 */
				@Override
				public void run() {
					try {new JLSStart();}
					catch(HeadlessException e) {
						System.out.println("No usable display device found!");
						System.out.println("Are you using a headless remote session?");
						System.exit(1);
					}
				} // end of run method

			};
			EventQueue.invokeLater(mainwindow);
		}

	} // end of start method

	/**
	 * Load a circuit file for a headless one-shot mode, exactly as the
	 * batch and image-export paths do: sniffing loader, trailing-content
	 * check, finishLoad with no graphics. Any failure prints one
	 * "jls: error:" line and exits 1; on return the circuit is fully
	 * assembled (wire nets built).
	 *
	 * @param file The circuit file path.
	 *
	 * @return the loaded circuit.
	 */
	private static Circuit loadCircuitHeadless(String file) {

		String name;
		if (file.endsWith(".jls~")) {
			name = file.replaceAll("\\.jls~$","");
		}
		else {
			name = file.replaceAll("\\.jls$","");
		}
		String cname = Util.isValidFileName(name);
		if (cname == null) {
			System.err.println("jls: error: " + file
					+ " is not a valid circuit file name");
			System.exit(1);
			throw new AssertionError("unreachable after System.exit");
		}

		Scanner input = FileAbstractor.openCircuit(file);
		if (input == null) {
			System.err.println("jls: error: can't open " + file
					+ ": " + JLSInfo.loadError);
			System.exit(1);
			throw new AssertionError("unreachable after System.exit");
		}

		Circuit circ = new Circuit(cname);
		boolean loadOK = circ.load(input);
		if (loadOK && input.hasNext()) {
			// file shouldn't have anything after ENDCIRCUIT; without
			// a message the failure would be reported blank (#58)
			loadOK = false;
			JLSInfo.setLoadError(LoadError.of(
					LoadError.Category.MALFORMED,
					"there is extra content after the ENDCIRCUIT trailer",
					"The file may contain more than one circuit or "
							+ "trailing garbage; re-save it from JLS."));
		}
		input.close();
		if (!loadOK) {
			System.err.println("jls: error: " + file
					+ " is not a valid circuit file: " + JLSInfo.loadError);
			System.exit(1);
		}

		try {
			if (!circ.finishLoad(null)) {
				System.err.println("jls: error: " + file
						+ " is not a valid circuit file: " + JLSInfo.loadError);
				System.exit(1);
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("jls: error: " + file
					+ " is not a valid circuit file: " + JLSInfo.loadError);
			System.exit(1);
		}
		return circ;
	} // end of loadCircuitHeadless method

	/**
	 * Display values of watched elements to stdout.
	 * Descends into subcircuits recursively.
	 *
	 * The elements of each circuit level are visited in element-name
	 * order (Unicode code point order). Circuit.getElements() is backed
	 * by a HashSet, so plain iteration used to print watched elements
	 * that live at the same level in hash order, which is not stable
	 * across JVMs or code changes; batch output is a text contract
	 * (docs/batch-interface.md), so the order is pinned here at the
	 * display site (issue #72).
	 *
	 * @param circ The circuit to find watched elements in.
	 * @param qual Qualified name of subcircuit.
	 *
	 * @jls.testedby jls.BatchSimulationGoldenTest#watchedElementsPrintInNameOrder()
	 */
	public static void displayResults(Circuit circ, String qual) {

		List<Element> ordered = new ArrayList<Element>(circ.getElements());
		ordered.sort(Comparator.comparing(
				(Element el) -> el.getName() == null ? "" : el.getName()));
		for (Element el : ordered) {
			if (el.isWatched()) {
				switch (el) {
					case Register reg -> reg.printValue(qual);
					case Memory mem -> mem.printChangedValues(qual);
					case OutputPin pin -> pin.printValue(qual);
					default -> { }
				}
			}
			else if (el instanceof SubCircuit sel) {
				Circuit subCirc = sel.getSubCircuit();
				String subQual = "";
				if (qual.isEmpty()) {
					subQual = subCirc.getName();
				}
				else {
					subQual = qual + "." + subCirc.getName();
				}
				displayResults(subCirc,subQual);

			}
		}
	} // end of displayResults method

	/**
	 * Operand arity of a command-line flag (issue #71).
	 */
	private enum Arity {
		/** The flag takes no operand. */
		NONE,
		/** The flag requires an operand. */
		REQUIRED,
		/** The flag may take an operand but need not. */
		OPTIONAL
	}

	/**
	 * One row of the command-line flag table: the flag name (one or
	 * more characters, e.g. "t" or "vcd"), its operand arity, the
	 * operand's name for the usage text, the phrase for "requires ..."
	 * errors, and the usage description.
	 */
	private static final class FlagSpec {

		/** The flag name, without the leading '-'. */
		final String flag;
		/** Whether the flag takes no, a required, or an optional operand. */
		final Arity arity;
		/** The operand's name in the usage text, or null if the flag takes none. */
		final @Nullable String operandName;
		/** The operand phrase for "requires ..." errors, or null if the flag takes none. */
		final @Nullable String operandWhat;
		/** The usage description for this flag. */
		final String description;

		/**
		 * Create one flag-table row.
		 *
		 * @param flag The flag name (without the leading '-').
		 * @param arity Whether the flag takes no, a required, or an optional operand.
		 * @param operandName The operand's name in the usage text, or null.
		 * @param operandWhat The operand phrase for "requires ..." errors, or null.
		 * @param description The usage description for this flag.
		 */
		FlagSpec(String flag, Arity arity, @Nullable String operandName,
				@Nullable String operandWhat, String description) {
			this.flag = flag;
			this.arity = arity;
			this.operandName = operandName;
			this.operandWhat = operandWhat;
			this.description = description;
		}
	} // end of FlagSpec class

	/**
	 * The single authoritative flag specification (issue #71): both
	 * parseCommandLine and usage() are driven by this table, so the
	 * parser and its documentation cannot drift apart. When one flag
	 * name is a prefix of another (-v / -vcd), the longest match wins,
	 * so "-vcd" is the VCD flag, never "-v" with the attached operand
	 * "cd" (issue #72).
	 */
	private static final FlagSpec[] FLAGS = {
		new FlagSpec("h", Arity.NONE, null, null,
				"print this message and exit"),
		new FlagSpec("b", Arity.NONE, null, null,
				"run in batch (headless) mode"),
		new FlagSpec("i", Arity.OPTIONAL, "imagefile", "an image file",
				"export an image of the circuit (default circuit_file.png; use .jpg/.jpeg for JPEG, .svg for SVG)"),
		new FlagSpec("s", Arity.REQUIRED, "file", "a startup file",
				"startup parameter file"),
		new FlagSpec("t", Arity.REQUIRED, "file", "a test file",
				"test input file"),
		new FlagSpec("d", Arity.REQUIRED, "time", "a time limit",
				"set simulation time limit (a positive integer)"),
		new FlagSpec("p", Arity.REQUIRED, "printer", "a printer name",
				"print the whole circuit (subcircuits too) to the named printer"),
		new FlagSpec("v", Arity.REQUIRED, "printer", "a printer name",
				"print only the top level of the circuit to the named printer"),
		new FlagSpec("r", Arity.REQUIRED, "printer", "a printer name",
				"print the signal trace to the named printer"),
		new FlagSpec("vcd", Arity.REQUIRED, "file", "a VCD output file",
				"write watched-signal waveforms to the named VCD file (batch mode)"),
		new FlagSpec("export", Arity.REQUIRED, "file", "an output file",
				"export the circuit as Verilog-2005 (.v) or VHDL (.vhd/.vhdl), chosen by the file extension"),
		new FlagSpec("savetext", Arity.REQUIRED, "file", "an output file",
				"re-save the circuit to the named .jls file as plain (uncompressed) text"),
	};

	/**
	 * The flag names the parser accepts, from the flag table.
	 * Package-visible for the documentation drift test (issue #71).
	 *
	 * @return a fresh array of the accepted flag names.
	 *
	 * @jls.testedby jls.CliFlagTableTest#tableFlags()
	 */
	static String[] commandLineFlags() {

		String[] flags = new String[FLAGS.length];
		for (int f = 0; f < FLAGS.length; f += 1) {
			flags[f] = FLAGS[f].flag;
		}
		return flags;
	} // end of commandLineFlags method

	/**
	 * Whether a file name names a supported image output format:
	 * .png, .jpg or .jpeg (issue #71), or .svg (issue #154),
	 * case-insensitive.
	 *
	 * @param name The file name to check.
	 *
	 * @return true if the name ends in a supported image extension.
	 */
	private static boolean isImageFileName(String name) {

		String lower = name.toLowerCase(java.util.Locale.ROOT);
		return lower.endsWith(".png") || lower.endsWith(".jpg")
				|| lower.endsWith(".jpeg") || lower.endsWith(".svg");
	} // end of isImageFileName method

	/**
	 * Parse command line.
	 *
	 * @param args The command line arguments.
	 */
	public static void parseCommandLine(String [] args) {

		boolean endOfFlags = false;
		int pos = 0;
		while (pos < args.length) {
			String arg = args[pos];
			if (arg.isEmpty()) {
				usageError("empty argument");
			}
			if (!endOfFlags && arg.equals("--")) {
				// end-of-flags marker: everything after it is an
				// operand, so circuit files may begin with '-'
				endOfFlags = true;
				pos += 1;
				continue;
			}
			if (!endOfFlags && arg.charAt(0) == '-') {
				if (arg.length() < 2) {
					usageError("bare '-' is not a valid option");
				}
				// longest flag-name match, so -vcd beats -v (issue #72)
				String body = arg.substring(1);
				FlagSpec spec = null;
				for (FlagSpec s : FLAGS) {
					if (body.startsWith(s.flag)
							&& (spec == null
									|| s.flag.length() > spec.flag.length())) {
						spec = s;
					}
				}
				if (spec == null) {
					usageError("unknown option " + arg);
					return;
				}
				String attached = body.substring(spec.flag.length());
				String opnd = null;
				switch (spec.arity) {
				case NONE:
					if (!attached.isEmpty()) {
						usageError("unknown option " + arg);
					}
					break;
				case REQUIRED:
					if (!attached.isEmpty()) {
						opnd = attached;
					}
					else {
						opnd = operand(args, pos, "-" + spec.flag,
								spec.operandWhat);
						pos += 1;
					}
					break;
				case OPTIONAL:
					// only -i is OPTIONAL; a separated operand is only
					// consumed when it names an image file, so that
					// "jls -i circuit.jls" still exports to the default
					if (!attached.isEmpty()) {
						opnd = attached;
					}
					else if (pos + 1 < args.length
							&& isImageFileName(args[pos + 1])) {
						opnd = args[pos + 1];
						pos += 1;
					}
					break;
				}
				apply(spec.flag, opnd);
			}
			else {
				if (startFile == null) {
					startFile = arg;
				}
				else {
					usageError("arguments after circuit file not allowed: " + arg);
				}
			}
			pos += 1;
		}
	} // end of parseCommandLine method

	/**
	 * Whether this invocation, as parsed, will start the interactive GUI
	 * rather than one of the headless one-shot modes (batch, image
	 * export, HDL export, printing a named circuit). Mirrors the mode
	 * dispatch at the top of start(). Used by the startup toolkit policy
	 * (issue #105), which must never touch or fail a flow that needs no
	 * display.
	 *
	 * @return true if start() will launch the GUI.
	 */
	static boolean guiSessionRequested() {

		return !(printCircuit && startFile != null)
				&& !JLSInfo.batch && !JLSInfo.imgexport
				&& !JLSInfo.hdlexport && !JLSInfo.textsave;
	} // end of guiSessionRequested method

	/**
	 * The fully qualified class name of the default look-and-feel: FlatLaf
	 * light, adopted by issue #153 (evaluation in
	 * {@code docs/flatlaf-evaluation-2026-07.md}), superseding the recorded
	 * "force cross-platform Metal, same everywhere" decision. Named as a
	 * string rather than referenced directly so this seam stays decoupled
	 * from the FlatLaf jar, exactly as {@code -Djls.laf=<class>} does.
	 */
	static final String DEFAULT_LOOK_AND_FEEL =
			"com.formdev.flatlaf.FlatLightLaf";

	/**
	 * The look-and-feel class name selected by the {@code jls.laf} JVM
	 * property (issue #153). Unset or blank installs the default,
	 * {@linkplain #DEFAULT_LOOK_AND_FEEL FlatLaf light}; {@code metal}
	 * keeps the older recorded cross-platform (Metal) look as the
	 * documented escape hatch; {@code system} selects the platform's
	 * native look-and-feel; any other value is taken as the fully
	 * qualified class name of a {@link javax.swing.LookAndFeel} on the
	 * classpath. The class-name form is the evaluation seam for further
	 * third-party looks: drop the jar on the classpath and name its class,
	 * no rebuild needed. Package-visible for the headless policy tests.
	 *
	 * @return the class name of the selected look-and-feel.
	 *
	 * @jls.testedby jls.LookAndFeelPolicyTest
	 */
	static String lookAndFeelClassName() {

		String choice = System.getProperty("jls.laf", "").trim();
		if (choice.isEmpty()) {
			return DEFAULT_LOOK_AND_FEEL;
		}
		if (choice.equals("metal")) {
			return UIManager.getCrossPlatformLookAndFeelClassName();
		}
		if (choice.equals("system")) {
			return UIManager.getSystemLookAndFeelClassName();
		}
		return choice;
	} // end of lookAndFeelClassName method

	/**
	 * Install the look-and-feel selected by {@code -Djls.laf}. A failing
	 * explicit selection (misspelled class, jar missing from the
	 * classpath, look-and-feel unsupported on this platform) is not
	 * fatal: one {@code jls: warning:} line goes to stderr and the
	 * cross-platform default is installed instead, so no experiment can
	 * make JLS unlaunchable. Only a failure of the cross-platform
	 * default itself reports failure, which the caller treats as fatal
	 * exactly as before (issue #153). Package-visible for the headless
	 * policy tests.
	 *
	 * @return true if a look-and-feel was installed.
	 *
	 * @jls.testedby jls.LookAndFeelPolicyTest
	 */
	static boolean installLookAndFeel() {

		String chosen = lookAndFeelClassName();
		String fallback = UIManager.getCrossPlatformLookAndFeelClassName();
		try {
			UIManager.setLookAndFeel(chosen);
			return true;
		}
		catch (Exception ex) {
			if (chosen.equals(fallback)) {
				return false;
			}
			System.err.println("jls: warning: can't set look and feel "
					+ chosen + " (" + ex + "); using the default");
		}
		try {
			UIManager.setLookAndFeel(fallback);
			return true;
		}
		catch (Exception ex) {
			return false;
		}
	} // end of installLookAndFeel method

	/**
	 * Act on one parsed flag.
	 *
	 * @param flag The flag name (guaranteed present in the flag table).
	 * @param opnd Its operand, or null for flags without one.
	 */
	private static void apply(String flag, @Nullable String opnd) {

		switch (flag) {
		case "h":
			usage();
			System.exit(0);
			break;
		case "b":
			JLSInfo.batch = true;
			break;
		case "i":
			JLSInfo.imgexport = true;
			if (opnd != null) {
				if (!isImageFileName(opnd)) {
					usageError("option -i output file must end in .png, .jpg, .jpeg or .svg: "
							+ opnd);
				}
				imageFile = opnd;
			}
			break;
		case "r":
			printer = opnd;
			JLSInfo.printTrace = true;
			break;
		case "p":
			printer = opnd;
			printCircuit = true;
			printCircuitTop = false;
			break;
		case "v":
			printer = opnd;
			printCircuit = true;
			printCircuitTop = true;
			break;
		case "s":
			paramFile = opnd;
			break;
		case "d":
			long limit = 0;
			try {
				limit = Long.parseLong(opnd);
			}
			catch (NumberFormatException ex) {
				usageError("time limit not an integer: " + opnd);
			}
			if (limit <= 0) {
				usageError("option -d requires a positive integer time limit, got "
						+ opnd);
			}
			timeLimit = limit;
			break;
		case "t":
			testFile = opnd;
			break;
		case "vcd":
			vcdFile = opnd;
			break;
		case "export":
			// the extension selects the emitter: .v is Verilog-2005,
			// .vhd/.vhdl is VHDL (#60).
			// -export is Arity.REQUIRED so opnd cannot be null here; the
			// guard keeps that invariant locally checkable
			String hdlName = opnd == null ? ""
					: opnd.toLowerCase(java.util.Locale.ROOT);
			if (!hdlName.endsWith(".v") && !hdlName.endsWith(".vhd")
					&& !hdlName.endsWith(".vhdl")) {
				usageError("option -export output file must end in .v, "
						+ ".vhd or .vhdl: " + opnd);
			}
			JLSInfo.hdlexport = true;
			exportFile = opnd;
			break;
		case "savetext":
			// the output must reopen by the same rules as any other
			// circuit file, so its name is validated up front (#129):
			// a .jls suffix on a valid circuit name
			// (-savetext is Arity.REQUIRED, so opnd cannot be null; the
			// guard keeps that invariant locally checkable)
			String textName = opnd == null ? "" : opnd;
			if (!textName.endsWith(".jls")
					|| Util.isValidFileName(
							textName.replaceAll("\\.jls$", "")) == null) {
				usageError("option -savetext output file must be a .jls "
						+ "file named like a circuit (letters, digits and "
						+ "underscores, starting with a letter): " + opnd);
			}
			JLSInfo.textsave = true;
			textSaveFile = opnd;
			break;
		default:
			// unreachable: parseCommandLine only passes table flags
			usageError("unknown option -" + flag);
			break;
		}
	} // end of apply method

	/**
	 * Report a usage error per the CLI contract (#42) and exit 2:
	 * one line on stderr, no dialog, no crash file.
	 *
	 * @param message What was wrong with the command line.
	 */
	private static void usageError(String message) {

		System.err.println("jls: error: " + message
				+ "; run 'jls -h' for usage");
		System.exit(2);
	} // end of usageError method

	/**
	 * The operand of a flag, or a usage error if it is missing. The
	 * previous parser read args[pos+1] unchecked and crashed on a
	 * trailing flag (issue #42).
	 *
	 * @param args The command line arguments.
	 * @param pos The position of the flag itself.
	 * @param flag The flag, for the error message.
	 * @param what What kind of operand the flag needs.
	 *
	 * @return the operand.
	 */
	private static String operand(String[] args, int pos, String flag,
			@Nullable String what) {

		if (pos + 1 >= args.length || args[pos + 1].isEmpty()
				|| args[pos + 1].charAt(0) == '-') {
			usageError("option " + flag + " requires " + what + " operand");
		}
		return args[pos + 1];
	} // end of operand method

	/**
	 * Print usage information to stderr.
	 */
	private static void usage() {

		System.err.print(usageText());
	} // end of usage method

	/**
	 * The usage text, generated from the flag table so it cannot
	 * drift from what the parser accepts (issue #71).
	 * Package-visible for the documentation drift test.
	 *
	 * @return the complete usage message.
	 *
	 * @jls.testedby jls.CliFlagTableTest#helpPrintsTheGeneratedUsageAndExitsZero()
	 * @jls.testedby jls.CliFlagTableTest#usageDocumentsExactlyTheParserFlags()
	 * @jls.testedby jls.WaylandStartupCliTest#helpIsUnaffectedAndDocumentsTheEscapeHatch()
	 */
	static String usageText() {

		StringBuilder text = new StringBuilder();
		text.append("usage: jls [ flags ] [ -- ] [ circuit_file ]\n");
		for (FlagSpec spec : FLAGS) {
			text.append("  -").append(spec.flag);
			if (spec.arity == Arity.REQUIRED) {
				text.append(' ').append(spec.operandName);
			}
			else if (spec.arity == Arity.OPTIONAL) {
				text.append(" [").append(spec.operandName).append(']');
			}
			text.append(" : ").append(spec.description).append('\n');
		}
		text.append("operands may also be attached to the flag: -tfile, -d10000\n");
		text.append("'--' ends flag processing so operands may begin with '-'\n");
		text.append("JVM property -Djls.toolkit=default|wayland overrides Wayland toolkit auto-selection\n");
		text.append("JVM property -Djls.laf=metal|system|<class> selects the Swing look-and-feel (default FlatLaf light)\n");
		text.append("exit status: 0 success, 1 runtime failure, 2 usage error\n");
		text.append("example: jls -b -sstartup -d10000 counter.jls\n");
		return text.toString();
	} // end of usageText method

	/**
	 * Set up main window.
	 */
	public JLSStart() {

		// install the selected look-and-feel: FlatLaf light by default
		// (issue #153), unless -Djls.laf picks another one; a failing
		// FlatLaf falls back to the cross-platform default inside
		// installLookAndFeel, so only a failure of that fallback is fatal
		if (!installLookAndFeel()) {

			TellUser.error(this, "Can't set cross platform look and feel", "Error");
			System.exit(1);
		}

		// apply persisted user preferences (theme, colors) before any
		// editor window exists (issue #76)
		prefs.applyStartup();

		// save reference for exceptions
		if (exHandler != null)
			exHandler.setJLS(this);

		// save reference for dialogs
		JLSInfo.frame = this;

		// handle window closings
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		addWindowListener (
				new WindowAdapter() {
					/**
					 * Begin an orderly JLS shutdown when the window's
					 * close box is used.
					 *
					 * @param e Unused.
					 */
					@Override
					public void windowClosing(WindowEvent e) {
						shutdown();
					}
				}
		);

		// set up menu bar
		JMenuBar bar = new JMenuBar();
		bar.add(fileMenu());
		bar.add(simMenu());
		bar.add(viewMenu());
		bar.add(globalMenu());
		bar.add(Box.createHorizontalGlue());
		bar.add(helpMenu());
		setJMenuBar(bar);

		// set up main display
		window = getContentPane();
		window.setLayout(new BorderLayout());
		both = new JSplitPane(JSplitPane.VERTICAL_SPLIT,true);
		//both.setDividerLocation(1.0);
		edits = new JTabbedPane(JTabbedPane.TOP,JTabbedPane.SCROLL_TAB_LAYOUT);
		edits.setMinimumSize(new Dimension(1,1));
		both.setTopComponent(edits);
		window.add(both,BorderLayout.CENTER);
		edits.addChangeListener(this);

		// finish up GUI settings
		setTitle(JLSInfo.version);
		int width = (int)(1.618*JLSInfo.windowsize);	// golden ratio
		int height = JLSInfo.windowsize;
		setSize(width,height);
		// centered in the screen; setLocationRelativeTo(null) asks the
		// GraphicsEnvironment for the real center point instead of the
		// Toolkit whole-screen size, which is unreliable on mixed-DPI and
		// Wayland setups (issue #105)
		setLocationRelativeTo(null);
		setVisible(true);

		// set up simulator
		interSim = new InteractiveSimulator();
		JLSInfo.sim = interSim;
		interSim.setTestFile(testFile);

		// load circuit if name given on command line
		if (startFile != null) {
			open(startFile);
		}

	} // end of constructor

	/**
	 * Make simulator point at the correct circuit.
	 * This is the circuit currently being edited if the circuit is not a
	 * subcircuit.
	 * If it is a subcircuit, then the simulator will be pointed at the
	 * root of the subcircuit containment.
	 * If no tab is selected (i.e., all editors have been closed),
	 * then the simulator's circuit is set to null.
	 * 
	 * @param event Unused.
	 */
	@Override
	public void stateChanged(ChangeEvent event) {

		Editor ed = (Editor)edits.getSelectedComponent();
		if (ed == null) {
			interSim.setCircuit(null);
			both.remove(interSim.getWindow());
			return;
		}
		Circuit circ = ed.getCircuit();
		SubCircuit sub;
		while ((sub = circ.getSubElement()) != null) {
			circ = sub.getCircuit();
		}
		interSim.setCircuit(circ);

		// the newly selected editor's canvas takes keyboard focus, so
		// canvas shortcuts work immediately without mousing over it
		// (issue #75: focus follows tab selection and clicks, not hover)
		ed.focusOnCanvas();
	} // end of stateChanged method

	/**
	 * Terminate JLS, but first make sure modified circuits get saved if the
	 * user wants them to be.
	 * Called when JLS is asked to exit.
	 */
	private void shutdown() {

		boolean cancel = false;
		for (Component comp : edits.getComponents()) {
			if (comp instanceof Editor editor) {
				if (!editor.shutdown()) {
					cancel = true;
					break;
				}
				edits.remove(editor);
			}
		}
		if (!cancel)
			System.exit(0);
	} // end of shutdown method

	/**
	 * Get currently visible editor.
	 * 
	 * @return the currently visible editor, or null if no editor is visible.
	 */
	public Editor getVisibleEditor() {

		return (Editor)edits.getSelectedComponent();
	} // end of getVisibleEditor

	/**
	 * Set up file menu.
	 * 
	 * @return the file menu created.
	 */
	public JMenu fileMenu() {

		// menu accelerators are registered by Swing in the window's
		// WHEN_IN_FOCUSED_WINDOW map, so every file operation works no
		// matter which component has focus (issue #75); the platform
		// modifier and the mnemonic come from MenuAcceleratorPolicy so
		// the scheme is unit-testable headless
		String osName = System.getProperty("os.name");
		JMenu menu = new JMenu("File");
		menu.setMnemonic(MenuAcceleratorPolicy.fileMenuMnemonic());

		// new
		JMenuItem newc = new JMenuItem("New");
		newc.setToolTipText("create a new circuit");
		newc.setAccelerator(MenuAcceleratorPolicy.newCircuit(osName));
		newc.setMnemonic(KeyEvent.VK_N);
		menu.add(newc);
		newc.addActionListener(new ActionListener() {
			/**
			 * Create a new circuit when the New menu item is chosen.
			 *
			 * @param event Unused.
			 */
			@Override
			public void actionPerformed(ActionEvent event) {
				newCircuit();
			}
		});

		// open
		JMenuItem open = new JMenuItem("Open");
		open.setToolTipText("open an existing circuit file");
		open.setAccelerator(MenuAcceleratorPolicy.open(osName));
		open.setMnemonic(KeyEvent.VK_O);
		menu.add(open);
		open.addActionListener(new ActionListener() {
			/**
			 * Prompt for and open an existing circuit when the Open menu
			 * item is chosen.
			 *
			 * @param event Unused.
			 */
			@Override
			public void actionPerformed(ActionEvent event) {
				open(null);
			}
		});

		// save
		JMenuItem saveItem = new JMenuItem("Save");
		saveItem.setToolTipText("save the currently visible circuit");
		saveItem.setAccelerator(MenuAcceleratorPolicy.save(osName));
		saveItem.setMnemonic(KeyEvent.VK_S);
		menu.add(saveItem);
		saveItem.addActionListener(new ActionListener() {
			/**
			 * Save the currently visible circuit when the Save menu item
			 * is chosen.
			 *
			 * @param event Unused.
			 */
			@Override
			public void actionPerformed(ActionEvent event) {
				Editor ed = (Editor)(edits.getSelectedComponent());
				if (ed != null)
					ed.save();
			}
		});

		// save as
		JMenuItem saveAs = new JMenuItem("Save As");
		saveAs.setToolTipText("save the currently visible circuit under a new name");
		saveAs.setAccelerator(MenuAcceleratorPolicy.saveAs(osName));
		saveAs.setMnemonic(KeyEvent.VK_A);
		menu.add(saveAs);
		saveAs.addActionListener(new ActionListener() {
			/**
			 * Save the currently visible circuit under a new name when the
			 * Save As menu item is chosen.
			 *
			 * @param event Unused.
			 */
			@Override
			public void actionPerformed(ActionEvent event) {
				Editor ed = (Editor)(edits.getSelectedComponent());
				if (ed != null)
					ed.saveAs();
			}
		});

		// print menu item
		JMenu print = new JMenu("Print...");
		print.setMnemonic(KeyEvent.VK_P);
		JMenuItem printAll = new JMenuItem("Entire circuit");
		printAll.setToolTipText("print the circuit and all subcircuits");
		JMenuItem justThis = new JMenuItem("Just visible window");
		justThis.setToolTipText("print only the currently visible window");
		menu.add(print);
		print.add(printAll);
		print.add(justThis);
		printAll.addActionListener(new ActionListener() {
			/**
			 * Print the entire circuit when the "Entire circuit" menu item
			 * is chosen.
			 *
			 * @param event Unused.
			 */
			@Override
			public void actionPerformed(ActionEvent event) {
				print(true);
			}
		});
		justThis.addActionListener(new ActionListener() {
			/**
			 * Print only the visible window when the "Just visible window"
			 * menu item is chosen.
			 *
			 * @param event Unused.
			 */
			@Override
			public void actionPerformed(ActionEvent event) {
				print(false);
			}
		});

		// import from file menu item
		JMenuItem importItem = new JMenuItem("Import");
		importItem.setToolTipText("create a subcircuit from a circuit file");
		importItem.setMnemonic(KeyEvent.VK_I);
		menu.add(importItem);
		importItem.addActionListener(new ActionListener() {
			/**
			 * Import a circuit from a file when the Import menu item is
			 * chosen.
			 *
			 * @param event Unused.
			 */
			@Override
			public void actionPerformed(ActionEvent event) {
				try {
					fileImport();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});

		// export circuit image menu item
		JMenuItem exportItem = new JMenuItem("Export Image");
		exportItem.setToolTipText("create a JPEG image file of the circuit");
		exportItem.setMnemonic(KeyEvent.VK_E);
		menu.add(exportItem);
		exportItem.addActionListener(new ActionListener() {
			/**
			 * Export a circuit image when the Export Image menu item is
			 * chosen.
			 *
			 * @param event Unused.
			 */
			@Override
			public void actionPerformed(ActionEvent event) {
				try {
					exportImage();
				} catch (Exception e) {
					System.out.println("ERROR: failed to export image, invalid "
							+ "class reference; could not initialize graphics");
					e.printStackTrace();
				}
			}
		});

		// close menu item
		// no accelerator on Close yet: the platform-conventional Cmd/Ctrl+W
		// still starts a wire (or toggles a watch) when the canvas has
		// focus, and issue #75's adjudication keeps that old binding as an
		// alias, so advertising W here would promise a close the canvas
		// would intercept; the wire-start rebinding owns that migration
		JMenuItem close = new JMenuItem("Close");
		close.setToolTipText("close the currently visible circuit");
		close.setMnemonic(KeyEvent.VK_C);
		menu.add(close);
		close.addActionListener(new ActionListener() {
			/**
			 * Close the currently visible editor when the Close menu item
			 * is chosen.
			 *
			 * @param event Unused.
			 */
			@Override
			public void actionPerformed(ActionEvent event) {
				closeVisibleEditor();
			}
		});

		// exit menu item
		JMenuItem exit = new JMenuItem("Exit");
		exit.setToolTipText("terminate JLS");
		exit.setAccelerator(MenuAcceleratorPolicy.exit(osName));
		exit.setMnemonic(KeyEvent.VK_X);
		menu.add(exit);
		exit.addActionListener(new ActionListener() {
			/**
			 * Terminate JLS when the Exit menu item is chosen.
			 *
			 * @param event Unused.
			 */
			@Override
			public void actionPerformed(ActionEvent event) {
				shutdown();
			}
		});

		return menu;
	} // end of fileMenu method

	/**
	 * Set up simulator menu.
	 * 
	 * @return the menu.
	 */
	public JMenu simMenu() {

		JMenu menu = new JMenu("Simulator");
		menu.setMnemonic(MenuAcceleratorPolicy.simulatorMenuMnemonic());
		JMenuItem show = new JMenuItem("Show Simulator Window");
		show.setToolTipText("make simulator window appear");
		show.setMnemonic(KeyEvent.VK_S);
		menu.add(show);
		JMenuItem hide = new JMenuItem("Hide Simulator Window");
		hide.setToolTipText("make simulator window disappear");
		hide.setMnemonic(KeyEvent.VK_H);
		menu.add(hide);
		JMenuItem run = new JMenuItem("Run (in background)");
		run.setToolTipText("run simulator, don't show window");
		run.setAccelerator(KeyStroke.getKeyStroke("F5"));
		run.setMnemonic(KeyEvent.VK_R);
		menu.add(run);
		JMenuItem stop = new JMenuItem("Stop (background simulator)");
		stop.setToolTipText("stop runaway simulator");
		stop.setAccelerator(KeyStroke.getKeyStroke("F7"));
		stop.setMnemonic(KeyEvent.VK_T);
		menu.add(stop);

		run.addActionListener(new ActionListener() {
			/**
			 * Run the simulator in the background when the Run menu item
			 * is chosen.
			 *
			 * @param event Unused.
			 */
			@Override
			public void actionPerformed(ActionEvent event) {
				both.setBottomComponent(interSim.getStatusBar());
				both.setDividerLocation(0.9);
				// per-run flag: the old JLSInfo.batch toggle was reset
				// before the sim thread ever read it (issue #49, M15)
				interSim.setMaxTime();
				interSim.runSim(true);
			}
		});

		stop.addActionListener(new ActionListener() {
			/**
			 * Stop a runaway background simulator when the Stop menu item
			 * is chosen.
			 *
			 * @param event Unused.
			 */
			@Override
			public void actionPerformed(ActionEvent event) {
				interSim.stop();
			}
		});

		show.addActionListener(new ActionListener() {
			/**
			 * Make the simulator window appear when the Show menu item is
			 * chosen.
			 *
			 * @param event Unused.
			 */
			@Override
			public void actionPerformed(ActionEvent event) {
				both.setDividerLocation(0.7);
				both.setBottomComponent(interSim.getWindow());
			}
		});

		hide.addActionListener(new ActionListener() {
			/**
			 * Make the simulator window disappear when the Hide menu item
			 * is chosen.
			 *
			 * @param event Unused.
			 */
			@Override
			public void actionPerformed(ActionEvent event) {
				both.remove(interSim.getWindow());
				both.setDividerLocation(1.0);
			}
		});

		return menu;
	} // end of simMenu method

	/**
	 * Set up the View menu (issue #74): canvas zoom in/out, actual size,
	 * and fit-to-circuit, each acting on the currently selected editor
	 * tab. The accelerators mirror the canvas key bindings so both the
	 * menu and the shortcuts drive the same {@link jls.edit.SimpleEditor}
	 * zoom methods.
	 *
	 * @return the menu.
	 */
	public JMenu viewMenu() {

		int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
		JMenu menu = new JMenu("View");
		menu.setMnemonic(KeyEvent.VK_V);

		JMenuItem zoomIn = new JMenuItem("Zoom In");
		zoomIn.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS,mask));
		menu.add(zoomIn);
		zoomIn.addActionListener(new ActionListener() {
			/**
			 * Zoom the visible editor in one ladder stop.
			 *
			 * @param event Unused.
			 */
			@Override
			public void actionPerformed(ActionEvent event) {
				Editor ed = (Editor)edits.getSelectedComponent();
				if (ed != null)
					ed.zoomIn();
			}
		});

		JMenuItem zoomOut = new JMenuItem("Zoom Out");
		zoomOut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS,mask));
		menu.add(zoomOut);
		zoomOut.addActionListener(new ActionListener() {
			/**
			 * Zoom the visible editor out one ladder stop.
			 *
			 * @param event Unused.
			 */
			@Override
			public void actionPerformed(ActionEvent event) {
				Editor ed = (Editor)edits.getSelectedComponent();
				if (ed != null)
					ed.zoomOut();
			}
		});

		JMenuItem actualSize = new JMenuItem("Actual Size");
		actualSize.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0,mask));
		menu.add(actualSize);
		actualSize.addActionListener(new ActionListener() {
			/**
			 * Reset the visible editor to 100% zoom.
			 *
			 * @param event Unused.
			 */
			@Override
			public void actionPerformed(ActionEvent event) {
				Editor ed = (Editor)edits.getSelectedComponent();
				if (ed != null)
					ed.zoomReset();
			}
		});

		JMenuItem fit = new JMenuItem("Fit to Circuit");
		fit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_9,mask));
		menu.add(fit);
		fit.addActionListener(new ActionListener() {
			/**
			 * Fit the visible editor's whole circuit into the canvas.
			 *
			 * @param event Unused.
			 */
			@Override
			public void actionPerformed(ActionEvent event) {
				Editor ed = (Editor)edits.getSelectedComponent();
				if (ed != null)
					ed.zoomToFit();
			}
		});

		return menu;
	} // end of viewMenu method

	/**
	 * Set up global change menu.
	 *
	 * @return the menu.
	 */
	public JMenu globalMenu() {

		JMenu menu = new JMenu("Global");
		menu.setMnemonic(MenuAcceleratorPolicy.globalMenuMnemonic());

		JMenuItem resetDelays = new JMenuItem("Reset all propagation delays");
		resetDelays.setMnemonic(KeyEvent.VK_R);
		menu.add(resetDelays);
		resetDelays.addActionListener(new ActionListener() {
			/**
			 * Reset all propagation delays in the visible circuit when the
			 * menu item is chosen.
			 *
			 * @param event Unused.
			 */
			@Override
			public void actionPerformed(ActionEvent event) {
				Editor ed = (Editor)edits.getSelectedComponent();
				if (ed != null) {
					ed.getCircuit().resetAllDelays();
					ed.repaint();
				}
			}
		});

		JMenuItem removeProbes = new JMenuItem("Remove all probes");
		removeProbes.setMnemonic(KeyEvent.VK_P);
		menu.add(removeProbes);
		removeProbes.addActionListener(new ActionListener() {
			/**
			 * Remove all probes from the visible circuit when the menu
			 * item is chosen.
			 *
			 * @param event Unused.
			 */
			@Override
			public void actionPerformed(ActionEvent event) {
				Editor ed = (Editor)edits.getSelectedComponent();
				if (ed != null) {
					ed.getCircuit().removeAllProbes();
					ed.repaint();
				}
			}
		});

		JMenuItem clearWatches = new JMenuItem("Unwatch all elements");
		clearWatches.setMnemonic(KeyEvent.VK_U);
		menu.add(clearWatches);
		clearWatches.addActionListener(new ActionListener() {
			/**
			 * Unwatch all elements in the visible circuit when the menu
			 * item is chosen.
			 *
			 * @param event Unused.
			 */
			@Override
			public void actionPerformed(ActionEvent event) {
				Editor ed = (Editor)edits.getSelectedComponent();
				if (ed != null) {
					ed.getCircuit().clearAllWatches();
					ed.repaint();
				}
			}
		});

		// The "Expand circuit drawing area by 10%" item is retired (issue
		// #74): the canvas auto-grows and zoom lives in the View menu.

		JMenu scheme = new JMenu("Color scheme");
		menu.add(scheme);
		ButtonGroup schemeGroup = new ButtonGroup();
		for (Theme theme : Theme.all()) {
			JRadioButtonMenuItem item =
					new JRadioButtonMenuItem(theme.name());
			schemeGroup.add(item);
			scheme.add(item);
			item.setSelected(theme == Theme.active());
			item.addActionListener(new ActionListener() {
				/**
				 * Apply and persist the chosen color scheme, then
				 * repaint every open editor so it takes effect at once.
				 *
				 * @param event Unused.
				 */
				@Override
				public void actionPerformed(ActionEvent event) {
					theme.apply();
					prefs.rememberTheme(theme.name());
					refreshEditorColors();
				}
			});
		}

		JMenuItem gridCol = new JMenuItem("Change editor window grid color");
		gridCol.setMnemonic(KeyEvent.VK_G);
		menu.add(gridCol);
		gridCol.addActionListener(new ActionListener() {
			/**
			 * Prompt for and apply a new editor grid color when the menu
			 * item is chosen.
			 *
			 * @param event Unused.
			 */
			@Override
			public void actionPerformed(ActionEvent event) {
				Color newColor = JColorChooser.showDialog(null, "Select Grid Color", JLSInfo.gridColor);
				if (newColor != null) {
					JLSInfo.gridColor = newColor;
					prefs.rememberGridColor(newColor);
				}
				Editor ed = (Editor)edits.getSelectedComponent();
				if (ed != null) {
					ed.repaint();
				}
			}
		});

		JMenuItem editBkg = new JMenuItem("Change editor window background color");
		editBkg.setMnemonic(KeyEvent.VK_B);
		menu.add(editBkg);
		editBkg.addActionListener(new ActionListener() {
			/**
			 * Prompt for and apply a new editor background color when the
			 * menu item is chosen.
			 *
			 * @param event Unused.
			 */
			@Override
			public void actionPerformed(ActionEvent event) {
				Color newColor = JColorChooser.showDialog(null, "Select Background Color", JLSInfo.backgroundColor);
				if (newColor != null) {
					JLSInfo.backgroundColor = newColor;
					prefs.rememberBackgroundColor(newColor);
				}
				Editor ed = (Editor)edits.getSelectedComponent();
				if (ed != null) {
					ed.changeBackgroundColor();
					ed.repaint();
				}
			}
		});

		JMenuItem undoDepth = new JMenuItem("Change undo depth");
		undoDepth.setMnemonic(KeyEvent.VK_D);
		menu.add(undoDepth);
		undoDepth.addActionListener(new ActionListener() {
			/**
			 * Prompt for and apply a new undo depth when the menu item is
			 * chosen.
			 *
			 * @param event Unused.
			 */
			@Override
			public void actionPerformed(ActionEvent event) {
				String input = TellUser.prompt(JLSStart.this, "Enter new undo depth",
						Integer.toString(JLSInfo.undoStackDepth));
				if (input != null) {
					try {
						int newDepth = Integer.parseInt(input);
						if (newDepth > 0) {
							JLSInfo.undoStackDepth = newDepth;
							prefs.rememberUndoDepth(newDepth);
						}
						else {
							TellUser.error(JLSStart.this, "Undo depth must be positive.",
									"Invalid undo depth");
						}
					}
					catch (NumberFormatException ex) {
						TellUser.error(JLSStart.this, "Undo depth must be an integer.",
								"Invalid undo depth");
					}
				}
			}
		});

		return menu;
	} // end of globalMenu method

	/**
	 * Push the active theme's colors into every open editor tab and
	 * repaint, so a color scheme change takes effect immediately.
	 */
	private void refreshEditorColors() {

		for (int i = 0; i < edits.getTabCount(); i += 1) {
			Component c = edits.getComponentAt(i);
			if (c instanceof Editor ec) {
				ec.changeBackgroundColor();
				c.repaint();
			}
		}
	} // end of refreshEditorColors method

	/**
	 * Create help menu.
	 * 
	 * @return the menu.
	 */
	public JMenu helpMenu() {

		JMenu help = new JMenu("Help");
		help.setMnemonic(MenuAcceleratorPolicy.helpMenuMnemonic());

		// set up about
		JMenuItem about = new JMenuItem("About");
		about.setMnemonic(KeyEvent.VK_A);
		help.add(about);
		about.addActionListener(new ActionListener() {
			/**
			 * Show the About dialog when the About menu item is chosen.
			 *
			 * @param event Unused.
			 */
			@Override
			public void actionPerformed(ActionEvent event) {
				new About();
			}
		});

		// set up tutorial
		JMenu tutorial = new JMenu("Tutorial");
		tutorial.setMnemonic(KeyEvent.VK_T);
		help.add(tutorial);
		JMenuItem tutorial1 = new JMenuItem("Introduction");
		String tip1 = "<html>This tutorial demonstrates the " +
		"basic drawing capabilities<br>" + 
		"using simple gates and wires, " +
		"and how to use the simulator&nbsp;&nbsp;<br>" +
		"to watch the circuit in action.</html>";
		tutorial1.setToolTipText(tip1);
		tutorial.add(tutorial1);
		tutorial1.addActionListener(new ActionListener() {
			/**
			 * Open the introduction tutorial when its menu item is chosen.
			 *
			 * @param event Unused.
			 */
			@Override
			public void actionPerformed(ActionEvent event) {
				new Tutorial(JLSInfo.frame,0);
			}
		});
		JMenuItem tutorial2 = new JMenuItem("4-Bit Counter");
		String tip2 = "<html>Demonstrates the use of more complex&nbsp;&nbsp;<br>" + 
		"elements and multi-wire connections.</html>";
		tutorial2.setToolTipText(tip2);
		tutorial.add(tutorial2);
		tutorial2.addActionListener(new ActionListener() {
			/**
			 * Open the 4-bit counter tutorial when its menu item is chosen.
			 *
			 * @param event Unused.
			 */
			@Override
			public void actionPerformed(ActionEvent event) {
				new Tutorial(JLSInfo.frame,1);
			}
		});
		JMenuItem tutorial3 = new JMenuItem("Full Adder");
		String tip3 = "<html>Demonstrates how to define and use subcircuits.";
		tutorial3.setToolTipText(tip3);
		tutorial.add(tutorial3);
		tutorial3.addActionListener(new ActionListener() {
			/**
			 * Open the full adder tutorial when its menu item is chosen.
			 *
			 * @param event Unused.
			 */
			@Override
			public void actionPerformed(ActionEvent event) {
				new Tutorial(JLSInfo.frame,2);
			}
		});
		JMenuItem tutorial4 = new JMenuItem("Sign Extension");
		String tip4 = "<html>Demonstrates how to bundle/unbundle&nbsp;&nbsp;<br>" +
		"and to use the signal copy element.";
		tutorial4.setToolTipText(tip4);
		tutorial.add(tutorial4);
		tutorial4.addActionListener(new ActionListener() {
			/**
			 * Open the sign extension tutorial when its menu item is chosen.
			 *
			 * @param event Unused.
			 */
			@Override
			public void actionPerformed(ActionEvent event) {
				new Tutorial(JLSInfo.frame,3);
			}
		});

		// set up help contents viewer
		JMenuItem contents = new JMenuItem("Contents");
		contents.setMnemonic(KeyEvent.VK_C);
		help.add(contents);
		contents.addActionListener(new ActionListener() {
			/**
			 * Open the help contents viewer when the Contents menu item is
			 * chosen.
			 *
			 * @param event Unused.
			 */
			@Override
			public void actionPerformed(ActionEvent event) {
				Help.showTopic("top");
			}
		});
		return help;
	} // end of helpMenu method

	/**
	 * Create a new circuit.
	 */
	private void newCircuit() {

		String name = TellUser.prompt(this, "Enter circuit name (without .jls)");
		if (name == null || name.isEmpty())
			return;
		if (!Util.isValidName(name)) {
			TellUser.error(JLSInfo.frame,"Invalid circuit name - must have only letters, digits & _", "Error");
			return;
		}

		// strip .jls if there
		name = name.replaceAll("\\.jls$","");

		// don't allow duplicate names
		if (duplicateName(name))
			return;

		// create circuit and set up editor
		Circuit circ = new Circuit(name);
		circ.setDirectory(Util.defaultDirectory());
		if (exHandler != null)
			exHandler.setCircuit(circ);
		setupEditor(circ,name);
	} // end of newCircuit method

	/** The directory of the previous file opened. */
	private String prevOpenDir = "";  // the directory of the previous file opened

	/**
	 * Open an existing circuit.
	 * 
	 * @param filePath The name of the circuit.  If null, then prompt user for
	 * the name.
	 */
	private void open(@Nullable String filePath) {

		File file = null;
		String dir = "";

		// get circuit name from user if parameter is null
		if (filePath == null) {
			prevOpenDir = Util.seedDirectory(prevOpenDir);
			JFileChooser chooser = new JFileChooser( prevOpenDir );
			
			javax.swing.filechooser.FileFilter filter =
				new javax.swing.filechooser.FileFilter() {
				/**
				 * Accept JLS circuit files and directories in the open
				 * dialog.
				 *
				 * @param f The file to test.
				 *
				 * @return true if f is a .jls/.jls~ file or a directory.
				 */
				@Override
				public boolean accept(File f) {
					return f.getName().endsWith(".jls") || f.getName().endsWith(".jls~")
					|| f.isDirectory();
				}
				/**
				 * The open dialog file filter description.
				 *
				 * @return the description text.
				 */
				@Override
				public String getDescription() {
					return "JLS Circuit Files";
				}
			};
			
			chooser.setFileFilter(filter);
			if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) 
				return;
			file = chooser.getSelectedFile();
			filePath = file.getAbsolutePath();
			if (filePath == null || filePath.isEmpty())
				return;
			prevOpenDir = chooser.getCurrentDirectory().toString();
			dir = prevOpenDir;
		}else {
			file = new File(filePath);
			// a bare relative filename has no parent; it was resolved
			// against the working directory (this branch only runs for a
			// command-line start file), so user.dir - not user.home - IS
			// the file's real directory.  Deliberately kept on user.dir;
			// see issue #130's H1 exclusion.
			String parent = file.getParent();
			prevOpenDir = (parent == null || parent.isEmpty())
					? System.getProperty("user.dir") : parent;
			dir = prevOpenDir;
		}
		
		Scanner input = FileAbstractor.openCircuit(filePath);
		if (input == null) {
			TellUser.error(this,
					"can't open " + filePath + ": " + JLSInfo.loadError, "Error");
			return;
		}

		String cname;
		cname = file.getName().replaceAll("\\.jls~$", "");
		cname = cname.replaceAll("\\.jls$", "");

		// create new circuit
		Circuit circ = new Circuit(cname);
		circ.setDirectory(dir);

		// read circuit from file
		boolean loadOK = circ.load(input);
		if (loadOK && input.hasNext()) {
			// file shouldn't have anything after ENDCIRCUIT; without
			// a message the failure would be reported blank (#58)
			loadOK = false;
			JLSInfo.setLoadError(LoadError.of(
					LoadError.Category.MALFORMED,
					"there is extra content after the ENDCIRCUIT trailer",
					"The file may contain more than one circuit or "
							+ "trailing garbage; re-save it from JLS."));
		}
		input.close();
		if (!loadOK) {
			TellUser.error(this,
					// the message carries the line number itself (#58)
					filePath + " is not a valid circuit file: " + JLSInfo.loadError, "Error");
			return;
		}

		// delete checkpoint file if there is one (beside the opened file)
		new File(dir, cname + ".jls~").delete();

		// create editor
		if (exHandler != null)
			exHandler.setCircuit(circ);
		setupEditor(circ,cname);
	} // end of open method

	/**
	 * Set up editor window
	 * 
	 * @param circ The circuit the editor will edit.
	 * @param name The name of the circuit.
	 */
	public void setupEditor(Circuit circ, String name) {

		// create editor
		Editor ed = new Editor(edits,circ,name,clipboard);
		circ.setEditor(ed);

		// update all import menus
		for (Component edit : edits.getComponents()) {
			if (!(edit instanceof Editor otherEditor))
				continue;

			// add this circuit to another circuit's import menu
			otherEditor.addToImportMenu(circ);

			// add other non-imported circuits to this circuit's import menu
			if (!otherEditor.getCircuit().isImported())
				ed.addToImportMenu(otherEditor.getCircuit());
		}

		// show on screen
		edits.add(ed);
		edits.setSelectedComponent(ed);

	} // end of setupEditor method

	/**
	 * Close the currently visible editor.
	 * If the currently visible editor is editing a circuit (not a subcircuit) with
	 * subcircuits, close all editted subcircuits too.
	 * If the currently visible editor is editing a circuit that is not a subcircuit,
	 * remove the circuit's name from the import lists of all other editors of
	 * non-imported subcircuits.
	 */
	private void closeVisibleEditor() {

		// get the currently visible editor, and exit if there isn't one
		Editor ed = (Editor)(edits.getSelectedComponent());
		if (ed == null)
			return;
		ed.close();

		// get newly visible editor (if one) and tell exception handler
		ed = (Editor)(edits.getSelectedComponent());
		if (ed == null) {
			if (exHandler != null)
				exHandler.setCircuit(null);
		}
		else {
			Circuit c = ed.getCircuit();
			SubCircuit sub;
			while ((sub = c.getSubElement()) != null)
				c = sub.getCircuit();
			if (exHandler != null)
				exHandler.setCircuit(c);
		}
	} // end of close method

	/**
	 * Assemble the {@link Book} a "Print..." menu item would hand to a
	 * {@link PrinterJob}, without showing the interactive print dialog
	 * or submitting anything to a printer. Extracted from
	 * {@link #print(boolean)} so the editor-driven print path - which
	 * circuit the currently selected {@link Editor} tab hands over, and
	 * whether all subcircuits are booked or just the visible one - is
	 * exercisable headlessly (issue #56).
	 *
	 * @param all True to book the entire circuit, subcircuits included
	 *        (the "Entire circuit" menu item); false to book only the
	 *        currently visible circuit (the "Just visible window" menu
	 *        item).
	 * @return The assembled book, or null if no editor tab is selected.
	 */
	@Nullable Book assemblePrintBook(boolean all) {

		// get the currently selected editor, return if none
		Editor ed = (Editor)(edits.getSelectedComponent());
		if (ed == null)
			return null;

		// page format doesn't need a printer job instance beyond this call
		PageFormat format = PrinterJob.getPrinterJob().defaultPage();
		format.setOrientation(PageFormat.LANDSCAPE);
		Book book = new Book();

		// find all unique sub-circuits (and their state machines)
		if (all) {
			ed.getCircuit().addToBook(book,format);
		}
		else {
			book.append(ed.getCircuit(),format);
		}

		return book;
	} // end of assemblePrintBook method

	/**
	 * Print circuit currently being edited, plus any state machines.
	 *
	 * @param all True to print the entire circuit, false to print just what's visible.
	 */
	public void print(boolean all) {

		Book book = assemblePrintBook(all);
		if (book == null)
			return;

		// set up printer job
		PrinterJob job = PrinterJob.getPrinterJob();
		job.setPageable(book);

		// show dialog, and if ok, print
		if (job.printDialog()) {
			try {
				job.print();
			}
			catch (PrinterException ex) {
				System.out.println("printing error: " + ex.getMessage());
			}
		}
	} // end of print method

	/**
	 * Check for duplicate of circuit already being edited.
	 * 
	 * @param name The new name.
	 * 
	 * @return true if a duplicate, false if not.
	 */
	public boolean duplicateName(String name) {

		for (int e=0; e<edits.getTabCount(); e+=1) {
			if (name.equals(edits.getTitleAt(e))) {
				TellUser.error(this,
						name + " is already being editted", "Error");
				return true;
			}
		}
		return false;
	} // end of duplicateName method

	
	/**
	 * Import a circuit from a file into this circuit.
	 * @throws Exception if an unexpected problem stops the import.
	 */
	public void fileImport() throws Exception {

		Editor ed = (Editor)edits.getSelectedComponent();
		if (ed == null) {
			TellUser.error(this,
					"no circuit to import into", "Error");
			return;
		}
		JFileChooser chooser = new JFileChooser(Util.defaultDirectory());
		javax.swing.filechooser.FileFilter filter =
			new javax.swing.filechooser.FileFilter() {
			/**
			 * Accept JLS circuit files and directories in the import
			 * dialog.
			 *
			 * @param f The file to test.
			 *
			 * @return true if f is a .jls file or a directory.
			 */
			@Override
			public boolean accept(File f) {
				return f.getName().endsWith(".jls") || f.isDirectory();
			}
			/**
			 * The import dialog file filter description.
			 *
			 * @return the description text.
			 */
			@Override
			public String getDescription() {
				return "JLS Circuit Files";
			}
		};
		chooser.setFileFilter(filter);
		if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) 
			return;
		
		Scanner input = FileAbstractor.openCircuit(chooser.getSelectedFile().getAbsolutePath());
		if (input == null) {
			TellUser.error(this,
					"can't open " + chooser.getSelectedFile().getName() + ": " + JLSInfo.loadError, "Error");
			return;
		}

		// create new circuit
		Circuit circ = new Circuit(chooser.getSelectedFile().getName().trim().replaceAll("\\.jls$",""));

		// read circuit from file
		boolean loadOK = circ.load(input);
		if (loadOK && input.hasNext()) {
			// file shouldn't have anything after ENDCIRCUIT; without
			// a message the failure would be reported blank (#58)
			loadOK = false;
			JLSInfo.setLoadError(LoadError.of(
					LoadError.Category.MALFORMED,
					"there is extra content after the ENDCIRCUIT trailer",
					"The file may contain more than one circuit or "
							+ "trailing garbage; re-save it from JLS."));
		}
		input.close();
		if (!loadOK) {
			TellUser.error(this,
					circ.getName() + " is not a valid circuit file: "
							+ JLSInfo.loadError, "Error");
			return;
		}
		try {
			if (!circ.finishLoad(null)) {
				TellUser.error(this,
						"can't import " + circ.getName() + ": "
								+ JLSInfo.loadError, "Error");
				return;
			}
		} catch (Exception ex) {
			TellUser.error(this,
					"can't import " + circ.getName() + ": " + ex.getMessage(),
					"Error");
			return;
		}

		ed.finishImport(circ);
	} // end of fileImport method

	/**
	 * Check for parameter file in the current directory,
	 * 
	 * @param paramFile The name of the file containing JLS parameters.
	 * @param circuit The circuit to apply the parameters too.
	 */
	public static void processParamFile(String paramFile, Circuit circuit) {

		try {

			// open file, set up miscellaneous stuff
			FileInputStream input = new FileInputStream(paramFile);
			Scanner scan = new Scanner(input, StandardCharsets.UTF_8);

			// read info
			while (scan.hasNext()) {
				String key = scan.next();

				// process TYPE command
				if (key.equals("TYPE")) {

					Class<?> cl = null;

					// get element type
					if (!scan.hasNext()) {
						System.out.print(paramFile + ": expected element type,");
						System.out.println(" got end of file");
						System.exit(1);
					}
					String type = scan.next();
					try {
						cl = Class.forName("jls.elem." + type);
					}
					catch (ClassNotFoundException ex) {
						System.out.print(paramFile + ": expected element type,");
						System.out.println(" got \"" + type + "\"");
						System.exit(1);
						return;
					}
					if (!LogicElement.class.isAssignableFrom(cl)) {
						// e.g. TYPE Wire used to crash on an unguarded
						// cast in setPropDelays (issue #38)
						System.out.print(paramFile + ": " + type);
						System.out.println(" is not a simulated element type");
						System.exit(1);
					}

					// get PROPDELAY
					if (!scan.hasNext()) {
						System.out.print(paramFile + ": expected PROPDELAY,");
						System.out.println(" got end of file");
						System.exit(1);
					}
					String word = scan.next();
					if (!word.equals("PROPDELAY")) {
						System.out.print(paramFile + ": expected PROPDELAY,");
						System.out.println(" got \"" + word + "\"");
						System.exit(1);
					}

					// get propagation delay value
					if (!scan.hasNext()) {
						System.out.print(paramFile + ": expected propagation delay,");
						System.out.println(" got end of file");
						System.exit(1);
					}
					if (!scan.hasNextInt()) {
						System.out.print(paramFile + ": expected propagation delay,");
						System.out.println(" got \"" + scan.next() + "\"");
						System.exit(1);
					}

					// get delay value for this type
					int delay = scan.nextInt();
					if (delay < 1) {
						System.out.print(paramFile + ": expected propagation delay > 0,");
						System.out.println(" got \"" + delay + "\"");
						System.exit(1);
					}

					// send to all elements of this type
					setPropDelays(circuit, cl, delay);

				}

				// process ELEMENT command
				else if (key.equals("ELEMENT")) {

					// get name
					if (!scan.hasNext()) {
						System.out.print(paramFile + ": expected element name,");
						System.out.println(" got end of file");
						System.exit(1);
					}
					String name = scan.next();
					Vector<String> qualifiedName = parseName(name);
					if (qualifiedName == null) {
						System.out.println(paramFile + ": invalid element name " + name);
						System.exit(1);
						return;
					}

					// run down into subcircuits
					String elementName = qualifiedName.remove(qualifiedName.size()-1);
					Circuit circ = circuit;
					if (qualifiedName.size() > 0) {
						for (String sub : qualifiedName) {
							Circuit next = null;
							for (Element el : circ.getElements()) {
								if (el instanceof SubCircuit lel) {
									if (sub.equals(lel.getName())) {
										next = lel.getSubCircuit();
										break;
									}
								}
							}
							if (next == null) {
								System.out.println(paramFile + ": no such element name " + name);
								System.exit(1);
								return;
							}
							circ = next;
						}
					}

					// look for element in the appropriate circuit or subcircuit
					LogicElement element = null;
					for (Element el : circ.getElements()) {
						if (el instanceof LogicElement lel) {
							if (elementName.equals(lel.getName())) {
								element = lel;
								break;
							}
						}
					}
					if (element == null) {
						System.out.print(paramFile + ": no such element named");
						System.out.println(" \"" + name + "\"");
						System.exit(1);
						return;
					}
					if (!scan.hasNext()) {
						System.out.print(paramFile + ": expected element property,");
						System.out.println(" got end of file");
						System.exit(1);
					}

					// get element property to change
					String prop = scan.next();
					if (prop.equals("WATCHED")) {

						// get watched info
						if (!scan.hasNext()) {
							System.out.print(paramFile + ": expected true or false,");
							System.out.println(" got end of file");
							System.exit(1);
						}
						String tf = scan.next();
						if (tf.equals("true")) {
							element.setWatched(true);
						}
						else if (tf.equals("false")) {
							element.setWatched(false);
						}
						else {
							System.out.print(paramFile + ": expected true or false,");
							System.out.println(" got \"" + tf + "\"");
							System.exit(1);
						}
					}

					// propdelay info
					else if (prop.equals("PROPDELAY")) {

						if (!scan.hasNext()) {
							System.out.print(paramFile + ": expected prop delay value,");
							System.out.println(" got end of file");
							System.exit(1);
						}
						if (!scan.hasNextInt()) {
							System.out.print(paramFile + ": expected prop delay value,");
							System.out.println(" got \"" + scan.next() + "\"");
							System.exit(1);
						}
						int delay = scan.nextInt();
						if (delay < 1) {
							System.out.print(paramFile + ": expected prop delay > 0,");
							System.out.println(" got \"" + delay + "\"");
							System.exit(1);
						}
						element.setDelay(delay);
					}

					// register info
					else if (prop.equals("INITIALLY")) {
						if (!(element instanceof Register)) {
							System.out.println(name + " is not a Register element");
							System.exit(1);
						}
						if (!scan.hasNextBigInteger()) {
							System.out.print(paramFile + ": expected initial value,");
							System.out.println(" got end of file");
							System.exit(1);
						}
						BigInteger init = scan.nextBigInteger();
						Register reg = (Register)element;
						reg.setInitialValue(init);
					}

					// memory file info
					else if (prop.equals("FILENAME")) {
						if (!(element instanceof Memory)) {
							System.out.println(name + " is not a Memory element");
							System.exit(1);
						}
						if (!scan.hasNext()) {
							System.out.print(paramFile + ": expected memory file name,");
							System.out.println(" got end of file");
							System.exit(1);
						}
						String file = scan.next();
						Memory mem = (Memory)element;
						mem.setMemFile(file);
					}

					// error
					else {
						System.out.print(paramFile + ": expected element property,");
						System.out.println(" got \"" + prop + "\"");
						System.exit(1);
					}
				}

				// process CLEAR command
				else if (key.equals("CLEAR")) {
					if (!scan.hasNext()) {
						System.out.print(paramFile + ": expected WATCHES or PROBES,");
						System.out.println(" got end of file");
						System.exit(1);
					}
					String word = scan.next();
					if (!word.equals("WATCHES") && !word.equals("PROBES")) {
						System.out.print(paramFile + ": expected WATCHES or PROBES,");
						System.out.println(" got \"" + word + "\"");
						System.exit(1);
					}

					if (word.equals("WATCHES")) {

						// clear all watched flags in the circuit
						circuit.clearAllWatches();
					}
					else {

						// clear all probes in the circuit
						circuit.removeAllProbes();
					}
				}


				// process RESET command
				else if (key.equals("RESET")) {
					if (!scan.hasNext()) {
						System.out.print(paramFile + ": expected PROPDELAYS,");
						System.out.println(" got end of file");
						System.exit(1);
					}
					String word = scan.next();
					if (word.equals("PROPDELAYS")) {

						// reset all propagation delays in the circuit
						circuit.resetAllDelays();
					}
					else {
						System.out.print(paramFile + ": expected PROPDELAYS,");
						System.out.println(" got " + word);
						System.exit(1);
					}
				}

				// else an invalid command
				else {
					System.out.println(paramFile + ": invalid command [" + key + "]");
					System.exit(1);
				}

			}
			scan.close();
		}
		catch (FileNotFoundException ex) {
			System.out.println("warning: can't open parameter file " + paramFile + ", file ignored");
			// do nothing if the file does not exist or can't be read
		}


		catch (InputMismatchException ex) {
			// should not happen, ignore if it does
		}

	} // end of processParamFile method

	/**
	 * Set the propagation delay of all elements of a certain type
	 * in a circuit and its subcircuits.
	 * 
	 * @param circ The circuit.
	 * @param cl The type (class) of element to change.
	 * @param delay The new propagation delay.
	 */
	public static void setPropDelays(Circuit circ, Class<?> cl, int delay) {

		for (Element el : circ.getElements()) {
			if (el instanceof SubCircuit sub) {
				Circuit c = sub.getSubCircuit();
				setPropDelays(c,cl,delay);
			}
			else if (el.getClass() == cl && el instanceof LogicElement lel) {
				lel.setDelay(delay);
			}
		}
	} // end of setPropDelays method

	/**
	 * Print the circuit specified in the start file.
	 * 
	 * @param justTop True if just the top level of the circuit is to be printed, false if the whole thing.
	 */
	private static void printCirc(boolean justTop) {

		if (startFile == null) {
			System.err.println("jls: error: printing requires a circuit file");
			System.exit(1);
			return;
		}
		String name = startFile.replaceAll("\\.jls$","");

		// open and load the named file through the standard sniffing
		// loader, as batch mode does - this path used to print an empty
		// circuit because it never read the file at all (issue #48)
		Scanner input = FileAbstractor.openCircuit(startFile);
		if (input == null) {
			System.err.println("can't open " + startFile
					+ ": " + JLSInfo.loadError);
			System.exit(1);
			return;
		}
		Circuit circ = new Circuit(name);
		boolean loadOK = circ.load(input);
		if (loadOK && input.hasNext()) {
			// file shouldn't have anything after ENDCIRCUIT; without
			// a message the failure would be reported blank (#58)
			loadOK = false;
			JLSInfo.setLoadError(LoadError.of(
					LoadError.Category.MALFORMED,
					"there is extra content after the ENDCIRCUIT trailer",
					"The file may contain more than one circuit or "
							+ "trailing garbage; re-save it from JLS."));
		}
		input.close();
		if (!loadOK) {
			System.err.println(startFile + " is not a valid circuit file: "
					+ JLSInfo.loadError);
			System.exit(1);
		}
		try {
			if (!circ.finishLoad(null)) {
				System.err.println(startFile + " is not a valid circuit file: "
						+ JLSInfo.loadError);
				System.exit(1);
			}
		} catch (Exception e) {
			System.err.println(startFile + " is not a valid circuit file: "
					+ e.getMessage());
			e.printStackTrace();
			System.exit(1);
		}

		// set up printer job
		PrinterJob job = PrinterJob.getPrinterJob();
		PrintService [] services = PrinterJob.lookupPrintServices();
		PrintService want = null;
		for (PrintService s : services) {
			if (s.getName().equals(printer)) {
				want = s;
			}
		}
		if (want ==  null) {
			System.out.println(printer + " is an invalid printer");
			System.exit(1);
		}
		try {
			job.setPrintService(want);
		}
		catch (PrinterException ex) {
			System.out.println(printer + " is an invalid printer");
		}
		PageFormat format = job.defaultPage();
		format.setOrientation(PageFormat.LANDSCAPE);
		Book book = new Book();

		// print either the top level or the entire circuit
		if (justTop) {
			book.append(circ,format);
		}
		else {
			circ.addToBook(book,format);
		}

		// finish up book
		job.setPageable(book);

		// print the circuit
		try {
			job.print();
		}
		catch (PrinterException ex) {
			System.out.println("printing error: " + ex.getMessage());
		}
	} // end of printCirc method

	/**
	 * Write an image of the circuit to a file.
	 * @throws Exception if the image file cannot be written.
	 */
	public void exportImage() throws Exception {

		Editor ed = (Editor)edits.getSelectedComponent();
		if (ed == null)
			return;

		// get name from user
		JFileChooser chooser = new JFileChooser(Util.defaultDirectory());
		javax.swing.filechooser.FileFilter filter =
			new javax.swing.filechooser.FileFilter() {
			/**
			 * Accept JPEG image files and directories in the export
			 * dialog.
			 *
			 * @param f The file to test.
			 *
			 * @return true if f is a .jpg file or a directory.
			 */
			@Override
			public boolean accept(File f) {
				return f.getName().endsWith(".jpg") || f.isDirectory();
			}
			/**
			 * The export image dialog file filter description.
			 *
			 * @return the description text.
			 */
			@Override
			public String getDescription() {
				return "JLS Circuit Images";
			}
		};
		chooser.setFileFilter(filter);
		if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) 
			return;
		String fileName = chooser.getSelectedFile().getName().trim();
		if (fileName == null || fileName.isEmpty())
			return;
		String tempName = fileName.replaceAll("\\.jpg$","");
		if (!Util.isValidName(tempName)) {
			TellUser.error(JLSInfo.frame,"Invalid file name - must contain only letters, digits & _", "Error");
			return;
		}
		if (!fileName.endsWith(".jpg")) {
			fileName = fileName + ".jpg";
		}
		fileName = chooser.getCurrentDirectory() + "/" + fileName;

		// export the image
		Circuit circ = ed.getCircuit();
		circ.exportImage(fileName);
	} // end of exportImage method

	/**
	 * Break compound name (a.b.c) into components.
	 * 
	 * @param name The compound name.
	 * 
	 * @return the components of the name, or null if the name is not valid.
	 */
	public static @Nullable Vector<String> parseName(String name) {

		Vector<String> comp = new Vector<String>();
		int first = 0;
		for (int p=0; p<name.length(); p+=1) {
			if (name.charAt(p) == '.') {
				String component = name.substring(first,p);
				if (!Util.isValidName(component))
					return null;
				if (component.isEmpty())
					return null;
				comp.add(component);
				first = p+1;
			}
		}
		if (name.charAt(name.length()-1) == '.')
			return null;
		String component = name.substring(first,name.length());
		if (!Util.isValidName(component))
			return null;
		comp.add(component);
		return comp;
	} // end of parseName method

} // end of JLSStart class
