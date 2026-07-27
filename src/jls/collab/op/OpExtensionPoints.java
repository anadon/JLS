package jls.collab.op;

import jls.module.ExtensionPoint;

/**
 * The extension points published by the operation layer (issue #223):
 * the typed identities modules contribute op-stream consumers
 * through. Constants live here, in the seam's home package, so
 * {@code jls.module} stays a pure mechanism that names no contract —
 * the host layer owns its own seams. Catalogued in
 * {@code docs/extension-points.md} and pinned by
 * {@code ExtensionPointCatalogTest}.
 */
public final class OpExtensionPoints {

	/**
	 * The op-observer seam (issue #167, grand-architecture §4.3 seam
	 * 5): contributions are {@link OpSink} implementations — the
	 * receiving side of the mutation stream. The editor submits every
	 * {@link CircuitOp} into the sink; a contributed sink is a
	 * consumer of that stream (undo snapshots, checkpointing,
	 * collaboration replication #171, per-peer attribution) observing
	 * the same seam the editor writes.
	 */
	public static final ExtensionPoint<OpSink> OP_OBSERVER =
			new ExtensionPoint<OpSink>("collab.op-observer",
					OpSink.class);

	/** Not instantiable: a constant holder only. */
	private OpExtensionPoints() {
	} // end of constructor

} // end of OpExtensionPoints class
