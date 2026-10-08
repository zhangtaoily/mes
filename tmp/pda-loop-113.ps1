# Feed + batch-on for craft 113 (task PT20261007000002, batch 26091341, schedule 15)
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

$WSID = 1
$TASKID = 6
$TASKNO = 'PT20261007000002'
$BATCH = '26091341'
$SCHID = 15
$P1 = 1
$NOW = (Get-Date).ToString('yyyy-MM-ddTHH:mm:ss')

# equipment for workshop 1
$r = HttpJson 'GET' '/api/plan/base/equipments' $null $T
$eq = @($r.json.data) | Where-Object { $_.workshopId -eq $WSID } | Select-Object -First 1
$EQID = $eq.id
Write-Host ("equipment id=" + $EQID + " name-as-bytes=" + [Text.Encoding]::UTF8.GetString([Text.Encoding]::UTF8.GetBytes($eq.machineNo)))

# 1. feed (finish)
$feedBody = '{"feed":{"taskNo":"' + $TASKNO + '","workshopId":' + $WSID + ',"batchNo":"' + $BATCH + '","rawDetail":"E2E-raw wool","feedQty":500,"isFinish":1,"feedBy":"admin","feedTime":"' + $NOW + '"},"items":[{"rawBatchNo":"RAW-113-001","materialNo":"M001","materialName":"WoolTop","materialSpec":"21.5u","pickQty":500,"thisFeedQty":500,"fedQty":500}]}'
$r = HttpJson 'POST' '/api/site/feed-record' $feedBody $T
Write-Host ("feed code=" + $r.json.code + " msg=" + $r.json.msg + " data=" + $r.json.data)

# 2. batch-on (PC source=1)
$boBody = '{"batchOn":{"scheduleId":' + $SCHID + ',"workshopId":' + $WSID + ',"batchNo":"' + $BATCH + '","traceBatchNo":"' + $BATCH + '","processId":' + $P1 + ',"processFlag":"E2E","equipmentId":' + $EQID + ',"source":1,"batchOnTime":"' + $NOW + '","planQty":500},"flags":[],"crafts":[]}'
$r = HttpJson 'POST' '/api/site/batch-on' $boBody $T
$BOID = $null
if ($r.json.code -eq 200) {
    $bd = $r.json.data
    if ($bd -is [array]) { $BOID = $bd[0].id } elseif ($bd.id) { $BOID = $bd.id } else { $BOID = $bd }
}
Write-Host ("batch-on code=" + $r.json.code + " msg=" + $r.json.msg + " batchOnId=" + $BOID)
if ($r.json.code -ne 200) { Write-Host ("raw=" + $r.raw) }
