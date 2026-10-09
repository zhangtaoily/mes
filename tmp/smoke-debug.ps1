﻿﻿﻿﻿﻿﻿﻿﻿# 冒烟失败项原始响应调试
$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [Text.Encoding]::UTF8
$BASE = 'http://127.0.0.1:8080'

function HttpRaw($method, $path, $token) {
    $req = [System.Net.HttpWebRequest]::Create("$BASE$path")
    $req.Method = $method
    $req.ContentType = 'application/json; charset=utf-8'
    $req.Timeout = 30000
    if ($token) { $req.Headers.Add('Authorization', "Bearer $token") }
    try { $resp = $req.GetResponse() } catch [System.Net.WebException] { $resp = $_.Exception.Response }
    $ms = New-Object IO.MemoryStream
    $resp.GetResponseStream().CopyTo($ms)
    return @{ http = [int]$resp.StatusCode; raw = [Text.Encoding]::UTF8.GetString($ms.ToArray()) }
}

$login = HttpRaw 'POST' '/api/auth/login' $null
# login needs body; use full inline for admin
$req = [System.Net.HttpWebRequest]::Create("$BASE/api/auth/login")
$req.Method = 'POST'; $req.ContentType = 'application/json; charset=utf-8'
$b = [Text.Encoding]::UTF8.GetBytes('{"loginName":"admin","password":"admin@2026"}')
$req.ContentLength = $b.Length
$s = $req.GetRequestStream(); $s.Write($b,0,$b.Length); $s.Close()
$resp = $req.GetResponse(); $ms = New-Object IO.MemoryStream; $resp.GetResponseStream().CopyTo($ms)
$T = (([Text.Encoding]::UTF8.GetString($ms.ToArray()) | ConvertFrom-Json).data.token)

$req = [System.Net.HttpWebRequest]::Create("$BASE/api/auth/login")
$req.Method = 'POST'; $req.ContentType = 'application/json; charset=utf-8'
$b = [Text.Encoding]::UTF8.GetBytes('{"loginName":"zhangt","password":"admin@2026"}')
$req.ContentLength = $b.Length
$s = $req.GetRequestStream(); $s.Write($b,0,$b.Length); $s.Close()
$resp = $req.GetResponse(); $ms = New-Object IO.MemoryStream; $resp.GetResponseStream().CopyTo($ms)
$TZ = (([Text.Encoding]::UTF8.GetString($ms.ToArray()) | ConvertFrom-Json).data.token)

Write-Host '--- 1. /api/auth/info (admin) ---'
$r = HttpRaw 'GET' '/api/auth/info' $T
Write-Host ("http={0} raw={1}" -f $r.http, $r.raw.Substring(0, [Math]::Min(400, $r.raw.Length)))

Write-Host '--- 2. /api/auth/menus (admin) 顶层与子级 ---'
$r = HttpRaw 'GET' '/api/auth/menus' $T
$j = $r.raw | ConvertFrom-Json
Write-Host ("顶层=" + @($j.data).Count)
$total = @($j.data).Count
foreach ($m in @($j.data)) { $total += @($m.children).Count }
Write-Host ("顶层+子级=" + $total)

Write-Host '--- 3. /api/hello/perm (zhangt) ---'
$r = HttpRaw 'GET' '/api/hello/perm' $TZ
Write-Host ("http={0} raw={1}" -f $r.http, $r.raw)

Write-Host '--- 4. /api/engine/code/next/工艺单 (URL编码) ---'
$enc = [Uri]::EscapeDataString('工艺单')
$r = HttpRaw 'GET' "/api/engine/code/next/$enc" $T
Write-Host ("http={0} raw={1}" -f $r.http, $r.raw)

Write-Host '--- 5. /api/engine/flow/buttons?billType=检验报告 ---'
$enc2 = [Uri]::EscapeDataString('检验报告')
$r = HttpRaw 'GET' "/api/engine/flow/buttons?billType=$enc2&status=编制中" $T
Write-Host ("http={0} raw={1}" -f $r.http, $r.raw.Substring(0, [Math]::Min(500, $r.raw.Length)))

Write-Host '--- 6. /api/engine/meta/columns/qc_report_item ---'
$r = HttpRaw 'GET' '/api/engine/meta/columns/qc_report_item' $T
Write-Host ("http={0} raw={1}" -f $r.http, $r.raw.Substring(0, [Math]::Min(500, $r.raw.Length)))

Write-Host '--- 7. /api/plan/schedules/task-details?taskId=4 ---'
$r = HttpRaw 'GET' '/api/plan/schedules/task-details?taskId=4' $T
Write-Host ("http={0} raw={1}" -f $r.http, $r.raw.Substring(0, [Math]::Min(500, $r.raw.Length)))

Write-Host '--- 8. /api/site/output?pageNum=1&pageSize=5 ---'
$r = HttpRaw 'GET' '/api/site/output?pageNum=1&pageSize=5' $T
Write-Host ("http={0} raw={1}" -f $r.http, $r.raw.Substring(0, [Math]::Min(300, $r.raw.Length)))
