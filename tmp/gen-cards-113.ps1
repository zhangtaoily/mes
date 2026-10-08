# Generate machine cards for craft 113 schedules (batch 26091341)
$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [Text.Encoding]::UTF8
$BASE = 'http://127.0.0.1:8080'

function HttpJson($method, $path, $body, $token) {
    $req = [System.Net.HttpWebRequest]::Create("$BASE$path")
    $req.Method = $method
    $req.ContentType = 'application/json; charset=utf-8'
    $req.Timeout = 30000
    if ($token) { $req.Headers.Add('Authorization', "Bearer $token") }
    if ($body) {
        $bytes = [Text.Encoding]::UTF8.GetBytes($body)
        $req.ContentLength = $bytes.Length
        $s = $req.GetRequestStream(); $s.Write($bytes, 0, $bytes.Length); $s.Close()
    }
    try { $resp = $req.GetResponse() } catch [System.Net.WebException] { $resp = $_.Exception.Response }
    $ms = New-Object IO.MemoryStream
    $resp.GetResponseStream().CopyTo($ms)
    $raw = [Text.Encoding]::UTF8.GetString($ms.ToArray())
    $json = $null
    try { $json = $raw | ConvertFrom-Json } catch {}
    return @{ http = [int]$resp.StatusCode; json = $json; raw = $raw }
}

$r = HttpJson 'POST' '/api/auth/login' '{"loginName":"admin","password":"admin@2026"}' $null
$T = $r.json.data.token
Write-Host ("login code=" + $r.json.code)

# 1. get schedules, find batch 26091341
$r = HttpJson 'GET' '/api/plan/schedules?pageNum=1&pageSize=20' $null $T
Write-Host ("schedules http=" + $r.http + " rawhead=" + $r.raw.Substring(0, [Math]::Min(400, $r.raw.Length)))
$rows = @($r.json.data.rows)
$ids = @()
foreach ($s in $rows) {
    $bn = $s.schedule.batchNo
    Write-Host ("  sched " + $s.schedule.scheduleNo + " id=" + $s.schedule.id + " batch=" + $bn)
    if ($bn -eq '26091341') { $ids += [int]$s.schedule.id }
}
Write-Host ("ids=" + ($ids -join ','))

if ($ids.Count -gt 0) {
    $body = '{"ids":[' + ($ids -join ',') + ']}'
    $r = HttpJson 'POST' '/api/plan/cards/generate' $body $T
    Write-Host ("generate http=" + $r.http + " code=" + $r.json.code + " msg=" + $r.json.message + " data=" + ($r.json.data -join ','))
}

# 2. verify card list
$r = HttpJson 'GET' '/api/plan/cards?pageNum=1&pageSize=20' $null $T
$cards = @($r.json.data.rows)
Write-Host ("cards total=" + $r.json.data.total)
foreach ($c in $cards) {
    Write-Host ("  card " + $c.card.cardNo + " | " + $c.card.status + " | sched=" + $c.scheduleNo + " | batch=" + $c.card.batchNo + " | procId=" + $c.card.processId + " | machine=" + $c.machineNo + " | id=" + $c.card.id)
}
