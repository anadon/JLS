/**
 * Root package of JLS, a digital logic-circuit simulator. It holds the
 * application entry point and main window ({@link jls.JLS},
 * {@link jls.JLSStart}), the core {@link jls.Circuit} model that owns a
 * circuit's elements and drives its simulation, and the project-wide constants
 * in {@link jls.JLSInfo}. Alongside these sit the shared infrastructure the
 * rest of the program depends on: circuit-file reading and writing
 * ({@link jls.FileAbstractor}), user-facing dialogs and help (About, Help,
 * Tutorial, KeyPad, TellUser), and general utilities such as {@link jls.Util},
 * {@link jls.BitSetUtils}, and {@link jls.SpatialIndex}. The functional
 * subsystems live in the subpackages this package coordinates: {@code jls.edit}
 * (interactive editors), {@code jls.elem} (logic elements), {@code jls.sim}
 * (the simulator), and {@code jls.hdl} (Verilog/VHDL export).
 */
package jls;
