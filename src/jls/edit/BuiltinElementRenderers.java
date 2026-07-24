package jls.edit;

/**
 * The single registration point for the GUI-side element renderers and
 * creation dialogs extracted by the #77 element long-tail. As each
 * element is converted to a headless model, its renderer and (if any)
 * its dialog are registered here; {@link ElementRenderers} and
 * {@link ElementDialogs} each trigger this once, before any draw or
 * placement, so the registrations are in place for both the interactive
 * GUI and the headless image-export path.
 *
 * <p>Empty today (issue #77 W0 P0.5): the registry seams exist and every
 * element still draws and sets itself up, so behavior is unchanged. The
 * element wave adds one {@code ElementRenderers.register(...)} (and, for
 * elements with a dialog, {@code ElementDialogs.register(...)}) line here
 * per converted element.
 */
final class BuiltinElementRenderers {

	/** True once the registrations have run, so it happens exactly once. */
	private static boolean installed = false;

	private BuiltinElementRenderers() {} // no instances

	/**
	 * Register every extracted element renderer and dialog. Idempotent.
	 */
	static synchronized void install() {
		if (installed) {
			return;
		}
		installed = true;

		// The element wave (#77 W2) registers converted elements here:
		//   ElementRenderers.register(Foo.class, new FooRenderer());
		//   ElementDialogs.register(Foo.class, new FooDialog());
		ElementRenderers.register(jls.elem.Adder.class, new AdderRenderer());
		ElementDialogs.register(jls.elem.Adder.class, new AdderDialog());
		ElementRenderers.register(jls.elem.TriState.class, new TriStateRenderer());
		ElementDialogs.register(jls.elem.TriState.class, new TriStateDialog());
		ElementRenderers.register(jls.elem.SigGen.class, new SigGenRenderer());
		ElementDialogs.register(jls.elem.SigGen.class, new SigGenDialog(true));
		ElementDialogs.registerChange(jls.elem.SigGen.class,
				new SigGenDialog(false));
		// Clock
		ElementRenderers.register(jls.elem.Clock.class, new ClockRenderer());
		ElementDialogs.register(jls.elem.Clock.class, new ClockDialog(true));
		ElementDialogs.registerChange(jls.elem.Clock.class,
				new ClockDialog(false));
		// Constant
		ElementRenderers.register(jls.elem.Constant.class,
				new ConstantRenderer());
		ElementDialogs.register(jls.elem.Constant.class,
				new ConstantDialog(true));
		ElementDialogs.registerChange(jls.elem.Constant.class,
				new ConstantDialog(false));
		// Decoder
		ElementRenderers.register(jls.elem.Decoder.class, new DecoderRenderer());
		ElementDialogs.register(jls.elem.Decoder.class, new DecoderDialog());
		// Display
		ElementRenderers.register(jls.elem.Display.class, new DisplayRenderer());
		ElementDialogs.register(jls.elem.Display.class, new DisplayDialog());
		// Mux
		ElementRenderers.register(jls.elem.Mux.class, new MuxRenderer());
		ElementDialogs.register(jls.elem.Mux.class, new MuxDialog());
		// Memory
		ElementRenderers.register(jls.elem.Memory.class, new MemoryRenderer());
		ElementDialogs.register(jls.elem.Memory.class, new MemoryDialog(true));
		ElementDialogs.registerChange(jls.elem.Memory.class,
				new MemoryDialog(false));
		// Register
		ElementRenderers.register(jls.elem.Register.class, new RegisterRenderer());
		ElementDialogs.register(jls.elem.Register.class, new RegisterDialog(true));
		ElementDialogs.registerChange(jls.elem.Register.class,
				new RegisterDialog(false));
		// Text
		ElementRenderers.register(jls.elem.Text.class, new TextRenderer());
		ElementDialogs.register(jls.elem.Text.class, new TextDialog(true));
		ElementDialogs.registerChange(jls.elem.Text.class,
				new TextDialog(false));
		// SubCircuit
		ElementRenderers.register(jls.elem.SubCircuit.class,
				new SubCircuitRenderer());
		ElementDialogs.register(jls.elem.SubCircuit.class,
				new SubCircuitDialog());
		// Stop
		ElementRenderers.register(jls.elem.Stop.class, new StopRenderer());
		ElementDialogs.register(jls.elem.Stop.class, new StopDialog());
		// Pause
		ElementRenderers.register(jls.elem.Pause.class, new PauseRenderer());
		ElementDialogs.register(jls.elem.Pause.class, new PauseDialog());
		// Group family
		ElementRenderers.register(jls.elem.Binder.class, new BinderRenderer());
		ElementDialogs.register(jls.elem.Binder.class, new GroupDialog());
		ElementRenderers.register(jls.elem.Splitter.class,
				new SplitterRenderer());
		ElementDialogs.register(jls.elem.Splitter.class, new GroupDialog());
		// Wire family
		ElementRenderers.register(jls.elem.Wire.class, new WireRenderer());
		ElementRenderers.register(jls.elem.WireEnd.class, new WireEndRenderer());
		// Jump family
		ElementRenderers.register(jls.elem.JumpStart.class,
				new JumpStartRenderer());
		ElementDialogs.register(jls.elem.JumpStart.class,
				new JumpStartDialog());
		ElementRenderers.register(jls.elem.JumpEnd.class, new JumpEndRenderer());
		ElementDialogs.register(jls.elem.JumpEnd.class, new JumpEndDialog());
		// State family
		ElementRenderers.register(jls.elem.StateMachine.class,
				new StateMachineRenderer());
		ElementDialogs.register(jls.elem.StateMachine.class,
				new StateMachineDialog(true));
		ElementDialogs.registerChange(jls.elem.StateMachine.class,
				new StateMachineDialog(false));
		// TruthTable family
		ElementRenderers.register(jls.elem.TruthTable.class,
				new TruthTableRenderer());
		ElementDialogs.register(jls.elem.TruthTable.class,
				new TruthTableDialog(true));
		ElementDialogs.registerChange(jls.elem.TruthTable.class,
				new TruthTableDialog(false));
		// ExtendDelay family
		ElementDialogs.register(jls.elem.DelayGate.class,
				new DelayGateDialog());
		// Gate family
		GateRenderer gateRenderer = new GateRenderer();
		GateDialog gateDialog = new GateDialog();
		ElementRenderers.register(jls.elem.AndGate.class, gateRenderer);
		ElementDialogs.register(jls.elem.AndGate.class, gateDialog);
		ElementRenderers.register(jls.elem.OrGate.class, gateRenderer);
		ElementDialogs.register(jls.elem.OrGate.class, gateDialog);
		ElementRenderers.register(jls.elem.NandGate.class, gateRenderer);
		ElementDialogs.register(jls.elem.NandGate.class, gateDialog);
		ElementRenderers.register(jls.elem.NorGate.class, gateRenderer);
		ElementDialogs.register(jls.elem.NorGate.class, gateDialog);
		ElementRenderers.register(jls.elem.XorGate.class, gateRenderer);
		ElementDialogs.register(jls.elem.XorGate.class, gateDialog);
		ElementRenderers.register(jls.elem.NotGate.class, gateRenderer);
		ElementDialogs.register(jls.elem.NotGate.class, gateDialog);
		ElementRenderers.register(jls.elem.Extend.class, gateRenderer);
		ElementDialogs.register(jls.elem.Extend.class, gateDialog);
		ElementRenderers.register(jls.elem.DelayGate.class, gateRenderer); // DelayGate keeps its existing DelayGateDialog
		// ShiftRegister
		ElementRenderers.register(jls.elem.ShiftRegister.class,
				new ShiftRegisterRenderer());
		// Pin family
		ElementRenderers.register(jls.elem.InputPin.class, new PinRenderer());
		ElementDialogs.register(jls.elem.InputPin.class, new PinDialog());
		ElementRenderers.register(jls.elem.OutputPin.class, new PinRenderer());
		ElementDialogs.register(jls.elem.OutputPin.class, new PinDialog());
	} // end of install method

} // end of BuiltinElementRenderers class
