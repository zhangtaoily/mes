# E2E loop round 2: feed + batch-on for task PT20261007000003 (batch 26091341, schedule 17)
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
$TASKNO = 'PT20261007000003'
$BATCH = '26091341'
$SCHID = 17
$NOW = (Get-Date).ToString('yyyy-MM-ddTHH:mm:ss')

# equipment for workshop 1
$r = HttpJson 'GET' '/api/plan/base/equipments' $null $T
$eq = @($r.json.data) | Where-Object { $_.workshopId -eq $WSID } | Select-Object -First 1
$EQID = $eq.id
Write-Host ("equipment id=" + $EQID + " machineNo=" + $eq.machineNo)

# 1. task materials (from craft 11)
$r = HttpJson 'GET' "/api/site/feed-record/materials?taskNo=$TASKNO" $null $T
$mats = @($r.json.data)
Write-Host ("materials count=" + $mats.Count)
foreach ($m in $mats) {
    Write-Host ("  id=" + $m.id + " rawBatchNo=" + $m.rawBatchNo + " materialNo=" + $m.materialNo + " name=" + $m.materialName + " pick=" + $m.pickQty + " fed=" + $m.fedQty)
}
if ($mats.Count -eq 0) { Write-Host 'NO-MATERIALS'; exit 1 }

$m1 = $mats[0]
$feedBody = '{"feed":{"taskNo":"' + $TASKNO + '","workshopId":' + $WSID + ',"batchNo":"' + $BATCH + '","rawDetail":"E2E loop2","feedQty":500,"isFinish":1,"feedBy":"admin","feedTime":"' + $NOW + '"},"items":[{"rawBatchNo":"' + $m1.rawBatchNo + '","materialNo":"' + $m1.materialNo + '","materialName":"' + $m1.materialName + '","materialSpec":"' + $m1.materialSpec + '","pickQty":500,"thisFeedQty":500,"fedQty":500}]}'
$r = HttpJson 'POST' '/api/site/feed-record' $feedBody $T
Write-Host ("feed code=" + $r.json.code + " msg=" + $r.json.msg + " data=" + $r.json.data)
if ($r.json.code -ne 200) { Write-Host ("feed raw=" + $r.raw) }

# 2. batch-on (PC source=1) on schedule 17
$boBody = '{"batchOn":{"scheduleId":' + $SCHID + ',"workshopId":' + $WSID + ',"batchNo":"' + $BATCH + '","traceBatchNo":"' + $BATCH + '","processId":1,"processFlag":"E2E2","equipmentId":' + $EQID + ',"shift":"甲班","source":1,"batchOnTime":"' + $NOW + '","planQty":500},"flags":[],"crafts":[]}'
$r = HttpJson 'POST' '/api/site/batch-on' $boBody $T
Write-Host ("batch-on code=" + $r.json.code + " msg=" + $r.json.msg + " data=" + $r.json.data)
if ($r.json.code -ne 200) { Write-Host ("bo raw=" + $r.raw) }

# 3. verify batch-on list
$r = HttpJson 'GET' '/api/site/batch-on?pageNum=1&pageSize=5' $null $T
$rows = @($r.json.data.rows)
Write-Host ("batch-on total=" + $r.json.data.total)
foreach ($b in $rows) {
    Write-Host ("  bo " + $b.batchOn.batchOnNo + " sched=" + $b.batchOn.scheduleId + " isOff=" + $b.batchOn.isBatchOff + " shift=" + $b.batchOn.shift + " proc=" + $b.processName + " source=" + $b.batchOn.source)
}
