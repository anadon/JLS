# Extension-point catalog (#223)

The host publishes typed extension points and never names a module —
the inversion-of-control seam recorded in
[`grand-architecture.md`](grand-architecture.md) §4.3. This page is
the catalog of those seams: the concrete API surface of the module
program. Each typed-now seam is a `jls.module.ExtensionPoint`
constant living in the seam's *home package* (never in `jls.module`,
which stays a pure, AWT-free mechanism), and contributions flow
through a `jls.module.ExtensionRegistry` declared over these
constants.

Rules of the catalog:

- **Ids are stable.** A point id is kebab-case, dot-prefixed by its
  home area (`elem.`, `hdl.`, `collab.`, `gui.`, `app.`), and never
  changes once shipped: manifests and diagnostics name it.
- **One constant per typed-now seam.** `ExtensionPointCatalogTest`
  reflectively collects every constant and cross-checks it against
  the table below in both directions — adding a typed-now row without
  a constant, or a constant without a row, is a build failure.
- **Pending seams are named here first.** A seam gets its row (and
  its owning issue) before its contract exists, so nobody invents a
  parallel mechanism in the meantime.

## The seams

| Seam | Point id | Contract | Home package | Cardinality | Lifecycle phase | Status |
| --- | --- | --- | --- | --- | --- | --- |
| Element provider | `elem.element-provider` | `jls.elem.ElementType` | `jls.elem` | many | register (eager, before any load) | typed now (#78 shipped; #212 external) |
| Palette contributor | `gui.palette-contributor` | `jls.edit.PaletteEntry` | `jls.edit` | many | register (GUI startup) | typed now (#78 shipped; #84 consumes) |
| Exporter | `hdl.exporter` | `jls.hdl.HdlEmitter` | `jls.hdl` | many | on-command (export) | typed now (#60 shipped; #213/#215 extend) |
| Importer | `hdl.importer` | cell-map/layout contract to be defined | `jls.hdl.imp` | many | on-command (import) | pending (#61/#62) |
| Op observer | `collab.op-observer` | `jls.collab.op.OpSink` | `jls.collab.op` | many | register, then observe every submit | typed now (#167 shipped; #171 consumes) |
| Command / activation trigger | `app.command` | shim contract over `jls.module.Activation` | `jls.module` | many | register (lazy activation vocabulary) | pending (#84, with #220's runtime) |
| Preferences / theme contributor | `gui.theme` | theme/preferences object replacing `JLSInfo` statics | `jls.edit` | one active | register (GUI startup) | pending (#76) |

## Column meanings

- **Contract** — the type every contribution must implement; the
  registry casts against it at contribution time, so a wrong-typed
  raw contribution fails at the boundary.
- **Cardinality** — `many`: all contributions are kept, in
  deterministic contribution order; `one active`: many may register,
  one is selected.
- **Lifecycle phase** — when contributions are consumed, in the
  vocabulary of `jls.module.Activation` (#220): eager registration at
  startup, or lazily on a command/event.
- **Status** — `typed now`: the constant exists and is pinned by
  `ExtensionPointCatalogTest`; `pending`: the seam is named and owned
  by the listed issue, and its contract lands with that issue.

## Built-in contributions (the completeness inventory)

- `elem.element-provider` — every row of `jls.elem.ElementRegistry`'s
  table (`ElementRegistryTest` keeps it total over loadable element
  classes).
- `gui.palette-contributor` — every row of the static
  `jls.edit.Palette` table (`PaletteContractTest` keeps it total over
  registered element types).
- `hdl.exporter` — `jls.hdl.VerilogEmitter` and
  `jls.hdl.VhdlEmitter` (both accepted by the point's contract;
  pinned by `ExtensionPointCatalogTest`).
- `collab.op-observer` — the editor-side `OpSink` that applies and
  records ops today; collaboration replication (#171) contributes its
  observers here.

Wiring these built-ins through an `ExtensionRegistry` instance at
startup (so `ElementRegistry`/`SimpleEditor` *consume* the registry
instead of their static tables) is deliberately **not** part of this
slice — it is a follow-on integration slice of #220/#224.
