# 修复 loop-test.ps1 的换行符为 CRLF（PS5.1 here-string 要求），保留 UTF8 BOM
$p = 'd:\prj\mes\tmp\loop-test.ps1'
$c = [System.IO.File]::ReadAllText($p)
$crCount = ([regex]::Matches($c, "`r")).Count
$lfCount = ([regex]::Matches($c, "`n")).Count
Write-Host ("BEFORE: CR={0} LF={1}" -f $crCount, $lfCount)
$c = $c.Replace("`r`n", "`n").Replace("`n", "`r`n")
[System.IO.File]::WriteAllText($p, $c, (New-Object System.Text.UTF8Encoding $true))
$tokens = $null; $errs = $null
[System.Management.Automation.Language.Parser]::ParseFile($p, [ref]$tokens, [ref]$errs) | Out-Null
if ($errs.Count -eq 0) { Write-Host 'PARSE OK' } else { $errs | ForEach-Object { Write-Host ("L{0} C{1}: {2}" -f $_.Extent.StartLineNumber, $_.Extent.StartColumnNumber, $_.Message) } }
