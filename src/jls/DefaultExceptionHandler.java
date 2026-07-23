package jls;

import java.nio.charset.StandardCharsets;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import org.jspecify.annotations.Nullable;

/**
 * Handle unexpected errors and exceptions.
 * 
 * @author David A. Poplawski
 */
public final class DefaultExceptionHandler implements Thread.UncaughtExceptionHandler {
	
	// properties
	/** True once an exception is being handled, so a second one just exits. */
	private boolean recovering = false;
	/** Reference to the main class, or null when running in batch mode. */
	private @Nullable JLSStart jls = null;
	/** The circuit to dump with the stack trace, if any. */
	private @Nullable Circuit circuit = null;
	/** Memory released when handling an OutOfMemoryError so recovery can proceed. */
	private int @Nullable [] extraSpace = new int[10000];

	// -- termination seams (issue #208) ---------------------------------
	// The interactive branches show a modal dialog and only then exit. On
	// an unattended display (the Xvfb UI-CI lane), no human dismisses the
	// dialog, so the modal blocks the handler thread forever and the exit
	// never runs - the process hangs until a wall-clock timeout instead of
	// failing fast (issue #208). GraphicsEnvironment.isHeadless() is false
	// under Xvfb, so it cannot tell a CI lane from a real user; rather than
	// classify the session, we guarantee a bounded exit unconditionally: a
	// daemon watchdog force-terminates after a short interval, so a
	// dismissed dialog exits immediately and an unattended one exits when
	// the watchdog fires. These fields are package-private seams the tests
	// drive (a blocking dialog + recording exiters) without a real display.

	/** Ensures exactly one termination path runs, whichever fires first. */
	private final AtomicBoolean terminated = new AtomicBoolean(false);
	/** Orderly, immediate exit (dialog dismissed / batch path). */
	IntConsumer exiter = System::exit;
	/** Hard exit the watchdog uses, in case shutdown hooks also hang. */
	IntConsumer hardExiter = code -> Runtime.getRuntime().halt(code);
	/** Runs the modal dialog; may block indefinitely when unattended. */
	Consumer<Runnable> dialogRunner = Runnable::run;
	/** How long the watchdog waits before forcing termination. */
	long watchdogMillis = Long.getLong("jls.exitWatchdogMillis", 5000L);
	/**
	 * Forces the interactive branch even without a real {@link JLSStart}
	 * (test seam); null means the live {@code jls != null} decision.
	 */
	@Nullable Boolean interactiveOverride = null;

	/**
	 * Create a handler with no JLS window or circuit attached yet;
	 * setJLS and setCircuit supply them once they exist.
	 */
	public DefaultExceptionHandler() {
	} // end of constructor

	/**
	 * Save reference to JLS in case circuit(s) can be saved.
	 * 
	 * @param jls A reference to the main class.
	 */
	public void setJLS(JLSStart jls) {
		
		this.jls = jls;
	} // end of setJLS method
	
	/**
	 * Save reference to circuit for trace.
	 * 
	 * @param circ The circuit.
	 */
	public void setCircuit(@Nullable Circuit circ) {

		circuit = circ;
	} // end of setCircuit method

	/**
	 * Show a dialog and print a stack trace to a file.
	 * While debugging print a stack trace to the console.
	 * 
	 * @param t Unused.
	 * @param th A throwable containing the exception/error information.
	 */
	@Override
	public void uncaughtException(Thread t, Throwable th) {

		// if already trying to recover, forget it
		if (recovering) {
			terminate(1, false);
			return;
		}
		recovering = true;

		boolean interactive = interactiveOverride != null
				? interactiveOverride.booleanValue()
				: (jls != null);

		// if out of memory
		if (th instanceof OutOfMemoryError) {

			// free up some memory and garbage collect
			extraSpace = null;
			System.gc();

			// if batch, print message and quit
			if (!interactive) {
				System.out.println("Not enough memory to simulate circuit");
				System.out.println("Run JLS again and give JVM more memory");
				terminate(1, false);
			}

			// otherwise display message and quit -- but arm the watchdog
			// first so an unattended display cannot hang here (issue #208);
			// the branch has already freed extraSpace for this path
			else {
				armWatchdog();
				dialogRunner.accept(() -> {
					TellUser.error(null, "Not enough memory", "Error");
					TellUser.warn(null,
							"Run JLS again and give JVM more memory",
							"Warning");
				});
				terminate(1, false);
			}
		}

		// all other errors/exceptions...
		else {

			// if batch, print message and quit
			if (!interactive) {
				saveTrace(th);
				System.out.println("UNEXPECTED INTERNAL ERROR!");
				System.out.println("JLS will create a file called JLSerror in the current folder/directory.");
				System.out.println("Please attach it to a bug report at https://github.com/anadon/JLS/issues so it can be fixed.");
				terminate(1, false);
			}

			// otherwise show message and quit. The trace is written before
			// the dialog, and the watchdog is armed before it, so the
			// JLSerror file lands and the process exits within a bounded
			// time whether or not a human ever dismisses the dialog (#208)
			else {
				saveTrace(th);
				armWatchdog();
				String msg = "<html>" +
					"UNEXPECTED INTERNAL ERROR! Try to save circuit(s)." +
					"<p>" +
					"JLS will create a file called JLSerror in the current folder/directory." +
					"<br>Please attach it to a bug report at https://github.com/anadon/JLS/issues so it can be fixed." +
					"<br><br>Try restarting JLS using checkpoints of open circuits" +
					"<br>(i.e., <i>file</i>.jls~, where <i>file</i> is the name of the open circuit)" +
					"</html>";
				dialogRunner.accept(() -> TellUser.error(null, msg, "Error"));
				terminate(1, false);
			}
		}
	} // end of uncaughtException method

	/**
	 * Terminate the process exactly once, whichever path fires first
	 * (issue #208). The immediate branches call this after their dialog;
	 * the watchdog calls it with {@code hard} true to bypass shutdown
	 * hooks that might themselves hang.
	 *
	 * @param code The exit status.
	 * @param hard True to use the hard-halt exiter (the watchdog path).
	 */
	private void terminate(int code, boolean hard) {
		if (terminated.compareAndSet(false, true)) {
			(hard ? hardExiter : exiter).accept(code);
		}
	} // end of terminate method

	/**
	 * Arm a daemon watchdog that force-terminates after
	 * {@link #watchdogMillis}, so a modal error dialog on an unattended
	 * display (the Xvfb UI-CI lane) cannot block the handler thread past
	 * a bounded time (issue #208). If the dialog is dismissed first, the
	 * immediate {@link #terminate} wins and this fires harmlessly into the
	 * already-set {@code terminated} guard.
	 */
	private void armWatchdog() {
		Thread watchdog = new Thread(() -> {
			try {
				Thread.sleep(watchdogMillis);
			} catch (InterruptedException ignored) {
				return;
			}
			terminate(1, true);
		}, "jls-exit-watchdog");
		watchdog.setDaemon(true);
		watchdog.start();
	} // end of armWatchdog method
	
	/**
	 * Save stack trace in a file.
	 * 
	 * @param th The throwable (exception/error).
	 */
	protected void saveTrace(Throwable th) {
		
		//th.printStackTrace(); // remove this when not debugging
		try {
			PrintWriter out = new PrintWriter("JLSerror", StandardCharsets.UTF_8);
			out.println("Please attach this file to a bug report at");
			out.println("https://github.com/anadon/JLS/issues along with a");
			out.println("short description of what you were doing when the");
			out.println("error occurred. Thanks.");
			out.println("JLS " + JLSInfo.versionString);
			// only environment facts that help diagnose a crash: the full
			// System.getProperties() dump leaked user/host details into a
			// file users are told to share (issue #51)
			for (String key : new String[] { "java.version", "java.vendor",
					"java.vm.name", "os.name", "os.version", "os.arch" }) {
				out.println(key + "=" + System.getProperty(key));
			}
			th.printStackTrace(out);
			// Circuit.save's only mutation is the per-pass element-ID
			// renumbering every ordinary save also performs; it does not
			// touch the changed flag, so dumping the circuit here does
			// not alter user-visible state (issue #51)
			if (circuit != null)
				circuit.save(out);
			out.close();
		}
		catch (IOException ex) {
			System.out.println("Can't create JLSerror file");
		}
	} // end of saveTrace method
	
} // end of DefaultExceptionHander class
