# ASCII-only: parse progressively larger prefixes at balanced checkpoints
# to find the line range where the parse error first appears
$p = 'd:\prj\mes\tmp\loop-test.ps1'
$all = [System.IO.File]::ReadAllText($p)
$lines = $all -split "`r?`n"
$checkpoints = @(39, 41, 60, 81, 88, 112, 123, 140, 150, 169, 178, 187, 223, 254, 278, 282)
foreach ($n in $checkpoints) {
    $prefix = ($lines[0..($n-1)] -join "`r`n")
    $tokens = $null; $errs = $null
    [System.Management.Automation.Language.Parser]::ParseInput($prefix, [ref]$tokens, [ref]$errs) | Out-Null
    if ($errs.Count -eq 0) {
        Write-Host ("L{0}: OK" -f $n)
    }
    else {
        $msgs = ($errs | Select-Object -First 3 | ForEach-Object { ("L{0}:{1}" -f $_.Extent.StartLineNumber, $_.ErrorId) }) -join ' | '
        Write-Host ("L{0}: ERR {1}" -f $n, $msgs)
    }
}
