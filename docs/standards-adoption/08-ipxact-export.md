## IEEE 1685 IP-XACT export for subcircuits (#4)

**Verdict up front: do not build this yet.** The port mapping is real and
mechanical — `HdlExporter.buildModel` already produces exactly the payload —
but everything that makes IP-XACT worth consuming (bus interfaces, memory
maps, parameterization, library identity) has no counterpart anywhere in JLS,
and no JLS user has asked. What follows is a complete specification held in
reserve behind a demand gate, in the same posture `docs/grand-architecture.md`
§9 holds #212 ("No plugin execution surface ahead of demand … the registry is
closed today and opens … only when a real user asks"). §5 states the gate.

### Does the theory survive contact with the code?

The premise in `docs/standards-landscape.md` (line 147: "a subcircuit already
*is* a component with typed, named, bit-width-carrying ports, which is
precisely IP-XACT's `component`/`busInterface` payload") is **half true, and
the false half is the valuable half.**

What checks out:

- `src/jls/elem/SubCircuit.java` `init()` derives this element's puts from the
  nested circuit's pins: every `InputPin` becomes an `Input` named
  `pin.getName()` of `pin.getBits()` bits, every `OutputPin` an `Output`
  likewise (lines ~197–270). Typed, named, width-carrying: confirmed.
- `src/jls/hdl/HdlExporter.buildModel` (lines 256–292) already performs the
  full walk: input pins sorted by name → `HdlModel.Port(name, INPUT, bits,
  comment)`; each `Clock` → a synthesized 1-bit `clk` input port; output pins
  sorted by name → `OUTPUT` ports. Names are legalized through `HdlNames` and
  the changes recorded in `model.renames()`. `HdlModel.Port`
  (`src/jls/hdl/HdlModel.java:43`) is, field for field, the IP-XACT
  `ipxact:port/ipxact:wire` payload. This half is genuinely free.

What does not check out — each verified in the tree:

1. **There is no VLNV, and three of its four fields must be invented.** A JLS
   subcircuit's entire identity is the nested `CIRCUIT` name token
   (`docs/file-format.md` §3.1: `letter (letter | digit | "_")*`, via
   `Util.isValidName`). No vendor, no library, no version, no author, no
   revision. IP-XACT requires all four (`identifier.xsd`,
   `versionedIdentifier` group: `vendor`, `library`, `name`, `version`, all
   mandatory).
2. **There is no reuse identity to reference.** `SubCircuit.save`
   (`src/jls/elem/SubCircuit.java:282`) writes the *entire nested circuit
   inline* into the parent file, and `Circuit.load` constructs a fresh
   `Circuit` per `SubCircuit` element (`src/jls/Circuit.java:1015–1021,
   setImported`). Two instances of "the same" subcircuit are two independent
   copies. IP-XACT's whole point is that a design instances a component *by
   VLNV reference*; JLS has nothing to reference and no way to tell whether
   two subcircuits are the same IP.
3. **There is no bidirectional port.** `jls.elem.Pin` is
   `sealed … permits InputPin, OutputPin`. `ipxact:direction` will therefore
   only ever be `in` or `out`; `inout` and `phantom` never appear. Tri-state
   is a property of the *net*, not the port (`Output.setTriState`,
   `OutputPin.isLoadTriState`, applied in `SubCircuit.init`), and has no
   faithful component-port encoding — it is simply lost.
4. **There are no bus interfaces.** JLS has no protocol, no grouped port
   bundles (`Binder`/`Splitter` are bit routing, not interfaces), no
   abstraction definitions. `busInterfaces` is optional in the schema, so the
   document stays valid — but IP-XACT integration is bus-interface driven, so
   a component without them cannot be auto-connected to anything.
5. **There are no memory maps.** `Memory` exists as an element but is not
   address-mapped through any interface. `memoryMaps` without an
   `addressSpace`/bus binding conveys nothing.
6. **There is no parameterization.** IP-XACT reuse rests on `parameters` plus
   configurable element values; JLS bit widths are constants fixed at edit
   time. Every emitted component is one fixed-width instance.
7. **`buildModel` cannot even be pointed at a circuit containing a
   subcircuit.** `HdlExporter.EXPORTED` (line 418) omits `SubCircuit` and
   `Memory`, and anything outside `EXPORTED`/`SKIPPED`/`TOPOLOGY` throws
   `HdlExportException` listing every offender (lines 189–193). The headline
   use case hits that wall on the first try. §2 step 1 fixes this.

**Plain statement of the mapping's thinness:** what JLS can emit is the least
interesting legal IP-XACT document — a made-up VLNV plus a flat list of wire
ports. It restates, in ~40 lines of XML, information already present and more
usable in the Verilog module header that `-export` writes today.

### What conformance actually means

**Document:** IEEE Std 1685, *Standard Structure for Packaging, Integrating,
and Reusing IP within Tool Flows* ("IP-XACT"). Revisions in circulation:
1685-2009, 1685-2014, 1685-2022. Accellera hosts the normative XML Schemas;
IEEE publishes the standard text.

**There are no conformance levels, classes, or profiles to target, and no
certification scheme.** For a *producer* of IP-XACT, "conformance" reduces to
two things:

1. The emitted document is **schema-valid** against the normative XSD set for
   the namespace it declares. This is machine-checkable and is the entirety of
   the objective claim.
2. It obeys the semantic rules the schema cannot express (the standard text
   carries a set of semantic consistency rules; *the exact clause/annex number
   and name are unverified* — I did not have the paywalled text). For the
   component-only, ports-only subset specified here, almost none of those
   rules are reachable, because they mostly govern bus-interface,
   memory-map and design-instance consistency.

**Which revision to emit: 1685-2022. Recommended, not surveyed.** Three
reasons, in order of decisiveness:

- **License.** The 1685-2022 XSDs carry an Apache-2.0 header (verified by
  reading the header of `ieee-1685-2022/component.xsd` in the
  `edaa-org/IPXACT-Schema` mirror: *"Accellera licenses this file to you under
  the Apache License, Version 2.0"*). The 2014 and 2009 schemas carry an
  Accellera "sharing friendly" notice that forbids modification and derived
  works and whose redistribution terms are not clearly stated (that mirror's
  own README calls their license state unknown and notes rules that threaten
  open-source redistribution — *the precise 2014 notice text is unverified*).
  A GPLv3 repository that wants to vendor a schema for offline testing can
  vendor Apache-2.0 material and should not vendor the other.
- **The lag argument buys nothing here.** It is true that commercial packagers
  were built on 2009/2014 and that 2022 adoption trails; if JLS had a
  commercial consumer, that would decide it. JLS has none, so the argument has
  no purchase.
- **The free consumer supports both.** Kactus2 (github.com/kactus2/kactus2dev,
  GPL-2.0, Tampere University) supports IEEE 1685-2014 *and* 1685-2022
  (verified). So the round-trip check exists either way.

If a real requester appears, **the first question to ask them is which
revision their tool ingests**, because the only document worth emitting is the
one their parser accepts. Accellera publishes XSLT up-conversion scripts N→N+1
(verified as a description on the download page; *not fetched and not tested*),
so a 2014 output could be produced by conversion rather than by a second
emitter. Do not build the 2014 path speculatively.

**Minimum for a valid `ipxact:component` (1685-2022).** Verified against
`identifier.xsd` and `component.xsd`: only the `documentNameGroup` is
mandatory, which reduces to the four `versionedIdentifier` elements in order —
`ipxact:vendor` (`xs:Name`), `ipxact:library` (`xs:Name`), `ipxact:name`
(`xs:NMTOKEN`), `ipxact:version` (`xs:NMTOKEN`). *Everything else* on
`component` — `busInterfaces`, `channels`, `modes`, `addressSpaces`,
`memoryMaps`, `model`, `componentGenerators`, `choices`, `fileSets`, `cpus`,
`parameters`, `vendorExtensions` — is `minOccurs="0"`. **A component with zero
ports is schema-valid.** That is worth saying out loud: schema validity is a
very low bar for this document type, and any conformance claim that rests only
on it must say so.

The minimal *useful* shape (port nesting verified against the Accellera
`SampleComponent.xml` for 1685-2014; the 2022 nesting for this subset is
believed identical but **MUST be re-checked against `model.xsd` before the
first golden is minted — unverified**):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<ipxact:component
    xmlns:ipxact="http://www.accellera.org/XMLSchema/IPXACT/1685-2022"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.accellera.org/XMLSchema/IPXACT/1685-2022
                        http://www.accellera.org/XMLSchema/IPXACT/1685-2022/index.xsd">
  <ipxact:vendor>io.github.anadon</ipxact:vendor>
  <ipxact:library>lab7</ipxact:library>
  <ipxact:name>alu</ipxact:name>
  <ipxact:version>1.0</ipxact:version>
  <ipxact:description>Generated by JLS ... . Interface only: wire ports
    with fixed widths. No bus interfaces, memory maps, parameters, views or
    file sets are represented, and JLS tri-state and propagation-delay
    information is not carried.</ipxact:description>
  <ipxact:model>
    <ipxact:ports>
      <ipxact:port>
        <ipxact:name>a</ipxact:name>
        <ipxact:wire>
          <ipxact:direction>in</ipxact:direction>
          <ipxact:vectors>
            <ipxact:vector>
              <ipxact:left>7</ipxact:left>
              <ipxact:right>0</ipxact:right>
            </ipxact:vector>
          </ipxact:vectors>
        </ipxact:wire>
      </ipxact:port>
    </ipxact:ports>
  </ipxact:model>
</ipxact:component>
```

**What is claimed:** JLS emits an IEEE 1685-2022 `ipxact:component` document
describing the *interface* of one JLS circuit or subcircuit — VLNV plus wire
ports with fixed widths — and that document is schema-valid against the
normative Accellera 1685-2022 XSD set.

**What is not claimed:** no `busInterfaces`, `abstractionDefinition`,
`memoryMaps`, `addressSpaces`, `cpus`, `parameters`, `views`, `fileSets`,
`componentGenerators`/TGI, `design` or `designConfiguration` documents; no
IP-XACT *import*; no claim of being an "IP-XACT compliant tool" in whatever
sense the standard's tool clauses use that phrase (unverified — the text is
paywalled).

**The artifact the claim rests on:** the committed golden XML files under
`test/resources/ipxact/` and a green `IpXactSchemaValidationTest` in a public
CI run. Nothing else. This mirrors how `ARCHITECTURE.md` already treats
goldens as "the oracles the normative docs cite".

### Implementation procedure

All paths below marked **(new)** are to be created; the rest exist and were
read.

1. **Extract the port walk so it cannot drift, and free it from the element
   policy.** In `src/jls/hdl/HdlExporter.java`, factor lines ~253–292 (input
   pins by name → `Clock`s → output pins by name, all reserved through one
   `HdlNames` instance) into

   ```java
   public static List<HdlModel.Port> buildPorts(Circuit circ)
   ```

   and have `buildModel` call it, so a single implementation feeds both. Two
   properties are load-bearing: (a) ports are reserved *before* any net name,
   so port identifiers depend only on pins and clocks and are stable; (b)
   `buildPorts` **must not** run the offender check at lines 171–193, so a
   circuit containing a `SubCircuit` or `Memory` still yields a port list even
   though it has no Verilog rendering. This is the only edit to existing code
   and it must be byte-behaviour-preserving; `VerilogExportGoldenTest` and
   `VhdlExportGoldenTest` prove that in seconds.

2. **New package `jls.ipxact`, not `jls.hdl`.** IP-XACT is metadata, not a
   hardware description language; `HdlEmitter.emit` is contractually "complete
   HDL source text" with a `fileExtension()` naming a language, and shoehorning
   XML into it would make both lies. Do **not** publish a new
   `ExtensionPoint` for it: `docs/extension-points.md` states pending seams get
   a row only with an owning issue, and no second IP-XACT emitter is
   conceivable. Files:
   - `src/jls/ipxact/package-info.java` **(new)** — `@NullMarked` from birth
     (CONTRIBUTING's ratchet convention); add `jls.ipxact` to the list in
     `test/jls/NullMarkedRatchetTest.java`.
   - `src/jls/ipxact/IpXactComponent.java` **(new)** — a `record` carrying
     `vendor`, `library`, `name`, `version`, `description`, and
     `List<HdlModel.Port> ports` (value semantics per CONTRIBUTING #94).
   - `src/jls/ipxact/IpXactWriter.java` **(new)** —
     `public static String write(IpXactComponent c)`.
   - `src/jls/ipxact/XmlText.java` **(new, package-private)** — the escaper.

   `jls.ipxact` sits under the headless-core rule automatically once added to
   `HeadlessCoreRatchetTest.CORE_PACKAGE_PREFIXES` (which already lists
   `src/jls/hdl/`, `src/jls/module/`, `src/jls/core/`). Add it there in the
   same commit, with no baseline entry, ever. `javax.xml.*` is not among the
   forbidden prefixes (`java.awt.`, `javax.swing.`, `jls.edit.`), so the
   ratchet does not constrain this choice either way.

3. **XML generation: hand-written string emission. No new dependency, and no
   `javax.xml` on the production side.** Verified context: `pom.xml` declares
   exactly four runtime dependencies (`org.tukaani:xz`,
   `org.jfree:org.jfree.svg`, `com.formdev:flatlaf`, `org.jspecify:jspecify`)
   — none is an XML library — and a grep of `src/` and `test/` for
   `javax.xml`, `org.w3c.dom`, `DocumentBuilder`, `XMLStreamWriter` and
   `Transformer` returns **nothing**: JLS writes no XML today (the SVG path
   goes through JFreeSVG, which builds its own strings). Recommendation and
   reasons:
   - `HdlEmitter.emit`'s javadoc already imposes "same model, same bytes", the
     goldens enforce it, and `pom.xml` pins `project.build.outputTimestamp`
     for reproducibility. Routing through `javax.xml.transform.Transformer`
     would inherit the JDK serializer's indentation, attribute ordering and
     namespace-prefix choices, none of which is specified and all of which
     have moved across JDK releases. That is a reproducibility hazard for zero
     benefit.
   - The document has no mixed content, one level of recursion, and three
     attributes, all on the root. The escaping surface is `&`, `<`, `>` in
     element text.
   - `javax.xml.stream.XMLStreamWriter` is JDK-native and would be the
     fallback if the document ever grew real attribute/namespace complexity —
     but it does not pretty-print, so indentation would still be hand-managed:
     all of the byte-control work, none of the savings.
   - Use `javax.xml` **test-side only**, for `SchemaFactory` validation (§4).
     That is where a library's correctness is actually wanted.

   Emit LF line endings unconditionally, the same discipline and for the same
   reason as `Circuit.save`'s `canonicalNewlines` wrapper (a file written on
   Windows must byte-match one written on Linux, or the goldens are
   platform-dependent). UTF-8, no BOM.

4. **The escaper is load-bearing, not boilerplate.** Pin names are validated
   only in the GUI (`src/jls/edit/PinDialog.java:181` calls
   `Util.isValidName`); the *loader* does not validate them — `Pin` has no
   String `setValue` override, so the `name` attribute arrives through the
   generic `Attribute` registry unchecked, and `docs/file-format.md` §6's
   quoted-string escaping round-trips arbitrary text including `&`, `<`, `>`
   and newlines. A hand-edited or hostile `.jls` therefore reaches the XML
   writer with arbitrary content. Escape `&` → `&amp;`, `<` → `&lt;`,
   `>` → `&gt;` in element text; refuse (with an `HdlExportException`-style
   diagnostic, one `jls: error:` line per the CLI contract) any name
   containing a character XML 1.0 cannot represent at all, e.g. U+0000. Do not
   silently drop characters.

5. **VLNV mapping. Recommendation, and it must be declared normative in the
   same commit that first emits one** — the moment a file exists in the world,
   these four strings are a de facto interface.
   - `vendor` = `io.github.anadon`, the project's Maven `groupId`
     (`pom.xml:7`). It is a valid `xs:Name`. **Do not use a URL**: `xs:Name`
     forbids `/`, so `github.com/anadon/JLS` is not merely ugly, it is
     invalid.
   - `library` = the base name of the containing `.jls` file, which
     `Util.isValidFileName` already constrains to `isValidName`; fall back to
     the literal `jls` when there is no file name. A `.jls` file is the nearest
     thing JLS has to a library of subcircuits.
   - `name` = the nested `CIRCUIT` name (`SubCircuit.getName()` /
     `Circuit.getName()`), **unmangled**. `Util.isValidName` accepts any
     Unicode letter (`Character.isLetter`), and `xs:NMTOKEN` accepts those
     too, so no legalization is needed. Explicitly do **not** run the VLNV
     name through `HdlNames`, whose sanitizer flattens non-ASCII to `_` for
     Verilog-2005's benefit.
   - `port/name` = the **`HdlNames`-legalized** identifier, i.e. exactly what
     `buildPorts` returns. Rationale: the only plausible use of this file is
     alongside the Verilog `-export` writes, and the port names must match that
     module's header or the pairing is useless. Where legalization changed a
     name (`HdlModel.renames()`), record the original in the port's
     `ipxact:description` so nothing is silently lost.
   - `version` = the literal `1.0` by default. **Do not use the JLS release
     version.** That is the tempting choice (the Verilog header does it) and
     it is wrong: it would re-version every component on every JLS upgrade,
     destroying the one thing the field is for. Put the JLS version in
     `ipxact:description` instead, and tokenize it as `@VERSION@` in the
     goldens exactly as `VerilogExportGoldenTest` already does.
   - State plainly in the doc that JLS **cannot** guarantee VLNV uniqueness
     across a library; two different labs may both produce
     `io.github.anadon:lab7:alu:1.0`.

6. **Put the caveat inside the file.** Emit an `ipxact:description` on the
   component saying what is and is not represented (see the skeleton above).
   Documentation does not travel with an exported artifact; this line does. It
   is the highest-leverage line in the feature.

7. **CLI surface.** In `src/jls/JLSStart.java`: a new
   `FlagSpec("ipxact", Arity.REQUIRED, "file", "an output file", …)` in
   `FLAGS`, a `JLSInfo.ipxactexport` mode flag alongside `hdlexport` /
   `imgexport` / `textsave`, added to the mode-exclusivity check (~line 933)
   and routed through the existing temp-file-and-rename writer used by the HDL
   export path (~line 620). Add `-sub <name>` to select a nested subcircuit by
   name, defaulting to the top-level circuit. Diagnostics keep the contract:
   one `jls: error: …` line on stderr, exit 1 for runtime failure, 2 for usage
   error.

8. **Documentation.** New `docs/ipxact-export.md` **(new)** in house style:
   RFC 2119 keywords for the normative parts (the VLNV scheme, the port
   mapping, the LF/UTF-8 byte rules), issue-number citations, and an explicit
   "not represented" list. Cross-reference it from `README.md`'s command-line
   options paragraph and from `docs/standards-landscape.md` §13.1 item 5. Add
   a CHANGELOG entry.

**Stability-contract impact.**
- `docs/file-format.md`: **untouched.** Nothing is read from or written to
  `.jls`.
- `docs/batch-interface.md`: **untouched.** That contract covers `-t`, the
  watched-element stdout format, the exit/stream contract and `-vcd`; HDL
  export already sits outside it and IP-XACT export sits in the same place.
- `JLSStart.FLAGS` is the authoritative flag table and `-h` is generated from
  it; `test/jls/CliFlagTableTest.java` will fail until the flag is added
  consistently on both sides. That is the drift gate working, not extra work.
  A new flag is still a user-visible CLI change and needs a CHANGELOG entry.
- **The one genuinely new contract** is the emitted VLNV scheme. Declare it
  normative in `docs/ipxact-export.md` from day one, or you will be unable to
  change `library` later without breaking somebody's library index.

**Migration and compatibility.** None required. This is an output-only
surface: no saved-file change, no format-version bump, no behavioural change
to any existing mode. Every existing `.jls` file exports unchanged. The
migration proof is that the Verilog and VHDL goldens are byte-identical after
step 1.

### Testing procedure

All test classes below are **to be created** under `test/jls/ipxact/`, in the
existing house style (JUnit 5, headless, golden files under
`test/resources/`).

1. **`IpXactGoldenTest.java`** — a direct structural copy of
   `test/jls/hdl/VerilogExportGoldenTest.java`: build circuits with the
   existing `test/jls/hdl/HdlCircuitBuilder.java` helper, emit, replace
   `JLSInfo.versionString` with the `@VERSION@` token, compare byte-for-byte
   against `test/resources/ipxact/<name>.xml`, regenerate with
   `-Djls.ipxact.regenerate=true` and review the diff like source. Golden
   cases, chosen to pin the places this can silently rot:
   - one 1-bit input and one 1-bit output (the scalar case — decide and pin
     whether a 1-bit port emits `vectors` at all);
   - an 8-bit bus (pins `left=7`/`right=0`);
   - a circuit with **no** pins (the degenerate schema-valid document);
   - a pin named `output` (a Verilog reserved word — `HdlNames` appends `_`)
     and a pin with a non-ASCII letter (sanitized to `_`), both of which must
     surface the original in `ipxact:description`;
   - two pins whose legalized names collide (uniquification to `_2`);
   - a `Clock` (the synthesized `clk` input port);
   - **a circuit containing a `SubCircuit`** — this is the regression that
     proves `buildPorts` does not inherit `buildModel`'s offender rejection at
     `HdlExporter.java:189`.
2. **`IpXactSchemaValidationTest.java`** — validates the goldens against the
   vendored XSD set using JDK-native
   `SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)` with
   `FEATURE_SECURE_PROCESSING` on, `ACCESS_EXTERNAL_DTD` set to `""` and
   `ACCESS_EXTERNAL_SCHEMA` set to `"file"`, and a resolver over the local
   files. Two design rules:
   - **Directory-driven, not enumerated.** Glob `test/resources/ipxact/*.xml`
     so a golden cannot be added without being validated.
   - **A negative control is mandatory.** Include a deliberately-broken
     fixture (a component with `vendor` omitted, and one with `vectors`
     hoisted out of `wire`) that the test asserts *fails* validation. Without
     it, a validator that has been silently neutered — the single most likely
     failure of this whole exercise — reports green and proves nothing.
3. **`XmlEscapingTest.java`** — property/fuzz test over `XmlText`: for random
   strings, the escaped form parses as XML character data and decodes back to
   the input. Then the higher-value fuzz: generate circuits with adversarial
   pin names (`&`, `<`, `"`, embedded newlines, U+0000, very long names) via
   `HdlCircuitBuilder` and assert the emitted document *still schema-validates*
   or is rejected with a clean diagnostic. This is the only place untrusted
   file content reaches a structured output format, so it is where fuzzing
   earns its keep; it complements the existing hostile-input posture of
   `UntrustedFileHardeningTest`.
4. **`IpXactVlnvPolicyTest.java`** — pins the normative §2 step 5 rules against
   `docs/ipxact-export.md`: vendor is exactly `io.github.anadon`; library
   derives from the file stem; name is the circuit name unmangled (assert with
   a non-ASCII circuit name); version is `1.0` and does **not** track
   `JLSInfo.versionString`. This is the drift test that keeps the scheme from
   changing by accident once third parties key on it.
5. **`Kactus2RoundTripTest.java` — proposed, and possibly not buildable.**
   Follow the exact skip-when-absent pattern of
   `test/jls/hdl/IverilogCompileTest.java`: `ToolLocator.findOnPath("kactus2")`
   (`test/jls/hdl/ToolLocator.java` already handles Windows `PATHEXT`) guarded
   by `Assumptions.assumeTrue(...)`. **Honest caveat:** Kactus2 is a Qt GUI
   application; whether it exposes a headless/batch mode that can import a
   component and re-emit or report it is **unverified**, as is the binary
   name. Check before promising this test. If no CLI exists, the fallback is a
   manual once-per-release checklist entry in the style of
   `docs/wayland-desktop-checklist.md`: open the emitted component in Kactus2,
   confirm it enters the library and shows the expected ports and widths,
   record the Kactus2 version in the checklist. Say plainly which of the two
   you shipped. The schema test is the load-bearing evidence; the round trip
   is corroboration.

**Where the schema comes from: vendor it, do not download it.**
`test/resources/ipxact/1685-2022/` **(new)**, test scope only, never inside the
jar. Reasoning:
- The project's no-network stance is about *runtime* ("no network, no server,
  no install step may be assumed at runtime"), not about CI — CI already
  downloads a SHA-256-pinned oss-cad-suite bundle in the Windows lane
  (`.github/workflows/ci.yml`, the `Arm the HDL simulator toolchain` step). So
  downloading is not forbidden on principle.
- But a downloaded schema makes a hermetic test depend on accellera.org's
  uptime and on a URL that has already moved at least once (accellera.org and
  eda.org both serve the schema tree; **both returned HTTP 403 to a plain
  fetch during this research**). Vendoring is strictly better: deterministic,
  offline, reviewable in the diff, and immune to a silent upstream edit.
- License check before committing: the 2022 `component.xsd` header is
  Apache-2.0 (**verified**), and GPLv3 can incorporate Apache-2.0 material.
  But `component.xsd` includes `busInterface.xsd`, `identifier.xsd`,
  `generator.xsd`, `commonStructures.xsd`, `model.xsd`, `subInstances.xsd` and
  `constraints.xsd`, plus their transitive includes — **each file's header
  must be checked individually; only one was.** Add
  `test/resources/ipxact/1685-2022/README` recording source URL, retrieval
  date, revision, and license, and keep every header intact. **Do not vendor
  the 2014 or 2009 schemas**: their license is restrictive-or-unclear, and
  "unclear" is itself the reason not to.
- Total set size is *unverified* (a few dozen XSD files, low hundreds of KB is
  the expectation). Confirm before committing; if it turns out to be
  multi-megabyte, that is a reason to reconsider vendoring, not a reason to
  reach for the network.

**CI lane changes (`.github/workflows/ci.yml`): none required.** The golden,
schema, escaping and policy tests are pure JVM and run inside the existing
`mvn -B verify` of the Linux, Windows and macOS build jobs. Do **not** add a
Kactus2 install step: it is a Qt GUI package with a large dependency closure
and no verified headless mode, and the skip-when-absent guard keeps it a
local/manual tool. If a Kactus2 CLI is later confirmed, arm it best-effort in
the Linux lane's existing `Install HDL toolchain and virtual display` step
(line 62), which already uses the
`|| echo "some optional tools unavailable; their tests will skip"` idiom.

**What regression turns the suite red:**
- any change to port order, naming or width in `HdlExporter.buildPorts` → the
  IP-XACT goldens **and** `VerilogExportGoldenTest`/`VhdlExportGoldenTest`,
  which is the proof the walk is genuinely shared rather than forked;
- any change to XML structure, indentation, line endings or escaping → the
  IP-XACT goldens;
- any structural change that leaves the schema (moving `vectors` out of
  `wire`, dropping `library`) → `IpXactSchemaValidationTest`, with the
  negative control catching a validator that has stopped validating;
- VLNV scheme drift → `IpXactVlnvPolicyTest`;
- a new element type that changes what counts as a port → the golden with the
  `SubCircuit` fixture.

### Certification / conformance procedure

**There is no certification. Say so plainly and do not dress it up.** IP-XACT
has no certification body, no conformance registry, no official test suite, no
logo program, and no accredited assessor. Accellera's IP-XACT Working Group
maintains and publishes the schemas; IEEE publishes the standard text. Nobody
certifies producers of IP-XACT files, and no one will audit this.

A credible self-assertion for this project therefore consists of exactly four
things:

1. **A precise scope statement** — `docs/ipxact-export.md`, naming the
   revision (1685-2022), the document type (`component` only), the subset
   emitted (VLNV + `model/ports`), the VLNV assignment rules, and an explicit
   *not represented* list (bus interfaces, memory maps, address spaces,
   parameters, views, file sets, generators, designs; and the JLS-side losses:
   tri-state, propagation delay, subcircuit nesting).
2. **Machine-checked validity** — `IpXactSchemaValidationTest` passing against
   the vendored normative XSDs, with the negative control proving the
   validator is live.
3. **Reproducible evidence anyone can re-run** — committed goldens plus
   `mvn verify` green in a public CI run, which is already how this project
   substantiates its normative documents.
4. **A named external corroboration** — the Kactus2 result (automated or
   checklist), with the tool version recorded, so the claim is not purely
   self-referential.

**Cost and elapsed time.** The XSD schemas and the Accellera IP-XACT User
Guide are free downloads; a User Guide PDF exists for the 2014 revision (dated
2018; *its content was not fetched and is unverified*). The IEEE 1685-2022
standard *text* is behind IEEE's paywall — **I did not verify the list price
and will not invent one; check standards.ieee.org before budgeting.** For the
component-only, ports-only subset specified here, the schemas plus the User
Guide are sufficient and the text need not be bought. If the scope ever grew
past ports, buying it becomes necessary, because the semantic rules the schema
cannot express exist only in the text.

**Validity period, renewal, maintenance.** None; there is nothing to renew.
Maintenance is ordinary software maintenance. A future IP-XACT revision
(cadence has been roughly one per 5–8 years: 2009, 2014, 2022) does not
invalidate documents already emitted against an older namespace. What *does*
invalidate the claim: (a) the emitter drifting out of schema validity — caught
by CI on every push; (b) `docs/ipxact-export.md` claiming a subset the emitter
does not actually produce — caught by `IpXactVlnvPolicyTest` and the goldens
for the parts they cover, and by nothing at all for the parts they do not,
which is an argument for keeping that document short and literal.

**What "valid" buys, and what it does not.** Valid means a conforming parser
will accept the file and can read the VLNV and the port list. It does **not**
mean a tool can do anything useful with it: no bus interfaces means no
automatic connection in an integrator; no memory maps means no register-header
generation; no view or file set means there is no implementation to elaborate
or simulate. A schema-valid, ports-only component is the IP-XACT equivalent of
a Verilog file containing nothing but a module header. Any announcement of
this feature that omits that sentence is overselling it.

### Effort, risk, and failure modes

**Sizing — 3–5 maintainer-days**, built up from the parts:

| Work | Days | Reasoning |
|---|---|---|
| Extract `buildPorts` from `buildModel` | 0.5 | Small diff, but it crosses an order-sensitive naming pass; the existing Verilog/VHDL goldens verify it in minutes. |
| `IpXactComponent` / `IpXactWriter` / escaper | 0.5 | ~200 lines of straight-line string building. |
| CLI flag, mode routing, `-sub` selection, `-h`, CHANGELOG | 0.5 | `CliFlagTableTest` and `usage()` generation make this mechanical. |
| Vendor the XSD set, per-file license check, offline-safe validator wiring | 1.0 | **The time sink.** Getting `SchemaFactory` to resolve a multi-file include graph entirely from `test/resources` with external access disabled is fiddly and platform-quirky, and every included file's license header needs reading. |
| Goldens (7 fixtures), schema test, negative control, escaping property test | 1.0 | Fixture construction dominates. |
| `docs/ipxact-export.md` + cross-references | 0.5 | House RFC 2119 style. |
| Kactus2 corroboration (or the checklist fallback) | 0.5–1.0 | May be partly wasted; see risk 3. |

**Top three ways this goes wrong:**

1. **The file is valid and useless, and someone notices publicly.** A user
   opens the component in a packager, finds a bare port list and no bus
   interfaces, and concludes JLS's IP-XACT support is broken rather than
   deliberately minimal. Mitigation is the in-file `ipxact:description` (§2
   step 6) and a scope statement in the release note — not a doc page nobody
   fetches alongside the XML.
2. **The XSD vendoring collapses into a licensing problem.** Apache-2.0 is
   verified for one file out of a set of at least eight plus transitive
   includes. If any of them is not Apache-2.0, vendoring is off, and the only
   remaining path is a network-fetching test — which then has to be demoted to
   an optional/nightly lane, at which point the feature loses its sole piece
   of objective conformance evidence and is reduced to goldens asserting that
   JLS still produces the bytes JLS produced last week.
3. **The VLNV gets locked in wrong before anyone thinks about it.** Once a
   file exists, `vendor`/`library`/`version` are an interface. The tempting
   `version = JLS release` choice (which the Verilog header does, and which
   the goldens' `@VERSION@` tokenization would happily absorb) destroys the
   identity semantics of the field, and undoing it later breaks anyone's
   library index. Cheap to get wrong, expensive to reverse.

**When the project should NOT do this — the operative recommendation.**

Do not build it now. **No JLS user has asked, and the user who would ask is
hard to construct.** JLS's population is students in an introductory
digital-logic course and the instructors grading them; IP-XACT's population is
SoC integration engineers assembling third-party IP across a bus fabric. In
practice the overlap is empty: a student who genuinely needs their subcircuit
in an SoC flow needs the *Verilog*, which `-export` already writes, and every
packager worth using can infer a component from a Verilog module header. **This
feature would add a second, weaker description of information JLS already
exports in the form the consumer actually consumes.** That is the argument
against, and it should be recorded as the reason the item stays gated rather
than being quietly forgotten.

Also do not do it as a standards checkbox. `docs/standards-landscape.md` §13.1
ranks it fifth and calls it "speculative but structurally free"; "structurally
free" is true of the *ports* and false of everything else. That correction is
this section's main contribution, and it should be folded back into the
survey.

**The demand gate — two conditions, either of which opens it:**

- **(a) A named consumer.** A person or course states the concrete flow: "I
  need my JLS subcircuit in *this* tool, which reads IP-XACT *this*
  revision." The revision they name, not 2022, is then the target. Record the
  request on the issue the way #212's gate is recorded.
- **(b) The item stops being standalone.** If a real bus abstraction enters
  JLS (a Wishbone-style teaching bus element, landscape #11), or a register-map
  feature (SystemRDL, #38), IP-XACT acquires content worth emitting and the
  emitter's cost is already sunk. Until then the document has nothing to say
  beyond what the Verilog header says better.

Until one of those fires, this file is the specification, `mvn verify` stays
untouched, and the correct engineering action is zero lines of code.

### Sources

**Repo paths — all read and verified at HEAD (`9ab4797`):**

- `/home/user/JLS/docs/standards-landscape.md` — entry #4 (line 123), the
  "subcircuit already *is* a component" premise (lines 147–149), the §13.1
  ranking and demand-gate framing (lines 740–742).
- `/home/user/JLS/src/jls/hdl/HdlExporter.java` — `buildModel` (line 166), the
  offender check (189–193), the port walk (253–292), the `EXPORTED` /
  `SKIPPED` / `TOPOLOGY` policy sets (418–433), the class javadoc naming
  `SubCircuit` and `Memory` as rejected (line 84).
- `/home/user/JLS/src/jls/hdl/HdlModel.java` — `Port` record (line 43),
  `ports()` / `renames()` accessors (lines ~858, ~882).
- `/home/user/JLS/src/jls/hdl/HdlNames.java` — the legalization rule and
  Verilog-2005 reserved-word set.
- `/home/user/JLS/src/jls/hdl/HdlEmitter.java` — the "same model, same bytes"
  determinism contract.
- `/home/user/JLS/src/jls/elem/SubCircuit.java` — port derivation in `init`
  (lines ~197–270), `save` writing the nested circuit inline (line 282).
- `/home/user/JLS/src/jls/elem/Pin.java` — `sealed … permits InputPin,
  OutputPin`; no name validation in `setValue`.
- `/home/user/JLS/src/jls/Circuit.java` — nested-circuit load and
  `setImported` (lines 1015–1021), `canonicalNewlines` rationale (~1470).
- `/home/user/JLS/src/jls/Util.java` — `isValidName` (line 219).
- `/home/user/JLS/src/jls/edit/PinDialog.java:181` — where pin names *are*
  validated (GUI only).
- `/home/user/JLS/src/jls/JLSStart.java` — `FLAGS` table (~line 765–786),
  export-mode routing (~363–472), the shared temp-and-rename export writer
  (~620), mode exclusivity (~933).
- `/home/user/JLS/pom.xml` — groupId `io.github.anadon` (line 7), the four
  runtime dependencies (lines 59–95), `project.build.outputTimestamp`.
- `/home/user/JLS/test/jls/HeadlessCoreRatchetTest.java` — the core package
  prefixes and the forbidden-import set.
- `/home/user/JLS/test/jls/hdl/VerilogExportGoldenTest.java` — the golden-file
  and `@VERSION@` tokenization pattern; `.../ToolLocator.java` and
  `.../IverilogCompileTest.java` — the skip-when-absent pattern;
  `.../HdlCircuitBuilder.java` — the fixture builder.
- `/home/user/JLS/docs/file-format.md` — §3.1 names, §4 `FORMAT` header, §6
  string escaping, §7 the `SubCircuit` row.
- `/home/user/JLS/docs/batch-interface.md` §1 — the scope of the batch
  stability contract (HDL export is outside it).
- `/home/user/JLS/docs/extension-points.md` — the `hdl.exporter` seam and the
  "pending seams need an owning issue" rule.
- `/home/user/JLS/docs/grand-architecture.md` §4.3, §9 — the #212 demand-gate
  language this section mirrors.
- `/home/user/JLS/CONTRIBUTING.md` — `@NullMarked` ratchet, value-semantics
  and sealed-dispatch rules, SpotBugs/coverage gates.
- `/home/user/JLS/.github/workflows/ci.yml` — the Linux HDL-toolchain install
  step (line 62), the pinned oss-cad-suite download in the Windows lane
  (lines 147–213).

**External — verification status marked:**

- IP-XACT schema hosting and licensing — Accellera / eda.org download pages.
  **Verified via search summary**, not fetched: both `accellera.org` and
  `eda.org` returned HTTP 403 to a direct fetch during this research.
  <https://www.accellera.org/downloads/standards/ip-xact>,
  <https://www.eda.org/downloads/standards/ip-xact>
- `ieee-1685-2022/component.xsd` Apache-2.0 license header, target namespace
  `http://www.accellera.org/XMLSchema/IPXACT/1685-2022`, include list, and the
  fact that all `component` children except `documentNameGroup` are
  `minOccurs="0"` — **verified** by fetching the `edaa-org/IPXACT-Schema`
  mirror. <https://github.com/edaa-org/IPXACT-Schema>
- `ieee-1685-2022/identifier.xsd` `versionedIdentifier` group (vendor
  `xs:Name`, library `xs:Name`, name `xs:NMTOKEN`, version `xs:NMTOKEN`, all
  mandatory) and `documentNameGroup` — **verified** by fetching the same
  mirror.
- 1685-2014 component nesting `port/wire/direction` and
  `wire/vectors/vector/left|right` — **verified** against
  `tudortimi/ipxact` `tests/ieee/xml/SampleComponent.xml`. **The 2022 nesting
  for this subset is assumed identical and is UNVERIFIED**; check `model.xsd`
  before minting the first golden.
- 2014/2009 schema license ("sharing friendly", no modification or derived
  works) — **partially verified**: reported by the `edaa-org` mirror's
  documentation and by a search summary; the actual notice text was not read.
- Kactus2 is open source (GPL-2.0), hosted at
  <https://github.com/kactus2/kactus2dev>, and supports both IEEE 1685-2014
  and 1685-2022 — **verified via search summaries**; the repository README was
  not read directly. **Whether it has a headless/CLI mode usable from a test,
  and its binary name, are UNVERIFIED.**
- Accellera XSLT up-conversion scripts N→N+1 — **unverified**; described on the
  download page summary, never fetched or run.
- IEEE 1685-2022 standard text price and the exact clause/annex containing the
  semantic consistency rules — **unverified; deliberately not guessed.**
- IEEE 1685 revision cadence (2009 / 2014 / 2022) — revision years verified
  from IEEE Xplore search results; the "one per 5–8 years" projection is my
  inference, not a published schedule.
