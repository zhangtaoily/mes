# 诊断：列出所有 here-string token 的起止行，并统计 token 级花括号深度
$p = 'd:\prj\mes\tmp\loop-test.ps1'
$text = [System.IO.File]::ReadAllText($p)
$tokens = $null; $errs = $null
[System.Management.Automation.Language.Parser]::ParseInput($text, [ref]$tokens, [ref]$errs) | Out-Null

Write-Host '--- HereString tokens ---'
$tokens | Where-Object { $_.Kind -like '*HereString*' } | ForEach-Object {
    Write-Host ("  {0}: L{1}C{2} -> L{3}C{4}" -f $_.Kind, $_.Extent.StartLineNumber, $_.Extent.StartColumnNumber, $_.Extent.EndLineNumber, $_.Extent.EndColumnNumber)
}

Write-Host '--- Brace depth (token level, outside strings) ---'
$depth = 0
$tokens | ForEach-Object {
    if ($_.Kind -eq 'LCurly') { $depth++ }
    elseif ($_.Kind -eq 'RCurly') { $depth-- }
}
Write-Host ("FINAL DEPTH = " + $depth)

Write-Host '--- Parse errors ---'
$errs | Select-Object -First 5 | ForEach-Object { Write-Host ("  L{0} C{1}: {2}" -f $_.Extent.StartLineNumber, $_.Extent.StartColumnNumber, $_.Message) }
