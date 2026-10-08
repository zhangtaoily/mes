$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [Text.Encoding]::UTF8
$BASE = 'http://127.0.0.1:8080'
$req = [System.Net.HttpWebRequest]::Create("$BASE/api/auth/login")
$req.Method = 'POST'; $req.ContentType = 'application/json; charset=utf-8'
$b = [Text.Encoding]::UTF8.GetBytes('{"loginName":"admin","password":"admin@2026"}')
$req.ContentLength = $b.Length
$s = $req.GetRequestStream(); $s.Write($b,0,$b.Length); $s.Close()
$resp = $req.GetResponse(); $ms = New-Object IO.MemoryStream; $resp.GetResponseStream().CopyTo($ms)
$T = (([Text.Encoding]::UTF8.GetString($ms.ToArray()) | ConvertFrom-Json).data.token)

$req2 = [System.Net.HttpWebRequest]::Create("$BASE/api/plan/schedules/task-details")
$req2.Method = 'POST'; $req2.ContentType = 'application/json; charset=utf-8'
$req2.Headers.Add('Authorization', "Bearer $T")
$b2 = [Text.Encoding]::UTF8.GetBytes('{"taskId":4}')
$req2.ContentLength = $b2.Length
$s2 = $req2.GetRequestStream(); $s2.Write($b2,0,$b2.Length); $s2.Close()
try { $r2 = $req2.GetResponse() } catch [System.Net.WebException] { $r2 = $_.Exception.Response }
$ms2 = New-Object IO.MemoryStream; $r2.GetResponseStream().CopyTo($ms2)
Write-Host ("http=" + [int]$r2.StatusCode)
Write-Host ([Text.Encoding]::UTF8.GetString($ms2.ToArray()))
