package jls.module;

/**
 * When a module's logic comes alive (issue #220, grand-architecture
 * §4.2 rule 7): the default is a lazy trigger — a command, an event,
 * or first demand — and only the true kernel core opts into
 * {@link Eager} boot-time activation. This mirrors VS Code
 * {@code activationEvents} and Emacs autoloads, and it is the literal
 * meaning of issue #212's demand gate: a module registers a cheap shim
 * and its real start runs on first use.
 *
 * <p>Data-only in this slice: the manifest records which trigger a
 * module declares; the runner that honors the trigger arrives with the
 * lifecycle slice.</p>
 */
public sealed interface Activation {

	/**
	 * Activate at boot, in topological order. Reserved for the true
	 * kernel core (core model, configuration, look-and-feel).
	 */
	record Eager() implements Activation {
	} // end of Eager record

	/**
	 * Activate the first time a named command is invoked.
	 *
	 * @param command The command identifier that triggers activation;
	 *            must be non-blank.
	 */
	record OnCommand(String command) implements Activation {

		/**
		 * Reject blank command identifiers.
		 *
		 * @throws IllegalArgumentException if the command is blank.
		 */
		public OnCommand {

			if (command.isBlank()) {
				throw new IllegalArgumentException(
						"blank activation command");
			}
		} // end of compact constructor

	} // end of OnCommand record

	/**
	 * Activate the first time a named event fires.
	 *
	 * @param event The event identifier that triggers activation; must
	 *            be non-blank.
	 */
	record OnEvent(String event) implements Activation {

		/**
		 * Reject blank event identifiers.
		 *
		 * @throws IllegalArgumentException if the event is blank.
		 */
		public OnEvent {

			if (event.isBlank()) {
				throw new IllegalArgumentException(
						"blank activation event");
			}
		} // end of compact constructor

	} // end of OnEvent record

	/**
	 * Activate on first real use of anything the module provides — the
	 * most lazy trigger, and the default posture for non-core modules.
	 */
	record OnDemand() implements Activation {
	} // end of OnDemand record

} // end of Activation interface
