<#
.SYNOPSIS
  windows-rig-selftest.ps1 - logic self-test for scripts\windows-rig.ps1
  (issue #111 Stage W7).

.DESCRIPTION
  The real rig (windows-rig.ps1) can only run on a windows-latest runner with an
  interactive window station, System.Drawing screen capture, and the JLS jar.
  Its *control flow* - the failure classification that decides between exit 0
  (JLS window mapped + non-blank screenshot), exit 1 (JLS-side failure), and
  exit 2 (environment: control never mapped, or a blank/uncaptured station) -
  is pure PowerShell and must not regress silently before first light on CI.

  This harness dot-sources the unmodified rig with -NoAutoRun (so it defines its
  functions without launching anything) and drives Invoke-GuiBootRig against
  injected stub delegates - a fake window probe, capture, color count, and
  process launchers we script per case - asserting the rig classifies each
  scenario with the documented exit code. It needs no Windows GUI, no real
  process, and no network. (Note: PowerShell was unavailable in the authoring
  sandbox, so unlike the Linux/macOS selftests this one is first executed on the
  Windows CI runner itself.)

  Usage:  pwsh -File scripts\windows-rig-selftest.ps1
  Exit 0 when every scenario classified correctly; 1 otherwise.
#>
[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

. (Join-Path $PSScriptRoot 'windows-rig.ps1') -NoAutoRun

$script:Work = Join-Path ([System.IO.Path]::GetTempPath()) ("windows-rig-selftest-" + [guid]::NewGuid())
New-Item -ItemType Directory -Force -Path $script:Work | Out-Null

# scenario knobs, reset per case
$script:S = @{}

# --- stub delegates (all read $script:S) ---------------------------------
$stubLaunchControl = {
    if ($script:S.ThrowInControl) { throw 'stub control launch delegate threw an unexpected exception' }
    [pscustomobject]@{ Id = 1001; HasExited = $false; Kind = 'control' }
}
$stubLaunchJls     = {
    if ($script:S.ThrowInJls) { throw 'stub JLS launch delegate threw an unexpected exception' }
    [pscustomobject]@{ Id = 1002; HasExited = [bool]$script:S.JlsCrash; Kind = 'jls' }
}

$stubWindowProbe = {
    param($Proc, $TitleRegex)
    switch ($Proc.Kind) {
        'control' { if ($script:S.ControlMaps) { 1 } else { 0 } }
        'jls'     { if ($script:S.JlsMaps)     { 1 } else { 0 } }
        default   { 0 }
    }
}

$stubCapture = {
    param($Path)
    [bool]$script:S.CaptureOk
}

$stubColorCount = {
    param($Path)
    if ($Path -like '*control*') { if ($script:S.ControlBlank) { return 1 } else { return 186 } }
    if ($Path -like '*jls*')     { if ($script:S.JlsBlank)     { return 1 } else { return 186 } }
    return 186
}

$script:Fail = 0

function Invoke-Case {
    param([string]$Name, [int]$Want, [hashtable]$Scenario)
    # sensible defaults; the scenario overrides specific knobs
    $script:S = @{
        ControlMaps    = $true
        JlsMaps        = $true
        JlsCrash       = $false
        ControlBlank   = $false
        JlsBlank       = $false
        CaptureOk      = $true
        ThrowInControl = $false
        ThrowInJls     = $false
    }
    foreach ($k in $Scenario.Keys) { $script:S[$k] = $Scenario[$k] }

    $artifacts = Join-Path $script:Work "run-$Name"
    $got = -1
    try {
        $got = Invoke-GuiBootRig `
            -ArtifactsDir $artifacts `
            -LaunchControl $stubLaunchControl `
            -LaunchJls $stubLaunchJls `
            -WindowProbe $stubWindowProbe `
            -Capture $stubCapture `
            -ColorCount $stubColorCount `
            -ControlTimeout 2 `
            -WindowTimeout 2 `
            -RenderTimeout 2
    } catch {
        Write-Host "selftest: FAIL  $Name (threw: $($_.Exception.Message))"
        $script:Fail = 1
        return
    }
    if ($got -eq $Want) {
        Write-Host "selftest: PASS  $Name (exit $got)"
    } else {
        Write-Host "selftest: FAIL  $Name (expected exit $Want, got $got)"
        $script:Fail = 1
    }
}

# 1) success: control maps + non-blank, JLS maps + non-blank -> exit 0
Invoke-Case 'success' 0 @{}
# 2) JLS-side failure: control ok, JLS crashes before a window -> exit 1
Invoke-Case 'jls-crash' 1 @{ JlsMaps = $false; JlsCrash = $true }
# 3) JLS-side failure: control ok, JLS never maps a window -> exit 1
Invoke-Case 'jls-nowindow' 1 @{ JlsMaps = $false }
# 4) JLS-side failure: JLS window maps but the screenshot is blank -> exit 1
Invoke-Case 'jls-blank' 1 @{ JlsBlank = $true }
# 5) environment blocker: even the control window never maps -> exit 2
Invoke-Case 'env-no-control' 2 @{ ControlMaps = $false }
# 6) environment blocker: the control screenshot is blank (station not
#    captured) -> exit 2
Invoke-Case 'env-control-blank' 2 @{ ControlBlank = $true }
# 7) environment blocker: the readiness screenshot fails outright -> exit 2
Invoke-Case 'env-capture-fail' 2 @{ CaptureOk = $false }
# 8) environment blocker: a launch delegate throws an UNEXPECTED exception ->
#    exit 2 (rig/environment fault), never a bare pwsh failure or exit 1
Invoke-Case 'jls-throws'     2 @{ ThrowInJls = $true }
Invoke-Case 'control-throws' 2 @{ ThrowInControl = $true }

# --- production-path integrity ------------------------------------------------
# The exact bug class the Windows first-light run hit: a production launch helper
# calling a function its scope could not see ("The term 'Start-JavaProcess' is
# not recognized"), which the stub-delegate scenarios above can NEVER catch
# because they replace the launch path entirely. Two checks that DO catch it,
# both of which FAIL on the pre-fix script:
#   (1) AST-parse windows-rig.ps1 and assert every function the production path
#       calls is DEFINED at top-level script scope. Pre-fix there were no
#       Invoke-ControlLaunch / Invoke-JlsLaunch functions at all - the launch
#       logic hid inside .GetNewClosure() locals reachable only during auto-run -
#       so the required-set assertion reports them missing and fails.
#   (2) A reachability probe that actually INVOKES the real launch delegates with
#       Start-JavaProcess overridden by a no-op stub (no java is launched) and
#       asserts they resolve it. If the launch path is ever wrapped in a scope
#       that cannot see Start-JavaProcess (the closure bug), this reproduces
#       "Start-JavaProcess is not recognized" as a red test.
function Test-ProductionPath {
    $rig = Join-Path $PSScriptRoot 'windows-rig.ps1'
    $tokens = $null; $perrors = $null
    $ast = [System.Management.Automation.Language.Parser]::ParseFile($rig, [ref]$tokens, [ref]$perrors)
    if ($perrors -and $perrors.Count -gt 0) {
        Write-Host "selftest: FAIL  production-path (windows-rig.ps1 has $($perrors.Count) parse error(s))"
        $script:Fail = 1; return
    }

    $defined = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::OrdinalIgnoreCase)
    foreach ($f in $ast.FindAll({ param($n) $n -is [System.Management.Automation.Language.FunctionDefinitionAst] }, $true)) {
        [void]$defined.Add($f.Name)
    }
    $required = @(
        'Start-JavaProcess','Invoke-ControlLaunch','Invoke-JlsLaunch','Invoke-GuiBootRig',
        'Get-WindowCount','Save-Screenshot','Get-ColorCount','Wait-ForWindow',
        'Stop-ProcessSafe','Write-RigLog'
    )
    $missing = @($required | Where-Object { -not $defined.Contains($_) })
    if ($missing.Count -gt 0) {
        Write-Host "selftest: FAIL  production-path (undefined production-path function(s): $($missing -join ', '))"
        $script:Fail = 1; return
    }

    # each launch delegate must call a function the script actually defines
    foreach ($name in 'Invoke-ControlLaunch','Invoke-JlsLaunch') {
        $fn = $ast.FindAll({ param($n)
            $n -is [System.Management.Automation.Language.FunctionDefinitionAst] -and $n.Name -eq $name
        }, $true) | Select-Object -First 1
        $calls = @($fn.Body.FindAll({ param($n) $n -is [System.Management.Automation.Language.CommandAst] }, $true) |
            ForEach-Object { $_.GetCommandName() } | Where-Object { $_ })
        if ($calls -notcontains 'Start-JavaProcess') {
            Write-Host "selftest: FAIL  production-path ($name does not call Start-JavaProcess)"
            $script:Fail = 1; return
        }
    }

    # reachability probe: override Start-JavaProcess at script scope with a no-op
    # (records the call, launches nothing) and invoke the REAL launch delegates.
    $script:ProbeCalled = @{}
    function script:Start-JavaProcess {
        param([string[]]$ArgList, [string]$OutLog, [string]$ErrLog)
        $script:ProbeCalled[$OutLog] = $true
        [pscustomobject]@{ Id = 4242; HasExited = $false; Probe = $true }
    }
    $probeDir = Join-Path $script:Work 'probe'
    New-Item -ItemType Directory -Force -Path $probeDir | Out-Null
    $script:ArtifactsDir = $probeDir
    $script:ControlSrc   = Join-Path $probeDir 'HelloSwingControl.java'
    $script:Jar          = Join-Path $probeDir 'jls-probe.jar'
    $ok = $true
    try {
        $c = Invoke-ControlLaunch
        $j = Invoke-JlsLaunch
        if ($null -eq $c -or $c.Id -ne 4242) { $ok = $false }
        if ($null -eq $j -or $j.Id -ne 4242) { $ok = $false }
        if ($script:ProbeCalled.Count -lt 2) { $ok = $false }
    } catch {
        Write-Host "selftest: FAIL  production-path (launch delegate threw resolving Start-JavaProcess: $($_.Exception.Message))"
        $script:Fail = 1; return
    }
    if ($ok) {
        Write-Host 'selftest: PASS  production-path (launch delegates defined and resolve Start-JavaProcess)'
    } else {
        Write-Host 'selftest: FAIL  production-path (launch delegates did not invoke the stubbed Start-JavaProcess)'
        $script:Fail = 1
    }
}
Test-ProductionPath

Remove-Item -Recurse -Force $script:Work -ErrorAction SilentlyContinue

if ($script:Fail -eq 0) {
    Write-Host 'selftest: all scenarios classified correctly'
} else {
    Write-Host 'selftest: one or more scenarios misclassified'
}
exit $script:Fail
