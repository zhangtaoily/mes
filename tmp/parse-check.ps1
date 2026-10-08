# 语法检查 loop-test.ps1
$tokens = $null; $errs = $null
[System.Management.Automation.Language.Parser]::ParseFile('d:\prj\mes\tmp\loop-test.ps1', [ref]$tokens, [ref]$errs) | Out-Null
if ($errs.Count -eq 0) { Write-Host 'PARSE OK' }
else {
    Write-Host ("PARSE ERRORS: " + $errs.Count)
    $errs | Select-Object -First 10 | ForEach-Object {
        Write-Host ("  L{0} C{1}: {2}" -f $_.Extent.StartLineNumber, $_.Extent.StartColumnNumber, $_.Message)
    }
}
# 检查 BOM
$bytes = [System.IO.File]::ReadAllBytes('d:\prj\mes\tmp\loop-test.ps1')
Write-Host ("First bytes: {0:X2} {1:X2} {2:X2}" -f $bytes[0], $bytes[1], $bytes[2])
