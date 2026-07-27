<#
.SYNOPSIS
  windows-rig.ps1 - Windows GUI app-boot first-light rig for JLS
  (issue #111 Stage W7; the Windows analogue of scripts/x11-rig.sh /
  scripts/macos-rig.sh).

.DESCRIPTION
  Boots the REAL JLS application entry point exactly as a user would -
  `java -jar target\jls-*.jar` (the shaded jar's Main-Class jls.JLS) - on the
  windows-latest runner's REAL interactive window station (unlike the Linux
  lanes there is NO Xvfb/compositor to stand up), then asserts that a top-level
  window belonging to the JLS process actually maps, and captures the evidence
  into $ArtifactsDir:

    control-stdout.log / control-stderr.log   HelloSwingControl console output
    control-verdict.txt                        did the control window map?
    control.png                                screenshot with the control up
    jls-stdout.log / jls-stderr.log            JLS console output (first-light census)
    jls-window-info.txt                        window verdict for the JLS process
    desktop-before.png                         screenshot before anything launches
    jls-window.png                             screenshot with the JLS window up
    nonblank.txt                               unique-color counts (non-blank proof)

  WHY THIS LANE EXISTS (and how it differs from the display JUnit suite):
  the @Tag("display") JUnit UI tests exercise UI *unit* classes in-process.
  This rig is different: it LAUNCHES THE APP as a user would and proves the
  actual app window comes up on the window station and can be screenshotted -
  the same distinction the Linux gui-x11 lane already draws. It is the Windows
  twin of the Linux/macOS GUI-boot rigs.

  CONTROL FRAME FIRST: before JLS, the rig launches
  scripts\HelloSwingControl.java - a minimal JFrame on the same runtime and
  window station - so every run separates the two failure modes by itself.
  control up + JLS down means a JLS startup defect (exit 1); control down (or a
  capture the control frame ALSO shows blank) means the environment is the
  blocker (exit 2), and JLS is not blamed for it.

  Exit status (the same contract as the Linux/macOS rigs):
    0  a real JLS window mapped on the window station AND a non-blank
       screenshot was captured (first light).
    1  JLS-side failure: the control window mapped and its screenshot was
       non-blank, but JLS did not (no window, crashed before mapping, or its
       screenshot was blank).
    2  environment/rig fault: even the minimal control window never mapped, the
       control screenshot was blank, or a required tool is missing. Never
       blamed on JLS.

  GENUINE FIRST LIGHT (honest, not verifiable offline): this rig has NEVER run
  on a real Windows runner - its first execution is on CI, and PowerShell is
  not available in the authoring sandbox, so only the exit-code classification
  was self-tested (scripts\windows-rig-selftest.ps1, via injected stubs). A
  hosted non-interactive CI session MAY hand back a blank/desktop-only frame or
  an empty MainWindowTitle if the process's window is not on the captured
  station/desktop; either is classified as exit 2 (environment) via the
  control-frame comparison, NEVER exit 1. If first light shows MainWindowHandle
  detection is insufficient, the documented deeper fallbacks are a user32
  EnumWindows title enumeration (Add-Type) and an in-JVM java.awt.Robot
  self-capture. Every wait loop is bounded by a hard Stopwatch timeout, so a
  withheld station degrades to a clean exit 2, never a hang.

  NON-BLANK PROOF: window presence is polled from Get-Process MainWindowHandle
  (authoritative for the launched process) with a MainWindowTitle-contains
  fallback; the screenshot is a System.Drawing Graphics.CopyFromScreen of the
  virtual screen; the non-blank gate counts distinct sampled colors in the same
  script (a black/failed capture has 1 color, so >1 is the P1 gate, and the
  control-frame comparison disambiguates a blank capture as environment).

  PROMOTION: advisory `continue-on-error` -> required once 20 consecutive runs
  show at most one failure (the #188/#101 rule the gui-wayland/gui-x11 lanes
  followed), then drop continue-on-error and register the lane's exact `name:`
  ("GUI boot (Windows, WindowStation)") as a required check with recorded run
  IDs, mirroring installer-reproducibility-aarch64.

.PARAMETER NoAutoRun
  Dot-source the script for its functions without running the rig (used by the
  selftest to inject stubs into Invoke-GuiBootRig).
#>
[CmdletBinding()]
param(
    [string]$ArtifactsDir = $(if ($env:ARTIFACTS_DIR) { $env:ARTIFACTS_DIR } else { 'artifacts\windows-rig' }),
    [string]$Jar          = $env:JAR,
    [string]$JavaExe      = $(if ($env:JAVA) { $env:JAVA } else { 'java' }),
    [int]$ControlTimeout  = $(if ($env:CONTROL_TIMEOUT) { [int]$env:CONTROL_TIMEOUT } else { 60 }),
    [int]$WindowTimeout   = $(if ($env:WINDOW_TIMEOUT)  { [int]$env:WINDOW_TIMEOUT }  else { 90 }),
    [int]$RenderTimeout   = $(if ($env:RENDER_TIMEOUT)  { [int]$env:RENDER_TIMEOUT }  else { 30 }),
    [switch]$NoAutoRun
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Write-RigLog { param([string]$Message) Write-Host "windows-rig: $Message" }

# ----------------------------------------------------- real implementations
# (each is passed into Invoke-GuiBootRig as a script block by the auto-run
# block below; the selftest injects stubs in their place)

function Start-JavaProcess {
    param([string[]]$ArgList, [string]$OutLog, [string]$ErrLog)
    return Start-Process -FilePath $script:JavaExe -ArgumentList $ArgList -PassThru `
        -RedirectStandardOutput $OutLog -RedirectStandardError $ErrLog
}

# The production launch delegates, as TOP-LEVEL functions - deliberately NOT
# .GetNewClosure() script blocks. A closure runs in a fresh dynamic-module scope
# that captures variables but CANNOT see script-scope functions, so a closure
# body calling Start-JavaProcess dies at runtime with "The term
# 'Start-JavaProcess' is not recognized" - the exact Windows first-light failure
# (#265 Stage 2, taxonomy entry ps-scope/error-misclassification): the selftest's
# injected stubs replaced the launch path, so this production-only helper was
# never exercised until CI. As named functions passed by ${function:...}
# reference (exactly like Get-WindowCount/Save-Screenshot/Get-ColorCount below)
# they run in the script session state and DO resolve Start-JavaProcess plus the
# $script:-scoped inputs the auto-run block sets. The selftest OVERRIDES
# Start-JavaProcess to probe this reachability without launching java, so it is
# no longer defined only under stub injection.
function Invoke-ControlLaunch {
    Start-JavaProcess `
        -ArgList @('-Djava.awt.headless=false', $script:ControlSrc) `
        -OutLog (Join-Path $script:ArtifactsDir 'control-stdout.log') `
        -ErrLog (Join-Path $script:ArtifactsDir 'control-stderr.log')
}

function Invoke-JlsLaunch {
    Start-JavaProcess `
        -ArgList @('-Djava.awt.headless=false', '-jar', $script:Jar) `
        -OutLog (Join-Path $script:ArtifactsDir 'jls-stdout.log') `
        -ErrLog (Join-Path $script:ArtifactsDir 'jls-stderr.log')
}

# Count top-level windows belonging to the launched process. Primary signal:
# the process's own MainWindowHandle (authoritative). Fallback: any process
# whose MainWindowTitle contains the expected title (JLS's is JLSInfo.version,
# e.g. "JLS 5.0"; the control's is "HelloSwingControl"). A user32 EnumWindows
# title enumeration is the documented deeper fallback if first light shows this
# misses (some AWT frames are not registered as the process MainWindow).
function Get-WindowCount {
    param($Proc, [string]$TitleRegex)
    $count = 0
    try {
        $p = Get-Process -Id $Proc.Id -ErrorAction SilentlyContinue
        if ($p) {
            $p.Refresh()
            if ($p.MainWindowHandle -ne [IntPtr]::Zero) { $count++ }
        }
    } catch { }
    if ($count -eq 0) {
        try {
            foreach ($p in Get-Process -ErrorAction SilentlyContinue) {
                if ($p.MainWindowHandle -ne [IntPtr]::Zero -and
                        $p.MainWindowTitle -and $p.MainWindowTitle -match $TitleRegex) {
                    $count++
                }
            }
        } catch { }
    }
    return $count
}

function Save-Screenshot {
    param([string]$Path)
    try {
        Add-Type -AssemblyName System.Windows.Forms -ErrorAction Stop
        Add-Type -AssemblyName System.Drawing -ErrorAction Stop
        $bounds = [System.Windows.Forms.SystemInformation]::VirtualScreen
        $bmp = New-Object System.Drawing.Bitmap($bounds.Width, $bounds.Height)
        $gfx = [System.Drawing.Graphics]::FromImage($bmp)
        $gfx.CopyFromScreen($bounds.Location, [System.Drawing.Point]::Empty, $bounds.Size)
        $bmp.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
        $gfx.Dispose(); $bmp.Dispose()
        return $true
    } catch {
        Write-RigLog "screenshot failed: $($_.Exception.Message)"
        return $false
    }
}

# Distinct-color count over a sampled grid (capped). A solid/black frame has 1
# color; any real captured frame has many, so >1 is the always-on non-blank /
# not-a-failed-capture gate. Returns 0 on any error.
function Get-ColorCount {
    param([string]$Path)
    try {
        Add-Type -AssemblyName System.Drawing -ErrorAction Stop
        $bmp = New-Object System.Drawing.Bitmap($Path)
        $w = $bmp.Width; $h = $bmp.Height
        if ($w -le 0 -or $h -le 0) { $bmp.Dispose(); return 0 }
        $sx = [Math]::Max(1, [int]($w / 256)); $sy = [Math]::Max(1, [int]($h / 256))
        $seen = New-Object 'System.Collections.Generic.HashSet[int]'
        for ($y = 0; $y -lt $h; $y += $sy) {
            for ($x = 0; $x -lt $w; $x += $sx) {
                [void]$seen.Add($bmp.GetPixel($x, $y).ToArgb())
                if ($seen.Count -ge 4096) { $bmp.Dispose(); return $seen.Count }
            }
        }
        $bmp.Dispose()
        return $seen.Count
    } catch {
        Write-RigLog "color count failed: $($_.Exception.Message)"
        return 0
    }
}

# ------------------------------------------------------------- wait helper
# Polls $Probe for a mapped window up to $TimeoutSec, bounded by a Stopwatch so
# it can never hang. Returns 0 (mapped), 1 (timeout), or 2 (process exited).
function Wait-ForWindow {
    param($Proc, [string]$TitleRegex, [int]$TimeoutSec, [scriptblock]$Probe)
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    while ($sw.Elapsed.TotalSeconds -lt $TimeoutSec) {
        try { if ($Proc.HasExited) { return 2 } } catch { }
        $n = & $Probe $Proc $TitleRegex
        if ($n -gt 0) { $script:LastWindowCount = $n; return 0 }
        Start-Sleep -Seconds 1
    }
    return 1
}

# ------------------------------------------------------------- core rig
# The whole exit-code classification lives here, driven entirely through the
# injected delegates so scripts\windows-rig-selftest.ps1 can exercise every
# branch with no real GUI. Returns the process exit code (0/1/2).
function Invoke-GuiBootRig {
    param(
        [string]$ArtifactsDir,
        [scriptblock]$LaunchControl,
        [scriptblock]$LaunchJls,
        [scriptblock]$WindowProbe,
        [scriptblock]$Capture,
        [scriptblock]$ColorCount,
        [int]$ControlTimeout = 60,
        [int]$WindowTimeout = 90,
        [int]$RenderTimeout = 30
    )

    New-Item -ItemType Directory -Force -Path $ArtifactsDir | Out-Null

    # readiness / empty baseline: prove the station is screenshottable first. A
    # capture delegate that THROWS (not just returns $false) is still an
    # environment/rig fault, so classify it exit 2 rather than letting it escape.
    try {
        $ready = & $Capture (Join-Path $ArtifactsDir 'desktop-before.png')
    } catch {
        Write-RigLog "error: readiness capture threw ($($_.Exception.Message)); environment/rig fault, not JLS"
        return 2
    }
    if (-not $ready) {
        Write-RigLog 'error: could not screenshot the desktop (capture failed); environment/rig fault, not JLS'
        return 2
    }
    Write-RigLog 'readiness gate passed: desktop screenshot OK'

    $control = $null
    $jls = $null
    try {
        # ---- control experiment ----
        Write-RigLog 'launching HelloSwingControl (environment-blocker control)'
        $control = & $LaunchControl
        $cs = Wait-ForWindow $control 'HelloSwingControl' $ControlTimeout $WindowProbe
        if ($cs -ne 0) {
            $why = if ($cs -eq 2) { 'control process exited before mapping a window' }
                   else { "no control window after ${ControlTimeout}s" }
            Set-Content -Path (Join-Path $ArtifactsDir 'control-verdict.txt') -Value $why
            Write-RigLog "error: $why; the blocker is the environment (window station/AWT), not JLS"
            return 2
        }
        Set-Content -Path (Join-Path $ArtifactsDir 'control-verdict.txt') `
            -Value "control window mapped ($script:LastWindowCount window(s))"

        # the control window must also screenshot non-blank: a blank capture
        # here means the station is not being captured (environment), and the
        # control frame exhibiting it is what disambiguates it from a JLS-side
        # blank window later.
        [void](& $Capture (Join-Path $ArtifactsDir 'control.png'))
        $cc = [int](& $ColorCount (Join-Path $ArtifactsDir 'control.png'))
        Write-RigLog "control screenshot unique colors: $cc"
        if ($cc -le 1) {
            Write-RigLog "error: the control screenshot is blank ($cc unique color); the window station is not being captured - environment fault, not JLS"
            return 2
        }
        Stop-ProcessSafe $control
        $control = $null

        # ---- launch the real jar ----
        Write-RigLog 'launching JLS (real app entry point, jar Main-Class jls.JLS)'
        $jls = & $LaunchJls
        $js = Wait-ForWindow $jls 'JLS' $WindowTimeout $WindowProbe
        if ($js -ne 0) {
            $why = if ($js -eq 2) { 'JLS exited before mapping a window' }
                   else { "no JLS window mapped after ${WindowTimeout}s" }
            # control mapped + non-blank, JLS did not: JLS-side failure (exit 1)
            Write-RigLog "error: $why (the control mapped and captured non-blank, so this is a JLS-side failure)"
            return 1
        }
        Set-Content -Path (Join-Path $ArtifactsDir 'jls-window-info.txt') `
            -Value "JLS windows mapped: $script:LastWindowCount"
        Write-RigLog "JLS window is up ($script:LastWindowCount window(s))"

        # ---- non-blank proof (P1) ----
        # Swing shows the frame a beat before it paints; poll the screenshot
        # until it has more than one unique color, up to RenderTimeout.
        $jc = 0
        $sw = [System.Diagnostics.Stopwatch]::StartNew()
        do {
            [void](& $Capture (Join-Path $ArtifactsDir 'jls-window.png'))
            $jc = [int](& $ColorCount (Join-Path $ArtifactsDir 'jls-window.png'))
            if ($jc -gt 1) { break }
            Start-Sleep -Seconds 1
        } while ($sw.Elapsed.TotalSeconds -lt $RenderTimeout)
        Set-Content -Path (Join-Path $ArtifactsDir 'nonblank.txt') `
            -Value @("control unique colors: $cc", "jls-window unique colors: $jc")
        Write-RigLog "non-blank check: window=$jc colors"
        if ($jc -le 1) {
            # control captured non-blank moments ago, so capture works; a blank
            # JLS capture now is a JLS-side failure (mapped but rendered nothing).
            Write-RigLog "error: the JLS screenshot is blank ($jc unique color); the JLS window mapped but rendered nothing - JLS-side failure"
            return 1
        }

        Write-RigLog 'first light: success'
        return 0
    }
    catch {
        # Any unexpected exception from a delegate or the classification body is
        # an environment/rig fault, NOT a JLS-side failure: classify exit 2 with
        # the message and stack rather than letting it escape as a bare pwsh
        # error (which pwsh reports as exit 1 and would be mis-read as JLS-side).
        Write-RigLog "unexpected rig fault (classified environment/exit 2): $($_.Exception.Message)"
        if ($_.ScriptStackTrace) { Write-RigLog "stack: $($_.ScriptStackTrace)" }
        return 2
    }
    finally {
        Stop-ProcessSafe $jls
        Stop-ProcessSafe $control
    }
}

function Stop-ProcessSafe {
    param($Proc)
    if ($null -eq $Proc) { return }
    try { if (-not $Proc.HasExited) { $Proc.Kill() } } catch { }
}

# ------------------------------------------------------------- auto-run
# The ENTIRE entry point is wrapped so any unhandled script error classifies as
# exit 2 (environment/rig fault) with its message and stack - never exit 1 and
# never a bare pwsh failure. With Set-StrictMode + $ErrorActionPreference='Stop'
# an uncaught error would otherwise terminate pwsh with exit 1, which the JLS
# contract reserves for a JLS-side failure - the precise misclassification that
# turned the Windows first-light rig bug red as if JLS had failed (#265 Stage 2).
# The explicit `exit 2`/`exit $code` calls below terminate the process directly
# and are NOT intercepted by the catch.
if (-not $NoAutoRun) {
    try {
        if (-not (Get-Command $JavaExe -ErrorAction SilentlyContinue)) {
            Write-RigLog "error: no java found (set JAVA= or add one to PATH)"
            exit 2
        }

        $rigDir = Split-Path -Parent $PSCommandPath
        # $script:-scoped so the top-level Invoke-ControlLaunch / Invoke-JlsLaunch
        # functions (passed by reference into Invoke-GuiBootRig) can read them.
        $script:ControlSrc = Join-Path $rigDir 'HelloSwingControl.java'
        if (-not (Test-Path $script:ControlSrc)) {
            Write-RigLog "error: control program missing: $script:ControlSrc"
            exit 2
        }

        if (-not $Jar) {
            $Jar = Get-ChildItem -Path 'target\jls-*.jar' -ErrorAction SilentlyContinue |
                Sort-Object LastWriteTime -Descending | Select-Object -First 1 -ExpandProperty FullName
        }
        if (-not $Jar -or -not (Test-Path $Jar)) {
            Write-RigLog "error: no jar found; run 'mvn -DskipTests package' first or set JAR="
            exit 2
        }
        $script:Jar = $Jar
        $script:ArtifactsDir = $ArtifactsDir

        New-Item -ItemType Directory -Force -Path $ArtifactsDir | Out-Null
        Write-RigLog "artifacts: $ArtifactsDir"
        Write-RigLog "jar:       $Jar"

        # Launch helpers are passed by FUNCTION REFERENCE (${function:...}), not as
        # .GetNewClosure() script blocks: a closure cannot resolve the script-scope
        # Start-JavaProcess and dies "Start-JavaProcess is not recognized" (the
        # Windows first-light bug). Function references run in the script session
        # state, exactly like the probe/capture/color-count delegates.
        $code = Invoke-GuiBootRig `
            -ArtifactsDir $ArtifactsDir `
            -LaunchControl ${function:Invoke-ControlLaunch} `
            -LaunchJls ${function:Invoke-JlsLaunch} `
            -WindowProbe ${function:Get-WindowCount} `
            -Capture ${function:Save-Screenshot} `
            -ColorCount ${function:Get-ColorCount} `
            -ControlTimeout $ControlTimeout `
            -WindowTimeout $WindowTimeout `
            -RenderTimeout $RenderTimeout

        exit $code
    }
    catch {
        Write-RigLog "FATAL: unhandled rig/environment fault (classified exit 2, NOT a JLS-side failure): $($_.Exception.Message)"
        if ($_.ScriptStackTrace) { Write-RigLog "stack: $($_.ScriptStackTrace)" }
        exit 2
    }
}
