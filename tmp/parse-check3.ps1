# ASCII-only: track brace depth, report unmatched close braces
$p = 'd:\prj\mes\tmp\loop-test.ps1'
$text = [System.IO.File]::ReadAllText($p)
$tokens = $null; $errs = $null
[System.Management.Automation.Language.Parser]::ParseInput($text, [ref]$tokens, [ref]$errs) | Out-Null

$depth = 0
$lines = $text -split "`n"
foreach ($t in $tokens) {
    if ($t.Kind -eq 'LCurly') { $depth++ }
    elseif ($t.Kind -eq 'RCurly') {
        $depth--
        if ($depth -lt 0) {
            $ln = $t.Extent.StartLineNumber
            $line = $lines[$ln - 1]
            if ($line.Length -gt 130) { $line = $line.Substring(0, 130) }
            Write-Host ("EXTRA-CLOSE at L{0}C{1} depth={2}" -f $ln, $t.Extent.StartColumnNumber, $depth)
            Write-Host ("    LINE: " + $line.TrimEnd())
            $depth = 0
        }
    }
}
Write-Host ("FINAL-DEPTH " + $depth)
