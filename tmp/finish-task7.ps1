# Task finish (le-bi) for task 7: negative test (schedule 18 no batch-on) -> add batch-on -> positive finish
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

# [NEG] finish task 7 -> expect reject: schedule 18 has no batch-on (chain broken)
$r = HttpJson 'POST' '/api/plan/tasks/finish' '{"ids":[7]}' $T
Write-Host ("[NEG] finish code=" + $r.json.code + " msg=" + $r.json.msg)

# add batch-on for schedule 18 (process 2, same batch 26091341)
$body = '{"batchOn":{"scheduleId":18,"processId":2,"equipmentId":1,"source":1,"planQty":500}}'
$r = HttpJson 'POST' '/api/site/batch-on' $body $T
Write-Host ("batch-on s18 code=" + $r.json.code + " msg=" + $r.json.msg + " data=" + $r.json.data)

# [POS] finish task 7 again -> expect 200 + cascade (auto off-batch, flag release, route mark)
$r = HttpJson 'POST' '/api/plan/tasks/finish' '{"ids":[7]}' $T
Write-Host ("[POS] finish code=" + $r.json.code + " msg=" + $r.json.msg)

# verify task status
$r = HttpJson 'GET' '/api/plan/tasks/7' $null $T
$d = $r.json.data
Write-Host ("task7 no=" + $d.taskNo + " docStatus=" + $d.docStatus + " status=" + $d.status)

# verify batch-on closed loop
$r = HttpJson 'GET' '/api/site/batch-on?pageNum=1&pageSize=5' $null $T
$rows = @($r.json.data.rows)
foreach ($b in $rows) {
    Write-Host ("  bo " + $b.batchOn.batchOnNo + " sched=" + $b.batchOn.scheduleId + " off=" + $b.batchOn.isBatchOff + " fin=" + $b.batchOn.isFinished)
}
